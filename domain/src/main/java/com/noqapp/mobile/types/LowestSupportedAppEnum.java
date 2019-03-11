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
    VI("1.1.10",                        //Oldest Supported App Version in String
        DeviceTypeEnum.I,                               //Device Type
        AppFlavorEnum.NQCL,
        "1.1.10",                       //Latest App Version on App Store
        "Version iPhone"),
    /* 1.1.10 is not released. */

    VACL("1.2.230",                     //Oldest Supported App Version in String
        DeviceTypeEnum.A,                               //Device Type
        AppFlavorEnum.NQCL,
        "1.2.235",                      //Latest App Version on Play Store
        "Version Android Client"),

    VACH("1.2.235",                     //Oldest Supported App Version in String
        DeviceTypeEnum.A,                               //Device Type
        AppFlavorEnum.NQCH,
        "1.2.235",                      //Latest App Version on Play Store
        "Version Android Client HealthCare"),

    VAMS("1.2.235",                     //Oldest Supported App Version in String
        DeviceTypeEnum.A,                               //Device Type
        AppFlavorEnum.NQMS,
        "1.2.235",                      //Latest App Version on Play Store
        "Version Android Merchant Store"),

    VAMH("1.2.230",                     //Oldest Supported App Version in String
        DeviceTypeEnum.A,                               //Device Type
        AppFlavorEnum.NQMH,
        "1.2.235",                      //Latest App Version on Play Store
        "Version Android Merchant HealthCare"),

    VAMT("1.2.235",                     //Oldest Supported App Version in String
        DeviceTypeEnum.A,                               //Device Type
        AppFlavorEnum.NQMT,
        "1.2.235",                      //Latest App Version on Play Store
        "Version Android Merchant TV");

    private static final Logger LOG = LoggerFactory.getLogger(LowestSupportedAppEnum.class);

    private String oldestAppVersion;
    private DeviceTypeEnum deviceType;
    private AppFlavorEnum appFlavor;
    private String latestAppVersion;
    private String description;

    LowestSupportedAppEnum(
        String oldestAppVersion,
        DeviceTypeEnum deviceType,
        AppFlavorEnum appFlavor,
        String latestAppVersion,
        String description
    ) {
        this.oldestAppVersion = oldestAppVersion;
        this.deviceType = deviceType;
        this.appFlavor = appFlavor;
        this.latestAppVersion = latestAppVersion;
        this.description = description;
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

    public static LowestSupportedAppEnum findBasedOnDeviceType(DeviceTypeEnum deviceType, AppFlavorEnum appFlavor) {
        for (LowestSupportedAppEnum lowestSupportedApp : LowestSupportedAppEnum.values()) {
            if (lowestSupportedApp.deviceType == deviceType && lowestSupportedApp.appFlavor == appFlavor) {
                return lowestSupportedApp;
            }
        }

        return null;
    }

    public String getDescription() {
        return description;
    }
}
