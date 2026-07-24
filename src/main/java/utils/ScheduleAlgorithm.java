package utils;

import dao.CaDAO;
import dao.LichLVDAO;
import dao.NhanVienDAO;
import dao.ViTriCVDAO;
import dao.YeuCauDLDAO;
import models.Ca;
import models.LichLV;
import models.NhanVien;
import models.ViTriCV;
import models.YeuCauDL;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Triển khai thuật toán sắp xếp lịch làm việc: Tham lam (Greedy) và Heuristic
 * với các cải tiến cho việc sắp lịch quản lý
 */
public class ScheduleAlgorithm {
    
    private final NhanVienDAO nhanVienDAO;
    private final CaDAO caDAO;
    private final LichLVDAO lichLVDAO;
    private final ViTriCVDAO viTriCVDAO;
    private final YeuCauDLDAO yeuCauDLDAO;
    
    // Lưu trữ thông tin vị trí công việc 
    private Map<Integer, String> bangViTriCV;
    
    // Hằng số cho các vị trí công việc
    private static final int VI_TRI_QUAN_LY = 1;
    private static final int VI_TRI_DAU_BEP = 2;
    private static final int VI_TRI_DAU_BEP_PHU = 3;
    private static final int VI_TRI_THU_NGAN = 4;
    private static final int VI_TRI_PHUC_VU = 5;
    private static final int VI_TRI_BAO_VE = 6;
    
    // Hằng số cho yêu cầu nhân viên theo ca
    private static final int PHUC_VU_CA_SANG = 1;
    private static final int PHUC_VU_CA_CHIEU = 1;
    private static final int PHUC_VU_CA_TOI = 2;
    private static final int DAU_BEP_CA_TOI = 1;
    private static final int DAU_BEP_PHU_CA_TOI = 2;
    private static final int BAO_VE_CA_TOI = 2;
   
    private static final int SO_CA_TOI_DA_MOI_THANG = 60; // Số ca tối đa mỗi nhân viên mỗi tháng
    private static final int SO_CA_TOI_DA_MOI_NGAY = 2;   // Tối đa 2 ca mỗi ngày
    private static final int SO_NGAY_LIEN_TIEP_TOI_DA = 6;    // Số ngày làm việc liên tiếp tối đa
    
    // Giới hạn riêng cho quản lý
    private static final int SO_CA_TOI_DA_QUAN_LY = 70; // Quản lý có thể làm nhiều hơn 10 ca so với nhân viên thường
    private static final int SO_CA_TOI_DA_MOI_NGAY_QUAN_LY = 3; // Quản lý có thể làm tối đa 3 ca/ngày
    private static final int SO_QUAN_LY_TOI_THIEU_MOI_CA = 1; // Ít nhất 1 quản lý mỗi ca
    
    public ScheduleAlgorithm() {
        nhanVienDAO = new NhanVienDAO();
        caDAO = new CaDAO();
        lichLVDAO = new LichLVDAO();
        viTriCVDAO = new ViTriCVDAO();
        yeuCauDLDAO = new YeuCauDLDAO();
        loadPositionMap();
    }
    
    private void loadPositionMap() {
        bangViTriCV = new HashMap<>();
        for (ViTriCV viTri : viTriCVDAO.getAllViTriCV()) {
            bangViTriCV.put(viTri.getMaVT(), viTri.getTenVT());
        }
    }
    
    /**
     * Kiểm tra xem một tháng đã có lịch làm việc hay chưa
     * @param thang Tháng (1-12)
     * @param nam Năm
     * @return true nếu tháng đã có lịch làm việc, false nếu chưa
     */
    public boolean kiemTraThangCoLichLamViec(int thang, int nam) {
        YearMonth namThang = YearMonth.of(nam, thang);
        LocalDate ngayBatDau = namThang.atDay(1);
        LocalDate ngayKetThuc = namThang.atEndOfMonth();
        
        List<LichLV> lichHienCo = lichLVDAO.getLichLVByDateRange(ngayBatDau, ngayKetThuc);
        return !lichHienCo.isEmpty();
    }
    
    /**
     * Tạo lịch làm việc ban đầu cho một tháng sử dụng thuật toán Tham lam
     * với cải tiến cho quản lý
     * @param thang Tháng (1-12)
     * @param nam Năm
     * @return true nếu việc tạo lịch thành công, false nếu có lỗi
     */
    
    /**
    public boolean taoLichBangThuatToanThamLam(int thang, int nam) {
        if (kiemTraThangCoLichLamViec(thang, nam)) {
            System.out.println("Tháng " + thang + "/" + nam + " đã có lịch làm việc.");
            return true;
        }

        // Xóa lịch cũ nếu có
        lichLVDAO.deleteAllLichLVForMonth(thang, nam);

        // Lấy danh sách nhân viên
        List<NhanVien> allEmployees = nhanVienDAO.getActiveNhanVien();
        
        // Tách quản lý và nhân viên thường
        List<NhanVien> managers = allEmployees.stream()
            .filter(e -> e.getMaVT() == VI_TRI_QUAN_LY)
            .collect(Collectors.toList());
        List<NhanVien> regularEmployees = allEmployees.stream()
            .filter(e -> e.getMaVT() != VI_TRI_QUAN_LY)
            .collect(Collectors.toList());

        // Xử lý trường hợp không có quản lý
        if (managers.isEmpty()) {
            System.out.println("Cảnh báo: Không có quản lý nào trong hệ thống!");
            return false;
        }

        // Lấy danh sách ca làm việc
        List<Ca> allShifts = caDAO.getAllCa();

        // Xác định số ngày trong tháng
        YearMonth yearMonth = YearMonth.of(nam, thang);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        // Lấy danh sách nhân viên có yêu cầu nghỉ phép
        List<YeuCauDL> approvedLeaves = yeuCauDLDAO.getApprovedLeaveRequestsForMonth(thang, nam);

        // Bộ đếm để đảm bảo công bằng
        Map<Integer, Integer> employeeShiftCount = new HashMap<>(); // Tổng số ca mỗi nhân viên
        Map<Integer, Integer> morningShiftCount = new HashMap<>(); // Số ca sáng
        Map<Integer, Integer> afternoonShiftCount = new HashMap<>(); // Số ca chiều
        Map<Integer, Integer> eveningShiftCount = new HashMap<>(); // Số ca tối
        Map<Integer, Integer> consecutiveDays = new HashMap<>(); // Số ngày làm việc liên tiếp

        // Khởi tạo bộ đếm
        for (NhanVien e : allEmployees) {
            employeeShiftCount.put(e.getMaNV(), 0);
            morningShiftCount.put(e.getMaNV(), 0);
            afternoonShiftCount.put(e.getMaNV(), 0);
            eveningShiftCount.put(e.getMaNV(), 0);
            consecutiveDays.put(e.getMaNV(), 0);
        }

        // Thống kê để hiển thị tỉ lệ trùng lặp
        Map<Integer, Map<Integer, Integer>> shiftPatternCount = new HashMap<>();

        // Duyệt từng ngày trong tháng
        for (LocalDate currentDate = startDate; !currentDate.isAfter(endDate); currentDate = currentDate.plusDays(1)) {
            DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
            boolean isWeekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;

            // Duyệt từng ca làm việc
            for (Ca shift : allShifts) {
                int shiftType = shift.getMaCa();
                Map<Integer, Integer> requiredStaff = determineRequiredStaff(shift, currentDate);

                // Phân công quản lý với logic mới
                assignManagersWithNewLogic(managers, shiftType, currentDate, employeeShiftCount, 
                    morningShiftCount, afternoonShiftCount, eveningShiftCount, 
                    consecutiveDays, approvedLeaves, startDate, endDate);

                // Duyệt từng vị trí cần nhân viên (trừ quản lý đã xử lý)
                for (Map.Entry<Integer, Integer> entry : requiredStaff.entrySet()) {
                    int position = entry.getKey();
                    int required = entry.getValue();
                    int assigned = 0;

                    // Bỏ qua vị trí quản lý đã xử lý
                    if (position == VI_TRI_QUAN_LY) continue;

                    // Lọc nhân viên theo vị trí
                    List<NhanVien> candidates = regularEmployees.stream()
                        .filter(e -> e.getMaVT() == position)
                        .collect(Collectors.toList());

                    // Sắp xếp nhân viên theo số ca đã làm (ưu tiên người ít ca hơn)
                    candidates.sort(Comparator.comparingInt(e -> employeeShiftCount.get(e.getMaNV())));

                    // Phân công nhân viên
                    for (NhanVien emp : candidates) {
                        if (assigned >= required) break;

                        int empId = emp.getMaNV();
                        boolean canAssign = canAssignShift(emp, shiftType, currentDate, 
                            employeeShiftCount, morningShiftCount, afternoonShiftCount, 
                            eveningShiftCount, consecutiveDays, approvedLeaves);

                        if (canAssign) {
                            LichLV schedule = new LichLV();
                            schedule.setMaNV(empId);
                            schedule.setMaCa(shiftType);
                            schedule.setNgayLam(currentDate);

                            if (lichLVDAO.insertLichLV(schedule)) {
                                // Cập nhật bộ đếm
                                employeeShiftCount.put(empId, employeeShiftCount.get(empId) + 1);
                                consecutiveDays.put(empId, consecutiveDays.get(empId) + 1);
                                
                                switch (shiftType) {
                                    case 1: morningShiftCount.put(empId, morningShiftCount.get(empId) + 1); break;
                                    case 2: afternoonShiftCount.put(empId, afternoonShiftCount.get(empId) + 1); break;
                                    case 3: eveningShiftCount.put(empId, eveningShiftCount.get(empId) + 1); break;
                                }
                                
                                // Cập nhật thống kê pattern
                                shiftPatternCount.computeIfAbsent(empId, k -> new HashMap<>())
                                    .merge(shiftType, 1, Integer::sum);
                                
                                assigned++;
                            }
                        }
                    }

                    // Nếu không đủ nhân viên, thử phân công nhân viên làm thêm ca
                    if (assigned < required) {
                        System.out.println("Cảnh báo: Không đủ nhân viên cho vị trí " + bangViTriCV.get(position) 
                            + " vào ngày " + currentDate + " ca " + shift.getTenCa());
                        
                        // Thử phân công nhân viên từ vị trí tương thích
                        List<Integer> compatiblePositions = getCompatiblePositions(position);
                        for (int compPos : compatiblePositions) {
                            if (compPos == position) continue;
                            
                            List<NhanVien> compCandidates = regularEmployees.stream()
                                .filter(e -> e.getMaVT() == compPos)
                                .collect(Collectors.toList());
                            
                            compCandidates.sort(Comparator.comparingInt(e -> employeeShiftCount.get(e.getMaNV())));
                            
                            for (NhanVien emp : compCandidates) {
                                if (assigned >= required) break;
                                
                                int empId = emp.getMaNV();
                                boolean canAssign = canAssignShift(emp, shiftType, currentDate, 
                                    employeeShiftCount, morningShiftCount, afternoonShiftCount, 
                                    eveningShiftCount, consecutiveDays, approvedLeaves);
                                
                                if (canAssign) {
                                    LichLV schedule = new LichLV();
                                    schedule.setMaNV(empId);
                                    schedule.setMaCa(shiftType);
                                    schedule.setNgayLam(currentDate);

                                    if (lichLVDAO.insertLichLV(schedule)) {
                                        employeeShiftCount.put(empId, employeeShiftCount.get(empId) + 1);
                                        consecutiveDays.put(empId, consecutiveDays.get(empId) + 1);
                                        
                                        switch (shiftType) {
                                            case 1: morningShiftCount.put(empId, morningShiftCount.get(empId) + 1); break;
                                            case 2: afternoonShiftCount.put(empId, afternoonShiftCount.get(empId) + 1); break;
                                            case 3: eveningShiftCount.put(empId, eveningShiftCount.get(empId) + 1); break;
                                        }
                                        
                                        assigned++;
                                        System.out.println("Đã phân công " + emp.getHoTen() + " từ vị trí " 
                                            + bangViTriCV.get(compPos) + " sang làm " + bangViTriCV.get(position));
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Reset ngày làm liên tiếp nếu nhân viên không làm việc ngày hôm nay
            for (NhanVien e : allEmployees) {
                if (!isEmployeeScheduledOnDate(e.getMaNV(), currentDate)) {
                    consecutiveDays.put(e.getMaNV(), 0);
                }
            }
        }

        // Hiển thị thống kê trùng lặp lịch làm việc
        displayShiftPatternStatistics(shiftPatternCount, allEmployees);
        
        return true;
    }
    **/
    
//    /**
// * Tạo lịch làm việc ban đầu cho một tháng sử dụng thuật toán Tham lam
// * và hiển thị bảng đánh giá cho nhiều kích thước nhân viên
// * @param thang Tháng (1-12)
// * @param nam Năm
// * @return true nếu tạo lịch thành công, false nếu có lỗi
// */
//public boolean taoLichBangThuatToanThamLam(int thang, int nam) {
//    // Danh sách kích thước nhân viên để kiểm tra
//    int[] employeeSizes = {10, 50, 100, 200};
//    List<Map<String, Double>> allResults = new ArrayList<>();
//    
//    for (int size : employeeSizes) {
//        long startTime = System.currentTimeMillis();
//        
//        // Giả lập số nhân viên bằng cách giới hạn danh sách
//        List<NhanVien> allEmployees = nhanVienDAO.getActiveNhanVien();
//        if (allEmployees.size() < size) {
//            System.out.println("Không đủ nhân viên để kiểm tra kích thước " + size);
//            continue;
//        }
//        List<NhanVien> limitedEmployees = allEmployees.subList(0, size);
//        
//        // Xóa lịch cũ
//        lichLVDAO.deleteAllLichLVForMonth(thang, nam);
//        
//        // Tách quản lý và nhân viên thường
//        List<NhanVien> managers = limitedEmployees.stream()
//            .filter(e -> e.getMaVT() == VI_TRI_QUAN_LY)
//            .collect(Collectors.toList());
//        List<NhanVien> regularEmployees = limitedEmployees.stream()
//            .filter(e -> e.getMaVT() != VI_TRI_QUAN_LY)
//            .collect(Collectors.toList());
//        
//        if (managers.isEmpty()) {
//            System.out.println("Không có quản lý cho kích thước " + size);
//            continue;
//        }
//        
//        // Logic xếp lịch (giữ nguyên từ code gốc)
//        List<Ca> allShifts = caDAO.getAllCa();
//        YearMonth yearMonth = YearMonth.of(nam, thang);
//        LocalDate startDate = yearMonth.atDay(1);
//        LocalDate endDate = yearMonth.atEndOfMonth();
//        List<YeuCauDL> approvedLeaves = yeuCauDLDAO.getApprovedLeaveRequestsForMonth(thang, nam);
//        
//        Map<Integer, Integer> employeeShiftCount = new HashMap<>();
//        Map<Integer, Integer> morningShiftCount = new HashMap<>();
//        Map<Integer, Integer> afternoonShiftCount = new HashMap<>();
//        Map<Integer, Integer> eveningShiftCount = new HashMap<>();
//        Map<Integer, Integer> consecutiveDays = new HashMap<>();
//        
//        for (NhanVien e : limitedEmployees) {
//            employeeShiftCount.put(e.getMaNV(), 0);
//            morningShiftCount.put(e.getMaNV(), 0);
//            afternoonShiftCount.put(e.getMaNV(), 0);
//            eveningShiftCount.put(e.getMaNV(), 0);
//            consecutiveDays.put(e.getMaNV(), 0);
//        }
//        
//        for (LocalDate currentDate = startDate; !currentDate.isAfter(endDate); currentDate = currentDate.plusDays(1)) {
//            for (Ca shift : allShifts) {
//                int shiftType = shift.getMaCa();
//                Map<Integer, Integer> requiredStaff = determineRequiredStaff(shift, currentDate);
//                
//                assignManagersWithNewLogic(managers, shiftType, currentDate, employeeShiftCount, 
//                    morningShiftCount, afternoonShiftCount, eveningShiftCount, 
//                    consecutiveDays, approvedLeaves, startDate, endDate);
//                
//                for (Map.Entry<Integer, Integer> entry : requiredStaff.entrySet()) {
//                    int position = entry.getKey();
//                    int required = entry.getValue();
//                    int assigned = 0;
//                    
//                    if (position == VI_TRI_QUAN_LY) continue;
//                    
//                    List<NhanVien> candidates = regularEmployees.stream()
//                        .filter(e -> e.getMaVT() == position)
//                        .collect(Collectors.toList());
//                    
//                    candidates.sort(Comparator.comparingInt(e -> employeeShiftCount.get(e.getMaNV())));
//                    
//                    for (NhanVien emp : candidates) {
//                        if (assigned >= required) break;
//                        
//                        int empId = emp.getMaNV();
//                        boolean canAssign = canAssignShift(emp, shiftType, currentDate, 
//                            employeeShiftCount, morningShiftCount, afternoonShiftCount, 
//                            eveningShiftCount, consecutiveDays, approvedLeaves);
//                        
//                        if (canAssign) {
//                            LichLV schedule = new LichLV();
//                            schedule.setMaNV(empId);
//                            schedule.setMaCa(shiftType);
//                            schedule.setNgayLam(currentDate);
//                            
//                            if (lichLVDAO.insertLichLV(schedule)) {
//                                employeeShiftCount.put(empId, employeeShiftCount.get(empId) + 1);
//                                consecutiveDays.put(empId, consecutiveDays.get(empId) + 1);
//                                switch (shiftType) {
//                                    case 1: morningShiftCount.put(empId, morningShiftCount.get(empId) + 1); break;
//                                    case 2: afternoonShiftCount.put(empId, afternoonShiftCount.get(empId) + 1); break;
//                                    case 3: eveningShiftCount.put(empId, eveningShiftCount.get(empId) + 1); break;
//                                }
//                                assigned++;
//                            }
//                        }
//                    }
//                }
//            }
//        }
//        
//        // Đo thời gian chạy
//        long endTime = System.currentTimeMillis();
//        double runtimeSeconds = (endTime - startTime) / 1000.0;
//        
//        // Đánh giá và lưu kết quả
//        ScheduleEvaluator evaluator = new ScheduleEvaluator(lichLVDAO, nhanVienDAO);
//        Map<String, Double> result = evaluator.evaluateScheduleForTable(thang, nam, runtimeSeconds);
//        allResults.add(result);
//    }
//    
//    // Hiển thị bảng kết quả
//    displayResultTable(allResults);
//    return true;
//}

    /**
 * Tạo lịch làm việc ban đầu cho một tháng sử dụng thuật toán Tham lam
 * @param thang Tháng (1-12)
 * @param nam Năm
 * @return true nếu tạo lịch thành công, false nếu có lỗi
 */
public boolean taoLichBangThuatToanThamLam(int thang, int nam) {
    long startTime = System.currentTimeMillis(); // Bắt đầu đo thời gian

    // Kiểm tra xem tháng đã có lịch chưa
    if (kiemTraThangCoLichLamViec(thang, nam)) {
        System.out.println("Tháng " + thang + "/" + nam + " đã có lịch làm việc.");
        return true;
    }

    // Xóa lịch cũ nếu có
    lichLVDAO.deleteAllLichLVForMonth(thang, nam);

    // Lấy danh sách nhân viên từ CSDL
    List<NhanVien> allEmployees = nhanVienDAO.getActiveNhanVien();

    // Kiểm tra nếu không có nhân viên
    if (allEmployees == null || allEmployees.isEmpty()) {
        System.out.println("Không có nhân viên hoạt động trong hệ thống.");
        return false;
    }

    // Tách quản lý và nhân viên thường
    List<NhanVien> managers = allEmployees.stream()
        .filter(e -> e.getMaVT() == VI_TRI_QUAN_LY)
        .collect(Collectors.toList());
    List<NhanVien> regularEmployees = allEmployees.stream()
        .filter(e -> e.getMaVT() != VI_TRI_QUAN_LY)
        .collect(Collectors.toList());

    // Kiểm tra nếu không có quản lý
    if (managers.isEmpty()) {
        System.out.println("Cảnh báo: Không có quản lý nào trong hệ thống!");
        return false;
    }

    // Lấy danh sách ca làm việc
    List<Ca> allShifts = caDAO.getAllCa();

    // Xác định số ngày trong tháng
    YearMonth yearMonth = YearMonth.of(nam, thang);
    LocalDate startDate = yearMonth.atDay(1);
    LocalDate endDate = yearMonth.atEndOfMonth();

    // Lấy danh sách yêu cầu nghỉ phép đã phê duyệt
    List<YeuCauDL> approvedLeaves = yeuCauDLDAO.getApprovedLeaveRequestsForMonth(thang, nam);

    // Khởi tạo bộ đếm để đảm bảo công bằng
    Map<Integer, Integer> employeeShiftCount = new HashMap<>(); // Tổng số ca
    Map<Integer, Integer> morningShiftCount = new HashMap<>();  // Ca sáng
    Map<Integer, Integer> afternoonShiftCount = new HashMap<>(); // Ca chiều
    Map<Integer, Integer> eveningShiftCount = new HashMap<>();  // Ca tối
    Map<Integer, Integer> consecutiveDays = new HashMap<>();    // Ngày làm liên tiếp

    for (NhanVien e : allEmployees) {
        employeeShiftCount.put(e.getMaNV(), 0);
        morningShiftCount.put(e.getMaNV(), 0);
        afternoonShiftCount.put(e.getMaNV(), 0);
        eveningShiftCount.put(e.getMaNV(), 0);
        consecutiveDays.put(e.getMaNV(), 0);
    }

    // Thống kê mẫu ca để đánh giá trùng lặp
    Map<Integer, Map<Integer, Integer>> shiftPatternCount = new HashMap<>();

    // Duyệt từng ngày trong tháng
    for (LocalDate currentDate = startDate; !currentDate.isAfter(endDate); currentDate = currentDate.plusDays(1)) {
        DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
        boolean isWeekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;

        // Duyệt từng ca làm việc
        for (Ca shift : allShifts) {
            int shiftType = shift.getMaCa();
            Map<Integer, Integer> requiredStaff = determineRequiredStaff(shift, currentDate);

            // Phân công quản lý
            assignManagersWithNewLogic(managers, shiftType, currentDate, employeeShiftCount,
                morningShiftCount, afternoonShiftCount, eveningShiftCount,
                consecutiveDays, approvedLeaves, startDate, endDate);

            // Phân công nhân viên thường
            for (Map.Entry<Integer, Integer> entry : requiredStaff.entrySet()) {
                int position = entry.getKey();
                int required = entry.getValue();
                int assigned = 0;

                if (position == VI_TRI_QUAN_LY) continue; // Bỏ qua quản lý đã xử lý

                // Lọc nhân viên theo vị trí
                List<NhanVien> candidates = regularEmployees.stream()
                    .filter(e -> e.getMaVT() == position)
                    .collect(Collectors.toList());

                // Sắp xếp theo số ca đã làm (ưu tiên ít ca hơn)
                candidates.sort(Comparator.comparingInt(e -> employeeShiftCount.get(e.getMaNV())));

                // Phân công nhân viên
                for (NhanVien emp : candidates) {
                    if (assigned >= required) break;

                    int empId = emp.getMaNV();
                    boolean canAssign = canAssignShift(emp, shiftType, currentDate,
                        employeeShiftCount, morningShiftCount, afternoonShiftCount,
                        eveningShiftCount, consecutiveDays, approvedLeaves);

                    if (canAssign) {
                        LichLV schedule = new LichLV();
                        schedule.setMaNV(empId);
                        schedule.setMaCa(shiftType);
                        schedule.setNgayLam(currentDate);

                        if (lichLVDAO.insertLichLV(schedule)) {
                            employeeShiftCount.put(empId, employeeShiftCount.get(empId) + 1);
                            consecutiveDays.put(empId, consecutiveDays.get(empId) + 1);

                            switch (shiftType) {
                                case 1: morningShiftCount.put(empId, morningShiftCount.get(empId) + 1); break;
                                case 2: afternoonShiftCount.put(empId, afternoonShiftCount.get(empId) + 1); break;
                                case 3: eveningShiftCount.put(empId, eveningShiftCount.get(empId) + 1); break;
                            }

                            // Cập nhật thống kê mẫu ca
                            shiftPatternCount.computeIfAbsent(empId, k -> new HashMap<>())
                                .merge(shiftType, 1, Integer::sum);

                            assigned++;
                        }
                    }
                }

                // Xử lý trường hợp không đủ nhân viên
                if (assigned < required) {
                    System.out.println("Cảnh báo: Không đủ nhân viên cho vị trí " + bangViTriCV.get(position)
                        + " vào ngày " + currentDate + " ca " + shift.getTenCa());
                }
            }
        }

        // Reset ngày làm liên tiếp nếu không làm hôm nay
        for (NhanVien e : allEmployees) {
            if (!isEmployeeScheduledOnDate(e.getMaNV(), currentDate)) {
                consecutiveDays.put(e.getMaNV(), 0);
            }
        }
    }

    // Hiển thị thống kê trùng lặp
    displayShiftPatternStatistics(shiftPatternCount, allEmployees);

    // Đánh giá thuật toán
    long endTime = System.currentTimeMillis();
    double runtimeSeconds = (endTime - startTime) / 1000.0;
    System.out.println("\n=== ĐÁNH GIÁ THUẬT TOÁN GREEDY ===");
    System.out.println("Số nhân viên: " + allEmployees.size());
    System.out.println("Thời gian chạy thuật toán: " + String.format("%.2f", runtimeSeconds) + " giây");

    // Đánh giá lịch làm việc
    ScheduleEvaluator evaluator = new ScheduleEvaluator(lichLVDAO, nhanVienDAO);
    Map<String, Double> evaluationResults = evaluator.evaluateScheduleForTable(thang, nam, runtimeSeconds);

    return true;
}
    
/**
 * Hiển thị bảng kết quả lên console
 * @param results Danh sách kết quả đánh giá
 */
private void displayResultTable(List<Map<String, Double>> results) {
    String[] headers = {"Số nhân viên", "Thời gian chờ (s)", "Tỉ lệ trung lập (%)", "Entropy", "Tần suất lập", "Độ lệch chuẩn"};
    System.out.println("\n=== BẢNG ĐÁNH GIÁ HIỆU SUẤT ===");
    
    // In tiêu đề
    for (String header : headers) {
        System.out.printf("%-20s", header);
    }
    System.out.println();
    
    // In đường kẻ
    for (int i = 0; i < headers.length * 20; i++) {
        System.out.print("-");
    }
    System.out.println();
    
    // In dữ liệu
    for (Map<String, Double> result : results) {
        System.out.printf("%-20.0f", result.get("Số nhân viên"));
        System.out.printf("%-20.1f", result.get("Thời gian chờ (s)"));
        System.out.printf("%-20.1f", result.get("Tỉ lệ trung lập (%)"));
        System.out.printf("%-20.2f", result.get("Entropy"));
        System.out.printf("%-20.0f", result.get("Tần suất lập"));
        System.out.printf("%-20.1f", result.get("Độ lệch chuẩn"));
        System.out.println();
    }
}

    /**
     * Phân công quản lý với logic mới:
     * - Nếu chỉ có 1 quản lý: phân bố đều các ca trong tháng, không cần có mặt tất cả các ca
     * - Nếu có nhiều quản lý: phân bố đều cho các quản lý
     */
    private void assignManagersWithNewLogic(List<NhanVien> managers, int shiftType, LocalDate date,
            Map<Integer, Integer> employeeShiftCount, 
            Map<Integer, Integer> morningShiftCount,
            Map<Integer, Integer> afternoonShiftCount,
            Map<Integer, Integer> eveningShiftCount,
            Map<Integer, Integer> consecutiveDays,
            List<YeuCauDL> approvedLeaves,
            LocalDate startDate, LocalDate endDate) {
        
        // Tính số ngày trong tháng
        int daysInMonth = endDate.getDayOfMonth();
        
        // Nếu chỉ có 1 quản lý
        if (managers.size() == 1) {
            NhanVien manager = managers.get(0);
            int managerId = manager.getMaNV();
            
            // Tính số ca mục tiêu cho quản lý (phân bố đều trong tháng)
            int targetShifts = SO_CA_TOI_DA_QUAN_LY * daysInMonth / (daysInMonth * 3); // ~1/3 số ca
            
            // Kiểm tra nếu đã đủ số ca mục tiêu
            if (employeeShiftCount.get(managerId) >= targetShifts) {
                return;
            }
            
            // Kiểm tra điều kiện phân công
            boolean canAssign = canAssignManagerShift(manager, shiftType, date, 
                employeeShiftCount, morningShiftCount, afternoonShiftCount, 
                eveningShiftCount, consecutiveDays, approvedLeaves);
            
            if (canAssign) {
                LichLV schedule = new LichLV();
                schedule.setMaNV(managerId);
                schedule.setMaCa(shiftType);
                schedule.setNgayLam(date);

                if (lichLVDAO.insertLichLV(schedule)) {
                    // Cập nhật bộ đếm
                    employeeShiftCount.put(managerId, employeeShiftCount.get(managerId) + 1);
                    consecutiveDays.put(managerId, consecutiveDays.get(managerId) + 1);
                    
                    switch (shiftType) {
                        case 1: morningShiftCount.put(managerId, morningShiftCount.get(managerId) + 1); break;
                        case 2: afternoonShiftCount.put(managerId, afternoonShiftCount.get(managerId) + 1); break;
                        case 3: eveningShiftCount.put(managerId, eveningShiftCount.get(managerId) + 1); break;
                    }
                }
            }
        } 
        // Nếu có nhiều quản lý
        else {
            // Sắp xếp quản lý theo số ca đã làm (ưu tiên người ít ca hơn)
            managers.sort(Comparator.comparingInt(m -> employeeShiftCount.get(m.getMaNV())));
            
            int assigned = 0;
            int required = SO_QUAN_LY_TOI_THIEU_MOI_CA;
            
            for (NhanVien manager : managers) {
                if (assigned >= required) break;
                
                int managerId = manager.getMaNV();
                boolean canAssign = canAssignManagerShift(manager, shiftType, date, 
                    employeeShiftCount, morningShiftCount, afternoonShiftCount, 
                    eveningShiftCount, consecutiveDays, approvedLeaves);
                
                if (canAssign) {
                    LichLV schedule = new LichLV();
                    schedule.setMaNV(managerId);
                    schedule.setMaCa(shiftType);
                    schedule.setNgayLam(date);

                    if (lichLVDAO.insertLichLV(schedule)) {
                        // Cập nhật bộ đếm
                        employeeShiftCount.put(managerId, employeeShiftCount.get(managerId) + 1);
                        consecutiveDays.put(managerId, consecutiveDays.get(managerId) + 1);
                        
                        switch (shiftType) {
                            case 1: morningShiftCount.put(managerId, morningShiftCount.get(managerId) + 1); break;
                            case 2: afternoonShiftCount.put(managerId, afternoonShiftCount.get(managerId) + 1); break;
                            case 3: eveningShiftCount.put(managerId, eveningShiftCount.get(managerId) + 1); break;
                        }
                        
                        assigned++;
                    }
                }
            }
            
            if (assigned < required) {
                System.out.println("Cảnh báo: Không đủ quản lý cho ca " + shiftType + " ngày " + date);
            }
        }
    }

    /**
     * Kiểm tra xem có thể phân công ca cho nhân viên không
     */
    private boolean canAssignShift(NhanVien employee, int shiftType, LocalDate date, 
            Map<Integer, Integer> employeeShiftCount, 
            Map<Integer, Integer> morningShiftCount,
            Map<Integer, Integer> afternoonShiftCount,
            Map<Integer, Integer> eveningShiftCount,
            Map<Integer, Integer> consecutiveDays,
            List<YeuCauDL> approvedLeaves) {
        
        int empId = employee.getMaNV();
        int maxShifts = employee.getMaVT() == VI_TRI_QUAN_LY ? SO_CA_TOI_DA_QUAN_LY : SO_CA_TOI_DA_MOI_THANG;
        int maxShiftsPerDay = employee.getMaVT() == VI_TRI_QUAN_LY ? SO_CA_TOI_DA_MOI_NGAY_QUAN_LY : SO_CA_TOI_DA_MOI_NGAY;
        
        // Kiểm tra số ca tối đa trong tháng
        if (employeeShiftCount.get(empId) >= maxShifts) {
            return false;
        }
        
        // Kiểm tra số ngày làm việc liên tiếp
        if (consecutiveDays.get(empId) >= SO_NGAY_LIEN_TIEP_TOI_DA) {
            return false;
        }
        
        // Kiểm tra nhân viên đã có ca nào trong ngày chưa
        if (isEmployeeScheduledOnDate(empId, date)) {
            if (lichLVDAO.countShiftsOnDate(empId, date) >= maxShiftsPerDay) {
                return false;
            }
        }
        
        // Kiểm tra yêu cầu nghỉ phép
        if (yeuCauDLDAO.hasApprovedLeaveRequestOnDate(empId, date)) {
            return false;
        }
        
        // Kiểm tra phân bổ ca hợp lý (tránh làm quá nhiều ca sáng/chiều/tối)
        int maxShiftsPerType = maxShifts / 3 + 2; // Cho phép chênh lệch 2 ca
        switch (shiftType) {
            case 1: 
                return morningShiftCount.get(empId) < maxShiftsPerType;
            case 2: 
                return afternoonShiftCount.get(empId) < maxShiftsPerType;
            case 3: 
                return eveningShiftCount.get(empId) < maxShiftsPerType;
            default:
                return true;
        }
    }
    
    /**
     * Kiểm tra xem có thể phân công ca cho quản lý không (có điều kiện riêng)
     */
    
    private boolean canAssignManagerShift(NhanVien manager, int shiftType, LocalDate date, 
        Map<Integer, Integer> employeeShiftCount, 
        Map<Integer, Integer> morningShiftCount,
        Map<Integer, Integer> afternoonShiftCount,
        Map<Integer, Integer> eveningShiftCount,
        Map<Integer, Integer> consecutiveDays,
        List<YeuCauDL> approvedLeaves) {
        
        int managerId = manager.getMaNV();
        
        // Kiểm tra các điều kiện cơ bản
        if (!canAssignShift(manager, shiftType, date, employeeShiftCount, 
            morningShiftCount, afternoonShiftCount, eveningShiftCount, 
            consecutiveDays, approvedLeaves)) {
            return false;
        }
        
        // Kiểm tra thêm: Quản lý không nên làm quá 3 ca liên tiếp cùng loại
        int recentSameShifts = countRecentSameShifts(managerId, shiftType, date.minusDays(3), date.minusDays(1));
        return recentSameShifts < 3;
    }
    
    /**
     * Điều chỉnh lịch làm việc bằng heuristic khi nhân viên yêu cầu nghỉ phép hoặc thay đổi ca
     * @param employeeId ID nhân viên
     * @param startDate Ngày bắt đầu nghỉ
     * @param endDate Ngày kết thúc nghỉ
     * @return true nếu điều chỉnh lịch thành công, false nếu không
     */
    public boolean adjustScheduleForLeave(int employeeId, LocalDate startDate, LocalDate endDate) {
        // Lấy các ca cần điều chỉnh
        List<LichLV> affectedShifts = lichLVDAO.getLichLVByDateRange(startDate, endDate).stream()
            .filter(l -> l.getMaNV() == employeeId)
            .collect(Collectors.toList());
        
        if (affectedShifts.isEmpty()) return true;

        // Lấy thông tin nhân viên
        NhanVien employee = nhanVienDAO.getNhanVienById(employeeId);
        if (employee == null) return false;
        
        int employeePosition = employee.getMaVT();
        
        // Nếu là quản lý, chỉ tìm quản lý khác thay thế
        if (employeePosition == VI_TRI_QUAN_LY) {
            List<NhanVien> otherManagers = nhanVienDAO.getActiveNhanVien().stream()
                .filter(e -> e.getMaVT() == VI_TRI_QUAN_LY && e.getMaNV() != employeeId)
                .collect(Collectors.toList());
            
            if (otherManagers.isEmpty()) {
                System.out.println("Không có quản lý khác để thay thế");
                return false;
            }
            
            boolean allReassigned = true;
            for (LichLV shift : affectedShifts) {
                boolean reassigned = false;
                for (NhanVien manager : otherManagers) {
                    if (tryReassignShift(shift, manager.getMaNV())) {
                        reassigned = true;
                        break;
                    }
                }
                if (!reassigned) {
                    allReassigned = false;
                    System.out.println("Không thể tìm quản lý thay thế cho ca " + shift.getMaCa() 
                        + " ngày " + shift.getNgayLam());
                }
            }
            return allReassigned;
        }
        
        // Xử lý nhân viên thường
        List<NhanVien> samePositionEmployees = nhanVienDAO.getActiveNhanVien().stream()
            .filter(e -> e.getMaVT() == employeePosition && e.getMaNV() != employeeId)
            .collect(Collectors.toList());
        
        // Sắp xếp theo số ca ít nhất
        samePositionEmployees.sort(Comparator.comparingInt(e -> 
            lichLVDAO.countEmployeeShiftsInMonth(e.getMaNV(), startDate.getMonthValue(), startDate.getYear())));
        
        boolean allReassigned = true;
        
        for (LichLV shift : affectedShifts) {
            boolean reassigned = false;
            
            // Thử tìm nhân viên cùng vị trí
            for (NhanVien replacement : samePositionEmployees) {
                if (tryReassignShift(shift, replacement.getMaNV())) {
                    reassigned = true;
                    break;
                }
            }
            
            // Nếu không tìm được, thử các vị trí tương thích
            if (!reassigned) {
                List<Integer> compatiblePositions = getCompatiblePositions(employeePosition);
                
                for (int position : compatiblePositions) {
                    if (position == employeePosition) continue;
                    
                    List<NhanVien> candidates = nhanVienDAO.getActiveNhanVien().stream()
                        .filter(e -> e.getMaVT() == position)
                        .sorted(Comparator.comparingInt(e -> 
                            lichLVDAO.countEmployeeShiftsInMonth(e.getMaNV(), startDate.getMonthValue(), startDate.getYear())))
                        .collect(Collectors.toList());
                    
                    for (NhanVien candidate : candidates) {
                        if (tryReassignShift(shift, candidate.getMaNV())) {
                            reassigned = true;
                            break;
                        }
                    }
                    
                    if (reassigned) break;
                }
            }
            
            if (!reassigned) {
                allReassigned = false;
                System.out.println("Không thể tìm người thay thế cho ca " + shift.getMaCa() 
                    + " ngày " + shift.getNgayLam());
            }
        }
        
        return allReassigned;
    }
    
    /**
     * Đổi ca làm việc giữa hai nhân viên
     * @param employeeId ID nhân viên yêu cầu đổi ca
     * @param shiftId ID ca cần đổi
     * @param swapWithEmployeeId ID nhân viên được đổi ca (không bắt buộc)
     * @return true nếu đổi ca thành công, false nếu thất bại
     */
    public boolean swapShifts(int employeeId, int shiftId, Integer swapWithEmployeeId) {
        LichLV shift = lichLVDAO.getLichLVById(shiftId);
        if (shift == null || shift.getMaNV() != employeeId) return false;
        
        LocalDate shiftDate = shift.getNgayLam();
        int shiftType = shift.getMaCa();
        
        NhanVien employee = nhanVienDAO.getNhanVienById(employeeId);
        if (employee == null) return false;
        
        int employeePosition = employee.getMaVT();
        
        // Nếu có chỉ định nhân viên đổi
        if (swapWithEmployeeId != null) {
            NhanVien swapPartner = nhanVienDAO.getNhanVienById(swapWithEmployeeId);
            if (swapPartner == null || !isPositionCompatible(employeePosition, swapPartner.getMaVT())) {
                return false;
            }
            
            // Tìm ca của partner để đổi
            List<LichLV> partnerShifts = lichLVDAO.getLichLVByEmployeeAndDateRange(
                swapWithEmployeeId, 
                shiftDate.minusDays(3), 
                shiftDate.plusDays(3)
            );
            
            if (!partnerShifts.isEmpty()) {
                LichLV partnerShift = partnerShifts.get(0);
                
                // Thực hiện đổi
                int temp = shift.getMaNV();
                shift.setMaNV(partnerShift.getMaNV());
                partnerShift.setMaNV(temp);
                
                return lichLVDAO.updateLichLV(shift) && lichLVDAO.updateLichLV(partnerShift);
            }
            
            return false;
        } 
        // Tự động tìm partner phù hợp
        else {
            List<Integer> compatiblePositions = getCompatiblePositions(employeePosition);
            List<NhanVien> candidates = nhanVienDAO.getActiveNhanVien().stream()
                .filter(e -> compatiblePositions.contains(e.getMaVT()) && e.getMaNV() != employeeId)
                .sorted(Comparator.comparingInt(e -> 
                    lichLVDAO.countEmployeeShiftsInMonth(e.getMaNV(), shiftDate.getMonthValue(), shiftDate.getYear())))
                .collect(Collectors.toList());
            
            for (NhanVien candidate : candidates) {
                // Tìm ca của candidate để đổi
                List<LichLV> candidateShifts = lichLVDAO.getLichLVByEmployeeAndDateRange(
                    candidate.getMaNV(), 
                    shiftDate.minusDays(3), 
                    shiftDate.plusDays(3)
                );
                
                for (LichLV candidateShift : candidateShifts) {
                    // Thực hiện đổi
                    int temp = shift.getMaNV();
                    shift.setMaNV(candidateShift.getMaNV());
                    candidateShift.setMaNV(temp);
                    
                    if (lichLVDAO.updateLichLV(shift) && lichLVDAO.updateLichLV(candidateShift)) {
                        return true;
                    }
                }
            }
            
            return false;
        }
    }
    
    /**
     * Xác định yêu cầu nhân sự cho từng ca
     */
    private Map<Integer, Integer> determineRequiredStaff(Ca shift, LocalDate date) {
        Map<Integer, Integer> requirements = new HashMap<>();
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        boolean isWeekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;

        switch (shift.getMaCa()) {
            case 1: // Ca sáng (6h-12h)
                requirements.put(VI_TRI_QUAN_LY, 1); // Ít nhất 1 quản lý
                requirements.put(VI_TRI_DAU_BEP, 1);
                requirements.put(VI_TRI_DAU_BEP_PHU, isWeekend ? 2 : 1);
                requirements.put(VI_TRI_THU_NGAN, 1);
                requirements.put(VI_TRI_PHUC_VU, isWeekend ? 2 : 1);
                requirements.put(VI_TRI_BAO_VE, 1);
                break;
            case 2: // Ca chiều (13h-17h)
                requirements.put(VI_TRI_QUAN_LY, 1); // Ít nhất 1 quản lý
                requirements.put(VI_TRI_DAU_BEP, 1);
                requirements.put(VI_TRI_DAU_BEP_PHU, isWeekend ? 2 : 1);
                requirements.put(VI_TRI_THU_NGAN, 1);
                requirements.put(VI_TRI_PHUC_VU, isWeekend ? 2 : 1);
                requirements.put(VI_TRI_BAO_VE, 1);
                break;
            case 3: // Ca tối (18h-22h)
                requirements.put(VI_TRI_QUAN_LY, 1); // Ít nhất 1 quản lý
                requirements.put(VI_TRI_DAU_BEP, 1);
                requirements.put(VI_TRI_DAU_BEP_PHU, 2);
                requirements.put(VI_TRI_THU_NGAN, 1);
                requirements.put(VI_TRI_PHUC_VU, 2);
                requirements.put(VI_TRI_BAO_VE, 2);
                break;
        }
        
        return requirements;
    }
    
    /**
     * Thử phân công lại ca làm việc cho nhân viên khác
     */
    private boolean tryReassignShift(LichLV shift, int newEmployeeId) {
        // Kiểm tra điều kiện
        if (isEmployeeScheduledOnDate(newEmployeeId, shift.getNgayLam())) {
            NhanVien employee = nhanVienDAO.getNhanVienById(newEmployeeId);
            int maxShiftsPerDay = (employee != null && employee.getMaVT() == VI_TRI_QUAN_LY) 
                ? SO_CA_TOI_DA_MOI_NGAY_QUAN_LY : SO_CA_TOI_DA_MOI_NGAY;
            
            if (lichLVDAO.countShiftsOnDate(newEmployeeId, shift.getNgayLam()) >= maxShiftsPerDay) {
                return false;
            }
        }
        
        if (yeuCauDLDAO.hasApprovedLeaveRequestOnDate(newEmployeeId, shift.getNgayLam())) {
            return false;
        }
        
        // Thực hiện đổi
        shift.setMaNV(newEmployeeId);
        return lichLVDAO.updateLichLV(shift);
    }

    /**
     * Đếm số ca cùng loại trong khoảng thời gian gần đây
     */
    private int countRecentSameShifts(int employeeId, int shiftType, LocalDate startDate, LocalDate endDate) {
        List<LichLV> shifts = lichLVDAO.getLichLVByEmployeeAndDateRange(employeeId, startDate, endDate);
        return (int) shifts.stream()
            .filter(s -> s.getMaCa() == shiftType)
            .count();
    }
    
    /**
     * Hiển thị thống kê trùng lặp lịch làm việc
     */
    private void displayShiftPatternStatistics(Map<Integer, Map<Integer, Integer>> shiftPatternCount, 
            List<NhanVien> employees) {
        
        System.out.println("\n=== THỐNG KÊ TRÙNG LẶP LỊCH LÀM VIỆC ===");
        
        // Hiển thị thống kê cho quản lý trước
        List<NhanVien> managers = employees.stream()
            .filter(e -> e.getMaVT() == VI_TRI_QUAN_LY)
            .collect(Collectors.toList());
        
        if (!managers.isEmpty()) {
            System.out.println("\n=== THỐNG KÊ QUẢN LÝ ===");
            for (NhanVien manager : managers) {
                displayEmployeeStatistics(manager, shiftPatternCount.getOrDefault(manager.getMaNV(), new HashMap<>()));
            }
        }
        
        // Hiển thị thống kê cho nhân viên thường
        List<NhanVien> regularEmployees = employees.stream()
            .filter(e -> e.getMaVT() != VI_TRI_QUAN_LY)
            .collect(Collectors.toList());
        
        if (!regularEmployees.isEmpty()) {
            System.out.println("\n=== THỐNG KÊ NHÂN VIÊN THƯỜNG ===");
            for (NhanVien emp : regularEmployees) {
                displayEmployeeStatistics(emp, shiftPatternCount.getOrDefault(emp.getMaNV(), new HashMap<>()));
            }
        }
    }
    
    private void displayEmployeeStatistics(NhanVien employee, Map<Integer, Integer> empStats) {
        int totalShifts = empStats.values().stream().mapToInt(Integer::intValue).sum();
        
        if (totalShifts == 0) return;
        
        System.out.printf("\nNhân viên: %s (%s)\n", employee.getHoTen(), bangViTriCV.get(employee.getMaVT()));
        System.out.printf("Tổng số ca: %d\n", totalShifts);
        
        for (Map.Entry<Integer, Integer> entry : empStats.entrySet()) {
            double percentage = (double) entry.getValue() / totalShifts * 100;
            System.out.printf("- Ca %d: %d ca (%.1f%%)\n", entry.getKey(), entry.getValue(), percentage);
        }
        
        // Tính điểm đa dạng (1 là hoàn toàn đa dạng, 0 là hoàn toàn trùng lặp)
        double diversityScore = calculateDiversityScore(empStats, totalShifts);
        System.out.printf("Điểm đa dạng: %.2f\n", diversityScore);
    }
    
    private double calculateDiversityScore(Map<Integer, Integer> empStats, int totalShifts) {
        if (empStats.size() <= 1) return 0;
        
        double entropy = 0;
        for (int count : empStats.values()) {
            double p = (double) count / totalShifts;
            entropy -= p * Math.log(p);
        }
        
        // Chuẩn hóa về khoảng 0-1
        return entropy / Math.log(empStats.size());
    }
    
    /**
     * Kiểm tra nhân viên đã có lịch làm việc trong ngày chưa
     */
    private boolean isEmployeeScheduledOnDate(int employeeId, LocalDate date) {
        return lichLVDAO.isEmployeeScheduledOnDate(employeeId, date);
    }
    
    /**
     * Lấy danh sách vị trí tương thích với vị trí hiện tại
     */
    private List<Integer> getCompatiblePositions(int position) {
        List<Integer> compatiblePositions = new ArrayList<>();
        
        switch (position) {
            case VI_TRI_DAU_BEP:
                compatiblePositions.add(VI_TRI_DAU_BEP_PHU);
                break;
            case VI_TRI_DAU_BEP_PHU:
                compatiblePositions.add(VI_TRI_DAU_BEP);
                break;
            case VI_TRI_THU_NGAN:
                compatiblePositions.add(VI_TRI_PHUC_VU);
                break;
            case VI_TRI_PHUC_VU:
                compatiblePositions.add(VI_TRI_THU_NGAN);
                break;
            case VI_TRI_QUAN_LY:
                // Quản lý có thể thay thế nhiều vị trí khi cần
                compatiblePositions.add(VI_TRI_THU_NGAN);
                compatiblePositions.add(VI_TRI_PHUC_VU);
                break;
            default:
                // No direct compatibility for other positions
                break;
        }
        
        // Always add the same position
        compatiblePositions.add(0, position);
        
        return compatiblePositions;
    }
    
    /**
     * Kiểm tra hai vị trí có tương thích không
     */
    private boolean isPositionCompatible(int position1, int position2) {
        return getCompatiblePositions(position1).contains(position2);
    }
    
    /**
 * Xử lý khi có nhân viên mới được thêm vào hệ thống bằng thuật toán heuristic
 * @param maNV ID của nhân viên mới
 * @return true nếu điều chỉnh lịch thành công, false nếu có lỗi
 */
public boolean xuLyThemNhanVienMoi(int maNV) {
    NhanVien nhanVienMoi = nhanVienDAO.getNhanVienById(maNV);
    if (nhanVienMoi == null || nhanVienMoi.getTrangThai() == 0) {
        return false;
    }

    // Lấy tháng hiện tại
    LocalDate now = LocalDate.now();
    int thang = now.getMonthValue();
    int nam = now.getYear();

    // Kiểm tra xem tháng hiện tại đã có lịch chưa
    if (!kiemTraThangCoLichLamViec(thang, nam)) {
        return true; // Không cần điều chỉnh nếu chưa có lịch
    }

    // Lấy tất cả lịch làm việc của tháng
    YearMonth namThang = YearMonth.of(nam, thang);
    LocalDate ngayBatDau = namThang.atDay(1);
    LocalDate ngayKetThuc = namThang.atEndOfMonth();
    List<LichLV> lichThang = lichLVDAO.getLichLVByDateRange(ngayBatDau, ngayKetThuc);

    // Phân bố công việc cho nhân viên mới bằng heuristic
    return phanBoCongViecChoNhanVienMoi(nhanVienMoi, lichThang, ngayBatDau, ngayKetThuc);
}

/**
 * Thuật toán heuristic để phân bố công việc cho nhân viên mới
 */
private boolean phanBoCongViecChoNhanVienMoi(NhanVien nhanVienMoi, List<LichLV> lichThang, 
        LocalDate ngayBatDau, LocalDate ngayKetThuc) {
    int maVT = nhanVienMoi.getMaVT();
    int maNV = nhanVienMoi.getMaNV();

    // Lấy danh sách nhân viên cùng vị trí
    List<NhanVien> nhanVienCungViTri = nhanVienDAO.getActiveNhanVien().stream()
            .filter(e -> e.getMaVT() == maVT && e.getMaNV() != maNV)
            .collect(Collectors.toList());

    if (nhanVienCungViTri.isEmpty()) {
        return true; // Không có ai cùng vị trí để cân bằng
    }

    // Tính số ca trung bình của nhân viên cùng vị trí
    double soCaTrungBinh = tinhSoCaTrungBinh(nhanVienCungViTri, ngayBatDau, ngayKetThuc);

    // Tìm các ca có thể chuyển cho nhân viên mới
    List<LichLV> caCoTheChuyen = new ArrayList<>();
    for (NhanVien nv : nhanVienCungViTri) {
        int soCaHienTai = lichLVDAO.countEmployeeShiftsInMonth(nv.getMaNV(), 
                ngayBatDau.getMonthValue(), ngayBatDau.getYear());
        
        if (soCaHienTai > soCaTrungBinh) {
            List<LichLV> caCuaNhanVien = lichThang.stream()
                    .filter(l -> l.getMaNV() == nv.getMaNV())
                    .sorted(Comparator.comparing(LichLV::getNgayLam))
                    .collect(Collectors.toList());
            
            // Thêm các ca vượt quá trung bình vào danh sách có thể chuyển
            int soCaCanChuyen = (int) Math.ceil((soCaHienTai - soCaTrungBinh) / 2.0);
            for (int i = 0; i < Math.min(soCaCanChuyen, caCuaNhanVien.size()); i++) {
                caCoTheChuyen.add(caCuaNhanVien.get(i));
            }
        }
    }

    // Sắp xếp các ca có thể chuyển theo thứ tự ưu tiên (ưu tiên ca gần ngày hiện tại)
    caCoTheChuyen.sort(Comparator.comparing(LichLV::getNgayLam));

    // Phân công các ca cho nhân viên mới
    int soCaDaPhan = 0;
    int soCaMucTieu = (int) Math.ceil(soCaTrungBinh * 0.7); // Nhân viên mới chỉ nhận 70% số ca trung bình

    for (LichLV ca : caCoTheChuyen) {
        if (soCaDaPhan >= soCaMucTieu) break;

        // Kiểm tra nhân viên mới có thể nhận ca này không
        if (canAssignShift(nhanVienMoi, ca.getMaCa(), ca.getNgayLam(), 
                new HashMap<>(), new HashMap<>(), new HashMap<>(), 
                new HashMap<>(), new HashMap<>(), new ArrayList<>())) {
            
            // Chuyển ca cho nhân viên mới
            ca.setMaNV(maNV);
            if (lichLVDAO.updateLichLV(ca)) {
                soCaDaPhan++;
            }
        }
    }

    return soCaDaPhan > 0;
}

/**
 * Tính số ca trung bình của các nhân viên cùng vị trí
 */
private double tinhSoCaTrungBinh(List<NhanVien> danhSachNhanVien, LocalDate ngayBatDau, LocalDate ngayKetThuc) {
    int tongCa = 0;
    int soNhanVien = 0;

    for (NhanVien nv : danhSachNhanVien) {
        int soCa = lichLVDAO.countEmployeeShiftsInMonth(nv.getMaNV(), 
                ngayBatDau.getMonthValue(), ngayBatDau.getYear());
        tongCa += soCa;
        soNhanVien++;
    }

    return soNhanVien > 0 ? (double) tongCa / soNhanVien : 0;
}

/**
 * Xử lý khi có nhân viên nghỉ việc (trạng thái = 0) bằng thuật toán heuristic
 * @param maNV ID của nhân viên nghỉ việc
 * @return true nếu điều chỉnh lịch thành công, false nếu có lỗi
 */
public boolean xuLyNhanVienNghiViec(int maNV) {
    // Lấy tháng hiện tại
    LocalDate now = LocalDate.now();
    int thang = now.getMonthValue();
    int nam = now.getYear();

    // Kiểm tra xem tháng hiện tại đã có lịch chưa
    if (!kiemTraThangCoLichLamViec(thang, nam)) {
        return true; // Không cần điều chỉnh nếu chưa có lịch
    }

    // Lấy tất cả ca làm việc trong tháng của nhân viên nghỉ việc
    YearMonth namThang = YearMonth.of(nam, thang);
    LocalDate ngayBatDau = namThang.atDay(1);
    LocalDate ngayKetThuc = namThang.atEndOfMonth();
    
    List<LichLV> caCuaNhanVienNghi = lichLVDAO.getLichLVByEmployeeAndDateRange(maNV, ngayBatDau, ngayKetThuc);
    if (caCuaNhanVienNghi.isEmpty()) {
        return true;
    }

    // Lấy thông tin vị trí của nhân viên nghỉ việc
    NhanVien nhanVienNghi = nhanVienDAO.getNhanVienById(maNV);
    if (nhanVienNghi == null) {
        return false;
    }
    int maVT = nhanVienNghi.getMaVT();

    // Phân phối lại các ca bằng thuật toán heuristic
    return phanPhoiLaiCaKhiNhanVienNghi(caCuaNhanVienNghi, maVT);
}

/**
 * Thuật toán heuristic để phân phối lại ca khi nhân viên nghỉ việc
 */
private boolean phanPhoiLaiCaKhiNhanVienNghi(List<LichLV> danhSachCa, int maVT) {
    // Lấy danh sách nhân viên cùng vị trí còn hoạt động
    List<NhanVien> nhanVienCungViTri = nhanVienDAO.getActiveNhanVien().stream()
            .filter(e -> e.getMaVT() == maVT)
            .collect(Collectors.toList());

    if (nhanVienCungViTri.isEmpty()) {
        // Nếu không có nhân viên cùng vị trí, thử các vị trí tương thích
        List<Integer> viTriTuongThich = getCompatiblePositions(maVT);
        for (int viTri : viTriTuongThich) {
            if (viTri == maVT) continue;
            
            nhanVienCungViTri = nhanVienDAO.getActiveNhanVien().stream()
                    .filter(e -> e.getMaVT() == viTri)
                    .collect(Collectors.toList());
            
            if (!nhanVienCungViTri.isEmpty()) break;
        }
    }

    if (nhanVienCungViTri.isEmpty()) {
        System.out.println("Không có nhân viên nào có thể thay thế");
        return false;
    }

    // Sắp xếp nhân viên theo số ca ít nhất (ưu tiên người ít ca hơn)
    nhanVienCungViTri.sort(Comparator.comparingInt(e -> 
            lichLVDAO.countEmployeeShiftsInMonth(e.getMaNV(), 
                    danhSachCa.get(0).getNgayLam().getMonthValue(), 
                    danhSachCa.get(0).getNgayLam().getYear())));

    // Phân phối các ca
    boolean tatCaCaDuocPhanPhoi = true;
    for (LichLV ca : danhSachCa) {
        boolean daPhanPhoi = false;
        
        // Thử phân phối cho nhân viên cùng vị trí trước
        for (NhanVien nv : nhanVienCungViTri) {
            if (tryReassignShift(ca, nv.getMaNV())) {
                daPhanPhoi = true;
                break;
            }
        }
        
        if (!daPhanPhoi) {
            System.out.println("Không thể phân phối ca ngày " + ca.getNgayLam() + " ca " + ca.getMaCa());
            tatCaCaDuocPhanPhoi = false;
        }
    }

    return tatCaCaDuocPhanPhoi;
}
}