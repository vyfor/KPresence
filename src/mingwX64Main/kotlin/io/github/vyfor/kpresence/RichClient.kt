package io.github.vyfor.kpresence

/**
 * Gets the default base path for Discord IPC on the Windows platform.
 *
 * @return A mutable list containing the Windows pipe base path.
 */
actual fun getDefaultPaths(): MutableList<String> {
  return mutableListOf("\\\\.\\pipe")
}
