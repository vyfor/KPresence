package io.github.vyfor.kpresence.utils

import kotlinx.cinterop.*
import platform.posix.*

actual fun epochMillis(): Long = memScoped {
  val timeVal = alloc<timeval>()
  gettimeofday(timeVal.ptr, null)
  (timeVal.tv_sec * 1000) + (timeVal.tv_usec / 1000)
}

actual fun getProcessId(): Int = getpid()