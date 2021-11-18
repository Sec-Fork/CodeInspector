package org.sec.model;

import java.util.ArrayList;
import java.util.List;

public class ResultInfo {
    public static final int HIGH_RISK = 3;
    public static final int MID_RISK = 2;
    public static final int LOW_RISK = 1;

    private String vulnName;
    private int risk;
    private final List<String> chains = new ArrayList<>();

    public String getVulnName() {
        return vulnName;
    }

    public void setVulnName(String vulnName) {
        this.vulnName = vulnName;
    }

    public int getRisk() {
        return risk;
    }

    public void setRisk(int risk) {
        this.risk = risk;
    }

    public List<String> getChains() {
        return chains;
    }
}
