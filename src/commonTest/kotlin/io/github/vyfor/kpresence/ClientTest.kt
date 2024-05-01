package io.github.vyfor.kpresence

import io.github.vyfor.kpresence.event.ReadyEvent
import io.github.vyfor.kpresence.logger.ILogger
import io.github.vyfor.kpresence.rpc.*
import io.github.vyfor.kpresence.utils.epochMillis
import kotlin.test.Test

class ClientTest {
  @Test
  fun testClient() {
    val client = RichClient(1216296290451984424)
    client.logger = ILogger.default()
    
    client.on<ReadyEvent> {
      client.logger?.info("Awaiting input (<text>, clear, shutdown):")
    }.connect()
    
    while (true) {
      val input = readln()
      
      if (input == "shutdown") {
        client.shutdown()
        break
      }
      
      if (input == "clear") {
        client.clear()
        continue
      }
      
      client.update(
        activity {
          details = input
          state = "KPresence"
        }
      )
    }
    
    if (client.connectionState != ConnectionState.DISCONNECTED) {
      client.shutdown()
    }
  }
  
  @Test
  fun testEpochMillis() {
    println(epochMillis())
  }
}
