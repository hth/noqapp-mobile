package com.noqapp.mobile.view.common;

import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.MOBILE_JSON;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.USER_INPUT;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

import com.noqapp.common.errors.ErrorEncounteredJson;
import com.noqapp.common.utils.ParseJsonStringToMap;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.types.OnOffEnum;
import com.noqapp.mobile.view.util.HttpRequestResponseParser;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

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
    private String deviceLanguage = "en";
    private final double[] coordinate = new double[] {0.0, 0.0};
    private String ipAddress;
    private boolean missingCoordinate;

    private OnOffEnum locationOnMobile = OnOffEnum.F;

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

    public String getDeviceLanguage() {
        return deviceLanguage;
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
                LOG.error("Failed missing tokenFCM");
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

            if (map.containsKey("dl")) {
                //TODO remove null check after 1.3.120
                if (null != map.get("dl")) {
                    deviceLanguage = map.get("dl").getText();
                }
            }

            if (locationOnMobile == OnOffEnum.F) {
                missingCoordinate = true;
            } else {
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
            }

            if (map.containsKey("ip") && map.get("ip") != null) {
                ipAddress = map.get("ip").getText();
            } else {
                ipAddress = HttpRequestResponseParser.getClientIpAddress(request);
                LOG.info("Default ip {}", ipAddress);
            }
        } catch (IOException | NullPointerException e) {
            LOG.error("Could not parse json={} errorResponse={}", tokenJson, e.getLocalizedMessage());
            errorResponse = ErrorEncounteredJson.toJson("Could not parse JSON", MOBILE_JSON);
        }
    }
}
