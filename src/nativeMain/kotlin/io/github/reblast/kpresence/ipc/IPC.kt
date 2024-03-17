@file:Suppress("unused")

package io.github.reblast.kpresence.ipc

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.refTo
import io.github.reblast.kpresence.utils.putInt
import io.github.reblast.kpresence.utils.reverseBytes
import platform.posix.*
import platform.zlib.uLong
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
fun openPipe(): Int {
  if (Platform.osFamily == OsFamily.WINDOWS) {
    for (i in 0..9) {
      val handle = open("\\\\.\\pipe\\discord-ipc-$i", O_RDWR)
      
      if (handle == -1) continue
      else return handle
    }
  } else {
    val dir =
      getenv("XDG_RUNTIME_DIR") ?:
      getenv("TMPDIR") ?:
      getenv("TMP") ?:
      getenv("TEMP") ?:
      "/tmp"
    
    for (i in 0..9) {
      val handle = open("${dir}/discord-ipc-$i", O_RDWR)
      
      if (handle == -1) continue
      else return handle
    }
  }
  
  throw RuntimeException("Could not connect to the pipe!")
}

fun closePipe(handle: Int) {
  close(handle)
}
