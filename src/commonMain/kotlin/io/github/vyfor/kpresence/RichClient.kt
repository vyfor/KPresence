@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package io.github.vyfor.kpresence

import io.github.vyfor.kpresence.exception.InvalidClientIdException
import io.github.vyfor.kpresence.ipc.*
import io.github.vyfor.kpresence.rpc.Activity
import io.github.vyfor.kpresence.rpc.Packet
import io.github.vyfor.kpresence.rpc.PacketArgs
import io.github.vyfor.kpresence.utils.epochMillis
import io.github.vyfor.kpresence.utils.getProcessId
import kotlinx.coroutines.*
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
  private val clientScope = CoroutineScope(Dispatchers.IO)
  private var lastActivity: Activity? = null
  private var lastUpdated = 0L
  private var updateTimer: Job? = null

  /**
   * Establishes a connection to Discord.
   * @param callback The callback function to be executed after establishing the connection.
   * @return The current Client instance for chaining.
   */
  fun connect(callback: (RichClient.() -> Unit)? = null): RichClient {
    if (connectionState != ConnectionState.DISCONNECTED) {
      callback?.invoke(this)
      return this
    }

    connection.open()
    connectionState = ConnectionState.CONNECTED
    handshake()

    callback?.invoke(this)

    return this
  }

  /**
   * Attempts to reconnect if there is an already active connection.
   * @return The current Client instance for chaining.
   */
  fun reconnect(): RichClient {
    require(connectionState != ConnectionState.DISCONNECTED) { "Reconnection is not possible while disconnected." }

    shutdown()
    connect()

    return this
  }

  /**
   * Updates the current activity shown on Discord.
   * Skips identical presence updates.
   * @param activity The activity to display.
   * @return The current Client instance for chaining.
   */
  fun update(activity: Activity?): RichClient {
    require(connectionState == ConnectionState.SENT_HANDSHAKE) { "Presence updates are not allowed while disconnected." }
    if (lastActivity == activity) return this
    lastActivity = activity
    val currentTime = epochMillis()
    val timeSinceLastUpdate = currentTime - lastUpdated

    if (timeSinceLastUpdate >= UPDATE_INTERVAL) {
      sendActivityUpdate()
      lastUpdated = currentTime
    } else if (updateTimer?.isActive != true) {
      updateTimer = clientScope.launch {
        delay(UPDATE_INTERVAL - timeSinceLastUpdate)
        sendActivityUpdate()
        lastUpdated = epochMillis()
      }
    }
    return this
  }
  
  /**
   * Clears the current activity shown on Discord.
   * @return The current Client instance for chaining.
   */
  fun clear(): RichClient {
    require(connectionState == ConnectionState.SENT_HANDSHAKE) { "Cannot clear presence while disconnected." }
    update(null)
    return this
  }
  
  /**
   * Shuts down the connection to Discord and cleans up resources.
   * @return The current Client instance for chaining.
   */
  fun shutdown(): RichClient {
    if (connectionState == ConnectionState.DISCONNECTED) return this
    // TODO: Send valid payload
    connection.write(2, "{\"v\": 1,\"client_id\":\"$clientId\"}")
    connection.read()
    connection.close()
    connectionState = ConnectionState.DISCONNECTED
    updateTimer?.cancel()
    updateTimer = null
    lastActivity = null
    lastUpdated = 0
    return this
  }
  
  private fun sendActivityUpdate() {
    if (connectionState != ConnectionState.SENT_HANDSHAKE) return
    val packet = Json.encodeToString(Packet("SET_ACTIVITY", PacketArgs(getProcessId(), lastActivity), "-"))
    connection.write(1, packet)
    connection.read()
  }
  
  private fun handshake() {
    connection.write(0, "{\"v\": 1,\"client_id\":\"$clientId\"}")
    if (connection.read().decodeToString().contains("Invalid client ID")) {
      throw InvalidClientIdException("'$clientId' is not a valid client ID")
    }
    connectionState = ConnectionState.SENT_HANDSHAKE
  }

  companion object {
    private const val UPDATE_INTERVAL = 15000L
  }
}

enum class ConnectionState {
  DISCONNECTED,
  CONNECTED,
  SENT_HANDSHAKE,
}


