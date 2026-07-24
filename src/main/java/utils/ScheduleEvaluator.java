package utils;

import dao.LichLVDAO;
import dao.NhanVienDAO;
import models.LichLV;
import models.NhanVien;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

public class ScheduleEvaluator {
    
    private final LichLVDAO lichLVDAO;
    private final NhanVienDAO nhanVienDAO;
    
    public ScheduleEvaluator(LichLVDAO lichLVDAO, NhanVienDAO nhanVienDAO) {
        this.lichLVDAO = lichLVDAO;
        this.nhanVienDAO = nhanVienDAO;
    }
    
    /**
     * Đánh giá lịch làm việc của một tháng và trả về dữ liệu cho bảng
     * @param thang Tháng (1-12)
     * @param nam Năm
     * @param runtime Thời gian chạy thuật toán (giây)
     * @return Map chứa dữ liệu đánh giá
     */
    public Map<String, Double> evaluateScheduleForTable(int thang, int nam, double runtime) {
        YearMonth yearMonth = YearMonth.of(nam, thang);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        
        List<NhanVien> employees = nhanVienDAO.getActiveNhanVien();
        Map<String, Double> results = new HashMap<>();
        
        // Số nhân viên
        results.put("Số nhân viên", (double) employees.size());
        
        // Thời gian chờ (thời gian chạy thuật toán)
        results.put("Thời gian chờ (s)", runtime);
        
        // Độ lệch chuẩn
        double stdDev = calculateShiftCountStdDev(employees, thang, nam);
        results.put("Độ lệch chuẩn", stdDev);
        
        // Tính trung bình các chỉ số của từng nhân viên
        double totalDuplicateRatio = 0;
        double totalEntropy = 0;
        double totalConsecutiveShifts = 0;
        int employeeCount = 0;
        
        for (NhanVien emp : employees) {
            List<LichLV> shifts = lichLVDAO.getLichLVByEmployeeAndDateRange(emp.getMaNV(), startDate, endDate);
            if (shifts.isEmpty()) continue;
            
            Map<Integer, Integer> shiftCount = new HashMap<>();
            for (LichLV shift : shifts) {
                shiftCount.put(shift.getMaCa(), shiftCount.getOrDefault(shift.getMaCa(), 0) + 1);
            }
            
            int totalShifts = shifts.size();
            totalDuplicateRatio += calculateDuplicateRatio(shiftCount, totalShifts);
            totalEntropy += calculateEntropy(shiftCount, totalShifts);
            totalConsecutiveShifts += countConsecutiveShifts(shifts, 1);
            employeeCount++;
        }
        
        // Tỉ lệ trung lập (trùng lặp trung bình)
        results.put("Tỉ lệ trung lập (%)", employeeCount > 0 ? totalDuplicateRatio / employeeCount : 0);
        
        // Entropy trung bình
        results.put("Entropy", employeeCount > 0 ? totalEntropy / employeeCount : 0);
        
        // Tần suất lập trung bình
        results.put("Tần suất lập", employeeCount > 0 ? totalConsecutiveShifts / employeeCount : 0);
        
        return results;
    }
    
    private double calculateShiftCountStdDev(List<NhanVien> employees, int thang, int nam) {
        List<Integer> shiftCounts = new ArrayList<>();
        for (NhanVien emp : employees) {
            int shifts = lichLVDAO.countEmployeeShiftsInMonth(emp.getMaNV(), thang, nam);
            shiftCounts.add(shifts);
        }
        
        double mean = shiftCounts.stream().mapToInt(Integer::intValue).average().orElse(0);
        double sumSquaredDiff = 0;
        for (int count : shiftCounts) {
            sumSquaredDiff += Math.pow(count - mean, 2);
        }
        return Math.sqrt(sumSquaredDiff / employees.size());
    }
    
    private double calculateDuplicateRatio(Map<Integer, Integer> shiftCount, int totalShifts) {
        int maxShiftsPerType = totalShifts / 3 + 2; // Giả sử có 3 loại ca
        int duplicateShifts = 0;
        for (int count : shiftCount.values()) {
            if (count > maxShiftsPerType) {
                duplicateShifts += (count - maxShiftsPerType);
            }
        }
        return (double) duplicateShifts / totalShifts * 100;
    }
    
    private double calculateEntropy(Map<Integer, Integer> shiftCount, int totalShifts) {
        double entropy = 0;
        for (int count : shiftCount.values()) {
            double p = (double) count / totalShifts;
            entropy -= p * Math.log(p);
        }
        return entropy / Math.log(shiftCount.size());
    }
    
    private int countConsecutiveShifts(List<LichLV> shifts, int shiftType) {
        shifts.sort(Comparator.comparing(LichLV::getNgayLam));
        int maxConsecutive = 0;
        int currentConsecutive = 0;
        LocalDate lastDate = null;
        
        for (LichLV shift : shifts) {
            if (shift.getMaCa() == shiftType) {
                if (lastDate != null && shift.getNgayLam().equals(lastDate.plusDays(1))) {
                    currentConsecutive++;
                } else {
                    currentConsecutive = 1;
                }
                maxConsecutive = Math.max(maxConsecutive, currentConsecutive);
                lastDate = shift.getNgayLam();
            } else {
                currentConsecutive = 0;
            }
        }
        return maxConsecutive >= 3 ? maxConsecutive : 0; // Chỉ tính chuỗi >= 3 ngày
    }
}