package io.github.reblast.kpresence.ipc

import io.github.reblast.kpresence.utils.putInt
import io.github.reblast.kpresence.utils.reverseBytes
import kotlinx.cinterop.*
import platform.posix.*

actual fun openPipe(): Int {
  for (i in 0..9) {
    val pipe = open("\\\\.\\pipe\\discord-ipc-$i", O_RDWR)
    
    if (pipe == -1) continue
    else return pipe
  }
  
  throw RuntimeException("Could not connect to the pipe!")
}

actual fun closePipe(pipe: Int) {
  close(pipe)
}

actual fun readBytes(pipe: Int, bufferSize: Int): ByteArray {
  if (pipe == -1) throw IllegalStateException("Not connected")
  
  val buffer = ByteArray(bufferSize)
  val bytesRead = read(pipe, buffer.refTo(0), buffer.size.toUInt())
  
  return buffer.copyOf(bytesRead)
}

actual fun writeBytes(pipe: Int, opcode: Int, data: String) {
  if (pipe == -1) throw IllegalStateException("Not connected")

  val bytes = data.encodeToByteArray()
  val buffer = ByteArray(bytes.size + 8)

  buffer.putInt(opcode.reverseBytes())
  buffer.putInt(bytes.size.reverseBytes(), 4)
  bytes.copyInto(buffer, 8)

  write(pipe, buffer.refTo(0), buffer.size.toUInt())
}