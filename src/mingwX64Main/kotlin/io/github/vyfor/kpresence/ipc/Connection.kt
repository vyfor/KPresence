package io.github.vyfor.kpresence.ipc

import io.github.vyfor.kpresence.exception.*
import io.github.vyfor.kpresence.utils.byteArrayToInt
import io.github.vyfor.kpresence.utils.putInt
import io.github.vyfor.kpresence.utils.reverseBytes
import kotlinx.cinterop.*
import platform.windows.*

actual class Connection {
  private var pipe: HANDLE? = null
  
  actual fun open() {
    for (i in 0..9) {
      val pipeHandle = CreateFileW("\\\\.\\pipe\\discord-ipc-$i", GENERIC_READ or GENERIC_WRITE.convert(), 0u, null, OPEN_EXISTING.convert(), 0u, null)
      
      if (pipeHandle == INVALID_HANDLE_VALUE) {
        val err = GetLastError()
        if (err.toInt() == ERROR_FILE_NOT_FOUND) continue
        else throw ConnectionException(Exception(formatError(err)))
      }
      else {
        pipe = pipeHandle
        return
      }
    }
    
    throw PipeNotFoundException()
  }
  
  actual fun read(): ByteArray = memScoped {
    pipe?.let { _ ->
      readBytes(4)
      val length = readBytes(4).first.byteArrayToInt().reverseBytes()
      val buffer = ByteArray(length)
      val bytesRead = readBytes(4).second
      
      return buffer.copyOf(bytesRead)
    } ?: throw NotConnectedException()
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
      
      if (success == FALSE) throw PipeWriteException(formatError(GetLastError()))
    } ?: throw NotConnectedException()
  }
  
  actual fun close() {
    DisconnectNamedPipe(pipe)
    CloseHandle(pipe)
    pipe = null
  }
  
  private fun readBytes(size: Int): Pair<ByteArray, Int> = memScoped {
    val bytes = ByteArray(size)
    val bytesRead = alloc<UIntVar>()
    ReadFile(pipe, bytes.pin().addressOf(0), size.convert(), bytesRead.ptr, null).let { success ->
      if (success == FALSE) {
        throw PipeReadException(formatError(GetLastError()))
      }
    }
    return bytes to bytesRead.value.toInt()
  }
  
  private fun formatError(err: DWORD) = memScoped {
    val errMessage = ByteArray(1024)
    val result = FormatMessageA(
      FORMAT_MESSAGE_FROM_SYSTEM.toUInt() or FORMAT_MESSAGE_IGNORE_INSERTS.toUInt(),
      null,
      err,
      0u,
      errMessage.pin().addressOf(0),
      errMessage.size.convert(),
      null
    )
    if (result != 0u) errMessage.decodeToString()
    else "Error code: $err"
  }
}