package com.iso11820.ui.dialog;

import com.iso11820.dao.ApparatusDao;
import com.iso11820.dao.ProductMasterDao;
import com.iso11820.dao.TestMasterDao;
import com.iso11820.dao.impl.ApparatusDaoImpl;
import com.iso11820.dao.impl.ProductMasterDaoImpl;
import com.iso11820.dao.impl.TestMasterDaoImpl;
import com.iso11820.entity.Apparatus;
import com.iso11820.entity.ProductMaster;
import com.iso11820.entity.TestMaster;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 新建试验对话框控制器 —— 模态窗口，填写样品信息和试验参数。
 *
 * @author ISO11820 Development Team
 * @version 1.0
 * @since 2026-06-29
 */
public class NewTestDialogController {

    @FXML private TextField productIdField;
    @FXML private TextField productNameField;
    @FXML private TextField specificField;
    @FXML private TextField diameterField;
    @FXML private TextField heightField;
    @FXML private TextField preWeightField;
    @FXML private TextField ambTempField;
    @FXML private TextField ambHumiField;
    @FXML private RadioButton radioStandard;
    @FXML private RadioButton radioCustom;
    @FXML private TextField customDurationField;
    @FXML private Label apparatusInfoLabel;

    private final ApparatusDao apparatusDao = new ApparatusDaoImpl();
    private final ProductMasterDao productDao = new ProductMasterDaoImpl();
    private final TestMasterDao testDao = new TestMasterDaoImpl();

    /** 回调结果 */
    private boolean confirmed = false;
    private String createdProductId;
    private String createdTestId;
    private int targetDurationSeconds;
    private String operatorName;

    @FXML
    public void initialize() {
        radioCustom.selectedProperty().addListener((obs, old, sel) ->
                customDurationField.setDisable(!sel));
        loadApparatusInfo();
    }

    public void setOperator(String operatorName) {
        this.operatorName = operatorName;
    }

    public boolean isConfirmed() { return confirmed; }
    public String getCreatedProductId() { return createdProductId; }
    public String getCreatedTestId() { return createdTestId; }
    public int getTargetDurationSeconds() { return targetDurationSeconds; }

    private void loadApparatusInfo() {
        try {
            Apparatus app = apparatusDao.getById(0);
            if (app != null) {
                apparatusInfoLabel.setText(String.format(
                        "设备编号: %s  名称: %s  检定至: %s",
                        app.getInnernumber(), app.getApparatusname(), app.getCheckdatet()));
            } else {
                apparatusInfoLabel.setText("设备信息未找到");
            }
        } catch (Exception e) {
            apparatusInfoLabel.setText("读取设备信息失败");
        }
    }

    @FXML
    private void handleCreate() {
        // 1. 输入校验
        String productId = productIdField.getText().trim();
        String productName = productNameField.getText().trim();
        String specific = specificField.getText().trim();

        if (productId.isEmpty() || productName.isEmpty()) {
            showAlert("请填写样品编号和样品名称");
            return;
        }

        double diameter, height, preWeight, ambTemp, ambHumi;
        try {
            diameter = Double.parseDouble(diameterField.getText().trim());
            height = Double.parseDouble(heightField.getText().trim());
            preWeight = Double.parseDouble(preWeightField.getText().trim());
            ambTemp = Double.parseDouble(ambTempField.getText().trim());
            ambHumi = Double.parseDouble(ambHumiField.getText().trim());
        } catch (NumberFormatException e) {
            showAlert("数值输入格式错误，请检查");
            return;
        }

        if (preWeight <= 0) {
            showAlert("试验前质量必须大于 0");
            return;
        }

        // 2. 时长计算
        if (radioCustom.isSelected()) {
            try {
                targetDurationSeconds = (int) (Double.parseDouble(customDurationField.getText().trim()) * 60);
            } catch (NumberFormatException e) {
                showAlert("自定义时长格式错误");
                return;
            }
        } else {
            targetDurationSeconds = 3600;
        }

        // 3. 生成 testId
        String testId = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // 4. 保存样品（如不存在）
        try {
            if (productDao.getById(productId) == null) {
                ProductMaster pm = new ProductMaster(productId, productName, specific,
                        diameter, height, null);
                productDao.insert(pm);
            }
        } catch (Exception e) {
            showAlert("保存样品信息失败: " + e.getMessage());
            return;
        }

        // 5. 创建试验记录
        try {
            Apparatus app = apparatusDao.getById(0);
            TestMaster tm = new TestMaster();
            tm.setProductid(productId);
            tm.setTestid(testId);
            tm.setTestdate(today);
            tm.setAmbtemp(ambTemp);
            tm.setAmbhumi(ambHumi);
            tm.setAccording("ISO 11820:2022");
            tm.setOperator(operatorName);
            tm.setApparatusid(app != null ? app.getInnernumber() : "FURNACE-01");
            tm.setApparatusname(app != null ? app.getApparatusname() : "一号试验炉");
            tm.setApparatuschkdate(app != null ? app.getCheckdatet() : today);
            tm.setRptno(productId);
            tm.setPreweight(preWeight);
            tm.setPostweight(0.0);
            tm.setLostweight(0.0);
            tm.setLostweightPer(0.0);
            tm.setTotaltesttime(0);
            tm.setConstpower(app != null && app.getConstpower() != null ? app.getConstpower() : 2048);
            tm.setPhenocode("");
            tm.setFlametime(0);
            tm.setFlameduration(0);
            // 温度统计全填 0
            tm.setMaxtf1(0.0); tm.setMaxtf2(0.0); tm.setMaxts(0.0); tm.setMaxtc(0.0);
            tm.setMaxtf1Time(0); tm.setMaxtf2Time(0); tm.setMaxtsTime(0); tm.setMaxtcTime(0);
            tm.setFinaltf1(0.0); tm.setFinaltf2(0.0); tm.setFinalts(0.0); tm.setFinaltc(0.0);
            tm.setFinaltf1Time(0); tm.setFinaltf2Time(0); tm.setFinaltsTime(0); tm.setFinaltcTime(0);
            tm.setDeltatf1(0.0); tm.setDeltatf2(0.0); tm.setDeltatf(0.0); tm.setDeltats(0.0); tm.setDeltatc(0.0);

            testDao.insert(tm);
        } catch (Exception e) {
            showAlert("创建试验记录失败: " + e.getMessage());
            return;
        }

        // 6. 成功
        this.createdProductId = productId;
        this.createdTestId = testId;
        this.confirmed = true;
        closeDialog();
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) productIdField.getScene().getWindow();
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
