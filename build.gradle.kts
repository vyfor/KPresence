plugins {
    kotlin("multiplatform") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
}

group = "me.blast"
version = "0.1.0"

repositories {
    mavenCentral()
}

kotlin {
    sourceSets {
        val nativeMain by getting
        val nativeTest by getting
        
        nativeMain.dependencies {
            implementation(kotlin("stdlib"))
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
        }
        
        nativeTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
