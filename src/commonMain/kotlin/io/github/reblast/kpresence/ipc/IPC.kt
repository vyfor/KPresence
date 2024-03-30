package io.github.reblast.kpresence.ipc

expect fun openPipe(): Int

expect fun closePipe(pipe: Int)

expect fun readBytes(pipe: Int, bufferSize: Int = 4096): ByteArray

expect fun writeBytes(pipe: Int, opcode: Int, data: String)
