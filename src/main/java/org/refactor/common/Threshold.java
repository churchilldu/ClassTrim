package org.refactor.common;

public class Threshold {
    private final int WMC;
    private final int CBO;
    private final int RFC;

    public Threshold(int wmc, int cbo, int rfc) {
        WMC = wmc;
        CBO = cbo;
        RFC = rfc;
    }

    public int getWMC() {
        return WMC;
    }

    public int getCBO() {
        return CBO;
    }

    public int getRFC() {
        return RFC;
    }
}
