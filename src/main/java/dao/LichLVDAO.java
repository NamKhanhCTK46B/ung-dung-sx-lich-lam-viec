package dao;

import models.LichLV;

import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class LichLVDAO {
    private final Connection conn;
    
    public LichLVDAO() {
        conn = DBConnection.getConnection();
    }
    
    public List<LichLV> getAllLichLV() {
        List<LichLV> lichList = new ArrayList<>();
        String sql = "SELECT l.*, nv.HoTen, c.TenCa, c.GioBD, c.GioKT, vt.TenVT " +
                     "FROM LichLV l " +
                     "JOIN NhanVien nv ON l.MaNV = nv.MaNV " +
                     "LEFT JOIN Ca c ON l.MaCa = c.MaCa " +
                     "JOIN ViTriCV vt ON nv.MaVT = vt.MaVT " +
                     "ORDER BY l.NgayLam, c.GioBD";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                LichLV lich = mapResultSetToLichLV(rs);
                lichList.add(lich);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving all work schedules");
            e.printStackTrace();
        }
        
        return lichList;
    }
    
    public List<LichLV> getLichLVByMonth(int month, int year) {
        List<LichLV> lichList = new ArrayList<>();
        String sql = "SELECT l.*, nv.HoTen, c.TenCa, c.GioBD, c.GioKT, vt.TenVT " +
                     "FROM LichLV l " +
                     "JOIN NhanVien nv ON l.MaNV = nv.MaNV " +
                     "LEFT JOIN Ca c ON l.MaCa = c.MaCa " +
                     "JOIN ViTriCV vt ON nv.MaVT = vt.MaVT " +
                     "WHERE MONTH(l.NgayLam) = ? AND YEAR(l.NgayLam) = ? " +
                     "ORDER BY l.NgayLam, c.GioBD";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, month);
            stmt.setInt(2, year);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    LichLV lich = mapResultSetToLichLV(rs);
                    lichList.add(lich);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving work schedules by month");
            e.printStackTrace();
        }
        
        return lichList;
    }
    
    public List<LichLV> getLichLVByDateRange(LocalDate startDate, LocalDate endDate) {
        List<LichLV> lichList = new ArrayList<>();
        String sql = "SELECT l.*, nv.HoTen, c.TenCa, c.GioBD, c.GioKT, vt.TenVT " +
                     "FROM LichLV l " +
                     "JOIN NhanVien nv ON l.MaNV = nv.MaNV " +
                     "LEFT JOIN Ca c ON l.MaCa = c.MaCa " +
                     "JOIN ViTriCV vt ON nv.MaVT = vt.MaVT " +
                     "WHERE l.NgayLam BETWEEN ? AND ? " +
                     "ORDER BY l.NgayLam, c.GioBD";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(startDate));
            stmt.setDate(2, Date.valueOf(endDate));
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    LichLV lich = mapResultSetToLichLV(rs);
                    lichList.add(lich);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving work schedules by date range");
            e.printStackTrace();
        }
        
        return lichList;
    }
    
    public List<LichLV> getLichLVByEmployee(int maNV) {
        List<LichLV> lichList = new ArrayList<>();
        String sql = "SELECT l.*, nv.HoTen, c.TenCa, c.GioBD, c.GioKT, vt.TenVT " +
                     "FROM LichLV l " +
                     "JOIN NhanVien nv ON l.MaNV = nv.MaNV " +
                     "LEFT JOIN Ca c ON l.MaCa = c.MaCa " +
                     "JOIN ViTriCV vt ON nv.MaVT = vt.MaVT " +
                     "WHERE l.MaNV = ? " +
                     "ORDER BY l.NgayLam, c.GioBD";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, maNV);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    LichLV lich = mapResultSetToLichLV(rs);
                    lichList.add(lich);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving work schedules by employee");
            e.printStackTrace();
        }
        
        return lichList;
    }
    
    public List<LichLV> getLichLVByEmployeeAndMonth(int maNV, int month, int year) {
        List<LichLV> lichList = new ArrayList<>();
        String sql = "SELECT l.*, nv.HoTen, c.TenCa, c.GioBD, c.GioKT, vt.TenVT " +
                     "FROM LichLV l " +
                     "JOIN NhanVien nv ON l.MaNV = nv.MaNV " +
                     "LEFT JOIN Ca c ON l.MaCa = c.MaCa " +
                     "JOIN ViTriCV vt ON nv.MaVT = vt.MaVT " +
                     "WHERE l.MaNV = ? AND MONTH(l.NgayLam) = ? AND YEAR(l.NgayLam) = ? " +
                     "ORDER BY l.NgayLam, c.GioBD";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, maNV);
            stmt.setInt(2, month);
            stmt.setInt(3, year);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    LichLV lich = mapResultSetToLichLV(rs);
                    lichList.add(lich);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving work schedules by employee and month");
            e.printStackTrace();
        }
        
        return lichList;
    }
    
    public LichLV getLichLVById(int maLich) {
        String sql = "SELECT l.*, nv.HoTen, c.TenCa, c.GioBD, c.GioKT, vt.TenVT " +
                     "FROM LichLV l " +
                     "JOIN NhanVien nv ON l.MaNV = nv.MaNV " +
                     "LEFT JOIN Ca c ON l.MaCa = c.MaCa " +
                     "JOIN ViTriCV vt ON nv.MaVT = vt.MaVT " +
                     "WHERE l.MaLich = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, maLich);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToLichLV(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving schedule by ID");
            e.printStackTrace();
        }
        
        return null;
    }
    
    public boolean insertLichLV(LichLV lich) {
        String sql = "INSERT INTO LichLV (MaNV, MaCa, NgayLam) VALUES (?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, lich.getMaNV());
            
            if (lich.getMaCa() > 0) {
                stmt.setInt(2, lich.getMaCa());
            } else {
                stmt.setNull(2, Types.INTEGER);
            }
            
            stmt.setDate(3, Date.valueOf(lich.getNgayLam()));
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error inserting work schedule");
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean updateLichLV(LichLV lich) {
        String sql = "UPDATE LichLV SET MaNV = ?, MaCa = ?, NgayLam = ? WHERE MaLich = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, lich.getMaNV());
            
            if (lich.getMaCa() > 0) {
                stmt.setInt(2, lich.getMaCa());
            } else {
                stmt.setNull(2, Types.INTEGER);
            }
            
            stmt.setDate(3, Date.valueOf(lich.getNgayLam()));
            stmt.setInt(4, lich.getMaLich());
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error updating work schedule");
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean deleteLichLV(int maLich) {
        String sql = "DELETE FROM LichLV WHERE MaLich = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, maLich);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting work schedule");
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean deleteAllLichLVForMonth(int month, int year) {
        String sql = "DELETE FROM LichLV WHERE MONTH(NgayLam) = ? AND YEAR(NgayLam) = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, month);
            stmt.setInt(2, year);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting all schedules for month");
            e.printStackTrace();
            return false;
        }
    }
    
    private LichLV mapResultSetToLichLV(ResultSet rs) throws SQLException {
        LichLV lich = new LichLV();
        lich.setMaLich(rs.getInt("MaLich"));
        lich.setMaNV(rs.getInt("MaNV"));
        
        // Handle MaCa which can be null
        Object maCaObj = rs.getObject("MaCa");
        if (maCaObj != null) {
            lich.setMaCa(rs.getInt("MaCa"));
        } else {
            lich.setMaCa(0); // Using 0 to denote null/no shift
        }
        
        lich.setNgayLam(rs.getDate("NgayLam").toLocalDate());
        lich.setHoTenNV(rs.getString("HoTen"));
        lich.setTenVT(rs.getString("TenVT"));
        
        // Handle shift details which can be null
        if (maCaObj != null) {
            lich.setTenCa(rs.getString("TenCa"));
            lich.setGioBD(rs.getTime("GioBD").toLocalTime());
            lich.setGioKT(rs.getTime("GioKT").toLocalTime());
        }
        
        return lich;
    }
    
    /**
     * Kiểm tra xem nhân viên đã có lịch làm việc vào ngày cụ thể hay chưa
     * @param maNV ID của nhân viên
     * @param ngay Ngày cần kiểm tra
     * @return true nếu nhân viên đã có lịch vào ngày đó, false nếu không
     */
    public boolean isEmployeeScheduledOnDate(int maNV, LocalDate ngay) {
        String sql = "SELECT COUNT(*) FROM LichLV WHERE MaNV = ? AND NgayLam = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, maNV);
            stmt.setDate(2, Date.valueOf(ngay));
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking if employee is scheduled on date");
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Đếm số ca làm việc của nhân viên trong một tháng
     * @param maNV ID của nhân viên
     * @param thang Tháng (1-12)
     * @param nam Năm
     * @return Số ca làm việc
     */
    public int countEmployeeShiftsInMonth(int maNV, int thang, int nam) {
        String sql = "SELECT COUNT(*) FROM LichLV " +
                     "WHERE MaNV = ? AND MONTH(NgayLam) = ? AND YEAR(NgayLam) = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, maNV);
            stmt.setInt(2, thang);
            stmt.setInt(3, nam);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error counting employee shifts in month");
            e.printStackTrace();
        }
        
        return 0;
    }
    
    /**
     * Lấy danh sách lịch làm việc của nhân viên trong một khoảng thời gian
     * @param maNV ID của nhân viên
     * @param ngayBatDau Ngày bắt đầu
     * @param ngayKetThuc Ngày kết thúc
     * @return Danh sách lịch làm việc
     */
    public List<LichLV> getLichLVByEmployeeAndDateRange(int maNV, LocalDate ngayBatDau, LocalDate ngayKetThuc) {
        List<LichLV> lichList = new ArrayList<>();
        String sql = "SELECT l.*, nv.HoTen, c.TenCa, c.GioBD, c.GioKT, vt.TenVT " +
                     "FROM LichLV l " +
                     "JOIN NhanVien nv ON l.MaNV = nv.MaNV " +
                     "LEFT JOIN Ca c ON l.MaCa = c.MaCa " +
                     "JOIN ViTriCV vt ON nv.MaVT = vt.MaVT " +
                     "WHERE l.MaNV = ? AND l.NgayLam BETWEEN ? AND ? " +
                     "ORDER BY l.NgayLam, c.GioBD";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, maNV);
            stmt.setDate(2, Date.valueOf(ngayBatDau));
            stmt.setDate(3, Date.valueOf(ngayKetThuc));
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    LichLV lich = mapResultSetToLichLV(rs);
                    lichList.add(lich);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving schedules by employee and date range");
            e.printStackTrace();
        }
        
        return lichList;
    }
    
    /**
 * Đếm số ca làm việc của một nhân viên trong một ngày cụ thể
 * @param maNV ID của nhân viên
 * @param ngay Ngày cần kiểm tra
 * @return Số ca làm việc của nhân viên trong ngày đó
 */
public int countShiftsOnDate(int maNV, LocalDate ngay) {
    String sql = "SELECT COUNT(*) FROM LichLV WHERE MaNV = ? AND NgayLam = ?";
    
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setInt(1, maNV);
        stmt.setDate(2, Date.valueOf(ngay));
        
        try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
    } catch (SQLException e) {
        System.err.println("Error counting shifts for employee on date");
        e.printStackTrace();
    }
    
    return 0;
}
}
