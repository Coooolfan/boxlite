package io.boxlite.highlevel;

import io.boxlite.ConfigException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 在盒子中执行 Python 代码的高层封装。 */
public final class CodeBox extends SimpleBox {
    /** 未提供自定义选项时使用的默认基础镜像。 */
    public static final String DEFAULT_IMAGE = CodeBoxOptions.DEFAULT_IMAGE;

    private final String pythonExecutable;
    private final String pipExecutable;

    /** 使用默认选项创建 CodeBox。 */
    public CodeBox() {
        this(CodeBoxOptions.builder().build());
    }

    /**
     * 使用显式选项创建 CodeBox。
     *
     * @param options CodeBox 选项。
     */
    public CodeBox(CodeBoxOptions options) {
        super(requireOptions(options).simpleBoxOptions());
        CodeBoxOptions resolved = requireOptions(options);
        this.pythonExecutable = resolved.pythonExecutable();
        this.pipExecutable = resolved.pipExecutable();
    }

    /**
     * 如果盒子尚未启动，则启动盒子。
     *
     * @return 当前实例。
     */
    @Override
    public synchronized CodeBox start() {
        super.start();
        return this;
    }

    /**
     * 通过 {@code python -c} 执行内联 Python 代码。
     *
     * @param code Python 源码。
     * @return 执行输出。
     */
    public ExecOutput run(String code) {
        return run(code, Map.of());
    }

    /**
     * 执行内联 Python 代码，并附加环境变量。
     *
     * @param code Python 源码。
     * @param env 环境变量。
     * @return 执行输出。
     */
    public ExecOutput run(String code, Map<String, String> env) {
        if (code == null || code.isBlank()) {
            throw new ConfigException("code must not be null or blank");
        }
        return exec(pythonExecutable, List.of("-c", code), env);
    }

    /**
     * {@link #run(String)} 的脚本内容别名。
     *
     * @param scriptContent Python 源码。
     * @return 执行输出。
     */
    public ExecOutput runScriptContent(String scriptContent) {
        return run(scriptContent);
    }

    /**
     * 通过 pip 安装单个 Python 包。
     *
     * @param packageName 包名。
     * @return 执行输出。
     */
    public ExecOutput installPackage(String packageName) {
        if (packageName == null || packageName.isBlank()) {
            throw new ConfigException("packageName must not be null or blank");
        }
        return exec(pipExecutable, List.of("install", packageName), Map.of());
    }

    /**
     * 通过 pip 安装多个 Python 包。
     *
     * @param packageNames 包名列表。
     * @return 执行输出。
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
     * {@link #installPackages(List)} 的可变参数重载。
     *
     * @param packageNames 包名列表。
     * @return 执行输出。
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
