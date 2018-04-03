package com.noqapp.mobile.types;

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
    VI("1.0.1",                 //Oldest Supported App Version in String
            DeviceTypeEnum.I,   //Device Type
            "1.0.1"),           //Latest App Version on App Store


    VA("1.1.40",                //Oldest Supported App Version in String
            DeviceTypeEnum.A,   //Device Type
            "1.1.47");          //Latest App Version on Play Store

    private static final Logger LOG = LoggerFactory.getLogger(LowestSupportedAppEnum.class);

    private String oldestAppVersion;
    private DeviceTypeEnum deviceType;
    private String latestAppVersion;

    /**
     *
     * @param oldestAppVersion
     * @param deviceType
     * @param latestAppVersion
     */
    LowestSupportedAppEnum(
            String oldestAppVersion,
            DeviceTypeEnum deviceType,
            String latestAppVersion
    ) {
        this.oldestAppVersion = oldestAppVersion;
        this.deviceType = deviceType;
        this.latestAppVersion = latestAppVersion;
    }

    public String getLatestAppVersion() {
        return latestAppVersion;
    }

    public static boolean isSupportedVersion(LowestSupportedAppEnum lowestSupportedApp, String appVersionNumber) {
        LOG.info("App Version={} device={}",
                appVersionNumber,
                lowestSupportedApp.deviceType);

        boolean supported = AppVersion.compare(appVersionNumber, lowestSupportedApp.oldestAppVersion);
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
