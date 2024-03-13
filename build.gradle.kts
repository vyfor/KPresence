import org.jetbrains.dokka.gradle.DokkaTask
import java.util.Base64

plugins {
    kotlin("multiplatform") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    id("org.jetbrains.dokka") version "1.9.20"
    id("maven-publish")
    id("signing")
    id("com.louiscad.complete-kotlin") version "1.1.0"
}

group = "me.blast"
version = "0.1.3"

repositories {
    mavenCentral()
}

kotlin {
    val targets = listOf(
        mingwX64(),
        linuxX64(),
        linuxArm64(),
        macosX64(),
        macosArm64()
    )
    
    sourceSets {
        val commonMain by getting
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

val dokkaOutputDir = "$buildDir/dokka"

tasks.getByName<DokkaTask>("dokkaHtml") {
    outputDirectory.set(file(dokkaOutputDir))
}

val deleteDokkaOutputDir by tasks.register<Delete>("deleteDokkaOutputDirectory") {
    delete(dokkaOutputDir)
}

val javadocJar = tasks.register<Jar>("javadocJar") {
    dependsOn(deleteDokkaOutputDir, tasks.dokkaHtml)
    archiveClassifier.set("javadoc")
    from(dokkaOutputDir)
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/reblast/KPresence")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
        
        maven {
            name = "OSS"
            val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deployByRepositoryId/${project.findProperty("sonatype.repository")}/")
            val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
            
            credentials {
                username = project.findProperty("sonatype.username") as String? ?: System.getenv("SONATYPE_USERNAME")
                password = project.findProperty("sonatype.password") as String? ?: System.getenv("SONATYPE_PASSWORD")
            }
        }
    }
    
    publications {
        withType<MavenPublication> {
            artifact(javadocJar)
            
            pom {
                groupId = "me.blast"
                artifactId = "kpresence"
                version = project.version.toString()
                name.set("KPresence")
                description.set("A lightweight, cross-platform Kotlin library for Discord Rich Presence interaction.")
                url.set("https://github.com/reblast/KPresence")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        name.set("axeon")
                        email.set(project.findProperty("sonatype.email") as String)
                        url.set("https://github.com/reblast")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/reblast/KPresence.git")
                    developerConnection.set("scm:git:ssh://github.com:reblast/KPresence.git")
                    url.set("http://github.com/reblast/KPresence/tree/master")
                }
            }
        }
    }
}

signing {
    useInMemoryPgpKeys(
        Base64.getDecoder().decode(project.findProperty("gpg.key") as String? ?: System.getenv("GPG_PRIVATE_KEY")).decodeToString(),
        project.findProperty("gpg.password") as String? ?: System.getenv("GPG_PRIVATE_PASSWORD")
    )
    sign(publishing.publications)
}