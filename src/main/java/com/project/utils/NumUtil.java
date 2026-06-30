package com.project.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * <h1>数值工具类 — 纯静态方法，线程安全</h1>
 *
 * <p>统一封装数值精度控制、空值安全转换、数字校验等高频操作，
 * 避免项目中散落 {@code Double.parseDouble} 和 {@code String.format} 的重复写法。</p>
 *
 * <h2>核心能力</h2>
 * <table>
 *   <tr><th>分类</th><th>方法</th><th>说明</th></tr>
 *   <tr><td>精度控制</td><td>{@link #round(double, int)}</td><td>四舍五入保留 n 位小数</td></tr>
 *   <tr><td>空值转换</td><td>{@link #toDoubleOrNull(String)}</td><td>转换失败返回 null</td></tr>
 *   <tr><td>空值转换</td><td>{@link #toDoubleOrDefault(String, double)}</td><td>转换失败返回默认值</td></tr>
 *   <tr><td>数字校验</td><td>{@link #isNumber(String)}</td><td>判断字符串是否为合法数字</td></tr>
 *   <tr><td>格式化</td><td>{@link #format(double, int)}</td><td>格式化为固定小数位字符串</td></tr>
 * </table>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 *   // 精度控制
 *   double r = NumUtil.round(3.14159, 2);              // 3.14
 *   double r2 = NumUtil.round(3.14159, 4, RoundingMode.FLOOR); // 3.1415
 *
 *   // 空值安全转换
 *   Double d = NumUtil.toDoubleOrNull("12.34");         // 12.34
 *   Double n = NumUtil.toDoubleOrNull("abc");           // null
 *   double d2 = NumUtil.toDoubleOrDefault("abc", 0.0);  // 0.0
 *
 *   // 数字校验
 *   boolean ok = NumUtil.isNumber("123.45");            // true
 *   boolean nok = NumUtil.isNumber("abc");              // false
 *
 *   // 格式化
 *   String s = NumUtil.format(750.5, 1);               // "750.5"
 *   String s2 = NumUtil.formatPercent(0.1234, 2);      // "12.34%"
 * }</pre>
 *
 * @author Project Team - Utility Layer
 * @version 1.0
 * @since 2026-06-30
 */
public final class NumUtil {

    /** 默认小数位数 */
    public static final int DEFAULT_SCALE = 2;

    /** 温度精度（1 位小数） */
    public static final int TEMP_SCALE = 1;

    /** 百分比精度（2 位小数） */
    public static final int PERCENT_SCALE = 2;

    /** 小数位上限 */
    private static final int MAX_SCALE = 10;

    private NumUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    // ============================================================
    //  精度控制
    // ============================================================

    /**
     * 四舍五入保留指定小数位。
     *
     * <pre>{@code
     * round(3.14159, 2) → 3.14
     * round(3.14159, 0) → 3.0
     * round(750.555, 1) → 750.6
     * }</pre>
     *
     * @param value 原始值
     * @param scale 小数位数（0 ~ 10）
     * @return 四舍五入后的值
     * @throws IllegalArgumentException scale 超出范围
     */
    public static double round(double value, int scale) {
        return round(value, scale, RoundingMode.HALF_UP);
    }

    /**
     * 指定舍入模式保留小数位。
     *
     * @param value        原始值
     * @param scale        小数位数（0 ~ 10）
     * @param roundingMode 舍入模式，如 {@link RoundingMode#FLOOR}、{@link RoundingMode#CEILING}
     * @return 舍入后的值
     * @throws IllegalArgumentException scale 超出范围
     */
    public static double round(double value, int scale, RoundingMode roundingMode) {
        if (scale < 0 || scale > MAX_SCALE) {
            throw new IllegalArgumentException("小数位数必须在 0 ~ " + MAX_SCALE + " 之间，当前: " + scale);
        }
        if (roundingMode == null) {
            roundingMode = RoundingMode.HALF_UP;
        }
        return BigDecimal.valueOf(value).setScale(scale, roundingMode).doubleValue();
    }

    /**
     * 温度值精度控制（保留 1 位小数，四舍五入）。
     * 快捷方法，等价于 {@code round(value, TEMP_SCALE)}。
     *
     * @param temperature 温度值
     * @return 保留 1 位小数的温度
     */
    public static double roundTemp(double temperature) {
        return round(temperature, TEMP_SCALE);
    }

    /**
     * 百分比精度控制（保留 2 位小数，四舍五入）。
     * 快捷方法，等价于 {@code round(value, PERCENT_SCALE)}。
     *
     * @param percent 百分比值
     * @return 保留 2 位小数的百分比
     */
    public static double roundPercent(double percent) {
        return round(percent, PERCENT_SCALE);
    }

    // ============================================================
    //  空值安全转换
    // ============================================================

    /**
     * 字符串 → Double，转换失败返回 null。
     *
     * <pre>{@code
     * toDoubleOrNull("12.34")  → 12.34
     * toDoubleOrNull("abc")    → null
     * toDoubleOrNull(null)     → null
     * toDoubleOrNull("  ")     → null
     * }</pre>
     *
     * @param str 数字字符串，可能为 null
     * @return Double 或 null
     */
    public static Double toDoubleOrNull(String str) {
        if (str == null || str.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(str.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 字符串 → Double，转换失败返回默认值。
     *
     * <pre>{@code
     * toDoubleOrDefault("12.34", 0.0) → 12.34
     * toDoubleOrDefault("abc", 0.0)   → 0.0
     * toDoubleOrDefault(null, -1.0)   → -1.0
     * }</pre>
     *
     * @param str        数字字符串，可能为 null
     * @param defaultVal 默认值
     * @return 转换后的值或默认值
     */
    public static double toDoubleOrDefault(String str, double defaultVal) {
        Double result = toDoubleOrNull(str);
        return result != null ? result : defaultVal;
    }

    /**
     * 字符串 → Integer，转换失败返回 null。
     *
     * @param str 整数字符串，可能为 null
     * @return Integer 或 null
     */
    public static Integer toIntOrNull(String str) {
        if (str == null || str.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 字符串 → Integer，转换失败返回默认值。
     *
     * @param str        整数字符串，可能为 null
     * @param defaultVal 默认值
     * @return 转换后的值或默认值
     */
    public static int toIntOrDefault(String str, int defaultVal) {
        Integer result = toIntOrNull(str);
        return result != null ? result : defaultVal;
    }

    /**
     * 字符串 → Long，转换失败返回 null。
     *
     * @param str 长整数字符串，可能为 null
     * @return Long 或 null
     */
    public static Long toLongOrNull(String str) {
        if (str == null || str.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(str.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 字符串 → Long，转换失败返回默认值。
     *
     * @param str        长整数字符串，可能为 null
     * @param defaultVal 默认值
     * @return 转换后的值或默认值
     */
    public static long toLongOrDefault(String str, long defaultVal) {
        Long result = toLongOrNull(str);
        return result != null ? result : defaultVal;
    }

    // ============================================================
    //  数字校验
    // ============================================================

    /**
     * 判断字符串是否为合法数字（整数或小数，含正负号）。
     *
     * <pre>{@code
     * isNumber("123")     → true
     * isNumber("-12.34")  → true
     * isNumber("+5.0")    → true
     * isNumber("1e5")     → true  (科学计数法)
     * isNumber("abc")     → false
     * isNumber(null)      → false
     * isNumber("")        → false
     * }</pre>
     *
     * @param str 待校验字符串
     * @return true 表示合法数字
     */
    public static boolean isNumber(String str) {
        if (str == null || str.isBlank()) {
            return false;
        }
        try {
            Double.parseDouble(str.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 判断字符串是否为合法整数。
     *
     * @param str 待校验字符串
     * @return true 表示合法整数
     */
    public static boolean isInteger(String str) {
        if (str == null || str.isBlank()) {
            return false;
        }
        try {
            Integer.parseInt(str.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 判断值是否在 [min, max] 闭区间内。
     *
     * @param value 待判断值
     * @param min   下限
     * @param max   上限
     * @return true 表示在区间内
     */
    public static boolean inRange(double value, double min, double max) {
        return value >= min && value <= max;
    }

    // ============================================================
    //  格式化
    // ============================================================

    /**
     * 格式化为固定小数位的字符串。
     *
     * <pre>{@code
     * format(750.5, 1)    → "750.5"
     * format(3.14159, 2)  → "3.14"
     * format(3.0, 2)      → "3.00"
     * }</pre>
     *
     * @param value 数值
     * @param scale 小数位数
     * @return 格式化字符串
     */
    public static String format(double value, int scale) {
        if (scale < 0 || scale > MAX_SCALE) {
            throw new IllegalArgumentException("小数位数必须在 0 ~ " + MAX_SCALE + " 之间");
        }
        // 构建格式模式，如 "#.00" 或 "#.0"
        StringBuilder pattern = new StringBuilder("0");
        if (scale > 0) {
            pattern.append(".");
            pattern.append("0".repeat(scale));
        }
        return new DecimalFormat(pattern.toString()).format(value);
    }

    /**
     * 格式化为百分比字符串。
     *
     * <pre>{@code
     * formatPercent(0.1234, 2)  → "12.34%"
     * formatPercent(0.5, 1)     → "50.0%"
     * formatPercent(1.0, 0)     → "100%"
     * }</pre>
     *
     * @param fraction 小数形式的比例（如 0.1234 表示 12.34%）
     * @param scale    小数位数
     * @return 百分比字符串，如 "12.34%"
     */
    public static String formatPercent(double fraction, int scale) {
        return format(fraction * 100.0, scale) + "%";
    }

    /**
     * 格式化为温度字符串（含单位）。
     *
     * <pre>{@code
     * formatTemp(750.5)  → "750.5 °C"
     * formatTemp(32.0)   → "32.0 °C"
     * }</pre>
     *
     * @param temperature 温度值
     * @return 温度字符串
     */
    public static String formatTemp(double temperature) {
        return format(temperature, TEMP_SCALE) + " °C";
    }

    /**
     * 格式化为质量字符串（含单位）。
     *
     * @param weight 质量值（g）
     * @return 质量字符串，如 "50.0 g"
     */
    public static String formatWeight(double weight) {
        return format(weight, DEFAULT_SCALE) + " g";
    }

    // ============================================================
    //  便捷计算
    // ============================================================

    /**
     * 计算百分比（值 / 总量 × 100），保留指定小数位。
     *
     * <pre>{@code
     * calcPercent(5.0, 50.0, 2)  → 10.00
     * calcPercent(1.0, 3.0, 1)   → 33.3
     * }</pre>
     *
     * @param part  部分值
     * @param total 总量
     * @param scale 小数位数
     * @return 百分比数值
     * @throws IllegalArgumentException total 为 0
     */
    public static double calcPercent(double part, double total, int scale) {
        if (total == 0.0) {
            throw new IllegalArgumentException("总量不能为 0");
        }
        return round((part / total) * 100.0, scale);
    }

    /**
     * 安全除法，除数为 0 时返回默认值。
     *
     * @param numerator   分子
     * @param denominator 分母
     * @param defaultVal  分母为 0 时的默认值
     * @return 商或默认值
     */
    public static double safeDivide(double numerator, double denominator, double defaultVal) {
        if (denominator == 0.0) {
            return defaultVal;
        }
        return numerator / denominator;
    }

    /**
     * 比较两个 double 是否相等（容差比较）。
     *
     * @param a         值1
     * @param b         值2
     * @param tolerance 容差，如 0.001
     * @return true 表示两值在容差范围内相等
     */
    public static boolean equalsWithTolerance(double a, double b, double tolerance) {
        return Math.abs(a - b) <= tolerance;
    }
}