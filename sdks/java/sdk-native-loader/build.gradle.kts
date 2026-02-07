import java.io.File
import java.nio.charset.StandardCharsets
import java.util.Locale
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
import org.gradle.language.jvm.tasks.ProcessResources

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

fun expectedNativeLibraryName(platformId: String): String =
    when {
        platformId.startsWith("darwin-") -> "libboxlite_java_native.dylib"
        platformId.startsWith("linux-") -> "libboxlite_java_native.so"
        else -> throw GradleException("Unsupported native bundle platform: $platformId")
    }

fun parseBooleanProperty(propertyName: String, configuredValue: String?): Boolean {
    if (configuredValue == null) {
        return false
    }

    return when (configuredValue.trim().lowercase(Locale.ROOT)) {
        "true", "1", "yes", "on" -> true
        "false", "0", "no", "off", "" -> false
        else -> throw GradleException(
            "Invalid boolean for -P$propertyName: '$configuredValue' (expected true/false)",
        )
    }
}

fun resolvePathFromRoot(rootDir: File, configuredPath: String): File {
    val pathFile = File(configuredPath)
    return if (pathFile.isAbsolute) {
        pathFile
    } else {
        rootDir.resolve(configuredPath)
    }
}

fun syncPlatformBundleFromDirectory(
    bundlesRoot: File,
    platform: String,
    nativeOutputRoot: File,
): String? {
    val sourcePlatformDir = bundlesRoot.resolve(platform)
    if (!sourcePlatformDir.isDirectory) {
        return "$platform: platform directory missing at ${sourcePlatformDir.path}"
    }

    val expectedLibraryName = expectedNativeLibraryName(platform)
    val requiredEntries = listOf(expectedLibraryName, "boxlite-shim", "boxlite-guest", "runtime")
    val missingEntries = requiredEntries.filter { entry ->
        !sourcePlatformDir.resolve(entry).exists()
    }
    if (missingEntries.isNotEmpty()) {
        return "$platform: missing required entries ${missingEntries.joinToString(", ")}"
    }

    val runtimeSourceDir = sourcePlatformDir.resolve("runtime")
    val runtimeSourceFiles = runtimeSourceDir.listFiles()
        ?.filter { it.isFile && it.name != "index.txt" }
        ?.sortedBy { it.name }
        .orEmpty()
    if (runtimeSourceFiles.isEmpty()) {
        return "$platform: runtime directory has no files under ${runtimeSourceDir.path}"
    }

    val targetPlatformDir = nativeOutputRoot.resolve(platform)
    if (targetPlatformDir.exists()) {
        targetPlatformDir.deleteRecursively()
    }
    targetPlatformDir.mkdirs()

    sourcePlatformDir.resolve(expectedLibraryName)
        .copyTo(targetPlatformDir.resolve(expectedLibraryName), overwrite = true)

    copyExecutableFile(
        source = sourcePlatformDir.resolve("boxlite-shim"),
        destination = targetPlatformDir.resolve("boxlite-shim"),
        fileLabel = "shim binary",
    )
    copyExecutableFile(
        source = sourcePlatformDir.resolve("boxlite-guest"),
        destination = targetPlatformDir.resolve("boxlite-guest"),
        fileLabel = "guest binary",
    )

    val runtimeTargetDir = targetPlatformDir.resolve("runtime")
    runtimeTargetDir.mkdirs()
    runtimeSourceFiles.forEach { runtimeFile ->
        runtimeFile.copyTo(runtimeTargetDir.resolve(runtimeFile.name), overwrite = true)
    }

    val runtimeIndex = runtimeTargetDir.resolve("index.txt")
    runtimeIndex.writeText(
        runtimeSourceFiles.joinToString(
            separator = "\n",
            postfix = if (runtimeSourceFiles.isEmpty()) "" else "\n",
        ) { it.name },
        StandardCharsets.UTF_8,
    )

    return null
}

fun copyExecutableFile(source: File, destination: File, fileLabel: String) {
    source.copyTo(destination, overwrite = true)
    if (!destination.setExecutable(true, false)) {
        throw GradleException("Failed to mark $fileLabel as executable: ${destination.path}")
    }
}

fun buildBundleReport(
    bundlesRoot: File,
    strictModeEnabled: Boolean,
    configuredPlatforms: List<String>,
    includedPlatforms: List<String>,
    issues: List<String>,
): String {
    val lines = mutableListOf<String>()
    lines += "nativeBundlesDir=${bundlesRoot.path}"
    lines += "strictMode=$strictModeEnabled"
    lines += "requestedPlatforms=${configuredPlatforms.joinToString(",")}"
    lines += "includedPlatforms=${includedPlatforms.joinToString(",")}"

    if (issues.isEmpty()) {
        lines += "status=ok"
    } else {
        lines += "status=missing_or_incomplete"
        lines += "issues:"
        issues.forEach { issue -> lines += "- $issue" }
    }

    return lines.joinToString(separator = "\n", postfix = "\n")
}

fun resolvePlatformId(osName: String, archName: String): String {
    val os = osName.lowercase(Locale.ROOT)
    val archRaw = archName.lowercase(Locale.ROOT)

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
    val archRaw = archName.lowercase(Locale.ROOT)
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
val defaultNativeBundlePlatforms = listOf("darwin-aarch64", "linux-x86_64", "linux-aarch64")
val configuredNativeBundlesDir = resolvePathFromRoot(
    rootDir = rootProject.projectDir,
    configuredPath = providers.gradleProperty("boxlite.nativeBundlesDir").orNull ?: "dist/native",
)
val configuredNativePlatforms = (
    providers.gradleProperty("boxlite.nativePlatforms").orNull
        ?: defaultNativeBundlePlatforms.joinToString(",")
    ).split(",")
    .map(String::trim)
    .filter(String::isNotEmpty)
    .distinct()
if (configuredNativePlatforms.isEmpty()) {
    throw GradleException("No platforms configured for -Pboxlite.nativePlatforms")
}
val configuredStrictNativePlatforms = parseBooleanProperty(
    propertyName = "boxlite.strictNativePlatforms",
    configuredValue = providers.gradleProperty("boxlite.strictNativePlatforms").orNull,
)
val configuredSyncNativeFromSource = parseBooleanProperty(
    propertyName = "boxlite.syncNativeFromSource",
    configuredValue = providers.gradleProperty("boxlite.syncNativeFromSource").orNull ?: "true",
)
val nativeBundleOutputDir = layout.buildDirectory.dir("generated/native-bundles")
val nativeBundleReportOutputFile = layout.buildDirectory.file("reports/native-bundles-report.txt")
val selectedNativeResourcesDir = if (configuredSyncNativeFromSource) {
    layout.buildDirectory.dir("generated/native")
} else {
    nativeBundleOutputDir
}

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

val syncNativeResourcesFromBundles = tasks.register("syncNativeResourcesFromBundles") {
    notCompatibleWithConfigurationCache(
        "Bundle sync task currently uses script-level helper state.",
    )

    if (configuredNativeBundlesDir.exists()) {
        inputs.dir(configuredNativeBundlesDir)
            .withPathSensitivity(PathSensitivity.RELATIVE)
    } else {
        inputs.property("nativeBundlesDirMissing", configuredNativeBundlesDir.path)
    }
    inputs.property("targetPlatforms", configuredNativePlatforms)
    inputs.property("strictMode", configuredStrictNativePlatforms)
    outputs.dir(nativeBundleOutputDir)
    outputs.file(nativeBundleReportOutputFile)

    doLast {
        val outputRoot = nativeBundleOutputDir.get().asFile
        if (outputRoot.exists()) {
            outputRoot.deleteRecursively()
        }

        val nativeOutputRoot = outputRoot.resolve("native")
        nativeOutputRoot.mkdirs()

        val includedPlatforms = mutableListOf<String>()
        val issues = mutableListOf<String>()
        if (!configuredNativeBundlesDir.isDirectory) {
            configuredNativePlatforms.forEach { platform ->
                issues += "$platform: bundle root missing at ${configuredNativeBundlesDir.path}"
            }
        } else {
            configuredNativePlatforms.forEach { platform ->
                val issue = syncPlatformBundleFromDirectory(
                    bundlesRoot = configuredNativeBundlesDir,
                    platform = platform,
                    nativeOutputRoot = nativeOutputRoot,
                )
                if (issue == null) {
                    includedPlatforms += platform
                } else {
                    issues += issue
                }
            }
        }

        val report = nativeBundleReportOutputFile.get().asFile
        report.parentFile.mkdirs()
        report.writeText(
            buildBundleReport(
                bundlesRoot = configuredNativeBundlesDir,
                strictModeEnabled = configuredStrictNativePlatforms,
                configuredPlatforms = configuredNativePlatforms,
                includedPlatforms = includedPlatforms,
                issues = issues,
            ),
            StandardCharsets.UTF_8,
        )

        if (configuredStrictNativePlatforms && issues.isNotEmpty()) {
            throw GradleException(
                "Native bundle validation failed in strict mode. See report: ${report.path}",
            )
        }
    }
}

tasks.named<ProcessResources>("processResources") {
    from(selectedNativeResourcesDir) {
        include("native/**")
    }

    if (configuredSyncNativeFromSource) {
        dependsOn(syncNativeResources)
    } else {
        dependsOn(syncNativeResourcesFromBundles)
    }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.12.0")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
