package com.noqapp.mobile.view.controller.api.merchant.health;

import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.common.utils.CommonUtil.UNAUTHORIZED;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.MEDICAL_RECORD_ENTRY_DENIED;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.MOBILE_JSON;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.domain.body.merchant.FindMedicalProfile;
import com.noqapp.mobile.service.AccountMobileService;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.service.BusinessUserStoreService;

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

import javax.servlet.http.HttpServletResponse;

/**
 * hitender
 * 7/25/18 5:29 PM
 */
@SuppressWarnings({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/api/m/h/medicalUserProfile")
public class MedicalUserProfileController {
    private static final Logger LOG = LoggerFactory.getLogger(MedicalUserProfileController.class);

    private AuthenticateMobileService authenticateMobileService;
    private AccountMobileService accountMobileService;
    private BusinessUserStoreService businessUserStoreService;
    private ApiHealthService apiHealthService;

    @Autowired
    public MedicalUserProfileController(
        AuthenticateMobileService authenticateMobileService,
        AccountMobileService accountMobileService,
        BusinessUserStoreService businessUserStoreService,
        ApiHealthService apiHealthService
    ) {
        this.authenticateMobileService = authenticateMobileService;
        this.accountMobileService = accountMobileService;
        this.businessUserStoreService = businessUserStoreService;
        this.apiHealthService = apiHealthService;
    }

    /**
     * Fetch medical profile of client for merchant.
     * @return JsonProfile
     */
    @PostMapping(
        value = "/fetch",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String fetch(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        FindMedicalProfile findMedicalProfile,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Fetch mail={} did={} deviceType={} auth={}", mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/h/medicalUserProfile/fetch by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            if (StringUtils.isBlank(findMedicalProfile.getCodeQR())) {
                LOG.warn("Not a valid codeQR={} qid={}", findMedicalProfile.getCodeQR(), qid);
                return getErrorReason("Not a valid queue code.", MOBILE_JSON);
            } else if (!businessUserStoreService.hasAccess(qid, findMedicalProfile.getCodeQR())) {
                LOG.info("Your are not authorized to see medical profile of client mail={}", mail);
                return getErrorReason("Your are not authorized to see medical profile of client", MEDICAL_RECORD_ENTRY_DENIED);
            }

            return accountMobileService.getProfileForMedicalAsJson(findMedicalProfile.getQueueUserId()).asJson();
        } finally {
            apiHealthService.insert(
                "/fetch",
                "fetch",
                MedicalUserProfileController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }
}
