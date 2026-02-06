# BoxLite Java SDK (Phase 1)

Java SDK for BoxLite using JNI to call Rust core directly.

## Modules

- `sdk-core`: public Java API (`Boxlite`, `BoxliteRuntime`, `BoxHandle`, models)
- `sdk-native-loader`: native library loading and ABI checks
- `sdk-highlevel`: high-level API placeholder for next phases
- `samples/smoke`: local smoke app (runtime + create/get/list/remove/shutdown)
- `native`: Rust JNI crate (`boxlite-java-native`)

## Local Commands

From repository root:

```bash
GRADLE_USER_HOME=.gradle-local ./sdks/java/gradlew -p sdks/java build
GRADLE_USER_HOME=.gradle-local ./sdks/java/gradlew -p sdks/java :samples:smoke:run
```

Run VM integration tests (start/stop/copy) explicitly:

```bash
BOXLITE_JAVA_RUN_VM_TESTS=1 ./sdks/java/gradlew -p sdks/java :sdk-core:test
```

## Native Override

For local debugging, you can force the loader to use an explicit native library path:

```bash
export BOXLITE_JAVA_NATIVE_LIB=/absolute/path/to/libboxlite_java_native.dylib
```

(Use `.so` on Linux.)
