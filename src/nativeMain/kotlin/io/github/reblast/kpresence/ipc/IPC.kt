@file:Suppress("unused")

package io.github.reblast.kpresence.ipc

import io.github.reblast.kpresence.utils.putInt
import io.github.reblast.kpresence.utils.reverseBytes
import kotlinx.cinterop.*
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
    println("OS: Linux\n")

    val dir =
      (getenv("XDG_RUNTIME_DIR") ?:
      getenv("TMPDIR") ?:
      getenv("TMP") ?:
      getenv("TEMP"))?.toKString() ?:
      "/tmp"

    print("Found temp directory at `$dir`")
    print("Trying to connect\n")

    for (i in 0..9) {
      val pipePath = "${dir}/discord-ipc-$i"
      print("Connecting to $pipePath")
      val handle = open(pipePath, O_RDWR)
      
      if (handle == -1) {
        println("Failed")
        continue
      } else {
        println("Success")
        return handle
      }
    }

    println("\nTrying to connect to flatpack path\n")

    // $XDG_RUNTIME_DIR/app/com.discordapp.Discord/discord-ipc-0


    for (i in 0..9) {
      val pipePath = "${dir}/app/com.discordapp.Discord/discord-ipc-$i"
      print("Connecting to $pipePath")
      val handle = open(pipePath, O_RDWR)
      
      if (handle == -1) {
        println("Failed")
        continue
      } else {
        println("Success")
        return handle
      }
    }
  }
  

  throw RuntimeException("Could not connect to the pipe!")
}

fun closePipe(handle: Int) {
  close(handle)
}
