package com.project.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

/**
 * <h1>日期时间工具类 — 纯静态方法，线程安全</h1>
 *
 * <p>统一封装项目中所有日期时间操作，消除重复的 {@code SimpleDateFormat} 和
 * {@code Calendar} 写法。基于 {@code java.time} 包（Java 8+），所有
 * {@link DateTimeFormatter} 实例均为不可变对象，天然线程安全。</p>
 *
 * <h2>常用格式常量</h2>
 * <table>
 *   <tr><th>常量</th><th>示例</th></tr>
 *   <tr><td>{@link #PATTERN_STANDARD}</td><td>2026-06-30 14:30:00</td></tr>
 *   <tr><td>{@link #PATTERN_DATE}</td><td>2026-06-30</td></tr>
 *   <tr><td>{@link #PATTERN_TIME}</td><td>14:30:00</td></tr>
 *   <tr><td>{@link #PATTERN_COMPACT}</td><td>20260630143000</td></tr>
 *   <tr><td>{@link #PATTERN_TESTID}</td><td>20260630-143000</td></tr>
 * </table>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 *   // 格式化
 *   String now = DateUtil.now();                          // "2026-06-30 14:30:00"
 *   String today = DateUtil.today();                      // "2026-06-30"
 *
 *   // 生成试验 ID
 *   String testId = DateUtil.generateTestId();            // "20260630-143000"
 *
 *   // 时间戳转换
 *   long ts = DateUtil.toTimestamp("2026-06-30 14:30:00"); // 毫秒时间戳
 *   String dt = DateUtil.fromTimestamp(ts);                // "2026-06-30 14:30:00"
 *
 *   // 时间差计算
 *   long seconds = DateUtil.elapsedSeconds(startTime, endTime);  // 秒差
 *   String readable = DateUtil.readableDuration(3661);            // "1小时0分1秒"
 * }</pre>
 *
 * @author Project Team - Utility Layer
 * @version 1.0
 * @since 2026-06-30
 */
public final class DateUtil {

    // ============================================================
    //  格式常量（DateTimeFormatter 线程安全）
    // ============================================================

    /** 标准日期时间格式：yyyy-MM-dd HH:mm:ss */
    public static final String PATTERN_STANDARD = "yyyy-MM-dd HH:mm:ss";

    /** 纯日期格式：yyyy-MM-dd */
    public static final String PATTERN_DATE = "yyyy-MM-dd";

    /** 纯时间格式：HH:mm:ss */
    public static final String PATTERN_TIME = "HH:mm:ss";

    /** 紧凑格式（无分隔符）：yyyyMMddHHmmss */
    public static final String PATTERN_COMPACT = "yyyyMMddHHmmss";

    /** 试验 ID 格式：yyyyMMdd-HHmmss */
    public static final String PATTERN_TESTID = "yyyyMMdd-HHmmss";

    /** 中文日期格式：yyyy年MM月dd日 HH:mm:ss */
    public static final String PATTERN_CN = "yyyy年MM月dd日 HH:mm:ss";

    // ============================================================
    //  Formatter 缓存（不可变，可安全共享）
    // ============================================================

    /** 标准日期时间 */
    private static final DateTimeFormatter FMT_STANDARD = DateTimeFormatter.ofPattern(PATTERN_STANDARD);

    /** 纯日期 */
    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern(PATTERN_DATE);

    /** 纯时间 */
    private static final DateTimeFormatter FMT_TIME = DateTimeFormatter.ofPattern(PATTERN_TIME);

    /** 紧凑格式 */
    private static final DateTimeFormatter FMT_COMPACT = DateTimeFormatter.ofPattern(PATTERN_COMPACT);

    /** 试验 ID */
    private static final DateTimeFormatter FMT_TESTID = DateTimeFormatter.ofPattern(PATTERN_TESTID);

    /** 中文格式 */
    private static final DateTimeFormatter FMT_CN = DateTimeFormatter.ofPattern(PATTERN_CN);

    /** 默认时区 */
    private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();

    // ============================================================
    //  禁止实例化
    // ============================================================

    private DateUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    // ============================================================
    //  获取当前时间
    // ============================================================

    /**
     * 获取当前日期时间字符串（标准格式）。
     *
     * @return 如 {@code "2026-06-30 14:30:00"}
     */
    public static String now() {
        return LocalDateTime.now().format(FMT_STANDARD);
    }

    /**
     * 获取当前日期时间字符串（指定格式）。
     *
     * @param pattern 格式字符串，如 {@code "yyyy/MM/dd"}
     * @return 格式化后的当前时间
     * @throws IllegalArgumentException pattern 为 null 或空白
     */
    public static String now(String pattern) {
        if (pattern == null || pattern.isBlank()) {
            throw new IllegalArgumentException("日期格式不能为空");
        }
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 获取当前日期（纯日期格式）。
     *
     * @return 如 {@code "2026-06-30"}
     */
    public static String today() {
        return LocalDate.now().format(FMT_DATE);
    }

    /**
     * 获取当前时间（纯时间格式）。
     *
     * @return 如 {@code "14:30:00"}
     */
    public static String currentTime() {
        return LocalDateTime.now().format(FMT_TIME);
    }

    /**
     * 生成试验 ID（yyyyMMdd-HHmmss）。
     * 用于 testmaster 表的 testid 字段。
     *
     * @return 如 {@code "20260630-143000"}
     */
    public static String generateTestId() {
        return LocalDateTime.now().format(FMT_TESTID);
    }

    // ============================================================
    //  格式化
    // ============================================================

    /**
     * 将 LocalDateTime 格式化为标准字符串。
     *
     * @param dateTime 日期时间对象，不能为 null
     * @return 格式化的字符串
     * @throws IllegalArgumentException dateTime 为 null
     */
    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) {
            throw new IllegalArgumentException("dateTime 不能为 null");
        }
        return dateTime.format(FMT_STANDARD);
    }

    /**
     * 将 LocalDateTime 格式化为指定格式字符串。
     *
     * @param dateTime 日期时间对象，不能为 null
     * @param pattern  格式字符串，如 {@code "yyyy/MM/dd HH:mm"}
     * @return 格式化的字符串
     * @throws IllegalArgumentException dateTime 或 pattern 为 null/空白
     */
    public static String format(LocalDateTime dateTime, String pattern) {
        if (dateTime == null) {
            throw new IllegalArgumentException("dateTime 不能为 null");
        }
        if (pattern == null || pattern.isBlank()) {
            throw new IllegalArgumentException("日期格式不能为空");
        }
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 将 LocalDate 格式化为日期字符串。
     *
     * @param date 日期对象，不能为 null
     * @return 如 {@code "2026-06-30"}
     * @throws IllegalArgumentException date 为 null
     */
    public static String formatDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("date 不能为 null");
        }
        return date.format(FMT_DATE);
    }

    // ============================================================
    //  解析
    // ============================================================

    /**
     * 解析标准格式日期时间字符串。
     *
     * @param dateTimeStr 如 {@code "2026-06-30 14:30:00"}
     * @return LocalDateTime 对象
     * @throws IllegalArgumentException dateTimeStr 为 null/空白
     * @throws RuntimeException 格式不匹配时抛出（含原始异常信息）
     */
    public static LocalDateTime parse(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) {
            throw new IllegalArgumentException("日期时间字符串不能为空");
        }
        try {
            return LocalDateTime.parse(dateTimeStr, FMT_STANDARD);
        } catch (Exception e) {
            throw new RuntimeException("日期解析失败: " + dateTimeStr + "，期望格式: " + PATTERN_STANDARD, e);
        }
    }

    /**
     * 解析指定格式的日期时间字符串。
     *
     * @param dateTimeStr 日期时间字符串
     * @param pattern     格式，如 {@code "yyyyMMdd"}
     * @return LocalDateTime 对象
     * @throws IllegalArgumentException 参数为 null/空白
     */
    public static LocalDateTime parse(String dateTimeStr, String pattern) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) {
            throw new IllegalArgumentException("日期时间字符串不能为空");
        }
        if (pattern == null || pattern.isBlank()) {
            throw new IllegalArgumentException("日期格式不能为空");
        }
        try {
            return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern(pattern));
        } catch (Exception e) {
            throw new RuntimeException("日期解析失败: " + dateTimeStr + "，期望格式: " + pattern, e);
        }
    }

    /**
     * 解析纯日期字符串。
     *
     * @param dateStr 如 {@code "2026-06-30"}
     * @return LocalDate 对象
     * @throws IllegalArgumentException dateStr 为 null/空白
     */
    public static LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            throw new IllegalArgumentException("日期字符串不能为空");
        }
        try {
            return LocalDate.parse(dateStr, FMT_DATE);
        } catch (Exception e) {
            throw new RuntimeException("日期解析失败: " + dateStr + "，期望格式: " + PATTERN_DATE, e);
        }
    }

    /**
     * 尝试解析日期时间字符串，失败时返回 null 而不抛异常。
     *
     * @param dateTimeStr 日期时间字符串
     * @param pattern     格式
     * @return LocalDateTime 或 null
     */
    public static LocalDateTime parseOrNull(String dateTimeStr, String pattern) {
        try {
            return parse(dateTimeStr, pattern);
        } catch (Exception e) {
            return null;
        }
    }

    // ============================================================
    //  时间戳转换
    // ============================================================

    /**
     * LocalDateTime → 毫秒时间戳。
     *
     * @param dateTime 日期时间，不能为 null
     * @return 毫秒时间戳（Unix epoch）
     */
    public static long toTimestamp(LocalDateTime dateTime) {
        if (dateTime == null) {
            throw new IllegalArgumentException("dateTime 不能为 null");
        }
        return dateTime.atZone(DEFAULT_ZONE).toInstant().toEpochMilli();
    }

    /**
     * 日期时间字符串 → 毫秒时间戳（标准格式）。
     *
     * @param dateTimeStr 如 {@code "2026-06-30 14:30:00"}
     * @return 毫秒时间戳
     */
    public static long toTimestamp(String dateTimeStr) {
        return toTimestamp(parse(dateTimeStr));
    }

    /**
     * 毫秒时间戳 → LocalDateTime。
     *
     * @param timestampMs 毫秒时间戳
     * @return LocalDateTime 对象
     */
    public static LocalDateTime fromTimestamp(long timestampMs) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestampMs), DEFAULT_ZONE);
    }

    /**
     * 毫秒时间戳 → 标准格式字符串。
     *
     * @param timestampMs 毫秒时间戳
     * @return 如 {@code "2026-06-30 14:30:00"}
     */
    public static String fromTimestampFormatted(long timestampMs) {
        return fromTimestamp(timestampMs).format(FMT_STANDARD);
    }

    /**
     * 获取当前毫秒时间戳。
     *
     * @return 当前毫秒时间戳
     */
    public static long currentTimestamp() {
        return System.currentTimeMillis();
    }

    // ============================================================
    //  时间差计算
    // ============================================================

    /**
     * 计算两个 LocalDateTime 之间的秒数差。
     *
     * @param start 开始时间，不能为 null
     * @param end   结束时间，不能为 null
     * @return 秒差（end - start），可能为负数
     */
    public static long elapsedSeconds(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("开始时间和结束时间不能为 null");
        }
        return ChronoUnit.SECONDS.between(start, end);
    }

    /**
     * 计算两个时间戳之间的秒数差。
     *
     * @param startMs 开始毫秒时间戳
     * @param endMs   结束毫秒时间戳
     * @return 秒差（endMs - startMs）
     */
    public static long elapsedSeconds(long startMs, long endMs) {
        return TimeUnit.MILLISECONDS.toSeconds(endMs - startMs);
    }

    /**
     * 计算两个 LocalDateTime 之间的毫秒数差。
     *
     * @param start 开始时间，不能为 null
     * @param end   结束时间，不能为 null
     * @return 毫秒差（end - start）
     */
    public static long elapsedMillis(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("开始时间和结束时间不能为 null");
        }
        return ChronoUnit.MILLIS.between(start, end);
    }

    // ============================================================
    //  可读时长
    // ============================================================

    /**
     * 将秒数转换为可读时长字符串。
     *
     * <pre>{@code
     * readableDuration(0)      → "0秒"
     * readableDuration(65)     → "1分5秒"
     * readableDuration(3661)   → "1小时1分1秒"
     * readableDuration(90061)  → "1天1小时1分1秒"
     * }</pre>
     *
     * @param totalSeconds 总秒数（≥ 0）
     * @return 可读的时长字符串
     * @throws IllegalArgumentException totalSeconds 为负数
     */
    public static String readableDuration(long totalSeconds) {
        if (totalSeconds < 0) {
            throw new IllegalArgumentException("秒数不能为负数: " + totalSeconds);
        }
        if (totalSeconds == 0) {
            return "0秒";
        }
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("天");
        if (hours > 0) sb.append(hours).append("小时");
        if (minutes > 0) sb.append(minutes).append("分");
        if (seconds > 0) sb.append(seconds).append("秒");
        return sb.toString();
    }

    /**
     * 将秒数转换为 HH:mm:ss 格式。
     *
     * <pre>{@code
     * toHms(0)      → "00:00:00"
     * toHms(65)     → "00:01:05"
     * toHms(3661)   → "01:01:01"
     * }</pre>
     *
     * @param totalSeconds 总秒数（≥ 0）
     * @return HH:mm:ss 格式字符串
     * @throws IllegalArgumentException totalSeconds 为负数
     */
    public static String toHms(long totalSeconds) {
        if (totalSeconds < 0) {
            throw new IllegalArgumentException("秒数不能为负数: " + totalSeconds);
        }
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    // ============================================================
    //  日期比较
    // ============================================================

    /**
     * 判断 dateTime 是否在 [start, end] 区间内（含边界）。
     *
     * @param dateTime 待判断的时间
     * @param start    区间开始
     * @param end      区间结束
     * @return true 表示在区间内
     * @throws IllegalArgumentException 任一参数为 null
     */
    public static boolean isBetween(LocalDateTime dateTime, LocalDateTime start, LocalDateTime end) {
        if (dateTime == null || start == null || end == null) {
            throw new IllegalArgumentException("参数不能为 null");
        }
        return !dateTime.isBefore(start) && !dateTime.isAfter(end);
    }

    /**
     * 判断两个 LocalDateTime 是否为同一天。
     *
     * @param dt1 时间1
     * @param dt2 时间2
     * @return true 表示同一天
     */
    public static boolean isSameDay(LocalDateTime dt1, LocalDateTime dt2) {
        if (dt1 == null || dt2 == null) {
            throw new IllegalArgumentException("参数不能为 null");
        }
        return dt1.toLocalDate().isEqual(dt2.toLocalDate());
    }
}