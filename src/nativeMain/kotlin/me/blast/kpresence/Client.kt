@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package me.blast.kpresence

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.blast.kpresence.ipc.openPipe
import me.blast.kpresence.ipc.writeBytes
import me.blast.kpresence.rpc.*
import platform.posix.close
import platform.posix.getpid

class Client(val clientId: Long) {
  var handle = -1
  
  fun connect(): Client {
    handle = openPipe()
    handshake()
    return this
  }
  
  fun update(activity: Activity): Client {
    writeBytes(
      handle,
      1,
      Json.encodeToString(
        Packet(
          "SET_ACTIVITY",
          PacketArgs(
            getpid(),
            activity
          ),
          "-"
        )
      )
    )
    return this
  }
  
  fun clear(): Client {
    writeBytes(handle,
      1,
      Json.encodeToString(
        Packet(
          "SET_ACTIVITY",
          PacketArgs(
            getpid(),
            null
          ),
          "-"
        )
      )
    )
    return this
  }
  
  fun shutdown(): Client {
    clear()
    close(handle)
    handle = -1
    return this
  }
  
  private fun handshake() {
    writeBytes(handle, 0, "{\"v\": 1, \"client_id\": \"$clientId\"}")
  }
}
