rootProject.name = "content-pocket"

buildscript {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven {
            url = uri("https://sandec.jfrog.io/artifactory/repo")
        }
    }
    dependencies {
        classpath("one.jpro:jpro-gradle-plugin:2025.3.1-SNAPSHOT")
    }
}

// Desktop App Components
include(":commands-api")
include(":content-pocket")
include(":subscriptions")
include(":batch")
include(":common")

// Web Stack Components
include(":web-jpro")