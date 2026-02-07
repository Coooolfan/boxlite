package io.boxlite.highlevel;

import io.boxlite.ConfigException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** High-level wrapper for executing Python code in a box. */
public final class CodeBox extends SimpleBox {
    /** Default base image used when no custom options are provided. */
    public static final String DEFAULT_IMAGE = CodeBoxOptions.DEFAULT_IMAGE;

    private final String pythonExecutable;
    private final String pipExecutable;

    /** Creates a CodeBox with default options. */
    public CodeBox() {
        this(CodeBoxOptions.builder().build());
    }

    /**
     * Creates a CodeBox with explicit options.
     *
     * @param options code-box options
     */
    public CodeBox(CodeBoxOptions options) {
        super(requireOptions(options).simpleBoxOptions());
        CodeBoxOptions resolved = requireOptions(options);
        this.pythonExecutable = resolved.pythonExecutable();
        this.pipExecutable = resolved.pipExecutable();
    }

    /**
     * Starts the box if it is not already started.
     *
     * @return this instance
     */
    @Override
    public synchronized CodeBox start() {
        super.start();
        return this;
    }

    /**
     * Executes inline Python code via {@code python -c}.
     *
     * @param code python source code
     * @return execution output
     */
    public ExecOutput run(String code) {
        return run(code, Map.of());
    }

    /**
     * Executes inline Python code with environment variables.
     *
     * @param code python source code
     * @param env environment variables
     * @return execution output
     */
    public ExecOutput run(String code, Map<String, String> env) {
        if (code == null || code.isBlank()) {
            throw new ConfigException("code must not be null or blank");
        }
        return exec(pythonExecutable, List.of("-c", code), env);
    }

    /**
     * Alias of {@link #run(String)} for script-content naming.
     *
     * @param scriptContent python source code
     * @return execution output
     */
    public ExecOutput runScriptContent(String scriptContent) {
        return run(scriptContent);
    }

    /**
     * Installs a single Python package via pip.
     *
     * @param packageName package name
     * @return execution output
     */
    public ExecOutput installPackage(String packageName) {
        if (packageName == null || packageName.isBlank()) {
            throw new ConfigException("packageName must not be null or blank");
        }
        return exec(pipExecutable, List.of("install", packageName), Map.of());
    }

    /**
     * Installs multiple Python packages via pip.
     *
     * @param packageNames package names
     * @return execution output
     */
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

    /**
     * Varargs overload of {@link #installPackages(List)}.
     *
     * @param packageNames package names
     * @return execution output
     */
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
