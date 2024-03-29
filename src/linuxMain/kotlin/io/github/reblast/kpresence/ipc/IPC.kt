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
      print("Successfully set pipe address")
      
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
  val bytesRead = read(handle, buffer.refTo(0), buffer.size.toULong()).toInt()
  
  return buffer.copyOf(bytesRead)
}

actual fun writeBytes(handle: Int, opcode: Int, data: String) {
  if (handle == -1) throw IllegalStateException("Not connected")
  
  val bytes = data.encodeToByteArray()
  val buffer = ByteArray(bytes.size + 8)
  
  buffer.putInt(opcode.reverseBytes())
  buffer.putInt(bytes.size.reverseBytes(), 4)
  bytes.copyInto(buffer, 8)
  
  write(handle, buffer.refTo(0), buffer.size.toULong())
}