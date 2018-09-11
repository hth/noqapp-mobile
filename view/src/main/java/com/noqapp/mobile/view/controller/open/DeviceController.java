package com.noqapp.mobile.view.controller.open;

import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MOBILE_UPGRADE;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.USER_INPUT;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.json.JsonLatestAppVersion;
import com.noqapp.domain.types.AppFlavorEnum;
import com.noqapp.domain.types.DeviceTypeEnum;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.common.util.ErrorEncounteredJson;
import com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum;
import com.noqapp.mobile.domain.DeviceRegistered;
import com.noqapp.mobile.service.DeviceService;
import com.noqapp.mobile.types.LowestSupportedAppEnum;
import com.noqapp.mobile.view.common.ParseTokenFCM;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * User: hitender
 * Date: 3/1/17 12:04 PM
 */
@SuppressWarnings ({
        "PMD.BeanMembersShouldSerialize",
        "PMD.LocalVariableCouldBeFinal",
        "PMD.MethodArgumentCouldBeFinal",
        "PMD.LongVariable"
})
@RestController
@RequestMapping (value = "/open/device")
public class DeviceController {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceController.class);

    private DeviceService deviceService;
    private ApiHealthService apiHealthService;

    @Autowired
    public DeviceController(DeviceService deviceService, ApiHealthService apiHealthService) {
        this.deviceService = deviceService;
        this.apiHealthService = apiHealthService;
    }

    /** Finds if device exists or saves the device when does not exists. Most likely this call would not be required. */
    @PostMapping(
            value = "/register",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String registerDevice(
            @RequestHeader ("X-R-DID")
            ScrubbedInput did,

            @RequestHeader (value = "X-R-DT")
            ScrubbedInput deviceType,

            @RequestHeader ("X-R-AF")
            ScrubbedInput appFlavor,

            @RequestBody
            String tokenJson
    ) {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Register deviceType={} appFlavor={} did={} token={}",
                deviceType.getText(),
                appFlavor.getText(),
                did.getText(),
                tokenJson);

        DeviceTypeEnum deviceTypeEnum;
        try {
            deviceTypeEnum = DeviceTypeEnum.valueOf(deviceType.getText());
        } catch (Exception e) {
            LOG.error("Failed parsing deviceType, reason={}", e.getLocalizedMessage(), e);
            return getErrorReason("Incorrect device type.", USER_INPUT);
        }

        AppFlavorEnum appFlavorEnum;
        try {
            appFlavorEnum = AppFlavorEnum.valueOf(appFlavor.getText());
        } catch (Exception e) {
            LOG.error("Failed parsing appFlavor, reason={}", e.getLocalizedMessage(), e);
            return getErrorReason("Incorrect appFlavor type.", USER_INPUT);
        }

        ParseTokenFCM parseTokenFCM = ParseTokenFCM.newInstance(tokenJson);
        if (StringUtils.isNotBlank(parseTokenFCM.getErrorResponse())) {
            return parseTokenFCM.getErrorResponse();
        }

        try {
            deviceService.registerDevice(
                null,
                did.getText(),
                deviceTypeEnum,
                appFlavorEnum,
                parseTokenFCM.getTokenFCM(),
                parseTokenFCM.getModel(),
                parseTokenFCM.getOsVersion());
            return DeviceRegistered.newInstance(true).asJson();
        } catch (Exception e) {
            LOG.error("Failed registering deviceType={}, reason={}", deviceTypeEnum, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                    "/register",
                    "registerDevice",
                    DeviceController.class.getName(),
                    Duration.between(start, Instant.now()),
                    methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Checks is device version is supported. */
    @PostMapping (
        value = "/version",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String isSupportedAppVersion(
        @RequestHeader ("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader ("X-R-AF")
        ScrubbedInput appFlavor,

        @RequestHeader (value = "X-R-VR")
        ScrubbedInput versionRelease
    ) {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Supported device did={} deviceType={} appFlavor={} versionRelease={}", did, deviceType, appFlavor, versionRelease);

        try {
            DeviceTypeEnum deviceTypeEnum = DeviceTypeEnum.valueOf(deviceType.getText());
            AppFlavorEnum appFlavorEnum = AppFlavorEnum.valueOf(appFlavor.getText());
            LOG.info("Check if API version is supported for {} versionRelease={}",
                deviceTypeEnum.getDescription(),
                versionRelease);

            try {
                LowestSupportedAppEnum lowestSupportedApp = LowestSupportedAppEnum.findBasedOnDeviceType(deviceTypeEnum, appFlavorEnum);
                if (!LowestSupportedAppEnum.isSupportedVersion(lowestSupportedApp, versionRelease.getText())) {
                    LOG.warn("Sent warning to upgrade versionNumber={}", versionRelease.getText());
                    return getErrorReason("To continue, please upgrade to latest version", MOBILE_UPGRADE);
                }

                return new JsonLatestAppVersion(lowestSupportedApp.getLatestAppVersion()).asJson();
            } catch (NumberFormatException e) {
                LOG.error("Failed parsing API version, reason={}", e.getLocalizedMessage(), e);
                return getErrorReason("Failed to read API version type.", USER_INPUT);
            } catch (Exception e) {
                LOG.error("Failed parsing API version, reason={}", e.getLocalizedMessage(), e);
                return getErrorReason("Incorrect API version type.", USER_INPUT);
            }
        } catch (Exception e) {
            LOG.error("Failed parsing deviceType, reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Incorrect device type.", USER_INPUT);
        } finally {
            apiHealthService.insert(
                "/version",
                "/version",
                DeviceController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    @Deprecated
    /** Checks is device version is supported. */
    @PostMapping (
            value = "/v1/version",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String isSupportedAppVersionObsolete(
            @RequestHeader ("X-R-DID")
            ScrubbedInput did,

            @RequestHeader ("X-R-DT")
            ScrubbedInput deviceType,

            @RequestHeader ("X-R-AF")
            ScrubbedInput appFlavor,

            @RequestHeader (value = "X-R-VR")
            ScrubbedInput versionRelease
    ) {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Supported device did={} deviceType={} appFlavor={} versionRelease={}", did, deviceType, appFlavor, versionRelease);

        try {
            DeviceTypeEnum deviceTypeEnum = DeviceTypeEnum.valueOf(deviceType.getText());
            AppFlavorEnum appFlavorEnum = AppFlavorEnum.valueOf(appFlavor.getText());
            LOG.info("Check if API version is supported for {} versionRelease={}",
                    deviceTypeEnum.getDescription(),
                    versionRelease);

            try {
                LowestSupportedAppEnum lowestSupportedApp = LowestSupportedAppEnum.findBasedOnDeviceType(deviceTypeEnum, appFlavorEnum);
                if (!LowestSupportedAppEnum.isSupportedVersion(lowestSupportedApp, versionRelease.getText())) {
                    LOG.warn("Sent warning to upgrade versionNumber={}", versionRelease.getText());
                    return getErrorReason("To continue, please upgrade to latest version", MOBILE_UPGRADE);
                }

                return new JsonLatestAppVersion(lowestSupportedApp.getLatestAppVersion()).asJson();
            } catch (NumberFormatException e) {
                LOG.error("Failed parsing API version, reason={}", e.getLocalizedMessage(), e);
                return getErrorReason("Failed to read API version type.", USER_INPUT);
            } catch (Exception e) {
                LOG.error("Failed parsing API version, reason={}", e.getLocalizedMessage(), e);
                return getErrorReason("Incorrect API version type.", USER_INPUT);
            }
        } catch (Exception e) {
            LOG.error("Failed parsing deviceType, reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Incorrect device type.", USER_INPUT);
        } finally {
            apiHealthService.insert(
                    "/v1/version",
                    "/v1/version",
                    DeviceController.class.getName(),
                    Duration.between(start, Instant.now()),
                    methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    public static String getErrorReason(String reason, MobileSystemErrorCodeEnum mobileSystemErrorCode) {
        Map<String, String> errors = new HashMap<>();
        errors.put(ErrorEncounteredJson.REASON, reason);
        errors.put(ErrorEncounteredJson.SYSTEM_ERROR, mobileSystemErrorCode.name());
        errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, mobileSystemErrorCode.getCode());

        return ErrorEncounteredJson.toJson(errors);
    }
}
