package com.noqapp.mobile.view.common;

import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.MOBILE_JSON;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.USER_INPUT;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

import com.noqapp.common.utils.ParseJsonStringToMap;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.common.errors.ErrorEncounteredJson;
import com.noqapp.mobile.view.util.HttpRequestResponseParser;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

    private ParseTokenFCM(String tokenJson, HttpServletRequest request) {
        parseForFCM(tokenJson, request);
    }

    public static ParseTokenFCM newInstance(String tokenJson, HttpServletRequest request) {
        return new ParseTokenFCM(tokenJson, request);
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

    private void parseForFCM(String tokenJson, HttpServletRequest request) {
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

            if (map.containsKey("lng") && map.get("lng") != null && map.containsKey("lat") && map.get("lat") != null) {
                try {
                    coordinate[0] = Double.parseDouble(map.get("lng").getText());
                    coordinate[1] = Double.parseDouble(map.get("lat").getText());
                } catch (NumberFormatException | NullPointerException e) {
                    LOG.info("Coordinate missing errorResponse={}", e.getLocalizedMessage());
                    missingCoordinate = true;
                }
            } else {
                missingCoordinate = true;
            }

            if (map.containsKey("ip") && map.get("ip") != null) {
                ipAddress = map.get("ip").getText();
            } else {
                ipAddress = HttpRequestResponseParser.getClientIpAddress(request);
                LOG.info("Default ip {}", ipAddress);
            }
        } catch (IOException | NullPointerException e) {
            LOG.error("Could not parse json={} errorResponse={}", tokenJson, e.getLocalizedMessage(), e);
            errorResponse = ErrorEncounteredJson.toJson("Could not parse JSON", MOBILE_JSON);
        }
    }
}
