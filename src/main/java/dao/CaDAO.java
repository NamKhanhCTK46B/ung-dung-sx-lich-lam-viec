package dao;

import models.Ca;

import java.sql.*;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class CaDAO {
    private final Connection conn;
    
    public CaDAO() {
        conn = DBConnection.getConnection();
    }
    
    public List<Ca> getAllCa() {
        List<Ca> caList = new ArrayList<>();
        String sql = "SELECT * FROM Ca ORDER BY GioBD";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Ca ca = mapResultSetToCa(rs);
                caList.add(ca);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving work shifts");
            e.printStackTrace();
        }
        
        return caList;
    }
    
    public Ca getCaById(int maCa) {
        String sql = "SELECT * FROM Ca WHERE MaCa = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, maCa);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCa(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving work shift by ID");
            e.printStackTrace();
        }
        return null;
    }
    
    public boolean insertCa(Ca ca) {
        String sql = "INSERT INTO Ca (TenCa, GioBD, GioKT) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, ca.getTenCa());
            stmt.setTime(2, Time.valueOf(ca.getGioBD()));
            stmt.setTime(3, Time.valueOf(ca.getGioKT()));
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error inserting work shift");
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean updateCa(Ca ca) {
        String sql = "UPDATE Ca SET TenCa = ?, GioBD = ?, GioKT = ? WHERE MaCa = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, ca.getTenCa());
            stmt.setTime(2, Time.valueOf(ca.getGioBD()));
            stmt.setTime(3, Time.valueOf(ca.getGioKT()));
            stmt.setInt(4, ca.getMaCa());
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error updating work shift");
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean deleteCa(int maCa) {
        // First check if this shift is used in any schedule
        String checkSql = "SELECT COUNT(*) FROM LichLV WHERE MaCa = ?";
        
        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setInt(1, maCa);
            
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    // This shift is used in schedules, cannot delete
                    return false;
                }
            }
            
            // If not used, proceed with deletion
            String deleteSql = "DELETE FROM Ca WHERE MaCa = ?";
            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                deleteStmt.setInt(1, maCa);
                int rowsAffected = deleteStmt.executeUpdate();
                return rowsAffected > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error deleting work shift");
            e.printStackTrace();
            return false;
        }
    }
    
    private Ca mapResultSetToCa(ResultSet rs) throws SQLException {
        Ca ca = new Ca();
        ca.setMaCa(rs.getInt("MaCa"));
        ca.setTenCa(rs.getString("TenCa"));
        ca.setGioBD(rs.getTime("GioBD").toLocalTime());
        ca.setGioKT(rs.getTime("GioKT").toLocalTime());
        return ca;
    }
}
