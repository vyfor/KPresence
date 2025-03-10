package io.github.vyfor.kpresence.ipc

import io.github.vyfor.kpresence.exception.*
import io.github.vyfor.kpresence.utils.byteArrayToInt
import io.github.vyfor.kpresence.utils.putInt
import io.github.vyfor.kpresence.utils.reverseBytes
import kotlinx.cinterop.*
import platform.windows.*

actual class Connection {
  private var pipe: HANDLE? = null

  actual fun open(paths: List<String>) {
    for (basePath in paths) {
      for (i in 0..9) {
        val fullPath = "$basePath\\discord-ipc-$i"
        val pipeHandle =
                CreateFileW(
                        fullPath,
                        GENERIC_READ or GENERIC_WRITE.convert(),
                        0u,
                        null,
                        OPEN_EXISTING.convert(),
                        FILE_FLAG_OVERLAPPED.convert(),
                        null
                )

        if (pipeHandle == INVALID_HANDLE_VALUE) {
          val err = GetLastError()
          if (err.toInt() == ERROR_FILE_NOT_FOUND) continue
          else throw ConnectionException(Exception(formatError(err)))
        } else {
          pipe = pipeHandle
          return
        }
      }
    }

    throw PipeNotFoundException()
  }

  actual fun read(): Message? = memScoped {
    pipe?.let { _ ->
      val opcode = readBytes(4)?.first?.byteArrayToInt()?.reverseBytes() ?: return@memScoped null
      val length = readBytes(4)?.first?.byteArrayToInt()?.reverseBytes() ?: return@memScoped null
      val (buffer) = readBytes(length) ?: return@memScoped null

      return Message(opcode, buffer)
    }
            ?: throw NotConnectedException()
  }

  actual fun write(opcode: Int, data: String?) {
    pipe?.let { handle ->
      val bytes = data?.encodeToByteArray()
      val buffer = ByteArray((bytes?.size ?: 0) + 8)

      buffer.putInt(opcode.reverseBytes())
      if (bytes != null) {
        buffer.putInt(bytes.size.reverseBytes(), 4)
        bytes.copyInto(buffer, 8)
      } else {
        buffer.putInt(0, 4)
      }

      val success =
              buffer.usePinned {
                WriteFile(handle, it.addressOf(0), buffer.size.convert(), null, null)
              }

      if (success == FALSE) {
        val err = GetLastError()

        if (err.toInt() == ERROR_BROKEN_PIPE) {
          throw ConnectionClosedException(formatError(err))
        }
        throw PipeWriteException(formatError(err))
      }
    }
            ?: throw NotConnectedException()
  }

  actual fun close() {
    DisconnectNamedPipe(pipe)
    CloseHandle(pipe)
    pipe = null
  }

  private fun readBytes(size: Int): Pair<ByteArray, Int>? = memScoped {
    val bytesAvailable = alloc<UIntVar>()
    val result = PeekNamedPipe(pipe, null, 0u, null, bytesAvailable.ptr, null)

    if (result == FALSE) {
      val err = GetLastError()

      if (err.toInt() == ERROR_BROKEN_PIPE) {
        throw ConnectionClosedException(formatError(err))
      }
      throw PipeReadException(formatError(err))
    }
    if (bytesAvailable.value == 0u) {
      return null
    }

    val bytes = ByteArray(size)
    val bytesRead = alloc<UIntVar>()
    bytes.usePinned { pinnedBytes ->
      ReadFile(pipe, pinnedBytes.addressOf(0), size.convert(), bytesRead.ptr, null).let { success ->
        if (success == FALSE) {
          val err = GetLastError()

          if (err.toInt() == ERROR_BROKEN_PIPE) {
            throw ConnectionClosedException(formatError(err))
          }
          throw PipeReadException(formatError(err))
        }
      }
    }

    if (bytesRead.value == 0u) return null
    return bytes to bytesRead.value.toInt()
  }

  private fun formatError(err: DWORD) = memScoped {
    val errMessage = ByteArray(1024)
    val result =
            errMessage.usePinned { pinnedErrMessage ->
              FormatMessageA(
                      FORMAT_MESSAGE_FROM_SYSTEM.toUInt() or FORMAT_MESSAGE_IGNORE_INSERTS.toUInt(),
                      null,
                      err,
                      0u,
                      pinnedErrMessage.addressOf(0),
                      errMessage.size.convert(),
                      null
              )
            }
    if (result != 0u) errMessage.decodeToString() else "Error code: $err"
  }
}
