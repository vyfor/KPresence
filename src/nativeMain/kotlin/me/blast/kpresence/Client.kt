@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package me.blast.kpresence

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.blast.kpresence.ipc.openPipe
import me.blast.kpresence.ipc.writeBytes
import me.blast.kpresence.rpc.*
import platform.posix.close
import platform.posix.getpid

/**
 * Manages client connections and activity updates for Discord presence.
 * @property clientId The Discord application client ID.
 */
class Client(val clientId: Long) {
  var handle = -1
    private set
  
  /**
   * Establishes a connection to Discord.
   * Attempts to reconnect if there is an already active connection.
   * @return The current Client instance for chaining.
   */
  fun connect(): Client {
    if (handle != -1) close(handle)
    handle = openPipe()
    handshake()
    return this
  }
  
  /**
   * Updates the current activity shown on Discord.
   * @param activity The activity to display.
   * @return The current Client instance for chaining.
   */
  fun update(activity: Activity): Client {
    val packet = Json.encodeToString(Packet("SET_ACTIVITY", PacketArgs(getpid(), activity), "-"))
    writeBytes(handle, 1, packet)
    return this
  }
  
  /**
   * Clears the current activity shown on Discord.
   * @return The current Client instance for chaining.
   */
  fun clear(): Client {
    val packet = Json.encodeToString(Packet("SET_ACTIVITY", PacketArgs(getpid(), null), "-"))
    writeBytes(handle, 1, packet)
    return this
  }
  
  /**
   * Shuts down the connection to Discord and cleans up resources.
   * @return The current Client instance for chaining.
   */
  fun shutdown(): Client {
    clear()
    close(handle)
    handle = -1
    return this
  }
  
  /**
   * Performs the initial handshake with Discord.
   */
  private fun handshake() {
    writeBytes(handle, 0, "{\"v\": 1, \"client_id\": \"$clientId\"}")
  }
}
