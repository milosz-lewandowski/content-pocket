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
    mainModule.set("pl.mewash.contentpocket")
    mainClass.set("pl.mewash.contentpocket.app.ContentPocketApplication")
    applicationDefaultJvmArgs = listOf("-Dfile.encoding=UTF-8")
}

dependencies {
    implementation(project(":commands-api"))
    implementation(project(":common"))

    implementation(project(":subscriptions"))
    implementation(project(":batch"))
}

// --- enable build with or without tools bundled (default = true) ---
val isBundleWithTools: Boolean = providers
    .gradleProperty("isBundleWithTools")
    .map { it.toBoolean() }
    .orElse(true)
    .get()

// --- introducing conditional build to avoid keeping two branches with same codebase differed only by build files ---
val isWindows = System.getProperty("os.name").lowercase().contains("win")
val isMac = System.getProperty("os.name").lowercase().contains("mac")

jlink {
    options.set(
        listOf(
            "--strip-debug",
            "--no-header-files",
            "--no-man-pages"
        )
    )

    launcher {
        name = "ContentPocket"
    }

    jpackage {
        imageName = "ContentPocket"
        appVersion = providers
            .gradleProperty("appVersion").get()

        // --- windows setup ---
        if (isWindows) {
            installerType = "app-image"
            skipInstaller = true
            icon = file("src/main/resources/icons/app-icon.ico").absolutePath
            resourceDir = file("src/main/resources")
        }

        // --- macos setup ---
        if (isMac) {
            installerType = "dmg"
            skipInstaller = false
            icon = file("src/main/resources/icons/app-icon.icns").absolutePath
            resourceDir = file("src/main/resources")
        }
    }
}

// --- bundle tools execs into zip ---
if (isWindows && isBundleWithTools) {
    tasks.named<JPackageImageTask>("jpackageImage") {
        doLast {
            val targetTools = layout.buildDirectory.dir("jpackage/ContentPocket/tools").get().asFile
            copy {
                from("tools")
                into(targetTools)
            }
            println("✔ Bundled tools into ZIP distribution")
        }
    }
}

// --- build ZIP distro if compiled on windows ---
if (isWindows) {
    tasks.register<Zip>("zipPortableApp") {
        dependsOn("jpackageImage")

        val appVersion = providers.gradleProperty("appVersion").get()

        val suffix = if (isBundleWithTools) {
            providers.gradleProperty("bundleBuildSuffix").get()
        } else {
            providers.gradleProperty("nonBundleBuildSuffix").get()
        }

        val archiveName = "ContentPocket-$appVersion-$suffix.zip"


        group = "distribution"
        description = "Content Pocket executable archive"

        archiveFileName.set(archiveName)
        destinationDirectory.set(layout.buildDirectory.dir("distributions"))

        from(layout.buildDirectory.dir("jpackage/ContentPocket"))

        // --- Copy README.md to zip ---
        from(rootProject.file("README.md")) {
            into(".")
        }
    }
}

// --- just prints current version to add it to .dmg name in workflow build ---
if (isMac) {
    tasks.register("printVersion") {
        doLast {
            println(providers.gradleProperty("appVersion").get())
        }
    }
}