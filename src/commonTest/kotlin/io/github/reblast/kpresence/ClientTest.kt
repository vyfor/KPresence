package io.github.reblast.kpresence

import io.github.reblast.kpresence.rpc.*
import io.github.reblast.kpresence.utils.epochMillis
import kotlin.test.Test

class ClientTest {
  @Test
  fun testClient() {
    val client = RichClient(1216296290451984424)
    
    client.connect {
      println("Connected")
      println("Awaiting input (<text>, clear, shutdown):")
    }
    
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
  }
  
  @Test
  fun testEpochMillis() {
    println(epochMillis())
  }
}
