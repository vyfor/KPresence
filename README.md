# 🧩 KPresence

**📦 A lightweight, cross-platform [Kotlin](https://kotlinlang.org/) library for Discord Rich Presence interaction.**

## ⚙️ Installation

```gradle
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/reblast/KPresence")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("me.blast:kpresence-native:VERSION")
}
```

## ✨ Examples
```kt
val client = Client(CLIENT_ID)
  
client.connect()

val activity = activity {
    type = ActivityType.GAME
    details = "Exploring Kotlin Native"
    state = "Writing code"
    
    timestamps {
        start = epochSeconds() - 3600
        end = epochSeconds() + 3600
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
