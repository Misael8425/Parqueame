plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    id("application")
    id("io.ktor.plugin") version "3.1.3"
}

repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
}

application {
    mainClass.set("com.parqueame.ApplicationKt")
}

ktor {
    docker {
        jreVersion.set(JavaVersion.VERSION_17)
        localImageName.set("parqueame-backend")
        imageTag.set("latest")
    }
}

dependencies {
    // MongoDB
    implementation("org.litote.kmongo:kmongo-coroutine:5.1.0")
    implementation("org.litote.kmongo:kmongo-serialization:5.1.0")

    // Serialización JSON
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")

    // DateTime
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")

    // Commons CSV
    implementation("org.apache.commons:commons-csv:1.11.0")

    // dotenv
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.13")

    // Ktor server
    implementation("io.ktor:ktor-server-core:3.1.3")
    implementation("io.ktor:ktor-server-netty:3.1.3")
    implementation("io.ktor:ktor-server-content-negotiation:3.1.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.1.3")
    implementation("io.ktor:ktor-server-status-pages:3.1.3")
    implementation("io.ktor:ktor-server-call-logging:3.1.3")
    implementation("io.ktor:ktor-server-default-headers:3.1.3")
    implementation("io.ktor:ktor-server-host-common:3.1.3")
    implementation("io.ktor:ktor-server-cors:3.1.3") // <-- agregado (CORS)

    // Ktor client (versión unificada 3.1.3)
    implementation("io.ktor:ktor-client-core:3.1.3")
    implementation("io.ktor:ktor-client-cio:3.1.3")
    implementation("io.ktor:ktor-client-content-negotiation:3.1.3")
    implementation("io.ktor:ktor-client-logging:3.1.3")

    // Jsoup
    implementation("org.jsoup:jsoup:1.17.2")

    // BCrypt
    implementation("org.mindrot:jbcrypt:0.4")

    // Mail
    implementation("com.sun.mail:jakarta.mail:2.0.1")

    // Selenium (si realmente lo usas en server)
    implementation("org.seleniumhq.selenium:selenium-java:4.15.0")
    implementation("org.seleniumhq.selenium:selenium-chrome-driver:4.15.0")
    implementation("io.github.bonigarcia:webdrivermanager:5.6.2")

    // Stripe Server
    implementation("com.stripe:stripe-java:24.0.0")

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}