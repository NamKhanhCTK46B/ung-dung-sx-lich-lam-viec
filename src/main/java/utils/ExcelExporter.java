package utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import dao.DBConnection;


 // Lớp tiện ích để xuất lịch làm việc ra file Excel
 
public class ExcelExporter {
    
    
     // Xuất lịch làm việc theo tháng ra file Excel
     // @param month Tháng cần xuất (1-12)
     // @param year Năm cần xuất
     // @param filePath Đường dẫn file Excel sẽ tạo
     // @return true nếu xuất thành công, false nếu có lỗi
     
    public static boolean exportMonthlySchedule(int month, int year, String filePath) {
        try (Workbook workbook = new XSSFWorkbook()) {
            // Tạo sheet
            Sheet sheet = workbook.createSheet("Lịch làm việc tháng " + month + "/" + year);
            
            // Tạo các style cho cell
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);
            CellStyle normalStyle = createNormalStyle(workbook);
            CellStyle weekendStyle = createWeekendStyle(workbook);
            
            // Thiết lập độ rộng cột
            sheet.setColumnWidth(0, 4000); // Tên nhân viên
            for (int i = 1; i <= 31; i++) {
                sheet.setColumnWidth(i, 2500); // Các ngày trong tháng
            }
            
            // Tạo tiêu đề
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("LỊCH LÀM VIỆC THÁNG " + month + "/" + year);
            titleCell.setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 31));
            
            // Tạo hàng ngày
            Row dayRow = sheet.createRow(1);
            Cell firstCell = dayRow.createCell(0);
            firstCell.setCellValue("Nhân viên");
            firstCell.setCellStyle(headerStyle);
            
            // Tính số ngày trong tháng
            LocalDate date = LocalDate.of(year, month, 1);
            int daysInMonth = date.lengthOfMonth();
            
            // Thêm các cột ngày
            DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("dd/MM");
            for (int i = 1; i <= daysInMonth; i++) {
                LocalDate currentDate = LocalDate.of(year, month, i);
                Cell dayCell = dayRow.createCell(i);
                dayCell.setCellValue(currentDate.format(dayFormatter));
                
                // Kiểm tra cuối tuần và áp dụng style khác
                int dayOfWeek = currentDate.getDayOfWeek().getValue();
                if (dayOfWeek == 6 || dayOfWeek == 7) { // Thứ 7 hoặc Chủ nhật
                    dayCell.setCellStyle(weekendStyle);
                } else {
                    dayCell.setCellStyle(headerStyle);
                }
            }
            
            // Lấy dữ liệu lịch làm việc từ database
            Map<Integer, Map<LocalDate, String>> scheduleData = getScheduleData(month, year);
            
            // Lấy danh sách nhân viên
            List<Map<String, Object>> employees = getEmployees();
            
            // Thêm dữ liệu cho từng nhân viên
            int rowNum = 2;
            for (Map<String, Object> employee : employees) {
                int employeeId = (Integer) employee.get("MaNV");
                String employeeName = (String) employee.get("HoTen");
                
                Row row = sheet.createRow(rowNum++);
                
                // Tên nhân viên
                Cell empCell = row.createCell(0);
                empCell.setCellValue(employeeName);
                empCell.setCellStyle(normalStyle);
                
                // Thêm lịch làm việc của nhân viên
                Map<LocalDate, String> employeeSchedule = scheduleData.getOrDefault(employeeId, new HashMap<>());
                
                for (int i = 1; i <= daysInMonth; i++) {
                    LocalDate currentDate = LocalDate.of(year, month, i);
                    Cell cell = row.createCell(i);
                    
                    // Nếu có ca làm việc vào ngày này
                    String shiftInfo = employeeSchedule.get(currentDate);
                    cell.setCellValue(shiftInfo != null ? shiftInfo : "");
                    
                    // Kiểm tra cuối tuần và áp dụng style khác
                    int dayOfWeek = currentDate.getDayOfWeek().getValue();
                    if (dayOfWeek == 6 || dayOfWeek == 7) { // Thứ 7 hoặc Chủ nhật
                        cell.setCellStyle(weekendStyle);
                    } else {
                        cell.setCellStyle(normalStyle);
                    }
                }
            }
            
            // Lưu workbook ra file
            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
                return true;
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    
     // Lấy dữ liệu lịch làm việc từ cơ sở dữ liệu
     
    private static Map<Integer, Map<LocalDate, String>> getScheduleData(int month, int year) {
        Map<Integer, Map<LocalDate, String>> scheduleData = new HashMap<>();
        
        String sql = "SELECT l.MaNV, l.NgayLam, c.TenCa, c.GioBD, c.GioKT " +
                    "FROM LichLV l " +
                    "JOIN Ca c ON l.MaCa = c.MaCa " +
                    "WHERE MONTH(l.NgayLam) = ? AND YEAR(l.NgayLam) = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, month);
            stmt.setInt(2, year);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int employeeId = rs.getInt("MaNV");
                    LocalDate date = rs.getDate("NgayLam").toLocalDate();
                    String shiftName = rs.getString("TenCa");
                    
                    // Chuyển đổi giờ thành định dạng hh:mm
                    String startTime = rs.getTime("GioBD").toString().substring(0, 5);
                    String endTime = rs.getTime("GioKT").toString().substring(0, 5);
                    
                    // Tạo thông tin ca làm việc
                    String shiftInfo = shiftName + "\n" + startTime + "-" + endTime;
                    
                    // Thêm vào map
                    if (!scheduleData.containsKey(employeeId)) {
                        scheduleData.put(employeeId, new HashMap<>());
                    }
                    
                    scheduleData.get(employeeId).put(date, shiftInfo);
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return scheduleData;
    }
    
    
     // Lấy danh sách nhân viên từ cơ sở dữ liệu
     
    private static List<Map<String, Object>> getEmployees() {
        List<Map<String, Object>> employees = new ArrayList<>();
        
        String sql = "SELECT MaNV, HoTen FROM NhanVien WHERE TrangThai = 1 ORDER BY HoTen";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Map<String, Object> employee = new HashMap<>();
                employee.put("MaNV", rs.getInt("MaNV"));
                employee.put("HoTen", rs.getString("HoTen"));
                employees.add(employee);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return employees;
    }
    
    
     // Tạo style cho tiêu đề
     
    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setFillForegroundColor(IndexedColors.LIGHT_BLUE.index);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        XSSFFont font = ((XSSFWorkbook) workbook).createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 12);
        font.setBold(true);
        style.setFont(font);
        
        return style;
    }
    
    
     // Tạo style cho các ô ngày
     
    private static CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        
        XSSFFont font = ((XSSFWorkbook) workbook).createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 11);
        font.setBold(true);
        style.setFont(font);
        
        return style;
    }
    
    
     // Tạo style cho các ô thông thường
     
    private static CellStyle createNormalStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setWrapText(true);
        
        XSSFFont font = ((XSSFWorkbook) workbook).createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        
        return style;
    }
    
    
     // Tạo style cho các ô cuối tuần
     
    private static CellStyle createWeekendStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.index);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setWrapText(true);
        
        XSSFFont font = ((XSSFWorkbook) workbook).createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        
        return style;
    }
}