plugins {
    kotlin("multiplatform") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
}

group = "me.blast"
version = "0.1"

repositories {
    mavenCentral()
}

kotlin {
    val hostOs = System.getProperty("os.name")
    val isArm64 = System.getProperty("os.arch") == "aarch64"
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" && isArm64 -> macosArm64("native")
        hostOs == "Mac OS X" && !isArm64 -> macosX64("native")
        hostOs == "Linux" && isArm64 -> linuxArm64("native")
        hostOs == "Linux" && !isArm64 -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }
    
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
