package com.iso11820.ui.dialog;

import com.iso11820.utils.AppConfig;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 参数设置对话框控制器 —— 配置仿真参数并持久化到 appsettings.json。
 *
 * @author ISO11820 Development Team
 * @version 1.0
 * @since 2026-06-30
 */
public class SettingsDialogController {

    @FXML private TextField targetTempField;
    @FXML private TextField heatingRateField;
    @FXML private TextField fluctuationField;
    @FXML private TextField durationField;

    private final AppConfig config = AppConfig.getInstance();

    @FXML
    public void initialize() {
        try {
            targetTempField.setText(String.valueOf(config.getDouble("Simulation.TargetFurnaceTemp", 750.0)));
            heatingRateField.setText(String.valueOf(config.getDouble("Simulation.HeatingRatePerSecond", 40.0)));
            fluctuationField.setText(String.valueOf(config.getDouble("Simulation.TempFluctuation", 0.5)));
            int stdDuration = config.getInt("Simulation.StandardDurationSeconds", 3600);
            durationField.setText(String.valueOf(stdDuration));
        } catch (Exception e) {
            e.printStackTrace();
            // 使用默认值兜底
            if (targetTempField.getText().isEmpty()) targetTempField.setText("750.0");
            if (heatingRateField.getText().isEmpty()) heatingRateField.setText("40.0");
            if (fluctuationField.getText().isEmpty()) fluctuationField.setText("0.5");
            if (durationField.getText().isEmpty()) durationField.setText("3600");
        }
    }

    @FXML
    private void handleConfirm() {
        try {
            double target = parseDouble(targetTempField, "目标炉温", 100, 1200);
            double rate = parseDouble(heatingRateField, "升温速率", 0.1, 500);
            double fluct = parseDouble(fluctuationField, "温度波动", 0, 50);
            int duration = parseInt(durationField, "标准时长", 1, 86400);

            saveToConfigFile(target, rate, fluct, duration);

            showInfo("参数已保存，下次新建试验时生效");
            closeDialog();
        } catch (IllegalArgumentException e) {
            showAlert(e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private double parseDouble(TextField field, String name, double min, double max) {
        String text = field.getText().trim();
        if (text.isEmpty()) throw new IllegalArgumentException(name + "不能为空");
        try {
            double v = Double.parseDouble(text);
            if (v < min || v > max)
                throw new IllegalArgumentException(name + "范围: " + min + " ~ " + max);
            return v;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + "格式错误");
        }
    }

    private int parseInt(TextField field, String name, int min, int max) {
        String text = field.getText().trim();
        if (text.isEmpty()) throw new IllegalArgumentException(name + "不能为空");
        try {
            int v = Integer.parseInt(text);
            if (v < min || v > max)
                throw new IllegalArgumentException(name + "范围: " + min + " ~ " + max);
            return v;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + "格式错误");
        }
    }

    /**
     * 将参数持久化写入 appsettings.json 文件。
     */
    private void saveToConfigFile(double target, double rate, double fluct, int duration) {
        Path configPath = Paths.get("appsettings.json");
        // 回退到 classpath 资源路径
        if (!Files.exists(configPath)) {
            configPath = Paths.get("src/main/resources/appsettings.json");
        }
        if (!Files.exists(configPath)) {
            System.err.println("[Settings] 配置文件未找到，参数仅本次会话有效");
            return;
        }
        try {
            String content = Files.readString(configPath);
            content = content.replaceAll("\"TargetFurnaceTemp\":\\s*[\\d.]+",
                    "\"TargetFurnaceTemp\": " + target);
            content = content.replaceAll("\"HeatingRatePerSecond\":\\s*[\\d.]+",
                    "\"HeatingRatePerSecond\": " + rate);
            content = content.replaceAll("\"TempFluctuation\":\\s*[\\d.]+",
                    "\"TempFluctuation\": " + fluct);
            content = content.replaceAll("\"StandardDurationSeconds\":\\s*\\d+",
                    "\"StandardDurationSeconds\": " + duration);
            Files.writeString(configPath, content);
            System.out.println("[Settings] 配置已保存到: " + configPath);
        } catch (IOException e) {
            System.err.println("[Settings] 配置写入失败: " + e.getMessage());
        }
        config.reload();
    }

    private void closeDialog() {
        Stage stage = (Stage) targetTempField.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("提示");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void showInfo(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("提示");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}