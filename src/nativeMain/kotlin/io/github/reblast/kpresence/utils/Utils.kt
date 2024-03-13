@file:Suppress("unused")

package io.github.reblast.kpresence.utils

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
fun epochSeconds(): Int = time(null).toInt()

@OptIn(ExperimentalForeignApi::class)
fun formatEpoch(seconds: Long) = memScoped {
  val timePtr = alloc<time_tVar>()
  timePtr.value = seconds
  val tm = gmtime(timePtr.ptr)
  buildString {
    append(
      "${tm!!.pointed.tm_year + 1900}-" +
      "${(tm.pointed.tm_mon + 1).toString().padStart(2, '0')}-" +
      "${tm.pointed.tm_mday.toString().padStart(2, '0')} "
    )
    append(
      "${tm.pointed.tm_hour.toString().padStart(2, '0')}:" +
      "${tm.pointed.tm_min.toString().padStart(2, '0')}:" +
      "${tm.pointed.tm_sec.toString().padStart(2, '0')}"
    )
  }
}