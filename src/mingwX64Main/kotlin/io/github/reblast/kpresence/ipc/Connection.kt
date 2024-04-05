package io.github.reblast.kpresence.ipc

import io.github.reblast.kpresence.utils.putInt
import io.github.reblast.kpresence.utils.reverseBytes
import kotlinx.cinterop.*
import platform.posix.*

actual class Connection actual constructor(pipePath: String?) {
  private var pipe = -1
  
  actual fun open() {
    for (i in 0..9) {
      val pipeHandle = open("\\\\.\\pipe\\discord-ipc-$i", O_RDWR)
      
      if (pipeHandle == -1) continue
      else {
        pipe = pipeHandle
        return
      }
    }
    
    throw RuntimeException("Could not connect to the pipe!")
  }
  
  actual fun read(bufferSize: Int): ByteArray {
    if (pipe == -1) throw IllegalStateException("Not connected")
    
    val buffer = ByteArray(bufferSize)
    val bytesRead = read(pipe, buffer.refTo(0), buffer.size.convert())
    
    return buffer.copyOf(bytesRead)
  }
  
  actual fun write(opcode: Int, data: String) {
    if (pipe == -1) throw IllegalStateException("Not connected")
    
    val bytes = data.encodeToByteArray()
    val buffer = ByteArray(bytes.size + 8)
    
    buffer.putInt(opcode.reverseBytes())
    buffer.putInt(bytes.size.reverseBytes(), 4)
    bytes.copyInto(buffer, 8)
    
    write(pipe, buffer.refTo(0), buffer.size.toUInt())
  }
  
  actual fun close() {
    close(pipe)
  }
}