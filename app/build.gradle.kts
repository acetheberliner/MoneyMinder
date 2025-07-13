plugins {
    application                        // contiene già 'java'
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "money.minder"
version = "1.0.0"

application {
    mainClass.set("app.App")           // package corretto
}

dependencies {
    implementation("info.picocli:picocli:4.7.5")              // CLI
    annotationProcessor("info.picocli:picocli-codegen:4.7.5") // autocompl.
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.1")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

tasks.test { useJUnitPlatform() }
