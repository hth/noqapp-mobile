package com.noqapp.mobile.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * hitender
 * 2/7/18 5:21 AM
 */
public class AppVersion {
    private static final Logger LOG = LoggerFactory.getLogger(AppVersion.class);

    /* 1.1.10001, example 10001 are five digits. So the combination can be 00000.00000.00000 */
    private static final int NUMBER_OF_DIGITS = 5;

    private static String normalisedVersion(String version) {
        return normalisedVersion(version, ".", NUMBER_OF_DIGITS);
    }

    private static String normalisedVersion(String version, String sep, int maxWidth) {
        String[] split = Pattern.compile(sep, Pattern.LITERAL).split(version);
        StringBuilder sb = new StringBuilder();
        for (String s : split) {
            sb.append(String.format("%" + maxWidth + 's', s));
        }
        return sb.toString();
    }

    public static boolean compare(String v1, String v2) {
        String s1 = normalisedVersion(v1);
        String s2 = normalisedVersion(v2);
        int cmp = s1.compareTo(s2);
        String cmpStr = cmp < 0 ? "<" : cmp > 0 ? ">" : "==";
        LOG.info("{} {} {}", v1, cmpStr, v2);
        return cmp >= 0;
    }
}
