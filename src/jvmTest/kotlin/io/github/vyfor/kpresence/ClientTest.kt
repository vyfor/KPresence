package io.github.vyfor.kpresence

import io.github.vyfor.kpresence.logger.ILogger
import io.github.vyfor.kpresence.logger.LogLevel
import kotlin.random.Random
import kotlin.test.Test

class JVMClientTest {
  @Test
  fun testClient() {
    val client = RichClient(1216296290451984424)
    client.logger = ILogger.default(LogLevel.TRACE)
    
    client.connect(true)
    
    repeat(20) {
      client.update {
        details = Random.nextInt().toString()
        state = "KPresence"
      }
      Thread.sleep(1000)
    }
    
    client.shutdown()
  }
}
