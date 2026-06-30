package com.iso11820.ui.dialog;

import com.iso11820.core.TestMaster;
import com.iso11820.dao.TestMasterDao;
import com.iso11820.dao.impl.TestMasterDaoImpl;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * 试验记录对话框控制器 —— 填写火焰现象、试验后质量，自动计算并保存。
 *
 * @author ISO11820 Development Team
 * @version 1.0
 * @since 2026-06-29
 */
public class TestRecordDialogController {

    @FXML private Label summaryLabel;
    @FXML private CheckBox flameCheckBox;
    @FXML private TextField flameTimeField;
    @FXML private TextField flameDurationField;
    @FXML private TextField postWeightField;
    @FXML private TextArea memoArea;
    @FXML private Label calcLabel;

    private final TestMasterDao testDao = new TestMasterDaoImpl();

    /** 核心层 TestMaster 引用 */
    private TestMaster coreTestMaster;
    /** 数据库实体（新建试验时已写入） */
    private com.iso11820.entity.TestMaster dbEntity;
    /** 操作员 */
    private String operatorName;
    /** 是否已保存 */
    private boolean saved = false;

    @FXML
    public void initialize() {
        flameCheckBox.selectedProperty().addListener((obs, old, sel) -> {
            flameTimeField.setDisable(!sel);
            flameDurationField.setDisable(!sel);
        });
    }

    /**
     * 设置试验上下文 —— 由 MainController 在打开对话框前调用。
     */
    public void setTestContext(TestMaster coreTestMaster, com.iso11820.entity.TestMaster dbEntity,
                                String operatorName) {
        this.coreTestMaster = coreTestMaster;
        this.dbEntity = dbEntity;
        this.operatorName = operatorName;

        // 加载摘要
        loadTestSummary();

        // 实时更新计算结果
        double preWeight = dbEntity != null && dbEntity.getPreweight() != null
                ? dbEntity.getPreweight() : 0.0;
        postWeightField.textProperty().addListener(
                (obs, old, text) -> updateCalculation(preWeight));
    }

    public boolean isSaved() { return saved; }

    /**
     * 从核心层 TestResult 加载试验数据摘要。
     */
    private void loadTestSummary() {
        if (coreTestMaster == null) {
            summaryLabel.setText("数据不可用");
            return;
        }

        TestMaster.TestResult r = coreTestMaster.getTestResult();
        if (r == null) {
            summaryLabel.setText("试验尚未完成，无法获取统计结果");
            return;
        }

        double preWeight = dbEntity != null && dbEntity.getPreweight() != null
                ? dbEntity.getPreweight() : 0.0;

        summaryLabel.setText(String.format(
                "样品编号: %s | 试验ID: %s\n总记录时长: %d 秒 | 恒功率: %.1f kW\n"
                        + "炉温1 最高: %.1f°C@%ds | 炉温2 最高: %.1f°C@%ds\n"
                        + "表面温 最高: %.1f°C@%ds | 中心温 最高: %.1f°C@%ds\n"
                        + "试验前质量: %.1f g | 温漂: %.2f °C/10min",
                dbEntity != null ? dbEntity.getProductid() : "—",
                dbEntity != null ? dbEntity.getTestid() : "—",
                r.totalRecordTime(), r.constantPower(),
                r.maxTf1(), r.maxTf1Time(), r.maxTf2(), r.maxTf2Time(),
                r.maxTs(), r.maxTsTime(), r.maxTc(), r.maxTcTime(),
                preWeight, r.tempDrift()
        ));

        // 更新计算展示
        updateCalculation(preWeight);
    }

    private void updateCalculation(double preWeight) {
        String postText = postWeightField.getText().trim();
        if (postText.isEmpty()) {
            calcLabel.setText("请填写试验后质量");
            return;
        }

        try {
            double postW = Double.parseDouble(postText);
            double lostW = preWeight - postW;
            double lostPct = preWeight > 0 ? (lostW / preWeight) * 100.0 : 0;

            TestMaster.TestResult r = coreTestMaster.getTestResult();
            String deltaInfo = "";
            if (r != null) {
                deltaInfo = String.format(
                        "\n炉温1 温升: %.1f°C | 炉温2 温升: %.1f°C\n"
                                + "表面温 温升: %.1f°C | 中心温 温升: %.1f°C\n"
                                + "综合温升(deltaTf): %.1f°C",
                        r.deltaTf1(), r.deltaTf2(), r.deltaTs(), r.deltaTc(), r.deltaTf());
            }

            calcLabel.setText(String.format(
                    "失重量: %.1f g | 失重率: %.1f%%\n"
                            + "%s\n"
                            + "判定: 综合温升 %s 50°C | 失重率 %s 50%%",
                    lostW, lostPct, deltaInfo,
                    (r != null && r.deltaTf() <= 50) ? "≤" : ">",
                    lostPct <= 50 ? "≤" : ">"
            ));
        } catch (NumberFormatException e) {
            calcLabel.setText("试验后质量格式错误");
        }
    }

    @FXML
    private void handleSave() {
        // 校验试验后质量
        String postText = postWeightField.getText().trim();
        if (postText.isEmpty()) {
            showAlert("试验后质量为必填项");
            return;
        }
        double postWeight;
        try {
            postWeight = Double.parseDouble(postText);
        } catch (NumberFormatException e) {
            showAlert("试验后质量格式错误");
            return;
        }
        if (postWeight < 0) {
            showAlert("试验后质量不能为负数");
            return;
        }

        double preWeight = dbEntity != null && dbEntity.getPreweight() != null
                ? dbEntity.getPreweight() : 0.0;
        double lostWeight = preWeight - postWeight;
        double lostPer = preWeight > 0 ? (lostWeight / preWeight) * 100.0 : 0;

        // 火焰数据
        int flameTime = 0, flameDuration = 0;
        String phenocode = "";
        if (flameCheckBox.isSelected()) {
            phenocode = "flame";
            try {
                flameTime = Integer.parseInt(flameTimeField.getText().trim());
                flameDuration = Integer.parseInt(flameDurationField.getText().trim());
            } catch (NumberFormatException e) {
                // 默认为 0
            }
        }

        // 从核心层获取统计
        TestMaster.TestResult r = coreTestMaster.getTestResult();
        if (r == null) {
            showAlert("无法获取试验统计数据");
            return;
        }

        try {
            // 更新数据库
            dbEntity.setPostweight(postWeight);
            dbEntity.setLostweight(lostWeight);
            dbEntity.setLostweightPer(lostPer);
            dbEntity.setTotaltesttime(r.totalRecordTime());
            dbEntity.setConstpower((int) r.constantPower());
            dbEntity.setPhenocode(phenocode);
            dbEntity.setFlametime(flameTime);
            dbEntity.setFlameduration(flameDuration);

            dbEntity.setMaxtf1(r.maxTf1()); dbEntity.setMaxtf1Time(r.maxTf1Time());
            dbEntity.setMaxtf2(r.maxTf2()); dbEntity.setMaxtf2Time(r.maxTf2Time());
            dbEntity.setMaxts(r.maxTs()); dbEntity.setMaxtsTime(r.maxTsTime());
            dbEntity.setMaxtc(r.maxTc()); dbEntity.setMaxtcTime(r.maxTcTime());

            dbEntity.setFinaltf1(r.finalTf1()); dbEntity.setFinaltf2(r.finalTf2());
            dbEntity.setFinalts(r.finalTs()); dbEntity.setFinaltc(r.finalTc());
            dbEntity.setFinaltf1Time(r.totalRecordTime());
            dbEntity.setFinaltf2Time(r.totalRecordTime());
            dbEntity.setFinaltsTime(r.totalRecordTime());
            dbEntity.setFinaltcTime(r.totalRecordTime());

            dbEntity.setDeltatf1(r.deltaTf1()); dbEntity.setDeltatf2(r.deltaTf2());
            dbEntity.setDeltatf(r.deltaTf()); dbEntity.setDeltats(r.deltaTs());
            dbEntity.setDeltatc(r.deltaTc());

            dbEntity.setMemo(memoArea.getText().trim());
            dbEntity.setFlag("10000000");

            testDao.updateResultWithTransaction(dbEntity);

            // 标记核心层已保存
            coreTestMaster.markSaved();

            saved = true;
            closeDialog();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("保存试验记录失败: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) postWeightField.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("提示");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
