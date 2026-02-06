package io.boxlite.loader;

import io.boxlite.BoxliteException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/** Loads native BoxLite JNI libraries from explicit path or classpath resources. */
public final class NativeLoader {
    static final String NATIVE_LIB_ENV = "BOXLITE_JAVA_NATIVE_LIB";

    private static final AtomicBoolean LOADED = new AtomicBoolean(false);

    private NativeLoader() {
    }

    public static void load() {
        if (LOADED.get()) {
            return;
        }

        synchronized (NativeLoader.class) {
            if (LOADED.get()) {
                return;
            }

            loadInternal();
            LOADED.set(true);
        }
    }

    static String toPlatformId(String osName, String archName) {
        String os = osName.toLowerCase(Locale.ROOT);
        String archRaw = archName.toLowerCase(Locale.ROOT);

        String arch = switch (archRaw) {
            case "aarch64", "arm64" -> "aarch64";
            case "x86_64", "amd64" -> "x86_64";
            default -> throw new BoxliteException("Unsupported architecture: " + archName);
        };

        if (os.contains("mac")) {
            return "darwin-" + arch;
        }
        if (os.contains("linux")) {
            return "linux-" + arch;
        }

        throw new BoxliteException("Unsupported operating system: " + osName);
    }

    static String currentPlatformId() {
        return toPlatformId(System.getProperty("os.name"), System.getProperty("os.arch"));
    }

    private static void loadInternal() {
        String directPath = System.getenv(NATIVE_LIB_ENV);
        if (directPath != null && !directPath.isBlank()) {
            System.load(Path.of(directPath).toAbsolutePath().toString());
            return;
        }

        String platform = currentPlatformId();
        String libraryFileName = System.mapLibraryName("boxlite_java_native");
        String libraryResourcePath = "/native/" + platform + "/" + libraryFileName;

        try {
            Path tempDir = Files.createTempDirectory("boxlite-java-native-");
            tempDir.toFile().deleteOnExit();

            Path extractedLibrary = tempDir.resolve(libraryFileName);
            copyResource(libraryResourcePath, extractedLibrary);
            extractedLibrary.toFile().deleteOnExit();

            extractRuntimeLibraries(tempDir, platform);
            System.load(extractedLibrary.toAbsolutePath().toString());
        } catch (IOException e) {
            throw new BoxliteException("Failed to load native library for platform " + platform, e);
        }
    }

    private static void extractRuntimeLibraries(Path tempDir, String platform) throws IOException {
        String indexResourcePath = "/native/" + platform + "/runtime/index.txt";

        try (InputStream indexStream = NativeLoader.class.getResourceAsStream(indexResourcePath)) {
            if (indexStream == null) {
                return;
            }

            List<String> files;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(indexStream, StandardCharsets.UTF_8))) {
                files = reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .toList();
            }

            if (files.isEmpty()) {
                return;
            }

            Path runtimeDir = tempDir.resolve("runtime");
            Files.createDirectories(runtimeDir);
            runtimeDir.toFile().deleteOnExit();

            for (String fileName : files) {
                String resourcePath = "/native/" + platform + "/runtime/" + fileName;
                Path outFile = runtimeDir.resolve(fileName);
                copyResource(resourcePath, outFile);
                outFile.toFile().deleteOnExit();
            }
        }
    }

    private static void copyResource(String resourcePath, Path destination) throws IOException {
        try (InputStream inputStream = NativeLoader.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new BoxliteException("Missing native resource: " + resourcePath);
            }
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
