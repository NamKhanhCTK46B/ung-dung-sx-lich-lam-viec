module com.tieu_luan.sapxeplichlv {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires jcodec;
    requires java.sql;
    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;
    requires itextpdf;
    requires jbcrypt;

    opens models to javafx.base;
    opens com.tieu_luan.sapxeplichlv to javafx.fxml;
    exports com.tieu_luan.sapxeplichlv;
}
