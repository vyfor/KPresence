# 🧩 KPresence

**📦 A lightweight, cross-platform [Kotlin](https://kotlinlang.org/) library for Discord Rich Presence interaction.**

## ⚙️ Installation

### Gradle

```gradle
repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.reblast:KPresence:VERSION")
}
```

### Maven
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.reblast</groupId>
    <artifactId>KPresence</artifactId>
    <version>VERSION</version>
</dependency>
```

## ✨ Examples
```kt
val client = Client(CLIENT_ID).connect()

val activity = activity {
    type = ActivityType.GAME
    details = "Exploring Kotlin Native"
    state = "Writing code"
    
    timestamps {
        start = epochMillis() - 3600_000
        end = epochMillis() + 3600_000
    }
    
    emoji {
        name = "🚀"
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