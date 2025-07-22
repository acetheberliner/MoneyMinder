// settings.gradle.kts ‒ nella root del progetto
pluginManagement {
    repositories {
        gradlePluginPortal()                    // default (serve per Shadow, JLink…)
        mavenCentral()                          // in realtà non serve, ma non guasta
        // repository dove viene pubblicato il plugin OpenJFX
        maven("https://s01.oss.sonatype.org/content/repositories/releases")
    }
}

rootProject.name = "MoneyMinder"
