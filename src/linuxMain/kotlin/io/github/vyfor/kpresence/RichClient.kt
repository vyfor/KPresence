package io.github.vyfor.kpresence

import platform.posix.getenv

/**
 * Gets the default base paths for Discord IPC on the Linux platform.
 *
 * @return A mutable list of default base paths.
 */
actual fun getDefaultPaths(): MutableList<String> {
  val dirs =
      listOf("XDG_RUNTIME_DIR", "TMPDIR", "TMP", "TEMP")
          .mapNotNull { getenv(it)?.toKString() }
          .plus("/tmp")
          .flatMap { base ->
            listOf(base, "$base/app/com.discordapp.Discord", "$base/snap.discord")
          }

  return dirs.toMutableList()
}
