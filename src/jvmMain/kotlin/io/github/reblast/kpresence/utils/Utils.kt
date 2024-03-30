@file:JvmName("Utils")

package io.github.reblast.kpresence.utils

import java.lang.management.ManagementFactory

actual fun epochMillis(): Long = System.currentTimeMillis()

actual fun getProcessId(): Int = ManagementFactory
  .getRuntimeMXBean()
  .name
  .let { it.substring(0, it.indexOf("@")) }
  .toIntOrNull() ?:
  throw RuntimeException("Could not obtain process id")