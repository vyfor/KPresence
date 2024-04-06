@file:JvmName("Utils")

package io.github.reblast.kpresence.utils

actual fun epochMillis(): Long = System.currentTimeMillis()

actual fun getProcessId(): Int = ProcessHandle.current().pid().toInt()