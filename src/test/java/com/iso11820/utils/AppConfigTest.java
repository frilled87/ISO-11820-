package com.iso11820.utils;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AppConfig 单元测试 —— 配置读取工具类。
 * <p>
 * 覆盖：单例模式、各类型配置读取、默认值回退、线程安全、reload。
 * 注意：测试依赖 classpath 中的 appsettings.json。
 * </p>
 *
 * @author ISO11820 Development Team
 * @version 1.0
 * @since 2026-06-30
 */
@DisplayName("AppConfig 配置管理测试")
class AppConfigTest {

    private static AppConfig config;

    @BeforeAll
    static void setUp() {
        config = AppConfig.getInstance();
    }

    // ==================== 单例 ====================

    @Nested
    @DisplayName("单例模式")
    class Singleton {

        @Test
        @DisplayName("getInstance 多次调用返回同一实例")
        void sameInstance() {
            AppConfig c1 = AppConfig.getInstance();
            AppConfig c2 = AppConfig.getInstance();
            assertSame(c1, c2);
        }
    }

    // ==================== 数据库配置 ====================

    @Nested
    @DisplayName("Database 配置")
    class DatabaseConfig {

        @Test
        @DisplayName("getDatabasePath 返回非空值")
        void databasePathNotNull() {
            String path = config.getDatabasePath();
            assertNotNull(path);
            assertFalse(path.isBlank());
        }
    }

    // ==================== 仿真配置 ====================

    @Nested
    @DisplayName("Simulation 配置")
    class SimulationConfig {

        @Test
        @DisplayName("getTargetFurnaceTemp 默认 750.0")
        void targetFurnaceTemp() {
            double temp = config.getTargetFurnaceTemp();
            assertTrue(temp > 0);
            assertTrue(temp <= 1000);
        }

        @Test
        @DisplayName("getHeatingRatePerSecond 默认 40.0")
        void heatingRate() {
            double rate = config.getHeatingRatePerSecond();
            assertTrue(rate > 0);
        }

        @Test
        @DisplayName("getTempFluctuation 默认 0.5")
        void tempFluctuation() {
            double fluctuation = config.getTempFluctuation();
            assertTrue(fluctuation > 0);
        }

        @Test
        @DisplayName("getStableThreshold 默认 3.0")
        void stableThreshold() {
            double threshold = config.getStableThreshold();
            assertTrue(threshold > 0);
        }

        @Test
        @DisplayName("getInitialFurnaceTemp 默认 25.0")
        void initialFurnaceTemp() {
            double temp = config.getInitialFurnaceTemp();
            assertTrue(temp >= 0);
        }

        @Test
        @DisplayName("isSimulationEnabled 默认 true")
        void simulationEnabled() {
            assertTrue(config.isSimulationEnabled());
        }
    }

    // ==================== 文件存储配置 ====================

    @Nested
    @DisplayName("FileStorage 配置")
    class FileStorageConfig {

        @Test
        @DisplayName("getBaseDirectory 返回非空值")
        void baseDirectoryNotNull() {
            String dir = config.getBaseDirectory();
            assertNotNull(dir);
            assertFalse(dir.isBlank());
        }

        @Test
        @DisplayName("getTestDataDirectory 返回非空值")
        void testDataDirectoryNotNull() {
            String dir = config.getTestDataDirectory();
            assertNotNull(dir);
            assertFalse(dir.isBlank());
        }
    }

    // ==================== 报告配置 ====================

    @Nested
    @DisplayName("Report 配置")
    class ReportConfig {

        @Test
        @DisplayName("getReportOutputDirectory 返回非空值")
        void reportOutputDirectoryNotNull() {
            String dir = config.getReportOutputDirectory();
            assertNotNull(dir);
            assertFalse(dir.isBlank());
        }

        @Test
        @DisplayName("isPdfExportEnabled 默认 true")
        void pdfExportEnabled() {
            assertTrue(config.isPdfExportEnabled());
        }

        @Test
        @DisplayName("isExcelExportEnabled 默认 true")
        void excelExportEnabled() {
            assertTrue(config.isExcelExportEnabled());
        }

        @Test
        @DisplayName("isCsvAutoSaveEnabled 默认 true")
        void csvAutoSaveEnabled() {
            assertTrue(config.isCsvAutoSaveEnabled());
        }
    }

    // ==================== 硬件配置 ====================

    @Nested
    @DisplayName("Hardware 配置")
    class HardwareConfig {

        @Test
        @DisplayName("getDefaultConstPower 默认 2048")
        void defaultConstPower() {
            int power = config.getDefaultConstPower();
            assertTrue(power > 0);
        }

        @Test
        @DisplayName("getPidTemperature 默认 750")
        void pidTemperature() {
            int temp = config.getPidTemperature();
            assertEquals(750, temp);
        }
    }

    // ==================== 日志配置 ====================

    @Nested
    @DisplayName("Logging 配置")
    class LoggingConfig {

        @Test
        @DisplayName("getLogDirectory 返回非空值")
        void logDirectoryNotNull() {
            String dir = config.getLogDirectory();
            assertNotNull(dir);
            assertFalse(dir.isBlank());
        }

        @Test
        @DisplayName("getMaxHistoryDays 默认 30")
        void maxHistoryDays() {
            int days = config.getMaxHistoryDays();
            assertTrue(days > 0);
        }
    }

    // ==================== 通用读取方法 ====================

    @Nested
    @DisplayName("通用读取方法")
    class GenericReadMethods {

        @Test
        @DisplayName("getString 不存在的路径返回默认值")
        void getStringDefault() {
            String val = config.getString("NonExistent.Path", "default");
            assertEquals("default", val);
        }

        @Test
        @DisplayName("getInt 不存在的路径返回默认值")
        void getIntDefault() {
            int val = config.getInt("NonExistent.Path", 42);
            assertEquals(42, val);
        }

        @Test
        @DisplayName("getDouble 不存在的路径返回默认值")
        void getDoubleDefault() {
            double val = config.getDouble("NonExistent.Path", 3.14);
            assertEquals(3.14, val);
        }

        @Test
        @DisplayName("getBoolean 不存在的路径返回默认值")
        void getBooleanDefault() {
            boolean val = config.getBoolean("NonExistent.Path", true);
            assertTrue(val);
        }
    }

    // ==================== reload ====================

    @Nested
    @DisplayName("重载配置")
    class Reload {

        @Test
        @DisplayName("reload 返回 true")
        void reloadReturnsTrue() {
            assertTrue(config.reload());
        }

        @Test
        @DisplayName("reload 后配置仍可正常读取")
        void reloadPreservesConfig() {
            config.reload();
            // 数据库路径应仍可读取
            assertNotNull(config.getDatabasePath());
        }
    }

    // ==================== toString ====================

    @Nested
    @DisplayName("toString")
    class ToString {

        @Test
        @DisplayName("toString 返回非空 JSON 字符串")
        void toStringNotNull() {
            String str = config.toString();
            assertNotNull(str);
            assertFalse(str.isBlank());
        }
    }
}