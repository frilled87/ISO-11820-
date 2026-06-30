package com.iso11820.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NumUtil 单元测试 —— 数值工具类全覆盖。
 * <p>
 * 覆盖：精度控制、空值安全转换、数字校验、格式化、便捷计算。
 * </p>
 *
 * @author ISO11820 Development Team
 * @version 1.0
 * @since 2026-06-30
 */
@DisplayName("NumUtil 数值工具类测试")
class NumUtilTest {

    // ==================== 精度控制 ====================

    @Nested
    @DisplayName("round — 四舍五入")
    class RoundTests {

        @Test
        @DisplayName("round(3.14159, 2) → 3.14")
        void roundToTwoDecimals() {
            assertEquals(3.14, NumUtil.round(3.14159, 2));
        }

        @Test
        @DisplayName("round(3.14159, 0) → 3.0")
        void roundToZeroDecimals() {
            assertEquals(3.0, NumUtil.round(3.14159, 0));
        }

        @Test
        @DisplayName("round(750.555, 1) → 750.6")
        void roundTemperatureHalfUp() {
            assertEquals(750.6, NumUtil.round(750.555, 1));
        }

        @Test
        @DisplayName("round with FLOOR 向下取整")
        void roundFloor() {
            assertEquals(3.14, NumUtil.round(3.14159, 2, RoundingMode.FLOOR));
        }

        @Test
        @DisplayName("round with CEILING 向上取整")
        void roundCeiling() {
            assertEquals(3.15, NumUtil.round(3.14159, 2, RoundingMode.CEILING));
        }

        @Test
        @DisplayName("round scale 超出范围抛异常")
        void roundInvalidScale() {
            assertThrows(IllegalArgumentException.class, () -> NumUtil.round(1.0, -1));
            assertThrows(IllegalArgumentException.class, () -> NumUtil.round(1.0, 11));
        }

        @Test
        @DisplayName("round with null RoundingMode 默认 HALF_UP")
        void roundNullRoundingMode() {
            assertEquals(3.14, NumUtil.round(3.14159, 2, null));
        }
    }

    @Nested
    @DisplayName("roundTemp — 温度精度")
    class RoundTempTests {

        @Test
        @DisplayName("roundTemp 保留 1 位小数")
        void roundTempOneDecimal() {
            assertEquals(750.5, NumUtil.roundTemp(750.55));
            assertEquals(750.6, NumUtil.roundTemp(750.55)); // 0.55 → 0.6
        }
    }

    @Nested
    @DisplayName("roundPercent — 百分比精度")
    class RoundPercentTests {

        @Test
        @DisplayName("roundPercent 保留 2 位小数")
        void roundPercentTwoDecimals() {
            assertEquals(12.35, NumUtil.roundPercent(12.345));
        }
    }

    // ==================== 空值安全转换 ====================

    @Nested
    @DisplayName("toDoubleOrNull")
    class ToDoubleOrNullTests {

        @Test
        @DisplayName("合法数字字符串转换成功")
        void validDoubleString() {
            assertEquals(12.34, NumUtil.toDoubleOrNull("12.34"));
            assertEquals(-5.0, NumUtil.toDoubleOrNull("-5.0"));
            assertEquals(0.0, NumUtil.toDoubleOrNull("0"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "abc", "12.34.56"})
        @DisplayName("非法输入返回 null")
        void invalidInputReturnsNull(String input) {
            assertNull(NumUtil.toDoubleOrNull(input));
        }
    }

    @Nested
    @DisplayName("toDoubleOrDefault")
    class ToDoubleOrDefaultTests {

        @Test
        @DisplayName("合法数字返回转换值")
        void validReturnsParsed() {
            assertEquals(12.34, NumUtil.toDoubleOrDefault("12.34", 0.0));
        }

        @Test
        @DisplayName("非法输入返回默认值")
        void invalidReturnsDefault() {
            assertEquals(0.0, NumUtil.toDoubleOrDefault("abc", 0.0));
            assertEquals(-1.0, NumUtil.toDoubleOrDefault(null, -1.0));
        }
    }

    @Nested
    @DisplayName("toIntOrNull / toIntOrDefault")
    class ToIntTests {

        @Test
        @DisplayName("合法整数字符串")
        void validIntString() {
            assertEquals(42, NumUtil.toIntOrNull("42"));
            assertEquals(-10, NumUtil.toIntOrNull("-10"));
        }

        @Test
        @DisplayName("非法输入返回 null")
        void invalidIntReturnsNull() {
            assertNull(NumUtil.toIntOrNull("abc"));
            assertNull(NumUtil.toIntOrNull(null));
        }

        @Test
        @DisplayName("toIntOrDefault 返回默认值")
        void toIntOrDefaultReturnsDefault() {
            assertEquals(99, NumUtil.toIntOrDefault("abc", 99));
        }
    }

    @Nested
    @DisplayName("toLongOrNull / toLongOrDefault")
    class ToLongTests {

        @Test
        @DisplayName("合法长整数字符串")
        void validLongString() {
            assertEquals(9999999999L, NumUtil.toLongOrNull("9999999999"));
        }

        @Test
        @DisplayName("非法输入返回 null")
        void invalidLongReturnsNull() {
            assertNull(NumUtil.toLongOrNull("abc"));
        }

        @Test
        @DisplayName("toLongOrDefault 返回默认值")
        void toLongOrDefaultReturnsDefault() {
            assertEquals(100L, NumUtil.toLongOrDefault("abc", 100L));
        }
    }

    // ==================== 数字校验 ====================

    @Nested
    @DisplayName("isNumber")
    class IsNumberTests {

        @ParameterizedTest
        @ValueSource(strings = {"123", "-12.34", "+5.0", "1e5", "0", "3.14159"})
        @DisplayName("合法数字返回 true")
        void validNumbers(String input) {
            assertTrue(NumUtil.isNumber(input));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "abc", "12.34.56", "NaN"})
        @DisplayName("非法数字返回 false")
        void invalidNumbers(String input) {
            assertFalse(NumUtil.isNumber(input));
        }
    }

    @Nested
    @DisplayName("isInteger")
    class IsIntegerTests {

        @ParameterizedTest
        @ValueSource(strings = {"123", "-5", "0"})
        @DisplayName("合法整数返回 true")
        void validIntegers(String input) {
            assertTrue(NumUtil.isInteger(input));
        }

        @ParameterizedTest
        @ValueSource(strings = {"12.34", "abc", ""})
        @DisplayName("非法整数返回 false")
        void invalidIntegers(String input) {
            assertFalse(NumUtil.isInteger(input));
        }
    }

    @Nested
    @DisplayName("inRange")
    class InRangeTests {

        @Test
        @DisplayName("值在区间内返回 true")
        void valueInRange() {
            assertTrue(NumUtil.inRange(750.0, 745.0, 755.0));
            assertTrue(NumUtil.inRange(745.0, 745.0, 755.0)); // 边界
            assertTrue(NumUtil.inRange(755.0, 745.0, 755.0)); // 边界
        }

        @Test
        @DisplayName("值在区间外返回 false")
        void valueOutOfRange() {
            assertFalse(NumUtil.inRange(744.9, 745.0, 755.0));
            assertFalse(NumUtil.inRange(755.1, 745.0, 755.0));
        }
    }

    // ==================== 格式化 ====================

    @Nested
    @DisplayName("format")
    class FormatTests {

        @Test
        @DisplayName("format(750.5, 1) → \"750.5\"")
        void formatOneDecimal() {
            assertEquals("750.5", NumUtil.format(750.5, 1));
        }

        @Test
        @DisplayName("format(3.14159, 2) → \"3.14\"")
        void formatTwoDecimals() {
            assertEquals("3.14", NumUtil.format(3.14159, 2));
        }

        @Test
        @DisplayName("format(3.0, 2) → \"3.00\"")
        void formatPadZeros() {
            assertEquals("3.00", NumUtil.format(3.0, 2));
        }

        @Test
        @DisplayName("format scale 超出范围抛异常")
        void formatInvalidScale() {
            assertThrows(IllegalArgumentException.class, () -> NumUtil.format(1.0, -1));
            assertThrows(IllegalArgumentException.class, () -> NumUtil.format(1.0, 11));
        }
    }

    @Nested
    @DisplayName("formatPercent")
    class FormatPercentTests {

        @Test
        @DisplayName("formatPercent(0.1234, 2) → \"12.34%\"")
        void formatPercent() {
            assertEquals("12.34%", NumUtil.formatPercent(0.1234, 2));
        }

        @Test
        @DisplayName("formatPercent(0.5, 1) → \"50.0%\"")
        void formatPercentHalf() {
            assertEquals("50.0%", NumUtil.formatPercent(0.5, 1));
        }
    }

    @Nested
    @DisplayName("formatTemp / formatWeight")
    class FormatWithUnitTests {

        @Test
        @DisplayName("formatTemp(750.5) → \"750.5 °C\"")
        void formatTemp() {
            assertEquals("750.5 °C", NumUtil.formatTemp(750.5));
        }

        @Test
        @DisplayName("formatWeight(50.0) → \"50.00 g\"")
        void formatWeight() {
            assertEquals("50.00 g", NumUtil.formatWeight(50.0));
        }
    }

    // ==================== 便捷计算 ====================

    @Nested
    @DisplayName("calcPercent")
    class CalcPercentTests {

        @Test
        @DisplayName("calcPercent(5.0, 50.0, 2) → 10.00")
        void calcPercent() {
            assertEquals(10.0, NumUtil.calcPercent(5.0, 50.0, 2));
        }

        @Test
        @DisplayName("总量为 0 抛异常")
        void calcPercentZeroTotal() {
            assertThrows(IllegalArgumentException.class, () -> NumUtil.calcPercent(5.0, 0.0, 2));
        }
    }

    @Nested
    @DisplayName("safeDivide")
    class SafeDivideTests {

        @Test
        @DisplayName("正常除法")
        void normalDivision() {
            assertEquals(2.5, NumUtil.safeDivide(5.0, 2.0, 0.0));
        }

        @Test
        @DisplayName("除数为 0 返回默认值")
        void divideByZeroReturnsDefault() {
            assertEquals(-1.0, NumUtil.safeDivide(5.0, 0.0, -1.0));
        }
    }

    @Nested
    @DisplayName("equalsWithTolerance")
    class EqualsWithToleranceTests {

        @Test
        @DisplayName("容差范围内相等")
        void withinTolerance() {
            assertTrue(NumUtil.equalsWithTolerance(750.001, 750.002, 0.01));
        }

        @Test
        @DisplayName("超出容差不相等")
        void outsideTolerance() {
            assertFalse(NumUtil.equalsWithTolerance(750.0, 751.0, 0.5));
        }
    }

    // ==================== 边界条件 ====================

    @Nested
    @DisplayName("边界条件")
    class BoundaryConditions {

        @Test
        @DisplayName("NaN 值处理")
        void nanHandling() {
            assertFalse(NumUtil.isNumber("NaN"));
            assertNull(NumUtil.toDoubleOrNull("NaN"));
        }

        @Test
        @DisplayName("极大值处理")
        void largeValueHandling() {
            assertTrue(NumUtil.isNumber("1E308"));
            assertEquals(Double.POSITIVE_INFINITY, NumUtil.toDoubleOrNull("1E309"));
        }

        @Test
        @DisplayName("空字符串处理")
        void emptyStringHandling() {
            assertNull(NumUtil.toDoubleOrNull(""));
            assertFalse(NumUtil.isNumber(""));
            assertNull(NumUtil.toIntOrNull(""));
        }

        @Test
        @DisplayName("工具类不允许实例化")
        void cannotInstantiate() {
            assertThrows(UnsupportedOperationException.class, NumUtil::new);
        }
    }
}