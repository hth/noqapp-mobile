package com.token.mobile.view.controller.open;

import static com.token.mobile.common.util.MobileSystemErrorCodeEnum.MOBILE_JSON;
import static com.token.mobile.common.util.MobileSystemErrorCodeEnum.USER_INPUT;

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

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.token.domain.types.DeviceTypeEnum;
import com.token.mobile.common.util.ErrorEncounteredJson;
import com.token.mobile.common.util.MobileSystemErrorCodeEnum;
import com.token.mobile.domain.DeviceRegistered;
import com.token.mobile.service.DeviceService;
import com.token.utils.ParseJsonStringToMap;
import com.token.utils.ScrubbedInput;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

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

    @Autowired
    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
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
    @Timed
    @ExceptionMetered
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
        LOG.info("Register did={} token={}", did.getText(), tokenJson);

        DeviceTypeEnum deviceTypeEnum;
        try {
            deviceTypeEnum = DeviceTypeEnum.valueOf(deviceType.getText());
        } catch (Exception e) {
            LOG.error("Failed parsing deviceType, reason={}", e.getLocalizedMessage(), e);
            return getErrorReason("Incorrect device type.", USER_INPUT);
        }

        Map<String, ScrubbedInput> map;
        String deviceToken;
        try {
            map = ParseJsonStringToMap.jsonStringToMap(tokenJson);
            if (map.isEmpty()) {
                /** Validation failure as there is not data in the map. */
                return getErrorReason("Failed data validation.", USER_INPUT);
            }

            deviceToken = map.get("tk").getText();
            if (StringUtils.isBlank(deviceToken)) {
                return getErrorReason("Failed data validation.", USER_INPUT);
            }
        } catch (IOException e) {
            LOG.error("Could not parse json={} reason={}", tokenJson, e.getLocalizedMessage(), e);
            return ErrorEncounteredJson.toJson("Could not parse JSON", MOBILE_JSON);
        }

        try {
            return DeviceRegistered.newInstance(deviceService.registerDevice(null, did.getText(), deviceTypeEnum, deviceToken)).asJson();
        } catch (Exception e) {
            LOG.error("Failed registering deviceType={}, reason={}", deviceTypeEnum, e.getLocalizedMessage(), e);
            return getErrorReason("Something went wrong. Engineers are looking into this.", USER_INPUT);
        }
    }

    static String getErrorReason(String reason, MobileSystemErrorCodeEnum mobileSystemErrorCode) {
        Map<String, String> errors = new HashMap<>();
        errors.put(ErrorEncounteredJson.REASON, reason);
        errors.put(ErrorEncounteredJson.SYSTEM_ERROR, mobileSystemErrorCode.name());
        errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, mobileSystemErrorCode.getCode());

        return ErrorEncounteredJson.toJson(errors);
    }
}
