package utils;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import dao.DBConnection;

import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
// Sử dụng tên đầy đủ cho java.util.List để tránh xung đột với com.itextpdf.text.List
import java.util.List;


 // Lớp tiện ích để xuất lịch làm việc ra file PDF
 
public class PDFExporter {

    // Các màu sử dụng trong PDF
    private static final BaseColor HEADER_COLOR = new BaseColor(135, 206, 250); // LightSkyBlue
    private static final BaseColor WEEKEND_COLOR = new BaseColor(255, 255, 224); // LightYellow

    
     // Xuất lịch làm việc theo tháng ra file PDF
     // @param month Tháng cần xuất (1-12)
     // @param year Năm cần xuất
     // @param filePath Đường dẫn file PDF sẽ tạo
     // @return true nếu xuất thành công, false nếu có lỗi
     
    public static boolean exportMonthlySchedule(int month, int year, String filePath) {
        try {
            // Tạo document
            Document document = new Document(PageSize.A3.rotate());
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(filePath));
            
            document.open();
            
            // Thêm metadata
            document.addTitle("Lịch làm việc tháng " + month + "/" + year);
            document.addSubject("Lịch làm việc nhà hàng");
            document.addKeywords("lịch làm việc, nhà hàng, tháng " + month);
            document.addCreator("Restaurant Scheduler Application");
            
            // Font chữ - Sử dụng font mặc định nếu không tìm thấy font tùy chỉnh
            BaseFont baseFont;
            try {
                // Cố gắng tải font tùy chỉnh
                String fontPath = FontLoader.loadFontFromResource("com/tieu_luan/sx_lich_lam_viec/fonts/arial-unicode-ms.ttf");
                baseFont = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            } catch (Exception e) {
                // Nếu không tìm thấy, sử dụng font có sẵn
                baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.EMBEDDED);
                System.out.println("Sử dụng font mặc định vì không tìm thấy font tùy chỉnh: " + e.getMessage());
            }
            
            Font titleFont = new Font(baseFont, 18, Font.BOLD);
            Font headerFont = new Font(baseFont, 10, Font.BOLD);
            Font normalFont = new Font(baseFont, 9, Font.NORMAL);
            
            // Tạo tiêu đề
            Paragraph title = new Paragraph("LỊCH LÀM VIỆC THÁNG " + month + "/" + year, titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(15);
            document.add(title);
            
            // Tính số ngày trong tháng
            LocalDate date = LocalDate.of(year, month, 1);
            int daysInMonth = date.lengthOfMonth();
            
            // Tạo bảng lịch làm việc
            PdfPTable table = new PdfPTable(daysInMonth + 1);
            table.setWidthPercentage(100);
            
            // Thiết lập độ rộng cột
            float[] columnWidths = new float[daysInMonth + 1];
            columnWidths[0] = 3f; // Cột tên nhân viên
            for (int i = 1; i <= daysInMonth; i++) {
                columnWidths[i] = 1f; // Các cột ngày
            }
            table.setWidths(columnWidths);
            
            // Tạo hàng tiêu đề
            PdfPCell headerCell = new PdfPCell(new Phrase("Nhân viên", headerFont));
            headerCell.setBackgroundColor(HEADER_COLOR);
            headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            headerCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            headerCell.setPadding(5);
            table.addCell(headerCell);
            
            // Thêm các cột ngày
            DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("dd/MM");
            for (int i = 1; i <= daysInMonth; i++) {
                LocalDate currentDate = LocalDate.of(year, month, i);
                
                PdfPCell dayCell = new PdfPCell(new Phrase(currentDate.format(dayFormatter), headerFont));
                dayCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                dayCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                dayCell.setPadding(5);
                
                // Kiểm tra cuối tuần và áp dụng style khác
                int dayOfWeek = currentDate.getDayOfWeek().getValue();
                if (dayOfWeek == 6 || dayOfWeek == 7) { // Thứ 7 hoặc Chủ nhật
                    dayCell.setBackgroundColor(WEEKEND_COLOR);
                } else {
                    dayCell.setBackgroundColor(HEADER_COLOR);
                }
                
                table.addCell(dayCell);
            }
            
            // Lấy dữ liệu lịch làm việc từ database
            Map<Integer, Map<LocalDate, String>> scheduleData = getScheduleData(month, year);
            
            // Lấy danh sách nhân viên
            List<Map<String, Object>> employees = getEmployees();
            
            // Thêm dữ liệu cho từng nhân viên
            for (Map<String, Object> employee : employees) {
                int employeeId = (Integer) employee.get("MaNV");
                String employeeName = (String) employee.get("HoTen");
                
                // Tên nhân viên
                PdfPCell empCell = new PdfPCell(new Phrase(employeeName, normalFont));
                empCell.setHorizontalAlignment(Element.ALIGN_LEFT);
                empCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                empCell.setPadding(5);
                table.addCell(empCell);
                
                // Thêm lịch làm việc của nhân viên
                Map<LocalDate, String> employeeSchedule = scheduleData.getOrDefault(employeeId, new HashMap<>());
                
                for (int i = 1; i <= daysInMonth; i++) {
                    LocalDate currentDate = LocalDate.of(year, month, i);
                    
                    // Nếu có ca làm việc vào ngày này
                    String shiftInfo = employeeSchedule.get(currentDate);
                    PdfPCell cell = new PdfPCell(new Phrase(shiftInfo != null ? shiftInfo : "", normalFont));
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    cell.setPadding(3);
                    
                    // Kiểm tra cuối tuần và áp dụng style khác
                    int dayOfWeek = currentDate.getDayOfWeek().getValue();
                    if (dayOfWeek == 6 || dayOfWeek == 7) { // Thứ 7 hoặc Chủ nhật
                        cell.setBackgroundColor(WEEKEND_COLOR);
                    }
                    
                    table.addCell(cell);
                }
            }
            
            document.add(table);
            
            // Thêm chú thích và thông tin
            Paragraph footer = new Paragraph();
            footer.setSpacingBefore(15);
            footer.add(new Phrase("Chú thích: ", headerFont));
            footer.add(new Phrase("Các ô trống là ngày nghỉ. Thông tin hiển thị trong mỗi ô là tên ca và giờ làm việc.\n", normalFont));
            footer.add(new Phrase("Ngày xuất báo cáo: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), normalFont));
            document.add(footer);
            
            document.close();
            return true;
            
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
}