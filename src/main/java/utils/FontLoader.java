package utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


 // Lớp tiện ích để tải font từ tài nguyên

public class FontLoader {

    /**
     * Tải font từ thư mục resources và tạo file tạm
     * @param fontPath đường dẫn đến font trong thư mục resources (vd: "fonts/times.ttf")
     * @return đường dẫn đầy đủ đến file font tạm
     * @throws IOException nếu có lỗi khi đọc/ghi file
     */
    public static String loadFontFromResource(String fontPath) throws IOException {
        // Tạo thư mục tạm nếu chưa tồn tại
        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "restaurant_scheduler_fonts");
        if (!Files.exists(tempDir)) {
            Files.createDirectories(tempDir);
        }
        
        // Lấy tên file từ đường dẫn
        String fontFileName = Paths.get(fontPath).getFileName().toString();
        File tempFile = new File(tempDir.toFile(), fontFileName);
        
        // Nếu file đã tồn tại, không cần tạo lại
        if (tempFile.exists()) {
            return tempFile.getAbsolutePath();
        }
        
        // Đọc font từ resources và ghi ra file tạm
        try (InputStream is = FontLoader.class.getClassLoader().getResourceAsStream(fontPath)) {
            if (is == null) {
                throw new IOException("Không tìm thấy font trong resources: " + fontPath);
            }
            
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, length);
                }
            }
        }
        
        return tempFile.getAbsolutePath();
    }
}