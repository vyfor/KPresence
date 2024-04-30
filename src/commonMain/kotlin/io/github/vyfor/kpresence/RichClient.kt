@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package io.github.vyfor.kpresence

import io.github.vyfor.kpresence.exception.*
import io.github.vyfor.kpresence.ipc.*
import io.github.vyfor.kpresence.logger.ILogger
import io.github.vyfor.kpresence.rpc.Activity
import io.github.vyfor.kpresence.rpc.Packet
import io.github.vyfor.kpresence.rpc.PacketArgs
import io.github.vyfor.kpresence.utils.epochMillis
import io.github.vyfor.kpresence.utils.getProcessId
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Manages client connections and activity updates for Discord presence.
 * @property clientId The Discord application client ID.
 */
class RichClient(var clientId: Long) {
  var connectionState = ConnectionState.DISCONNECTED
    private set
  
  private val connection = Connection()
  private var lastActivity: Activity? = null
  var logger: ILogger? = null

  /**
   * Establishes a connection to Discord.
   * @param callback The callback function to be executed after establishing the connection.
   * @return The current Client instance for chaining.
   * @throws InvalidClientIdException if the provided client ID is not valid.
   * @throws ConnectionException if an error occurs while establishing the connection.
   * @throws PipeReadException if an error occurs while reading from the IPC pipe.
   * @throws PipeWriteException if an error occurs while writing to the IPC pipe.
   */
  fun connect(callback: (RichClient.() -> Unit)? = null): RichClient {
    if (connectionState != ConnectionState.DISCONNECTED) {
      logger?.warn("Already connected to Discord. Skipping")
      callback?.invoke(this)
      return this
    }

    connection.open()
    connectionState = ConnectionState.CONNECTED
    logger?.info("Successfully connected to Discord")
    handshake()

    callback?.invoke(this)

    return this
  }

  /**
   * Attempts to reconnect if there is an already active connection.
   * @return The current Client instance for chaining.
   * @throws InvalidClientIdException if the provided client ID is not valid.
   * @throws ConnectionException if an error occurs while establishing the connection.
   * @throws PipeReadException if an error occurs while reading from the IPC pipe.
   * @throws PipeWriteException if an error occurs while writing to the IPC pipe.
   */
  fun reconnect(): RichClient {
    if (connectionState != ConnectionState.SENT_HANDSHAKE) {
      throw NotConnectedException()
    }

    shutdown()
    connect()

    return this
  }

  /**
   * Updates the current activity shown on Discord.
   * Skips identical presence updates.
   * @param activity The activity to display.
   * @return The current Client instance for chaining.
   * @throws NotConnectedException if the client is not connected to Discord.
   * @throws PipeReadException if an error occurs while reading from the IPC pipe.
   * @throws PipeWriteException if an error occurs while writing to the IPC pipe.
   */
  fun update(activity: Activity?): RichClient {
    if (connectionState != ConnectionState.SENT_HANDSHAKE) {
      throw NotConnectedException()
    }

    if (lastActivity == activity) {
      logger?.info("Received identical presence update. Skipping")
      return this
    }

    lastActivity = activity
    sendActivityUpdate()

    return this
  }
  
  /**
   * Clears the current activity shown on Discord.
   * @return The current Client instance for chaining.
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
   * @return The current Client instance for chaining.
   */
  fun shutdown(): RichClient {
    if (connectionState == ConnectionState.DISCONNECTED) {
      logger?.warn("Already disconnected from Discord. Skipping")
      return this
    }
    // TODO: Send valid payload
    connection.write(2, "{\"v\": 1,\"client_id\":\"$clientId\"}")
    connection.read()
    connection.close()
    connectionState = ConnectionState.DISCONNECTED
    logger?.info("Successfully disconnected from Discord")
    lastActivity = null
    return this
  }
  
  private fun sendActivityUpdate() {
    if (connectionState != ConnectionState.SENT_HANDSHAKE) return
    val packet = Json.encodeToString(Packet("SET_ACTIVITY", PacketArgs(getProcessId(), lastActivity), "-"))
    logger?.info("Sending presence update with payload: $packet")
    connection.write(1, packet)
    connection.read()
  }
  
  private fun handshake() {
    connection.write(0, "{\"v\": 1,\"client_id\":\"$clientId\"}")
    if (connection.read().decodeToString().contains("Invalid Client ID")) {
      throw InvalidClientIdException("'$clientId' is not a valid client ID")
    }
    connectionState = ConnectionState.SENT_HANDSHAKE
    logger?.info("Performed initial handshake")
  }
}

enum class ConnectionState {
  DISCONNECTED,
  CONNECTED,
  SENT_HANDSHAKE,
}


