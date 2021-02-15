package com.noqapp.mobile.view.controller.api.client;

import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.DEVICE_DETAIL_MISSING;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.mobile.view.controller.api.client.TokenQueueAPIController.authorizeRequest;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

import com.noqapp.common.utils.CommonUtil;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.shared.GeoPointOfQ;
import com.noqapp.domain.types.AppFlavorEnum;
import com.noqapp.domain.types.DeviceTypeEnum;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.domain.DeviceRegistered;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.service.DeviceRegistrationService;
import com.noqapp.mobile.service.exception.DeviceDetailMissingException;
import com.noqapp.mobile.view.common.ParseTokenFCM;
import com.noqapp.mobile.view.util.HttpRequestResponseParser;
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

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * hitender
 * 2/14/21 11:00 PM
 */
@SuppressWarnings({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/api/c/device")
public class DeviceClientController {

    private static final Logger LOG = LoggerFactory.getLogger(com.noqapp.mobile.view.controller.open.DeviceController.class);

    private AuthenticateMobileService authenticateMobileService;
    private DeviceRegistrationService deviceRegistrationService;
    private GeoIPLocationService geoIPLocationService;
    private ApiHealthService apiHealthService;

    @Autowired
    public DeviceClientController(
        AuthenticateMobileService authenticateMobileService,
        DeviceRegistrationService deviceRegistrationService,
        GeoIPLocationService geoIPLocationService,
        ApiHealthService apiHealthService
    ) {
        this.authenticateMobileService = authenticateMobileService;
        this.deviceRegistrationService = deviceRegistrationService;
        this.geoIPLocationService = geoIPLocationService;
        this.apiHealthService = apiHealthService;
    }

    @PostMapping(
        value = {"/register"},
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String registerDevice(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader (value = "X-R-AF")
        ScrubbedInput appFlavor,

        @RequestHeader("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        String tokenJson,

        HttpServletRequest request,
        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Client device registration did={} dt={} mail={}", did, deviceType, mail);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

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
                qid,
                deviceId,
                DeviceTypeEnum.valueOf(deviceType.getText()),
                AppFlavorEnum.valueOf(appFlavor.getText()),
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
            LOG.error("Failed registering deviceType={}, reason={}", deviceType.getText(), e.getLocalizedMessage(), e);
            return getErrorReason("Missing device details", DEVICE_DETAIL_MISSING);
        } catch (Exception e) {
            LOG.error("Failed registering deviceType={}, reason={}", deviceType.getText(), e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/register",
                "registerDevice",
                com.noqapp.mobile.view.controller.open.DeviceController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }
}
