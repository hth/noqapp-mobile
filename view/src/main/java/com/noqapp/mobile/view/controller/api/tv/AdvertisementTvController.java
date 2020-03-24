package com.noqapp.mobile.view.controller.api.tv;

import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.common.utils.CommonUtil.UNAUTHORIZED;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.MOBILE_JSON;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BusinessUserStoreEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.json.JsonResponse;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.service.AdvertisementMobileService;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.service.AccountService;
import com.noqapp.service.BusinessUserStoreService;
import com.noqapp.service.ProfessionalProfileService;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import javax.servlet.http.HttpServletResponse;

/**
 * hitender
 * 2018-12-20 10:21
 */
@SuppressWarnings ({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/api/tv/vigyapan")
public class AdvertisementTvController {
    private static final Logger LOG = LoggerFactory.getLogger(AdvertisementTvController.class);

    private AdvertisementMobileService advertisementMobileService;
    private BusinessUserStoreService businessUserStoreService;
    private ProfessionalProfileService professionalProfileService;
    private AuthenticateMobileService authenticateMobileService;
    private AccountService accountService;
    private ApiHealthService apiHealthService;

    @Autowired
    public AdvertisementTvController(
        AdvertisementMobileService advertisementMobileService,
        BusinessUserStoreService businessUserStoreService,
        ProfessionalProfileService professionalProfileService,
        AuthenticateMobileService authenticateMobileService,
        AccountService accountService,
        ApiHealthService apiHealthService
    ) {
        this.advertisementMobileService = advertisementMobileService;
        this.businessUserStoreService = businessUserStoreService;
        this.professionalProfileService = professionalProfileService;
        this.authenticateMobileService = authenticateMobileService;
        this.accountService = accountService;
        this.apiHealthService = apiHealthService;
    }

    /** Tag every time store profile is displayed. For example doctor is associated to store, hence mark store is displayed. */
    @PostMapping(
        value = "/tsd/{codeQR}",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String tagStoreAsDisplayed(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @PathVariable("codeQR")
        ScrubbedInput codeQR,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Displayed advt for codeQR={} request from mail={} did={} deviceType={} auth={}",
            codeQR,
            mail,
            did,
            deviceType,
            AUTH_KEY_HIDDEN);

        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/tv/vigyapan/tsd by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        if (StringUtils.isBlank(codeQR.getText())) {
            LOG.warn("Not a valid codeQR={} qid={}", codeQR.getText(), qid);
            return getErrorReason("Not a valid queue code.", MOBILE_JSON);
        } else if (!businessUserStoreService.hasAccess(qid, codeQR.getText())) {
            LOG.info("Un-authorized store access to /api/tv/vigyapan/tsd by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            advertisementMobileService.tagStoreAsDisplayed(codeQR.getText());
            return new JsonResponse(true).asJson();
        } catch (Exception e) {
            LOG.error("Failed marking displayed advt reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/tsd/{codeQR}",
                "tagStoreAsDisplayed",
                AdvertisementTvController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Tag every time store profile is displayed. For example doctor is associated to store, hence mark store is displayed. */
    @GetMapping(
        value = "/all",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String getAllAdvertisements(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Get all advt for request from mail={} did={} deviceType={} auth={}",
            mail,
            did,
            deviceType,
            AUTH_KEY_HIDDEN);

        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/tv/vigyapan/all by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            UserProfileEntity userProfile = accountService.findProfileByQueueUserId(qid);
            BusinessUserStoreEntity businessUserStore = businessUserStoreService.findUserManagingStoreWithUserLevel(qid, userProfile.getLevel());
            return advertisementMobileService.findAllMobileTVApprovedAdvertisements(businessUserStore.getBizNameId()).asJson();
        } catch (Exception e) {
            LOG.error("Failed getting advt reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/all",
                "getAllMobileAdvertisements",
                AdvertisementTvController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Gets all professional profile. */
    @GetMapping(
        value = "/professionalProfiles",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String getProfessionalProfiles(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Get all advt for request from mail={} did={} deviceType={} auth={}",
            mail,
            did,
            deviceType,
            AUTH_KEY_HIDDEN);

        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/tv/vigyapan/professionalProfiles by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            UserProfileEntity userProfile = accountService.findProfileByQueueUserId(qid);
            BusinessUserStoreEntity businessUserStore = businessUserStoreService.findUserManagingStoreWithUserLevel(qid, userProfile.getLevel());
            return professionalProfileService.findAllProfessionalProfile(businessUserStore.getBizNameId()).asJson();
        } catch (Exception e) {
            LOG.error("Failed getting advt reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/professionalProfiles",
                "professionalProfiles",
                AdvertisementTvController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }
}
