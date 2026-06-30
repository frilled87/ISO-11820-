package com.iso11820.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <h1>日志工具类 — 线程安全静态门面</h1>
 *
 * <p>封装 SLF4J/Logback，提供统一的日志记录入口。
 * 所有方法均为静态方法，线程安全，可直接在任意线程中调用。</p>
 *
 * <h2>日志级别说明</h2>
 * <table>
 *   <tr><th>级别</th><th>用途</th></tr>
 *   <tr><td>TRACE</td><td>最细粒度追踪，仅开发调试</td></tr>
 *   <tr><td>DEBUG</td><td>调试信息，生产环境通常关闭</td></tr>
 *   <tr><td>INFO</td><td>关键业务流程节点</td></tr>
 *   <tr><td>WARN</td><td>可恢复的异常或退化行为</td></tr>
 *   <tr><td>ERROR</td><td>需要人工介入的错误</td></tr>
 * </table>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 *   // 使用当前类 logger
 *   LogUtil.info("服务启动成功，端口: {}", port);
 *   LogUtil.error("导出失败", exception);
 *
 *   // 使用自定义 logger
 *   LogUtil.getLogger("EXPORT").info("Excel 导出完成: {} 行", rowCount);
 * }</pre>
 *
 * @author Project Team - Utility Layer
 * @version 1.0
 * @since 2026-06-30
 */
public final class LogUtil {

    /** 默认 logger 名称 */
    private static final String DEFAULT_NAME = "com.iso11820";

    /** 默认 logger */
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(DEFAULT_NAME);

    /** 私有构造，禁止实例化 */
    private LogUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    // ============================================================
    //  获取 Logger
    // ============================================================

    /**
     * 获取指定名称的 logger。
     *
     * <pre>{@code
     * Logger exportLogger = LogUtil.getLogger("EXPORT");
     * exportLogger.info("Excel 导出成功");
     * }</pre>
     *
     * @param name logger 名称，建议全大写，如 "EXPORT"、"CSV"、"DAO"
     * @return SLF4J Logger 实例
     * @throws IllegalArgumentException 如果 name 为 null 或空白
     */
    public static Logger getLogger(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Logger 名称不能为 null 或空白");
        }
        return LoggerFactory.getLogger(name);
    }

    /**
     * 获取指定类的 logger。
     *
     * @param clazz 类对象，不能为 null
     * @return SLF4J Logger 实例
     * @throws IllegalArgumentException 如果 clazz 为 null
     */
    public static Logger getLogger(Class<?> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("clazz 不能为 null");
        }
        return LoggerFactory.getLogger(clazz);
    }

    // ============================================================
    //  TRACE
    // ============================================================

    /** @see Logger#trace(String) */
    public static void trace(String msg) {
        DEFAULT_LOGGER.trace(msg);
    }

    /** @see Logger#trace(String, Object...) */
    public static void trace(String format, Object... arguments) {
        DEFAULT_LOGGER.trace(format, arguments);
    }

    // ============================================================
    //  DEBUG
    // ============================================================

    /** @see Logger#debug(String) */
    public static void debug(String msg) {
        DEFAULT_LOGGER.debug(msg);
    }

    /** @see Logger#debug(String, Object...) */
    public static void debug(String format, Object... arguments) {
        DEFAULT_LOGGER.debug(format, arguments);
    }

    // ============================================================
    //  INFO
    // ============================================================

    /** @see Logger#info(String) */
    public static void info(String msg) {
        DEFAULT_LOGGER.info(msg);
    }

    /** @see Logger#info(String, Object...) */
    public static void info(String format, Object... arguments) {
        DEFAULT_LOGGER.info(format, arguments);
    }

    // ============================================================
    //  WARN
    // ============================================================

    /** @see Logger#warn(String) */
    public static void warn(String msg) {
        DEFAULT_LOGGER.warn(msg);
    }

    /** @see Logger#warn(String, Object...) */
    public static void warn(String format, Object... arguments) {
        DEFAULT_LOGGER.warn(format, arguments);
    }

    /**
     * 记录警告日志，附带异常堆栈。
     *
     * @param msg 消息
     * @param t   异常
     */
    public static void warn(String msg, Throwable t) {
        DEFAULT_LOGGER.warn(msg, t);
    }

    // ============================================================
    //  ERROR
    // ============================================================

    /** @see Logger#error(String) */
    public static void error(String msg) {
        DEFAULT_LOGGER.error(msg);
    }

    /** @see Logger#error(String, Object...) */
    public static void error(String format, Object... arguments) {
        DEFAULT_LOGGER.error(format, arguments);
    }

    /**
     * 记录错误日志，附带异常堆栈。
     * 这是最常用的错误日志方法。
     *
     * @param msg 错误描述
     * @param t   异常对象
     */
    public static void error(String msg, Throwable t) {
        DEFAULT_LOGGER.error(msg, t);
    }

    // ============================================================
    //  便捷方法
    // ============================================================

    /**
     * 记录方法入口（DEBUG 级别）。
     *
     * <pre>{@code
     * public void exportReport(String path) {
     *     LogUtil.enter("exportReport", "path={}", path);
     *     // ...
     * }
     * }</pre>
     *
     * @param methodName 方法名
     * @param args       参数描述
     */
    public static void enter(String methodName, Object... args) {
        if (DEFAULT_LOGGER.isDebugEnabled()) {
            DEFAULT_LOGGER.debug("→ 进入 {}({})", methodName,
                    args.length > 0 ? String.join(", ", java.util.Arrays.stream(args)
                            .map(String::valueOf).toArray(String[]::new)) : "");
        }
    }

    /**
     * 记录方法出口（DEBUG 级别）。
     *
     * @param methodName 方法名
     * @param result     返回值描述
     */
    public static void exit(String methodName, Object result) {
        if (DEFAULT_LOGGER.isDebugEnabled()) {
            DEFAULT_LOGGER.debug("← 退出 {} → {}", methodName, result);
        }
    }

    /**
     * 记录方法执行耗时（INFO 级别）。
     *
     * <pre>{@code
     * long start = System.currentTimeMillis();
     * // ... 业务逻辑 ...
     * LogUtil.elapsed("exportReport", start);
     * }</pre>
     *
     * @param methodName 方法名
     * @param startMillis 开始时间戳（毫秒）
     */
    public static void elapsed(String methodName, long startMillis) {
        long elapsed = System.currentTimeMillis() - startMillis;
        DEFAULT_LOGGER.info("{} 耗时: {} ms", methodName, elapsed);
    }
}