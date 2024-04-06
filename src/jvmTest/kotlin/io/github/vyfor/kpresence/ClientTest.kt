package io.github.vyfor.kpresence

import io.github.vyfor.kpresence.rpc.*
import kotlin.random.Random
import kotlin.test.Test

class JVMClientTest {
  @Test
  fun testClient() {
    val client = RichClient(1216296290451984424)
    
    client.connect {
      println("Connected")
    }
    
    repeat(2) {
      println("Updating")
      client.update(
        activity {
          details = Random.nextInt().toString()
          state = "KPresence"
        }
      )
      Thread.sleep(15000)
    }
  }
}
