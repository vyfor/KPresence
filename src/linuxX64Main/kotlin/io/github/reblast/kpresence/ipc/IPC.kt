package io.github.reblast.kpresence.ipc

import io.github.reblast.kpresence.utils.putInt
import io.github.reblast.kpresence.utils.reverseBytes
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import platform.posix.read
import platform.posix.write

@OptIn(ExperimentalForeignApi::class)
actual fun readBytes(handle: Int, bufferSize: Int): ByteArray {
  if (handle == -1) throw IllegalStateException("Not connected")
  
  val buffer = ByteArray(bufferSize)
  val bytesRead = read(handle, buffer.refTo(0), buffer.size.toULong()).toInt()
  
  return buffer.copyOf(bytesRead)
}

@OptIn(ExperimentalForeignApi::class)
actual fun writeBytes(handle: Int, opcode: Int, data: String) {
  if (handle == -1) throw IllegalStateException("Not connected")
  
  val bytes = data.encodeToByteArray()
  val buffer = ByteArray(bytes.size + 8)
  
  buffer.putInt(opcode.reverseBytes())
  buffer.putInt(bytes.size.reverseBytes(), 4)
  bytes.copyInto(buffer, 8)
  
  write(handle, buffer.refTo(0), buffer.size.toULong())
}