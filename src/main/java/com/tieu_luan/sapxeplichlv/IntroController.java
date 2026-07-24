package com.tieu_luan.sapxeplichlv;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

import java.io.IOException;


// Controller cho màn hình giới thiệu
 
public class IntroController {
    
    @FXML 
    private Button getStartedBtn;
    
    @FXML 
    private Button accessAccountBtn;
    
    @FXML
    public void initialize() {
        // Khởi tạo màn hình
    }
    
    
      // Xử lý sự kiện khi nhấn nút "Get started"
     
    @FXML
    void onGetStartedBtnClick(ActionEvent event) {
        try {
            // Hiển thị màn hình đăng ký hoặc thông tin cho người dùng mới
            App.changeScene("views/login", "Đăng nhập"); // Tạm thời chuyển đến login
        } catch (IOException e) {
            e.printStackTrace();
            showError("Không thể mở trang đăng nhập: " + e.getMessage());
        }
    }
    
    
     // Xử lý sự kiện khi nhấn nút "Access account"
     
    @FXML
    void onAccessAccountBtnClick(ActionEvent event) {
        try {
            // Chuyển đến màn hình đăng nhập
            App.changeScene("views/login", "Đăng nhập");
        } catch (IOException e) {
            e.printStackTrace();
            showError("Không thể mở trang đăng nhập: " + e.getMessage());
        }
    }
    
    
     // Hiển thị thông báo lỗi
     
    private void showError(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}