package dao;

import models.ViTriCV;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ViTriCVDAO {
    private final Connection conn;
    
    public ViTriCVDAO() {
        conn = DBConnection.getConnection();
    }
    
    public List<ViTriCV> getAllViTriCV() {
        List<ViTriCV> viTriList = new ArrayList<>();
        String sql = "SELECT * FROM ViTriCV ORDER BY MaVT";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                ViTriCV viTri = mapResultSetToViTriCV(rs);
                viTriList.add(viTri);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving job positions");
            e.printStackTrace();
        }
        
        return viTriList;
    }
    
    public ViTriCV getViTriCVById(int maVT) {
        String sql = "SELECT * FROM ViTriCV WHERE MaVT = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, maVT);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToViTriCV(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving job position by ID");
            e.printStackTrace();
        }
        return null;
    }
    
    public boolean insertViTriCV(ViTriCV viTri) {
        String sql = "INSERT INTO ViTriCV (TenVT, MoTa) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, viTri.getTenVT());
            
            if (viTri.getMoTa() != null && !viTri.getMoTa().isEmpty()) {
                stmt.setString(2, viTri.getMoTa());
            } else {
                stmt.setNull(2, Types.NVARCHAR);
            }
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error inserting job position");
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean updateViTriCV(ViTriCV viTri) {
        String sql = "UPDATE ViTriCV SET TenVT = ?, MoTa = ? WHERE MaVT = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, viTri.getTenVT());
            
            if (viTri.getMoTa() != null && !viTri.getMoTa().isEmpty()) {
                stmt.setString(2, viTri.getMoTa());
            } else {
                stmt.setNull(2, Types.NVARCHAR);
            }
            
            stmt.setInt(3, viTri.getMaVT());
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error updating job position");
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean deleteViTriCV(int maVT) {
        // First check if this position is assigned to any employee
        String checkSql = "SELECT COUNT(*) FROM NhanVien WHERE MaVT = ?";
        
        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setInt(1, maVT);
            
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    // This position is assigned to employees, cannot delete
                    return false;
                }
            }
            
            // If not assigned, proceed with deletion
            String deleteSql = "DELETE FROM ViTriCV WHERE MaVT = ?";
            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                deleteStmt.setInt(1, maVT);
                int rowsAffected = deleteStmt.executeUpdate();
                return rowsAffected > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error deleting job position");
            e.printStackTrace();
            return false;
        }
    }
    
    private ViTriCV mapResultSetToViTriCV(ResultSet rs) throws SQLException {
        ViTriCV viTri = new ViTriCV();
        viTri.setMaVT(rs.getInt("MaVT"));
        viTri.setTenVT(rs.getString("TenVT"));
        viTri.setMoTa(rs.getString("MoTa"));
        return viTri;
    }
}
