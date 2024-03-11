# 🧩 KPresence

**📦 A lightweight, cross-platform [Kotlin](https://kotlinlang.org/) library for Discord Rich Presence interaction.**

## ⚙️ Installation

### Gradle

```gradle
repositories {
    ...
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

client.update(
  Activity(
    type = ActivityType.GAME,
    state = "via KPresence",
    assets = ActivityAssets(
      largeImage = "image"
    )
  )
)
```