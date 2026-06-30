package com.iso11820.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DateUtil 单元测试 —— 日期时间工具类全覆盖。
 * <p>
 * 覆盖：now/today/currentTime、generateTestId、format/parse、
 * 时间戳转换、时间差计算、可读时长、日期比较。
 * </p>
 *
 * @author ISO11820 Development Team
 * @version 1.0
 * @since 2026-06-30
 */
@DisplayName("DateUtil 日期工具类测试")
class DateUtilTest {

    // ==================== 当前时间 ====================

    @Nested
    @DisplayName("获取当前时间")
    class CurrentTimeTests {

        @Test
        @DisplayName("now() 返回标准格式 yyyy-MM-dd HH:mm:ss")
        void nowReturnsStandardFormat() {
            String now = DateUtil.now();
            assertNotNull(now);
            assertTrue(now.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
        }

        @Test
        @DisplayName("now(pattern) 返回自定义格式")
        void nowWithCustomPattern() {
            String now = DateUtil.now("yyyy/MM/dd");
            assertTrue(now.matches("\\d{4}/\\d{2}/\\d{2}"));
        }

        @Test
        @DisplayName("now(null) 抛异常")
        void nowNullPatternThrows() {
            assertThrows(IllegalArgumentException.class, () -> DateUtil.now(null));
        }

        @Test
        @DisplayName("today() 返回纯日期格式 yyyy-MM-dd")
        void todayReturnsDateFormat() {
            String today = DateUtil.today();
            assertTrue(today.matches("\\d{4}-\\d{2}-\\d{2}"));
        }

        @Test
        @DisplayName("currentTime() 返回纯时间格式 HH:mm:ss")
        void currentTimeReturnsTimeFormat() {
            String time = DateUtil.currentTime();
            assertTrue(time.matches("\\d{2}:\\d{2}:\\d{2}"));
        }

        @Test
        @DisplayName("generateTestId() 返回 yyyyMMdd-HHmmss 格式")
        void generateTestIdFormat() {
            String testId = DateUtil.generateTestId();
            assertTrue(testId.matches("\\d{8}-\\d{6}"));
        }
    }

    // ==================== 格式化 ====================

    @Nested
    @DisplayName("format — 格式化")
    class FormatTests {

        @Test
        @DisplayName("format(LocalDateTime) 标准格式")
        void formatStandard() {
            LocalDateTime dt = LocalDateTime.of(2026, 6, 30, 14, 30, 0);
            assertEquals("2026-06-30 14:30:00", DateUtil.format(dt));
        }

        @Test
        @DisplayName("format(LocalDateTime, pattern) 自定义格式")
        void formatCustom() {
            LocalDateTime dt = LocalDateTime.of(2026, 6, 30, 14, 30, 0);
            assertEquals("2026/06/30 14:30", DateUtil.format(dt, "yyyy/MM/dd HH:mm"));
        }

        @Test
        @DisplayName("format(null) 抛异常")
        void formatNullThrows() {
            assertThrows(IllegalArgumentException.class, () -> DateUtil.format((LocalDateTime) null));
            assertThrows(IllegalArgumentException.class, () -> DateUtil.format(LocalDateTime.now(), null));
        }

        @Test
        @DisplayName("formatDate(LocalDate)")
        void formatDate() {
            LocalDate date = LocalDate.of(2026, 6, 30);
            assertEquals("2026-06-30", DateUtil.formatDate(date));
        }

        @Test
        @DisplayName("formatDate(null) 抛异常")
        void formatDateNullThrows() {
            assertThrows(IllegalArgumentException.class, () -> DateUtil.formatDate(null));
        }
    }

    // ==================== 解析 ====================

    @Nested
    @DisplayName("parse — 解析")
    class ParseTests {

        @Test
        @DisplayName("parse 标准格式")
        void parseStandard() {
            LocalDateTime dt = DateUtil.parse("2026-06-30 14:30:00");
            assertEquals(2026, dt.getYear());
            assertEquals(6, dt.getMonthValue());
            assertEquals(30, dt.getDayOfMonth());
            assertEquals(14, dt.getHour());
            assertEquals(30, dt.getMinute());
        }

        @Test
        @DisplayName("parse 自定义格式")
        void parseCustom() {
            LocalDateTime dt = DateUtil.parse("20260630", "yyyyMMdd");
            assertEquals(2026, dt.getYear());
            assertEquals(6, dt.getMonthValue());
        }

        @Test
        @DisplayName("parse 格式不匹配抛异常")
        void parseMismatchThrows() {
            assertThrows(RuntimeException.class, () -> DateUtil.parse("bad date"));
        }

        @Test
        @DisplayName("parse(null) 抛异常")
        void parseNullThrows() {
            assertThrows(IllegalArgumentException.class, () -> DateUtil.parse(null));
            assertThrows(IllegalArgumentException.class, () -> DateUtil.parse("2026-06-30", null));
        }

        @Test
        @DisplayName("parseDate 纯日期")
        void parseDate() {
            LocalDate date = DateUtil.parseDate("2026-06-30");
            assertEquals(2026, date.getYear());
            assertEquals(6, date.getMonthValue());
        }

        @Test
        @DisplayName("parseOrNull 失败返回 null")
        void parseOrNullReturnsNull() {
            assertNull(DateUtil.parseOrNull("bad", "yyyy-MM-dd"));
            assertNotNull(DateUtil.parseOrNull("2026-06-30", "yyyy-MM-dd"));
        }
    }

    // ==================== 时间戳转换 ====================

    @Nested
    @DisplayName("时间戳转换")
    class TimestampTests {

        @Test
        @DisplayName("LocalDateTime → 毫秒时间戳")
        void toTimestamp() {
            LocalDateTime dt = LocalDateTime.of(2026, 6, 30, 14, 30, 0);
            long ts = DateUtil.toTimestamp(dt);
            assertTrue(ts > 0);
        }

        @Test
        @DisplayName("字符串 → 毫秒时间戳")
        void stringToTimestamp() {
            long ts = DateUtil.toTimestamp("2026-06-30 14:30:00");
            assertTrue(ts > 0);
        }

        @Test
        @DisplayName("toTimestamp(null) 抛异常")
        void toTimestampNullThrows() {
            assertThrows(IllegalArgumentException.class, () -> DateUtil.toTimestamp((LocalDateTime) null));
        }

        @Test
        @DisplayName("毫秒时间戳 → LocalDateTime")
        void fromTimestamp() {
            LocalDateTime original = LocalDateTime.of(2026, 6, 30, 14, 30, 0);
            long ts = DateUtil.toTimestamp(original);
            LocalDateTime restored = DateUtil.fromTimestamp(ts);
            assertEquals(original.getYear(), restored.getYear());
            assertEquals(original.getMonth(), restored.getMonth());
            assertEquals(original.getDayOfMonth(), restored.getDayOfMonth());
        }

        @Test
        @DisplayName("毫秒时间戳 → 格式化字符串")
        void fromTimestampFormatted() {
            long ts = DateUtil.toTimestamp("2026-06-30 14:30:00");
            String formatted = DateUtil.fromTimestampFormatted(ts);
            assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
        }

        @Test
        @DisplayName("currentTimestamp() 返回正值")
        void currentTimestampPositive() {
            assertTrue(DateUtil.currentTimestamp() > 0);
        }
    }

    // ==================== 时间差计算 ====================

    @Nested
    @DisplayName("时间差计算")
    class ElapsedTimeTests {

        @Test
        @DisplayName("elapsedSeconds(LocalDateTime, LocalDateTime)")
        void elapsedSecondsLocalDateTime() {
            LocalDateTime start = LocalDateTime.of(2026, 6, 30, 14, 0, 0);
            LocalDateTime end = LocalDateTime.of(2026, 6, 30, 14, 1, 0);
            assertEquals(60, DateUtil.elapsedSeconds(start, end));
        }

        @Test
        @DisplayName("elapsedSeconds 可能为负数")
        void elapsedSecondsNegative() {
            LocalDateTime start = LocalDateTime.of(2026, 6, 30, 14, 1, 0);
            LocalDateTime end = LocalDateTime.of(2026, 6, 30, 14, 0, 0);
            assertEquals(-60, DateUtil.elapsedSeconds(start, end));
        }

        @Test
        @DisplayName("elapsedSeconds(null) 抛异常")
        void elapsedSecondsNullThrows() {
            LocalDateTime now = LocalDateTime.now();
            assertThrows(IllegalArgumentException.class, () -> DateUtil.elapsedSeconds(null, now));
            assertThrows(IllegalArgumentException.class, () -> DateUtil.elapsedSeconds(now, null));
        }

        @Test
        @DisplayName("elapsedSeconds(long, long) 毫秒时间戳差")
        void elapsedSecondsTimestamps() {
            assertEquals(60, DateUtil.elapsedSeconds(1000000L, 1060000L));
        }

        @Test
        @DisplayName("elapsedMillis")
        void elapsedMillis() {
            LocalDateTime start = LocalDateTime.of(2026, 6, 30, 14, 0, 0);
            LocalDateTime end = LocalDateTime.of(2026, 6, 30, 14, 0, 1);
            assertEquals(1000, DateUtil.elapsedMillis(start, end));
        }

        @Test
        @DisplayName("elapsedMillis(null) 抛异常")
        void elapsedMillisNullThrows() {
            assertThrows(IllegalArgumentException.class, () -> DateUtil.elapsedMillis(null, LocalDateTime.now()));
        }
    }

    // ==================== 可读时长 ====================

    @Nested
    @DisplayName("readableDuration")
    class ReadableDurationTests {

        @ParameterizedTest
        @CsvSource({
            "0, 0秒",
            "65, 1分5秒",
            "3661, 1小时1分1秒",
            "90061, 1天1小时1分1秒",
            "3600, 1小时",
            "60, 1分"
        })
        @DisplayName("秒数转换为可读时长")
        void readableDuration(long seconds, String expected) {
            assertEquals(expected, DateUtil.readableDuration(seconds));
        }

        @Test
        @DisplayName("负数抛异常")
        void negativeSecondsThrows() {
            assertThrows(IllegalArgumentException.class, () -> DateUtil.readableDuration(-1));
        }
    }

    @Nested
    @DisplayName("toHms")
    class ToHmsTests {

        @ParameterizedTest
        @CsvSource({
            "0, 00:00:00",
            "65, 00:01:05",
            "3661, 01:01:01",
            "86399, 23:59:59"
        })
        @DisplayName("秒数转换为 HH:mm:ss")
        void toHms(long seconds, String expected) {
            assertEquals(expected, DateUtil.toHms(seconds));
        }

        @Test
        @DisplayName("负数抛异常")
        void negativeSecondsThrows() {
            assertThrows(IllegalArgumentException.class, () -> DateUtil.toHms(-1));
        }
    }

    // ==================== 日期比较 ====================

    @Nested
    @DisplayName("日期比较")
    class DateComparisonTests {

        @Test
        @DisplayName("isBetween 在区间内")
        void isBetween() {
            LocalDateTime dt = LocalDateTime.of(2026, 6, 30, 12, 0, 0);
            LocalDateTime start = LocalDateTime.of(2026, 6, 30, 0, 0, 0);
            LocalDateTime end = LocalDateTime.of(2026, 6, 30, 23, 59, 59);
            assertTrue(DateUtil.isBetween(dt, start, end));
        }

        @Test
        @DisplayName("isBetween 不在区间内")
        void notBetween() {
            LocalDateTime dt = LocalDateTime.of(2026, 7, 1, 0, 0, 0);
            LocalDateTime start = LocalDateTime.of(2026, 6, 30, 0, 0, 0);
            LocalDateTime end = LocalDateTime.of(2026, 6, 30, 23, 59, 59);
            assertFalse(DateUtil.isBetween(dt, start, end));
        }

        @Test
        @DisplayName("isBetween(null) 抛异常")
        void isBetweenNullThrows() {
            LocalDateTime now = LocalDateTime.now();
            assertThrows(IllegalArgumentException.class, () -> DateUtil.isBetween(null, now, now));
        }

        @Test
        @DisplayName("isSameDay 同一天")
        void isSameDay() {
            LocalDateTime dt1 = LocalDateTime.of(2026, 6, 30, 10, 0, 0);
            LocalDateTime dt2 = LocalDateTime.of(2026, 6, 30, 22, 0, 0);
            assertTrue(DateUtil.isSameDay(dt1, dt2));
        }

        @Test
        @DisplayName("isSameDay 不同天")
        void notSameDay() {
            LocalDateTime dt1 = LocalDateTime.of(2026, 6, 30, 10, 0, 0);
            LocalDateTime dt2 = LocalDateTime.of(2026, 7, 1, 10, 0, 0);
            assertFalse(DateUtil.isSameDay(dt1, dt2));
        }

        @Test
        @DisplayName("isSameDay(null) 抛异常")
        void isSameDayNullThrows() {
            assertThrows(IllegalArgumentException.class, () -> DateUtil.isSameDay(null, LocalDateTime.now()));
        }
    }

    // ==================== 边界条件 ====================

    @Nested
    @DisplayName("边界条件")
    class BoundaryConditions {

        @Test
        @DisplayName("工具类不允许实例化")
        void cannotInstantiate() {
            assertThrows(UnsupportedOperationException.class, DateUtil::new);
        }

        @Test
        @DisplayName("格式常量不为空")
        void formatConstantsNotNull() {
            assertNotNull(DateUtil.PATTERN_STANDARD);
            assertNotNull(DateUtil.PATTERN_DATE);
            assertNotNull(DateUtil.PATTERN_TIME);
            assertNotNull(DateUtil.PATTERN_COMPACT);
            assertNotNull(DateUtil.PATTERN_TESTID);
        }
    }
}