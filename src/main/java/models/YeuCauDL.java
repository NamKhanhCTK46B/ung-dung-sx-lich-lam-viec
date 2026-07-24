package models;

import java.time.LocalDate;

public class YeuCauDL {
    private int maYC;
    private int maNV;
    private int loaiYC;  // 0: Nghỉ phép, 1: Đổi ca
    private Integer maLich;  // Nullable
    private Integer nhanVienDoi;  // Nullable
    private LocalDate ngayBatDau;
    private LocalDate ngayKetThuc;
    private int trangThai;  // 0: Chờ duyệt, 1: Chấp nhận, 2: Từ chối
    
    // Additional fields for display
    private String hoTenNV;
    private String hoTenNVDoi;
    private String tenCa;
    private LocalDate ngayLam;

    public YeuCauDL() {
    }

    public YeuCauDL(int maYC, int maNV, int loaiYC, Integer maLich, Integer nhanVienDoi, 
                   LocalDate ngayBatDau, LocalDate ngayKetThuc, int trangThai) {
        this.maYC = maYC;
        this.maNV = maNV;
        this.loaiYC = loaiYC;
        this.maLich = maLich;
        this.nhanVienDoi = nhanVienDoi;
        this.ngayBatDau = ngayBatDau;
        this.ngayKetThuc = ngayKetThuc;
        this.trangThai = trangThai;
    }

    // Getters and Setters
    public int getMaYC() {
        return maYC;
    }

    public void setMaYC(int maYC) {
        this.maYC = maYC;
    }

    public int getMaNV() {
        return maNV;
    }

    public void setMaNV(int maNV) {
        this.maNV = maNV;
    }

    public int getLoaiYC() {
        return loaiYC;
    }

    public void setLoaiYC(int loaiYC) {
        this.loaiYC = loaiYC;
    }

    public Integer getMaLich() {
        return maLich;
    }

    public void setMaLich(Integer maLich) {
        this.maLich = maLich;
    }

    public Integer getNhanVienDoi() {
        return nhanVienDoi;
    }

    public void setNhanVienDoi(Integer nhanVienDoi) {
        this.nhanVienDoi = nhanVienDoi;
    }

    public LocalDate getNgayBatDau() {
        return ngayBatDau;
    }

    public void setNgayBatDau(LocalDate ngayBatDau) {
        this.ngayBatDau = ngayBatDau;
    }

    public LocalDate getNgayKetThuc() {
        return ngayKetThuc;
    }

    public void setNgayKetThuc(LocalDate ngayKetThuc) {
        this.ngayKetThuc = ngayKetThuc;
    }

    public int getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(int trangThai) {
        this.trangThai = trangThai;
    }
    
    public String getHoTenNV() {
        return hoTenNV;
    }

    public void setHoTenNV(String hoTenNV) {
        this.hoTenNV = hoTenNV;
    }

    public String getHoTenNVDoi() {
        return hoTenNVDoi;
    }

    public void setHoTenNVDoi(String hoTenNVDoi) {
        this.hoTenNVDoi = hoTenNVDoi;
    }

    public String getTenCa() {
        return tenCa;
    }

    public void setTenCa(String tenCa) {
        this.tenCa = tenCa;
    }

    public LocalDate getNgayLam() {
        return ngayLam;
    }

    public void setNgayLam(LocalDate ngayLam) {
        this.ngayLam = ngayLam;
    }
    
    public String getLoaiYCText() {
        return loaiYC == 0 ? "Nghỉ phép" : "Đổi ca";
    }
    
    public String getTrangThaiText() {
         switch (trangThai) {
            case 0:
                return "Chờ duyệt";
            case 1:
                return "Chấp nhận";
            case 2:
                return "Từ chối";
            default:
                return "Không xác định";
        }
    }
    
    @Override
    public String toString() {
        if (loaiYC == 0) {
            return "Yêu cầu nghỉ phép: " + hoTenNV + " (" + ngayBatDau + " đến " + ngayKetThuc + ")";
        } else {
            return "Yêu cầu đổi ca: " + hoTenNV + " - " + tenCa + " - " + ngayLam;
        }
    }
}
