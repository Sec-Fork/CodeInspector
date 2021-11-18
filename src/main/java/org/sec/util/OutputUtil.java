package org.sec.util;

import org.sec.model.ResultInfo;

import java.util.List;

public class OutputUtil {
    public static void doOutput(List<ResultInfo> results) {
        StringBuilder sb = new StringBuilder();
        for (ResultInfo resultInfo : results) {
            sb.append("---------------------------").append("\n");
            sb.append("Vuln Name: ").append(resultInfo.getVulnName()).append("\n");
            sb.append("Risk: ");
            if (resultInfo.getRisk() == ResultInfo.HIGH_RISK) {
                sb.append("High Level");
            }
            if (resultInfo.getRisk() == ResultInfo.MID_RISK) {
                sb.append("Middle Level");
            }
            if (resultInfo.getRisk() == ResultInfo.LOW_RISK) {
                sb.append("Low Level");
            }
            sb.append("\n");
            sb.append("Chains: ").append("\n");
            for (String call : resultInfo.getChains()) {
                sb.append("\t").append(call).append("\n");
            }
        }
        FileUtil.writeFile("results.txt", sb.toString());
    }
}
