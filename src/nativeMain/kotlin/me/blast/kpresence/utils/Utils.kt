package me.blast.kpresence

import kotlinx.cinterop.*
import platform.posix.*

internal fun ByteArray.putInt(value: Int, offset: Int = 0) {
  this[offset] = (value shr 24).toByte()
  this[offset + 1] = (value shr 16).toByte()
  this[offset + 2] = (value shr 8).toByte()
  this[offset + 3] = value.toByte()
}

internal fun Int.reverseBytes(): Int {
  return (this and -0x1000000 ushr 24) or
    (this and 0x00ff0000 ushr 8) or
    (this and 0x0000ff00 shl 8) or
    (this and 0x000000ff shl 24)
}

@OptIn(ExperimentalForeignApi::class)
internal fun epochMillis(): Int = time(null).toInt()