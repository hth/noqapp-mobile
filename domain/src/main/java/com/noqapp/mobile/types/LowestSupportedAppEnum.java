package com.noqapp.mobile.types;

import com.noqapp.domain.types.DeviceTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * API's are never old. App installed on device is old.
 * <p>
 * User: hitender
 * Date: 4/17/17 3:24 PM
 */
public enum LowestSupportedAppEnum {

    /* List lowest supported version of iPhone and Android app. */
    VI("1.0.0", 100, DeviceTypeEnum.I, "1.0.76"),
    VA("1.0.0", 100, DeviceTypeEnum.A, "1.0.0");

    private static final Logger LOG = LoggerFactory.getLogger(LowestSupportedAppEnum.class);

    private String oldestAppVersion;
    private int oldestAppVersionNumber;
    private DeviceTypeEnum deviceType;
    private String latestAppVersion;

    LowestSupportedAppEnum(
            String oldestAppVersion,
            int oldestAppVersionNumber,
            DeviceTypeEnum deviceType,
            String latestAppVersion
    ) {
        this.oldestAppVersion = oldestAppVersion;
        this.oldestAppVersionNumber = oldestAppVersionNumber;
        this.deviceType = deviceType;
        this.latestAppVersion = latestAppVersion;
    }

    public int getOldestAppVersionNumber() {
        return oldestAppVersionNumber;
    }

    public String getLatestAppVersion() {
        return latestAppVersion;
    }

    public static boolean isLessThanLowestSupportedVersion(LowestSupportedAppEnum lowestSupportedApp, int appVersionNumber) {
        int shortenedAppVersionNumber = Integer.valueOf(String.valueOf(Math.abs((long) appVersionNumber)).substring(0, 3));
        LOG.debug("App Version={} device={} and shortenedAppVersionNumber={}",
                appVersionNumber,
                lowestSupportedApp.deviceType,
                shortenedAppVersionNumber);

        boolean supported = true;
        if (lowestSupportedApp.oldestAppVersionNumber > shortenedAppVersionNumber) {
            supported = false;
        }

        LOG.debug("Calculated supported app version={}", supported);
        return supported;
    }

    public static LowestSupportedAppEnum findBasedOnDeviceType(DeviceTypeEnum deviceType) {
        for (LowestSupportedAppEnum lowestSupportedApp : LowestSupportedAppEnum.values()) {
            if (lowestSupportedApp.deviceType == deviceType) {
                return lowestSupportedApp;
            }
        }

        return null;
    }
}
