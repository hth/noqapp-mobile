package com.noqapp.mobile.view.common;

import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MOBILE_JSON;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.USER_INPUT;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.noqapp.mobile.common.util.ErrorEncounteredJson;
import com.noqapp.common.utils.ParseJsonStringToMap;
import com.noqapp.common.utils.ScrubbedInput;

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

    private void parseForFCM(String tokenJson) {
        Map<String, ScrubbedInput> map;
        try {
            map = ParseJsonStringToMap.jsonStringToMap(tokenJson);
            if (map.isEmpty()) {
                /** Validation failure as there is not data in the map. */
                errorResponse = getErrorReason("Failed data validation.", USER_INPUT);
            }

            tokenFCM = map.get("tk").getText();
            if (StringUtils.isBlank(tokenFCM)) {
                errorResponse = getErrorReason("Failed data validation.", USER_INPUT);
            }
        } catch (IOException e) {
            LOG.error("Could not parse json={} errorResponse={}", tokenJson, e.getLocalizedMessage(), e);
            errorResponse = ErrorEncounteredJson.toJson("Could not parse JSON", MOBILE_JSON);
        }
    }
}
