package com.noqapp.mobile.types;

import com.noqapp.domain.types.DeviceTypeEnum;

/**
 * API's are never old. App installed on device is old.
 * <p>
 * User: hitender
 * Date: 4/17/17 3:24 PM
 */
public enum LowestSupportedAppEnum {

    /* List lowest supported version of iPhone and Android app. */
    VI("1.0.0", 100, DeviceTypeEnum.I),
    VA("1.0.0", 100, DeviceTypeEnum.A);

    private String appVersion;
    private int appVersionNumber;
    private DeviceTypeEnum deviceType;

    LowestSupportedAppEnum(String appVersion, int appVersionNumber, DeviceTypeEnum deviceType) {
        this.appVersion = appVersion;
        this.appVersionNumber = appVersionNumber;
        this.deviceType = deviceType;
    }

    public int getAppVersionNumber() {
        return appVersionNumber;
    }

    public static boolean isLessThanLowestSupportedVersion(DeviceTypeEnum deviceType, int appVersionNumber) {
        int shortenedAppVersionNumber = Integer.valueOf(String.valueOf(Math.abs((long) appVersionNumber)).substring(0, 3));
        boolean notSupported = false;
        for (LowestSupportedAppEnum lowestSupportedAPI : LowestSupportedAppEnum.values()) {
            if (lowestSupportedAPI.deviceType == deviceType && lowestSupportedAPI.appVersionNumber >= shortenedAppVersionNumber) {
                notSupported = true;
            }
        }

        return notSupported;
    }
}
