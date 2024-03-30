package io.github.reblast.kpresence.ipc

import io.github.reblast.kpresence.utils.putInt
import io.github.reblast.kpresence.utils.reverseBytes
import kotlinx.cinterop.*
import platform.osx.sockaddr_un
import platform.posix.*

actual fun openPipe(): Int {
  val dir =
    (getenv("XDG_RUNTIME_DIR") ?:
     getenv("TMPDIR") ?:
     getenv("TMP") ?:
     getenv("TEMP"))?.toKString() ?:
    "/tmp"
  
  val socket = socket(AF_UNIX, SOCK_STREAM, 0)
  if (socket == -1) throw RuntimeException("Failed to create socket")
  
  memScoped {
    for (i in 0..9) {
      val pipeAddr = alloc<sockaddr_un>().apply {
        sun_family = AF_UNIX.convert()
        snprintf(sun_path, PATH_MAX.toULong(), "${dir}/discord-ipc-$i")
      }
      
      val err = connect(socket, pipeAddr.ptr.reinterpret(), sizeOf<sockaddr_un>().convert())
      if (err == 0) return socket
    }
  }
  
  close(socket)
  throw RuntimeException("Could not connect to the pipe!")
}

actual fun closePipe(pipe: Int) {
  close(pipe)
}

actual fun readBytes(pipe: Int, bufferSize: Int): ByteArray {
  if (pipe == -1) throw IllegalStateException("Not connected")
  
  val buffer = ByteArray(bufferSize)
  
  val bytesRead = recv(pipe, buffer.refTo(0), bufferSize.convert(), 0).toInt()
  if (bytesRead < 0) {
    if (errno == EAGAIN) return buffer
    
    throw RuntimeException("Error reading from socket")
  } else if (bytesRead == 0) {
    close(pipe)
    
    throw RuntimeException("Connection closed")
  }
  
  return buffer.copyOf(bytesRead)
}

actual fun writeBytes(pipe: Int, opcode: Int, data: String) {
  if (pipe == -1) throw IllegalStateException("Not connected")
  
  val bytes = data.encodeToByteArray()
  val buffer = ByteArray(bytes.size + 8)
  
  buffer.putInt(opcode.reverseBytes())
  buffer.putInt(bytes.size.reverseBytes(), 4)
  bytes.copyInto(buffer, 8)
  
  val bytesWritten = send(pipe, buffer.refTo(0), buffer.size.convert(), 0).toInt()
  if (bytesWritten < 0) {
    close(pipe)
    
    throw RuntimeException("Error writing to socket")
  }
}