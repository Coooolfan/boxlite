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
    implementation(project(":sdk-native-loader"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.0")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
