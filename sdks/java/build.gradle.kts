allprojects {
    group = "io.boxlite"
    version = "0.5.9"
}

subprojects {
    repositories {
        mavenCentral()
    }
}

tasks.register("fatJarAllPlatforms") {
    group = "build"
    description = "Build sdk-highlevel fat jar with multi-platform native resources."
    dependsOn(":sdk-highlevel:fatJarAllPlatforms")
}
