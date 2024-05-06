package io.github.vyfor.kpresence.ipc

import io.github.vyfor.kpresence.exception.*
import io.github.vyfor.kpresence.utils.byteArrayToInt
import io.github.vyfor.kpresence.utils.putInt
import io.github.vyfor.kpresence.utils.reverseBytes
import kotlinx.cinterop.*
import platform.osx.sockaddr_un
import platform.posix.*

actual class Connection {
  private var pipe = -1
  
  actual fun open() {
    val dirs =
      listOf("XDG_RUNTIME_DIR", "TMPDIR", "TMP", "TEMP")
        .mapNotNull { getenv(it)?.toKString() }
        .plus("/tmp")
        .flatMap { base ->
          listOf(
            base,
            "$base/app/com.discordapp.Discord",
            "$base/snap.discord"
          )
        }
    
    val socket = socket(AF_UNIX, SOCK_STREAM, 0)
    if (socket == -1) throw ConnectionException(Exception(strerror(errno)?.toKString().orEmpty()))
    
    val flags = fcntl(socket, F_GETFL, 0)
    if (flags == -1) throw ConnectionException(Exception(strerror(errno)?.toKString().orEmpty()))
    if (fcntl(socket, F_SETFL, flags or O_NONBLOCK) == -1) throw ConnectionException(Exception(strerror(errno)?.toKString().orEmpty()))
    
    memScoped {
      dirs.forEach { dir ->
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
    }
    
    close(socket)
    throw PipeNotFoundException()
  }
  
  actual fun read(): Message? {
    if (pipe == -1) throw NotConnectedException()
    
    val opcode = (readBytes(4) ?: return null).first.byteArrayToInt().reverseBytes()
    val length = (readBytes(4) ?: return null).first.byteArrayToInt().reverseBytes()
    val (buffer) = readBytes(length) ?: return null
    
    return Message(
      opcode,
      buffer
    )
  }
  
  actual fun write(opcode: Int, data: String?) {
    if (pipe == -1) throw NotConnectedException()
    
    val bytes = data?.encodeToByteArray()
    val buffer = ByteArray((bytes?.size ?: 0) + 8)
    
    buffer.putInt(opcode.reverseBytes())
    if (bytes != null) {
      buffer.putInt(bytes.size.reverseBytes(), 4)
      bytes.copyInto(buffer, 8)
    }
    
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
  
  private fun readBytes(size: Int): Pair<ByteArray, Long>? {
    val bytes = ByteArray(size)
    recv(pipe, bytes.refTo(0), bytes.size.convert(), MSG_DONTWAIT).let { bytesRead ->
      if (bytesRead < 0L) {
        if (errno == EAGAIN || errno == EWOULDBLOCK) return null
        throw PipeReadException(strerror(errno)?.toKString().orEmpty())
      } else if (bytesRead == 0L) {
        close()
        throw ConnectionClosedException(strerror(errno)?.toKString().orEmpty())
      }
      return bytes to bytesRead
    }
  }
}