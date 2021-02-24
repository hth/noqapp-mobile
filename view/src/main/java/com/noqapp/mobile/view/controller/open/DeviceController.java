package com.noqapp.mobile.view.controller.open;

import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.DEVICE_DETAIL_MISSING;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.MOBILE_UPGRADE;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.USER_INPUT;

import com.noqapp.common.utils.CommonUtil;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.json.JsonLatestAppVersion;
import com.noqapp.domain.shared.GeoPointOfQ;
import com.noqapp.domain.types.AppFlavorEnum;
import com.noqapp.domain.types.DeviceTypeEnum;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.common.errors.ErrorEncounteredJson;
import com.noqapp.common.errors.MobileSystemErrorCodeEnum;
import com.noqapp.mobile.domain.DeviceRegistered;
import com.noqapp.mobile.service.exception.DeviceDetailMissingException;
import com.noqapp.mobile.types.LowestSupportedAppEnum;
import com.noqapp.mobile.view.common.ParseTokenFCM;
import com.noqapp.mobile.view.util.HttpRequestResponseParser;
import com.noqapp.mobile.service.DeviceRegistrationService;
import com.noqapp.search.elastic.helper.IpCoordinate;
import com.noqapp.search.elastic.service.GeoIPLocationService;

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
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

/**
 * User: hitender
 * Date: 3/1/17 12:04 PM
 */
@SuppressWarnings({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/open/device")
public class DeviceController {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceController.class);

    private DeviceRegistrationService deviceRegistrationService;
    private GeoIPLocationService geoIPLocationService;
    private ApiHealthService apiHealthService;

    @Autowired
    public DeviceController(
        DeviceRegistrationService deviceRegistrationService,
        GeoIPLocationService geoIPLocationService,
        ApiHealthService apiHealthService
    ) {
        this.deviceRegistrationService = deviceRegistrationService;
        this.geoIPLocationService = geoIPLocationService;
        this.apiHealthService = apiHealthService;
    }

    /**
     * Finds if device exists or saves the device when does not exists. Most likely this call would not be required.
     * v1 is deprecated since 1.2.655.
     */
    @PostMapping(
        value = {"/v1/register", "/register"},
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String registerDevice(
        @RequestHeader("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader("X-R-AF")
        ScrubbedInput appFlavor,

        @RequestBody
        String tokenJson,

        HttpServletRequest request
    ) {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Register deviceType={} appFlavor={} token={}",
            deviceType.getText(),
            appFlavor.getText(),
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

        ParseTokenFCM parseTokenFCM = ParseTokenFCM.newInstance(tokenJson, request);
        if (StringUtils.isNotBlank(parseTokenFCM.getErrorResponse())) {
            return parseTokenFCM.getErrorResponse();
        }

        try {
            String deviceId = UUID.randomUUID().toString().toUpperCase();
            double[] coordinate;
            String ip;
            if (parseTokenFCM.isMissingCoordinate()) {
                IpCoordinate ipCoordinate = geoIPLocationService.computeIpCoordinate(
                    CommonUtil.retrieveIPV4(
                        parseTokenFCM.getIpAddress(),
                        HttpRequestResponseParser.getClientIpAddress(request)));

                coordinate = ipCoordinate.getCoordinate() == null ? parseTokenFCM.getCoordinate() : ipCoordinate.getCoordinate();
                ip = ipCoordinate.getIp();
            } else {
                coordinate = parseTokenFCM.getCoordinate();
                ip = parseTokenFCM.getIpAddress();
            }

            deviceRegistrationService.registerDevice(
                null,
                deviceId,
                deviceTypeEnum,
                appFlavorEnum,
                parseTokenFCM.getTokenFCM(),
                parseTokenFCM.getModel(),
                parseTokenFCM.getOsVersion(),
                parseTokenFCM.getAppVersion(),
                coordinate,
                ip);

            DeviceRegistered deviceRegistered = DeviceRegistered.newInstance(true, deviceId);
            if (null != coordinate) {
                deviceRegistered.setGeoPointOfQ(new GeoPointOfQ(coordinate[1], coordinate[0]));
            }
            return deviceRegistered.asJson();
        } catch (DeviceDetailMissingException e) {
            LOG.error("Failed registering deviceType={}, reason={}", deviceTypeEnum, e.getLocalizedMessage(), e);
            return getErrorReason("Missing device details", DEVICE_DETAIL_MISSING);
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

    /**
     * Checks is device version is supported.
     * X-R-DID is deprecated since 1.2.655.
     */
    @PostMapping(
        value = "/version",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Deprecated
    public String obsolete(
        @RequestHeader(value = "X-R-DID", required = false)
        ScrubbedInput did,

        @RequestHeader("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader("X-R-AF")
        ScrubbedInput appFlavor,

        @RequestHeader(value = "X-R-VR")
        ScrubbedInput versionRelease
    ) {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Supported device deviceType={} appFlavor={} versionRelease={}", deviceType, appFlavor, versionRelease);

        try {
            //TODO remove me
            if (null != did) {
                LOG.warn("Old app version did={} versionRelease={}", did, versionRelease);
                return getErrorReason("To continue, please upgrade to latest version", MOBILE_UPGRADE);
            }

            DeviceTypeEnum deviceTypeEnum = DeviceTypeEnum.valueOf(deviceType.getText());
            AppFlavorEnum appFlavorEnum = AppFlavorEnum.valueOf(appFlavor.getText());
            LOG.info("Check if supported app version {} {} versionRelease={}", deviceTypeEnum.getDescription(), appFlavor, versionRelease);

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

    @PostMapping(
        value = "/versionCheck",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String isSupportedAppVersion(
        @RequestHeader("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader("X-R-AF")
        ScrubbedInput appFlavor,

        @RequestHeader(value = "X-R-VR")
        ScrubbedInput versionRelease
    ) {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Supported device deviceType={} appFlavor={} versionRelease={}", deviceType, appFlavor, versionRelease);

        try {
            DeviceTypeEnum deviceTypeEnum = DeviceTypeEnum.valueOf(deviceType.getText());
            AppFlavorEnum appFlavorEnum = AppFlavorEnum.valueOf(appFlavor.getText());
            LOG.info("Check if supported app version {} {} versionRelease={}", deviceTypeEnum.getDescription(), appFlavor, versionRelease);

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
                "/versionCheck",
                "/versionCheck",
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
