package io.github.reblast.kpresence.utils

import kotlinx.cinterop.*
import platform.windows.FILETIME
import platform.windows.GetSystemTimeAsFileTime

actual fun epochMillis(): Long = memScoped {
  val ft = alloc<FILETIME>()
  GetSystemTimeAsFileTime(ft.ptr)
  val li = (ft.dwHighDateTime.toULong() shl 32) or ft.dwLowDateTime.toULong()
  (li.toLong() - 116444736000000000L) / 10000
}