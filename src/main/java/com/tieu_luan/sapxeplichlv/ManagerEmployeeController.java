package com.tieu_luan.sapxeplichlv;

import dao.NhanVienDAO;
import dao.ViTriCVDAO;
import models.NhanVien;
import models.ViTriCV;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class ManagerEmployeeController {

    @FXML private TableView<NhanVien> tblEmployees;
    @FXML private TableColumn<NhanVien, Integer> colMaNV;
    @FXML private TableColumn<NhanVien, String> colHoTen;
    @FXML private TableColumn<NhanVien, String> colCCCD;
    @FXML private TableColumn<NhanVien, String> colSDT;
    @FXML private TableColumn<NhanVien, String> colEmail;
    @FXML private TableColumn<NhanVien, String> colGioiTinh;
    @FXML private TableColumn<NhanVien, String> colViTri;
    @FXML private TableColumn<NhanVien, String> colTenDN;
    @FXML private TableColumn<NhanVien, String> colTrangThai;
    
    @FXML private TextField txtHoTen;
    @FXML private TextField txtCCCD;
    @FXML private TextField txtSDT;
    @FXML private TextField txtEmail;
    @FXML private ComboBox<Boolean> cmbGioiTinh;
    @FXML private ComboBox<ViTriCV> cmbViTri;
    @FXML private TextField txtTenDN;
    @FXML private PasswordField txtMatKhau;
    @FXML private ComboBox<Integer> cmbTrangThai;
    @FXML private CheckBox chkResetPassword;
    
    @FXML private Button btnAdd;
    @FXML private Button btnUpdate;
    @FXML private Button btnDelete;
    @FXML private Button btnClear;
    @FXML private Button btnBack;
    
    private NhanVienDAO nhanVienDAO;
    private ViTriCVDAO viTriCVDAO;
    private ObservableList<NhanVien> employeeList;
    private NhanVien selectedEmployee;

    @FXML
    public void initialize() {
        // Initialize DAOs
        nhanVienDAO = new NhanVienDAO();
        viTriCVDAO = new ViTriCVDAO();
        
        // Set up table columns
        colMaNV.setCellValueFactory(new PropertyValueFactory<>("maNV"));
        colHoTen.setCellValueFactory(new PropertyValueFactory<>("hoTen"));
        colCCCD.setCellValueFactory(new PropertyValueFactory<>("cccd"));
        colSDT.setCellValueFactory(new PropertyValueFactory<>("sdt"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colGioiTinh.setCellValueFactory(new PropertyValueFactory<>("gioiTinhText"));
        colViTri.setCellValueFactory(new PropertyValueFactory<>("tenVT"));
        colTenDN.setCellValueFactory(new PropertyValueFactory<>("tenDN"));
        colTrangThai.setCellValueFactory(new PropertyValueFactory<>("trangThaiText"));
        
        // Set up gender combo box
        cmbGioiTinh.setItems(FXCollections.observableArrayList(true, false));
        cmbGioiTinh.setConverter(new StringConverter<Boolean>() {
            @Override
            public String toString(Boolean object) {
                if (object == null) return null;
                return object ? "Nam" : "Nữ";
            }
            
            @Override
            public Boolean fromString(String string) {
                return string.equals("Nam");
            }
        });
        
        // Set up position combo box
        List<ViTriCV> positions = viTriCVDAO.getAllViTriCV();
        cmbViTri.setItems(FXCollections.observableArrayList(positions));
        
        // Set up status combo box
        cmbTrangThai.setItems(FXCollections.observableArrayList(0, 1, 2));
        cmbTrangThai.setConverter(new StringConverter<Integer>() {
            @Override
            public String toString(Integer object) {
                if (object == null) return null;
                switch (object) {
                    case 0: return "Đã nghỉ việc";
                    case 1: return "Đang làm việc";
                    case 2: return "Đang nghỉ phép";
                    default: return "Không xác định";
                }
            }
            
            @Override
            public Integer fromString(String string) {
                switch (string) {
                    case "Đã nghỉ việc": return 0;
                    case "Đang làm việc": return 1;
                    case "Đang nghỉ phép": return 2;
                    default: return 1;
                }
            }
        });
        
        // Set default values
        cmbGioiTinh.setValue(true);
        cmbTrangThai.setValue(1);
        chkResetPassword.setDisable(true);
        
        // Load employees
        loadEmployees();
        
        // Add table selection listener
        tblEmployees.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                selectedEmployee = newSelection;
                populateFields(selectedEmployee);
                btnUpdate.setDisable(false);
                btnDelete.setDisable(false);
                chkResetPassword.setDisable(false);
            } else {
                clearFields();
                selectedEmployee = null;
                btnUpdate.setDisable(true);
                btnDelete.setDisable(true);
                chkResetPassword.setDisable(true);
            }
        });
    }
    
    private void loadEmployees() {
        List<NhanVien> employees = nhanVienDAO.getAllNhanVien();
        employeeList = FXCollections.observableArrayList(employees);
        tblEmployees.setItems(employeeList);
    }
    
    private void populateFields(NhanVien employee) {
        txtHoTen.setText(employee.getHoTen());
        txtCCCD.setText(employee.getCccd());
        txtSDT.setText(employee.getSdt());
        txtEmail.setText(employee.getEmail() != null ? employee.getEmail() : "");
        cmbGioiTinh.setValue(employee.getGioiTinh());
        
        // Find and set the position
        for (ViTriCV position : cmbViTri.getItems()) {
            if (position.getMaVT() == employee.getMaVT()) {
                cmbViTri.setValue(position);
                break;
            }
        }
        
        txtTenDN.setText(employee.getTenDN());
        txtMatKhau.clear(); // Don't show password
        cmbTrangThai.setValue(employee.getTrangThai());
    }
    
    private void clearFields() {
        txtHoTen.clear();
        txtCCCD.clear();
        txtSDT.clear();
        txtEmail.clear();
        cmbGioiTinh.setValue(true);
        cmbViTri.setValue(null);
        txtTenDN.clear();
        txtMatKhau.clear();
        cmbTrangThai.setValue(1);
        chkResetPassword.setSelected(false);
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
        
        if (cmbViTri.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Thông tin thiếu", "Vui lòng chọn vị trí công việc");
            return false;
        }
        
        if (txtTenDN.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thông tin thiếu", "Vui lòng nhập tên đăng nhập");
            return false;
        }
        
        // Check if username is already used by another employee
        if (selectedEmployee == null || !selectedEmployee.getTenDN().equals(txtTenDN.getText().trim())) {
            for (NhanVien employee : employeeList) {
                if (employee.getTenDN().equals(txtTenDN.getText().trim())) {
                    showAlert(Alert.AlertType.WARNING, "Thông tin không hợp lệ", 
                        "Tên đăng nhập đã được sử dụng");
                    return false;
                }
            }
        }
        
        // Password is required for new employees
        if (selectedEmployee == null && txtMatKhau.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thông tin thiếu", "Vui lòng nhập mật khẩu");
            return false;
        }
        
        return true;
    }
    
    @FXML
    void handleAddEmployee(ActionEvent event) {
        if (!validateFields()) {
            return;
        }
        
        NhanVien employee = new NhanVien();
        employee.setHoTen(txtHoTen.getText().trim());
        employee.setCccd(txtCCCD.getText().trim());
        employee.setSdt(txtSDT.getText().trim());
        employee.setEmail(txtEmail.getText().trim().isEmpty() ? null : txtEmail.getText().trim());
        employee.setGioiTinh(cmbGioiTinh.getValue());
        employee.setMaVT(cmbViTri.getValue().getMaVT());
        employee.setTenDN(txtTenDN.getText().trim());
        employee.setMatKhau(txtMatKhau.getText().trim());
        employee.setTrangThai(cmbTrangThai.getValue());
        employee.setSoNgayNghiThang(0); // Default is 0
        
        if (nhanVienDAO.insertNhanVien(employee)) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã thêm nhân viên mới");
            clearFields();
            loadEmployees();
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể thêm nhân viên mới");
        }
    }
    
    @FXML
    void handleUpdateEmployee(ActionEvent event) {
        if (selectedEmployee == null) {
            showAlert(Alert.AlertType.WARNING, "Chưa chọn", "Vui lòng chọn một nhân viên để cập nhật");
            return;
        }
        
        if (!validateFields()) {
            return;
        }
        
        // Update employee information
        selectedEmployee.setHoTen(txtHoTen.getText().trim());
        selectedEmployee.setCccd(txtCCCD.getText().trim());
        selectedEmployee.setSdt(txtSDT.getText().trim());
        selectedEmployee.setEmail(txtEmail.getText().trim().isEmpty() ? null : txtEmail.getText().trim());
        selectedEmployee.setGioiTinh(cmbGioiTinh.getValue());
        selectedEmployee.setMaVT(cmbViTri.getValue().getMaVT());
        selectedEmployee.setTenDN(txtTenDN.getText().trim());
        selectedEmployee.setTrangThai(cmbTrangThai.getValue());
        
        boolean updateSuccess = nhanVienDAO.updateNhanVien(selectedEmployee);
        
        // If password reset is checked, update password
        if (chkResetPassword.isSelected() && !txtMatKhau.getText().trim().isEmpty()) {
            boolean passwordSuccess = nhanVienDAO.changePassword(
                selectedEmployee.getMaNV(), txtMatKhau.getText().trim());
            
            if (!passwordSuccess) {
                showAlert(Alert.AlertType.WARNING, "Lưu ý", 
                    "Đã cập nhật thông tin nhân viên nhưng không thể đổi mật khẩu");
            }
        }
        
        if (updateSuccess) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã cập nhật thông tin nhân viên");
            loadEmployees();
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể cập nhật thông tin nhân viên");
        }
    }
    
    @FXML
    void handleDeleteEmployee(ActionEvent event) {
        if (selectedEmployee == null) {
            showAlert(Alert.AlertType.WARNING, "Chưa chọn", "Vui lòng chọn một nhân viên để xóa");
            return;
        }
        
        // Confirm deletion
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Xác nhận");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("Bạn có chắc muốn đánh dấu nhân viên này là đã nghỉ việc?");
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (nhanVienDAO.deleteNhanVien(selectedEmployee.getMaNV())) {
                showAlert(Alert.AlertType.INFORMATION, "Thành công", 
                    "Đã đánh dấu nhân viên là đã nghỉ việc");
                clearFields();
                loadEmployees();
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể xóa nhân viên");
            }
        }
    }
    
    @FXML
    void handleClearFields(ActionEvent event) {
        clearFields();
        tblEmployees.getSelectionModel().clearSelection();
        selectedEmployee = null;
        btnUpdate.setDisable(true);
        btnDelete.setDisable(true);
    }
    
    @FXML
    void handleBack(ActionEvent event) {
        try {
            App.changeScene("views/manager_home", "Quản lý - Trang chủ");
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
