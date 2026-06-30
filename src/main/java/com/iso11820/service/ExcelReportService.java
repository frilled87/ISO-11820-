package com.iso11820.service;

import com.iso11820.service.entity.DataPoint;
import com.iso11820.service.model.ExportTestInfo;
import com.iso11820.utils.*;
import org.apache.poi.common.usermodel.fonts.FontCharset;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.charts.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.slf4j.Logger;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <h1>Excel 试验报告导出服务 — 线程安全单例</h1>
 *
 * <p>基于 Apache POI 5.x 生成 {@code .xlsx} 格式试验报告。
 * 固定输出 3 个 Sheet：试验信息、温度数据、温度曲线图。</p>
 *
 * <h2>Sheet 结构</h2>
 * <table>
 *   <tr><th>Sheet</th><th>名称</th><th>内容</th></tr>
 *   <tr><td>1</td><td>试验信息</td><td>居中加粗表头、自适应列宽、配色简洁</td></tr>
 *   <tr><td>2</td><td>温度数据</td><td>完整时序温度数据（从 CSV 读取）</td></tr>
 *   <tr><td>3</td><td>温度曲线</td><td>4 条折线（炉温1/2、表面温、中心温）</td></tr>
 * </table>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 *   ExcelReportService excel = ExcelReportService.getInstance();
 *
 *   // 单试验导出
 *   Path file = excel.exportSingle(testInfo);
 *
 *   // 批量导出
 *   List<Path> files = excel.exportBatch(testInfoList);
 * }</pre>
 *
 * @author Project Team - Utility Layer
 * @version 1.0
 * @since 2026-06-30
 */
public final class ExcelReportService {

    private static volatile ExcelReportService INSTANCE;

    private final Logger log = LogUtil.getLogger("EXPORT");
    private final AppConfig config;
    private final FilePathManageUtil pathUtil;
    private final ReentrantLock writeLock = new ReentrantLock();

    /** 固定列宽（字符数） */
    private static final int COL_WIDTH_LABEL = 20;
    private static final int COL_WIDTH_VALUE = 30;
    private static final int COL_WIDTH_DATA = 18;

    /** 图表尺寸（列跨度） */
    private static final int CHART_COL1 = 0;
    private static final int CHART_COL2 = 8;
    private static final int CHART_ROW1 = 0;
    private static final int CHART_ROW2 = 25;

    private ExcelReportService() {
        this.config = AppConfig.getInstance();
        this.pathUtil = FilePathManageUtil.getInstance();
    }

    public static ExcelReportService getInstance() {
        if (INSTANCE == null) {
            synchronized (ExcelReportService.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ExcelReportService();
                }
            }
        }
        return INSTANCE;
    }

    // ============================================================
    //  对外方法
    // ============================================================

    /**
     * 导出单个试验报告。
     *
     * @param info 试验数据实体，不能为 null
     * @return 生成的 Excel 文件路径
     * @throws IllegalArgumentException info 为 null
     * @throws RuntimeException 导出失败时抛出
     */
    public Path exportSingle(ExportTestInfo info) {
        if (info == null) {
            throw new IllegalArgumentException("ExportTestInfo 不能为 null");
        }
        return exportInternal(info);
    }

    /**
     * 批量导出多个试验报告。
     * 每个试验独立一个 xlsx 文件。
     *
     * @param infoList 试验数据列表，不能为 null
     * @return 生成的文件路径列表
     */
    public List<Path> exportBatch(List<ExportTestInfo> infoList) {
        if (infoList == null || infoList.isEmpty()) {
            log.warn("exportBatch 跳过: 试验列表为空");
            return Collections.emptyList();
        }
        List<Path> results = new ArrayList<>();
        for (ExportTestInfo info : infoList) {
            try {
                results.add(exportInternal(info));
            } catch (Exception e) {
                log.error("批量导出失败: {} / {}", info.getProductId(), info.getTestId(), e);
            }
        }
        log.info("批量导出完成: 成功 {}/{}", results.size(), infoList.size());
        return results;
    }

    // ============================================================
    //  内部导出逻辑
    // ============================================================

    private Path exportInternal(ExportTestInfo info) {
        writeLock.lock();
        try {
            // 确保目录存在
            pathUtil.ensureReportDir();

            Path filePath = pathUtil.getReportPath(info.getTestId(), "xlsx");

            try (XSSFWorkbook workbook = new XSSFWorkbook()) {
                // 创建样式
                Map<String, CellStyle> styles = createStyles(workbook);

                // Sheet 1: 试验信息
                createInfoSheet(workbook, info, styles);

                // Sheet 2: 温度数据
                createDataSheet(workbook, info, styles);

                // Sheet 3: 温度曲线
                createChartSheet(workbook, info);

                // 写入文件
                try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                    workbook.write(fos);
                }

                log.info("Excel 报告已生成: {}", filePath);
                return filePath;
            }

        } catch (IOException e) {
            log.error("Excel 报告生成失败: {}", info.getTestId(), e);
            throw new RuntimeException("Excel 报告生成失败: " + info.getTestId(), e);
        } finally {
            writeLock.unlock();
        }
    }

    // ============================================================
    //  Sheet 1: 试验信息
    // ============================================================

    private void createInfoSheet(XSSFWorkbook wb, ExportTestInfo info, Map<String, CellStyle> styles) {
        String sheetName = config.getString("Report.ExcelSheet1Name", "试验信息");
        XSSFSheet sheet = wb.createSheet(sheetName);

        // 标题行
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(config.getString("Report.ReportTitlePrefix", "ISO 11820 试验报告"));
        titleCell.setCellStyle(styles.get("title"));
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));

        // 空行
        sheet.createRow(1);

        // 信息条目（标签 + 值）
        int row = 2;
        row = addInfoRow(sheet, row, "样品编号", info.getProductId(), styles);
        row = addInfoRow(sheet, row, "试验ID", info.getTestId(), styles);
        row = addInfoRow(sheet, row, "试验日期", info.getTestDate(), styles);
        row = addInfoRow(sheet, row, "操作员", info.getOperator(), styles);
        row = addInfoRow(sheet, row, "试验依据", info.getAccording(), styles);
        row = addInfoRow(sheet, row, "设备名称", info.getApparatusName(), styles);
        row = addInfoRow(sheet, row, "设备编号", info.getApparatusId(), styles);
        row++;
        row = addInfoRow(sheet, row, "环境温度", info.getAmbientTemp() == 0 ? "--" : NumUtil.formatTemp(info.getAmbientTemp()), styles);
        row = addInfoRow(sheet, row, "环境湿度", info.getAmbientHumidity() == 0 ? "--" : NumUtil.format(info.getAmbientHumidity(), 1) + " %", styles);
        row++;
        row = addInfoRow(sheet, row, "试验前质量", info.getPreWeight() == 0 ? "--" : NumUtil.formatWeight(info.getPreWeight()), styles);
        row = addInfoRow(sheet, row, "试验后质量", info.getPostWeight() == 0 ? "--" : NumUtil.formatWeight(info.getPostWeight()), styles);
        row = addInfoRow(sheet, row, "失重量", info.getLostWeight() == 0 ? "--" : NumUtil.format(info.getLostWeight(), 2) + " g", styles);
        row = addInfoRow(sheet, row, "失重率", info.getLostWeightPer() == 0 ? "--" : NumUtil.format(info.getLostWeightPer(), 2) + " %", styles);
        row++;
        row = addInfoRow(sheet, row, "炉温1 最大值", ExportTestInfo.formatTemp(info.getMaxTf1()), styles);
        row = addInfoRow(sheet, row, "炉温2 最大值", ExportTestInfo.formatTemp(info.getMaxTf2()), styles);
        row = addInfoRow(sheet, row, "表面温 最大值", ExportTestInfo.formatTemp(info.getMaxTs()), styles);
        row = addInfoRow(sheet, row, "中心温 最大值", ExportTestInfo.formatTemp(info.getMaxTc()), styles);
        row++;
        row = addInfoRow(sheet, row, "综合温升 ΔTf", ExportTestInfo.formatTemp(info.getDeltaTf()), styles);
        row = addInfoRow(sheet, row, "表面温升", ExportTestInfo.formatTemp(info.getDeltaTs()), styles);
        row = addInfoRow(sheet, row, "中心温升", ExportTestInfo.formatTemp(info.getDeltaTc()), styles);
        row++;
        row = addInfoRow(sheet, row, "总试验时长", info.getTotalTestTime() == 0 ? "--" : DateUtil.readableDuration(info.getTotalTestTime()), styles);
        row = addInfoRow(sheet, row, "恒功率值", info.getConstPower() == 0 ? "--" : String.valueOf(info.getConstPower()), styles);
        row = addInfoRow(sheet, row, "火焰持续时间", info.getFlameTime() == 0 ? "无火焰" : info.getFlameDuration() + " 秒", styles);
        row++;
        row = addInfoRow(sheet, row, "判定结论", info.getConclusion().isEmpty() ? "--" : info.getConclusion(), styles);
        row = addInfoRow(sheet, row, "是否通过", info.isPassed() ? "✅ 通过" : "❌ 不通过", styles);
        row++;
        if (!info.getMemo().isEmpty()) {
            addInfoRow(sheet, row, "备注", info.getMemo(), styles);
        }

        // 设置列宽
        sheet.setColumnWidth(0, COL_WIDTH_LABEL * 256);
        sheet.setColumnWidth(1, COL_WIDTH_VALUE * 256);
    }

    private int addInfoRow(Sheet sheet, int rowNum, String label, String value, Map<String, CellStyle> styles) {
        Row row = sheet.createRow(rowNum);
        Cell c0 = row.createCell(0);
        c0.setCellValue(label);
        c0.setCellStyle(styles.get("label"));
        Cell c1 = row.createCell(1);
        c1.setCellValue(value);
        c1.setCellStyle(styles.get("value"));
        return rowNum + 1;
    }

    // ============================================================
    //  Sheet 2: 温度数据
    // ============================================================

    private void createDataSheet(XSSFWorkbook wb, ExportTestInfo info, Map<String, CellStyle> styles) {
        String sheetName = config.getString("Report.ExcelSheet2Name", "温度数据");
        XSSFSheet sheet = wb.createSheet(sheetName);

        // 表头
        String[] headers = {"时间(s)", "炉温1(°C)", "炉温2(°C)", "表面温(°C)", "中心温(°C)", "校准温(°C)", "环境温(°C)", "湿度(%)", "时间戳"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(styles.get("header"));
        }

        // 从 CSV 读取数据
        List<DataPoint> dataPoints = CsvDataService.getInstance().readAll(info.getProductId(), info.getTestId());
        if (dataPoints.isEmpty()) {
            log.debug("温度数据为空: {}/{}", info.getProductId(), info.getTestId());
        }

        for (int i = 0; i < dataPoints.size(); i++) {
            DataPoint dp = dataPoints.get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(dp.getTimeSeconds());
            row.createCell(1).setCellValue(dp.getTf1());
            row.createCell(2).setCellValue(dp.getTf2());
            row.createCell(3).setCellValue(dp.getTs());
            row.createCell(4).setCellValue(dp.getTc());
            row.createCell(5).setCellValue(dp.gettCal());
            row.createCell(6).setCellValue(dp.getAmbientTemp());
            row.createCell(7).setCellValue(dp.getAmbientHumidity());
            row.createCell(8).setCellValue(dp.getTimestamp());
        }

        // 自适应列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.setColumnWidth(i, COL_WIDTH_DATA * 256);
        }
    }

    // ============================================================
    //  Sheet 3: 温度曲线图
    // ============================================================

    private void createChartSheet(XSSFWorkbook wb, ExportTestInfo info) {
        String sheetName = config.getString("Report.ExcelSheet3Name", "温度曲线");
        XSSFSheet sheet = wb.createSheet(sheetName);

        List<DataPoint> dataPoints = CsvDataService.getInstance().readAll(info.getProductId(), info.getTestId());
        if (dataPoints.isEmpty()) {
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue("无温度数据，无法生成曲线图");
            return;
        }

        int dataSize = dataPoints.size();
        // 为图表准备数据区域（辅助数据 Sheet，隐藏）
        XSSFSheet dataSheet = wb.createSheet("_chartData");

        // 写入数据：Time | TF1 | TF2 | TS | TC
        String[] chartHeaders = {"Time", "TF1", "TF2", "TS", "TC"};
        Row hRow = dataSheet.createRow(0);
        for (int i = 0; i < chartHeaders.length; i++) {
            hRow.createCell(i).setCellValue(chartHeaders[i]);
        }
        for (int i = 0; i < dataSize; i++) {
            DataPoint dp = dataPoints.get(i);
            Row row = dataSheet.createRow(i + 1);
            row.createCell(0).setCellValue(dp.getTimeSeconds());
            row.createCell(1).setCellValue(dp.getTf1());
            row.createCell(2).setCellValue(dp.getTf2());
            row.createCell(3).setCellValue(dp.getTs());
            row.createCell(4).setCellValue(dp.getTc());
        }

        // 创建图表
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(
                0, 0, 0, 0,
                CHART_COL1, CHART_ROW1,
                CHART_COL2, CHART_ROW2);

        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText("温度变化曲线");
        chart.setTitleOverlay(false);

        // 分类轴（X 轴）：时间
        XDDFDataSource<String> timeAxis = XDDFDataSourcesFactory.fromStringCellRange(
                dataSheet, new CellRangeAddress(1, dataSize, 0, 0));

        // 数值轴（Y 轴）：温度
        XDDFNumericalDataSource<Double> tf1Data = XDDFDataSourcesFactory.fromNumericCellRange(
                dataSheet, new CellRangeAddress(1, dataSize, 1, 1));
        XDDFNumericalDataSource<Double> tf2Data = XDDFDataSourcesFactory.fromNumericCellRange(
                dataSheet, new CellRangeAddress(1, dataSize, 2, 2));
        XDDFNumericalDataSource<Double> tsData = XDDFDataSourcesFactory.fromNumericCellRange(
                dataSheet, new CellRangeAddress(1, dataSize, 3, 3));
        XDDFNumericalDataSource<Double> tcData = XDDFDataSourcesFactory.fromNumericCellRange(
                dataSheet, new CellRangeAddress(1, dataSize, 4, 4));

        // 创建折线图
        XDDFChartAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        bottomAxis.setTitle("时间 (秒)");
        XDDFChartAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
        leftAxis.setTitle("温度 (°C)");
        leftAxis.setCrosses(AxisCrosses.AUTO_ZERO);

        XDDFLineChartData lineData = (XDDFLineChartData) chart.createData(ChartTypes.LINE, bottomAxis, leftAxis);

        addSeries(lineData, timeAxis, tf1Data, "炉温1", new byte[]{(byte) 220, 50, 50});    // 红色
        addSeries(lineData, timeAxis, tf2Data, "炉温2", new byte[]{(byte) 50, 100, 220});   // 蓝色
        addSeries(lineData, timeAxis, tsData, "表面温", new byte[]{(byte) 50, 160, 50});    // 绿色
        addSeries(lineData, timeAxis, tcData, "中心温", new byte[]{(byte) 200, 150, 0});    // 橙色

        chart.plot(lineData);

        // 隐藏辅助数据 Sheet
        wb.setSheetHidden(wb.getSheetIndex(dataSheet), true);
    }

    /**
     * 添加一条折线到图表。
     */
    private void addSeries(XDDFLineChartData lineData,
                           XDDFDataSource<String> timeAxis,
                           XDDFNumericalDataSource<Double> valueData,
                           String name, byte[] color) {
        XDDFLineChartData.Series series = (XDDFLineChartData.Series) lineData.addSeries(timeAxis, valueData);
        series.setTitle(name, null);
        series.setSmooth(false);
        // 设置线条颜色
        series.setMarkerStyle(MarkerStyle.NONE);
        // 注：POI 5.x 中 Series 线条颜色设置方式有限，通过预设颜色实现
    }

    // ============================================================
    //  样式工厂
    // ============================================================

    /**
     * 创建所有预定义样式。
     */
    private Map<String, CellStyle> createStyles(XSSFWorkbook wb) {
        Map<String, CellStyle> styles = new LinkedHashMap<>();

        // 字体
        XSSFFont titleFont = wb.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 16);
        titleFont.setFontName("微软雅黑");

        XSSFFont labelFont = wb.createFont();
        labelFont.setBold(true);
        labelFont.setFontHeightInPoints((short) 11);
        labelFont.setFontName("微软雅黑");

        XSSFFont valueFont = wb.createFont();
        valueFont.setFontHeightInPoints((short) 11);
        valueFont.setFontName("微软雅黑");

        XSSFFont headerFont = wb.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 11);
        headerFont.setFontName("微软雅黑");
        headerFont.setColor(IndexedColors.WHITE.getIndex());

        // 标题样式
        CellStyle titleStyle = wb.createCellStyle();
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        styles.put("title", titleStyle);

        // 标签样式（浅灰背景）
        CellStyle labelStyle = wb.createCellStyle();
        labelStyle.setFont(labelFont);
        labelStyle.setAlignment(HorizontalAlignment.RIGHT);
        labelStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        labelStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        labelStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        labelStyle.setBorderBottom(BorderStyle.THIN);
        labelStyle.setBorderTop(BorderStyle.THIN);
        labelStyle.setBorderLeft(BorderStyle.THIN);
        labelStyle.setBorderRight(BorderStyle.THIN);
        styles.put("label", labelStyle);

        // 值样式
        CellStyle valueStyle = wb.createCellStyle();
        valueStyle.setFont(valueFont);
        valueStyle.setAlignment(HorizontalAlignment.LEFT);
        valueStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        valueStyle.setBorderBottom(BorderStyle.THIN);
        valueStyle.setBorderTop(BorderStyle.THIN);
        valueStyle.setBorderLeft(BorderStyle.THIN);
        valueStyle.setBorderRight(BorderStyle.THIN);
        styles.put("value", valueStyle);

        // 表头样式（深色背景白色字）
        CellStyle headerStyle = wb.createCellStyle();
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        styles.put("header", headerStyle);

        return styles;
    }
}