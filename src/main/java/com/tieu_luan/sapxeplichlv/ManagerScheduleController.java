package com.tieu_luan.sapxeplichlv;

import com.itextpdf.text.BaseColor;
import dao.CaDAO;
import dao.LichLVDAO;
import dao.NhanVienDAO;
import dao.YeuCauDLDAO;
import models.Ca;
import models.LichLV;
import models.NhanVien;
import models.YeuCauDL;
import utils.ScheduleAlgorithm;
import utils.Session;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import javafx.stage.FileChooser;
import javafx.scene.Node;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfWriter;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import utils.FontLoader;


public class ManagerScheduleController {

    @FXML private TableView<List<Object>> tblSchedule;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> cmbViewType;
    @FXML private ComboBox<NhanVien> cmbEmployee;
    @FXML private ComboBox<Ca> cmbShift;
    @FXML private Button btnAddSchedule;
    @FXML private Button btnUpdateSchedule;
    @FXML private Button btnDeleteSchedule;
    @FXML private Button btnExportExcel;
    @FXML private Button btnExportPDF;
    @FXML private Button btnBack;
    @FXML private Button btnRefresh;
    @FXML private Button btnAddRequest;
    @FXML private GridPane calendarGrid;
    @FXML private Label lblMonthYear;
    
    private NhanVienDAO nhanVienDAO;
    private CaDAO caDAO;
    private LichLVDAO lichLVDAO;
    private YeuCauDLDAO yeuCauDLDAO;
    
    private ObservableList<List<Object>> scheduleData;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private LocalDate currentViewDate;
    private String currentViewType;
    private LichLV selectedSchedule;

    @FXML
    public void initialize() {
        // Initialize DAOs
        nhanVienDAO = new NhanVienDAO();
        caDAO = new CaDAO();
        lichLVDAO = new LichLVDAO();
        yeuCauDLDAO = new YeuCauDLDAO();
        
        // Set up view type combo box
        cmbViewType.setItems(FXCollections.observableArrayList("Ngày", "Tuần", "Tháng"));
        cmbViewType.setValue("Tháng");
        currentViewType = "Tháng";
        
        // Set up employee combo box
        List<NhanVien> employees = nhanVienDAO.getActiveNhanVien();
        cmbEmployee.setItems(FXCollections.observableArrayList(employees));
        
        // Set up shift combo box
        List<Ca> shifts = caDAO.getAllCa();
        cmbShift.setItems(FXCollections.observableArrayList(shifts));
        
        // Set up date picker with current date
        currentViewDate = LocalDate.now();
        datePicker.setValue(currentViewDate);
        
        // Set up month/year label
        updateMonthYearLabel();
        
        // Set up table selection listener
        tblSchedule.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null && newSelection.size() > 0) {
                if (newSelection.get(0) instanceof LichLV) {
                    selectedSchedule = (LichLV) newSelection.get(0);
                    enableEditButtons(true);
                } else {
                    selectedSchedule = null;
                    enableEditButtons(false);
                }
            } else {
                selectedSchedule = null;
                enableEditButtons(false);
            }
        });
        
        // Load initial view
        loadScheduleView();
        
        // Add change listeners
        datePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                currentViewDate = newVal;
                updateMonthYearLabel();
                loadScheduleView();
            }
        });
        
        cmbViewType.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                currentViewType = newVal;
                loadScheduleView();
            }
        });
        
        // Kiểm tra và tạo lịch tự động khi mở ứng dụng vào đầu tháng
        checkAndGenerateMonthlySchedule();
        
        // Kiểm tra và xử lý các yêu cầu đang chờ
        checkPendingRequests();
    }
    
    private void updateMonthYearLabel() {
        YearMonth yearMonth = YearMonth.from(currentViewDate);
        lblMonthYear.setText("Tháng " + yearMonth.getMonthValue() + "/" + yearMonth.getYear());
    }
    
    private void enableEditButtons(boolean enable) {
        btnUpdateSchedule.setDisable(!enable);
        btnDeleteSchedule.setDisable(!enable);
    }
    
    private void loadScheduleView() {
        switch (currentViewType) {
            case "Ngày":
                loadDayView();
                break;
            case "Tuần":
                loadWeekView();
                break;
            case "Tháng":
                loadMonthView();
                break;
        }
    }
    
    private void loadDayView() {
        // Clear existing columns
        tblSchedule.getColumns().clear();
        
        // Set up columns for day view
        TableColumn<List<Object>, String> employeeCol = new TableColumn<>("Nhân viên");
        employeeCol.setCellValueFactory(cellData -> {
            if (cellData.getValue().get(0) instanceof LichLV) {
                LichLV lich = (LichLV) cellData.getValue().get(0);
                return new SimpleStringProperty(lich.getHoTenNV());
            }
            return new SimpleStringProperty("");
        });
        
        TableColumn<List<Object>, String> positionCol = new TableColumn<>("Vị trí");
        positionCol.setCellValueFactory(cellData -> {
            if (cellData.getValue().get(0) instanceof LichLV) {
                LichLV lich = (LichLV) cellData.getValue().get(0);
                return new SimpleStringProperty(lich.getTenVT());
            }
            return new SimpleStringProperty("");
        });
        
        TableColumn<List<Object>, String> shiftCol = new TableColumn<>("Ca làm việc");
        shiftCol.setCellValueFactory(cellData -> {
            if (cellData.getValue().get(0) instanceof LichLV) {
                LichLV lich = (LichLV) cellData.getValue().get(0);
                return new SimpleStringProperty(lich.getTenCa() != null ? 
                    lich.getTenCa() + " (" + lich.getGioBD() + " - " + lich.getGioKT() + ")" : "Nghỉ");
            }
            return new SimpleStringProperty("");
        });
        
        // Add columns to table
        tblSchedule.getColumns().addAll(employeeCol, positionCol, shiftCol);
        
        // Load data for the selected day
        List<LichLV> daySchedules = lichLVDAO.getLichLVByDateRange(currentViewDate, currentViewDate);
        
        // Convert to table format
        scheduleData = FXCollections.observableArrayList();
        for (LichLV schedule : daySchedules) {
            List<Object> row = new ArrayList<>();
            row.add(schedule);
            scheduleData.add(row);
        }
        
        tblSchedule.setItems(scheduleData);
    }
    
    private void loadWeekView() {
        // Clear existing columns
        tblSchedule.getColumns().clear();
        
        // Calculate the first day of the week (Monday)
        LocalDate weekStart = currentViewDate.with(DayOfWeek.MONDAY);
        
        // Set up employee column
        TableColumn<List<Object>, String> employeeCol = new TableColumn<>("Nhân viên");
        employeeCol.setCellValueFactory(cellData -> {
            if (cellData.getValue().get(0) instanceof NhanVien) {
                NhanVien nv = (NhanVien) cellData.getValue().get(0);
                return new SimpleStringProperty(nv.getHoTen() + " (" + nv.getTenVT() + ")");
            }
            return new SimpleStringProperty("");
        });
        
        tblSchedule.getColumns().add(employeeCol);
        
        // Add a column for each day of the week
        for (int i = 0; i < 7; i++) {
            LocalDate date = weekStart.plusDays(i);
            final int dayIndex = i + 1; // Column index (starting from 1 for the days)
            
            TableColumn<List<Object>, String> dayCol = new TableColumn<>(
                date.getDayOfWeek().toString() + "\n" + date.format(dateFormatter));
            
            dayCol.setCellValueFactory(cellData -> {
                if (cellData.getValue().size() > dayIndex && cellData.getValue().get(dayIndex) instanceof LichLV) {
                    LichLV lich = (LichLV) cellData.getValue().get(dayIndex);
                    return new SimpleStringProperty(lich.getTenCa() != null ? 
                        lich.getTenCa() + "\n" + lich.getGioBD() + " - " + lich.getGioKT() : "Nghỉ");
                }
                return new SimpleStringProperty("Nghỉ");
            });
            
            tblSchedule.getColumns().add(dayCol);
        }
        
        // Load data for the week
        LocalDate weekEnd = weekStart.plusDays(6);
        List<LichLV> weekSchedules = lichLVDAO.getLichLVByDateRange(weekStart, weekEnd);
        List<NhanVien> employees = nhanVienDAO.getActiveNhanVien();
        
        // Group schedules by employee
        Map<Integer, List<LichLV>> schedulesByEmployee = new HashMap<>();
        for (LichLV schedule : weekSchedules) {
            if (!schedulesByEmployee.containsKey(schedule.getMaNV())) {
                schedulesByEmployee.put(schedule.getMaNV(), new ArrayList<>());
            }
            schedulesByEmployee.get(schedule.getMaNV()).add(schedule);
        }
        
        // Create table data
        scheduleData = FXCollections.observableArrayList();
        
        for (NhanVien employee : employees) {
            List<Object> row = new ArrayList<>();
            row.add(employee); // First column is the employee
            
            // Add schedule for each day (or null if no schedule)
            for (int i = 0; i < 7; i++) {
                LocalDate date = weekStart.plusDays(i);
                LichLV daySchedule = findScheduleForDay(schedulesByEmployee.get(employee.getMaNV()), date);
                row.add(daySchedule);
            }
            
            scheduleData.add(row);
        }
        
        tblSchedule.setItems(scheduleData);
    }
    
    private LichLV findScheduleForDay(List<LichLV> schedules, LocalDate date) {
        if (schedules == null) return null;
        
        for (LichLV schedule : schedules) {
            if (schedule.getNgayLam().equals(date)) {
                return schedule;
            }
        }
        return null;
    }
    
    private void loadMonthView() {
        // Instead of using TableView, we'll use the GridPane for a calendar-like view
        tblSchedule.setVisible(false);
        calendarGrid.setVisible(true);
        
        // Clear the grid
        calendarGrid.getChildren().clear();
        
        // Get the year and month from the current date
        YearMonth yearMonth = YearMonth.from(currentViewDate);
        
        // Load all schedules for the month
        LocalDate firstDay = yearMonth.atDay(1);
        LocalDate lastDay = yearMonth.atEndOfMonth();
        List<LichLV> monthSchedules = lichLVDAO.getLichLVByDateRange(firstDay, lastDay);
        
        // Group schedules by date and employee
        Map<LocalDate, Map<Integer, LichLV>> schedulesByDateAndEmployee = new HashMap<>();
        for (LichLV schedule : monthSchedules) {
            if (!schedulesByDateAndEmployee.containsKey(schedule.getNgayLam())) {
                schedulesByDateAndEmployee.put(schedule.getNgayLam(), new HashMap<>());
            }
            schedulesByDateAndEmployee.get(schedule.getNgayLam()).put(schedule.getMaNV(), schedule);
        }
        
        // Add day headers (Mon, Tue, ...)
        String[] dayNames = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for (int i = 0; i < 7; i++) {
            Label dayLabel = new Label(dayNames[i]);
            dayLabel.getStyleClass().add("calendar-header");
            calendarGrid.add(dayLabel, i, 0);
        }
        
        // Determine the day of week for the first day of month (0 = Monday, ..., 6 = Sunday)
        int dayOfWeekValue = firstDay.getDayOfWeek().getValue() - 1; // Adjust for 0-based index
        
        // Generate calendar cells
        int day = 1;
        int maxDay = lastDay.getDayOfMonth();
        
        for (int week = 1; week <= 6; week++) {
            for (int weekday = 0; weekday < 7; weekday++) {
                if ((week == 1 && weekday < dayOfWeekValue) || day > maxDay) {
                    // Empty cell
                    calendarGrid.add(new Label(""), weekday, week);
                } else {
                    // Create date cell
                    LocalDate cellDate = yearMonth.atDay(day);
                    VBox cellContent = createDateCell(cellDate, schedulesByDateAndEmployee.get(cellDate));
                    calendarGrid.add(cellContent, weekday, week);
                    day++;
                }
            }
            if (day > maxDay) break;
        }
    }
    
    private VBox createDateCell(LocalDate date, Map<Integer, LichLV> dateSchedules) {
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
        
        // Add schedule entries
        if (dateSchedules != null) {
            // Group by shift to show in a more compact way
            Map<String, List<String>> employeesByShift = new HashMap<>();
            
            for (LichLV schedule : dateSchedules.values()) {
                String shiftName = schedule.getTenCa() != null ? schedule.getTenCa() : "Nghỉ";
                if (!employeesByShift.containsKey(shiftName)) {
                    employeesByShift.put(shiftName, new ArrayList<>());
                }
                
                employeesByShift.get(shiftName).add(schedule.getHoTenNV());
            }
            
            // Add shift labels
            for (Map.Entry<String, List<String>> entry : employeesByShift.entrySet()) {
                Label shiftLabel = new Label(entry.getKey() + ": " + String.join(", ", entry.getValue()));
                shiftLabel.setWrapText(true);
                shiftLabel.getStyleClass().add("shift-label");
                cell.getChildren().add(shiftLabel);
            }
        }
        
        // Make cell clickable to select the date
        cell.setOnMouseClicked(e -> {
            datePicker.setValue(date);
            cmbViewType.setValue("Ngày");
        });
        
        return cell;
    }
    
    @FXML
    void handleAddSchedule(ActionEvent event) {
        if (datePicker.getValue() == null || cmbEmployee.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Thông tin thiếu", 
                "Vui lòng chọn ngày và nhân viên để thêm lịch làm việc");
            return;
        }
        
        // Create new schedule
        LichLV newSchedule = new LichLV();
        newSchedule.setMaNV(cmbEmployee.getValue().getMaNV());
        newSchedule.setNgayLam(datePicker.getValue());
        
        if (cmbShift.getValue() != null) {
            newSchedule.setMaCa(cmbShift.getValue().getMaCa());
        } else {
            newSchedule.setMaCa(0); // No shift assigned (day off)
        }
        
        // Check if employee already has a schedule for this day
        boolean hasExistingSchedule = isEmployeeScheduledOnDate(
            cmbEmployee.getValue().getMaNV(), datePicker.getValue());
        
        if (hasExistingSchedule) {
            showAlert(Alert.AlertType.WARNING, "Trùng lịch", 
                "Nhân viên này đã có lịch làm việc vào ngày " + datePicker.getValue());
            return;
        }
        
        // Save new schedule
        if (lichLVDAO.insertLichLV(newSchedule)) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã thêm lịch làm việc mới");
            loadScheduleView();
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể thêm lịch làm việc mới");
        }
    }
    
    private boolean isEmployeeScheduledOnDate(int employeeId, LocalDate date) {
        List<LichLV> employeeSchedules = lichLVDAO.getLichLVByEmployee(employeeId);
        return employeeSchedules.stream()
            .anyMatch(l -> l.getNgayLam().equals(date));
    }
    
    @FXML
    void handleUpdateSchedule(ActionEvent event) {
        if (selectedSchedule == null) {
            showAlert(Alert.AlertType.WARNING, "Chưa chọn", "Vui lòng chọn một lịch làm việc để cập nhật");
            return;
        }
        
        if (cmbShift.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Thông tin thiếu", "Vui lòng chọn ca làm việc mới");
            return;
        }
        
        // Update the selected schedule
        selectedSchedule.setMaCa(cmbShift.getValue().getMaCa());
        
        if (lichLVDAO.updateLichLV(selectedSchedule)) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã cập nhật lịch làm việc");
            loadScheduleView();
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể cập nhật lịch làm việc");
        }
    }
    
    @FXML
    void handleDeleteSchedule(ActionEvent event) {
        if (selectedSchedule == null) {
            showAlert(Alert.AlertType.WARNING, "Chưa chọn", "Vui lòng chọn một lịch làm việc để xóa");
            return;
        }
        
        // Confirm before deleting
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Xác nhận");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("Bạn có chắc muốn xóa lịch làm việc này?");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (lichLVDAO.deleteLichLV(selectedSchedule.getMaLich())) {
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã xóa lịch làm việc");
                    loadScheduleView();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể xóa lịch làm việc");
                }
            }
        });
    }
    
    @FXML
    void handleAddRequest(ActionEvent event) {
        if (selectedSchedule == null && currentViewType.equals("Ngày")) {
            showAlert(Alert.AlertType.WARNING, "Chưa chọn", "Vui lòng chọn một lịch làm việc để tạo yêu cầu");
            return;
        }
        
        // Create a dialog for request type selection
        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Tạo yêu cầu đổi lịch");
        dialog.setHeaderText("Chọn loại yêu cầu");
        
        // Set the button types
        ButtonType btnShiftChange = new ButtonType("Đổi ca làm việc", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnLeaveRequest = new ButtonType("Xin nghỉ phép", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(btnShiftChange, btnLeaveRequest, btnCancel);
        
        // Result converter
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnShiftChange) {
                return 1; // Shift change
            } else if (dialogButton == btnLeaveRequest) {
                return 0; // Leave request
            }
            return null;
        });
        
        Optional<Integer> result = dialog.showAndWait();
        
        if (result.isPresent()) {
            int requestType = result.get();
            
            if (requestType == 1) { // Shift change request
                if (selectedSchedule == null) {
                    showAlert(Alert.AlertType.WARNING, "Chưa chọn", "Vui lòng chọn một lịch làm việc để đổi ca");
                    return;
                }
                
                createShiftChangeRequest(selectedSchedule);
            } else { // Leave request
                createLeaveRequest();
            }
        }
    }
    
    private void createShiftChangeRequest(LichLV schedule) {
        // Create a dialog for shift change details
        Dialog<YeuCauDL> dialog = new Dialog<>();
        dialog.setTitle("Tạo yêu cầu đổi ca");
        dialog.setHeaderText("Chọn nhân viên muốn đổi ca");
        
        // Set the button types
        ButtonType btnOk = new ButtonType("Tạo yêu cầu", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(btnOk, btnCancel);
        
        // Create dialog content
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        
        ComboBox<NhanVien> employeeCombo = new ComboBox<>();
        employeeCombo.setPromptText("Chọn nhân viên để đổi ca");
        
        // Get employees with same position
        List<NhanVien> samePositionEmployees = nhanVienDAO.getActiveNhanVien().stream()
            .filter(e -> e.getMaVT() == nhanVienDAO.getNhanVienById(schedule.getMaNV()).getMaVT()
                && e.getMaNV() != schedule.getMaNV())
            .toList();
        
        employeeCombo.setItems(FXCollections.observableArrayList(samePositionEmployees));
        
        grid.add(new Label("Nhân viên đổi ca:"), 0, 0);
        grid.add(employeeCombo, 1, 0);
        
        dialog.getDialogPane().setContent(grid);
        
        // Result converter
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnOk && employeeCombo.getValue() != null) {
                // Create request
                YeuCauDL request = new YeuCauDL();
                request.setMaNV(schedule.getMaNV());
                request.setLoaiYC(1); // Shift change
                request.setMaLich(schedule.getMaLich());
                request.setNhanVienDoi(employeeCombo.getValue().getMaNV());
                request.setTrangThai(0); // Pending
                
                return request;
            }
            return null;
        });
        
        Optional<YeuCauDL> result = dialog.showAndWait();
        
        if (result.isPresent()) {
            YeuCauDL request = result.get();
            
            if (yeuCauDLDAO.insertYeuCauDL(request)) {
                showAlert(Alert.AlertType.INFORMATION, "Thành công", 
                    "Đã tạo yêu cầu đổi ca, chờ quản lý phê duyệt");
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tạo yêu cầu đổi ca");
            }
        }
    }
    
    private void createLeaveRequest() {
        // Create a dialog for leave request details
        Dialog<YeuCauDL> dialog = new Dialog<>();
        dialog.setTitle("Tạo yêu cầu nghỉ phép");
        dialog.setHeaderText("Chọn nhân viên và thời gian nghỉ phép");
        
        // Set the button types
        ButtonType btnOk = new ButtonType("Tạo yêu cầu", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(btnOk, btnCancel);
        
        // Create dialog content
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        
        ComboBox<NhanVien> employeeCombo = new ComboBox<>();
        employeeCombo.setPromptText("Chọn nhân viên");
        employeeCombo.setItems(FXCollections.observableArrayList(nhanVienDAO.getActiveNhanVien()));
        
        DatePicker startDatePicker = new DatePicker(LocalDate.now());
        DatePicker endDatePicker = new DatePicker(LocalDate.now().plusDays(1));
        
        grid.add(new Label("Nhân viên:"), 0, 0);
        grid.add(employeeCombo, 1, 0);
        grid.add(new Label("Ngày bắt đầu:"), 0, 1);
        grid.add(startDatePicker, 1, 1);
        grid.add(new Label("Ngày kết thúc:"), 0, 2);
        grid.add(endDatePicker, 1, 2);
        
        dialog.getDialogPane().setContent(grid);
        
        // Result converter
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnOk && employeeCombo.getValue() != null) {
                // Validate dates
                if (endDatePicker.getValue().isBefore(startDatePicker.getValue())) {
                    showAlert(Alert.AlertType.WARNING, "Lỗi ngày", 
                        "Ngày kết thúc không thể trước ngày bắt đầu");
                    return null;
                }
                
                // Create request
                YeuCauDL request = new YeuCauDL();
                request.setMaNV(employeeCombo.getValue().getMaNV());
                request.setLoaiYC(0); // Leave request
                request.setNgayBatDau(startDatePicker.getValue());
                request.setNgayKetThuc(endDatePicker.getValue());
                request.setTrangThai(0); // Pending
                
                return request;
            }
            return null;
        });
        
        Optional<YeuCauDL> result = dialog.showAndWait();
        
        if (result.isPresent()) {
            YeuCauDL request = result.get();
            
            if (yeuCauDLDAO.insertYeuCauDL(request)) {
                showAlert(Alert.AlertType.INFORMATION, "Thành công", 
                    "Đã tạo yêu cầu nghỉ phép, chờ quản lý phê duyệt");
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tạo yêu cầu nghỉ phép");
            }
        }
    }
    
    @FXML
    void handleExportToExcel(ActionEvent event) {
        if (currentViewType.equals("Tháng")) {
            showAlert(Alert.AlertType.WARNING, "Chưa hỗ trợ", 
                "Xuất Excel chỉ hỗ trợ chế độ xem Ngày và Tuần");
            return;
        }
    
        // Chọn file để lưu
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Lưu file Excel");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Excel files (*.xlsx)", "*.xlsx"));
    
        // Tạo tên file dựa trên chế độ xem
        String fileName = "";
        if (currentViewType.equals("Ngày")) {
            fileName = "LichLamViec_" + currentViewDate.format(DateTimeFormatter.ISO_LOCAL_DATE) + ".xlsx";
        } else if (currentViewType.equals("Tuần")) {
            // Tính ngày đầu tuần (Thứ Hai) và ngày cuối tuần (Chủ Nhật)
            LocalDate weekStart = currentViewDate.with(DayOfWeek.MONDAY);
            LocalDate weekEnd = weekStart.plusDays(6);
            fileName = "LichLamViec_" + weekStart.format(DateTimeFormatter.ISO_LOCAL_DATE) + "_" + 
                weekEnd.format(DateTimeFormatter.ISO_LOCAL_DATE) + ".xlsx";
        }
    
        fileChooser.setInitialFileName(fileName);
    
        File file = fileChooser.showSaveDialog(btnExportExcel.getScene().getWindow());
        if (file == null) return;
    
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Lịch làm việc");
        
            // Tạo kiểu dáng cho tiêu đề
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
        
            // Tạo tiêu đề
            Row headerRow = sheet.createRow(0);
        
            if (currentViewType.equals("Ngày")) {
                String[] headers = {"STT", "Họ tên", "Vị trí", "Ca làm việc", "Giờ bắt đầu", "Giờ kết thúc"};
                for (int i = 0; i < headers.length; i++) {
                    org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[i]);
                    cell.setCellStyle(headerStyle);
                }
            
                // Thêm dữ liệu
                List<LichLV> daySchedules = lichLVDAO.getLichLVByDateRange(currentViewDate, currentViewDate);
                for (int i = 0; i < daySchedules.size(); i++) {
                    LichLV schedule = daySchedules.get(i);
                    Row row = sheet.createRow(i + 1);
                
                    row.createCell(0).setCellValue(i + 1);
                    row.createCell(1).setCellValue(schedule.getHoTenNV());
                    row.createCell(2).setCellValue(schedule.getTenVT());
                    row.createCell(3).setCellValue(schedule.getTenCa() != null ? schedule.getTenCa() : "Nghỉ");
                
                    if (schedule.getGioBD() != null) {
                        row.createCell(4).setCellValue(schedule.getGioBD().toString());
                    }
                
                    if (schedule.getGioKT() != null) {
                        row.createCell(5).setCellValue(schedule.getGioKT().toString());
                    }
                }
            } else if (currentViewType.equals("Tuần")) {
                // Tính ngày đầu tuần (Thứ Hai)
                LocalDate weekStart = currentViewDate.with(DayOfWeek.MONDAY);
            
                // Tạo tiêu đề
                String[] headers = new String[8];
                headers[0] = "Nhân viên";
            
                for (int i = 0; i < 7; i++) {
                    LocalDate date = weekStart.plusDays(i);
                    headers[i + 1] = date.getDayOfWeek().toString() + " " + date.format(dateFormatter);
                }
            
                for (int i = 0; i < headers.length; i++) {
                    org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[i]);
                    cell.setCellStyle(headerStyle);
                }
            
                // Lấy dữ liệu tuần
                LocalDate weekEnd = weekStart.plusDays(6);
                List<LichLV> weekSchedules = lichLVDAO.getLichLVByDateRange(weekStart, weekEnd);
                List<NhanVien> employees = nhanVienDAO.getActiveNhanVien();
            
                // Nhóm lịch làm việc theo nhân viên
                Map<Integer, Map<LocalDate, LichLV>> schedulesByEmployee = new HashMap<>();
                for (LichLV schedule : weekSchedules) {
                    if (!schedulesByEmployee.containsKey(schedule.getMaNV())) {
                        schedulesByEmployee.put(schedule.getMaNV(), new HashMap<>());
                    }
                    schedulesByEmployee.get(schedule.getMaNV()).put(schedule.getNgayLam(), schedule);
                }
            
                // Thêm dữ liệu
                for (int i = 0; i < employees.size(); i++) {
                    NhanVien employee = employees.get(i);
                    Row row = sheet.createRow(i + 1);
                
                    // Tên và vị trí của nhân viên
                    row.createCell(0).setCellValue(employee.getHoTen() + " (" + employee.getTenVT() + ")");
                
                    // Lịch làm việc cho mỗi ngày
                    for (int j = 0; j < 7; j++) {
                        LocalDate date = weekStart.plusDays(j);
                    
                        Map<LocalDate, LichLV> employeeSchedules = schedulesByEmployee.get(employee.getMaNV());
                        String cellValue = "Nghỉ";
                    
                        if (employeeSchedules != null && employeeSchedules.containsKey(date)) {
                            LichLV schedule = employeeSchedules.get(date);
                            if (schedule.getTenCa() != null) {
                                cellValue = schedule.getTenCa() + " (" + 
                                    schedule.getGioBD() + " - " + schedule.getGioKT() + ")";
                            }
                        }
                    
                        row.createCell(j + 1).setCellValue(cellValue);
                    }
                }
            }
        
            // Tự động điều chỉnh kích thước cột
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                sheet.autoSizeColumn(i);
            }
        
            // Ghi vào file
            try (FileOutputStream fileOut = new FileOutputStream(file)) {
                workbook.write(fileOut);
                showAlert(Alert.AlertType.INFORMATION, "Thành công", 
                    "Đã xuất lịch làm việc ra file Excel");
            }
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể xuất file Excel: " + e.getMessage());
        }
    }

    
     @FXML
    void handleExportToPDF(ActionEvent event) {
        if (currentViewType.equals("Tháng")) {
            showAlert(Alert.AlertType.WARNING, "Chưa hỗ trợ", 
                "Xuất PDF chỉ hỗ trợ chế độ xem Ngày và Tuần");
            return;
        }
        
        // Choose file to save
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Lưu file PDF");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("PDF files (*.pdf)", "*.pdf"));
        fileChooser.setInitialFileName("LichLamViec_" + 
            YearMonth.from(currentViewDate).toString() + ".pdf");
        
        File file = fileChooser.showSaveDialog(btnExportPDF.getScene().getWindow());
        if (file == null) return;
        
        try {
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();
            
            // Add title
            String title = "LỊCH LÀM VIỆC ";
            if (currentViewType.equals("Ngày")) {
                title += "NGÀY " + currentViewDate.format(dateFormatter);
            } else if (currentViewType.equals("Tuần")) {
                LocalDate weekStart = currentViewDate.with(DayOfWeek.MONDAY);
                LocalDate weekEnd = weekStart.plusDays(6);
                title += "TUẦN TỪ " + weekStart.format(dateFormatter) + 
                    " ĐẾN " + weekEnd.format(dateFormatter);
            }
            
            Paragraph titleParagraph = new Paragraph(title);
            titleParagraph.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            document.add(titleParagraph);
            document.add(new Paragraph(" ")); // Add space
            
            if (currentViewType.equals("Ngày")) {
                // Create table with 6 columns
                PdfPTable table = new PdfPTable(6);
                table.setWidthPercentage(100);
                
                // Add header row
                String[] headers = {"STT", "Họ tên", "Vị trí", "Ca làm việc", "Giờ bắt đầu", "Giờ kết thúc"};
                for (String header : headers) {
                    PdfPCell cell = new PdfPCell(new Paragraph(header));
                    cell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                    table.addCell(cell);
                }
                
                // Add data rows
                List<LichLV> daySchedules = lichLVDAO.getLichLVByDateRange(currentViewDate, currentViewDate);
                for (int i = 0; i < daySchedules.size(); i++) {
                    LichLV schedule = daySchedules.get(i);
                    
                    table.addCell(String.valueOf(i + 1));
                    table.addCell(schedule.getHoTenNV());
                    table.addCell(schedule.getTenVT());
                    table.addCell(schedule.getTenCa() != null ? schedule.getTenCa() : "Nghỉ");
                    table.addCell(schedule.getGioBD() != null ? schedule.getGioBD().toString() : "");
                    table.addCell(schedule.getGioKT() != null ? schedule.getGioKT().toString() : "");
                }
                
                document.add(table);
            } else if (currentViewType.equals("Tuần")) {
                // Calculate the first day of the week (Monday)
                LocalDate weekStart = currentViewDate.with(DayOfWeek.MONDAY);
                
                // Create table with 8 columns (1 for employee, 7 for days)
                PdfPTable table = new PdfPTable(8);
                table.setWidthPercentage(100);
                
                // Add header row
                String[] headers = new String[8];
                headers[0] = "Nhân viên";
                
                for (int i = 0; i < 7; i++) {
                    LocalDate date = weekStart.plusDays(i);
                    headers[i + 1] = date.getDayOfWeek().toString() + "\n" + date.format(dateFormatter);
                }
                
                for (String header : headers) {
                    PdfPCell cell = new PdfPCell(new Paragraph(header));
                    cell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                    table.addCell(cell);
                }
                
                // Get week data
                LocalDate weekEnd = weekStart.plusDays(6);
                List<LichLV> weekSchedules = lichLVDAO.getLichLVByDateRange(weekStart, weekEnd);
                List<NhanVien> employees = nhanVienDAO.getActiveNhanVien();
                
                // Group schedules by employee
                Map<Integer, Map<LocalDate, LichLV>> schedulesByEmployee = new HashMap<>();
                for (LichLV schedule : weekSchedules) {
                    if (!schedulesByEmployee.containsKey(schedule.getMaNV())) {
                        schedulesByEmployee.put(schedule.getMaNV(), new HashMap<>());
                    }
                    schedulesByEmployee.get(schedule.getMaNV()).put(schedule.getNgayLam(), schedule);
                }
                
                // Add data rows
                for (NhanVien employee : employees) {
                    table.addCell(employee.getHoTen() + " (" + employee.getTenVT() + ")");
                    
                    // Schedule for each day
                    for (int j = 0; j < 7; j++) {
                        LocalDate date = weekStart.plusDays(j);
                        
                        Map<LocalDate, LichLV> employeeSchedules = schedulesByEmployee.get(employee.getMaNV());
                        String cellValue = "Nghỉ";
                        
                        if (employeeSchedules != null && employeeSchedules.containsKey(date)) {
                            LichLV schedule = employeeSchedules.get(date);
                            if (schedule.getTenCa() != null) {
                                cellValue = schedule.getTenCa() + "\n" + 
                                    schedule.getGioBD() + " - " + schedule.getGioKT();
                            }
                        }
                        
                        table.addCell(cellValue);
                    }
                }
                
                document.add(table);
            }
            
            document.close();
            showAlert(Alert.AlertType.INFORMATION, "Thành công", 
                "Đã xuất lịch làm việc ra file PDF");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể xuất file PDF: " + e.getMessage());
        }
    }
    
    @FXML
    void handleBack(ActionEvent event) {
        try {
            App.changeScene("views/manager_home", "Quản lý - Trang chủ");
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể quay lại trang chủ: " + e.getMessage());
        }
    }
    
    @FXML
    void handleRefresh(ActionEvent event) {
        loadScheduleView();
    }
    
    
     // Tự động tạo lịch làm việc cho tháng mới
     // Chỉ nên chạy vào đầu tháng
     
    @FXML
    void handleGenerateMonthlySchedule(ActionEvent event) {
        // Lấy tháng và năm hiện tại
        YearMonth currentYearMonth = YearMonth.from(currentViewDate);
        
        // Kiểm tra xem đã gần cuối tháng chưa (từ ngày 25 trở đi)
        boolean isNearEndOfMonth = currentViewDate.getDayOfMonth() >= 25;
        YearMonth scheduleYearMonth;
        
        if (isNearEndOfMonth) {
            // Nếu gần cuối tháng, tạo lịch cho tháng tiếp theo
            scheduleYearMonth = currentYearMonth.plusMonths(1);
        } else {
            // Nếu không, tạo lịch cho tháng hiện tại
            scheduleYearMonth = currentYearMonth;
        }
        
        // Kiểm tra xem đã có lịch cho tháng này chưa
        LocalDate firstDayOfMonth = scheduleYearMonth.atDay(1);
        LocalDate lastDayOfMonth = scheduleYearMonth.atEndOfMonth();
        
        List<LichLV> existingSchedules = lichLVDAO.getLichLVByDateRange(firstDayOfMonth, lastDayOfMonth);
        
        if (!existingSchedules.isEmpty()) {
            // Hiển thị hộp thoại xác nhận
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Xác nhận tạo lịch");
            confirmAlert.setHeaderText(null);
            confirmAlert.setContentText("Đã có lịch làm việc cho tháng " + 
                                      scheduleYearMonth.getMonthValue() + "/" + 
                                      scheduleYearMonth.getYear() + 
                                      ". Bạn có muốn tạo lại không?");
            
            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() != ButtonType.OK) {
                return; // Người dùng không muốn tạo lại
            }
        }
        
        // Tạo đối tượng thuật toán lịch và gọi phương thức tạo lịch
        ScheduleAlgorithm algorithm = new ScheduleAlgorithm();
        boolean success = algorithm.taoLichBangThuatToanThamLam(
            scheduleYearMonth.getMonthValue(), 
            scheduleYearMonth.getYear()
        );
        
        if (success) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", 
                    "Đã tạo lịch làm việc cho tháng " + 
                    scheduleYearMonth.getMonthValue() + "/" + 
                    scheduleYearMonth.getYear());
            
            // Cập nhật ngày hiện tại để hiển thị tháng vừa tạo
            datePicker.setValue(firstDayOfMonth);
            loadScheduleView();
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi", 
                    "Không thể tạo lịch làm việc. Vui lòng kiểm tra lại.");
        }
    }
    
    
     // Xử lý yêu cầu đổi ca làm việc giữa hai nhân viên
     
    @FXML
    void handleProcessShiftSwapRequest(ActionEvent event) {
        processPendingRequests();
    }
    
    
     // Kiểm tra xem có yêu cầu đang chờ xử lý không
     
    private void checkPendingRequests() {
        // Lấy danh sách các yêu cầu đang chờ xử lý
        List<YeuCauDL> pendingRequests = yeuCauDLDAO.getYeuCauDLByTrangThai(0); // 0: Chờ duyệt
        
        if (!pendingRequests.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Yêu cầu đang chờ");
            alert.setHeaderText(null);
            alert.setContentText("Có " + pendingRequests.size() + " yêu cầu đang chờ xử lý. " +
                               "Bạn có muốn xem và xử lý ngay bây giờ không?");
            
            ButtonType btnYes = new ButtonType("Có");
            ButtonType btnNo = new ButtonType("Để sau", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(btnYes, btnNo);
            
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == btnYes) {
                processPendingRequests();
            }
        }
    }
    
    
     // Xử lý yêu cầu nghỉ phép
     
    private void processPendingRequests() {
        // Lấy danh sách các yêu cầu đang chờ xử lý
        List<YeuCauDL> pendingRequests = yeuCauDLDAO.getYeuCauDLByTrangThai(0); // 0: Chờ duyệt
        
        if (pendingRequests.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Thông báo", 
                     "Không có yêu cầu đổi lịch nào đang chờ xử lý.");
            return;
        }
        
        // Tạo và hiển thị một TableView để hiển thị các yêu cầu
        Dialog<YeuCauDL> dialog = new Dialog<>();
        dialog.setTitle("Xử lý yêu cầu đổi lịch");
        dialog.setHeaderText("Chọn yêu cầu cần xử lý");
        
        // Thiết lập các nút
        ButtonType processButtonType = new ButtonType("Xử lý", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(processButtonType, ButtonType.CANCEL);
        
        // Tạo TableView để hiển thị yêu cầu
        TableView<YeuCauDL> requestTable = new TableView<>();
        requestTable.setPrefHeight(300);
        requestTable.setPrefWidth(800);
        
        // Tạo các cột
        TableColumn<YeuCauDL, String> employeeCol = new TableColumn<>("Nhân viên");
        employeeCol.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getHoTenNV()));
        
        TableColumn<YeuCauDL, String> requestTypeCol = new TableColumn<>("Loại yêu cầu");
        requestTypeCol.setCellValueFactory(data -> {
            int loaiYC = data.getValue().getLoaiYC();
            return new SimpleStringProperty(loaiYC == 0 ? "Nghỉ phép" : "Đổi ca");
        });
        
        TableColumn<YeuCauDL, String> detailsCol = new TableColumn<>("Chi tiết");
        detailsCol.setCellValueFactory(data -> {
            YeuCauDL yeuCau = data.getValue();
            if (yeuCau.getLoaiYC() == 0) { // Nghỉ phép
                LocalDate startDate = yeuCau.getNgayBatDau();
                LocalDate endDate = yeuCau.getNgayKetThuc();
                return new SimpleStringProperty("Nghỉ từ " + 
                    (startDate != null ? startDate.format(dateFormatter) : "N/A") + 
                    " đến " + 
                    (endDate != null ? endDate.format(dateFormatter) : "N/A"));
            } else { // Đổi ca
                return new SimpleStringProperty("Đổi ca ngày " + 
                    (yeuCau.getNgayLam() != null ? yeuCau.getNgayLam().format(dateFormatter) : "N/A") + 
                    " với " + (yeuCau.getHoTenNVDoi() != null ? yeuCau.getHoTenNVDoi() : "N/A"));
            }
        });
        
        requestTable.getColumns().addAll(employeeCol, requestTypeCol, detailsCol);
        requestTable.setItems(FXCollections.observableArrayList(pendingRequests));
        
        // Xử lý sự kiện chọn
        Node processButton = dialog.getDialogPane().lookupButton(processButtonType);
        processButton.setDisable(true);
        
        requestTable.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> processButton.setDisable(newValue == null));
        
        dialog.getDialogPane().setContent(requestTable);
        
        // Chuyển đổi kết quả khi nút được nhấn
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == processButtonType) {
                return requestTable.getSelectionModel().getSelectedItem();
            }
            return null;
        });
        
        Optional<YeuCauDL> result = dialog.showAndWait();
        
        result.ifPresent(selectedRequest -> {
            // Xử lý yêu cầu đã chọn
            boolean processed = false;
            
            if (selectedRequest.getLoaiYC() == 0) { // Nghỉ phép
                processed = processLeaveRequest(selectedRequest);
            } else { // Đổi ca
                processed = processShiftSwapRequest(selectedRequest);
            }
            
            if (processed) {
                // Cập nhật trạng thái yêu cầu
                selectedRequest.setTrangThai(1); // 1: Đã chấp nhận
                yeuCauDLDAO.updateYeuCauDL(selectedRequest);
                
                showAlert(Alert.AlertType.INFORMATION, "Thành công", 
                         "Đã xử lý yêu cầu thành công.");
                
                // Cập nhật lại dữ liệu hiển thị
                loadScheduleView();
            } else {
                // Có thể cho phép người dùng từ chối yêu cầu ở đây
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("Từ chối yêu cầu");
                confirmAlert.setHeaderText(null);
                confirmAlert.setContentText("Không thể xử lý yêu cầu. Bạn có muốn từ chối yêu cầu này không?");
                
                Optional<ButtonType> confirmResult = confirmAlert.showAndWait();
                if (confirmResult.isPresent() && confirmResult.get() == ButtonType.OK) {
                    selectedRequest.setTrangThai(2); // 2: Từ chối
                    yeuCauDLDAO.updateYeuCauDL(selectedRequest);
                    
                    showAlert(Alert.AlertType.INFORMATION, "Thông báo", 
                             "Đã từ chối yêu cầu.");
                }
            }
        });
    }
    
    
     // Xử lý yêu cầu nghỉ phép
     
    private boolean processLeaveRequest(YeuCauDL yeuCau) {
        if (yeuCau.getNgayBatDau() == null || yeuCau.getNgayKetThuc() == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Thiếu thông tin ngày nghỉ phép.");
            return false;
        }
        
        // Sử dụng thuật toán heuristic để điều chỉnh lịch làm việc
        ScheduleAlgorithm algorithm = new ScheduleAlgorithm();
        return algorithm.adjustScheduleForLeave(
            yeuCau.getMaNV(), 
            yeuCau.getNgayBatDau(), 
            yeuCau.getNgayKetThuc()
        );
    }
    
    
     // Xử lý yêu cầu đổi ca
     
    private boolean processShiftSwapRequest(YeuCauDL yeuCau) {
        if (yeuCau.getMaLich() == null || yeuCau.getNhanVienDoi() == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Thiếu thông tin lịch hoặc nhân viên đổi ca.");
            return false;
        }
        
        // Lấy lịch cần đổi
        LichLV lichChinh = lichLVDAO.getLichLVById(yeuCau.getMaLich());
        
        if (lichChinh == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không tìm thấy lịch làm việc cần đổi.");
            return false;
        }
        
        // Tìm lịch của nhân viên đổi trong cùng ngày (nếu có)
        LocalDate ngayLam = lichChinh.getNgayLam();
        List<LichLV> lichCungNgay = lichLVDAO.getLichLVByDateRange(ngayLam, ngayLam);
        
        LichLV lichDoiDien = null;
        for (LichLV lich : lichCungNgay) {
            if (lich.getMaNV() == yeuCau.getNhanVienDoi()) {
                lichDoiDien = lich;
                break;
            }
        }
        
        // Xử lý đổi ca
        if (lichDoiDien != null) {
            // Trường hợp đổi ca: đổi nhân viên giữa hai ca trong cùng một ngày
            int tempMaNV = lichChinh.getMaNV();
            lichChinh.setMaNV(lichDoiDien.getMaNV());
            lichDoiDien.setMaNV(tempMaNV);
            
            return lichLVDAO.updateLichLV(lichChinh) && lichLVDAO.updateLichLV(lichDoiDien);
        } else {
            // Trường hợp nhân viên muốn đổi đang nghỉ ngày đó
            // Gán trực tiếp nhân viên đổi vào ca làm việc
            lichChinh.setMaNV(yeuCau.getNhanVienDoi());
            return lichLVDAO.updateLichLV(lichChinh);
        }
    }
    
    
     // Kiểm tra và tự động tạo lịch cho tháng mới nếu cần
     // Gọi vào lúc khởi tạo hoặc vào đầu mỗi tháng
     
    private void checkAndGenerateMonthlySchedule() {
        // Lấy ngày đầu tiên của tháng hiện tại
        LocalDate today = LocalDate.now();
        YearMonth currentYearMonth = YearMonth.from(today);
        LocalDate firstDayOfMonth = currentYearMonth.atDay(1);
        LocalDate lastDayOfMonth = currentYearMonth.atEndOfMonth();
        
        // Kiểm tra xem đã có lịch cho tháng này chưa
        List<LichLV> existingSchedules = lichLVDAO.getLichLVByDateRange(firstDayOfMonth, lastDayOfMonth);
        
        // Nếu đang ở đầu tháng (1-5) và chưa có lịch
        if (today.getDayOfMonth() <= 5 && existingSchedules.isEmpty()) {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Tạo lịch tháng mới");
            confirmAlert.setHeaderText(null);
            confirmAlert.setContentText("Chưa có lịch làm việc cho tháng " + 
                                      currentYearMonth.getMonthValue() + "/" + 
                                      currentYearMonth.getYear() + 
                                      ". Bạn có muốn tạo lịch ngay bây giờ không?");
            
            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                // Tạo lịch cho tháng hiện tại
                ScheduleAlgorithm algorithm = new ScheduleAlgorithm();
                boolean success = algorithm.taoLichBangThuatToanThamLam(
                    currentYearMonth.getMonthValue(), 
                    currentYearMonth.getYear()
                );
                
                if (success) {
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", 
                            "Đã tạo lịch làm việc cho tháng " + 
                            currentYearMonth.getMonthValue() + "/" + 
                            currentYearMonth.getYear());
                    loadScheduleView();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Lỗi", 
                            "Không thể tạo lịch làm việc. Vui lòng kiểm tra lại.");
                }
            }
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
