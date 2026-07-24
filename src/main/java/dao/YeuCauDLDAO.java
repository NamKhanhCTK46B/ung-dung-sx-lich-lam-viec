package dao;

import models.YeuCauDL;

import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class YeuCauDLDAO {
    private final Connection conn;
    
    public YeuCauDLDAO() {
        conn = DBConnection.getConnection();
    }
    
    public List<YeuCauDL> getAllYeuCauDL() {
        List<YeuCauDL> yeuCauList = new ArrayList<>();
        String sql = "SELECT yc.*, nv1.HoTen as HoTenNV, nv2.HoTen as HoTenNVDoi, " +
                     "l.NgayLam, c.TenCa " +
                     "FROM YeuCauDL yc " +
                     "JOIN NhanVien nv1 ON yc.MaNV = nv1.MaNV " +
                     "LEFT JOIN NhanVien nv2 ON yc.NhanVienDoi = nv2.MaNV " +
                     "LEFT JOIN LichLV l ON yc.MaLich = l.MaLich " +
                     "LEFT JOIN Ca c ON l.MaCa = c.MaCa " +
                     "ORDER BY yc.TrangThai, yc.MaYC DESC";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                YeuCauDL yeuCau = mapResultSetToYeuCauDL(rs);
                yeuCauList.add(yeuCau);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving all schedule change requests");
            e.printStackTrace();
        }
        
        return yeuCauList;
    }
    
    public List<YeuCauDL> getYeuCauDLByTrangThai(int trangThai) {
        List<YeuCauDL> yeuCauList = new ArrayList<>();
        String sql = "SELECT yc.*, nv1.HoTen as HoTenNV, nv2.HoTen as HoTenNVDoi, " +
                     "l.NgayLam, c.TenCa " +
                     "FROM YeuCauDL yc " +
                     "JOIN NhanVien nv1 ON yc.MaNV = nv1.MaNV " +
                     "LEFT JOIN NhanVien nv2 ON yc.NhanVienDoi = nv2.MaNV " +
                     "LEFT JOIN LichLV l ON yc.MaLich = l.MaLich " +
                     "LEFT JOIN Ca c ON l.MaCa = c.MaCa " +
                     "WHERE yc.TrangThai = ? " +
                     "ORDER BY yc.MaYC DESC";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, trangThai);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    YeuCauDL yeuCau = mapResultSetToYeuCauDL(rs);
                    yeuCauList.add(yeuCau);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving schedule change requests by status");
            e.printStackTrace();
        }
        
        return yeuCauList;
    }
    
    public List<YeuCauDL> getYeuCauDLByEmployee(int maNV) {
        List<YeuCauDL> yeuCauList = new ArrayList<>();
        String sql = "SELECT yc.*, nv1.HoTen as HoTenNV, nv2.HoTen as HoTenNVDoi, " +
                     "l.NgayLam, c.TenCa " +
                     "FROM YeuCauDL yc " +
                     "JOIN NhanVien nv1 ON yc.MaNV = nv1.MaNV " +
                     "LEFT JOIN NhanVien nv2 ON yc.NhanVienDoi = nv2.MaNV " +
                     "LEFT JOIN LichLV l ON yc.MaLich = l.MaLich " +
                     "LEFT JOIN Ca c ON l.MaCa = c.MaCa " +
                     "WHERE yc.MaNV = ? " +
                     "ORDER BY yc.TrangThai, yc.MaYC DESC";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, maNV);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    YeuCauDL yeuCau = mapResultSetToYeuCauDL(rs);
                    yeuCauList.add(yeuCau);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving schedule change requests by employee");
            e.printStackTrace();
        }
        
        return yeuCauList;
    }
    
    public YeuCauDL getYeuCauDLById(int maYC) {
        String sql = "SELECT yc.*, nv1.HoTen as HoTenNV, nv2.HoTen as HoTenNVDoi, " +
                     "l.NgayLam, c.TenCa " +
                     "FROM YeuCauDL yc " +
                     "JOIN NhanVien nv1 ON yc.MaNV = nv1.MaNV " +
                     "LEFT JOIN NhanVien nv2 ON yc.NhanVienDoi = nv2.MaNV " +
                     "LEFT JOIN LichLV l ON yc.MaLich = l.MaLich " +
                     "LEFT JOIN Ca c ON l.MaCa = c.MaCa " +
                     "WHERE yc.MaYC = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, maYC);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToYeuCauDL(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving schedule change request by ID");
            e.printStackTrace();
        }
        
        return null;
    }
    
    public boolean insertYeuCauDL(YeuCauDL yeuCau) {
        String sql = "INSERT INTO YeuCauDL (MaNV, LoaiYC, MaLich, NhanVienDoi, NgayBatDau, NgayKetThuc, TrangThai) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, yeuCau.getMaNV());
            stmt.setInt(2, yeuCau.getLoaiYC());
            
            if (yeuCau.getMaLich() != null) {
                stmt.setInt(3, yeuCau.getMaLich());
            } else {
                stmt.setNull(3, Types.INTEGER);
            }
            
            if (yeuCau.getNhanVienDoi() != null) {
                stmt.setInt(4, yeuCau.getNhanVienDoi());
            } else {
                stmt.setNull(4, Types.INTEGER);
            }
            
            if (yeuCau.getNgayBatDau() != null) {
                stmt.setDate(5, Date.valueOf(yeuCau.getNgayBatDau()));
            } else {
                stmt.setNull(5, Types.DATE);
            }
            
            if (yeuCau.getNgayKetThuc() != null) {
                stmt.setDate(6, Date.valueOf(yeuCau.getNgayKetThuc()));
            } else {
                stmt.setNull(6, Types.DATE);
            }
            
            stmt.setInt(7, yeuCau.getTrangThai());
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error inserting schedule change request");
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean updateYeuCauDL(YeuCauDL yeuCau) {
        String sql = "UPDATE YeuCauDL SET MaNV = ?, LoaiYC = ?, MaLich = ?, NhanVienDoi = ?, " +
                     "NgayBatDau = ?, NgayKetThuc = ?, TrangThai = ? WHERE MaYC = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, yeuCau.getMaNV());
            stmt.setInt(2, yeuCau.getLoaiYC());
            
            if (yeuCau.getMaLich() != null) {
                stmt.setInt(3, yeuCau.getMaLich());
            } else {
                stmt.setNull(3, Types.INTEGER);
            }
            
            if (yeuCau.getNhanVienDoi() != null) {
                stmt.setInt(4, yeuCau.getNhanVienDoi());
            } else {
                stmt.setNull(4, Types.INTEGER);
            }
            
            if (yeuCau.getNgayBatDau() != null) {
                stmt.setDate(5, Date.valueOf(yeuCau.getNgayBatDau()));
            } else {
                stmt.setNull(5, Types.DATE);
            }
            
            if (yeuCau.getNgayKetThuc() != null) {
                stmt.setDate(6, Date.valueOf(yeuCau.getNgayKetThuc()));
            } else {
                stmt.setNull(6, Types.DATE);
            }
            
            stmt.setInt(7, yeuCau.getTrangThai());
            stmt.setInt(8, yeuCau.getMaYC());
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error updating schedule change request");
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean updateYeuCauDLStatus(int maYC, int trangThai) {
        String sql = "UPDATE YeuCauDL SET TrangThai = ? WHERE MaYC = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, trangThai);
            stmt.setInt(2, maYC);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error updating schedule change request status");
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean deleteYeuCauDL(int maYC) {
        String sql = "DELETE FROM YeuCauDL WHERE MaYC = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, maYC);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting schedule change request");
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Kiểm tra nhân viên có yêu cầu nghỉ phép đã được chấp nhận vào ngày được chỉ định
     * @param employeeId ID của nhân viên
     * @param date Ngày cần kiểm tra
     * @return true nếu nhân viên có yêu cầu nghỉ phép được chấp nhận vào ngày đó, false nếu không
     */
    public boolean hasApprovedLeaveRequestOnDate(int employeeId, LocalDate date) {
        String sql = "SELECT * FROM YeuCauDL " +
                    "WHERE MaNV = ? AND LoaiYC = 0 AND TrangThai = 1 " + // LoaiYC=0: nghỉ phép, TrangThai=1: chấp nhận
                    "AND ? BETWEEN NgayBatDau AND NgayKetThuc";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, employeeId);
            stmt.setDate(2, Date.valueOf(date));
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next(); // Trả về true nếu tìm thấy bất kỳ kết quả nào
            }
        } catch (SQLException e) {
            System.err.println("Error checking leave requests for employee on date");
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Lấy tất cả yêu cầu nghỉ phép được chấp nhận trong tháng
     * @param month Tháng cần kiểm tra (1-12)
     * @param year Năm cần kiểm tra
     * @return Danh sách các yêu cầu nghỉ phép được chấp nhận
     */
    public List<YeuCauDL> getApprovedLeaveRequestsForMonth(int month, int year) {
        List<YeuCauDL> leaveRequests = new ArrayList<>();
        
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = YearMonth.of(year, month).atEndOfMonth();
        
        String sql = "SELECT yc.*, nv1.HoTen as HoTenNV " +
                    "FROM YeuCauDL yc " +
                    "JOIN NhanVien nv1 ON yc.MaNV = nv1.MaNV " +
                    "WHERE yc.LoaiYC = 0 AND yc.TrangThai = 1 " + // LoaiYC=0: nghỉ phép, TrangThai=1: chấp nhận
                    "AND ((yc.NgayBatDau BETWEEN ? AND ?) OR " +
                    "(yc.NgayKetThuc BETWEEN ? AND ?) OR " +
                    "(yc.NgayBatDau <= ? AND yc.NgayKetThuc >= ?))";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(startDate));
            stmt.setDate(2, Date.valueOf(endDate));
            stmt.setDate(3, Date.valueOf(startDate));
            stmt.setDate(4, Date.valueOf(endDate));
            stmt.setDate(5, Date.valueOf(startDate));
            stmt.setDate(6, Date.valueOf(endDate));
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    YeuCauDL yeuCau = mapResultSetToYeuCauDL(rs);
                    leaveRequests.add(yeuCau);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving approved leave requests for month");
            e.printStackTrace();
        }
        
        return leaveRequests;
    }
    
    private YeuCauDL mapResultSetToYeuCauDL(ResultSet rs) throws SQLException {
        YeuCauDL yeuCau = new YeuCauDL();
        yeuCau.setMaYC(rs.getInt("MaYC"));
        yeuCau.setMaNV(rs.getInt("MaNV"));
        yeuCau.setLoaiYC(rs.getInt("LoaiYC"));
        
        // Handle nullable fields
        Object maLichObj = rs.getObject("MaLich");
        if (maLichObj != null) {
            yeuCau.setMaLich(rs.getInt("MaLich"));
        }
        
        Object nhanVienDoiObj = rs.getObject("NhanVienDoi");
        if (nhanVienDoiObj != null) {
            yeuCau.setNhanVienDoi(rs.getInt("NhanVienDoi"));
        }
        
        Date ngayBatDau = rs.getDate("NgayBatDau");
        if (ngayBatDau != null) {
            yeuCau.setNgayBatDau(ngayBatDau.toLocalDate());
        }
        
        Date ngayKetThuc = rs.getDate("NgayKetThuc");
        if (ngayKetThuc != null) {
            yeuCau.setNgayKetThuc(ngayKetThuc.toLocalDate());
        }
        
        yeuCau.setTrangThai(rs.getInt("TrangThai"));
        yeuCau.setHoTenNV(rs.getString("HoTenNV"));
        
        String hoTenNVDoi = rs.getString("HoTenNVDoi");
        if (hoTenNVDoi != null) {
            yeuCau.setHoTenNVDoi(hoTenNVDoi);
        }
        
        String tenCa = rs.getString("TenCa");
        if (tenCa != null) {
            yeuCau.setTenCa(tenCa);
        }
        
        Date ngayLam = rs.getDate("NgayLam");
        if (ngayLam != null) {
            yeuCau.setNgayLam(ngayLam.toLocalDate());
        }
        
        return yeuCau;
    }
}
