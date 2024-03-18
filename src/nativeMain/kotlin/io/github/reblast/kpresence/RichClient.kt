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

/**
 * Manages client connections and activity updates for Discord presence.
 * @property clientId The Discord application client ID.
 */
class RichClient(val clientId: Long) {
  var handle = -1
    private set
  
  private val clientScope = CoroutineScope(Dispatchers.IO)
  private val updateInterval = 4000L
  private var lastActivity: Activity? = null
  private var lastUpdated = 0L
  private var updateTimer: Job? = null

  /**
   * Establishes a connection to Discord.
   * Attempts to reconnect if there is an already active connection.
   * @param callback The callback function to be executed after establishing the connection.
   * @return The current Client instance for chaining.
   */
  fun connect(callback: (RichClient.() -> Unit)? = null): RichClient {
    if (handle != -1) close(handle)
    handle = openPipe()
    handshake()
    callback?.invoke(this)
    return this
  }
  
  /**
   * Updates the current activity shown on Discord.
   * @param activity The activity to display.
   * @return The current Client instance for chaining.
   */
  fun update(activity: Activity): RichClient {
    lastActivity = activity
    val currentTime = epochMillis()
    val timeSinceLastUpdate = currentTime - lastUpdated

    if (timeSinceLastUpdate >= updateInterval) {
      sendActivityUpdate()
      lastUpdated = currentTime
    } else if (updateTimer?.isActive != true) {
      updateTimer = clientScope.launch {
        delay(updateInterval - timeSinceLastUpdate)
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
    val packet = Json.encodeToString(Packet("SET_ACTIVITY", PacketArgs(getpid(), null), "-"))
    writeBytes(handle, 1, packet)
    readBytes(handle)
    return this
  }
  
  /**
   * Shuts down the connection to Discord and cleans up resources.
   * @return The current Client instance for chaining.
   */
  fun shutdown(): RichClient {
    // TODO: Send valid payload
    writeBytes(handle, 2, "")
    close(handle)
    handle = -1
    updateTimer?.cancel()
    updateTimer = null
    lastActivity = null
    return this
  }
  
  private fun sendActivityUpdate() {
    val packet = Json.encodeToString(Packet("SET_ACTIVITY", PacketArgs(getpid(), lastActivity), "-"))
    writeBytes(handle, 1, packet)
  }
  
  /**
   * Performs the initial handshake with Discord.
   */
  private fun handshake() {
    writeBytes(handle, 0, "{\"v\": 1,\"client_id\":\"$clientId\"}")
    readBytes(handle)
  }
}
