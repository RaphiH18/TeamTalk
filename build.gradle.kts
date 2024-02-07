plugins {
    kotlin("jvm") version "1.8.22"
    kotlin("plugin.serialization") version "1.5.0"
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("com.github.johnrengelman.shadow") version "7.1.0"
}

group = "ch.abbts"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.22")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.1")
    implementation("org.apache.commons:commons-configuration2:2.9.0")
    implementation("org.yaml:snakeyaml:2.2")
    implementation("org.json:json:20231013")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
    useJUnitPlatform()
}

tasks {
    val serverJar by creating(com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
        archiveBaseName.set("TeamTalk-Server")
        manifest {
            attributes("Main-Class" to "teamtalk.server.ServerKt")
        }

        from(sourceSets.main.get().output)
        configurations.clear()
        configurations.add(project.configurations.runtimeClasspath.get())
    }

    val clientJar by creating(com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
        archiveBaseName.set("TeamTalk-Client")
        manifest {
            attributes("Main-Class" to "teamtalk.client.ClientKt")
        }

        from(sourceSets.main.get().output)
        configurations.clear()
        configurations.add(project.configurations.runtimeClasspath.get())
    }

    val allJAR by registering {
        dependsOn(serverJar, clientJar)
        doLast {
            println("Beide JAR-Dateien wurden erstellt.")
        }
    }
}

kotlin {
    jvmToolchain(8)
}

javafx {
    modules("javafx.controls", "javafx.base", "javafx.graphics", "javafx.swing")
}