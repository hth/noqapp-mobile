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
    VI("1.0.0",                 //Oldest App Version in String
            100,                //Oldest App Version as int
            DeviceTypeEnum.I,   //Device Type
            "1.0.0"),           //Latest App Version


    VA("1.0.0",                 //Oldest App Version in String
            100,                //Oldest App Version as int
            DeviceTypeEnum.A,   //Device Type
            "1.0.76");          //Latest App Version

    private static final Logger LOG = LoggerFactory.getLogger(LowestSupportedAppEnum.class);

    private String oldestAppVersion;
    private int oldestAppVersionNumber;
    private DeviceTypeEnum deviceType;
    private String latestAppVersion;

    /**
     *
     * @param oldestAppVersion
     * @param oldestAppVersionNumber
     * @param deviceType
     * @param latestAppVersion
     */
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

    public static boolean isSupportedVersion(LowestSupportedAppEnum lowestSupportedApp, int appVersionNumber) {
        int shortenedAppVersionNumber = Integer.valueOf(String.valueOf(Math.abs((long) appVersionNumber)).substring(0, 3));
        LOG.info("App Version={} device={} and shortenedAppVersionNumber={}",
                appVersionNumber,
                lowestSupportedApp.deviceType,
                shortenedAppVersionNumber);

        boolean supported = true;
        if (lowestSupportedApp.oldestAppVersionNumber > shortenedAppVersionNumber) {
            /* When Device App version is less than supported version for the device, return as not supported. */
            supported = false;
        }

        LOG.info("Calculated supported app version={}", supported);
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
