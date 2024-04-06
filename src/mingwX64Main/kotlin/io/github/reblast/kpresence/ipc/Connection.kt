package io.github.reblast.kpresence.ipc

import io.github.reblast.kpresence.utils.putInt
import io.github.reblast.kpresence.utils.reverseBytes
import kotlinx.cinterop.*
import platform.windows.*

actual class Connection {
  private var pipe: HANDLE? = null
  
  actual fun open() {
    for (i in 0..9) {
      val pipeHandle = CreateFileW("\\\\.\\pipe\\discord-ipc-$i", GENERIC_READ or GENERIC_WRITE.convert(), 0u, null, OPEN_EXISTING.convert(), 0u, null)
      
      if (pipeHandle == INVALID_HANDLE_VALUE) continue
      else {
        pipe = pipeHandle
        return
      }
    }
    
    throw RuntimeException("Could not connect to the pipe!")
  }
  
  actual fun read(bufferSize: Int): ByteArray = memScoped {
    pipe?.let { handle ->
      val buffer = ByteArray(bufferSize)
      val bytesRead = alloc<UIntVar>()
      
      val success = buffer.usePinned {
        ReadFile(handle, it.addressOf(0), bufferSize.convert(), bytesRead.ptr, null)
      }
      
      if (success == FALSE) throw RuntimeException("Error reading from socket")
      
      return buffer.copyOf(bytesRead.value.toInt())
    } ?: throw IllegalStateException("Not connected")
  }
  
  actual fun write(opcode: Int, data: String) {
    pipe?.let { handle ->
      val bytes = data.encodeToByteArray()
      val buffer = ByteArray(bytes.size + 8)
      
      buffer.putInt(opcode.reverseBytes())
      buffer.putInt(bytes.size.reverseBytes(), 4)
      bytes.copyInto(buffer, 8)
      
      val success = buffer.usePinned {
        WriteFile(handle, it.addressOf(0), buffer.size.convert(), null, null)
      }
      
      if (success == FALSE) throw RuntimeException("Error reading from socket")
    } ?: throw IllegalStateException("Not connected")
  }
  
  actual fun close() {
    CloseHandle(pipe)
  }
}