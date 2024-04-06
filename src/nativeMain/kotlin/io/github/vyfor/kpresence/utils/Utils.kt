@file:Suppress("unused")

package io.github.vyfor.kpresence.utils

import kotlinx.cinterop.*
import platform.posix.*

fun epochSeconds(): Int = time(null).toInt()

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
      tm.pointed.tm_sec.toString().padStart(2, '0')
    )
  }
}