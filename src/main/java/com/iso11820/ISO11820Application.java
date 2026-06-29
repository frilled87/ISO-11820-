package com.iso11820;

import javafx.application.Application;
import javafx.stage.Stage;

public class ISO11820Application extends Application {
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("ISO 11820 建材不燃性试验仿真系统");
        primaryStage.setWidth(800);
        primaryStage.setHeight(600);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}