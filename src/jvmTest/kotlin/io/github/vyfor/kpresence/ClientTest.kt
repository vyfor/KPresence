package io.github.vyfor.kpresence

import io.github.vyfor.kpresence.event.ReadyEvent
import io.github.vyfor.kpresence.logger.ILogger
import io.github.vyfor.kpresence.rpc.*
import kotlin.random.Random
import kotlin.test.Test

class JVMClientTest {
  @Test
  fun testClient() {
    val client = RichClient(1216296290451984424)
    client.logger = ILogger.default()
    
    client.connect()
    
    repeat(2) {
      client.update(
        activity {
          details = Random.nextInt().toString()
          state = "KPresence"
        }
      )
      Thread.sleep(15000)
    }
    
    client.shutdown()
  }
}
