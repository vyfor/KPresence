package io.github.vyfor.kpresence.ipc

import io.github.vyfor.kpresence.exception.*
import io.github.vyfor.kpresence.utils.byteArrayToInt
import io.github.vyfor.kpresence.utils.putInt
import io.github.vyfor.kpresence.utils.reverseBytes
import kotlinx.cinterop.*
import platform.linux.sockaddr_un
import platform.posix.*

actual class Connection {
  private var pipe = -1
  
  actual fun open() {
    val dir =
      (getenv("XDG_RUNTIME_DIR") ?:
       getenv("TMPDIR") ?:
       getenv("TMP") ?:
       getenv("TEMP"))?.toKString() ?:
      "/tmp"
    
    val socket = socket(AF_UNIX, SOCK_STREAM, 0)
    if (socket == -1) throw ConnectionException(Exception(strerror(errno)?.toKString().orEmpty()))
    
    memScoped {
      for (i in 0..9) {
        val pipeAddr = alloc<sockaddr_un>().apply {
          sun_family = AF_UNIX.convert()
          snprintf(sun_path, PATH_MAX.toULong(), "$dir/discord-ipc-$i")
        }
        
        val err = connect(socket, pipeAddr.ptr.reinterpret(), sizeOf<sockaddr_un>().convert())
        if (err == 0) {
          pipe = socket
          return
        } else if (errno != ENOENT) {
          throw ConnectionException(Exception(strerror(errno)?.toKString().orEmpty()))
        }
      }
    }
    
    close(socket)
    throw PipeNotFoundException()
  }
  
  actual fun read(): ByteArray {
    if (pipe == -1) throw NotConnectedException()
    
    readBytes(4)
    val length = readBytes(4).first.byteArrayToInt().reverseBytes()
    val buffer = ByteArray(length)
    val bytesRead = readBytes(4).second
    
    return buffer.copyOf(bytesRead.toInt())
  }
  
  actual fun write(opcode: Int, data: String) {
    if (pipe == -1) throw NotConnectedException()
    
    val bytes = data.encodeToByteArray()
    val buffer = ByteArray(bytes.size + 8)
    
    buffer.putInt(opcode.reverseBytes())
    buffer.putInt(bytes.size.reverseBytes(), 4)
    bytes.copyInto(buffer, 8)
    
    val bytesWritten = send(pipe, buffer.refTo(0), buffer.size.convert(), 0).toInt()
    if (bytesWritten < 0) {
      close()
      
      throw PipeWriteException(strerror(errno)?.toKString().orEmpty())
    }
  }
  
  actual fun close() {
    shutdown(pipe, SHUT_RDWR)
    close(pipe)
    pipe = -1
  }
  
  private fun readBytes(size: Int): Pair<ByteArray, Long> {
    val bytes = ByteArray(size)
    recv(pipe, bytes.refTo(0), bytes.size.convert(), 0).let { bytesRead ->
      if (bytesRead < 0L) {
        if (errno == EAGAIN) return bytes to 0
        throw PipeReadException(strerror(errno)?.toKString().orEmpty())
      } else if (bytesRead == 0L) {
        close()
        throw ConnectionClosedException(strerror(errno)?.toKString().orEmpty())
      }
      return bytes to bytesRead
    }
  }
}