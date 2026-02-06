package io.boxlite.highlevel;

import io.boxlite.ConfigException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** High-level wrapper for executing Python code in a box. */
public final class CodeBox extends SimpleBox {
    public static final String DEFAULT_IMAGE = CodeBoxOptions.DEFAULT_IMAGE;

    private final String pythonExecutable;
    private final String pipExecutable;

    public CodeBox() {
        this(CodeBoxOptions.builder().build());
    }

    public CodeBox(CodeBoxOptions options) {
        super(requireOptions(options).simpleBoxOptions());
        CodeBoxOptions resolved = requireOptions(options);
        this.pythonExecutable = resolved.pythonExecutable();
        this.pipExecutable = resolved.pipExecutable();
    }

    @Override
    public synchronized CodeBox start() {
        super.start();
        return this;
    }

    /** Execute inline Python code via {@code python -c}. */
    public ExecOutput run(String code) {
        return run(code, Map.of());
    }

    /** Execute inline Python code with additional environment variables. */
    public ExecOutput run(String code, Map<String, String> env) {
        if (code == null || code.isBlank()) {
            throw new ConfigException("code must not be null or blank");
        }
        return exec(pythonExecutable, List.of("-c", code), env);
    }

    /** Alias for executing script content directly. */
    public ExecOutput runScriptContent(String scriptContent) {
        return run(scriptContent);
    }

    /** Install a single Python package via pip. */
    public ExecOutput installPackage(String packageName) {
        if (packageName == null || packageName.isBlank()) {
            throw new ConfigException("packageName must not be null or blank");
        }
        return exec(pipExecutable, List.of("install", packageName), Map.of());
    }

    /** Install multiple Python packages via pip. */
    public ExecOutput installPackages(List<String> packageNames) {
        Objects.requireNonNull(packageNames, "packageNames must not be null");
        if (packageNames.isEmpty()) {
            throw new ConfigException("packageNames must not be empty");
        }

        ArrayList<String> args = new ArrayList<>();
        args.add("install");
        for (String packageName : packageNames) {
            if (packageName == null || packageName.isBlank()) {
                throw new ConfigException("packageNames must not contain null or blank values");
            }
            args.add(packageName);
        }
        return exec(pipExecutable, args, Map.of());
    }

    public ExecOutput installPackages(String... packageNames) {
        if (packageNames == null) {
            throw new ConfigException("packageNames must not be null");
        }
        return installPackages(Arrays.asList(packageNames));
    }

    private static CodeBoxOptions requireOptions(CodeBoxOptions options) {
        if (options == null) {
            throw new ConfigException("options must not be null");
        }
        return options;
    }
}
