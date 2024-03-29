package io.github.reblast.kpresence.ipc

import io.github.reblast.kpresence.utils.putInt
import io.github.reblast.kpresence.utils.reverseBytes
import kotlinx.cinterop.*
import platform.linux.sockaddr_un
import platform.posix.*

actual fun openPipe(): Int {
  val dir =
    (getenv("XDG_RUNTIME_DIR") ?:
     getenv("TMPDIR") ?:
     getenv("TMP") ?:
     getenv("TEMP"))?.toKString() ?:
    "/tmp"
  println("Found temp dir: $dir")
  println("Creating socket... ")
  val socket = socket(AF_UNIX, SOCK_STREAM, 0)
  if (socket == -1) {
    throw RuntimeException("Failed to create socket")
  }
  print("Success")
  
  println("fcntl... ")
  fcntl(socket, F_SETFL, O_NONBLOCK)
  print("Success")
  
  memScoped {
    for (i in 0..9) {
      println("Setting pipe address...")
      val pipeAddr = alloc<sockaddr_un>().apply {
        sun_family = AF_UNIX.convert()
        println("snprintf '${dir}/discord-ipc-$i'")
        snprintf(sun_path, PATH_MAX.toULong(), "${dir}/discord-ipc-%d", i)
      }
      println("Successfully set pipe address\n")
      
      println("Connecting to '${dir}/discord-ipc-$i'... ")
      val err = connect(socket, pipeAddr.ptr.reinterpret(), sizeOf<sockaddr_un>().convert())
      if (err == 0) {
        print("Success")
        return socket
      }
      print("Failed")
    }
  }
  
  throw RuntimeException("Could not connect to the pipe!")
}

actual fun closePipe(handle: Int) {
  close(handle)
}

actual fun readBytes(handle: Int, bufferSize: Int): ByteArray {
  if (handle == -1) throw IllegalStateException("Not connected")
  
  val buffer = ByteArray(bufferSize)
  val bytesRead = recv(handle, buffer.refTo(0), bufferSize.convert(), 0)
  
  if (bytesRead.toInt() == -1) {
    throw RuntimeException("Error reading from socket")
  }
  
  return buffer.copyOf(bytesRead.toInt())
}

actual fun writeBytes(handle: Int, opcode: Int, data: String) {
  if (handle == -1) throw IllegalStateException("Not connected")
  
  println("Sending the following data to the socket:")
  println("Opcode: $opcode")
  println("Payload: $data\n")
  
  val bytes = data.encodeToByteArray()
  val buffer = ByteArray(bytes.size + 8)
  
  buffer.putInt(opcode.reverseBytes())
  buffer.putInt(bytes.size.reverseBytes(), 4)
  bytes.copyInto(buffer, 8)
  
  val bytesWritten = send(handle, buffer.refTo(0), bytes.size.convert(), MSG_NOSIGNAL)
  
  if (bytesWritten.toInt() == -1) {
    throw RuntimeException("Error writing to socket")
  } else if (bytesWritten.toInt() != bytes.size) {
    throw RuntimeException("Incomplete write to socket")
  }
}