package me.blast

import me.blast.kpresence.rpc.Activity
import me.blast.kpresence.Client
import me.blast.kpresence.ipc.readBytes
import kotlin.test.Test

class ClientTest {
  @Test
  fun testClient() {
    val client = Client(1216296290451984424).connect()
    println(readBytes(client.handle, 512).decodeToString())
    
    client.update(
      Activity(
        state = "KPresence"
      )
    )
    println(readBytes(client.handle, 512).decodeToString())
    
    while (true) {}
  }
}