package io.github.reblast.kpresence

import io.github.reblast.kpresence.rpc.*
import io.github.reblast.kpresence.utils.epochMillis
import kotlin.system.exitProcess
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
      readln().apply {
        if (this == "shutdown") {
          client.shutdown()
          exitProcess(0)
        }

        if (this == "clear") {
          client.clear()
          return@apply
        }
        
        client.update(
          activity {
            details = this@apply
            state = "KPresence"
          }
        )
      }
    }
  }
  
  @Test
  fun testEpochMillis() {
    println(epochMillis())
  }
}
