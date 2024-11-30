package org.example.common;

public class Threshold {
    private final int WMC;
    private final int CBO;

    public Threshold(int wmc, int cbo) {
        WMC = wmc;
        CBO = cbo;
    }

    public int getWMC() {
        return WMC;
    }

    public int getCBO() {
        return CBO;
    }
}
