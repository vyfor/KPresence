@file:Suppress("unused")

package io.github.vyfor.kpresence.logger

import io.github.vyfor.kpresence.utils.epochMillis
import io.github.vyfor.kpresence.utils.formatTime

interface ILogger {
  fun error(message: String, throwable: Throwable)
  fun error(message: String)
  fun warn(message: String)
  fun info(message: String)
  fun debug(message: String)
  fun trace(message: String)
  
  companion object {
    /**
     * Returns a default implementation of the ILogger interface.
     */
    fun default(): ILogger = object : ILogger {
      override fun error(message: String, throwable: Throwable) {
        println("${formatTime(epochMillis())} \u001B[31m[KPresence - ERROR]\u001B[0m $message\n${throwable.stackTraceToString()}")
      }
      
      override fun error(message: String) {
        println("${formatTime(epochMillis())} \u001B[31m[KPresence - ERROR]\u001B[0m $message")
      }
      
      override fun warn(message: String) {
        println("${formatTime(epochMillis())} \u001B[33m[KPresence - WARN]\u001B[0m $message")
      }
      
      override fun info(message: String) {
        println("${formatTime(epochMillis())} \u001B[34m[KPresence - INFO]\u001B[0m $message")
      }
      
      override fun debug(message: String) {
        println("${formatTime(epochMillis())} \u001B[36m[KPresence - DEBUG]\u001B[0m $message")
      }
      
      override fun trace(message: String) {
        println("${formatTime(epochMillis())} \u001B[35m[KPresence - TRACE]\u001B[0m $message")
      }
    }
  }
}