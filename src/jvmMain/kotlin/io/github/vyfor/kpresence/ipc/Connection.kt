package io.github.vyfor.kpresence.ipc

import io.github.vyfor.kpresence.exception.*
import io.github.vyfor.kpresence.utils.putInt
import io.github.vyfor.kpresence.utils.reverseBytes
import java.lang.System.getenv
import java.net.SocketException
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.channels.SocketChannel
import java.nio.file.InvalidPathException
import java.nio.file.StandardOpenOption
import kotlin.io.path.Path
import kotlin.io.path.exists

actual class Connection {
  private val con =
    if (System.getProperty("os.name").lowercase().startsWith("windows"))
      WindowsConnection()
    else
      UnixConnection()
  
  actual fun open() {
    con.open()
  }
  
  actual fun read(): Message? {
    return con.read()
  }
  
  actual fun write(opcode: Int, data: String?) {
    con.write(opcode, data)
  }
  
  actual fun close() {
    con.close()
  }
  
  interface IConnection {
    fun open()
    fun read(): Message?
    fun write(opcode: Int, data: String?)
    fun close()
  }
  
  internal class WindowsConnection: IConnection {
    private var pipe: AsynchronousFileChannel? = null
    
    override fun open() {
      for (i in 0..9) {
        try {
          val path = Path("\\\\.\\pipe\\discord-ipc-$i")
          if (!path.exists()) continue
          pipe = AsynchronousFileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE)
          return
        } catch (e: Exception) {
          throw ConnectionException(e)
        }
      }
      
      throw PipeNotFoundException()
    }
    
    override fun read(): Message {
      pipe?.let { stream ->
        try {
          val opcode = stream.readInt(0).reverseBytes()
          val length = stream.readInt(4).reverseBytes()
          val buffer = ByteBuffer.allocate(length)

          stream.read(buffer, 8).get()
          return Message(
            opcode,
            buffer.array()
          )
        } catch (e: Exception) {
          throw PipeReadException(e.message.orEmpty())
        }
      } ?: throw NotConnectedException()
    }
    
    override fun write(opcode: Int, data: String?) {
      pipe?.let { stream ->
        try {
          val bytes = data?.encodeToByteArray()
          val buffer = ByteArray((bytes?.size ?: 0) + 8)

          buffer.putInt(opcode.reverseBytes())
          if (bytes != null) {
            buffer.putInt(bytes.size.reverseBytes(), 4)
            bytes.copyInto(buffer, 8)
          }

          stream.write(ByteBuffer.wrap(buffer), 0).get()
        } catch (e: Exception) {
          throw PipeWriteException(e.message.orEmpty())
        }
      } ?: throw NotConnectedException()
    }
    
    override fun close() {
      pipe?.close()
      pipe = null
    }
    
    private fun AsynchronousFileChannel.readInt(offset: Long): Int {
      val buffer = ByteBuffer.allocate(4)
      
      read(buffer, offset).get()
      return ((buffer[0].toUInt() shl 24) +
        (buffer[1].toUInt() shl 16) +
        (buffer[2].toUInt() shl 8) +
        buffer[3].toUInt() shl 0).toInt()
    }
  }
  
  internal class UnixConnection: IConnection {
    private var pipe: SocketChannel? = null
    
    override fun open() {
      val dirs =
        listOf("XDG_RUNTIME_DIR", "TMPDIR", "TMP", "TEMP")
          .mapNotNull(::getenv)
          .plus("/tmp")
          .flatMap { base ->
            listOf(
              base,
              "$base/app/com.discordapp.Discord",
              "$base/snap.discord"
            )
          }
      
      dirs.forEach { dir ->
        for (i in 0..9) {
          try {
            pipe = SocketChannel.open(UnixDomainSocketAddress.of("$dir/discord-ipc-$i"))
            return
          } catch (_: InvalidPathException) {
          } catch (_: IllegalArgumentException) {
          } catch (e: SocketException) {
            if (e.message == "No such file or directory") continue
            throw ConnectionException(e)
          } catch (e: Exception) {
            throw ConnectionException(e)
          }
        }
      }
      
      throw PipeNotFoundException()
    }
    
    override fun read(): Message {
      pipe?.let { stream ->
        try {
          val opcode = stream.readInt().reverseBytes()
          val length = stream.readInt().reverseBytes()
          val buffer = ByteBuffer.allocate(length)
          
          stream.read(buffer)
          return Message(
            opcode,
            buffer.array()
          )
        } catch (e: Exception) {
          throw PipeReadException(e.message.orEmpty())
        }
      } ?: throw NotConnectedException()
    }
    
    override fun write(opcode: Int, data: String?) {
      pipe?.let { stream ->
        try {
          val bytes = data?.encodeToByteArray()
          val buffer = ByteArray((bytes?.size ?: 0) + 8)
          
          buffer.putInt(opcode.reverseBytes())
          if (bytes != null) {
            buffer.putInt(bytes.size.reverseBytes(), 4)
            bytes.copyInto(buffer, 8)
          }
          
          stream.write(ByteBuffer.wrap(buffer))
        } catch (e: Exception) {
          throw PipeWriteException(e.message.orEmpty())
        }
      } ?: throw NotConnectedException()
    }
    
    override fun close() {
      pipe?.close()
      pipe = null
    }
    
    private fun SocketChannel.readInt(): Int {
      val buffer = ByteBuffer.allocate(4)
      
      read(buffer)
      return ((buffer[0].toUInt() shl 24) +
        (buffer[1].toUInt() shl 16) +
        (buffer[2].toUInt() shl 8) +
        buffer[3].toUInt() shl 0).toInt()
    }
  }
}
