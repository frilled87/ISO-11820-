package com.iso11820;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Objects;

/**
 * ISO 11820 建材不燃性试验仿真系统 — 程序入口。
 *
 * <p>启动时加载登录界面，验证通过后进入主窗口。
 * JavaFX 21 + Maven，桌面应用骨架。
 * </p>
 *
 * @author ISO11820 Development Team
 * @version 1.0
 * @since 2026-06-29
 */
public class ISO11820Application extends Application {

    /** 主舞台引用 */
    private Stage primaryStage;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        showLoginView();
    }

    /**
     * 加载并显示登录界面。
     */
    private void showLoginView() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/LoginView.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 480, 420);
            primaryStage.setTitle("ISO 11820 — 登录");
            primaryStage.setScene(scene);
            primaryStage.setResizable(false);
            primaryStage.centerOnScreen();
            primaryStage.show();

        } catch (Exception e) {
            System.err.println("无法加载登录界面: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        // 程序退出时的清理工作（后续轮次补充）
        System.out.println("ISO 11820 系统关闭");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
