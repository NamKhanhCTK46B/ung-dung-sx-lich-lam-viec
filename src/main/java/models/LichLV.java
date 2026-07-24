package models;

import java.time.LocalDate;
import java.time.LocalTime;

public class LichLV {
    private int maLich;
    private int maNV;
    private int maCa;
    private LocalDate ngayLam;
    
    // Additional fields for display and processing
    private String hoTenNV;
    private String tenCa;
    private LocalTime gioBD;
    private LocalTime gioKT;
    private String tenVT;

    public LichLV() {
    }

    public LichLV(int maLich, int maNV, int maCa, LocalDate ngayLam) {
        this.maLich = maLich;
        this.maNV = maNV;
        this.maCa = maCa;
        this.ngayLam = ngayLam;
    }

    // Getters and Setters
    public int getMaLich() {
        return maLich;
    }

    public void setMaLich(int maLich) {
        this.maLich = maLich;
    }

    public int getMaNV() {
        return maNV;
    }

    public void setMaNV(int maNV) {
        this.maNV = maNV;
    }

    public int getMaCa() {
        return maCa;
    }

    public void setMaCa(int maCa) {
        this.maCa = maCa;
    }

    public LocalDate getNgayLam() {
        return ngayLam;
    }

    public void setNgayLam(LocalDate ngayLam) {
        this.ngayLam = ngayLam;
    }
    
    public String getHoTenNV() {
        return hoTenNV;
    }

    public void setHoTenNV(String hoTenNV) {
        this.hoTenNV = hoTenNV;
    }

    public String getTenCa() {
        return tenCa;
    }

    public void setTenCa(String tenCa) {
        this.tenCa = tenCa;
    }

    public LocalTime getGioBD() {
        return gioBD;
    }

    public void setGioBD(LocalTime gioBD) {
        this.gioBD = gioBD;
    }

    public LocalTime getGioKT() {
        return gioKT;
    }

    public void setGioKT(LocalTime gioKT) {
        this.gioKT = gioKT;
    }
    
    public String getTenVT() {
        return tenVT;
    }

    public void setTenVT(String tenVT) {
        this.tenVT = tenVT;
    }
    
    public String getShiftDisplay() {
        if (tenCa == null) return "Nghỉ";
        return tenCa + "\n" + gioBD.toString() + " - " + gioKT.toString();
    }
    
    @Override
    public String toString() {
        return "Lịch #" + maLich + ": " + hoTenNV + " - " + tenCa + " - " + ngayLam;
    }
}
