package com.noqapp.mobile.view.controller.api.merchant.health;

import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.common.utils.CommonUtil.UNAUTHORIZED;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.types.BusinessTypeEnum;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.service.BizService;
import com.noqapp.service.PreferredBusinessService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import javax.servlet.http.HttpServletResponse;

/**
 * User: hitender
 * Date: 8/12/18 2:49 PM
 */
@SuppressWarnings({
        "PMD.BeanMembersShouldSerialize",
        "PMD.LocalVariableCouldBeFinal",
        "PMD.MethodArgumentCouldBeFinal",
        "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/api/m/h/preferredStore")
public class PreferredStoreController {
    private static final Logger LOG = LoggerFactory.getLogger(PreferredStoreController.class);

    private AuthenticateMobileService authenticateMobileService;
    private BizService bizService;
    private PreferredBusinessService preferredBusinessService;
    private ApiHealthService apiHealthService;

    @Autowired
    public PreferredStoreController(
        AuthenticateMobileService authenticateMobileService,
        BizService bizService,
        PreferredBusinessService preferredBusinessService,
        ApiHealthService apiHealthService
    ) {
        this.authenticateMobileService = authenticateMobileService;
        this.bizService = bizService;
        this.preferredBusinessService = preferredBusinessService;
        this.apiHealthService = apiHealthService;
    }

    /** Gets preferred business stores if any for business type. */
    @GetMapping(
            value = "/{businessType}/{codeQR}",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String getPreferredStoresByBusinessType(
            @RequestHeader("X-R-DID")
            ScrubbedInput did,

            @RequestHeader ("X-R-DT")
            ScrubbedInput deviceType,

            @RequestHeader ("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            @PathVariable("businessType")
            ScrubbedInput businessType,

            @PathVariable("codeQR")
            ScrubbedInput codeQR,

            HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Fetch mail={} did={} deviceType={} auth={}", mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/h/preferredStore/{businessType}/{codeQR} by {} {} mail={}", businessType.getText(), codeQR.getText(), mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            BizStoreEntity bizStore = bizService.findByCodeQR(codeQR.getText());
            return preferredBusinessService.findAllAsJson(bizStore, BusinessTypeEnum.valueOf(businessType.getText())).asJson();
        } catch (Exception e) {
            LOG.error("Failed getting preferred store qid={} code={} message={}", qid, codeQR.getText(), e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/api/m/h/preferredStore/{businessType}/{codeQR}",
                "getPreferredStoresByBusinessType",
                PreferredStoreController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Gets all preferred business stores. */
    @GetMapping(
            value = "/{codeQR}",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String getAllPreferredStores(
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
        LOG.info("Fetch mail={} did={} deviceType={} auth={}", mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/h/preferredStore/{codeQR} by {} mail={}", codeQR.getText(), mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            BizStoreEntity bizStore = bizService.findByCodeQR(codeQR.getText());
            return preferredBusinessService.findAllAsJson(bizStore).asJson();
        } catch (Exception e) {
            LOG.error("Failed getting preferred store qid={} code={} message={}", qid, codeQR.getText(), e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/api/m/h/preferredStore/{codeQR}",
                "getAllPreferredStores",
                PreferredStoreController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }
}
