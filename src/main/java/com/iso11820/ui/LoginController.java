package com.iso11820.ui;

import com.iso11820.dao.OperatorDao;
import com.iso11820.dao.impl.OperatorDaoImpl;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * 登录界面控制器 —— 处理角色选择、密码验证、窗口跳转。
 *
 * <p>角色与用户名映射：
 * <ul>
 *   <li>管理员 → {@code admin}</li>
 *   <li>试验员 → {@code experimenter}</li>
 * </ul>
 * </p>
 *
 * @author ISO11820 Development Team
 * @version 1.0
 * @since 2026-06-29
 */
public class LoginController {

    // ==================== FXML 绑定 ====================

    @FXML
    private RadioButton radioAdmin;

    @FXML
    private RadioButton radioExperimenter;

    @FXML
    private ToggleGroup roleGroup;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Button cancelButton;

    /** 操作员数据访问对象 */
    private final OperatorDao operatorDao;

    // ==================== 构造方法 ====================

    /** 无参构造 —— JavaFX FXML 加载要求。 */
    public LoginController() {
        this.operatorDao = new OperatorDaoImpl();
    }

    // ==================== 事件处理 ====================

    /**
     * 处理登录按钮点击。
     * <p>
     * 根据角色选择确定用户名，调用 {@link OperatorDao#login} 验证密码，
     * 成功后关闭登录窗口并打开主窗口，失败则弹出错误提示。
     * </p>
     */
    @FXML
    private void handleLogin() {
        String username = resolveUsername();
        String password = passwordField.getText();

        if (password == null || password.isEmpty()) {
            showAlert("提示", "请输入密码");
            return;
        }

        if (operatorDao.login(username, password)) {
            openMainWindow(username);
        } else {
            showAlert("登录失败", "密码错误，请重新输入");
            passwordField.clear();
            passwordField.requestFocus();
        }
    }

    /**
     * 处理取消按钮点击 —— 退出程序。
     */
    @FXML
    private void handleCancel() {
        Platform.exit();
    }

    // ==================== 内部方法 ====================

    /**
     * 根据选中的角色单选按钮解析对应的用户名。
     *
     * @return 管理员返回 {@code "admin"}，试验员返回 {@code "experimenter"}
     */
    private String resolveUsername() {
        if (radioExperimenter.isSelected()) {
            return "experimenter";
        }
        // 默认管理员
        return "admin";
    }

    /**
     * 登录成功后关闭当前登录窗口，打开主窗口。
     */
    private void openMainWindow(String username) {
        try {
            Stage loginStage = (Stage) loginButton.getScene().getWindow();
            loginStage.close();

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/MainView.fxml"));
            Parent root = loader.load();

            // 向 MainController 注入操作员信息
            MainController mainController = loader.getController();
            mainController.initController(username);

            Stage mainStage = new Stage();
            mainStage.setTitle("ISO 11820 建材不燃性试验仿真系统");
            mainStage.setScene(new Scene(root, 1200, 760));
            mainStage.setMinWidth(960);
            mainStage.setMinHeight(640);
            mainStage.show();

        } catch (IOException e) {
            showAlert("系统错误", "无法加载主界面: " + e.getMessage());
        }
    }

    /**
     * 弹出提示对话框。
     *
     * @param title   标题
     * @param message 消息内容
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
