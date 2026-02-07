import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.jvm.tasks.Jar

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
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.12.0")
}

val sdkCoreClassesDir = project(":sdk-core").layout.buildDirectory.dir("classes/java/main")
val sdkCoreResourcesDir = project(":sdk-core").layout.buildDirectory.dir("resources/main")
val sdkNativeLoaderClassesDir = project(":sdk-native-loader").layout.buildDirectory.dir("classes/java/main")
val sdkCoreSourcesDir = project(":sdk-core").layout.projectDirectory.dir("src/main/java")
val sdkNativeLoaderSourcesDir = project(":sdk-native-loader").layout.projectDirectory.dir("src/main/java")
val externalRuntimeArtifacts = configurations.runtimeClasspath.get().incoming.artifactView {
    componentFilter { componentId -> componentId is ModuleComponentIdentifier }
}.files

tasks.register<Jar>("fatJarAllPlatforms") {
    group = "build"
    description = "Build a sdk-highlevel fat jar with bundled native resources for configured platforms."

    dependsOn(tasks.named("classes"))
    dependsOn(":sdk-core:classes")
    dependsOn(":sdk-native-loader:compileJava")
    dependsOn(":sdk-native-loader:syncNativeResourcesFromBundles")

    archiveBaseName.set("boxlite-java-highlevel-allplatforms")
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(sourceSets.main.get().output)
    from(sdkCoreClassesDir)
    from(sdkCoreResourcesDir)
    from(sdkNativeLoaderClassesDir)

    from({
        externalRuntimeArtifacts.files.map { artifactFile ->
            if (artifactFile.isDirectory) artifactFile else zipTree(artifactFile)
        }
    })

    from(project(":sdk-native-loader").layout.buildDirectory.dir("generated/native-bundles")) {
        include("native/**")
    }
}

tasks.register<Jar>("fatJarAllPlatformsSources") {
    group = "build"
    description = "Build a sources jar aligned with fatJarAllPlatforms API classes."

    archiveBaseName.set("boxlite-java-highlevel-allplatforms")
    archiveClassifier.set("sources")
    archiveVersion.set(project.version.toString())
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(sourceSets.main.get().java)
    from(sdkCoreSourcesDir)
    from(sdkNativeLoaderSourcesDir)
}

tasks.named("fatJarAllPlatforms") {
    finalizedBy("fatJarAllPlatformsSources")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    maxParallelForks = 1
    forkEvery = 1
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    systemProperty("junit.jupiter.execution.parallel.enabled", "false")
    systemProperty("junit.jupiter.execution.timeout.default", "5m")
}
