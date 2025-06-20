plugins {
    application
    kotlin("jvm") version "1.8.0"
    kotlin("plugin.serialization") version "1.8.0"
}

application {
    mainClass.set("com.example.GameServerKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:2.3.3")
    implementation("io.ktor:ktor-server-websockets-jvm:2.3.3")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.3")
    implementation("ch.qos.logback:logback-classic:1.4.11")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:2.3.3")
}
