import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTestRun
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest

plugins {
    kotlin("multiplatform") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    id("org.jetbrains.dokka") version "1.9.20"
    id("com.louiscad.complete-kotlin") version "1.1.0"
    id("com.vanniktech.maven.publish") version "0.28.0"
}

group = "io.github.vyfor"
version = "0.5.2"

repositories {
    mavenCentral()
}

kotlin {
    val jvmTarget = jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "16"
            }
        }
        
        jvmToolchain(16)
    }
    val mingwTarget = mingwX64()
    val linuxTargets = listOf(
        linuxArm64(),
        linuxX64()
    )
    val macosTargets = listOf(
        macosArm64(),
        macosX64()
    )
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val nativeMain by creating {
            dependsOn(commonMain)
        }
        val mingwX64Main by getting {
            dependsOn(commonMain)
            dependsOn(nativeMain)
        }
        val linuxMain by creating {
            dependsOn(commonMain)
            dependsOn(nativeMain)
        }
        val linuxArm64Main by getting {
            dependsOn(linuxMain)
        }
        val linuxX64Main by getting {
            dependsOn(linuxMain)
        }
        val macosMain by creating {
            dependsOn(commonMain)
            dependsOn(nativeMain)
        }
        val macosArm64Main by getting {
            dependsOn(macosMain)
        }
        val macosX64Main by getting {
            dependsOn(macosMain)
        }
        
        jvmTarget.apply {
            compilations["main"].defaultSourceSet.apply {
                dependsOn(commonMain)
            }
            
            compilations["test"].defaultSourceSet.dependsOn(commonTest)
        }
        
        mingwTarget.apply {
            compilations["main"].defaultSourceSet.apply {
                dependsOn(nativeMain)
                dependsOn(mingwX64Main)
            }
            
            compilations["test"].defaultSourceSet.dependsOn(commonTest)
        }
        
        linuxTargets.forEach { target ->
            target.compilations["main"].defaultSourceSet.apply {
                dependsOn(nativeMain)
                dependsOn(linuxMain)
            }
            
            target.compilations["test"].defaultSourceSet.dependsOn(commonTest)
        }
        
        macosTargets.forEach { target ->
            target.compilations["main"].defaultSourceSet.apply {
                dependsOn(nativeMain)
                dependsOn(macosMain)
            }
            
            target.compilations["test"].defaultSourceSet.dependsOn(commonTest)
        }
        
        all {
            languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
        }
        
        targets.all {
            compilations.all {
                kotlinOptions {
                    kotlinOptions.freeCompilerArgs += "-Xexpect-actual-classes"
                }
            }
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
    
    coordinates("io.github.vyfor", "kpresence", project.version.toString())
    
    pom {
        name.set("kpresence")
        description.set("A lightweight, cross-platform Kotlin library for Discord Rich Presence interaction.")
        url.set("https://github.com/vyfor/KPresence")
        inceptionYear.set("2024")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                id.set("vyfor")
                name.set("vyfor")
                url.set("https://github.com/vyfor/")
            }
        }
        scm {
            url.set("https://github.com/vyfor/KPresence/")
            connection.set("scm:git:git://github.com/vyfor/KPresence.git")
            developerConnection.set("scm:git:ssh://git@github.com/vyfor/KPresence.git")
        }
        issueManagement {
            system.set("Github")
            url.set("https://github.com/vyfor/KPresence/issues")
        }
    }
    
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    
    signAllPublications()
}
