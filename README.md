# 🧩 KPresence

**📦 A lightweight, cross-platform [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) library for interacting with Discord Rich Presence.**

## 💎 Features
- Cross-platform compatibility (Windows, Linux, macOS)
- Fast and user-friendly
- Offers DSL support
- Provides both JVM and Native implementations
- Validates the activity fields before sending them
- Supports Flatpak and Snap installations

## 🔌 Requirements
- **Java**: `16 or later` (only for use within the JVM environment)

## ⚙️ Installation

```gradle
dependencies {
    implementation("io.github.vyfor:kpresence:0.6.2")
}
```

## ✨ Examples

### Initial connection and presence updates
```kt
val client = RichClient(CLIENT_ID)
  
client.connect()

client.update {
    type = ActivityType.GAME
    details = "Exploring Kotlin Native"
    state = "Writing code"
    
    timestamps {
        start = now() - 3600_000
        end = now() + 3600_000
    }
    
    party {
      id = "myParty"
      size(current = 1, max = 5)
    }
    
    assets {
        largeImage = "kotlin_logo"
        largeText = "Kotlin"
        smallImage = "jetbrains_logo"
        smallText = "JetBrains"
    }
    
    secrets {
        join = "joinSecret"
        spectate = "spectateSecret"
        match = "matchSecret"
    }
    
    button("Learn more", "https://kotlinlang.org/")
    button("Try it yourself", "https://play.kotlinlang.org/")
}
```

### Event handling
```kt
val client = RichClient(CLIENT_ID)

client.on<ReadyEvent> {
  update(activity)
}

client.on<ActivityUpdateEvent> {
  logger?.info("Updated rich presence")
}

client.on<DisconnectEvent> {
  connect(shouldBlock = true) // Attempt to reconnect
}

client.connect(shouldBlock = false)
```

### Logging
```kt
val client = RichClient(CLIENT_ID)
client.logger = ILogger.default()
```
