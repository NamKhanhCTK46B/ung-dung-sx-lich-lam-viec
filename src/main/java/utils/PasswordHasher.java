package utils;

import org.mindrot.jbcrypt.BCrypt;


 // Lớp tiện ích để mã hóa và xác thực mật khẩu sử dụng BCrypt.
 
public class PasswordHasher {
    
    private PasswordHasher() {
        // Constructor riêng tư để ngăn khởi tạo đối tượng
    }
    
     // Mã hóa mật khẩu sử dụng BCrypt
     // @param matKhauGoc Mật khẩu nguyên bản cần mã hóa
     // @return Mật khẩu đã được mã hóa
     
    public static String maHoaMatKhau(String matKhauGoc) {
        return BCrypt.hashpw(matKhauGoc, BCrypt.gensalt(12));
    }
    
    
     // Xác thực mật khẩu nguyên bản với mật khẩu đã mã hóa
     // @param matKhauGoc Mật khẩu nguyên bản cần kiểm tra
     // @param matKhauDaMaHoa Mật khẩu đã mã hóa để so sánh
     // @return true nếu mật khẩu khớp, false nếu không
     
    public static boolean kiemTraMatKhau(String matKhauGoc, String matKhauDaMaHoa) {
        boolean result = BCrypt.checkpw(matKhauGoc, matKhauDaMaHoa);
        System.out.println("Kiểm tra mật khẩu: " + matKhauGoc + " với " + matKhauDaMaHoa + " -> Kết quả: " + result);
        return result;
    }
    
    
     // Phương thức cũ hỗ trợ trong quá trình chuyển đổi sang tiếng Việt
     // @param plainPassword Mật khẩu nguyên bản cần mã hóa
     // @return Mật khẩu đã được mã hóa
     
    public static String hashPassword(String plainPassword) {
        return maHoaMatKhau(plainPassword);
    }
    
    
     // Phương thức cũ hỗ trợ trong quá trình chuyển đổi sang tiếng Việt
     // @param plainPassword Mật khẩu nguyên bản cần kiểm tra
     // @param hashedPassword Mật khẩu đã mã hóa để so sánh
     // @return true nếu mật khẩu khớp, false nếu không
     
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        return kiemTraMatKhau(plainPassword, hashedPassword);
    }
}
