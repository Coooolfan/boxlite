# BoxLite Java SDK

Java SDK for BoxLite using JNI to call Rust core directly.

## Modules

- `sdk-core`: public Java API (`Boxlite`, `BoxliteRuntime`, `BoxHandle`, models)
- `sdk-native-loader`: native library loading and ABI checks
- `sdk-highlevel`: high-level wrappers (`SimpleBox`, `CodeBox`)
- `samples/smoke`: local smoke app (runtime + create/get/list/remove/shutdown)
- `native`: Rust JNI crate (`boxlite-java-native`)

Phase 2 adds execution APIs in `sdk-core`:

- `BoxHandle.exec(ExecCommand)`
- `ExecutionHandle.stdinWrite/stdinClose/stdoutNextLine/stderrNextLine/waitFor/kill/resizeTty`
- `ExecResult`

Current high-level API in `sdk-highlevel`:

- `SimpleBox`
- `SimpleBoxOptions`
- `ExecOutput`
- `CodeBox`
- `CodeBoxOptions`

Example:

```java
import io.boxlite.BoxOptions;
import io.boxlite.highlevel.SimpleBox;
import io.boxlite.highlevel.SimpleBoxOptions;

try (SimpleBox box = new SimpleBox(
        SimpleBoxOptions.builder()
            .name("java-simplebox-demo")
            .boxOptions(BoxOptions.defaults())
            .build()
    ).start()) {
    var result = box.exec("sh", java.util.List.of("-lc", "echo hello"));
    System.out.println(result.stdout());
}
```

```java
import io.boxlite.highlevel.CodeBox;

try (CodeBox box = new CodeBox().start()) {
    var result = box.run("print('hello-from-codebox')");
    System.out.println(result.stdout());
}
```

## Local Commands

From repository root:

```bash
GRADLE_USER_HOME=.gradle-local ./sdks/java/gradlew -p sdks/java build
GRADLE_USER_HOME=.gradle-local ./sdks/java/gradlew -p sdks/java :samples:smoke:run
GRADLE_USER_HOME=.gradle-local ./sdks/java/gradlew -p sdks/java :samples:simplebox-basic:run
GRADLE_USER_HOME=.gradle-local ./sdks/java/gradlew -p sdks/java :samples:simplebox-reuse:run
GRADLE_USER_HOME=.gradle-local ./sdks/java/gradlew -p sdks/java :samples:codebox-basic:run
```

Note: run sample apps one at a time. BoxLite enforces a lock per `BOXLITE_HOME` directory.

Run full tests (unit + VM integration) locally:

```bash
./sdks/java/gradlew -p sdks/java test
```

`sdk-core` smoke tests now run VM scenarios by default. On macOS, local prerequisites are:
- Apple Silicon (`os.arch=aarch64`)
- Hypervisor.framework available (`sysctl -n kern.hv_support` returns `1`)
- Local image cache available in `~/.boxlite/images` (tests reuse cached images to avoid registry rate limits)

In restricted environments, keep using a writable Gradle cache path:

```bash
GRADLE_USER_HOME=.gradle-local ./sdks/java/gradlew -p sdks/java test
```

## Multi-platform Fat Jar (local)

Build a single `sdk-highlevel` fat Jar with bundled native resources for:
- `darwin-aarch64`
- `linux-x86_64`
- `linux-aarch64`

Default native bundle input directory:
- `sdks/java/dist/native/<platform>/`

Each platform directory should include:
- `libboxlite_java_native.dylib` (macOS) or `libboxlite_java_native.so` (Linux)
- `boxlite-shim`
- `boxlite-guest`
- `runtime/` (runtime helper files; `index.txt` is generated during packaging)

Build fat Jar:

```bash
GRADLE_USER_HOME=.gradle-local ./sdks/java/gradlew -p sdks/java fatJarAllPlatforms
```

Output:
- `sdks/java/sdk-highlevel/build/libs/boxlite-java-highlevel-allplatforms-<version>.jar`
- `sdks/java/sdk-highlevel/build/libs/boxlite-java-highlevel-allplatforms-<version>-sources.jar`

Default behavior is non-strict for local development:
- Missing or incomplete platform bundles are skipped
- A report is written to `sdks/java/sdk-native-loader/build/reports/native-bundles-report.txt`

Strict mode (recommended for CI):

```bash
GRADLE_USER_HOME=.gradle-local ./sdks/java/gradlew -p sdks/java fatJarAllPlatforms -Pboxlite.strictNativePlatforms=true
```

Optional properties:
- `-Pboxlite.nativeBundlesDir=<path>` (default: `dist/native`, relative to `sdks/java/`)
- `-Pboxlite.nativePlatforms=darwin-aarch64,linux-x86_64,linux-aarch64`
- `-Pboxlite.syncNativeFromSource=true|false` (default: `true`; set `false` to make `processResources` use bundle sync instead of local Cargo builds)

## Native Override

For local debugging, you can force the loader to use an explicit native library path:

```bash
export BOXLITE_JAVA_NATIVE_LIB=/absolute/path/to/libboxlite_java_native.dylib
```

(Use `.so` on Linux.)
