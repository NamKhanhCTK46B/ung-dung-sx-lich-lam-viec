package dao;

import models.NhanVien;
import utils.PasswordHasher;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NhanVienDAO {
    private final Connection conn;
    
    public NhanVienDAO() {
        conn = DBConnection.getConnection();
    }
    
    public NhanVien login(String username, String password) {
        String sql = "SELECT nv.*, vt.TenVT FROM NhanVien nv " +
                     "JOIN ViTriCV vt ON nv.MaVT = vt.MaVT " +
                     "WHERE nv.TenDN = ? AND nv.TrangThai > 0";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedPassword = rs.getString("MatKhau");
                    
                    // Kiểm tra mật khẩu - luôn sử dụng BCrypt cho xác thực
                    boolean isMatch = false;
                    
                    if (storedPassword.startsWith("$2a$")) { 
                    // Nếu mật khẩu đã mã hóa, dùng BCrypt để kiểm tra
                    isMatch = PasswordHasher.kiemTraMatKhau(password, storedPassword);
                } else { 
                    // Nếu chưa mã hóa, so sánh trực tiếp
                    isMatch = password.equals(storedPassword);
                    
                    // Nếu mật khẩu trùng khớp, tiến hành mã hóa và cập nhật vào CSDL
                    if (isMatch) {
                        String hashedPassword = PasswordHasher.maHoaMatKhau(password);
                        updateUserPassword(rs.getInt("MaNV"), hashedPassword);
                    }
                }

                    
                    if (isMatch) {
                        NhanVien nhanVien = mapResultSetToNhanVien(rs);
                        nhanVien.setTenVT(rs.getString("TenVT"));
                        return nhanVien;
                    }
                    else {
                        System.out.println("Sai mật khẩu!");
                    }
                }
                else {
                    System.out.println("Không tìm thấy tài khoản với username: " + username);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi trong quá trình đăng nhập");
            e.printStackTrace();
        }
        return null;
    }
    
    
     // Cập nhật mật khẩu của người dùng
     // @param maNV ID của nhân viên
     // @param newHashedPassword Mật khẩu đã mã hóa mới
     // @return true nếu cập nhật thành công, false nếu thất bại
     
    private boolean updateUserPassword(int maNV, String newHashedPassword) {
        String sql = "UPDATE NhanVien SET MatKhau = ? WHERE MaNV = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newHashedPassword);
            stmt.setInt(2, maNV);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi khi cập nhật mật khẩu: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public List<NhanVien> getAllNhanVien() {
        List<NhanVien> nhanViens = new ArrayList<>();
        String sql = "SELECT nv.*, vt.TenVT FROM NhanVien nv " +
                     "JOIN ViTriCV vt ON nv.MaVT = vt.MaVT " +
                     "ORDER BY nv.MaNV";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                NhanVien nhanVien = mapResultSetToNhanVien(rs);
                nhanVien.setTenVT(rs.getString("TenVT"));
                nhanViens.add(nhanVien);
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy danh sách tất cả nhân viên");
            e.printStackTrace();
        }
        
        return nhanViens;
    }
    
    public List<NhanVien> getActiveNhanVien() {
        List<NhanVien> nhanViens = new ArrayList<>();
        String sql = "SELECT nv.*, vt.TenVT FROM NhanVien nv " +
                     "JOIN ViTriCV vt ON nv.MaVT = vt.MaVT " +
                     "WHERE nv.TrangThai > 0 " +
                     "ORDER BY nv.MaNV";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                NhanVien nhanVien = mapResultSetToNhanVien(rs);
                nhanVien.setTenVT(rs.getString("TenVT"));
                nhanViens.add(nhanVien);
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy danh sách nhân viên đang làm việc");
            e.printStackTrace();
        }
        
        return nhanViens;
    }
    
    public NhanVien getNhanVienById(int maNV) {
        String sql = "SELECT nv.*, vt.TenVT FROM NhanVien nv " +
                     "JOIN ViTriCV vt ON nv.MaVT = vt.MaVT " +
                     "WHERE nv.MaNV = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, maNV);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    NhanVien nhanVien = mapResultSetToNhanVien(rs);
                    nhanVien.setTenVT(rs.getString("TenVT"));
                    return nhanVien;
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy thông tin nhân viên theo ID");
            e.printStackTrace();
        }
        return null;
    }
    
    public boolean insertNhanVien(NhanVien nhanVien) {
        String sql = "INSERT INTO NhanVien (HoTen, CCCD, SDT, Email, GioiTinh, MaVT, TenDN, MatKhau, TrangThai, SoNgayNghiThang) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nhanVien.getHoTen());
            stmt.setString(2, nhanVien.getCccd());
            stmt.setString(3, nhanVien.getSdt());
            stmt.setString(4, nhanVien.getEmail());
            
            if (nhanVien.getGioiTinh() != null) {
                stmt.setBoolean(5, nhanVien.getGioiTinh());
            } else {
                stmt.setNull(5, Types.BIT);
            }
            
            stmt.setInt(6, nhanVien.getMaVT());
            stmt.setString(7, nhanVien.getTenDN());
            
            // Xác định mật khẩu - mặc định là '123' nếu không có
            String password = nhanVien.getMatKhau() != null && !nhanVien.getMatKhau().isEmpty() ? 
                              nhanVien.getMatKhau() : "123";
            
            // Mã hóa mật khẩu
            String hashedPassword = PasswordHasher.hashPassword(password);
            stmt.setString(8, hashedPassword);
            
            stmt.setInt(9, nhanVien.getTrangThai());
            stmt.setInt(10, nhanVien.getSoNgayNghiThang());
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi khi thêm nhân viên");
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean updateNhanVien(NhanVien nhanVien) {
        String sql = "UPDATE NhanVien SET HoTen = ?, CCCD = ?, SDT = ?, Email = ?, " +
                     "GioiTinh = ?, MaVT = ?, TenDN = ?, TrangThai = ?, SoNgayNghiThang = ? " +
                     "WHERE MaNV = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nhanVien.getHoTen());
            stmt.setString(2, nhanVien.getCccd());
            stmt.setString(3, nhanVien.getSdt());
            stmt.setString(4, nhanVien.getEmail());
            
            if (nhanVien.getGioiTinh() != null) {
                stmt.setBoolean(5, nhanVien.getGioiTinh());
            } else {
                stmt.setNull(5, Types.BIT);
            }
            
            stmt.setInt(6, nhanVien.getMaVT());
            stmt.setString(7, nhanVien.getTenDN());
            stmt.setInt(8, nhanVien.getTrangThai());
            stmt.setInt(9, nhanVien.getSoNgayNghiThang());
            stmt.setInt(10, nhanVien.getMaNV());
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi khi cập nhật thông tin nhân viên");
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean changePassword(int maNV, String newPassword) {
        String hashedPassword = PasswordHasher.hashPassword(newPassword);
        String sql = "UPDATE NhanVien SET MatKhau = ? WHERE MaNV = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, hashedPassword);
            stmt.setInt(2, maNV);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi khi thay đổi mật khẩu");
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean deleteNhanVien(int maNV) {
        // Thay vì xóa, chúng ta cập nhật trạng thái thành "Đã nghỉ việc" (0)
        String sql = "UPDATE NhanVien SET TrangThai = 0 WHERE MaNV = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, maNV);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi khi xóa nhân viên");
            e.printStackTrace();
            return false;
        }
    }
    
    private NhanVien mapResultSetToNhanVien(ResultSet rs) throws SQLException {
        NhanVien nhanVien = new NhanVien();
        nhanVien.setMaNV(rs.getInt("MaNV"));
        nhanVien.setHoTen(rs.getString("HoTen"));
        nhanVien.setCccd(rs.getString("CCCD"));
        nhanVien.setSdt(rs.getString("SDT"));
        nhanVien.setEmail(rs.getString("Email"));
        
        // Xử lý giá trị null cho trường giới tính
        if (rs.getObject("GioiTinh") != null) {
            nhanVien.setGioiTinh(rs.getBoolean("GioiTinh"));
        }
        
        nhanVien.setMaVT(rs.getInt("MaVT"));
        nhanVien.setTenDN(rs.getString("TenDN"));
        nhanVien.setMatKhau(rs.getString("MatKhau"));
        nhanVien.setTrangThai(rs.getInt("TrangThai"));
        nhanVien.setSoNgayNghiThang(rs.getInt("SoNgayNghiThang"));
        
        return nhanVien;
    }
}
