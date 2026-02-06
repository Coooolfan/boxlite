plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    api(project(":sdk-core"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.0")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
