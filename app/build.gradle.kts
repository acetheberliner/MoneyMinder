plugins {
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "it.unibo"
version = "1.0.0"

application {
    mainClass.set("it.unibo.moneyminder.App")
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.1")
    implementation("info.picocli:picocli:4.7.5")
    annotationProcessor("info.picocli:picocli-codegen:4.7.5")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

tasks.test { useJUnitPlatform() }
