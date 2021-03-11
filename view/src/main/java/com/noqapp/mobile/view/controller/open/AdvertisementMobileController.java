package com.noqapp.mobile.view.controller.open;

import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.domain.body.client.Location;
import com.noqapp.mobile.service.AdvertisementMobileService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Point;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;

/**
 * User: hitender
 * Date: 2019-05-16 10:11
 */
@SuppressWarnings ({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/open/vigyapan")
public class AdvertisementMobileController {
    private static final Logger LOG = LoggerFactory.getLogger(AdvertisementMobileController.class);

    private AdvertisementMobileService advertisementMobileService;
    private ApiHealthService apiHealthService;

    @Autowired
    public AdvertisementMobileController(
        AdvertisementMobileService advertisementMobileService,
        ApiHealthService apiHealthService
    ) {
        this.advertisementMobileService = advertisementMobileService;
        this.apiHealthService = apiHealthService;
    }

    /**
     * Tag every time store profile is displayed. For example doctor is associated to store, hence mark store is displayed.
     */
    @PostMapping(
        value = "/near",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String getAdvertisementsByLocation(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput deviceType,

        @RequestBody
        Location location
    ) {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Get advt near city=\"{}\" latitude={} longitude={} for request from did={} deviceType={}",
            location.getCityName(), location.getLatitude(), location.getLongitude(), did, deviceType);

        try {
            Point point = new Point(Double.parseDouble(location.getLongitude().getText()), Double.parseDouble(location.getLatitude().getText()));
            String advt = advertisementMobileService.findAllMobileApprovedAdvertisementsByLocation(point).asJson();
            LOG.info("Vigyapan shown \"{}\"", advt);

            return advt;
        } catch (Exception e) {
            LOG.error("Failed getting advt reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/near",
                "getAdvertisementsByLocation",
                AdvertisementMobileController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }
}
