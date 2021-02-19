import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val dusseldorfKtorVersion = "1.5.0.3315e68"
val ktorVersion = ext.get("ktorVersion").toString()
val k9FormatVersion = "3.0.0.3794ec7"
val slf4jVersion = ext.get("slf4jVersion").toString()
val kotlinxCoroutinesVersion = ext.get("kotlinxCoroutinesVersion").toString()

val openhtmltopdfVersion = "1.0.6"
val kafkaEmbeddedEnvVersion = "2.4.0"
val kafkaVersion = "2.4.0" // Alligned med version fra kafka-embedded-env
val handlebarsVersion = "4.1.2"

val mainClass = "no.nav.helse.OmsorgspengerutbetalingeSoknadProsesseringKt"

plugins {
    kotlin("jvm") version "1.4.30"
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

buildscript {
    // Henter ut diverse dependency versjoner, i.e. ktorVersion.
    apply("https://raw.githubusercontent.com/navikt/dusseldorf-ktor/3315e68ec40ad9a24fa0c45d4f463d9cbae56fa0/gradle/dusseldorf-ktor.gradle.kts")
}

dependencies {
    // Server
    implementation ( "no.nav.helse:dusseldorf-ktor-core:$dusseldorfKtorVersion")
    implementation ( "no.nav.helse:dusseldorf-ktor-jackson:$dusseldorfKtorVersion")
    implementation ( "no.nav.helse:dusseldorf-ktor-metrics:$dusseldorfKtorVersion")
    implementation ( "no.nav.helse:dusseldorf-ktor-health:$dusseldorfKtorVersion")
    implementation ( "no.nav.helse:dusseldorf-ktor-auth:$dusseldorfKtorVersion")
    implementation ( "no.nav.k9:soknad-omsorgspenger-utbetaling:$k9FormatVersion")
    implementation ( "org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:$kotlinxCoroutinesVersion")
    
    // Client
    implementation ( "no.nav.helse:dusseldorf-ktor-client:$dusseldorfKtorVersion")
    implementation ( "no.nav.helse:dusseldorf-oauth2-client:$dusseldorfKtorVersion")

    // PDF
    implementation ( "com.openhtmltopdf:openhtmltopdf-pdfbox:$openhtmltopdfVersion")
    implementation ( "com.openhtmltopdf:openhtmltopdf-slf4j:$openhtmltopdfVersion")
    implementation ( "org.slf4j:jcl-over-slf4j:$slf4jVersion")
    implementation ( "com.github.jknack:handlebars:$handlebarsVersion")

    // Kafka
    implementation("org.apache.kafka:kafka-streams:$kafkaVersion")

    // Test
    testImplementation ( "org.apache.kafka:kafka-clients:$kafkaVersion")
    testImplementation ( "no.nav:kafka-embedded-env:$kafkaEmbeddedEnvVersion")
    testImplementation ( "no.nav.helse:dusseldorf-test-support:$dusseldorfKtorVersion")
    testImplementation ( "io.ktor:ktor-server-test-host:$ktorVersion") {
        exclude(group = "org.eclipse.jetty")
    }
    testImplementation("org.skyscreamer:jsonassert:1.5.0")
}

repositories {
    mavenLocal()

    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/navikt/k9-format")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_USERNAME")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }

    mavenCentral()
    jcenter()

    maven("https://dl.bintray.com/kotlin/ktor")
    maven("https://kotlin.bintray.com/kotlinx")
    maven("https://packages.confluent.io/maven/")
}


java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}


tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<ShadowJar> {
    archiveBaseName.set("app")
    archiveClassifier.set("")
    manifest {
        attributes(
            mapOf(
                "Main-Class" to mainClass
            )
        )
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "6.7.1"
}