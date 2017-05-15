package com.noqapp.mobile.view.controller.open;

import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.USER_INPUT;

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
import com.noqapp.domain.types.DeviceTypeEnum;
import com.noqapp.mobile.common.util.ErrorEncounteredJson;
import com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum;
import com.noqapp.mobile.domain.DeviceRegistered;
import com.noqapp.mobile.service.DeviceService;
import com.noqapp.mobile.view.common.ParseTokenFCM;
import com.noqapp.utils.ScrubbedInput;

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

        ParseTokenFCM parseTokenFCM = ParseTokenFCM.newInstance(tokenJson);
        if (StringUtils.isNotBlank(parseTokenFCM.getErrorResponse())) {
            return parseTokenFCM.getErrorResponse();
        }

        try {
            return DeviceRegistered.newInstance(deviceService.registerDevice(
                    null,
                    did.getText(),
                    deviceTypeEnum,
                    parseTokenFCM.getTokenFCM())).asJson();
        } catch (Exception e) {
            LOG.error("Failed registering deviceType={}, reason={}", deviceTypeEnum, e.getLocalizedMessage(), e);
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
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
