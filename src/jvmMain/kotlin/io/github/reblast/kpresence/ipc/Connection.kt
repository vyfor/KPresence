package io.github.reblast.kpresence.ipc

import io.github.reblast.kpresence.utils.reverseBytes
import java.io.FileNotFoundException
import java.io.RandomAccessFile
import java.lang.System.getenv
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.SocketChannel

actual class Connection {
  private val con =
    if (System.getProperty("os.name").lowercase().startsWith("windows"))
      WindowsConnection()
    else
      UnixConnection()
  
  actual fun open() {
    con.open()
  }
  
  actual fun read(bufferSize: Int): ByteArray {
    return con.read(bufferSize)
  }
  
  actual fun write(opcode: Int, data: String) {
    con.write(opcode, data)
  }
  
  actual fun close() {
    con.close()
  }
  
  interface IConnection {
    fun open()
    fun read(bufferSize: Int): ByteArray
    fun write(opcode: Int, data: String)
    fun close()
  }
  
  internal class WindowsConnection(): IConnection {
    private var pipe: FileChannel? = null
    
    override fun open() {
      for (i in 0..9) {
        try {
          pipe = RandomAccessFile("\\\\.\\pipe\\discord-ipc-$i", "rw").channel
          return
        } catch (_: FileNotFoundException) {}
      }
      
      throw RuntimeException("Could not connect to the pipe!")
    }
    
    override fun read(bufferSize: Int): ByteArray {
      pipe?.run {
        val buffer = ByteBuffer.allocate(bufferSize)
        
        pipe!!.read(buffer)
        return ByteArray(buffer.remaining())
      } ?: throw IllegalStateException("Not connected")
    }
    
    override fun write(opcode: Int, data: String) {
      pipe?.apply {
        val bytes = data.encodeToByteArray()
        val buffer = ByteBuffer.allocate(bytes.size + 8)
        
        buffer.putInt(opcode.reverseBytes())
        buffer.putInt(4, bytes.size.reverseBytes())
        buffer.position(8)
        buffer.put(bytes)
        
        val bytesWritten = write(buffer)
        if (bytesWritten < 0) {
          throw RuntimeException("Error writing to socket")
        }
      } ?: throw IllegalStateException("Not connected")
    }
    
    override fun close() {
      pipe?.close()
    }
  }
  
  internal class UnixConnection(): IConnection {
    private var pipe: SocketChannel? = null
    
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
          return
        } catch (_: FileNotFoundException) {}
      }
      
      throw RuntimeException("Could not connect to the pipe!")
    }
    
    override fun read(bufferSize: Int): ByteArray {
      pipe?.run {
        val buffer = ByteBuffer.allocate(bufferSize)
        
        pipe!!.read(buffer)
        return ByteArray(buffer.remaining())
      } ?: throw IllegalStateException("Not connected")
    }
    
    override fun write(opcode: Int, data: String) {
      pipe?.apply {
        val bytes = data.encodeToByteArray()
        val buffer = ByteBuffer.allocate(bytes.size + 8)
        
        buffer.putInt(opcode.reverseBytes())
        buffer.putInt(4, bytes.size.reverseBytes())
        buffer.position(8)
        buffer.put(bytes)
        
        val bytesWritten = write(buffer)
        if (bytesWritten < 0) {
          throw RuntimeException("Error writing to socket")
        }
      } ?: throw IllegalStateException("Not connected")
    }
    
    override fun close() {
      pipe?.close()
    }
  }
}