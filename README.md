# 🧩 KPresence

**📦 A lightweight, cross-platform [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) library for interacting with Discord Rich Presence.**

## 💎 Features
- Cross-platform compatibility (Windows, Linux, macOS*)
- Fast and user-friendly, featuring DSL support
- Provides both JVM and Native implementations
- Respects the ratelimit of one update per 15 seconds. The library will always send the newest presence update once the client is free to do so
- Validates the activity fields before sending them

## 🔌 Requirements
- **Java**: `16 or later` (only for use within the JVM environment)

## ⚙️ Installation

```gradle
dependencies {
    implementation("io.github.reblast:kpresence:0.5.0")
}
```

## ✨ Examples
```kt
val client = RichClient(CLIENT_ID)
  
client.connect()

val activity = activity {
    type = ActivityType.GAME
    details = "Exploring Kotlin Native"
    state = "Writing code"
    
    timestamps {
        start = epochMillis() - 3600_000
        end = epochMillis() + 3600_000
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

client.update(activity)
```

## Notes
> * To use on macOS, clone the repository and build it manually.
