package com.noqapp.mobile.view.controller.api.merchant;

import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.BUSINESS_APP_ACCESS_DENIED;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.DEVICE_DETAIL_MISSING;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.mobile.view.controller.api.client.TokenQueueAPIController.authorizeRequest;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

import com.noqapp.common.utils.CommonUtil;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.RegisteredDeviceEntity;
import com.noqapp.domain.UserAccountEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.json.JsonResponse;
import com.noqapp.domain.types.AppFlavorEnum;
import com.noqapp.domain.types.DeviceTypeEnum;
import com.noqapp.domain.types.UserLevelEnum;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.service.DeviceRegistrationService;
import com.noqapp.mobile.service.exception.DeviceDetailMissingException;
import com.noqapp.mobile.view.common.ParseTokenFCM;
import com.noqapp.mobile.view.util.HttpRequestResponseParser;
import com.noqapp.search.elastic.service.GeoIPLocationService;
import com.noqapp.service.AccountService;

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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * hitender
 * 10/25/20 2:08 AM
 */
@SuppressWarnings ({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/api/m/dv")
public class DeviceRegistrationController {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceRegistrationController.class);

    private AuthenticateMobileService authenticateMobileService;
    private DeviceRegistrationService deviceRegistrationService;
    private GeoIPLocationService geoIPLocationService;
    private AccountService accountService;
    private ApiHealthService apiHealthService;

    @Autowired
    public DeviceRegistrationController(
        AuthenticateMobileService authenticateMobileService,
        DeviceRegistrationService deviceRegistrationService,
        GeoIPLocationService geoIPLocationService,
        AccountService accountService,
        ApiHealthService apiHealthService
    ) {
        this.authenticateMobileService = authenticateMobileService;
        this.deviceRegistrationService = deviceRegistrationService;
        this.geoIPLocationService = geoIPLocationService;
        this.accountService = accountService;
        this.apiHealthService = apiHealthService;
    }

    @PostMapping(
        value = "/registration",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String registration(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader (value = "X-R-AF")
        ScrubbedInput appFlavor,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        String tokenJson,

        HttpServletRequest request,
        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("All historical joined queue did={} dt={} mail={}", did, deviceType, mail);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        ParseTokenFCM parseTokenFCM = ParseTokenFCM.newInstance(tokenJson, request);
        if (StringUtils.isNotBlank(parseTokenFCM.getErrorResponse())) {
            return parseTokenFCM.getErrorResponse();
        }

        UserProfileEntity userProfile = accountService.findProfileByQueueUserId(qid);
        switch (userProfile.getLevel()) {
            case Q_SUPERVISOR:
            case S_MANAGER:
                //Only allow these level
                break;
            case M_ADMIN:
                LOG.warn("Denied access to business app is denied qid={} {} {}", qid, parseTokenFCM.getAppVersion(), parseTokenFCM.getOsVersion());
                return getErrorReason("Access denied. Admin does not has app access.", BUSINESS_APP_ACCESS_DENIED);
            default:
                LOG.warn("Denied access to business app is denied qid={} {} {}", qid, parseTokenFCM.getAppVersion(), parseTokenFCM.getOsVersion());
                return getErrorReason("Access denied. Please ask admin to authorize.", BUSINESS_APP_ACCESS_DENIED);
        }

        try {
            double[] coordinate =  parseTokenFCM.isMissingCoordinate()
                ? geoIPLocationService.getLocationAsDouble(
                    CommonUtil.retrieveIPV4(
                        parseTokenFCM.getIpAddress(),
                        HttpRequestResponseParser.getClientIpAddress(request)))
                : parseTokenFCM.getCoordinate();

            RegisteredDeviceEntity registeredDevice = deviceRegistrationService.lastAccessed(
                qid,
                did.getText(),
                parseTokenFCM.getTokenFCM(),
                parseTokenFCM.getModel(),
                parseTokenFCM.getOsVersion());

            if (null == registeredDevice) {
                try {
                    deviceRegistrationService.registerDevice(
                        qid,
                        did.getText(),
                        DeviceTypeEnum.valueOf(deviceType.getText()),
                        AppFlavorEnum.valueOf(appFlavor.getText()),
                        parseTokenFCM.getTokenFCM(),
                        parseTokenFCM.getModel(),
                        parseTokenFCM.getOsVersion(),
                        parseTokenFCM.getAppVersion(),
                        coordinate,
                        parseTokenFCM.getIpAddress()
                    );
                } catch (DeviceDetailMissingException e) {
                    LOG.error("Failed registration as cannot find did={} token={} reason={}", did, parseTokenFCM.getTokenFCM(), e.getLocalizedMessage(), e);
                    throw new DeviceDetailMissingException("Something went wrong. Please restart the app.");
                }
                LOG.info("Historical new device queue did={} qid={} deviceType={}", did, qid, deviceType);
            } else {
                if (StringUtils.isBlank(registeredDevice.getQueueUserId())) {
                    try {
                        /* Save with QID when missing in registered device. */
                        deviceRegistrationService.registerDevice(
                            qid,
                            did.getText(),
                            DeviceTypeEnum.valueOf(deviceType.getText()),
                            AppFlavorEnum.valueOf(appFlavor.getText()),
                            parseTokenFCM.getTokenFCM(),
                            parseTokenFCM.getModel(),
                            parseTokenFCM.getOsVersion(),
                            parseTokenFCM.getAppVersion(),
                            coordinate,
                            parseTokenFCM.getIpAddress()
                        );

                    } catch (DeviceDetailMissingException e) {
                        LOG.error("Failed registration as cannot find did={} reason={}", did, e.getLocalizedMessage(), e);
                        throw new DeviceDetailMissingException("Something went wrong. Please restart the app.");
                    }
                }
            }

            return new JsonResponse(true).asJson();
        } catch (DeviceDetailMissingException e) {
            LOG.error("Failed registering deviceType={}, reason={}", deviceType, e.getLocalizedMessage(), e);
            return getErrorReason("Missing device details", DEVICE_DETAIL_MISSING);
        } catch (Exception e) {
            LOG.error("Failed getting queue state qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/registration",
                "registration",
                DeviceRegistrationController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }
}
