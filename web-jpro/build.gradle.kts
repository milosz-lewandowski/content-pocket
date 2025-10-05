plugins {
    id("java")
    id("application")
    id("org.openjfx.javafxplugin") version "0.0.13"
    id("jpro-gradle-plugin")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":"))
}

javafx {
    version = "25.0.2-jpro"
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.web", "javafx.graphics")
}

application {
    mainClass.set("pl.mewash.web.ContentPocketJProLauncher")
}

