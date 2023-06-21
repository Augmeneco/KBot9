plugins {
    kotlin("jvm") version "1.8.0"
    application
}

group = "ru.augmeneco"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("org.json:json:20220924")
    implementation("org.xerial:sqlite-jdbc:3.41.2.1")
    implementation("org.jsoup:jsoup:1.16.1")
}

kotlin {
    jvmToolchain(8)
}

application {
    mainClass.set("MainKt")
}
