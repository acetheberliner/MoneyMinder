pluginManagement {
    repositories {
        gradlePluginPortal() // per Shadow, JLink…
        mavenCentral()
        // repository dove viene pubblicato il plugin OpenJFX
        maven("https://s01.oss.sonatype.org/content/repositories/releases")
    }
}

rootProject.name = "MoneyMinder"
