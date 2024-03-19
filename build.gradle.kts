import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.dokka.gradle.DokkaTask
import java.util.Base64

plugins {
    kotlin("multiplatform") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    id("org.jetbrains.dokka") version "1.9.20"
    id("com.louiscad.complete-kotlin") version "1.1.0"
    id("com.vanniktech.maven.publish") version "0.28.0"
}

group = "io.github.reblast"
version = "0.3.0"

repositories {
    mavenCentral()
}

kotlin {
    val targets = listOf(
        linuxArm64(),
        linuxX64(),
        macosArm64(),
        macosX64(),
        mingwX64()
    )
    
    sourceSets {
        val commonMain by getting
        val linuxArm64Main by getting
        val linuxX64Main by getting
        val macosArm64Main by getting
        val macosX64Main by getting
        val mingwX64Main by getting
        val nativeMain by creating {
            dependsOn(commonMain)
            
            dependencies {
                implementation(kotlin("stdlib"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
            }
        }
        val nativeTest by creating {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        
        targets.forEach { target ->
            target.compilations["main"].defaultSourceSet.dependsOn(nativeMain)
        }
        
        targets.forEach { target ->
            target.compilations["test"].defaultSourceSet.dependsOn(nativeTest)
        }
    }
}

mavenPublishing {
    configure(
        KotlinMultiplatform(
            javadocJar = JavadocJar.Dokka("dokkaHtml"),
            sourcesJar = true
        )
    )
    
    coordinates("io.github.reblast", "kpresence", project.version.toString())
    
    pom {
        name.set("kpresence")
        description.set("A lightweight, cross-platform Kotlin library for Discord Rich Presence interaction.")
        url.set("https://github.com/reblast/KPresence")
        inceptionYear.set("2024")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                id.set("reblast")
                name.set("axeon")
                url.set("https://github.com/reblast/")
            }
        }
        scm {
            url.set("https://github.com/reblast/KPresence/")
            connection.set("scm:git:git://github.com/reblast/KPresence.git")
            developerConnection.set("scm:git:ssh://git@github.com/reblast/KPresence.git")
        }
        issueManagement {
            system.set("Github")
            url.set("https://github.com/reblast/KPresence/issues")
        }
    }
    
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    
    signAllPublications()
}
