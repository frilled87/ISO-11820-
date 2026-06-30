package com.project.service;

import com.project.service.model.ExportTestInfo;
import com.project.utils.*;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <h1>统一报告导出门面服务 — 线程安全单例</h1>
 *
 * <p>封装 {@link ExcelReportService} 和 {@link PdfReportService}，对外仅暴露一个极简
 * 核心方法 {@link #exportReport(ExportTestInfo, ExportProgressCallback)}。
 * 内部根据 {@link AppConfig} 配置自动判断导出格式组合。</p>
 *
 * <h2>配置驱动的导出策略</h2>
 * <table>
 *   <tr><th>配置项</th><th>行为</th></tr>
 *   <tr><td>EnableExcelExport=true + EnablePdfExport=true</td><td>同时生成 Excel + PDF</td></tr>
 *   <tr><td>EnableExcelExport=true</td><td>仅生成 Excel</td></tr>
 *   <tr><td>EnablePdfExport=true</td><td>仅生成 PDF</td></tr>
 *   <tr><td>均为 false</td><td>返回 false，记录 WARN 日志</td></tr>
 * </table>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 *   ReportExportService report = ReportExportService.getInstance();
 *
 *   // 最简单的调用方式
 *   boolean ok = report.exportReport(testInfo, null);
 *
 *   // 带进度回调
 *   boolean ok = report.exportReport(testInfo, (step, total, msg) -> {
 *       System.out.println("[" + step + "/" + total + "] " + msg);
 *   });
 * }</pre>
 *
 * @author Project Team - Utility Layer
 * @version 1.0
 * @since 2026-06-30
 */
public final class ReportExportService {

    // ============================================================
    //  单例
    // ============================================================

    private static volatile ReportExportService INSTANCE;

    /** 获取单例实例 */
    public static ReportExportService getInstance() {
        if (INSTANCE == null) {
            synchronized (ReportExportService.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ReportExportService();
                }
            }
        }
        return INSTANCE;
    }

    // ============================================================
    //  字段
    // ============================================================

    /** 统一日志标签 */
    private static final String LOG_TAG = "[REPORT_EXPORT]";

    /** 日志 */
    private final Logger log = LogUtil.getLogger("EXPORT");

    /** 配置 */
    private final AppConfig config;

    /** 路径工具 */
    private final FilePathManageUtil pathUtil;

    /** 写锁 */
    private final ReentrantLock lock = new ReentrantLock();

    private ReportExportService() {
        this.config = AppConfig.getInstance();
        this.pathUtil = FilePathManageUtil.getInstance();
    }

    // ============================================================
    //  核心方法
    // ============================================================

    /**
     * 导出试验报告（Excel + PDF，根据配置自动判断）。
     *
     * <p>内部自动完成：目录创建、CSV 数据读取、Excel 生成、PDF 生成。
     * 上层无需手动调用任何前置方法。</p>
     *
     * @param info     试验数据实体，不能为 null
     * @param callback 进度回调（可为 null）
     * @return true 表示至少一种格式导出成功
     * @throws IllegalArgumentException info 为 null
     */
    public boolean exportReport(ExportTestInfo info, ExportProgressCallback callback) {
        // ---- 参数校验 ----
        if (info == null) {
            log.error("{} exportReport 失败: ExportTestInfo 为 null", LOG_TAG);
            throw new IllegalArgumentException("ExportTestInfo 不能为 null");
        }
        if (info.getTestId() == null || info.getTestId().isBlank()) {
            log.error("{} exportReport 失败: testId 为空", LOG_TAG);
            return false;
        }

        lock.lock();
        try {
            // 确保报告目录存在
            pathUtil.ensureReportDir();

            boolean excelEnabled = config.isExcelExportEnabled();
            boolean pdfEnabled = config.isPdfExportEnabled();

            // 检查配置
            if (!excelEnabled && !pdfEnabled) {
                log.warn("{} Excel 和 PDF 导出均被禁用，跳过导出", LOG_TAG);
                notify(callback, 0, 1, "导出已禁用（配置）");
                return false;
            }

            int totalSteps = (excelEnabled ? 1 : 0) + (pdfEnabled ? 1 : 0);
            int currentStep = 0;
            boolean anySuccess = false;

            log.info("{} 开始导出: testId={}, Excel={}, PDF={}",
                    LOG_TAG, info.getTestId(), excelEnabled, pdfEnabled);

            // ---- Excel 导出 ----
            if (excelEnabled) {
                currentStep++;
                notify(callback, currentStep, totalSteps, "正在生成 Excel 报告...");
                try {
                    Path excelPath = ExcelReportService.getInstance().exportSingle(info);
                    log.info("{} ✅ Excel 报告已生成: {}", LOG_TAG, excelPath);
                    anySuccess = true;
                } catch (Exception e) {
                    log.error("{} ❌ Excel 导出失败: {}", LOG_TAG, info.getTestId(), e);
                }
            }

            // ---- PDF 导出 ----
            if (pdfEnabled) {
                currentStep++;
                notify(callback, currentStep, totalSteps, "正在生成 PDF 报告...");
                try {
                    Path pdfPath = PdfReportService.getInstance().export(info);
                    log.info("{} ✅ PDF 报告已生成: {}", LOG_TAG, pdfPath);
                    anySuccess = true;
                } catch (Exception e) {
                    log.error("{} ❌ PDF 导出失败: {}", LOG_TAG, info.getTestId(), e);
                }
            }

            // 汇总结果
            if (anySuccess) {
                log.info("{} 导出完成: testId={}", LOG_TAG, info.getTestId());
                notify(callback, totalSteps, totalSteps, "导出完成");
            } else {
                log.error("{} 导出全部失败: testId={}", LOG_TAG, info.getTestId());
                notify(callback, totalSteps, totalSteps, "导出失败");
            }

            return anySuccess;

        } finally {
            lock.unlock();
        }
    }

    /**
     * 导出试验报告（无回调版本）。
     *
     * @param info 试验数据实体
     * @return true 表示至少一种格式导出成功
     */
    public boolean exportReport(ExportTestInfo info) {
        return exportReport(info, null);
    }

    /**
     * 批量导出试验报告。
     *
     * @param infoList 试验数据列表
     * @param callback 进度回调（可为 null）
     * @return 成功导出的数量
     */
    public int exportBatchReport(List<ExportTestInfo> infoList, ExportProgressCallback callback) {
        if (infoList == null || infoList.isEmpty()) {
            log.warn("{} exportBatchReport 跳过: 列表为空", LOG_TAG);
            return 0;
        }

        int successCount = 0;
        int total = infoList.size();

        for (int i = 0; i < total; i++) {
            ExportTestInfo info = infoList.get(i);
            notify(callback, i + 1, total, "正在导出: " + info.getTestId());
            if (exportReport(info, null)) {
                successCount++;
            }
        }

        log.info("{} 批量导出完成: 成功 {}/{}", LOG_TAG, successCount, total);
        return successCount;
    }

    // ============================================================
    //  进度回调
    // ============================================================

    private void notify(ExportProgressCallback callback, int step, int total, String message) {
        if (callback != null) {
            try {
                callback.onProgress(step, total, message);
            } catch (Exception e) {
                log.warn("{} 进度回调异常: {}", LOG_TAG, e.getMessage());
            }
        }
    }

    /**
     * 统一导出进度回调函数式接口。
     */
    @FunctionalInterface
    public interface ExportProgressCallback {
        /**
         * @param currentStep 当前步骤（1-based）
         * @param totalSteps  总步骤数
         * @param message     当前步骤描述
         */
        void onProgress(int currentStep, int totalSteps, String message);
    }
}