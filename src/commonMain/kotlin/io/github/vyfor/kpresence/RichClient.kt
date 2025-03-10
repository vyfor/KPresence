@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package io.github.vyfor.kpresence

import io.github.vyfor.kpresence.event.ActivityUpdateEvent
import io.github.vyfor.kpresence.event.DisconnectEvent
import io.github.vyfor.kpresence.event.Event
import io.github.vyfor.kpresence.event.ReadyEvent
import io.github.vyfor.kpresence.exception.*
import io.github.vyfor.kpresence.ipc.*
import io.github.vyfor.kpresence.logger.ILogger
import io.github.vyfor.kpresence.rpc.*
import io.github.vyfor.kpresence.utils.getProcessId
import kotlin.concurrent.Volatile
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Gets the default base paths for Discord IPC on the current platform.
 * @return A mutable list of default base paths.
 */
expect fun getDefaultPaths(): MutableList<String>

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

  /**
   * List of paths to search for Discord IPC connections. For Windows, this is typically just
   * `\\.\pipe`. For Unix-like systems, these are directories like `/tmp` or environment variables.
   * The actual socket/pipe name (`discord-ipc-X`) will be appended by the implementation. This list
   * can be modified before calling [connect].
   */
  val discordPaths: MutableList<String> = getDefaultPaths()

  @Volatile
  var connectionState = ConnectionState.DISCONNECTED
    private set
  var onReady: (RichClient.() -> Unit)? = null
  var onDisconnect: (RichClient.() -> Unit)? = null
  var onActivityUpdate: (RichClient.() -> Unit)? = null
  var logger: ILogger? = null

  /**
   * Allows customization of the Discord IPC paths.
   * @param block A lambda that receives the mutable list of paths and can modify it.
   * @return The current [RichClient] instance for chaining.
   */
  fun configurePaths(block: MutableList<String>.() -> Unit): RichClient {
    discordPaths.apply(block)
    logger?.debug("Discord IPC paths configured: $discordPaths")
    return this
  }

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

    connection.open(discordPaths)
    connectionState = ConnectionState.CONNECTED
    logger?.info("Connected to Discord")
    handshake()
    listen()
    if (shouldBlock) {
      runBlocking { signal.lock() }
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
   * Updates the current activity shown on Discord. Skips identical presence updates.
   * @param activity The activity to display.
   * @return The current [RichClient] instance for chaining.
   * @throws NotConnectedException if the client is not connected to Discord.
   * @throws PipeReadException if an error occurs while reading from the IPC pipe.
   * @throws PipeWriteException if an error occurs while writing to the IPC pipe.
   * @throws IllegalArgumentException if the validation of the [activity]'s fields fails.
   */
  fun update(activity: Activity?): RichClient {
    sendActivityUpdate(activity)

    return this
  }

  /**
   * Updates the current activity shown on Discord. Skips identical presence updates.
   * @param activityBlock A lambda to construct an [Activity].
   * @return The current [RichClient] instance for chaining.
   * @throws NotConnectedException if the client is not connected to Discord.
   * @throws PipeReadException if an error occurs while reading from the IPC pipe.
   * @throws PipeWriteException if an error occurs while writing to the IPC pipe.
   * @throws IllegalArgumentException if the validation of the [activity]'s fields fails.
   */
  fun update(activityBlock: ActivityBuilder.() -> Unit): RichClient {
    sendActivityUpdate(ActivityBuilder().apply(activityBlock).build())

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

    write(2, null)
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

  private fun sendActivityUpdate(currentActivity: Activity?) {
    if (connectionState != ConnectionState.SENT_HANDSHAKE) {
      throw NotConnectedException()
    }

    if (lastActivity == currentActivity) {
      logger?.debug("Received identical presence update. Skipping")
      return
    }
    lastActivity = currentActivity

    val packet =
            Json.encodeToString(
                    Packet("SET_ACTIVITY", PacketArgs(getProcessId(), lastActivity), "-")
            )
    logger?.apply {
      debug("Sending presence update with payload:")
      debug(packet)
    }

    write(1, packet)
  }

  private fun handshake() {
    write(0, "{\"v\": 1,\"client_id\":\"$clientId\"}")
  }

  private fun listen(): Job {
    return coroutineScope.launch {
      while (isActive && connectionState != ConnectionState.DISCONNECTED) {
        val response =
                read() ?: if (connectionState == ConnectionState.DISCONNECTED) break else continue
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
            if (response.data.decodeToString().contains("Invalid Client ID")) {
              throw InvalidClientIdException("'$clientId' is not a valid client ID")
            }

            if (connectionState != ConnectionState.DISCONNECTED) {
              connectionState = ConnectionState.DISCONNECTED
              connection.close()
              lastActivity = null
              logger?.warn("The connection was forcibly closed")
              onDisconnect?.invoke(this@RichClient)
            }
            break
          }
        }
      }
    }
  }

  private fun read(): Message? {
    return try {
      connection.read()
    } catch (e: ConnectionClosedException) {
      connectionState = ConnectionState.DISCONNECTED
      logger?.warn(
              "The connection was forcibly closed: ${e.message?.trimEnd()}. Client will be disconnected"
      )
      connection.close()
      lastActivity = null
      onDisconnect?.invoke(this@RichClient)
      null
    }
  }

  private fun write(opcode: Int, data: String?) {
    try {
      connection.write(opcode, data)
    } catch (e: ConnectionClosedException) {
      connectionState = ConnectionState.DISCONNECTED
      logger?.warn(
              "The connection was forcibly closed: ${e.message?.trimEnd()}. Client will be disconnected"
      )
      connection.close()
      lastActivity = null
      onDisconnect?.invoke(this@RichClient)
    }
  }
}

enum class ConnectionState {
  DISCONNECTED,
  CONNECTED,
  SENT_HANDSHAKE,
}
