package io.github.vyfor.kpresence

import java.lang.System.getenv

/**
 * Gets the default base paths for Discord IPC on the JVM platform.
 * @return A mutable list of default base paths.
 */
actual fun getDefaultPaths(): MutableList<String> {
  return if (System.getProperty("os.name").lowercase().startsWith("windows")) {
    mutableListOf("\\\\.\\pipe")
  } else {
    val dirs =
            listOf("XDG_RUNTIME_DIR", "TMPDIR", "TMP", "TEMP")
                    .mapNotNull { getenv(it) }
                    .plus("/tmp")
                    .flatMap { base ->
                      listOf(base, "$base/app/com.discordapp.Discord", "$base/snap.discord")
                    }

    dirs.toMutableList()
  }
}
