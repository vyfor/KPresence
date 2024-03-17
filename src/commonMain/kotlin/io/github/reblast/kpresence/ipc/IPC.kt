package io.github.reblast.kpresence.ipc

expect fun readBytes(handle: Int, bufferSize: Int = 4096): ByteArray

expect fun writeBytes(handle: Int, opcode: Int, data: String)