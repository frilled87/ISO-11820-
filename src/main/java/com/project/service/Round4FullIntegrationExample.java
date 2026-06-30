package com.project.service;

import com.project.service.entity.DataPoint;
import com.project.service.model.ExportTestInfo;
import com.project.utils.*;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * <h1>第 4 轮 — 全流程集成演示</h1>
 *
 * <p>完整串联工具层全链路：
 * <ol>
 *   <li>读取 AppConfig 配置</li>
 *   <li>创建试验目录</li>
 *   <li>模拟生成 DataPoint 时序数据</li>
 *   <li>批量写入 CSV 文件</li>
 *   <li>组装 ExportTestInfo 试验数据</li>
 *   <li>调用 ReportExportService 一次性导出 Excel + PDF</li>
 *   <li>打印导出进度</li>
 * </ol>
 *
 * <p>直接运行 {@link #main} 方法即可完整测试整个工具层。</p>
 *
 * @author Project Team - Utility Layer
 * @version 1.0
 * @since 2026-06-30
 */
@SuppressWarnings("unused")
public final class Round4FullIntegrationExample {

    private static final Logger log = LogUtil.getLogger("EXPORT");

    private Round4FullIntegrationExample() {
        throw new UnsupportedOperationException("示例类不允许实例化");
    }

    // ============================================================
    //  全流程集成演示
    // ============================================================

    /**
     * 执行完整集成流程。
     *
     * @param args 命令行参数（未使用）
     */
    public static void main(String[] args) {
        printBanner();

        try {
            // ============================================================
            //  阶段 1: 读取配置 & 初始化
            // ============================================================
            log.info("══════ 阶段 1/6: 读取配置 & 初始化 ══════");
            AppConfig config = AppConfig.getInstance();
            FilePathManageUtil pathUtil = FilePathManageUtil.getInstance();

            System.out.println("数据根目录:   " + pathUtil.getBaseDir());
            System.out.println("试验数据目录: " + pathUtil.getTestDataRoot());
            System.out.println("报告输出目录: " + pathUtil.getReportRoot());
            System.out.println("数据库路径:   " + pathUtil.getDatabasePath());
            System.out.println("Excel 导出:   " + (config.isExcelExportEnabled() ? "启用" : "禁用"));
            System.out.println("PDF 导出:     " + (config.isPdfExportEnabled() ? "启用" : "禁用"));
            System.out.println("CSV 自动保存: " + (config.isCsvAutoSaveEnabled() ? "启用" : "禁用"));

            // 确保所有基础目录存在
            pathUtil.ensureAllBaseDirs();
            System.out.println("✅ 基础目录检查完成");

            // ============================================================
            //  阶段 2: 创建试验目录
            // ============================================================
            log.info("══════ 阶段 2/6: 创建试验目录 ══════");
            String productId = "20240613-001";
            String testId = DateUtil.generateTestId();

            pathUtil.ensureTestDataDirs(productId, testId);
            Path csvPath = pathUtil.getCsvPath(productId, testId);
            System.out.println("样品编号: " + productId);
            System.out.println("试验 ID:  " + testId);
            System.out.println("CSV 路径: " + csvPath);
            System.out.println("✅ 试验目录创建完成");

            // ============================================================
            //  阶段 3: 模拟生成时序数据 & 写入 CSV
            // ============================================================
            log.info("══════ 阶段 3/6: 模拟温度数据 & 写入 CSV ══════");
            CsvDataService csv = CsvDataService.getInstance();
            int totalSeconds = 120; // 模拟 2 分钟数据（演示用）

            List<DataPoint> buffer = new ArrayList<>();
            int batchSize = 20; // 每 20 秒批量写入一次

            for (int t = 0; t < totalSeconds; t++) {
                DataPoint dp = new DataPoint(
                        t,
                        generateTemp(t, 750.0, 0.5, 40.0),   // 炉温1
                        generateTemp(t, 750.0, 0.5, 40.0),   // 炉温2
                        generateTemp(t, 712.0, 0.3, 40.0),   // 表面温
                        generateTemp(t, 637.0, 0.2, 40.0),   // 中心温
                        generateTemp(t, 750.0, 1.0, 40.0),   // 校准温
                        26.0,                                  // 环境温度
                        60.0                                   // 环境湿度
                );
                buffer.add(dp);

                // 批量写入
                if (buffer.size() >= batchSize || t == totalSeconds - 1) {
                    csv.batchAppend(productId, testId, buffer);
                    if (t % 20 == 0 || t == totalSeconds - 1) {
                        System.out.printf("  写入进度: %d/%d 秒%n", t + 1, totalSeconds);
                    }
                    buffer.clear();
                }
            }

            System.out.println("✅ CSV 数据写入完成: " + totalSeconds + " 行");

            // 验证数据
            int rowCount = csv.countRows(productId, testId);
            System.out.println("  验证: CSV 中实际行数 = " + rowCount);

            // ============================================================
            //  阶段 4: 组装 ExportTestInfo
            // ============================================================
            log.info("══════ 阶段 4/6: 组装试验数据 ══════");
            ExportTestInfo info = buildTestInfo(productId, testId, totalSeconds);
            System.out.println("样品编号:     " + info.getProductId());
            System.out.println("试验 ID:      " + info.getTestId());
            System.out.println("操作员:       " + info.getOperator());
            System.out.println("试验前质量:   " + NumUtil.formatWeight(info.getPreWeight()));
            System.out.println("失重率:       " + NumUtil.format(info.getLostWeightPer(), 2) + " %");
            System.out.println("综合温升:     " + NumUtil.formatTemp(info.getDeltaTf()));
            System.out.println("判定结论:     " + info.getConclusion());
            System.out.println("✅ 试验数据组装完成");

            // ============================================================
            //  阶段 5: 调用 ReportExportService 导出
            // ============================================================
            log.info("══════ 阶段 5/6: 导出 Excel + PDF ══════");
            ReportExportService report = ReportExportService.getInstance();

            boolean exportOk = report.exportReport(info, (step, total, msg) -> {
                double pct = (double) step / total * 100;
                System.out.printf("  📊 导出进度 [%d/%d] %.0f%%: %s%n", step, total, pct, msg);
            });

            System.out.println(exportOk ? "✅ 报告导出成功" : "❌ 报告导出失败");

            // 验证导出结果
            if (config.isExcelExportEnabled()) {
                Path excelPath = pathUtil.getExcelReportPath(testId);
                boolean excelExists = pathUtil.fileExists(excelPath);
                long excelSize = pathUtil.getFileSize(excelPath);
                System.out.println("  Excel 文件: " + excelPath);
                System.out.println("  存在: " + excelExists + ", 大小: " + formatBytes(excelSize));
            }
            if (config.isPdfExportEnabled()) {
                Path pdfPath = pathUtil.getPdfReportPath(testId);
                boolean pdfExists = pathUtil.fileExists(pdfPath);
                long pdfSize = pathUtil.getFileSize(pdfPath);
                System.out.println("  PDF 文件:   " + pdfPath);
                System.out.println("  存在: " + pdfExists + ", 大小: " + formatBytes(pdfSize));
            }

            // ============================================================
            //  阶段 6: 数据统计 & 收尾
            // ============================================================
            log.info("══════ 阶段 6/6: 数据统计 & 收尾 ══════");
            System.out.println("═══════════════════════════════════");
            System.out.println("  工具层集成测试完成");
            System.out.println("═══════════════════════════════════");
            System.out.println("  CSV 文件数:   " + pathUtil.countCsvFiles());
            System.out.println("  报告文件数:   " + pathUtil.countReportFiles());
            System.out.println("  试验数据总大小: " + pathUtil.getTestDataSizeFormatted());
            System.out.println("  报告总大小:     " + pathUtil.getReportSizeFormatted());
            System.out.println("═══════════════════════════════════");

            // 调试：打印完整路径配置
            System.out.println("\n" + pathUtil);

            log.info("全流程集成测试通过 ✅");

        } catch (Exception e) {
            log.error("全流程集成测试失败", e);
            System.err.println("❌ 集成测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ============================================================
    //  辅助方法
    // ============================================================

    /**
     * 构建完整的 ExportTestInfo。
     */
    private static ExportTestInfo buildTestInfo(String productId, String testId, int totalSeconds) {
        ExportTestInfo info = new ExportTestInfo();
        info.setProductId(productId);
        info.setTestId(testId);
        info.setTestDate(DateUtil.today());
        info.setOperator("admin");
        info.setAccording("ISO 11820:2022");
        info.setApparatusId("FURNACE-01");
        info.setApparatusName("一号试验炉");
        info.setReportNo("RPT-" + testId);
        info.setAmbientTemp(26.0);
        info.setAmbientHumidity(60.0);
        info.setPreWeight(50.0);
        info.setPostWeight(45.0);
        info.setLostWeight(5.0);
        info.setLostWeightPer(10.0);
        info.setMaxTf1(750.5);
        info.setMaxTf2(750.3);
        info.setMaxTs(712.8);
        info.setMaxTc(637.5);
        info.setFinalTf1(750.1);
        info.setFinalTf2(749.9);
        info.setFinalTs(712.5);
        info.setFinalTc(637.2);
        info.setDeltaTf1(724.1);
        info.setDeltaTf2(724.0);
        info.setDeltaTs(686.5);
        info.setDeltaTc(611.2);
        info.setDeltaTf(686.5);
        info.setTotalTestTime(totalSeconds);
        info.setConstPower(2048);
        info.setFlameTime(0);
        info.setFlameDuration(0);
        info.setPhenoCode("1,2");
        info.setMemo("自动集成测试生成 — " + DateUtil.now());
        info.evaluatePassed();
        return info;
    }

    /**
     * 模拟温度生成函数。
     */
    private static double generateTemp(int second, double target, double noise, double heatingRate) {
        double base = target + (Math.random() - 0.5) * noise * 2;
        return NumUtil.roundTemp(base);
    }

    /**
     * 字节数转可读格式。
     */
    private static String formatBytes(long bytes) {
        if (bytes < 0) return "N/A";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    /**
     * 打印启动横幅。
     */
    private static void printBanner() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║   ISO 11820 — 工具层全流程集成测试              ║");
        System.out.println("║   Java 17 + Maven + SLF4J + Logback             ║");
        System.out.println("║   日期: " + DateUtil.now() + "                       ║");
        System.out.println("╚══════════════════════════════════════════════════╝");
        System.out.println();
    }
}