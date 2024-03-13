package io.github.reblast.kpresence.rpc

import kotlinx.serialization.Serializable

@Serializable
data class Packet(
  val cmd: String,
  val args: PacketArgs,
  val nonce: String? = null
)

@Serializable
data class PacketArgs(
  val pid: Int,
  val activity: Activity?
)