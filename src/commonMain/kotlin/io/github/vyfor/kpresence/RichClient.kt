@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package io.github.vyfor.kpresence

import io.github.vyfor.kpresence.event.ActivityUpdateEvent
import io.github.vyfor.kpresence.event.DisconnectEvent
import io.github.vyfor.kpresence.event.Event
import io.github.vyfor.kpresence.event.ReadyEvent
import io.github.vyfor.kpresence.exception.*
import io.github.vyfor.kpresence.ipc.*
import io.github.vyfor.kpresence.logger.ILogger
import io.github.vyfor.kpresence.rpc.Activity
import io.github.vyfor.kpresence.rpc.Packet
import io.github.vyfor.kpresence.rpc.PacketArgs
import io.github.vyfor.kpresence.utils.getProcessId
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Manages client connections and activity updates for Discord presence.
 * @property clientId The Discord application client ID.
 */
class RichClient(
  var clientId: Long,
  val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
  private val connection = Connection()
  private var signal = Mutex(true)
  private var lastActivity: Activity? = null
  
  var connectionState = ConnectionState.DISCONNECTED
    private set
  var onReady: (RichClient.() -> Unit)? = null
  var onDisconnect: (RichClient.() -> Unit)? = null
  var onActivityUpdate: (RichClient.() -> Unit)? = null
  var logger: ILogger? = null
  
  /**
   * Establishes a connection to Discord.
   * @param shouldBlock Whether to block the current thread until the connection is established.
   * @return The current [RichClient] instance for chaining.
   * @throws InvalidClientIdException if the provided client ID is not valid.
   * @throws ConnectionException if an error occurs while establishing the connection.
   * @throws PipeReadException if an error occurs while reading from the IPC pipe.
   * @throws PipeWriteException if an error occurs while writing to the IPC pipe.
   */
  fun connect(shouldBlock: Boolean = true): RichClient {
    if (connectionState != ConnectionState.DISCONNECTED) {
      logger?.warn("Already connected to Discord. Skipping")
      return this
    }
    
    connection.open()
    connectionState = ConnectionState.CONNECTED
    logger?.info("Connected to Discord")
    handshake()
    listen()
    if (shouldBlock) {
      runBlocking {
        signal.lock()
      }
    }
    
    return this
  }
  
  /**
   * Attempts to reconnect if there is an already active connection.
   * @param shouldBlock Whether to block the current thread until the connection is established.
   * @return The current [RichClient] instance for chaining.
   * @throws InvalidClientIdException if the provided client ID is not valid.
   * @throws ConnectionException if an error occurs while establishing the connection.
   * @throws PipeReadException if an error occurs while reading from the IPC pipe.
   * @throws PipeWriteException if an error occurs while writing to the IPC pipe.
   */
  fun reconnect(shouldBlock: Boolean = true): RichClient {
    if (connectionState != ConnectionState.SENT_HANDSHAKE) {
      throw NotConnectedException()
    }
    
    shutdown()
    connect(shouldBlock)
    
    return this
  }
  
  /**
   * Updates the current activity shown on Discord.
   * Skips identical presence updates.
   * @param activity The activity to display.
   * @return The current [RichClient] instance for chaining.
   * @throws NotConnectedException if the client is not connected to Discord.
   * @throws PipeReadException if an error occurs while reading from the IPC pipe.
   * @throws PipeWriteException if an error occurs while writing to the IPC pipe.
   * @throws IllegalArgumentException if the validation of the [activity]'s fields fails.
   */
  fun update(activity: Activity?): RichClient {
    if (connectionState != ConnectionState.SENT_HANDSHAKE) {
      throw NotConnectedException()
    }
    
    if (lastActivity == activity) {
      logger?.debug("Received identical presence update. Skipping")
      return this
    }
    
    lastActivity = activity
    sendActivityUpdate()
    
    return this
  }
  
  /**
   * Clears the current activity shown on Discord.
   * @return The current [RichClient] instance for chaining.
   * @throws NotConnectedException if the client is not connected to Discord.
   * @throws PipeReadException if an error occurs while reading from the IPC pipe.
   * @throws PipeWriteException if an error occurs while writing to the IPC pipe.
   */
  fun clear(): RichClient {
    if (connectionState != ConnectionState.SENT_HANDSHAKE) {
      throw NotConnectedException()
    }
    
    update(null)
    
    return this
  }
  
  /**
   * Shuts down the connection to Discord and cleans up resources.
   * @return The current [RichClient] instance for chaining.
   */
  fun shutdown(): RichClient {
    if (connectionState == ConnectionState.DISCONNECTED) {
      logger?.warn("Already disconnected from Discord. Skipping disconnection")
      return this
    }
    
    connection.write(2, null)
    connectionState = ConnectionState.DISCONNECTED
    connection.close()
    lastActivity = null
    logger?.info("Disconnected from Discord")
    onDisconnect?.invoke(this@RichClient)
    
    return this
  }
  
  /**
   * Registers a callback function for the specified event.
   * @param T The type of [Event].
   * @param block The callback function to be executed when the event is triggered.
   * @return The current [RichClient] instance for chaining.
   */
  inline fun <reified T : Event> on(noinline block: RichClient.() -> Unit): RichClient {
    when (T::class) {
      ReadyEvent::class -> onReady = block
      ActivityUpdateEvent::class -> onActivityUpdate = block
      DisconnectEvent::class -> onDisconnect = block
    }
    
    return this
  }
  
  private fun sendActivityUpdate() {
    if (connectionState != ConnectionState.SENT_HANDSHAKE) return
    val packet = Json.encodeToString(Packet("SET_ACTIVITY", PacketArgs(getProcessId(), lastActivity), "-"))
    logger?.apply {
      debug("Sending presence update with payload:")
      debug(packet)
    }
    
    connection.write(1, packet)
  }
  
  private fun handshake() {
    connection.write(0, "{\"v\": 1,\"client_id\":\"$clientId\"}")
  }
  
  private fun listen(): Job {
    return coroutineScope.launch {
      while (isActive && connectionState != ConnectionState.DISCONNECTED) {
        val response = connection.read() ?: continue
        logger?.apply {
          trace("Received response:")
          trace("Message(opcode: ${response.opcode}, data: ${response.data.decodeToString()})")
        }
        when (response.opcode) {
          1 -> {
            if (connectionState == ConnectionState.CONNECTED) {
              if (response.data.decodeToString().contains("Invalid Client ID")) {
                throw InvalidClientIdException("'$clientId' is not a valid client ID")
              }
              
              connectionState = ConnectionState.SENT_HANDSHAKE
              if (signal.isLocked) signal.unlock()
              logger?.debug("Performed initial handshake")
              onReady?.invoke(this@RichClient)
              continue
            }
            
            logger?.debug("Successfully updated presence")
            onActivityUpdate?.invoke(this@RichClient)
          }
          2 -> {
            if (connectionState != ConnectionState.DISCONNECTED) {
              connectionState = ConnectionState.DISCONNECTED
              connection.close()
              lastActivity = null
              logger?.warn("The connection was forcibly closed")
              onDisconnect?.invoke(this@RichClient)
              break
            }
          }
        }
      }
    }
  }
}

enum class ConnectionState {
  DISCONNECTED,
  CONNECTED,
  SENT_HANDSHAKE,
}
