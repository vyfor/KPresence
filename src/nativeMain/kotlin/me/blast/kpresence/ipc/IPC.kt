@file:Suppress("unused")

package me.blast.kpresence.ipc

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import me.blast.kpresence.putInt
import me.blast.kpresence.reverseBytes
import platform.posix.*
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
fun openPipe(): Int {
  if (Platform.osFamily == OsFamily.WINDOWS) {
    for (i in 0..9) {
      val handle = open("\\\\.\\pipe\\discord-ipc-$i", O_RDWR)
      
      if (handle == -1) continue
      else return handle
    }
  } else {
    val dir =
      getenv("XDG_RUNTIME_DIR") ?:
      getenv("TMPDIR") ?:
      getenv("TMP") ?:
      getenv("TEMP") ?:
      "/tmp"
    
    for (i in 0..9) {
      val handle = open("${dir}/discord-ipc-$i", O_RDWR)
      
      if (handle == -1) continue
      else return handle
    }
  }
  
  throw RuntimeException("Could not connect to the pipe!")
}

fun closePipe(handle: Int) {
  close(handle)
}

@OptIn(ExperimentalForeignApi::class)
fun writeBytes(handle: Int, opcode: Int, data: String): Int {
  if (handle == -1) throw IllegalStateException("Not connected")
  
  val bytes = data.encodeToByteArray()
  val buffer = ByteArray(bytes.size + 8)
  
  buffer.putInt(opcode.reverseBytes())
  buffer.putInt(bytes.size.reverseBytes(), 4)
  bytes.copyInto(buffer, 8)
  
  return write(handle, buffer.refTo(0), buffer.size.toUInt())
}

@OptIn(ExperimentalForeignApi::class)
fun readBytes(handle: Int, bufferSize: Int = 4096): ByteArray {
  if (handle == -1) throw IllegalStateException("Not connected")
  
  val buffer = ByteArray(bufferSize)
  read(handle, buffer.refTo(0), buffer.size.toUInt())
  
  return buffer
}
