package io.github.vyfor.kpresence.utils

internal fun ByteArray.putInt(value: Int, offset: Int = 0) {
  this[offset] = (value shr 24).toByte()
  this[offset + 1] = (value shr 16).toByte()
  this[offset + 2] = (value shr 8).toByte()
  this[offset + 3] = value.toByte()
}

internal fun ByteArray.putIntLE(value: Int, offset: Int = 0) {
  this[offset] = value.toByte()
  this[offset + 1] = (value shr 8).toByte()
  this[offset + 2] = (value shr 16).toByte()
  this[offset + 3] = (value shr 24).toByte()
}

internal fun ByteArray.putIntBE(value: Int, offset: Int = 0) {
  this[offset] = (value shr 24).toByte()
  this[offset + 1] = (value shr 16).toByte()
  this[offset + 2] = (value shr 8).toByte()
  this[offset + 3] = value.toByte()
}

internal fun ByteArray.getInt(offset: Int = 0): Int {
  return (this[offset].toInt() shl 24) or
    (this[offset + 1].toInt() shl 16) or
    (this[offset + 2].toInt() shl 8) or
    (this[offset + 3].toInt())
}

internal fun Int.reverseBytes(): Int {
  return (this and -0x1000000 ushr 24) or
    (this and 0x00ff0000 ushr 8) or
    (this and 0x0000ff00 shl 8) or
    (this and 0x000000ff shl 24)
}

internal fun ByteArray.byteArrayToInt(): Int {
  require(size == 4) { "ByteArray size must be 4" }
  
  return (this[0].toInt() shl 24) +
         (this[1].toInt() shl 16) +
         (this[2].toInt() shl 8) +
         (this[3].toInt() shl 0)
}

fun formatTime(epochMillis: Long): String {
  val seconds = (epochMillis / 1000) % 60
  val minutes = (epochMillis / (1000 * 60)) % 60
  val hours = (epochMillis / (1000 * 60 * 60)) % 24
  
  return "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}

expect fun epochMillis(): Long

expect fun getProcessId(): Int