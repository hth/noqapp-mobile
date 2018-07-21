package com.noqapp.mobile.types;

import com.noqapp.domain.types.AppFlavorEnum;
import com.noqapp.domain.types.DeviceTypeEnum;
import com.noqapp.mobile.common.util.AppVersion;

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
    VI("1.1.10",                    //Oldest Supported App Version in String
            DeviceTypeEnum.I,       //Device Type
            AppFlavorEnum.NQCL,
            "1.1.10"),              //Latest App Version on App Store
    /* 1.1.10 is not released. */


    VA("1.1.200",                   //Oldest Supported App Version in String
            DeviceTypeEnum.A,       //Device Type
            AppFlavorEnum.NQCL,
            "1.1.201"),             //Latest App Version on Play Store

    VACL("1.1.200",                  //Oldest Supported App Version in String
            DeviceTypeEnum.A,       //Device Type
            AppFlavorEnum.NQCL,
            "1.1.201"),             //Latest App Version on Play Store

    VACH("1.1.200",                 //Oldest Supported App Version in String
            DeviceTypeEnum.A,       //Device Type
            AppFlavorEnum.NQCH,
            "1.1.201"),             //Latest App Version on Play Store

    VAMS("1.1.200",
            DeviceTypeEnum.A,
            AppFlavorEnum.NQMS,
            "1.1.201"),

    VAMH("1.1.200",
            DeviceTypeEnum.A,
            AppFlavorEnum.NQMH,
            "1.1.201"),

    VAMT("1.1.200",
            DeviceTypeEnum.A,
            AppFlavorEnum.NQMT,
            "1.1.201");

    private static final Logger LOG = LoggerFactory.getLogger(LowestSupportedAppEnum.class);

    private String oldestAppVersion;
    private DeviceTypeEnum deviceType;
    private AppFlavorEnum appFlavor;
    private String latestAppVersion;

    LowestSupportedAppEnum(
            String oldestAppVersion,
            DeviceTypeEnum deviceType,
            AppFlavorEnum appFlavor,
            String latestAppVersion
    ) {
        this.oldestAppVersion = oldestAppVersion;
        this.deviceType = deviceType;
        this.appFlavor = appFlavor;
        this.latestAppVersion = latestAppVersion;
    }

    public String getOldestAppVersion() {
        return oldestAppVersion;
    }

    public String getLatestAppVersion() {
        return latestAppVersion;
    }

    public static boolean isSupportedVersion(LowestSupportedAppEnum lowestSupportedApp, String appVersionNumber) {
        boolean supported = AppVersion.compare(appVersionNumber, lowestSupportedApp.oldestAppVersion);
        LOG.info("App Version={} device={} appFlavor={} supported app version={}",
                appVersionNumber,
                lowestSupportedApp.deviceType.getName(),
                lowestSupportedApp.appFlavor.getName(),
                supported);
        return supported;
    }

    @Deprecated
    public static LowestSupportedAppEnum findBasedOnDeviceType(DeviceTypeEnum deviceType) {
        for (LowestSupportedAppEnum lowestSupportedApp : LowestSupportedAppEnum.values()) {
            if (lowestSupportedApp.deviceType == deviceType) {
                return lowestSupportedApp;
            }
        }

        return null;
    }

    public static LowestSupportedAppEnum findBasedOnDeviceType(DeviceTypeEnum deviceType, AppFlavorEnum appFlavor) {
        for (LowestSupportedAppEnum lowestSupportedApp : LowestSupportedAppEnum.values()) {
            if (lowestSupportedApp.deviceType == deviceType && lowestSupportedApp.appFlavor == appFlavor) {
                return lowestSupportedApp;
            }
        }

        return null;
    }
}
