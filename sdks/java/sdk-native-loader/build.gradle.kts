import java.io.File
import java.nio.charset.StandardCharsets
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

plugins {
    `java-library`
}

abstract class SyncNativeResourcesTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val nativeLibraryFile: RegularFileProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val boxliteBuildDir: DirectoryProperty

    @get:Input
    abstract val platformId: Property<String>

    @get:OutputDirectory
    abstract val outputBaseDir: DirectoryProperty

    @TaskAction
    fun sync() {
        val nativeFile = nativeLibraryFile.get().asFile
        if (!nativeFile.exists()) {
            throw GradleException("Native library not found after cargo build: ${nativeFile.path}")
        }

        val runtimeSourceDir = findRuntimeLibrariesDir(boxliteBuildDir.get().asFile)

        val platformDir = outputBaseDir.get().asFile.resolve("native/${platformId.get()}")
        if (platformDir.exists()) {
            platformDir.deleteRecursively()
        }
        platformDir.mkdirs()

        nativeFile.copyTo(platformDir.resolve(nativeFile.name), overwrite = true)

        val runtimeTargetDir = platformDir.resolve("runtime")
        runtimeSourceDir.copyRecursively(runtimeTargetDir, overwrite = true)

        val runtimeFiles = runtimeTargetDir.listFiles()
            ?.filter { it.isFile }
            ?.map { it.name }
            ?.sorted()
            .orEmpty()

        val indexFile = runtimeTargetDir.resolve("index.txt")
        indexFile.writeText(
            runtimeFiles.joinToString("\n", postfix = if (runtimeFiles.isEmpty()) "" else "\n"),
            StandardCharsets.UTF_8,
        )
    }

    private fun findRuntimeLibrariesDir(buildRoot: File): File {
        val candidates = buildRoot.listFiles()
            ?.filter { it.isDirectory && it.name.startsWith("boxlite-") }
            ?.map { it.resolve("out/runtime") }
            ?.filter { it.isDirectory }
            .orEmpty()

        return candidates.maxByOrNull { it.lastModified() }
            ?: throw GradleException(
                "Could not find runtime libraries under ${buildRoot.path}. Build output did not produce boxlite runtime artifacts.",
            )
    }
}

fun resolvePlatformId(osName: String, archName: String): String {
    val os = osName.lowercase()
    val archRaw = archName.lowercase()

    val arch = when (archRaw) {
        "aarch64", "arm64" -> "aarch64"
        "x86_64", "amd64" -> "x86_64"
        else -> throw GradleException("Unsupported architecture for Java SDK native build: $archName")
    }

    return when {
        os.contains("mac") -> "darwin-$arch"
        os.contains("linux") -> "linux-$arch"
        else -> throw GradleException("Unsupported operating system for Java SDK native build: $osName")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    withSourcesJar()
    withJavadocJar()
}

val repoRoot = rootProject.projectDir.resolve("../..").canonicalFile
val nativeCrateName = "boxlite-java-native"
val nativeLibraryBaseName = "boxlite_java_native"
val nativeLibraryName = System.mapLibraryName(nativeLibraryBaseName)
val nativeLibraryOutputFile = repoRoot.resolve("target/debug/$nativeLibraryName")

val cargoBuildNativeDebug = tasks.register<Exec>("cargoBuildNativeDebug") {
    workingDir = repoRoot
    commandLine("bash", "-lc", "source \"${'$'}HOME/.cargo/env\" && cargo build -p $nativeCrateName")

    inputs.file(repoRoot.resolve("Cargo.toml"))
    inputs.file(repoRoot.resolve("sdks/java/native/Cargo.toml"))
    inputs.dir(repoRoot.resolve("sdks/java/native/src"))
    outputs.file(nativeLibraryOutputFile)
}

val syncNativeResources = tasks.register<SyncNativeResourcesTask>("syncNativeResources") {
    dependsOn(cargoBuildNativeDebug)

    nativeLibraryFile.set(nativeLibraryOutputFile)
    boxliteBuildDir.set(repoRoot.resolve("target/debug/build"))
    platformId.set(resolvePlatformId(System.getProperty("os.name"), System.getProperty("os.arch")))
    outputBaseDir.set(layout.buildDirectory.dir("generated/native"))
}

sourceSets {
    named("main") {
        resources.srcDir(layout.buildDirectory.dir("generated/native"))
    }
}

tasks.named("processResources") {
    dependsOn(syncNativeResources)
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.0")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
