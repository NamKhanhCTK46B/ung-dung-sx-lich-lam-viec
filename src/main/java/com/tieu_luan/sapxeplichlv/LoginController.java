package com.tieu_luan.sapxeplichlv;

import dao.NhanVienDAO;
import models.NhanVien;
import utils.Session;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.io.IOException;

public class LoginController {
    
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginBtn;
    @FXML private Button backBtn;
    @FXML private javafx.scene.control.Label errorMsgLabel;
    
    private NhanVienDAO nhanVienDAO;
    
    @FXML
    public void initialize() {
        nhanVienDAO = new NhanVienDAO();
        
        // Hide error message by default
        errorMsgLabel.setVisible(false);
        
        // Enable login when pressing Enter in the password field
        passwordField.setOnKeyPressed(this::handleEnterKeyPressed);
    }
    
    @FXML
    void onLoginBtnClick(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        
        if (username.isEmpty() || password.isEmpty()) {
            errorMsgLabel.setText("Vui lòng nhập tên đăng nhập và mật khẩu");
            errorMsgLabel.setVisible(true);
            return;
        }
        
        NhanVien user = nhanVienDAO.login(username, password);
        if (user != null) {
            // Set user in session
            Session.setCurrentUser(user);
             System.out.println("Đăng nhập thành công: " + user.getTenDN());
            
            try {
                // Redirect to appropriate home page based on role
                if (Session.isManager()) {
                    App.changeScene("views/manager_home", "Quản lý - Trang Chủ");
                } else {
                    App.changeScene("views/employee_home", "Nhân viên - Trang Chủ");
                }
            } catch (IOException e) {
                e.printStackTrace();
                errorMsgLabel.setText("Không thể mở trang chủ: " + e.getMessage());
                errorMsgLabel.setVisible(true);
            }
        } else {
            errorMsgLabel.setText("Tên đăng nhập hoặc mật khẩu không đúng");
            errorMsgLabel.setVisible(true);
            passwordField.clear();
            passwordField.requestFocus();
        }
    }
    
    @FXML
    void onBackBtnClick(ActionEvent event) {
        try {
            App.changeScene("views/intro_page", "Giới thiệu");
        } catch (IOException e) {
            e.printStackTrace();
            errorMsgLabel.setText("Không thể quay lại trang giới thiệu: " + e.getMessage());
            errorMsgLabel.setVisible(true);
        }
    }
    
    private void handleEnterKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            loginBtn.fire();
        }
    }
    
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
