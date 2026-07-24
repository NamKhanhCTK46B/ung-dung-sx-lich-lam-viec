package models;

import java.time.LocalTime;

public class Ca {
    private int maCa;
    private String tenCa;
    private LocalTime gioBD;
    private LocalTime gioKT;

    public Ca() {
    }

    public Ca(int maCa, String tenCa, LocalTime gioBD, LocalTime gioKT) {
        this.maCa = maCa;
        this.tenCa = tenCa;
        this.gioBD = gioBD;
        this.gioKT = gioKT;
    }

    // Getters and Setters
    public int getMaCa() {
        return maCa;
    }

    public void setMaCa(int maCa) {
        this.maCa = maCa;
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
    
    public String getGioLamViec() {
        return gioBD.toString() + " - " + gioKT.toString();
    }
    
    @Override
    public String toString() {
        return tenCa + " (" + gioBD.toString() + "-" + gioKT.toString() + ")";
    }
}
