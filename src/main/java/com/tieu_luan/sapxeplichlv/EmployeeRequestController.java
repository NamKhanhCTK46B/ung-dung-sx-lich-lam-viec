package com.tieu_luan.sapxeplichlv;

import dao.LichLVDAO;
import dao.NhanVienDAO;
import dao.YeuCauDLDAO;
import models.LichLV;
import models.NhanVien;
import models.YeuCauDL;
import utils.Session;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class EmployeeRequestController {

    @FXML private TabPane tabPane;
    @FXML private Tab tabShiftChange;
    @FXML private Tab tabLeave;
    
    // Shift change tab
    @FXML private ComboBox<LichLV> cmbShift;
    @FXML private ComboBox<NhanVien> cmbEmployee;
    @FXML private Label lblCurrentShift;
    
    // Leave request tab
    @FXML private DatePicker dpStartDate;
    @FXML private DatePicker dpEndDate;
    @FXML private Label lblLeaveDays;
    @FXML private Label lblRemainingLeave;
    
    @FXML private Button btnSubmit;
    @FXML private Button btnBack;
    
    private LichLVDAO lichLVDAO;
    private NhanVienDAO nhanVienDAO;
    private YeuCauDLDAO yeuCauDLDAO;
    
    private List<LichLV> employeeSchedules;
    private NhanVien currentEmployee;

    @FXML
    public void initialize() {
        // Initialize DAOs
        lichLVDAO = new LichLVDAO();
        nhanVienDAO = new NhanVienDAO();
        yeuCauDLDAO = new YeuCauDLDAO();
        
        // Get current employee
        if (Session.getCurrentUser() != null) {
            currentEmployee = Session.getCurrentUser();
            
            // Load employee schedules for future dates
            LocalDate today = LocalDate.now();
            employeeSchedules = lichLVDAO.getLichLVByEmployee(currentEmployee.getMaNV()).stream()
                .filter(s -> !s.getNgayLam().isBefore(today) && s.getMaCa() > 0) // Filter for future dates with shifts
                .collect(Collectors.toList());
            
            // Set up shift combo box
            cmbShift.setItems(FXCollections.observableArrayList(employeeSchedules));
            cmbShift.setConverter(new javafx.util.StringConverter<LichLV>() {
                @Override
                public String toString(LichLV schedule) {
                    if (schedule != null) {
                        return schedule.getNgayLam() + " - " + schedule.getTenCa() + 
                            " (" + schedule.getGioBD() + " - " + schedule.getGioKT() + ")";
                    }
                    return null;
                }
                
                @Override
                public LichLV fromString(String string) {
                    return null;
                }
            });
            
            // Update shift details when selection changes
            cmbShift.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    updateShiftDetails(newVal);
                    loadAvailableEmployees(newVal);
                }
            });
            
            // Set up date pickers for leave request
            dpStartDate.setValue(today);
            dpEndDate.setValue(today.plusDays(1));
            
            // Update leave days when dates change
            dpStartDate.valueProperty().addListener((obs, oldVal, newVal) -> updateLeaveDays());
            dpEndDate.valueProperty().addListener((obs, oldVal, newVal) -> updateLeaveDays());
            
            // Update leave days initially
            updateLeaveDays();
            
            // Set remaining leave days
            lblRemainingLeave.setText(String.valueOf(4 - currentEmployee.getSoNgayNghiThang()));
        }
    }
    
    private void updateShiftDetails(LichLV shift) {
        if (shift != null) {
            lblCurrentShift.setText(shift.getTenCa() + " (" + 
                shift.getGioBD() + " - " + shift.getGioKT() + ") - " + shift.getNgayLam());
        } else {
            lblCurrentShift.setText("Chưa chọn ca");
        }
    }
    
    private void loadAvailableEmployees(LichLV shift) {
        // Get employees with same position who are not scheduled on this day
        int positionId = currentEmployee.getMaVT();
        LocalDate shiftDate = shift.getNgayLam();
        
        List<NhanVien> samePositionEmployees = nhanVienDAO.getActiveNhanVien().stream()
            .filter(e -> e.getMaVT() == positionId && e.getMaNV() != currentEmployee.getMaNV())
            .collect(Collectors.toList());
        
        // Filter out employees who already have a shift on this day
        List<NhanVien> availableEmployees = samePositionEmployees.stream()
            .filter(e -> !isEmployeeScheduledOnDate(e.getMaNV(), shiftDate))
            .collect(Collectors.toList());
        
        cmbEmployee.setItems(FXCollections.observableArrayList(availableEmployees));
    }
    
    private boolean isEmployeeScheduledOnDate(int employeeId, LocalDate date) {
        List<LichLV> employeeSchedules = lichLVDAO.getLichLVByEmployee(employeeId);
        return employeeSchedules.stream()
            .anyMatch(s -> s.getNgayLam().equals(date));
    }
    
    private void updateLeaveDays() {
        LocalDate startDate = dpStartDate.getValue();
        LocalDate endDate = dpEndDate.getValue();
        
        if (startDate != null && endDate != null) {
            if (endDate.isBefore(startDate)) {
                lblLeaveDays.setText("Lỗi: Ngày kết thúc trước ngày bắt đầu");
                btnSubmit.setDisable(true);
            } else {
                // Calculate number of days between start and end (inclusive)
                long days = endDate.toEpochDay() - startDate.toEpochDay() + 1;
                lblLeaveDays.setText(String.valueOf(days));
                
                // Check if requested days exceed allowed days
                int remainingDays = 4 - currentEmployee.getSoNgayNghiThang();
                btnSubmit.setDisable(days > remainingDays);
            }
        }
    }
    
    @FXML
    void handleSubmitRequest(ActionEvent event) {
        // Create request based on active tab
        if (tabPane.getSelectionModel().getSelectedItem() == tabShiftChange) {
            submitShiftChangeRequest();
        } else {
            submitLeaveRequest();
        }
    }
    
    private void submitShiftChangeRequest() {
        LichLV selectedShift = cmbShift.getValue();
        NhanVien selectedEmployee = cmbEmployee.getValue();
        
        if (selectedShift == null) {
            showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng chọn ca làm việc cần đổi");
            return;
        }
        
        // Create a shift change request
        YeuCauDL request = new YeuCauDL();
        request.setMaNV(currentEmployee.getMaNV());
        request.setLoaiYC(1); // Shift change
        request.setMaLich(selectedShift.getMaLich());
        
        if (selectedEmployee != null) {
            request.setNhanVienDoi(selectedEmployee.getMaNV());
        }
        
        request.setTrangThai(0); // Pending
        
        if (yeuCauDLDAO.insertYeuCauDL(request)) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", 
                "Đã gửi yêu cầu đổi ca, chờ quản lý phê duyệt");
            
            try {
                App.changeScene("views/employee_home", "Trang chủ");
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể quay lại trang chủ: " + e.getMessage());
            }
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tạo yêu cầu đổi ca");
        }
    }
    
    private void submitLeaveRequest() {
        LocalDate startDate = dpStartDate.getValue();
        LocalDate endDate = dpEndDate.getValue();
        
        if (startDate == null || endDate == null) {
            showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng chọn ngày bắt đầu và kết thúc");
            return;
        }
        
        if (endDate.isBefore(startDate)) {
            showAlert(Alert.AlertType.WARNING, "Lỗi", "Ngày kết thúc không thể trước ngày bắt đầu");
            return;
        }
        
        // Calculate days
        long days = endDate.toEpochDay() - startDate.toEpochDay() + 1;
        int remainingDays = 4 - currentEmployee.getSoNgayNghiThang();
        
        if (days > remainingDays) {
            showAlert(Alert.AlertType.WARNING, "Vượt quá số ngày", 
                "Số ngày nghỉ vượt quá số ngày được phép (" + remainingDays + " ngày)");
            return;
        }
        
        // Create a leave request
        YeuCauDL request = new YeuCauDL();
        request.setMaNV(currentEmployee.getMaNV());
        request.setLoaiYC(0); // Leave request
        request.setNgayBatDau(startDate);
        request.setNgayKetThuc(endDate);
        request.setTrangThai(0); // Pending
        
        if (yeuCauDLDAO.insertYeuCauDL(request)) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", 
                "Đã gửi yêu cầu nghỉ phép, chờ quản lý phê duyệt");
            
            try {
                App.changeScene("views/employee_home", "Trang chủ");
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể quay lại trang chủ: " + e.getMessage());
            }
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tạo yêu cầu nghỉ phép");
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
