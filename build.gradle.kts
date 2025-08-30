import org.beryx.jlink.JPackageImageTask

plugins {
    java
    application
    id("org.javamodularity.moduleplugin") version "1.8.12"
    id("org.openjfx.javafxplugin") version "0.0.13"
    id("org.beryx.jlink") version "2.25.0"
}

allprojects {
    group = providers.gradleProperty("group").get()
    version = providers.gradleProperty("appVersion").get()

    repositories {
        mavenCentral()
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

javafx {
    version = "21"
    modules = listOf("javafx.controls", "javafx.fxml")
}

application {
    mainModule.set("pl.mewash.contentlaundry")
    mainClass.set("pl.mewash.contentlaundry.app.LaundryApplication")
    applicationDefaultJvmArgs = listOf("-Dfile.encoding=UTF-8")
}

dependencies {
    implementation(project(":commands-api"))
    implementation(project(":common"))

    implementation(project(":subscriptions"))
    implementation(project(":batch"))
}

jlink {
    options.set(
        listOf(
            "--strip-debug",
            "--no-header-files",
            "--no-man-pages"
        )
    )

    launcher {
        name = "ContentLaundry"
    }

    jpackage {
        imageName = "ContentLaundry"
        installerType = "app-image"
        skipInstaller = true
        appVersion = providers
            .gradleProperty("appVersion").get()
        resourceDir = file("src/main/resources")
    }
}

tasks.named<JPackageImageTask>("jpackageImage") {
    doLast {
        val targetTools = layout.buildDirectory.dir("jpackage/ContentLaundry/tools").get().asFile
        copy {
            from("tools")
            into(targetTools)
        }
        println("✅ Copied tools to: $targetTools")
    }
}

tasks.register<Zip>("zipPortableApp") {
    dependsOn("jpackageImage")

    val archiveName = "ContentLaundry-" + providers
        .gradleProperty("appVersion")
        .get() + "-portable.zip"

    group = "distribution"
    description = "Content Laundry executable archive"

    archiveFileName.set(archiveName)
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))

    from(layout.buildDirectory.dir("jpackage/ContentLaundry"))
//    into("ContentLaundry") // disabled to avoid nested app dir
}