import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

kotlin {
    // Build with a modern local JDK (works on Apple Silicon), while keeping bytecode Java 8.
    jvmToolchain(17)
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(8)
}

application {
    mainClass.set("com.vladdrummer.prayerkmp.server.ApplicationKt")
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveFileName.set("kmp_prayer_backend.jar")
    mergeServiceFiles()
    manifest {
        attributes["Main-Class"] = "com.vladdrummer.prayerkmp.server.ApplicationKt"
    }
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:2.3.12")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.12")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:2.3.12")
    implementation("io.ktor:ktor-server-status-pages-jvm:2.3.12")
    implementation("mysql:mysql-connector-java:5.1.49")
    implementation("ch.qos.logback:logback-classic:1.2.13")
}
