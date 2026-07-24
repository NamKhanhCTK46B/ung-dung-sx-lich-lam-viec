package com.tieu_luan.sapxeplichlv;

import dao.YeuCauDLDAO;
import models.YeuCauDL;
import utils.ScheduleAlgorithm;
import utils.Session;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class ManagerHomeController {

    @FXML private TableView<YeuCauDL> tblRequests;
    @FXML private TableColumn<YeuCauDL, Integer> colMaYC;
    @FXML private TableColumn<YeuCauDL, String> colTenNV;
    @FXML private TableColumn<YeuCauDL, String> colLoaiYC;
    @FXML private TableColumn<YeuCauDL, LocalDate> colNgayBD;
    @FXML private TableColumn<YeuCauDL, LocalDate> colNgayKT;
    @FXML private TableColumn<YeuCauDL, String> colTrangThai;
    
    @FXML private Button btnApprove;
    @FXML private Button btnReject;
    @FXML private Button btnGenerateSchedule;
    @FXML private Button btnSchedule;
    @FXML private Button btnEmployees;
    @FXML private Button btnLogout;
    
    @FXML private Label lblWelcome;
    @FXML private Label lblCurrentMonth;
    
    private YeuCauDLDAO yeuCauDLDAO;
    private ScheduleAlgorithm scheduleAlgorithm;
    private ObservableList<YeuCauDL> pendingRequests;

    @FXML
    public void initialize() {
        // Set welcome message
        if (Session.getCurrentUser() != null) {
            lblWelcome.setText("Xin chào, " + Session.getCurrentUser().getHoTen());
        }
        
        // Set current month
        YearMonth currentMonth = YearMonth.now();
        lblCurrentMonth.setText("Tháng " + currentMonth.getMonthValue() + "/" + currentMonth.getYear());
        
        // Initialize DAOs
        yeuCauDLDAO = new YeuCauDLDAO();
        scheduleAlgorithm = new ScheduleAlgorithm();
        
        // Set up table columns
        colMaYC.setCellValueFactory(new PropertyValueFactory<>("maYC"));
        colTenNV.setCellValueFactory(new PropertyValueFactory<>("hoTenNV"));
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
        
        // Load pending requests
        loadPendingRequests();
        
        // Enable/disable buttons based on selection
        tblRequests.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean hasSelection = newSelection != null;
            btnApprove.setDisable(!hasSelection || newSelection.getTrangThai() != 0);
            btnReject.setDisable(!hasSelection || newSelection.getTrangThai() != 0);
        });
    }
    
    private void loadPendingRequests() {
        // Get all pending requests (status = 0)
        List<YeuCauDL> requests = yeuCauDLDAO.getYeuCauDLByTrangThai(0);
        pendingRequests = FXCollections.observableArrayList(requests);
        tblRequests.setItems(pendingRequests);
    }
    
    @FXML
    void handleApproveRequest(ActionEvent event) {
        YeuCauDL selectedRequest = tblRequests.getSelectionModel().getSelectedItem();
        if (selectedRequest == null) {
            showAlert(Alert.AlertType.WARNING, "Chưa chọn", "Vui lòng chọn một yêu cầu để duyệt");
            return;
        }
        
        // Update request status to approved (1)
        selectedRequest.setTrangThai(1);
        if (yeuCauDLDAO.updateYeuCauDL(selectedRequest)) {
            // Apply schedule changes based on request type
            boolean scheduleUpdated = false;
            
            if (selectedRequest.getLoaiYC() == 0) {  // Leave request
                // Adjust schedule for leave period
                scheduleUpdated = scheduleAlgorithm.adjustScheduleForLeave(
                    selectedRequest.getMaNV(),
                    selectedRequest.getNgayBatDau(),
                    selectedRequest.getNgayKetThuc()
                );
            } else if (selectedRequest.getLoaiYC() == 1) {  // Shift swap request
                // Handle shift swap
                scheduleUpdated = scheduleAlgorithm.swapShifts(
                    selectedRequest.getMaNV(),
                    selectedRequest.getMaLich(),
                    selectedRequest.getNhanVienDoi()
                );
            }
            
            if (scheduleUpdated) {
                showAlert(Alert.AlertType.INFORMATION, "Thành công", 
                    "Đã duyệt yêu cầu và cập nhật lịch làm việc thành công");
            } else {
                showAlert(Alert.AlertType.WARNING, "Lưu ý", 
                    "Đã duyệt yêu cầu nhưng có thể không thể cập nhật toàn bộ lịch làm việc");
            }
            
            // Refresh the table
            loadPendingRequests();
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể cập nhật trạng thái yêu cầu");
        }
    }
    
    @FXML
    void handleRejectRequest(ActionEvent event) {
        YeuCauDL selectedRequest = tblRequests.getSelectionModel().getSelectedItem();
        if (selectedRequest == null) {
            showAlert(Alert.AlertType.WARNING, "Chưa chọn", "Vui lòng chọn một yêu cầu để từ chối");
            return;
        }
        
        // Update request status to rejected (2)
        selectedRequest.setTrangThai(2);
        if (yeuCauDLDAO.updateYeuCauDL(selectedRequest)) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã từ chối yêu cầu");
            // Refresh the table
            loadPendingRequests();
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể cập nhật trạng thái yêu cầu");
        }
    }
    
    @FXML
    void handleGenerateSchedule(ActionEvent event) {
        // Confirm before generating schedule
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Xác nhận");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("Bạn có chắc muốn tạo lịch làm việc mới cho tháng hiện tại không? Lịch cũ sẽ bị xóa.");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Get current month and year
                LocalDate now = LocalDate.now();
                int month = now.getMonthValue();
                int year = now.getYear();
                
                // Generate schedule using Greedy algorithm
                boolean success = scheduleAlgorithm.taoLichBangThuatToanThamLam(month, year);
                
                if (success) {
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", 
                        "Đã tạo lịch làm việc thành công cho tháng " + month + "/" + year);
                } else {
                    showAlert(Alert.AlertType.ERROR, "Lỗi", 
                        "Không thể tạo lịch làm việc. Vui lòng thử lại sau.");
                }
            }
        });
    }
    
    @FXML
    void handleGoToSchedule(ActionEvent event) {
        try {
            App.changeScene("views/manager_schedule", "Quản lý lịch làm việc");
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể mở trang Quản lý lịch làm việc: " + e.getMessage());
        }
    }
    
    @FXML
    void handleGoToEmployees(ActionEvent event) {
        try {
            App.changeScene("views/manager_employee", "Quản lý nhân viên");
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể mở trang Quản lý nhân viên: " + e.getMessage());
        }
    }
    
    @FXML
    void handleLogout(ActionEvent event) {
        // Clear session
        Session.clear();
        
        try {
            App.changeScene("views/login", "Đăng nhập");
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể đăng xuất: " + e.getMessage());
        }
    }
    
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
}
