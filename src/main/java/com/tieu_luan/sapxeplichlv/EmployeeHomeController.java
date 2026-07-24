package com.tieu_luan.sapxeplichlv;

import dao.LichLVDAO;
import models.LichLV;
import utils.Session;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmployeeHomeController {

    @FXML private Label lblWelcome;
    @FXML private Label lblMonthYear;
    @FXML private GridPane calendarGrid;
    @FXML private Button btnPrevMonth;
    @FXML private Button btnNextMonth;
    @FXML private Button btnCreateRequest;
    @FXML private Button btnViewProfile;
    @FXML private Button btnLogout;
    
    private LichLVDAO lichLVDAO;
    private YearMonth currentYearMonth;
    private final DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy");

    @FXML
    public void initialize() {
        // Set welcome message
        if (Session.getCurrentUser() != null) {
            lblWelcome.setText("Xin chào, " + Session.getCurrentUser().getHoTen());
        }
        
        // Initialize DAO
        lichLVDAO = new LichLVDAO();
        
        // Set up current month
        currentYearMonth = YearMonth.now();
        updateCalendar();
        
        // Set month/year label
        updateMonthYearLabel();
    }
    
    private void updateMonthYearLabel() {
        lblMonthYear.setText("Tháng " + currentYearMonth.getMonthValue() + "/" + currentYearMonth.getYear());
    }
    
    @FXML
    void handlePrevMonth(ActionEvent event) {
        currentYearMonth = currentYearMonth.minusMonths(1);
        updateMonthYearLabel();
        updateCalendar();
    }
    
    @FXML
    void handleNextMonth(ActionEvent event) {
        currentYearMonth = currentYearMonth.plusMonths(1);
        updateMonthYearLabel();
        updateCalendar();
    }
    
    private void updateCalendar() {
        // Clear the grid
        calendarGrid.getChildren().clear();
        
        // Add day headers (Mon, Tue, ...)
        String[] dayNames = {"Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "CN"};
        for (int i = 0; i < 7; i++) {
            Label dayLabel = new Label(dayNames[i]);
            dayLabel.getStyleClass().add("calendar-header");
            calendarGrid.add(dayLabel, i, 0);
        }
        
        // Get employee's schedules for the month
        if (Session.getCurrentUser() != null) {
            int employeeId = Session.getCurrentUser().getMaNV();
            List<LichLV> employeeSchedules = lichLVDAO.getLichLVByEmployeeAndMonth(
                employeeId, currentYearMonth.getMonthValue(), currentYearMonth.getYear());
            
            // Group schedules by date
            Map<LocalDate, LichLV> schedulesByDate = new HashMap<>();
            for (LichLV schedule : employeeSchedules) {
                schedulesByDate.put(schedule.getNgayLam(), schedule);
            }
            
            // Determine the day of week for the first day of month (0 = Monday, ..., 6 = Sunday)
            LocalDate firstDay = currentYearMonth.atDay(1);
            int dayOfWeekValue = firstDay.getDayOfWeek().getValue() - 1; // Adjust for 0-based index
            
            // Generate calendar cells
            int day = 1;
            int maxDay = currentYearMonth.lengthOfMonth();
            
            for (int week = 1; week <= 6; week++) {
                for (int weekday = 0; weekday < 7; weekday++) {
                    if ((week == 1 && weekday < dayOfWeekValue) || day > maxDay) {
                        // Empty cell
                        calendarGrid.add(new Label(""), weekday, week);
                    } else {
                        // Create date cell
                        LocalDate cellDate = currentYearMonth.atDay(day);
                        VBox cellContent = createDateCell(cellDate, schedulesByDate.get(cellDate));
                        calendarGrid.add(cellContent, weekday, week);
                        day++;
                    }
                }
                if (day > maxDay) break;
            }
        }
    }
    
    private VBox createDateCell(LocalDate date, LichLV schedule) {
        VBox cell = new VBox(5);
        cell.getStyleClass().add("calendar-cell");
        
        // Add date label
        Label dateLabel = new Label(String.valueOf(date.getDayOfMonth()));
        dateLabel.getStyleClass().add("date-label");
        
        // Highlight today's date
        if (date.equals(LocalDate.now())) {
            dateLabel.getStyleClass().add("today-label");
        }
        
        cell.getChildren().add(dateLabel);
        
        // Add schedule information if available
        if (schedule != null && schedule.getTenCa() != null) {
            Label shiftLabel = new Label(schedule.getTenCa());
            shiftLabel.getStyleClass().add("shift-label");
            
            Label timeLabel = new Label(schedule.getGioBD() + " - " + schedule.getGioKT());
            timeLabel.getStyleClass().add("time-label");
            
            cell.getChildren().addAll(shiftLabel, timeLabel);
            cell.getStyleClass().add("has-shift");
        } else if (schedule != null) {
            // Employee has a day off
            Label offLabel = new Label("Nghỉ");
            offLabel.getStyleClass().add("off-label");
            cell.getChildren().add(offLabel);
        } else if (date.isBefore(LocalDate.now())) {
            // Past date without a schedule
            cell.getStyleClass().add("past-date");
        }
        
        return cell;
    }
    
    @FXML
    void handleCreateRequest(ActionEvent event) {
        try {
            App.changeScene("views/employee_request", "Tạo yêu cầu đổi lịch");
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể mở trang tạo yêu cầu: " + e.getMessage());
        }
    }
    
    @FXML
    void handleViewProfile(ActionEvent event) {
        try {
            App.changeScene("views/employee_profile", "Thông tin cá nhân");
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể mở trang thông tin cá nhân: " + e.getMessage());
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
        alert.showAndWait();
    }
}
