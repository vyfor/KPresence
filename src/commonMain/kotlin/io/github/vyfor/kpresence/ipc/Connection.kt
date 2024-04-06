package io.github.reblast.kpresence.ipc

expect class Connection() {
  fun open()
  fun read(bufferSize: Int = 4096): ByteArray
  fun write(opcode: Int, data: String)
  fun close()
}