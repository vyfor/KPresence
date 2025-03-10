package io.github.vyfor.kpresence

import io.github.vyfor.kpresence.event.ReadyEvent
import io.github.vyfor.kpresence.logger.ILogger
import io.github.vyfor.kpresence.logger.LogLevel
import kotlin.test.Test

class ClientTest {
  @Test
  fun testClient() {
    val client = RichClient(1219918880005165137)
    client.logger = ILogger.default(LogLevel.TRACE)

    client
            .on<ReadyEvent> {
              client.logger?.info(
                      "Awaiting input (<text>, clear, reconnect, reconnectAsync, shutdown):"
              )
            }
            .connect(true)

    while (true) {
      val input = readln()

      if (input == "shutdown") {
        client.shutdown()
        break
      }

      if (input == "reconnect") {
        client.reconnect(true)
        continue
      }

      if (input == "reconnectAsync") {
        client.reconnect(false)
        continue
      }

      if (client.connectionState == ConnectionState.DISCONNECTED) break

      if (input == "clear") {
        client.clear()
        continue
      }

      client.update {
        details = input
        state = "KPresence"
      }
    }
  }
}
