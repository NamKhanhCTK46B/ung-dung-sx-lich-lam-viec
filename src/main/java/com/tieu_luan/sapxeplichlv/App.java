package com.tieu_luan.sapxeplichlv;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import static javafx.application.Application.launch;

public class App extends Application {
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        try {
            changeScene("views/intro_page", "Quản lý Lịch làm việc Nhà hàng");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void changeScene(String fxml, String title) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        
        primaryStage.setTitle(title);
        primaryStage.setScene(scene);
        
        // Kiểm tra nếu không phải intro_page hoặc login thì mở full màn hình
        if (!fxml.contains("intro_page") && !fxml.contains("login")) {
            primaryStage.setFullScreen(true);
        } else {
            primaryStage.setFullScreen(false);
            primaryStage.setResizable(false); // Không cho resize intro và login
        }
        
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch();
    }

}