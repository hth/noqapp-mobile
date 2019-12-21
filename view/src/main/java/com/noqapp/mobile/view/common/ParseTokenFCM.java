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
    private String appVersion;
    private double[] coordinate;
    private String ipAddress;
    private boolean missingCoordinate;

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

    public String getAppVersion() {
        return appVersion;
    }

    public double[] getCoordinate() {
        return coordinate;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public boolean isMissingCoordinate() {
        return missingCoordinate;
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

            if (map.containsKey("mo")) {
                model = map.get("mo").getText();
            }

            if (map.containsKey("os")) {
                osVersion = map.get("os").getText();
            }

            if (map.containsKey("av")) {
                appVersion = map.get("av").getText();
            }

            if (map.containsKey("lng") && map.containsKey("lat")) {
                try {
                    coordinate[0] = Double.parseDouble(map.get("lng").getText());
                    coordinate[1] = Double.parseDouble(map.get("lat").getText());
                } catch (NumberFormatException | NullPointerException e) {
                    LOG.info("Coordinate missing lng={} lat={} errorResponse={}",
                        map.get("lng").getText(),
                        map.get("lat").getText(),
                        e.getLocalizedMessage());
                    missingCoordinate = true;
                }
            }

            if (map.containsKey("ip")) {
                ipAddress = map.get("ip").getText();
            }
        } catch (IOException | NullPointerException e) {
            LOG.error("Could not parse json={} errorResponse={}", tokenJson, e.getLocalizedMessage(), e);
            errorResponse = ErrorEncounteredJson.toJson("Could not parse JSON", MOBILE_JSON);
        }
    }
}
