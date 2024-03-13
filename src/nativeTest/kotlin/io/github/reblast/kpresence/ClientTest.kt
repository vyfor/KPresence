package io.github.reblast.kpresence

import io.github.reblast.kpresence.rpc.activity
import io.github.reblast.kpresence.utils.epochMillis
import platform.posix.sleep
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
      
      update(
        activity {
          details = "I'm ignored"
        }
      )
      
      sleep(4u)
      
      update(
        activity {
          details = "KPresence 2"
        }
      )
    }
    
    sleep(15u)
  }
  
  @Test
  fun testEpochMillis() {
    println(epochMillis())
  }
}