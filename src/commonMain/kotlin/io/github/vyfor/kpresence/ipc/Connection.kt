@file:Suppress("ArrayInDataClass")

package io.github.vyfor.kpresence.ipc

expect class Connection() {
  fun open(paths: List<String>)
  fun read(): Message?
  fun write(opcode: Int, data: String?)
  fun close()
}

data class Message(val opcode: Int, val data: ByteArray)
