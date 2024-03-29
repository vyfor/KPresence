@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package io.github.reblast.kpresence

import io.github.reblast.kpresence.ipc.openPipe
import io.github.reblast.kpresence.ipc.readBytes
import io.github.reblast.kpresence.ipc.writeBytes
import io.github.reblast.kpresence.rpc.Activity
import io.github.reblast.kpresence.rpc.Packet
import io.github.reblast.kpresence.rpc.PacketArgs
import io.github.reblast.kpresence.utils.epochMillis
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.posix.close
import platform.posix.getpid
import kotlin.io.print

/**
 * Manages client connections and activity updates for Discord presence.
 * @property clientId The Discord application client ID.
 */
class RichClient(val clientId: Long) {
  var state = State.DISCONNECTED
  var handle = -1
    private set
  
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
    if (state != State.DISCONNECTED) {
      callback?.invoke(this)
      return this
    }

    handle = openPipe()
    state = State.CONNECTED
    handshake()

    callback?.invoke(this)

    return this
  }

  /**
   * Attempts to reconnect if there is an already active connection.
   * @return The current Client instance for chaining.
   */
  fun reconnect(): RichClient {
    require(state != State.DISCONNECTED) { "Reconnection is not possible while disconnected." }

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
  @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
  fun update(activity: Activity?): RichClient {
    require(state == State.SENT_HANDSHAKE) { "Presence updates are not allowed while disconnected." }
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
    require(state == State.SENT_HANDSHAKE) { "Cannot clear presence while disconnected." }
    update(null)
    return this
  }
  
  /**
   * Shuts down the connection to Discord and cleans up resources.
   * @return The current Client instance for chaining.
   */
  fun shutdown(): RichClient {
    if (state == State.DISCONNECTED) return this
    // TODO: Send valid payload
    writeBytes(handle, 2, "[\"close_reason\"]")
    readBytes(handle)
    close(handle)
    state = State.DISCONNECTED
    handle = -1
    updateTimer?.cancel()
    updateTimer = null
    lastActivity = null
    lastUpdated = 0
    return this
  }
  
  private fun sendActivityUpdate() {
    if (state != State.SENT_HANDSHAKE) return
    val packet = Json.encodeToString(Packet("SET_ACTIVITY", PacketArgs(getpid(), lastActivity), "-"))
    writeBytes(handle, 1, packet)
    readBytes(handle)
  }
  
  private fun handshake() {
    writeBytes(handle, 0, "{\"v\": 1,\"client_id\":\"$clientId\"}")
    if (readBytes(handle).decodeToString().contains("Invalid client ID")) {
      throw RuntimeException("Provided invalid client ID: $clientId")
    }
    state = State.SENT_HANDSHAKE
  }

  companion object {
    private const val UPDATE_INTERVAL = 15000L
  }
}

enum class State {
  DISCONNECTED,
  CONNECTED,
  SENT_HANDSHAKE,
}


