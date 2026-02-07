package io.boxlite;

import io.boxlite.loader.NativeBindings;

/** Java SDK 运行时入口。 */
public final class Boxlite {
    private Boxlite() {
    }

    /**
     * 返回原生 SDK 版本号。
     *
     * @return 版本号字符串。
     */
    public static String version() {
        return NativeBindings.version();
    }

    /**
     * 使用默认选项创建新的运行时句柄。
     *
     * @return 运行时句柄。
     */
    public static BoxliteRuntime newRuntime() {
        return BoxliteRuntime.create(Options.defaults());
    }

    /**
     * 使用自定义选项创建新的运行时句柄。
     *
     * @param options 运行时选项。
     * @return 运行时句柄。
     */
    public static BoxliteRuntime newRuntime(Options options) {
        return BoxliteRuntime.create(options);
    }

    /**
     * 返回进程级默认运行时句柄。
     *
     * @return 默认运行时句柄。
     */
    public static BoxliteRuntime defaultRuntime() {
        return BoxliteRuntime.defaultRuntime();
    }

    /**
     * 初始化进程级默认运行时。
     *
     * <p>请在首次调用 {@link #defaultRuntime()} 前执行。
     *
     * @param options 默认运行时选项。
     */
    public static void initDefaultRuntime(Options options) {
        BoxliteRuntime.initDefaultRuntime(options);
    }
}
