plugins {
    application
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

dependencies {
    implementation(project(":sdk-core"))
}

application {
    mainClass.set("io.boxlite.samples.smoke.SmokeApp")
}
