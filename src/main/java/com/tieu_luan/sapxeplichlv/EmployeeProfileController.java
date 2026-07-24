package com.tieu_luan.sapxeplichlv;

import dao.NhanVienDAO;
import dao.YeuCauDLDAO;
import models.NhanVien;
import models.YeuCauDL;
import utils.Session;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public class EmployeeProfileController {

    @FXML private TextField txtMaNV;
    @FXML private TextField txtHoTen;
    @FXML private TextField txtCCCD;
    @FXML private TextField txtSDT;
    @FXML private TextField txtEmail;
    @FXML private TextField txtViTri;
    @FXML private TextField txtTenDN;
    @FXML private Label lblRemainingLeave;
    
    @FXML private PasswordField txtOldPassword;
    @FXML private PasswordField txtNewPassword;
    @FXML private PasswordField txtConfirmPassword;
    
    @FXML private TableView<YeuCauDL> tblRequests;
    @FXML private TableColumn<YeuCauDL, Integer> colMaYC;
    @FXML private TableColumn<YeuCauDL, String> colLoaiYC;
    @FXML private TableColumn<YeuCauDL, LocalDate> colNgayBD;
    @FXML private TableColumn<YeuCauDL, LocalDate> colNgayKT;
    @FXML private TableColumn<YeuCauDL, String> colTrangThai;
    
    @FXML private Button btnUpdate;
    @FXML private Button btnChangePassword;
    @FXML private Button btnBack;
    
    private NhanVienDAO nhanVienDAO;
    private YeuCauDLDAO yeuCauDLDAO;
    private NhanVien currentEmployee;

    @FXML
    public void initialize() {
        // Initialize DAOs
        nhanVienDAO = new NhanVienDAO();
        yeuCauDLDAO = new YeuCauDLDAO();
        
        // Set up table columns
        colMaYC.setCellValueFactory(new PropertyValueFactory<>("maYC"));
        colLoaiYC.setCellValueFactory(new PropertyValueFactory<>("loaiYCText"));
        colNgayBD.setCellValueFactory(new PropertyValueFactory<>("ngayBatDau"));
        colNgayKT.setCellValueFactory(new PropertyValueFactory<>("ngayKetThuc"));
        colTrangThai.setCellValueFactory(new PropertyValueFactory<>("trangThaiText"));
        
        // Format date columns
        colNgayBD.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                } else {
                    setText(date.toString());
                }
            }
        });
        
        colNgayKT.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                } else {
                    setText(date.toString());
                }
            }
        });
        
        // Get current employee info
        if (Session.getCurrentUser() != null) {
            currentEmployee = Session.getCurrentUser();
            loadEmployeeInfo();
            loadEmployeeRequests();
        }
    }
    
    private void loadEmployeeInfo() {
        // Make sure we have the latest data
        currentEmployee = nhanVienDAO.getNhanVienById(currentEmployee.getMaNV());
        
        // Populate fields
        txtMaNV.setText(String.valueOf(currentEmployee.getMaNV()));
        txtHoTen.setText(currentEmployee.getHoTen());
        txtCCCD.setText(currentEmployee.getCccd());
        txtSDT.setText(currentEmployee.getSdt());
        txtEmail.setText(currentEmployee.getEmail() != null ? currentEmployee.getEmail() : "");
        txtViTri.setText(currentEmployee.getTenVT());
        txtTenDN.setText(currentEmployee.getTenDN());
        
        // Remaining leave days
        lblRemainingLeave.setText(String.valueOf(4 - currentEmployee.getSoNgayNghiThang()));
    }
    
    private void loadEmployeeRequests() {
        List<YeuCauDL> requests = yeuCauDLDAO.getYeuCauDLByEmployee(currentEmployee.getMaNV());
        ObservableList<YeuCauDL> requestList = FXCollections.observableArrayList(requests);
        tblRequests.setItems(requestList);
    }
    
    @FXML
    void handleUpdateProfile(ActionEvent event) {
        if (!validateFields()) {
            return;
        }
        
        // Update employee information
        currentEmployee.setHoTen(txtHoTen.getText().trim());
        currentEmployee.setCccd(txtCCCD.getText().trim());
        currentEmployee.setSdt(txtSDT.getText().trim());
        currentEmployee.setEmail(txtEmail.getText().trim().isEmpty() ? null : txtEmail.getText().trim());
        
        if (nhanVienDAO.updateNhanVien(currentEmployee)) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã cập nhật thông tin cá nhân");
            // Update session user
            Session.setCurrentUser(currentEmployee);
            loadEmployeeInfo();
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể cập nhật thông tin cá nhân");
        }
    }
    
    private boolean validateFields() {
        if (txtHoTen.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thông tin thiếu", "Vui lòng nhập họ tên");
            return false;
        }
        
        if (txtCCCD.getText().trim().isEmpty() || txtCCCD.getText().trim().length() != 12) {
            showAlert(Alert.AlertType.WARNING, "Thông tin không hợp lệ", "CCCD phải có 12 số");
            return false;
        }
        
        if (txtSDT.getText().trim().isEmpty() || txtSDT.getText().trim().length() != 10) {
            showAlert(Alert.AlertType.WARNING, "Thông tin không hợp lệ", "Số điện thoại phải có 10 số");
            return false;
        }
        
        return true;
    }
    
    @FXML
    void handleChangePassword(ActionEvent event) {
        String oldPassword = txtOldPassword.getText();
        String newPassword = txtNewPassword.getText();
        String confirmPassword = txtConfirmPassword.getText();
        
        if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thông tin thiếu", "Vui lòng nhập đầy đủ thông tin");
            return;
        }
        
        if (!newPassword.equals(confirmPassword)) {
            showAlert(Alert.AlertType.WARNING, "Không khớp", "Mật khẩu mới và xác nhận mật khẩu không khớp");
            return;
        }
        
        // Verify old password
        NhanVien verifiedUser = nhanVienDAO.login(currentEmployee.getTenDN(), oldPassword);
        if (verifiedUser == null) {
            showAlert(Alert.AlertType.ERROR, "Sai mật khẩu", "Mật khẩu hiện tại không đúng");
            return;
        }
        
        // Change password
        if (nhanVienDAO.changePassword(currentEmployee.getMaNV(), newPassword)) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã đổi mật khẩu thành công");
            // Clear password fields
            txtOldPassword.clear();
            txtNewPassword.clear();
            txtConfirmPassword.clear();
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể đổi mật khẩu");
        }
    }
    
    @FXML
    void handleBack(ActionEvent event) {
        try {
            App.changeScene("views/employee_home", "Trang chủ");
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể quay lại trang chủ: " + e.getMessage());
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
