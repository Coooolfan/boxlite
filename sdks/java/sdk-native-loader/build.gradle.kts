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

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val guestBinaryFile: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val shimBinaryFile: RegularFileProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val boxliteBuildDir: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val signScriptFile: RegularFileProperty

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
        val guestBinary = guestBinaryFile.get().asFile
        if (!guestBinary.exists()) {
            throw GradleException("Guest binary not found after cargo build: ${guestBinary.path}")
        }
        val shimBinary = shimBinaryFile.get().asFile
        if (!shimBinary.exists()) {
            throw GradleException("Shim binary not found after cargo build: ${shimBinary.path}")
        }

        val runtimeSourceDir = findRuntimeLibrariesDir(boxliteBuildDir.get().asFile)

        val platformDir = outputBaseDir.get().asFile.resolve("native/${platformId.get()}")
        if (platformDir.exists()) {
            platformDir.deleteRecursively()
        }
        platformDir.mkdirs()

        nativeFile.copyTo(platformDir.resolve(nativeFile.name), overwrite = true)
        val guestTarget = platformDir.resolve("boxlite-guest")
        guestBinary.copyTo(guestTarget, overwrite = true)
        guestTarget.setExecutable(true, false)
        val shimTarget = platformDir.resolve("boxlite-shim")
        shimBinary.copyTo(shimTarget, overwrite = true)
        shimTarget.setExecutable(true, false)
        signShimIfRequired(shimTarget)

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

    private fun signShimIfRequired(shimBinary: File) {
        if (!platformId.get().startsWith("darwin-")) {
            return
        }

        val signScript = signScriptFile.get().asFile
        if (!signScript.exists()) {
            throw GradleException("Shim signing script not found: ${signScript.path}")
        }

        val process = ProcessBuilder("bash", signScript.absolutePath, shimBinary.absolutePath)
            .directory(signScript.parentFile)
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw GradleException(
                "Failed to sign shim binary at ${shimBinary.path} with exit code $exitCode:\n$output",
            )
        }
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

fun resolveGuestTarget(archName: String): String {
    val archRaw = archName.lowercase()
    return when (archRaw) {
        "aarch64", "arm64" -> "aarch64-unknown-linux-musl"
        "x86_64", "amd64" -> "x86_64-unknown-linux-musl"
        else -> throw GradleException("Unsupported architecture for guest build: $archName")
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
val guestTarget = resolveGuestTarget(System.getProperty("os.arch"))
val nativeLibraryOutputFile = repoRoot.resolve("target/debug/$nativeLibraryName")
val guestBinaryOutputFile = repoRoot.resolve("target/$guestTarget/debug/boxlite-guest")
val shimBinaryOutputFile = repoRoot.resolve("target/debug/boxlite-shim")

val cargoBuildNativeDebug = tasks.register<Exec>("cargoBuildNativeDebug") {
    workingDir = repoRoot
    commandLine(
        "bash",
        "-lc",
        "source \"${'$'}HOME/.cargo/env\" && cargo build -p $nativeCrateName && cargo build -p boxlite --bin boxlite-shim && cargo build --target $guestTarget -p boxlite-guest",
    )

    inputs.file(repoRoot.resolve("Cargo.toml"))
    inputs.file(repoRoot.resolve("sdks/java/native/Cargo.toml"))
    inputs.file(repoRoot.resolve("guest/Cargo.toml"))
    inputs.dir(repoRoot.resolve("sdks/java/native/src"))
    inputs.dir(repoRoot.resolve("boxlite/src"))
    inputs.dir(repoRoot.resolve("guest/src"))
    inputs.property("guestTarget", guestTarget)
    outputs.file(nativeLibraryOutputFile)
    outputs.file(guestBinaryOutputFile)
    outputs.file(shimBinaryOutputFile)
}

val syncNativeResources = tasks.register<SyncNativeResourcesTask>("syncNativeResources") {
    dependsOn(cargoBuildNativeDebug)

    nativeLibraryFile.set(nativeLibraryOutputFile)
    guestBinaryFile.set(guestBinaryOutputFile)
    shimBinaryFile.set(shimBinaryOutputFile)
    boxliteBuildDir.set(repoRoot.resolve("target/debug/build"))
    signScriptFile.set(repoRoot.resolve("scripts/build/sign.sh"))
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
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.12.0")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
