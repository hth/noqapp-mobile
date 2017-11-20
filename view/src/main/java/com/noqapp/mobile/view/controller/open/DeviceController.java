package com.noqapp.mobile.view.controller.open;

import com.noqapp.domain.json.JsonLatestAppVersion;
import com.noqapp.domain.types.DeviceTypeEnum;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.common.util.ErrorEncounteredJson;
import com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum;
import com.noqapp.mobile.domain.DeviceRegistered;
import com.noqapp.mobile.service.DeviceService;
import com.noqapp.mobile.types.LowestSupportedAppEnum;
import com.noqapp.mobile.view.common.ParseTokenFCM;
import com.noqapp.utils.ScrubbedInput;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.*;

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

    /**
     * Finds if device exists or saves the device when does not exists. Most likely this call would not be required.
     *
     * @param did
     * @param deviceType iPhone or Android
     * @param response
     * @return
     * @throws IOException
     */
    @RequestMapping (
            method = RequestMethod.POST,
            value = "/register",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String registerDevice(
            @RequestHeader ("X-R-DID")
            ScrubbedInput did,

            @RequestHeader (value = "X-R-DT")
            ScrubbedInput deviceType,

            @RequestBody
            String tokenJson,

            HttpServletResponse response
    ) throws IOException {
        Instant start = Instant.now();
        LOG.info("Register did={} token={}", did.getText(), tokenJson);

        DeviceTypeEnum deviceTypeEnum;
        try {
            deviceTypeEnum = DeviceTypeEnum.valueOf(deviceType.getText());
        } catch (Exception e) {
            LOG.error("Failed parsing deviceType, reason={}", e.getLocalizedMessage(), e);
            return getErrorReason("Incorrect device type.", USER_INPUT);
        }

        ParseTokenFCM parseTokenFCM = ParseTokenFCM.newInstance(tokenJson);
        if (StringUtils.isNotBlank(parseTokenFCM.getErrorResponse())) {
            return parseTokenFCM.getErrorResponse();
        }

        try {
            deviceService.registerDevice(null, did.getText(), deviceTypeEnum, parseTokenFCM.getTokenFCM());
            return DeviceRegistered.newInstance(true).asJson();
        } catch (Exception e) {
            LOG.error("Failed registering deviceType={}, reason={}", deviceTypeEnum, e.getLocalizedMessage(), e);
            apiHealthService.insert(
                    "/register",
                    "registerDevice",
                    DeviceController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.F);
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                    "/register",
                    "registerDevice",
                    DeviceController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.G);
        }
    }

    /**
     * Checks is device version is supported.
     *
     * @param did
     * @param deviceType
     * @param versionRelease
     * @return
     */
    @RequestMapping (
            method = RequestMethod.POST,
            value = "/version",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String isSupportedAppVersion(
            @RequestHeader ("X-R-DID")
            ScrubbedInput did,

            @RequestHeader ("X-R-DT")
            ScrubbedInput deviceType,

            @RequestHeader (value = "X-R-VR")
            ScrubbedInput versionRelease
    ) {
        Instant start = Instant.now();
        LOG.info("Supported device did={} deviceType={} versionRelease={}", did, deviceType, versionRelease);

        try {
            DeviceTypeEnum deviceTypeEnum = DeviceTypeEnum.valueOf(deviceType.getText());
            LOG.info("Check if API version is supported for {} versionRelease={}",
                    deviceTypeEnum.getDescription(),
                    versionRelease);

            try {
                int versionNumber = Integer.valueOf(versionRelease.getText());
                LowestSupportedAppEnum lowestSupportedApp = LowestSupportedAppEnum.findBasedOnDeviceType(deviceTypeEnum);
                if (!LowestSupportedAppEnum.isSupportedVersion(lowestSupportedApp, versionNumber)) {
                    LOG.warn("Sent warning to upgrade versionNumber={}", versionNumber);
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
            apiHealthService.insert(
                    "/version",
                    "version",
                    DeviceController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.F);
            return getErrorReason("Incorrect device type.", USER_INPUT);
        } finally {
            apiHealthService.insert(
                    "/version",
                    "version",
                    DeviceController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.G);
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
