plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    alias(libs.plugins.jvm)
    alias(libs.plugins.lombok.plugin)
    alias(libs.plugins.lombok)
    alias(libs.plugins.serialization)
   // Apply the application plugin to add support for building a CLI application in Java.
    application
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    // Use the Kotlin JUnit 5 integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")

    // Use the JUnit 5 integration.
    testImplementation(libs.junit.jupiter.engine)

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    // This dependency is used by the application.
    implementation(libs.guava)
    implementation(libs.serialization.json)
    implementation(libs.cassandra)
    implementation(libs.datastax.cassandra.driver)
    implementation(libs.datastax.cassandra.mapping)
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
    jvmToolchain(17)
}

group = "pl.poznan.put.cs"
version = "0.1.0-snapshot"

sourceSets.main {
    java.srcDirs("src/main/java", "src/main/kotlin")
    resources.srcDirs("src/main/resources")
}

application {
    // Define the main class for the application.
    mainClass.set("hector.AppKt")
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}

tasks.withType<Copy>().all { duplicatesStrategy = DuplicatesStrategy.INCLUDE }
