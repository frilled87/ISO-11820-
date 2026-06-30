package com.iso11820.ui;

import com.iso11820.core.DataChangeListener;
import com.iso11820.core.SensorData;
import com.iso11820.core.TestMaster;
import com.iso11820.core.TestState;
import com.iso11820.dao.CalibrationRecordsDao;
import com.iso11820.dao.TestMasterDao;
import com.iso11820.dao.impl.CalibrationRecordsDaoImpl;
import com.iso11820.dao.impl.TestMasterDaoImpl;
import com.iso11820.entity.CalibrationRecords;
import com.iso11820.service.CsvDataService;
import com.iso11820.service.ExcelReportService;
import com.iso11820.service.PdfReportService;
import com.iso11820.service.entity.DataPoint;
import com.iso11820.service.model.ExportTestInfo;
import com.iso11820.ui.dialog.NewTestDialogController;
import com.iso11820.ui.dialog.TestRecordDialogController;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingNode;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.markers.SeriesMarkers;

import javax.swing.SwingUtilities;
import java.awt.Color;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 主界面控制器 —— 完整联调版本。
 *
 * @author ISO11820 Development Team
 * @version 3.0
 * @since 2026-06-29
 */
public class MainController {

    // ==================== 常量 ====================
    private static final int MAX_CHART_POINTS = 750;
    private static final double Y_MIN = 0.0;
    private static final double Y_MAX = 800.0;
    private static final double TICK_SECONDS = 0.8;

    // ==================== FXML 绑定 ====================

    // --- 顶部 ---
    @FXML private HBox topBar;
    @FXML private Label operatorLabel;
    @FXML private Label statusLabel;

    // --- 左侧温度面板 ---
    @FXML private VBox tempPanel;
    @FXML private Label tf1Value, tf2Value, tsValue, tcValue, tCalValue;

    // --- 中部 Tab ---
    @FXML private TabPane tabPane;
    @FXML private StackPane chartContainer;
    @FXML private HBox chartLegend;
    @FXML private Label legendTf1, legendTf2, legendTs, legendTc;

    // --- 右侧按钮 ---
    @FXML private VBox buttonPanel;
    @FXML private Button btnNewTest, btnStartHeating, btnStopHeating;
    @FXML private Button btnStartRecording, btnStopRecording, btnSettings, btnTestRecord;

    // --- 底部 ---
    @FXML private VBox logPanel;
    @FXML private TextArea logTextArea;
    @FXML private Button btnClearLog;
    @FXML private HBox statusBar;
    @FXML private Label productIdLabel, durationLabel, driftLabel;

    // --- Tab 2: 记录查询 ---
    @FXML private TextField queryStartDate, queryEndDate, queryProductId, queryOperator;
    @FXML private TableView<Object[]> queryTable;

    // --- Tab 3: 设备校准 ---
    @FXML private Label calibTempLabel;
    @FXML private TableView<Object[]> calibTable;

    // ==================== 内部状态 ====================
    private TestMaster testMaster;
    private String operatorName;
    private String currentProductId;
    private String currentTestId;
    private com.iso11820.entity.TestMaster currentDbEntity;
    private int customDurationSeconds = 3600;
    private final List<String> messageHistory = new ArrayList<>();

    // ==================== DAO ====================
    private final TestMasterDao testDao = new TestMasterDaoImpl();
    private final CalibrationRecordsDao calibDao = new CalibrationRecordsDaoImpl();
    private final CsvDataService csvService = CsvDataService.getInstance();

    // ==================== XChart ====================
    private SwingNode chartSwingNode;
    private XYChart xyChart;
    private XYSeries seriesTf1, seriesTf2, seriesTs, seriesTc;
    private final LinkedList<Double> xBuffer = new LinkedList<>();
    private final LinkedList<Double> yTf1Buffer = new LinkedList<>();
    private final LinkedList<Double> yTf2Buffer = new LinkedList<>();
    private final LinkedList<Double> yTsBuffer  = new LinkedList<>();
    private final LinkedList<Double> yTcBuffer  = new LinkedList<>();
    private double elapsedSeconds = 0.0;
    private volatile boolean chartReady = false;

    // ==================== 初始化 ====================
    public MainController() {}

    public void initController(String operatorName) {
        this.operatorName = operatorName;
        operatorLabel.setText(operatorName);
        this.testMaster = new TestMaster();
        testMaster.addDataChangeListener(new MainDataListener());
        initChart();
        initQueryTable();
        initCalibTable();
        updateButtonStates(TestState.IDLE);
        appendLog("系统初始化，操作员：" + operatorName, "info");
    }

    public String getOperatorName() { return operatorName; }
    public TestMaster getTestMaster() { return testMaster; }

    // ==================== 曲线图 ====================
    private void initChart() {
        xyChart = new XYChartBuilder().width(800).height(400)
                .title("实时温度曲线").xAxisTitle("时间 (秒)").yAxisTitle("温度 (°C)").build();
        xyChart.getStyler().setLegendVisible(true);
        xyChart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNW);
        xyChart.getStyler().setYAxisMin(Y_MIN);
        xyChart.getStyler().setYAxisMax(Y_MAX);
        xyChart.getStyler().setDecimalPattern("#");
        xyChart.getStyler().setChartBackgroundColor(new Color(0xFA, 0xFA, 0xFA));
        xyChart.getStyler().setPlotBackgroundColor(Color.WHITE);
        xyChart.getStyler().setMarkerSize(0);
        xyChart.getStyler().setToolTipsEnabled(false);

        seriesTf1 = xyChart.addSeries("炉温1 (TF1)", new double[]{0}, new double[]{25.0});
        seriesTf2 = xyChart.addSeries("炉温2 (TF2)", new double[]{0}, new double[]{25.0});
        seriesTs  = xyChart.addSeries("表面温 (TS)",  new double[]{0}, new double[]{25.0});
        seriesTc  = xyChart.addSeries("中心温 (TC)",  new double[]{0}, new double[]{25.0});

        seriesTf1.setLineColor(new Color(0xE5, 0x39, 0x35)); seriesTf1.setLineWidth(1.5f);
        seriesTf1.setMarker(SeriesMarkers.NONE);
        seriesTf2.setLineColor(new Color(0xFB, 0x8C, 0x00)); seriesTf2.setLineWidth(1.5f);
        seriesTf2.setMarker(SeriesMarkers.NONE);
        seriesTs.setLineColor(new Color(0x1E, 0x88, 0xE5)); seriesTs.setLineWidth(1.5f);
        seriesTs.setMarker(SeriesMarkers.NONE);
        seriesTc.setLineColor(new Color(0x43, 0xA0, 0x47)); seriesTc.setLineWidth(1.5f);
        seriesTc.setMarker(SeriesMarkers.NONE);

        chartSwingNode = new SwingNode();
        SwingUtilities.invokeLater(() -> {
            chartSwingNode.setContent(new XChartPanel<>(xyChart));
            chartReady = true;
        });
        chartContainer.getChildren().add(chartSwingNode);
    }

    private void updateChart(SensorData data) {
        if (!chartReady) return;
        elapsedSeconds += TICK_SECONDS;
        xBuffer.addLast(elapsedSeconds);
        yTf1Buffer.addLast(data.getTf1()); yTf2Buffer.addLast(data.getTf2());
        yTsBuffer.addLast(data.getTs()); yTcBuffer.addLast(data.getTc());
        while (xBuffer.size() > MAX_CHART_POINTS) {
            xBuffer.removeFirst(); yTf1Buffer.removeFirst(); yTf2Buffer.removeFirst();
            yTsBuffer.removeFirst(); yTcBuffer.removeFirst();
        }
        double[] xArr = toArray(xBuffer);
        xyChart.updateXYSeries("炉温1 (TF1)", xArr, toArray(yTf1Buffer), null);
        xyChart.updateXYSeries("炉温2 (TF2)", xArr, toArray(yTf2Buffer), null);
        xyChart.updateXYSeries("表面温 (TS)",  xArr, toArray(yTsBuffer),  null);
        xyChart.updateXYSeries("中心温 (TC)",  xArr, toArray(yTcBuffer),  null);
        if (!xBuffer.isEmpty()) {
            double xMin = xBuffer.getFirst(), xMax = xBuffer.getLast();
            if (xMax - xMin < 60.0) xMax = xMin + 60.0;
            xyChart.getStyler().setXAxisMin(xMin);
            xyChart.getStyler().setXAxisMax(xMax);
        }
        Platform.runLater(() -> {
            legendTf1.setText("炉温1 " + SensorData.format(data.getTf1()) + "°C");
            legendTf2.setText("炉温2 " + SensorData.format(data.getTf2()) + "°C");
            legendTs.setText("表面温 " + SensorData.format(data.getTs()) + "°C");
            legendTc.setText("中心温 " + SensorData.format(data.getTc()) + "°C");
        });
        if (chartSwingNode.getContent() != null) chartSwingNode.getContent().repaint();
    }

    private void clearChart() {
        elapsedSeconds = 0.0;
        xBuffer.clear(); yTf1Buffer.clear(); yTf2Buffer.clear();
        yTsBuffer.clear(); yTcBuffer.clear();
        SwingUtilities.invokeLater(() -> {
            if (!chartReady) return;
            xyChart.updateXYSeries("炉温1 (TF1)", new double[0], new double[0], null);
            xyChart.updateXYSeries("炉温2 (TF2)", new double[0], new double[0], null);
            xyChart.updateXYSeries("表面温 (TS)",  new double[0], new double[0], null);
            xyChart.updateXYSeries("中心温 (TC)",  new double[0], new double[0], null);
            xyChart.getStyler().setXAxisMin(0.0); xyChart.getStyler().setXAxisMax(60.0);
            if (chartSwingNode.getContent() != null) chartSwingNode.getContent().repaint();
        });
        Platform.runLater(() -> {
            legendTf1.setText("炉温1 —"); legendTf2.setText("炉温2 —");
            legendTs.setText("表面温 —"); legendTc.setText("中心温 —");
        });
    }

    private static double[] toArray(LinkedList<Double> list) {
        double[] a = new double[list.size()]; int i = 0;
        for (double v : list) a[i++] = v;
        return a;
    }

    // ==================== 按钮事件 ====================

    @FXML
    private void handleNewTest() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/NewTestDialog.fxml"));
            Parent root = loader.load();
            NewTestDialogController ctrl = loader.getController();
            ctrl.setOperator(operatorName);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("新建试验");
            dialog.setScene(new Scene(root));
            dialog.setResizable(false);
            dialog.showAndWait();

            if (ctrl.isConfirmed()) {
                currentProductId = ctrl.getCreatedProductId();
                currentTestId = ctrl.getCreatedTestId();
                currentDbEntity = testDao.getByKey(currentProductId, currentTestId);
                productIdLabel.setText(currentProductId);
                customDurationSeconds = ctrl.getTargetDurationSeconds();
                appendLog("试验已创建: " + currentProductId + " / " + currentTestId, "info");
            }
        } catch (Exception e) {
            appendLog("打开新建试验窗口失败: " + e.getMessage(), "error");
        }
    }

    @FXML
    private void handleStartHeating() {
        if (testMaster != null && testMaster.startHeating()) {
            // ok
        } else {
            appendLog("无法开始升温，请先创建试验", "warn");
        }
    }

    @FXML
    private void handleStopHeating() {
        if (testMaster != null) { testMaster.stopHeating(); clearChart(); }
    }

    @FXML
    private void handleStartRecording() {
        if (testMaster != null) {
            if (!testMaster.startRecording(customDurationSeconds)) {
                appendLog("无法开始记录，当前状态不允许", "warn");
            }
        }
    }

    @FXML
    private void handleStopRecording() {
        if (testMaster != null && !testMaster.stopRecording()) {
            appendLog("记录时长不足30秒，试验无效", "warn");
        }
    }

    @FXML
    private void handleSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SettingsDialog.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("参数设置");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(tabPane.getScene().getWindow());
            stage.setResizable(false);
            stage.showAndWait();
            appendLog("参数设置已更新", "info");
        } catch (Exception e) {
            e.printStackTrace();
            String msg = e.getMessage();
            if (e.getCause() != null) msg = e.getCause().toString();
            appendLog("参数设置窗口加载失败: " + msg, "error");
        }
    }

    @FXML
    private void handleTestRecord() {
        if (testMaster == null || currentDbEntity == null) {
            appendLog("没有可保存的试验记录", "warn");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TestRecordDialog.fxml"));
            Parent root = loader.load();
            TestRecordDialogController ctrl = loader.getController();
            ctrl.setTestContext(testMaster, currentDbEntity, operatorName);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("试验记录");
            dialog.setScene(new Scene(root));
            dialog.setResizable(false);
            dialog.showAndWait();

            if (ctrl.isSaved()) {
                appendLog("试验记录已保存: " + currentProductId, "info");
                updateButtonStates(testMaster.getCurrentState());
                handleQuery(); // 刷新记录查询列表
            }
        } catch (Exception e) {
            appendLog("打开试验记录窗口失败: " + e.getMessage(), "error");
        }
    }

    @FXML
    private void handleClearLog() { logTextArea.clear(); messageHistory.clear(); }

    // ==================== Tab 2: 记录查询 ====================

    @SuppressWarnings("unchecked")
    private void initQueryTable() {
        // 设置默认日期范围
        queryStartDate.setText(LocalDate.now().minusMonths(6).toString());
        queryEndDate.setText(LocalDate.now().toString());

        // 绑定表格列
        var cols = queryTable.getColumns();
        ((TableColumn<Object[], String>) cols.get(0)).setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty((String) d.getValue()[0]));
        ((TableColumn<Object[], String>) cols.get(1)).setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty((String) d.getValue()[1]));
        ((TableColumn<Object[], String>) cols.get(2)).setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty((String) d.getValue()[2]));
        ((TableColumn<Object[], String>) cols.get(3)).setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty((String) d.getValue()[3]));
        ((TableColumn<Object[], String>) cols.get(4)).setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty((String) d.getValue()[4]));
        ((TableColumn<Object[], String>) cols.get(5)).setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty((String) d.getValue()[5]));
        ((TableColumn<Object[], String>) cols.get(6)).setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty((String) d.getValue()[6]));
        ((TableColumn<Object[], String>) cols.get(7)).setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty((String) d.getValue()[7]));

        // 双击查看详情
        queryTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Object[] row = queryTable.getSelectionModel().getSelectedItem();
                if (row != null) showTestDetail(row);
            }
        });
    }

    @FXML
    private void handleQuery() {
        String from = queryStartDate.getText().trim();
        String to = queryEndDate.getText().trim();
        String pid = queryProductId.getText().trim();
        String op = queryOperator.getText().trim();

        if (from.isEmpty()) from = "2000-01-01";
        if (to.isEmpty()) to = "2099-12-31";
        if (pid.isEmpty()) pid = null;
        if (op.isEmpty()) op = null;

        try {
            List<com.iso11820.entity.TestMaster> results = testDao.queryByCondition(from, to, pid, op, 1, 200);
            ObservableList<Object[]> data = FXCollections.observableArrayList();
            for (var tm : results) {
                data.add(new Object[]{
                        tm.getTestid(),
                        tm.getProductid(),
                        tm.getTestdate(),
                        tm.getOperator(),
                        tm.getTotaltesttime() != null ? tm.getTotaltesttime().toString() : "0",
                        tm.getDeltatf() != null ? String.format("%.1f", tm.getDeltatf()) : "0.0",
                        tm.getLostweightPer() != null ? String.format("%.1f", tm.getLostweightPer()) : "0.0",
                        "10000000".equals(tm.getFlag()) ? "是" : "否"
                });
            }
            queryTable.setItems(data);
            appendLog("查询完成，共 " + results.size() + " 条记录", "info");
        } catch (Exception e) {
            appendLog("查询失败: " + e.getMessage(), "error");
        }
    }

    private void showTestDetail(Object[] row) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("试验详情");
        alert.setHeaderText(null);
        alert.setContentText(String.format(
                "试验ID: %s\n样品编号: %s\n日期: %s\n操作员: %s\n"
                        + "总时长: %s 秒\n综合温升: %s °C\n失重率: %s%%\n已完成: %s",
                row[0], row[1], row[2], row[3],
                row[4], row[5], row[6], row[7]));
        alert.showAndWait();
    }

    @FXML
    private void handleExportExcel() {
        Object[] selected = queryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            appendLog("请先选择一条试验记录", "warn");
            return;
        }
        try {
            String testId = (String) selected[0];
            String productId = (String) selected[1];
            com.iso11820.entity.TestMaster tm = testDao.getByKey(productId, testId);
            if (tm == null) {
                appendLog("未找到试验记录: " + testId, "error");
                return;
            }
            ExportTestInfo info = buildExportInfo(tm);
            java.nio.file.Path file = ExcelReportService.getInstance().exportSingle(info);
            appendLog("Excel 报告已导出: " + file.toAbsolutePath(), "info");
            java.awt.Desktop.getDesktop().open(file.getParent().toFile());
        } catch (Exception e) {
            appendLog("导出 Excel 失败: " + e.getMessage(), "error");
        }
    }

    @FXML
    private void handleExportPdf() {
        Object[] selected = queryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            appendLog("请先选择一条试验记录", "warn");
            return;
        }
        try {
            String testId = (String) selected[0];
            String productId = (String) selected[1];
            com.iso11820.entity.TestMaster tm = testDao.getByKey(productId, testId);
            if (tm == null) {
                appendLog("未找到试验记录: " + testId, "error");
                return;
            }
            ExportTestInfo info = buildExportInfo(tm);
            java.nio.file.Path file = PdfReportService.getInstance().export(info);
            appendLog("PDF 报告已导出: " + file.toAbsolutePath(), "info");
            java.awt.Desktop.getDesktop().open(file.getParent().toFile());
        } catch (Exception e) {
            appendLog("导出 PDF 失败: " + e.getMessage(), "error");
        }
    }

    /**
     * 从 TestMaster 实体构建导出用的 ExportTestInfo。
     */
    private ExportTestInfo buildExportInfo(com.iso11820.entity.TestMaster tm) {
        ExportTestInfo info = new ExportTestInfo();
        info.setProductId(tm.getProductid());
        info.setTestId(tm.getTestid());
        info.setTestDate(tm.getTestdate());
        info.setOperator(tm.getOperator());
        info.setAccording(tm.getAccording() != null ? tm.getAccording() : "ISO 11820:2022");
        info.setApparatusName(tm.getApparatusname());
        info.setApparatusId(tm.getApparatusid());
        info.setAmbientTemp(tm.getAmbtemp() != null ? tm.getAmbtemp() : 0);
        info.setAmbientHumidity(tm.getAmbhumi() != null ? tm.getAmbhumi() : 0);
        info.setPreWeight(tm.getPreweight() != null ? tm.getPreweight() : 0);
        info.setPostWeight(tm.getPostweight() != null ? tm.getPostweight() : 0);
        info.setLostWeight(tm.getLostweight() != null ? tm.getLostweight() : 0);
        info.setLostWeightPer(tm.getLostweightPer() != null ? tm.getLostweightPer() : 0);
        info.setMaxTf1(tm.getMaxtf1() != null ? tm.getMaxtf1() : 0);
        info.setMaxTf2(tm.getMaxtf2() != null ? tm.getMaxtf2() : 0);
        info.setMaxTs(tm.getMaxts() != null ? tm.getMaxts() : 0);
        info.setMaxTc(tm.getMaxtc() != null ? tm.getMaxtc() : 0);
        info.setFinalTf1(tm.getFinaltf1() != null ? tm.getFinaltf1() : 0);
        info.setFinalTf2(tm.getFinaltf2() != null ? tm.getFinaltf2() : 0);
        info.setFinalTs(tm.getFinalts() != null ? tm.getFinalts() : 0);
        info.setFinalTc(tm.getFinaltc() != null ? tm.getFinaltc() : 0);
        info.setDeltaTf1(tm.getDeltatf1() != null ? tm.getDeltatf1() : 0);
        info.setDeltaTf2(tm.getDeltatf2() != null ? tm.getDeltatf2() : 0);
        info.setDeltaTf(tm.getDeltatf() != null ? tm.getDeltatf() : 0);
        info.setDeltaTs(tm.getDeltats() != null ? tm.getDeltats() : 0);
        info.setDeltaTc(tm.getDeltatc() != null ? tm.getDeltatc() : 0);
        info.setTotalTestTime(tm.getTotaltesttime() != null ? tm.getTotaltesttime() : 0);
        info.setConstPower(tm.getConstpower() != null ? tm.getConstpower() : 0);
        info.setFlameTime(tm.getFlametime() != null ? tm.getFlametime() : 0);
        info.setFlameDuration(tm.getFlameduration() != null ? tm.getFlameduration() : 0);
        info.setMemo(tm.getMemo() != null ? tm.getMemo() : "");
        info.setReportNo(tm.getRptno() != null ? tm.getRptno() : "");

        boolean passed = info.getDeltaTf() <= 50.0 && info.getLostWeightPer() <= 50.0 && info.getFlameDuration() < 5;
        info.setPassed(passed);
        info.setConclusion(passed ? "通过 — 符合不燃性材料标准" : "不通过 — 不符合不燃性材料标准");
        return info;
    }

    // ==================== Tab 3: 设备校准 ====================

    @SuppressWarnings("unchecked")
    private void initCalibTable() {
        var cols = calibTable.getColumns();
        ((TableColumn<Object[], String>) cols.get(0)).setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty((String) d.getValue()[0]));
        ((TableColumn<Object[], String>) cols.get(1)).setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty((String) d.getValue()[1]));
        ((TableColumn<Object[], String>) cols.get(2)).setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty((String) d.getValue()[2]));
        ((TableColumn<Object[], String>) cols.get(3)).setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty((String) d.getValue()[3]));
        ((TableColumn<Object[], String>) cols.get(4)).setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty((String) d.getValue()[4]));
        ((TableColumn<Object[], String>) cols.get(5)).setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty((String) d.getValue()[5]));
        refreshCalibList();
    }

    @FXML
    private void handleNewCalibration() {
        // 简化版：创建一条模拟校准记录
        try {
            CalibrationRecords r = new CalibrationRecords();
            r.setId(java.util.UUID.randomUUID().toString());
            r.setCalibrationDate(java.time.LocalDateTime.now().toString());
            r.setCalibrationType("Surface");
            r.setApparatusId(0);
            r.setOperator(operatorName);
            r.setAverageTemperature(750.0);
            r.setMaxDeviation(2.5);
            r.setUniformityResult(1.5);
            r.setPassedCriteria(1);
            r.setRemarks("仿真校准");
            r.setCreatedAt(java.time.LocalDateTime.now().toString());
            r.setTemperatureData("{}"); // 空JSON，满足NOT NULL约束

            // 模拟 9 个测温点
            r.setTempA1(749.0); r.setTempA2(750.5); r.setTempA3(751.0);
            r.setTempB1(748.5); r.setTempB2(750.0); r.setTempB3(750.5);
            r.setTempC1(749.5); r.setTempC2(751.0); r.setTempC3(750.0);
            r.settAvg(750.0);
            r.settAvgAxis1(749.0); r.settAvgAxis2(750.5); r.settAvgAxis3(750.5);
            r.settAvgLevela(750.2); r.settAvgLevelb(749.7); r.settAvgLevelc(750.2);
            r.settDevAxis1(1.0); r.settDevAxis2(0.5); r.settDevAxis3(0.5);
            r.settDevLevela(1.0); r.settDevLevelb(1.0); r.settDevLevelc(0.5);
            r.settAvgDevAxis(0.67); r.settAvgDevLevel(0.83);

            calibDao.insert(r);
            appendLog("校准记录已创建", "info");
            refreshCalibList();
        } catch (Exception e) {
            appendLog("创建校准记录失败: " + e.getMessage(), "error");
        }
    }

    @FXML
    private void handleRefreshCalibration() {
        refreshCalibList();
    }

    private void refreshCalibList() {
        try {
            List<CalibrationRecords> list = calibDao.listByApparatusId(0);
            ObservableList<Object[]> data = FXCollections.observableArrayList();
            for (var r : list) {
                data.add(new Object[]{
                        r.getCalibrationDate(),
                        r.getCalibrationType(),
                        r.getOperator(),
                        r.getAverageTemperature() != null ? String.format("%.1f", r.getAverageTemperature()) : "—",
                        r.getMaxDeviation() != null ? String.format("%.2f", r.getMaxDeviation()) : "—",
                        r.getPassedCriteria() != null && r.getPassedCriteria() == 1 ? "通过" : "未通过"
                });
            }
            calibTable.setItems(data);
        } catch (Exception e) {
            // 表可能不存在，静默处理
        }
    }

    // ==================== UI 更新 ====================
    private void updateTemperatureDisplay(SensorData data) {
        tf1Value.setText(SensorData.format(data.getTf1()) + " °C");
        tf2Value.setText(SensorData.format(data.getTf2()) + " °C");
        tsValue.setText(SensorData.format(data.getTs()) + " °C");
        tcValue.setText(SensorData.format(data.getTc()) + " °C");
        tCalValue.setText(SensorData.format(data.gettCal()) + " °C");
        calibTempLabel.setText(SensorData.format(data.gettCal()) + " °C");
    }

    private void updateStatusDisplay(TestState state) {
        statusLabel.setText(state.getDisplayName());
        statusLabel.getStyleClass().removeAll("status-idle","status-preparing","status-ready","status-recording","status-complete");
        statusLabel.getStyleClass().add(switch (state) {
            case IDLE -> "status-idle"; case PREPARING -> "status-preparing";
            case READY -> "status-ready"; case RECORDING -> "status-recording";
            case COMPLETE -> "status-complete";
        });
    }

    private void updateButtonStates(TestState state) {
        boolean saved = testMaster != null && testMaster.isSaved();
        boolean isComplete = testMaster != null && testMaster.getCurrentState() == TestState.COMPLETE;
        switch (state) {
            case IDLE -> {
                btnNewTest.setDisable(isComplete && !saved); btnStartHeating.setDisable(false);
                btnStopHeating.setDisable(true); btnStartRecording.setDisable(true);
                btnStopRecording.setDisable(true); btnSettings.setDisable(false);
                btnTestRecord.setDisable(true);
            }
            case PREPARING -> {
                btnNewTest.setDisable(true); btnStartHeating.setDisable(true);
                btnStopHeating.setDisable(false); btnStartRecording.setDisable(true);
                btnStopRecording.setDisable(true); btnSettings.setDisable(false);
                btnTestRecord.setDisable(true);
            }
            case READY -> {
                btnNewTest.setDisable(true); btnStartHeating.setDisable(true);
                btnStopHeating.setDisable(false); btnStartRecording.setDisable(false);
                btnStopRecording.setDisable(true); btnSettings.setDisable(false);
                btnTestRecord.setDisable(true);
            }
            case RECORDING -> {
                btnNewTest.setDisable(true); btnStartHeating.setDisable(true);
                btnStopHeating.setDisable(true); btnStartRecording.setDisable(true);
                btnStopRecording.setDisable(false); btnSettings.setDisable(true);
                btnTestRecord.setDisable(true);
            }
            case COMPLETE -> {
                btnNewTest.setDisable(!saved); btnStartHeating.setDisable(true);
                btnStopHeating.setDisable(false); btnStartRecording.setDisable(true);
                btnStopRecording.setDisable(true); btnSettings.setDisable(false);
                btnTestRecord.setDisable(!saved);
            }
        }
    }

    private void updateStatusBar(String pid, int duration, double drift) {
        productIdLabel.setText(pid != null ? pid : (currentProductId != null ? currentProductId : "—"));
        durationLabel.setText(duration + " 秒");
        driftLabel.setText(drift == Double.MAX_VALUE || drift == 0.0 ? "— °C/10min" : String.format("%.2f °C/10min", drift));
    }

    private void appendLog(String msg, String level) {
        messageHistory.add(msg);
        if (messageHistory.size() > 1000) messageHistory.subList(0, 200).clear();
        logTextArea.appendText((switch (level) { case "warn" -> "[!] "; case "error" -> "[✕] "; default -> ""; }) + msg + "\n");
        logTextArea.setScrollTop(Double.MAX_VALUE);
    }

    // ==================== 数据监听器 ====================
    private class MainDataListener implements DataChangeListener {
        @Override
        public void onDataChanged(SensorData data, TestState state, int duration, String msg) {
            Platform.runLater(() -> {
                updateTemperatureDisplay(data); updateStatusDisplay(state);
                updateButtonStates(state);
                updateStatusBar(currentProductId, duration,
                        testMaster != null ? testMaster.getTempDrift() : Double.MAX_VALUE);
                if (msg != null) appendLog(msg, msg.contains("终止")||msg.contains("无效")||msg.contains("不足")?"warn": msg.contains("错误")||msg.contains("失败")||msg.contains("未保存")?"error":"info");
            });
            SwingUtilities.invokeLater(() -> updateChart(data));

            // CSV 时序数据记录：Recording 状态下每秒追加一行
            if (state == TestState.RECORDING && data != null
                    && currentProductId != null && currentTestId != null) {
                DataPoint dp = new DataPoint();
                dp.setTimeSeconds(duration);
                dp.setTf1(data.getTf1());
                dp.setTf2(data.getTf2());
                dp.setTs(data.getTs());
                dp.setTc(data.getTc());
                dp.settCal(data.gettCal());
                csvService.appendRow(currentProductId, currentTestId, dp);
            }
        }
    }
}
