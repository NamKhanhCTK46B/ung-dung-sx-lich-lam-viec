package models;

public class ViTriCV {
    private int maVT;
    private String tenVT;
    private String moTa;

    public ViTriCV() {
    }

    public ViTriCV(int maVT, String tenVT, String moTa) {
        this.maVT = maVT;
        this.tenVT = tenVT;
        this.moTa = moTa;
    }

    // Getters and Setters
    public int getMaVT() {
        return maVT;
    }

    public void setMaVT(int maVT) {
        this.maVT = maVT;
    }

    public String getTenVT() {
        return tenVT;
    }

    public void setTenVT(String tenVT) {
        this.tenVT = tenVT;
    }

    public String getMoTa() {
        return moTa;
    }

    public void setMoTa(String moTa) {
        this.moTa = moTa;
    }
    
    @Override
    public String toString() {
        return tenVT;
    }
}
