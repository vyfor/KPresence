@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package me.blast.kpresence

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.blast.kpresence.ipc.openPipe
import me.blast.kpresence.ipc.writeBytes
import me.blast.kpresence.rpc.*
import kotlinx.cinterop.*
import platform.posix.*

/**
 * Manages client connections and activity updates for Discord presence.
 * @property clientId The Discord application client ID.
 */
class Client(val clientId: Long) {
  var handle = -1
    private set
  
  private var onMessageCallback: (Client.(ByteArray) -> Unit)? = null
  private var messageListener: Job? = null
  
  /**
   * Establishes a connection to Discord.
   * Attempts to reconnect if there is an already active connection.
   * @param callback The callback function to be executed after establishing the connection.
   * @return The current Client instance for chaining.
   */
  fun connect(callback: (Client.() -> Unit)? = null): Client {
    if (handle != -1) close(handle)
    handle = openPipe()
    listen()
    handshake()
    callback?.invoke(this)
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
    stopListening()
    close(handle)
    handle = -1
    return this
  }

  /**
   * Sets a callback to be executed when a message is received.
   * @param callback The code to be executed.
   */
  fun onMessage(callback: Client.(message: ByteArray) -> Unit): Client {
    onMessageCallback = callback
    return this
  }

  @OptIn(ExperimentalForeignApi::class)
  private fun listen() {
    val flow = flow {
      val buffer = ByteArray(2048)
      val bufferSize = buffer.size.toUInt()
      while (true) {
        val bytesRead = read(handle, buffer.refTo(0), bufferSize)
        if (bytesRead == -1) {
          perror("Error reading from pipe")
          break
        } else if (bytesRead == 0) {
          continue
        } else {
          emit(buffer.copyOf(bytesRead))
        }
      }
    }

    messageListener = GlobalScope.launch {
      flow.collect { message ->
        onMessageCallback?.invoke(this@Client, message)
      }
    }
  }

  private fun stopListening() {
    messageListener?.cancel()
    messageListener = null
  }
  
  /**
   * Performs the initial handshake with Discord.
   */
  private fun handshake() {
    writeBytes(handle, 0, "{\"v\": 1, \"client_id\": \"$clientId\"}")
  }
}
