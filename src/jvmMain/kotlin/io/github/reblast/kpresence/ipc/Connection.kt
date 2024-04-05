package io.github.reblast.kpresence.ipc

import io.github.reblast.kpresence.utils.putInt
import io.github.reblast.kpresence.utils.reverseBytes
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.lang.System.getenv
import java.net.Proxy
import java.net.Socket
import java.net.UnixDomainSocketAddress

actual class Connection actual constructor(pipePath: String?) {
  private val con =
    if (System.getProperty("os.name").lowercase().startsWith("windows"))
      WindowsConnection()
    else
      UnixConnection(pipePath)
  
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
  
  internal class WindowsConnection: IConnection {
    private var pipe: RandomAccessFile? = null
    
    override fun open() {
      for (i in 0..9) {
        try {
          pipe = RandomAccessFile("\\\\.\\pipe\\discord-ipc-$i", "rw")
          return
        } catch (_: Exception) {}
      }
      
      throw RuntimeException("Could not connect to the pipe!")
    }
    
    override fun read(bufferSize: Int): ByteArray {
      pipe?.let { stream ->
        val buffer = ByteArray(bufferSize)
        stream.read(buffer, 0, bufferSize)
        return buffer
      } ?: throw IllegalStateException("Not connected")
    }
    
    override fun write(opcode: Int, data: String) {
      pipe?.let { stream ->
        val bytes = data.encodeToByteArray()
        val buffer = ByteArray(bytes.size + 8)
        
        buffer.putInt(opcode.reverseBytes())
        buffer.putInt(bytes.size.reverseBytes(), 4)
        bytes.copyInto(buffer, 8)
        
        stream.write(buffer)
      } ?: throw IllegalStateException("Not connected")
    }
    
    override fun close() {
      pipe?.close()
    }
  }
  
  internal class UnixConnection(private val pipePath: String?): IConnection {
    private var pipe: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    
    override fun open() {
      val dir =
        (getenv("XDG_RUNTIME_DIR") ?:
         getenv("TMPDIR") ?:
         getenv("TMP") ?:
         getenv("TEMP")) ?:
        "/tmp"
      
      pipe = Socket(Proxy.NO_PROXY)
      for (i in 0..9) {
        try {
          pipe!!.connect(UnixDomainSocketAddress.of(pipePath ?: "$dir/discord-ipc-$i"))
          inputStream = pipe!!.getInputStream()
          outputStream = pipe!!.getOutputStream()
          return
        } catch (_: Exception) {}
      }
      
      throw RuntimeException("Could not connect to the pipe!")
    }
    
    override fun read(bufferSize: Int): ByteArray {
      inputStream?.let { stream ->
        val buffer = ByteArray(bufferSize)
        stream.read(buffer, 0, bufferSize)
        return buffer
      } ?: throw IllegalStateException("Not connected")
    }
    
    override fun write(opcode: Int, data: String) {
      outputStream?.let { stream ->
        val bytes = data.encodeToByteArray()
        val buffer = ByteArray(bytes.size + 8)
        
        buffer.putInt(opcode.reverseBytes())
        buffer.putInt(bytes.size.reverseBytes(), 4)
        bytes.copyInto(buffer, 8)
        
        stream.write(buffer)
      } ?: throw IllegalStateException("Not connected")
    }
    
    override fun close() {
      pipe?.close()
      inputStream?.close()
      outputStream?.close()
    }
  }
}