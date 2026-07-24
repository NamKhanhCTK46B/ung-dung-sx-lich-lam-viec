package models;

public class NhanVien {
    private int maNV;
    private String hoTen;
    private String cccd;
    private String sdt;
    private String email;
    private Boolean gioiTinh;  // true: Nam, false: Nữ
    private int maVT;
    private String tenDN;
    private String matKhau;
    private int trangThai;  // 0: Đã nghỉ, 1: Đang làm, 2: Đang nghỉ phép
    private int soNgayNghiThang;
    
    // Additional field to store position name for display purposes
    private String tenVT;

    public NhanVien() {
    }

    public NhanVien(int maNV, String hoTen, String cccd, String sdt, String email, Boolean gioiTinh, 
                   int maVT, String tenDN, String matKhau, int trangThai, int soNgayNghiThang) {
        this.maNV = maNV;
        this.hoTen = hoTen;
        this.cccd = cccd;
        this.sdt = sdt;
        this.email = email;
        this.gioiTinh = gioiTinh;
        this.maVT = maVT;
        this.tenDN = tenDN;
        this.matKhau = matKhau;
        this.trangThai = trangThai;
        this.soNgayNghiThang = soNgayNghiThang;
    }

    // Getters and Setters
    public int getMaNV() {
        return maNV;
    }

    public void setMaNV(int maNV) {
        this.maNV = maNV;
    }

    public String getHoTen() {
        return hoTen;
    }

    public void setHoTen(String hoTen) {
        this.hoTen = hoTen;
    }

    public String getCccd() {
        return cccd;
    }

    public void setCccd(String cccd) {
        this.cccd = cccd;
    }

    public String getSdt() {
        return sdt;
    }

    public void setSdt(String sdt) {
        this.sdt = sdt;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getGioiTinh() {
        return gioiTinh;
    }

    public void setGioiTinh(Boolean gioiTinh) {
        this.gioiTinh = gioiTinh;
    }

    public int getMaVT() {
        return maVT;
    }

    public void setMaVT(int maVT) {
        this.maVT = maVT;
    }

    public String getTenDN() {
        return tenDN;
    }

    public void setTenDN(String tenDN) {
        this.tenDN = tenDN;
    }

    public String getMatKhau() {
        return matKhau;
    }

    public void setMatKhau(String matKhau) {
        this.matKhau = matKhau;
    }

    public int getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(int trangThai) {
        this.trangThai = trangThai;
    }

    public int getSoNgayNghiThang() {
        return soNgayNghiThang;
    }

    public void setSoNgayNghiThang(int soNgayNghiThang) {
        this.soNgayNghiThang = soNgayNghiThang;
    }
    
    public String getTenVT() {
        return tenVT;
    }

    public void setTenVT(String tenVT) {
        this.tenVT = tenVT;
    }
    
    public String getTrangThaiText() {
        switch (trangThai) {
            case 0:
                return "Đã nghỉ việc";
            case 1:
                return "Đang làm việc";
            case 2:
                return "Đang nghỉ phép";
            default:
                return "Không xác định";
        }
    }
    
    public String getGioiTinhText() {
        return gioiTinh != null ? (gioiTinh ? "Nam" : "Nữ") : "Không xác định";
    }
    
    @Override
    public String toString() {
        return hoTen + " - " + tenVT;
    }
}
