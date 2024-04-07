@file:Suppress("unused")

package io.github.vyfor.kpresence.logger

import io.github.vyfor.kpresence.utils.epochMillis
import io.github.vyfor.kpresence.utils.formatTime

interface ILogger {
  fun info(message: String)
  fun warn(message: String)
  fun error(message: String)
  fun error(message: String, throwable: Throwable)
  
  companion object {
    /**
     * Returns a default implementation of the ILogger interface.
     */
    fun default(): ILogger = object : ILogger {
      override fun info(message: String) {
        println("${formatTime(epochMillis())} \u001B[34m[KPresence - INFO]\u001B[0m $message")
      }
      
      override fun warn(message: String) {
        println("${formatTime(epochMillis())} \u001B[33m[KPresence - WARN]\u001B[0m $message")
      }
      
      override fun error(message: String) {
        println("${formatTime(epochMillis())} \u001B[31m[KPresence - ERROR]\u001B[0m $message")
      }
      
      override fun error(message: String, throwable: Throwable) {
        println("${formatTime(epochMillis())} \u001B[31m[KPresence - ERROR]\u001B[0m $message\n${throwable.stackTraceToString()}")
      }
    }
  }
}