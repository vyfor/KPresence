package me.blast

import me.blast.kpresence.rpc.Activity
import me.blast.kpresence.Client
import kotlin.test.Test

class ClientTest {
  @Test
  fun testClient() {
    val client = Client(1216296290451984424)
    
    client.onMessage { message ->
      println("Received message:")
      println(message.decodeToString())
    }
    
    client.connect {
      println("hi im ready")
      update(
        Activity(
          state = "KPresence"
        )
      )
    }
    
    while (true) {}
  }
}