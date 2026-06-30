package com.iso11820.service;

import com.itextpdf.io.font.FontProgram;
import com.itextpdf.io.font.FontProgramFactory;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.iso11820.service.model.ExportTestInfo;
import com.iso11820.utils.*;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <h1>PDF 试验报告导出服务 — 线程安全单例</h1>
 *
 * <p>基于 iText7 生成 A4 标准 PDF 报告。内容包含：
 * <ul>
 *   <li>报告标题、编号</li>
 *   <li>试验基础信息表格</li>
 *   <li>温度统计摘要</li>
 *   <li>判定结论</li>
 *   <li>温度曲线占位图</li>
 *   <li>操作员签字栏</li>
 * </ul>
 *
 * <h2>中文字体</h2>
 * 自动按以下顺序查找中文字体：<br>
 * 1. classpath 下的 {@code fonts/NotoSansSC-Regular.otf}<br>
 * 2. 系统字体 {@code C:/Windows/Fonts/simsun.ttc,0}<br>
 * 3. 可通过 {@link #setCustomFontPath(String)} 指定自定义字体路径。
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 *   PdfReportService pdf = PdfReportService.getInstance();
 *
 *   // 设置字体路径（可选）
 *   pdf.setCustomFontPath("D:/fonts/msyh.ttf");
 *
 *   // 导出
 *   Path file = pdf.export(testInfo, (current, total) -> {
 *       System.out.println("进度: " + current + "/" + total);
 *   });
 * }</pre>
 *
 * @author Project Team - Utility Layer
 * @version 1.0
 * @since 2026-06-30
 */
public final class PdfReportService {

    // ============================================================
    //  单例
    // ============================================================

    private static volatile PdfReportService INSTANCE;

    public static PdfReportService getInstance() {
        if (INSTANCE == null) {
            synchronized (PdfReportService.class) {
                if (INSTANCE == null) {
                    INSTANCE = new PdfReportService();
                }
            }
        }
        return INSTANCE;
    }

    // ============================================================
    //  常量
    // ============================================================

    /** 主题色 */
    private static final DeviceRgb COLOR_PRIMARY = new DeviceRgb(30, 60, 120);
    /** 浅灰背景 */
    private static final DeviceRgb COLOR_LIGHT_GRAY = new DeviceRgb(240, 240, 245);
    /** 通过色 */
    private static final DeviceRgb COLOR_PASS = new DeviceRgb(0, 128, 0);
    /** 不通过色 */
    private static final DeviceRgb COLOR_FAIL = new DeviceRgb(200, 0, 0);

    /** 表格边框 */
    private static final SolidBorder TABLE_BORDER = new SolidBorder(new DeviceRgb(180, 180, 190), 0.5f);

    // ============================================================
    //  字段
    // ============================================================

    private final Logger log = LogUtil.getLogger("EXPORT");
    private final AppConfig config;
    private final FilePathManageUtil pathUtil;
    private final ReentrantLock writeLock = new ReentrantLock();

    /** 中文字体缓存 */
    private volatile PdfFont chineseFont;
    /** 自定义字体路径 */
    private volatile String customFontPath;

    private PdfReportService() {
        this.config = AppConfig.getInstance();
        this.pathUtil = FilePathManageUtil.getInstance();
    }

    // ============================================================
    //  字体配置
    // ============================================================

    /**
     * 设置自定义中文字体文件路径。
     * 必须在首次调用 {@link #export} 之前设置。
     *
     * @param fontPath 字体文件绝对路径，如 {@code "D:/fonts/msyh.ttf"}
     */
    public void setCustomFontPath(String fontPath) {
        if (fontPath == null || fontPath.isBlank()) {
            throw new IllegalArgumentException("字体路径不能为空");
        }
        this.customFontPath = fontPath;
        this.chineseFont = null; // 重置缓存
    }

    /**
     * 加载中文字体，按优先级：自定义路径 → classpath → 系统字体。
     */
    private PdfFont loadChineseFont() throws IOException {
        if (chineseFont != null) {
            return chineseFont;
        }

        // 1) 自定义路径
        if (customFontPath != null) {
            try {
                FontProgram fp = FontProgramFactory.createFont(customFontPath);
                chineseFont = PdfFontFactory.createFont(fp, PdfEncodings.IDENTITY_H);
                log.info("中文字体已加载（自定义）: {}", customFontPath);
                return chineseFont;
            } catch (IOException e) {
                log.warn("自定义字体加载失败: {}", customFontPath);
            }
        }

        // 2) classpath 资源
        String[] classpathFonts = {"fonts/NotoSansSC-Regular.otf", "fonts/simsun.ttf"};
        for (String cp : classpathFonts) {
            try {
                byte[] fontBytes = loadClasspathFont(cp);
                if (fontBytes != null) {
                    FontProgram fp = FontProgramFactory.createFont(fontBytes);
                    chineseFont = PdfFontFactory.createFont(fp, PdfEncodings.IDENTITY_H);
                    log.info("中文字体已加载（classpath）: {}", cp);
                    return chineseFont;
                }
            } catch (IOException e) {
                log.debug("classpath 字体加载失败: {}", cp);
            }
        }

        // 3) 系统字体（Windows）
        try {
            FontProgram fp = FontProgramFactory.createFont("C:/Windows/Fonts/simsun.ttc,0");
            chineseFont = PdfFontFactory.createFont(fp, PdfEncodings.IDENTITY_H);
            log.info("中文字体已加载（系统）: simsun.ttc");
            return chineseFont;
        } catch (IOException e) {
            log.debug("系统字体加载失败");
        }

        // 4) 最终兜底：使用 Helvetica（不支持中文，仅用于开发阶段）
        log.warn("未找到中文字体，将使用 Helvetica 兜底（中文可能无法正常显示）");
        chineseFont = PdfFontFactory.createFont();
        return chineseFont;
    }

    /**
     * 从 classpath 读取字体文件字节。
     */
    private byte[] loadClasspathFont(String path) {
        try (java.io.InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is != null) {
                return is.readAllBytes();
            }
        } catch (IOException e) {
            // ignore
        }
        return null;
    }

    // ============================================================
    //  对外导出方法
    // ============================================================

    /**
     * 导出单个试验 PDF 报告。
     *
     * @param info     试验数据实体，不能为 null
     * @param callback 进度回调（可为 null），参数 (currentStep, totalSteps)
     * @return 生成的 PDF 文件路径
     * @throws RuntimeException 导出失败时抛出
     */
    public Path export(ExportTestInfo info, ProgressCallback callback) {
        if (info == null) {
            throw new IllegalArgumentException("ExportTestInfo 不能为 null");
        }
        return exportInternal(info, callback);
    }

    /**
     * 导出单个试验 PDF 报告（无进度回调）。
     */
    public Path export(ExportTestInfo info) {
        return export(info, null);
    }

    // ============================================================
    //  内部导出逻辑
    // ============================================================

    private Path exportInternal(ExportTestInfo info, ProgressCallback callback) {
        writeLock.lock();
        try {
            // 确保目录
            pathUtil.ensureReportDir();
            Path filePath = pathUtil.getReportPath(info.getTestId(), "pdf");

            int totalSteps = 6;
            notifyProgress(callback, 0, totalSteps);

            try (PdfDocument pdfDoc = new PdfDocument(new PdfWriter(filePath.toFile()));
                 Document document = new Document(pdfDoc, PageSize.A4)) {

                // 设置页边距
                document.setMargins(36, 36, 36, 36);

                // 加载中文字体
                PdfFont cFont = loadChineseFont();
                document.setFont(cFont);

                // 1) 报告标题
                addTitle(document, cFont, info);
                notifyProgress(callback, 1, totalSteps);

                // 2) 基本信息表
                addInfoTable(document, cFont, info);
                notifyProgress(callback, 2, totalSteps);

                // 3) 温度统计摘要
                addTempSummary(document, cFont, info);
                notifyProgress(callback, 3, totalSteps);

                // 4) 判定结论
                addConclusion(document, cFont, info);
                notifyProgress(callback, 4, totalSteps);

                // 5) 温度曲线占位图
                addChartPlaceholder(document, cFont);
                notifyProgress(callback, 5, totalSteps);

                // 6) 签字栏 + 页脚
                addSignatureAndFooter(document, cFont, info);
                notifyProgress(callback, 6, totalSteps);

                log.info("PDF 报告已生成: {}", filePath);
                return filePath;
            }

        } catch (IOException e) {
            log.error("PDF 报告生成失败: {}", info.getTestId(), e);
            throw new RuntimeException("PDF 报告生成失败: " + info.getTestId(), e);
        } finally {
            writeLock.unlock();
        }
    }

    // ============================================================
    //  1. 报告标题
    // ============================================================

    private void addTitle(Document document, PdfFont cFont, ExportTestInfo info) {
        // 主标题
        Paragraph title = new Paragraph(
                config.getString("Report.ReportTitlePrefix", "ISO 11820 不燃性试验报告"))
                .setFont(cFont)
                .setFontSize(18)
                .setBold()
                .setFontColor(COLOR_PRIMARY)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(title);

        // 副标题：报告编号
        Paragraph subTitle = new Paragraph("报告编号：" + (info.getReportNo().isEmpty() ? info.getTestId() : info.getReportNo()))
                .setFont(cFont)
                .setFontSize(10)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(subTitle);

        // 分隔线
        document.add(new Paragraph("").setHeight(8));
        document.add(createDivider());
    }

    // ============================================================
    //  2. 基本信息表
    // ============================================================

    private void addInfoTable(Document document, PdfFont cFont, ExportTestInfo info) {
        Paragraph sectionTitle = new Paragraph("一、试验基本信息")
                .setFont(cFont).setFontSize(13).setBold()
                .setFontColor(COLOR_PRIMARY);
        document.add(sectionTitle);
        document.add(new Paragraph("").setHeight(4));

        // 创建 2 列 × N 行表格
        String[][] rows = {
                {"样品编号", info.getProductId()},
                {"试验 ID", info.getTestId()},
                {"试验日期", info.getTestDate()},
                {"操作员", info.getOperator()},
                {"试验依据", info.getAccording()},
                {"设备名称", info.getApparatusName()},
                {"环境温度", val(info.getAmbientTemp()) == null ? "--" : NumUtil.formatTemp(info.getAmbientTemp())},
                {"环境湿度", val(info.getAmbientHumidity()) == null ? "--" : NumUtil.format(info.getAmbientHumidity(), 1) + " %"},
                {"试验前质量", val(info.getPreWeight()) == null ? "--" : NumUtil.formatWeight(info.getPreWeight())},
                {"试验后质量", val(info.getPostWeight()) == null ? "--" : NumUtil.formatWeight(info.getPostWeight())},
                {"失重率", val(info.getLostWeightPer()) == null ? "--" : NumUtil.format(info.getLostWeightPer(), 2) + " %"},
                {"总试验时长", info.getTotalTestTime() == 0 ? "--" : DateUtil.readableDuration(info.getTotalTestTime())},
        };

        Table table = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                .useAllAvailableWidth();
        table.setBorder(TABLE_BORDER);

        for (int i = 0; i < rows.length; i++) {
            Cell labelCell = new Cell()
                    .add(new Paragraph(rows[i][0]).setFont(cFont).setFontSize(9).setBold())
                    .setBackgroundColor(COLOR_LIGHT_GRAY)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setPadding(5);
            Cell valueCell = new Cell()
                    .add(new Paragraph(rows[i][1]).setFont(cFont).setFontSize(9))
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setPadding(5);
            table.addCell(labelCell);
            table.addCell(valueCell);
        }

        document.add(table);
        document.add(new Paragraph("").setHeight(12));
    }

    // ============================================================
    //  3. 温度统计摘要
    // ============================================================

    private void addTempSummary(Document document, PdfFont cFont, ExportTestInfo info) {
        Paragraph sectionTitle = new Paragraph("二、温度统计摘要")
                .setFont(cFont).setFontSize(13).setBold()
                .setFontColor(COLOR_PRIMARY);
        document.add(sectionTitle);
        document.add(new Paragraph("").setHeight(4));

        String[][] rows = {
                {"通道", "最大值(°C)", "最终值(°C)", "温升(°C)"},
                {"炉温1", ExportTestInfo.formatTemp(info.getMaxTf1()), ExportTestInfo.formatTemp(info.getFinalTf1()), ExportTestInfo.formatTemp(info.getDeltaTf1())},
                {"炉温2", ExportTestInfo.formatTemp(info.getMaxTf2()), ExportTestInfo.formatTemp(info.getFinalTf2()), ExportTestInfo.formatTemp(info.getDeltaTf2())},
                {"表面温", ExportTestInfo.formatTemp(info.getMaxTs()), ExportTestInfo.formatTemp(info.getFinalTs()), ExportTestInfo.formatTemp(info.getDeltaTs())},
                {"中心温", ExportTestInfo.formatTemp(info.getMaxTc()), ExportTestInfo.formatTemp(info.getFinalTc()), ExportTestInfo.formatTemp(info.getDeltaTc())},
                {"综合温升", "--", "--", ExportTestInfo.formatTemp(info.getDeltaTf())},
        };

        Table table = new Table(UnitValue.createPercentArray(new float[]{25, 25, 25, 25}))
                .useAllAvailableWidth();
        table.setBorder(TABLE_BORDER);

        for (int i = 0; i < rows.length; i++) {
            for (String cellText : rows[i]) {
                Cell cell = new Cell()
                        .add(new Paragraph(cellText).setFont(cFont).setFontSize(9))
                        .setVerticalAlignment(VerticalAlignment.MIDDLE)
                        .setPadding(4)
                        .setTextAlignment(TextAlignment.CENTER);
                if (i == 0) {
                    cell.setBackgroundColor(COLOR_PRIMARY)
                            .setFontColor(ColorConstants.WHITE)
                            .setBold();
                }
                table.addCell(cell);
            }
        }

        document.add(table);
        document.add(new Paragraph("").setHeight(12));
    }

    // ============================================================
    //  4. 判定结论
    // ============================================================

    private void addConclusion(Document document, PdfFont cFont, ExportTestInfo info) {
        Paragraph sectionTitle = new Paragraph("三、判定结论")
                .setFont(cFont).setFontSize(13).setBold()
                .setFontColor(COLOR_PRIMARY);
        document.add(sectionTitle);
        document.add(new Paragraph("").setHeight(4));

        // 判定标准
        Paragraph criteria = new Paragraph("判定标准（ISO 11820 简化版）：综合温升 ΔTf ≤ 50°C 且 失重率 ≤ 50% 且 火焰持续时间 < 5 秒")
                .setFont(cFont).setFontSize(9).setFontColor(ColorConstants.GRAY);
        document.add(criteria);
        document.add(new Paragraph("").setHeight(6));

        // 指标
        Table table = new Table(UnitValue.createPercentArray(new float[]{35, 25, 40}))
                .useAllAvailableWidth();
        table.setBorder(TABLE_BORDER);

        addConclusionRow(table, cFont, "综合温升 ΔTf", NumUtil.formatTemp(info.getDeltaTf()), "≤ 50°C", true);
        addConclusionRow(table, cFont, "失重率", NumUtil.format(info.getLostWeightPer(), 2) + " %", "≤ 50%", true);
        addConclusionRow(table, cFont, "火焰持续时间", info.getFlameDuration() + " 秒", "< 5 秒", true);

        document.add(table);
        document.add(new Paragraph("").setHeight(8));

        // 结论
        DeviceRgb conclusionColor = info.isPassed() ? COLOR_PASS : COLOR_FAIL;
        String conclusionText = info.isPassed() ? "✅ 判定通过 — 符合不燃性材料标准" : "❌ 判定不通过 — 不符合不燃性材料标准";
        Paragraph conclusion = new Paragraph(conclusionText)
                .setFont(cFont).setFontSize(14).setBold()
                .setFontColor(conclusionColor)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(conclusion);
        document.add(new Paragraph("").setHeight(12));
    }

    private void addConclusionRow(Table table, PdfFont cFont, String indicator, String actual, String limit, boolean isHeader) {
        table.addCell(new Cell().add(new Paragraph(indicator).setFont(cFont).setFontSize(9).setBold())
                .setBackgroundColor(COLOR_LIGHT_GRAY).setPadding(4));
        table.addCell(new Cell().add(new Paragraph(actual).setFont(cFont).setFontSize(9))
                .setPadding(4).setTextAlignment(TextAlignment.CENTER));
        table.addCell(new Cell().add(new Paragraph(limit).setFont(cFont).setFontSize(9))
                .setPadding(4).setTextAlignment(TextAlignment.CENTER));
    }

    // ============================================================
    //  5. 温度曲线占位图
    // ============================================================

    private void addChartPlaceholder(Document document, PdfFont cFont) {
        Paragraph sectionTitle = new Paragraph("四、温度曲线图（占位）")
                .setFont(cFont).setFontSize(13).setBold()
                .setFontColor(COLOR_PRIMARY);
        document.add(sectionTitle);
        document.add(new Paragraph("").setHeight(4));

        // 占位矩形框
        Table placeholder = new Table(1).useAllAvailableWidth();
        Cell cell = new Cell()
                .setHeight(250)
                .setBorder(new SolidBorder(new DeviceRgb(180, 180, 190), 1))
                .setBackgroundColor(new DeviceRgb(248, 248, 250));
        Paragraph placeholderText = new Paragraph("温度曲线图\n（请参见 Excel 报告 Sheet3 或使用 XChart 生成）")
                .setFont(cFont).setFontSize(10)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER);
        cell.add(placeholderText);
        cell.setVerticalAlignment(VerticalAlignment.MIDDLE);
        placeholder.addCell(cell);
        document.add(placeholder);
        document.add(new Paragraph("").setHeight(12));
    }

    // ============================================================
    //  6. 签字栏 + 页脚
    // ============================================================

    private void addSignatureAndFooter(Document document, PdfFont cFont, ExportTestInfo info) {
        // 签字栏
        document.add(new Paragraph("").setHeight(20));
        Table sigTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .useAllAvailableWidth();
        sigTable.addCell(new Cell().add(new Paragraph("操作员签字：_______________").setFont(cFont).setFontSize(10))
                .setBorder(Border.NO_BORDER).setPadding(5));
        sigTable.addCell(new Cell().add(new Paragraph("日期：_______________").setFont(cFont).setFontSize(10))
                .setBorder(Border.NO_BORDER).setPadding(5));
        document.add(sigTable);

        // 备注
        if (!info.getMemo().isEmpty()) {
            document.add(new Paragraph("").setHeight(6));
            Paragraph memo = new Paragraph("备注：" + info.getMemo())
                    .setFont(cFont).setFontSize(8).setFontColor(ColorConstants.GRAY);
            document.add(memo);
        }

        // 页脚
        document.add(new Paragraph("").setHeight(8));
        document.add(createDivider());
        String footerText = config.getString("Report.ReportFooter", "本报告由 ISO 11820 仿真系统自动生成");
        Paragraph footer = new Paragraph(footerText)
                .setFont(cFont).setFontSize(7)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(footer);
    }

    // ============================================================
    //  辅助方法
    // ============================================================

    /** 分隔线 */
    private LineSeparator createDivider() {
        return new LineSeparator(new SolidBorder(new DeviceRgb(200, 200, 210), 0.5f));
    }

    /** 空值判断 */
    private Double val(double v) {
        return v == 0.0 ? null : v;
    }

    /** 进度回调 */
    private void notifyProgress(ProgressCallback callback, int step, int total) {
        if (callback != null) {
            try {
                callback.onProgress(step, total);
            } catch (Exception e) {
                log.warn("进度回调异常: {}", e.getMessage());
            }
        }
    }

    // ============================================================
    //  进度回调接口
    // ============================================================

    /**
     * 导出进度回调函数式接口。
     */
    @FunctionalInterface
    public interface ProgressCallback {
        /**
         * @param current 当前步骤（1-based）
         * @param total   总步骤数
         */
        void onProgress(int current, int total);
    }
}