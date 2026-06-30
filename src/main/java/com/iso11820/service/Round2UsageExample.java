package com.iso11820.service;

import com.iso11820.service.entity.DataPoint;
import com.iso11820.utils.AppConfig;
import com.iso11820.utils.FilePathManageUtil;
import com.iso11820.utils.LogUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * <h1>第 2 轮 — CSV 数据服务 + 文件路径管理调用示例</h1>
 *
 * <p>展示 FilePathManageUtil、DataPoint、CsvDataService 的完整调用方式。</p>
 *
 * @author Project Team - Utility Layer
 * @version 1.0
 * @since 2026-06-30
 */
@SuppressWarnings("unused")
public final class Round2UsageExample {

    private Round2UsageExample() {
        throw new UnsupportedOperationException("示例类不允许实例化");
    }

    // ============================================================
    //  FilePathManageUtil 示例
    // ============================================================

    public static void filePathUtilDemo() {
        FilePathManageUtil fpm = FilePathManageUtil.getInstance();

        // ---- 1. 基础路径 ----
        Path baseDir = fpm.getBaseDir();
        LogUtil.info("根目录: {}", baseDir);

        Path testDataRoot = fpm.getTestDataRoot();
        LogUtil.info("试验数据根目录: {}", testDataRoot);

        Path reportRoot = fpm.getReportRoot();
        LogUtil.info("报告根目录: {}", reportRoot);

        Path dbPath = fpm.getDatabasePath();
        LogUtil.info("数据库路径: {}", dbPath);

        Path logDir = fpm.getLogDir();
        LogUtil.info("日志目录: {}", logDir);

        // ---- 2. 试验 CSV 路径 ----
        String productId = "20240613-001";
        String testId = "20260630-143000";

        Path csvPath = fpm.getCsvPath(productId, testId);
        LogUtil.info("CSV 路径: {}", csvPath);
        // → ./ISO11820_Data/TestData/20240613-001/20260630-143000/sensor.csv

        Path testDataDir = fpm.getTestDataDir(productId, testId);
        LogUtil.info("试验数据目录: {}", testDataDir);

        // ---- 3. 报告路径 ----
        Path excelReport = fpm.getReportPath(testId, "xlsx");
        LogUtil.info("Excel 报告路径: {}", excelReport);
        // → ./ISO11820_Data/Reports/20260630-143000_报告.xlsx

        Path pdfReport = fpm.getReportPath(testId, "pdf");
        LogUtil.info("PDF 报告路径: {}", pdfReport);

        // ---- 4. 目录创建 ----
        // 确保所有基础目录存在（应用启动时调用一次）
        fpm.ensureAllBaseDirs();

        // 确保特定试验目录存在
        fpm.ensureTestDataDirs(productId, testId);
        fpm.ensureReportDir();

        // ---- 5. 文件检查 ----
        boolean exists = fpm.csvExists(productId, testId);
        LogUtil.info("CSV 是否存在: {}", exists);

        long size = fpm.getFileSize(csvPath);
        LogUtil.info("CSV 文件大小: {} 字节", size);

        // ---- 6. 打印路径概览 ----
        System.out.println(fpm);
    }

    // ============================================================
    //  DataPoint 示例
    // ============================================================

    public static void dataPointDemo() {
        // ---- 1. 无参构造 ----
        DataPoint dp1 = new DataPoint();
        dp1.setTimeSeconds(0);
        dp1.setTf1(25.0);
        dp1.setTf2(24.9);
        dp1.setTs(24.5);
        dp1.setTc(24.3);
        dp1.settCal(25.1);
        dp1.setAmbientTemp(26.0);
        dp1.setAmbientHumidity(60.0);

        System.out.println(dp1);
        // → DataPoint{t=0s, tf1=25.0, tf2=24.9, ts=24.5, tc=24.3, tCal=25.1, ambT=26.0, ambH=60.0, ts=2026-06-30 14:30:00}

        // ---- 2. 全参构造（温度自动保留 1 位小数） ----
        DataPoint dp2 = new DataPoint(
                1,              // 第 1 秒
                30.123,         // 炉温1 → 四舍五入后 30.1
                30.056,         // 炉温2 → 30.1
                24.6,           // 表面温
                24.4,           // 中心温
                25.0,           // 校准温
                26.0,           // 环境温度
                60.0            // 环境湿度
        );

        System.out.println(dp2);

        // ---- 3. equals / hashCode ----
        DataPoint dp3 = new DataPoint(1, 30.15, 30.05, 24.6, 24.4, 25.0, 26.0, 60.0);
        // 30.15 → 30.2, 30.05 → 30.1，与 dp2 不同
        System.out.println("dp2 == dp3? " + dp2.equals(dp3)); // false
    }

    // ============================================================
    //  CsvDataService 示例
    // ============================================================

    public static void csvServiceDemo() {
        CsvDataService csv = CsvDataService.getInstance();
        String productId = "20240613-001";
        String testId = "20260630-143000";

        // ---- 1. 写入单行（首次写入自动生成表头和目录） ----
        DataPoint dp = new DataPoint(0, 25.0, 24.9, 24.5, 24.3, 25.1, 26.0, 60.0);
        boolean ok = csv.appendRow(productId, testId, dp);
        LogUtil.info("单行写入: {}", ok ? "成功" : "失败");

        // ---- 2. 批量写入（推荐，减少 IO 次数） ----
        List<DataPoint> batch = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            batch.add(new DataPoint(
                    i,
                    25.0 + i * 0.5,     // 炉温1 缓慢上升
                    25.0 + i * 0.48,    // 炉温2 独立噪声
                    24.5 + i * 0.1,     // 表面温
                    24.3 + i * 0.08,    // 中心温
                    25.1 + i * 0.05,    // 校准温
                    26.0,               // 环境温度
                    60.0                // 环境湿度
            ));
        }
        boolean batchOk = csv.batchAppend(productId, testId, batch);
        LogUtil.info("批量写入 {} 行: {}", batch.size(), batchOk ? "成功" : "失败");

        // ---- 3. 读取全部数据 ----
        List<DataPoint> all = csv.readAll(productId, testId);
        LogUtil.info("读取全部数据: {} 行", all.size());
        for (DataPoint point : all) {
            System.out.println("  " + point);
        }

        // ---- 4. 按时间范围读取 ----
        // 读取第 3~7 秒的数据
        List<DataPoint> segment = csv.readRange(productId, testId, 3, 7);
        LogUtil.info("时间范围 [3,7] 秒: {} 行", segment.size());

        // ---- 5. 行数统计 ----
        int count = csv.countRows(productId, testId);
        LogUtil.info("总行数: {}", count);

        // ---- 6. 删除试验数据 ----
        // csv.deleteData(productId, testId); // 谨慎使用
    }

    // ============================================================
    //  综合示例：模拟试验过程中的 CSV 写入
    // ============================================================

    /**
     * 模拟试验记录阶段每秒写入温度数据。
     *
     * @param productId 样品编号
     * @param testId    试验 ID
     * @param totalSeconds 总记录时长（秒）
     */
    public static void simulateTestRecording(String productId, String testId, int totalSeconds) {
        CsvDataService csv = CsvDataService.getInstance();
        LogUtil.info("开始模拟试验记录: {} 秒", totalSeconds);

        List<DataPoint> buffer = new ArrayList<>();
        int batchSize = 10; // 每 10 秒批量写入一次

        for (int t = 0; t < totalSeconds; t++) {
            // 模拟温度数据
            double tf1 = simulateTemperature(t, 750.0, 0.5);
            double tf2 = simulateTemperature(t, 750.0, 0.5);
            double ts = simulateTemperature(t, 712.0, 0.3);
            double tc = simulateTemperature(t, 637.0, 0.2);
            double tCal = tf1 + (Math.random() - 0.5) * 2.0;

            DataPoint dp = new DataPoint(t, tf1, tf2, ts, tc, tCal, 26.0, 60.0);
            buffer.add(dp);

            // 每 batchSize 秒批量写入一次
            if (buffer.size() >= batchSize || t == totalSeconds - 1) {
                csv.batchAppend(productId, testId, buffer);
                buffer.clear();
            }
        }

        LogUtil.info("模拟试验记录完成: {} 行数据已保存", totalSeconds);
    }

    /**
     * 简单温度仿真函数。
     */
    private static double simulateTemperature(int second, double target, double noise) {
        double base = target + (Math.random() - 0.5) * noise * 2;
        return Math.round(base * 10.0) / 10.0;
    }

    // ============================================================
    //  main
    // ============================================================

    public static void main(String[] args) {
        System.out.println("========== FilePathManageUtil 示例 ==========");
        filePathUtilDemo();

        System.out.println("\n========== DataPoint 示例 ==========");
        dataPointDemo();

        System.out.println("\n========== CsvDataService 示例 ==========");
        csvServiceDemo();

        System.out.println("\n========== 综合模拟 ==========");
        simulateTestRecording("DEMO-001", "20260630-150000", 30);

        System.out.println("\n========== 第 2 轮示例完成 ==========");
    }
}