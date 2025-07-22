plugins {
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"   // fat-jar
    id("org.beryx.jlink")                 version "2.26.0"  // runtime image
}

group   = "money.minder"
version = "1.1.0"

application {
    mainClass.set("app.gui.LauncherFx")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

/* ---------- JavaFX come dipendenza ---------- */
repositories { mavenCentral() }

val jfxVersion = "21.0.1"
val os = org.gradle.internal.os.OperatingSystem.current()
val platform = when {
    os.isWindows -> "win"
    os.isMacOsX  -> "mac"
    else         -> "linux"
}

dependencies {
    listOf("base", "graphics", "controls", "fxml").forEach {
        implementation("org.openjfx:javafx-$it:$jfxVersion:$platform")
        runtimeOnly  ("org.openjfx:javafx-$it:$jfxVersion:$platform")
    }

    implementation("info.picocli:picocli:4.7.5")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.1")
    implementation("org.apache.poi:poi-ooxml:5.2.5")
    implementation("org.jfxtras:jmetro:11.6.15")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

tasks.test { useJUnitPlatform() }

/* ---------- Shadow fat-jar ---------- */
tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveBaseName.set("app")
    archiveClassifier.set("")                           // app-1.1.0-all.jar
    mergeServiceFiles()
}

/* ---------- JLink ---------- */
jlink {
    imageName.set("MoneyMinder")
    options.set(listOf("--strip-debug", "--compress=2", "--no-header-files"))
    launcher {                       // → bin/moneyminder
        name = "moneyminder"
    }
    addExtraDependencies("javafx")    // porta dentro le native-libs JavaFX
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.named<JavaExec>("run") {

    /* --- costruisci il module-path con TUTTO ciò che c’è in runtimeClasspath --- */
    val mp = configurations.runtimeClasspath.get()
        .joinToString(File.pathSeparator) { it.absolutePath }

    jvmArgs = listOf(
        "--module-path", mp,
        "--add-modules", "javafx.controls,javafx.fxml"
    )
}