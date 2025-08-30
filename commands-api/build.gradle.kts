plugins {
    `java-library`
}

version = providers
    .gradleProperty("commandsApiVersion")
    .orElse(providers
        .gradleProperty("appVersion")).get()

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")
}