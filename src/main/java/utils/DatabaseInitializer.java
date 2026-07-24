package utils;

import dao.DBConnection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


 // Lớp khởi tạo cơ sở dữ liệu từ tệp SQL

public class DatabaseInitializer {
    
    
     // Khởi tạo cơ sở dữ liệu từ tệp SQL
     // @return true nếu thành công, false nếu thất bại
     
    public static boolean initializeDatabase() {
        try {
            // Lấy kết nối đến cơ sở dữ liệu
            Connection conn = DBConnection.getConnection();
            if (conn == null) {
                System.err.println("Không thể kết nối đến cơ sở dữ liệu");
                return false;
            }
            
            // Đọc nội dung tệp SQL - sử dụng file ql_lich_lv.sql
            String sqlContent = readFileContent("/com/tieu_luan/sx_lich_lam_viec/sql/ql_lich_lv.sql");
            
            if (sqlContent == null || sqlContent.isEmpty()) {
                System.err.println("Không thể đọc tệp SQL ql_lich_lv.sql");
                // Thử đọc tệp SQL cũ nếu tệp mới không tồn tại
                sqlContent = readFileContent("/com/tieu_luan/sx_lich_lam_viec/sql/sqlserver.sql");
                
                if (sqlContent == null || sqlContent.isEmpty()) {
                    System.err.println("Không thể đọc tệp SQL");
                    return false;
                }
            }
            
            // Tách nội dung tệp SQL thành các lệnh SQL riêng biệt
            List<String> sqlStatements = parseSqlStatements(sqlContent);
            
            // Thực thi từng lệnh SQL
            for (String sql : sqlStatements) {
                if (sql.trim().isEmpty()) continue;
                
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(sql);
                } catch (SQLException e) {
                    System.err.println("Lỗi khi thực thi SQL: " + e.getMessage());
                    System.err.println("SQL gây lỗi: " + sql);
                    // Tiếp tục thực thi các lệnh khác, không dừng lại
                }
            }
            
            System.out.println("Đã khởi tạo cơ sở dữ liệu thành công");
            return true;
        } catch (Exception e) {
            System.err.println("Lỗi khi khởi tạo cơ sở dữ liệu: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    
     // Đọc nội dung tệp SQL từ tài nguyên
     // @param fileName Tên tệp SQL
     // @return Nội dung tệp SQL
    
    private static String readFileContent(String fileName) {
        // Thử đọc từ nguồn tài nguyên
        String content = readFromResource(fileName);
        if (content != null) {
            return content;
        }
        
        // Thử đọc từ thư mục gốc
        content = readFromResource("/" + fileName);
        if (content != null) {
            return content;
        }
        
        // Thử đọc từ ClassLoader.getSystemResourceAsStream
        content = readFromSystemResource(fileName);
        if (content != null) {
            return content;
        }
        
        // Nếu không tìm thấy tệp
        System.err.println("Không tìm thấy tệp: " + fileName);
        return null;
    }
    
    private static String readFromResource(String path) {
        try (InputStream is = DatabaseInitializer.class.getResourceAsStream(path)) {
            if (is == null) {
                return null;
            }
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            System.err.println("Lỗi khi đọc tệp từ resource '" + path + "': " + e.getMessage());
            return null;
        }
    }
    
    private static String readFromSystemResource(String path) {
        try (InputStream is = ClassLoader.getSystemResourceAsStream(path)) {
            if (is == null) {
                return null;
            }
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            System.err.println("Lỗi khi đọc tệp từ system resource '" + path + "': " + e.getMessage());
            return null;
        }
    }
    
    
     // Tách nội dung tệp SQL thành các lệnh SQL riêng biệt
     // @param sqlContent Nội dung tệp SQL
     // @return Danh sách các lệnh SQL
    
    private static List<String> parseSqlStatements(String sqlContent) {
        List<String> statements = new ArrayList<>();
        StringBuilder currentStatement = new StringBuilder();
        boolean inString = false;
        boolean inComment = false;
        boolean inLineComment = false;
        boolean statementComplete = false;
        boolean inDollarQuote = false;
        String dollarTag = "";
        
        for (int i = 0; i < sqlContent.length(); i++) {
            char c = sqlContent.charAt(i);
            char next = (i < sqlContent.length() - 1) ? sqlContent.charAt(i + 1) : '\0';
            
            // Xử lý comment dạng --
            if (c == '-' && next == '-' && !inString && !inComment && !inDollarQuote) {
                inLineComment = true;
                continue;
            }
            
            // Kết thúc comment dạng --
            if ((c == '\n' || c == '\r') && inLineComment) {
                inLineComment = false;
                continue;
            }
            
            // Bỏ qua nội dung trong comment
            if (inLineComment) {
                continue;
            }
            
            // Xử lý comment dạng /* */
            if (c == '/' && next == '*' && !inString && !inLineComment && !inDollarQuote) {
                inComment = true;
                i++; // Bỏ qua dấu *
                continue;
            }
            
            // Kết thúc comment dạng /* */
            if (c == '*' && next == '/' && inComment) {
                inComment = false;
                i++; // Bỏ qua dấu /
                continue;
            }
            
            // Bỏ qua nội dung trong comment
            if (inComment) {
                continue;
            }
            
            // Xử lý dollar-quoted string ($$...$$)
            if (c == '$' && !inString && !inComment && !inLineComment) {
                if (!inDollarQuote) {
                    // Bắt đầu dollar quote
                    int j = i + 1;
                    while (j < sqlContent.length() && 
                          (Character.isLetterOrDigit(sqlContent.charAt(j)) || 
                           sqlContent.charAt(j) == '_')) {
                        j++;
                    }
                    
                    if (j < sqlContent.length() && sqlContent.charAt(j) == '$') {
                        inDollarQuote = true;
                        dollarTag = sqlContent.substring(i, j + 1);
                        currentStatement.append(dollarTag);
                        i = j;
                        continue;
                    }
                } else {
                    // Kiểm tra xem có phải kết thúc dollar quote không
                    if (i + dollarTag.length() <= sqlContent.length() && 
                        sqlContent.substring(i, i + dollarTag.length()).equals(dollarTag)) {
                        inDollarQuote = false;
                        currentStatement.append(dollarTag);
                        i += dollarTag.length() - 1;
                        continue;
                    }
                }
            }
            
            // Xử lý string literals
            if (c == '\'' && !inComment && !inLineComment && !inDollarQuote) {
                inString = !inString;
                currentStatement.append(c);
                continue;
            }
            
            // Xử lý escaped single quotes trong string
            if (c == '\'' && next == '\'' && inString) {
                currentStatement.append("''");
                i++;
                continue;
            }
            
            // Kiểm tra kết thúc câu lệnh SQL (;)
            if (c == ';' && !inString && !inComment && !inLineComment && !inDollarQuote) {
                currentStatement.append(c);
                statementComplete = true;
            } else {
                currentStatement.append(c);
            }
            
            // Nếu câu lệnh đã hoàn thành, thêm vào danh sách và reset
            if (statementComplete) {
                statements.add(currentStatement.toString().trim());
                currentStatement = new StringBuilder();
                statementComplete = false;
            }
        }
        
        // Thêm câu lệnh cuối cùng nếu có
        String lastStatement = currentStatement.toString().trim();
        if (!lastStatement.isEmpty()) {
            statements.add(lastStatement);
        }
        
        return statements;
    }
}