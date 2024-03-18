package io.github.reblast.kpresence

import io.github.reblast.kpresence.rpc.activity
import io.github.reblast.kpresence.utils.epochMillis
import kotlin.system.exitProcess
import kotlin.test.Test

class ClientTest {
  @Test
  fun testClient() {
    val client = RichClient(1216296290451984424)
    
    client.connect {
      update(
        activity {
          details = "KPresence 1"
        }
      )
    }
    
    while (true) {
      readln().apply {
        if (this == "shutdown") {
          client.shutdown()
          exitProcess(0)
        }
        
        client.update(
          activity {
            state = this@apply
            details = "KPresence"
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
