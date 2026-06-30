package com.iso11820.service;

import com.iso11820.service.entity.DataPoint;
import com.iso11820.service.model.ExportTestInfo;
import com.iso11820.utils.*;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * <h1>第 3 轮 — Excel/PDF 导出服务调用示例</h1>
 *
 * <p>展示 ExcelReportService 和 PdfReportService 的完整调用方式，
 * 包含模拟数据构造和批量导出。</p>
 *
 * @author Project Team - Utility Layer
 * @version 1.0
 * @since 2026-06-30
 */
@SuppressWarnings("unused")
public final class Round3UsageExample {

    private static final Logger log = LogUtil.getLogger("EXPORT");

    private Round3UsageExample() {
        throw new UnsupportedOperationException("示例类不允许实例化");
    }

    // ============================================================
    //  构造模拟数据
    // ============================================================

    /**
     * 构造一份完整的试验数据，模拟真实试验结束后的数据状态。
     */
    public static ExportTestInfo buildMockTestInfo() {
        ExportTestInfo info = new ExportTestInfo();

        // 基本信息
        info.setProductId("20240613-001");
        info.setTestId("20260630-143000");
        info.setTestDate("2026-06-30");
        info.setOperator("admin");
        info.setAccording("ISO 11820:2022");
        info.setApparatusId("FURNACE-01");
        info.setApparatusName("一号试验炉");
        info.setReportNo("RPT-20260630-001");

        // 环境参数
        info.setAmbientTemp(26.0);
        info.setAmbientHumidity(60.0);

        // 质量数据
        info.setPreWeight(50.0);
        info.setPostWeight(45.0);
        info.setLostWeight(5.0);
        info.setLostWeightPer(10.0);

        // 温度统计
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
        info.setDeltaTf(686.5);  // 取表面温升

        // 试验过程
        info.setTotalTestTime(3600);
        info.setConstPower(2048);
        info.setFlameTime(0);
        info.setFlameDuration(0);
        info.setPhenoCode("1,2");
        info.setMemo("本次试验数据正常，样品无明显异常现象");

        // 自动判定
        info.evaluatePassed();

        return info;
    }

    // ============================================================
    //  准备 CSV 数据（模拟记录阶段数据）
    // ============================================================

    /**
     * 往 CSV 写入模拟温度数据，供 Excel 数据 Sheet 和图表使用。
     */
    public static void prepareCsvData(String productId, String testId, int seconds) {
        CsvDataService csv = CsvDataService.getInstance();
        List<DataPoint> buffer = new ArrayList<>();

        for (int t = 0; t < seconds; t++) {
            DataPoint dp = new DataPoint(
                    t,
                    750.0 + (Math.random() - 0.5) * 0.6,     // 炉温1
                    750.0 + (Math.random() - 0.5) * 0.5,     // 炉温2
                    712.0 + (Math.random() - 0.5) * 1.0,     // 表面温
                    637.0 + (Math.random() - 0.5) * 0.8,     // 中心温
                    750.0 + (Math.random() - 0.5) * 2.0,     // 校准温
                    26.0,                                      // 环境温度
                    60.0                                       // 环境湿度
            );
            buffer.add(dp);

            if (buffer.size() >= 50 || t == seconds - 1) {
                csv.batchAppend(productId, testId, buffer);
                buffer.clear();
            }
        }
        log.info("模拟 CSV 数据已准备: {} 行", seconds);
    }

    // ============================================================
    //  Excel 导出示例
    // ============================================================

    public static void excelExportDemo() {
        ExcelReportService excel = ExcelReportService.getInstance();

        // ---- 1. 单试验导出 ----
        ExportTestInfo info = buildMockTestInfo();
        String pid = info.getProductId();
        String tid = info.getTestId();

        // 先准备 CSV 数据
        prepareCsvData(pid, tid, 120); // 模拟 120 秒数据

        Path excelFile = excel.exportSingle(info);
        log.info("Excel 单试验报告已生成: {}", excelFile);

        // ---- 2. 批量导出 ----
        List<ExportTestInfo> batch = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            ExportTestInfo batchInfo = new ExportTestInfo();
            batchInfo.setProductId("P2026-" + String.format("%03d", i));
            batchInfo.setTestId("20260630-" + String.format("%06d", i));
            batchInfo.setTestDate("2026-06-30");
            batchInfo.setOperator("admin");
            batchInfo.setAccording("ISO 11820:2022");
            batchInfo.setApparatusName("一号试验炉");
            batchInfo.setApparatusId("FURNACE-01");
            batchInfo.setPreWeight(50.0 + i);
            batchInfo.setPostWeight(45.0 + i * 0.8);
            batchInfo.setLostWeight(5.0 + i * 0.2);
            batchInfo.setLostWeightPer(10.0 + i);
            batchInfo.setMaxTf1(750.0 + i * 0.1);
            batchInfo.setDeltaTf(680.0 + i * 2);
            batchInfo.setTotalTestTime(3600);
            batchInfo.setConstPower(2048);
            batchInfo.setAmbientTemp(26.0);
            batchInfo.setAmbientHumidity(60.0);
            batchInfo.evaluatePassed();

            // 准备 CSV 数据
            prepareCsvData(batchInfo.getProductId(), batchInfo.getTestId(), 60);

            batch.add(batchInfo);
        }

        List<Path> batchFiles = excel.exportBatch(batch);
        log.info("Excel 批量导出完成: {} 个文件", batchFiles.size());
        for (Path f : batchFiles) {
            System.out.println("  " + f);
        }
    }

    // ============================================================
    //  PDF 导出示例
    // ============================================================

    public static void pdfExportDemo() {
        PdfReportService pdf = PdfReportService.getInstance();

        // 可选：设置自定义中文字体路径
        // pdf.setCustomFontPath("D:/fonts/msyh.ttf");

        ExportTestInfo info = buildMockTestInfo();
        String pid = info.getProductId();
        String tid = info.getTestId();

        // 准备 CSV 数据
        prepareCsvData(pid, tid, 120);

        // ---- 1. 无回调导出 ----
        Path pdfFile = pdf.export(info);
        log.info("PDF 报告已生成: {}", pdfFile);

        // ---- 2. 带进度回调导出 ----
        Path pdfFile2 = pdf.export(info, (current, total) -> {
            double percent = (double) current / total * 100;
            System.out.printf("PDF 导出进度: %d/%d (%.0f%%)%n", current, total, percent);
        });
        log.info("PDF 报告（带进度）已生成: {}", pdfFile2);
    }

    // ============================================================
    //  综合示例：完整导出流程
    // ============================================================

    public static void fullExportDemo() {
        log.info("========== 开始完整导出流程 ==========");

        // 1. 确保基础目录
        FilePathManageUtil.getInstance().ensureAllBaseDirs();

        // 2. 构造数据
        ExportTestInfo info = buildMockTestInfo();
        String pid = info.getProductId();
        String tid = info.getTestId();

        // 3. 准备 CSV 温度数据
        prepareCsvData(pid, tid, 3600); // 模拟 3600 秒完整数据

        // 4. Excel 导出
        Path excelPath = ExcelReportService.getInstance().exportSingle(info);
        log.info("✅ Excel: {}", excelPath);

        // 5. PDF 导出
        Path pdfPath = PdfReportService.getInstance().export(info, (cur, total) ->
                System.out.printf("  PDF 进度: %d/%d%n", cur, total));
        log.info("✅ PDF: {}", pdfPath);

        log.info("========== 完整导出流程结束 ==========");
    }

    // ============================================================
    //  main
    // ============================================================

    public static void main(String[] args) {
        // 初始化基础目录
        FilePathManageUtil.getInstance().ensureAllBaseDirs();

        System.out.println("========== Excel 导出示例 ==========");
        excelExportDemo();

        System.out.println("\n========== PDF 导出示例 ==========");
        pdfExportDemo();

        System.out.println("\n========== 完整导出流程 ==========");
        fullExportDemo();

        System.out.println("\n========== 第 3 轮示例完成 ==========");
    }
}