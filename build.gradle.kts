import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.internal.os.OperatingSystem

plugins {
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "money.minder"
// version = "1.0"

application { mainClass.set("app.gui.LauncherFx") }
java        { toolchain.languageVersion.set(JavaLanguageVersion.of(17)) }

repositories { mavenCentral() }

/* ------------ JavaFX deps (NON dentro il fat-jar) ------------ */
val jfxVer = "21.0.8"
val platform = when {
    OperatingSystem.current().isWindows -> "win"
    OperatingSystem.current().isMacOsX  -> "mac"
    else                                -> "linux"
}

dependencies {
    listOf("base","graphics","controls","fxml").forEach {
        implementation("org.openjfx:javafx-$it:$jfxVer:$platform")
        runtimeOnly  ("org.openjfx:javafx-$it:$jfxVer:$platform")
    }

    implementation("info.picocli:picocli:4.7.5")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.1")
    implementation("org.apache.poi:poi-ooxml:5.2.5")
    implementation("org.jfxtras:jmetro:11.6.15")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

tasks.test { 
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

/* ------------ shadowJar ------------ */
tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("MoneyMinder")
    archiveClassifier.set("")
    // archiveVersion.set(project.version.toString())

    manifest {
        attributes["Main-Class"] = "app.gui.LauncherFx"
    }
}

tasks.withType<JavaCompile> { options.encoding = "UTF-8" }
