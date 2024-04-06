package io.github.vyfor.kpresence.ipc

expect class Connection() {
  fun open()
  fun read(): ByteArray
  fun write(opcode: Int, data: String)
  fun close()
}