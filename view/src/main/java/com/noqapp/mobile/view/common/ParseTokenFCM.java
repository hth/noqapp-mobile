package com.noqapp.mobile.view.common;

import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MOBILE_JSON;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.USER_INPUT;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

import com.noqapp.common.utils.ParseJsonStringToMap;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.mobile.common.util.ErrorEncounteredJson;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * User: hitender
 * Date: 4/17/17 10:38 PM
 */
public class ParseTokenFCM {
    private static final Logger LOG = LoggerFactory.getLogger(ParseTokenFCM.class);

    private String errorResponse;
    private String tokenFCM;
    private String model;
    private String osVersion;

    private ParseTokenFCM(String tokenJson) {
        parseForFCM(tokenJson);
    }

    public static ParseTokenFCM newInstance(String tokenJson) {
        return new ParseTokenFCM(tokenJson);
    }

    public String getErrorResponse() {
        return errorResponse;
    }

    public String getTokenFCM() {
        return tokenFCM;
    }

    public String getModel() {
        return model;
    }

    public String getOsVersion() {
        return osVersion;
    }

    private void parseForFCM(String tokenJson) {
        Map<String, ScrubbedInput> map;
        try {
            map = ParseJsonStringToMap.jsonStringToMap(tokenJson);
            if (map.isEmpty()) {
                /* Validation failure as there is not data in the map. */
                errorResponse = getErrorReason("Failed data validation.", USER_INPUT);
            }

            if (null == map.get("tk")) {
                LOG.error("Found empty firebase token contains={} but value={}", map.containsKey("tk"), map.get("tk"));
                /* Validation failure as there is not data in the map. */
                errorResponse = getErrorReason("Failed data validation.", USER_INPUT);
            }

            tokenFCM = map.get("tk").getText();
            if (StringUtils.isBlank(tokenFCM)) {
                errorResponse = getErrorReason("Failed data validation.", USER_INPUT);
            }

            model = map.get("mo").getText();
            osVersion = map.get("os").getText();
        } catch (IOException | NullPointerException e) {
            LOG.error("Could not parse json={} errorResponse={}", tokenJson, e.getLocalizedMessage(), e);
            errorResponse = ErrorEncounteredJson.toJson("Could not parse JSON", MOBILE_JSON);
        }
    }
}
