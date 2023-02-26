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
    implementation("com.github.kittinunf.fuel:fuel:2.3.1")
    implementation("org.json:json:20220924")

}

kotlin {
    jvmToolchain(8)
}

application {
    mainClass.set("MainKt")
}