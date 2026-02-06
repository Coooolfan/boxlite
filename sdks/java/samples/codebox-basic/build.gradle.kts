plugins {
    application
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

dependencies {
    implementation(project(":sdk-highlevel"))
}

application {
    mainClass.set("io.boxlite.samples.codebox.basic.CodeBoxBasicApp")
}
