# BoxLite Java SDK

Java SDK for BoxLite using JNI to call Rust core directly.

## Modules

- `sdk-core`: public Java API (`Boxlite`, `BoxliteRuntime`, `BoxHandle`, models)
- `sdk-native-loader`: native library loading and ABI checks
- `sdk-highlevel`: high-level wrappers (`SimpleBox` in current phase)
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

## Local Commands

From repository root:

```bash
GRADLE_USER_HOME=.gradle-local ./sdks/java/gradlew -p sdks/java build
GRADLE_USER_HOME=.gradle-local ./sdks/java/gradlew -p sdks/java :samples:smoke:run
GRADLE_USER_HOME=.gradle-local ./sdks/java/gradlew -p sdks/java :samples:simplebox-basic:run
GRADLE_USER_HOME=.gradle-local ./sdks/java/gradlew -p sdks/java :samples:simplebox-reuse:run
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

## Native Override

For local debugging, you can force the loader to use an explicit native library path:

```bash
export BOXLITE_JAVA_NATIVE_LIB=/absolute/path/to/libboxlite_java_native.dylib
```

(Use `.so` on Linux.)
