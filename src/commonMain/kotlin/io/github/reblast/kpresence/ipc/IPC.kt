package io.github.reblast.kpresence.ipc

expect fun openPipe(): Int

expect fun closePipe(handle: Int)

expect fun readBytes(handle: Int, bufferSize: Int = 4096): ByteArray

expect fun writeBytes(handle: Int, opcode: Int, data: String)
