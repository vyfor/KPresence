package io.github.vyfor.kpresence.utils

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

internal fun ByteArray.byteArrayToInt(): Int {
  require(size == 4) { "ByteArray size must be 4" }
  
  return (get(0).toInt() and 0xFF shl 24) or
    (get(1).toInt() and 0xFF shl 16) or
    (get(2).toInt() and 0xFF shl 8) or
    (get(3).toInt() and 0xFF)
}

expect fun epochMillis(): Long

expect fun getProcessId(): Int