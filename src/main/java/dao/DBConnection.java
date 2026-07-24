package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


 // Lớp quản lý kết nối với cơ sở dữ liệu

public class DBConnection {
    private static Connection connection = null;
    
    // Hằng số cấu hình SQL Server
    private static final String SQL_SERVER_DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    private static final String SQL_SERVER_URL = getRequiredEnvironmentVariable("DB_URL");
    private static final String SQL_SERVER_USER = getRequiredEnvironmentVariable("DB_USER");
    private static final String SQL_SERVER_PASSWORD = getRequiredEnvironmentVariable("DB_PASSWORD");
    
    // QUAN TRỌNG: Khi chạy trên máy cục bộ, bạn cần thay đổi thông tin kết nối SQL Server ở trên
    // phù hợp với cấu hình SQL Server của bạn (username, password, port, v.v.)
    
    
     // Private constructor để ngăn việc tạo instance
     
    private DBConnection() {
        // Private constructor to prevent instantiation
    }

    private static String getRequiredEnvironmentVariable(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Missing required environment variable: " + name);
        }
        return value;
    }
    
    
     // Lấy kết nối đến cơ sở dữ liệu (hoặc tạo kết nối mới nếu chưa có)
     
    public static Connection getConnection() {
        if (connection != null) {
            return connection;
        }
        
        try {
            return connectToSQLServer();
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    
     // Kết nối đến SQL Server trên môi trường local
     
    private static Connection connectToSQLServer() throws SQLException, ClassNotFoundException {
        // Đảm bảo driver SQL Server được load
        Class.forName(SQL_SERVER_DRIVER);
        
        // Tạo kết nối SQL Server
        connection = DriverManager.getConnection(SQL_SERVER_URL, SQL_SERVER_USER, SQL_SERVER_PASSWORD);
        //connection = DriverManager.getConnection(SQL_SERVER_URL);
        
        if (connection != null) {
            //System.out.println("SQL Server connected successfully");
            System.out.println("SQL Server connected successfully using Windows Authentication");
        }
        
        return connection;
    }
    
    
     // Đóng kết nối cơ sở dữ liệu
     
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
                System.out.println("Database connection closed");
            } catch (SQLException e) {
                System.err.println("Error closing database connection");
                e.printStackTrace();
            }
        }
    }
}
