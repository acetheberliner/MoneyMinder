plugins {
    application                                // <- basta questo
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "money.minder"
version = "1.1.0"

application {
    mainClass.set("app.gui.LauncherFx")
}

/* ---------- JavaFX deps cross-platform ---------- */
val jfxVersion = "21.0.1"
val os = org.gradle.internal.os.OperatingSystem.current()
val platform = when {
    os.isWindows -> "win"
    os.isMacOsX  -> "mac"
    else         -> "linux"
}

repositories { mavenCentral() }

dependencies {
    // JavaFX: controls + fxml sono sufficienti
    listOf("base", "graphics", "controls", "fxml").forEach {
        implementation("org.openjfx:javafx-$it:$jfxVersion:$platform")
        runtimeOnly  ("org.openjfx:javafx-$it:$jfxVersion:$platform")
    }

    implementation("info.picocli:picocli:4.7.5")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.1")

    implementation("org.jfxtras:jmetro:11.6.15")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

tasks.test { useJUnitPlatform() }

/* ---------- Passa module-path & add-modules al task run ---------- */
tasks.named<JavaExec>("run") {
    val jfxJars = configurations.runtimeClasspath.get()
        .filter { it.name.startsWith("javafx-") }
        .files.joinToString(File.pathSeparator)

    jvmArgs = listOf(
        "--module-path", jfxJars,
        "--add-modules", "javafx.controls,javafx.fxml"
    )
}

/* --------- idem per il fat-jar (shadowJar) ---------- */
tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    // includi i jar JavaFX nel fat-jar
    mergeServiceFiles()
}
