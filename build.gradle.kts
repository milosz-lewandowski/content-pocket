plugins {
    java
    application
    id("org.javamodularity.moduleplugin") version "1.8.12"
    id("org.openjfx.javafxplugin") version "0.0.13"
    id("org.beryx.jlink") version "2.25.0"
}

group = "pl.me-wash"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val junitVersion = "5.10.2"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

application {
    mainModule.set("pl.mewash.contentlaundry")
    mainClass.set("pl.mewash.contentlaundry.LaundryApplication")
    applicationDefaultJvmArgs = listOf("-Dfile.encoding=UTF-8")
}

javafx {
    version = "21"
    modules = listOf("javafx.controls", "javafx.fxml")
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.0")
    implementation("com.fasterxml.jackson.core:jackson-core:2.19.0")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.19.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.19.0")

    implementation("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

jlink {
    options.set(
        listOf(
            "--strip-debug",
            "--compress", "2",
            "--no-header-files",
            "--no-man-pages"
        )
    )

    launcher {
        name = "ContentLaundry"
    }

    jpackage {
        imageName = "ContentLaundry"
        installerType = "dmg" // Creates a folder, not an installer
        skipInstaller = false
        appVersion = "1.0.0"
        // icon = "icon.ico" // Add this later if needed
         resourceDir = file("src/main/jpackage")
    }
}

tasks.register("prepareMacTools") {
    doLast {
        file("src/main/jpackage/tools/mac").listFiles()?.forEach { file ->
            if (file.isFile) {
                exec {
                    commandLine("chmod", "+x", file.absolutePath)
                }
            }
        }
    }
}
tasks.named("jpackage") {
    dependsOn("prepareMacTools")
}