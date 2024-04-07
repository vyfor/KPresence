package io.github.vyfor.kpresence.ipc

import io.github.vyfor.kpresence.exception.*
import io.github.vyfor.kpresence.utils.putInt
import io.github.vyfor.kpresence.utils.reverseBytes
import java.io.EOFException
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.lang.IllegalArgumentException
import java.lang.System.getenv
import java.net.UnixDomainSocketAddress
import java.nio.channels.Channels
import java.nio.channels.SocketChannel
import java.nio.file.InvalidPathException

actual class Connection {
  private val con =
    if (System.getProperty("os.name").lowercase().startsWith("windows"))
      WindowsConnection()
    else
      UnixConnection()
  
  actual fun open() {
    con.open()
  }
  
  actual fun read(): ByteArray {
    return con.read()
  }
  
  actual fun write(opcode: Int, data: String) {
    con.write(opcode, data)
  }
  
  actual fun close() {
    con.close()
  }
  
  interface IConnection {
    fun open()
    fun read(): ByteArray
    fun write(opcode: Int, data: String)
    fun close()
  }
  
  internal class WindowsConnection: IConnection {
    private var pipe: RandomAccessFile? = null
    
    override fun open() {
      for (i in 0..9) {
        try {
          pipe = RandomAccessFile("\\\\.\\pipe\\discord-ipc-$i", "rw")
          return
        } catch (_: FileNotFoundException) {
        } catch (e: Exception) {
          throw ConnectionException(e)
        }
      }
      
      throw PipeNotFoundException()
    }
    
    override fun read(): ByteArray {
      pipe?.let { stream ->
        try {
          stream.readInt()
          val length = stream.readInt().reverseBytes()
          val buffer = ByteArray(length)
          
          stream.read(buffer, 0, length)
          return buffer
        } catch (e: Exception) {
          throw PipeReadException(e.message.orEmpty())
        }
      } ?: throw NotConnectedException()
    }
    
    override fun write(opcode: Int, data: String) {
      pipe?.let { stream ->
        try {
          val bytes = data.encodeToByteArray()
          val buffer = ByteArray(bytes.size + 8)
          
          buffer.putInt(opcode.reverseBytes())
          buffer.putInt(bytes.size.reverseBytes(), 4)
          bytes.copyInto(buffer, 8)
          
          stream.write(buffer)
        } catch (e: Exception) {
          throw PipeWriteException(e.message.orEmpty())
        }
      } ?: throw NotConnectedException()
    }
    
    override fun close() {
      pipe?.close()
    }
  }
  
  internal class UnixConnection: IConnection {
    private var pipe: SocketChannel? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    
    override fun open() {
      val dir =
        (getenv("XDG_RUNTIME_DIR") ?:
         getenv("TMPDIR") ?:
         getenv("TMP") ?:
         getenv("TEMP")) ?:
        "/tmp"
      
      for (i in 0..9) {
        try {
          pipe = SocketChannel.open(UnixDomainSocketAddress.of("$dir/discord-ipc-$i"))
          inputStream = Channels.newInputStream(pipe!!)
          outputStream = Channels.newOutputStream(pipe!!)
          return
        } catch (_: InvalidPathException) {
        } catch (_: IllegalArgumentException) {
        } catch (e: Exception) {
          throw ConnectionException(e)
        }
      }
      
      throw PipeNotFoundException()
    }
    
    override fun read(): ByteArray {
      inputStream?.let { stream ->
        try {
          stream.readInt()
          val length = stream.readInt()
          val buffer = ByteArray(length)
          
          stream.read(buffer, 0, length)
          return buffer
        } catch (e: Exception) {
          throw PipeReadException(e.message.orEmpty())
        }
      } ?: throw NotConnectedException()
    }
    
    override fun write(opcode: Int, data: String) {
      outputStream?.let { stream ->
        try {
          val bytes = data.encodeToByteArray()
          val buffer = ByteArray(bytes.size + 8)
          
          buffer.putInt(opcode.reverseBytes())
          buffer.putInt(bytes.size.reverseBytes(), 4)
          bytes.copyInto(buffer, 8)
          
          stream.write(buffer)
        } catch (e: Exception) {
          throw PipeWriteException(e.message.orEmpty())
        }
      } ?: throw NotConnectedException()
    }
    
    override fun close() {
      pipe?.close()
      inputStream?.close()
      outputStream?.close()
      pipe = null
    }
    
    private fun InputStream.readInt(): Int {
      val ch1: Int = read()
      val ch2: Int = read()
      val ch3: Int = read()
      val ch4: Int = read()
      return if (ch1 or ch2 or ch3 or ch4 < 0) {
        throw EOFException()
      } else {
        (ch1 shl 24) + (ch2 shl 16) + (ch3 shl 8) + (ch4 shl 0)
      }
    }
  }
}