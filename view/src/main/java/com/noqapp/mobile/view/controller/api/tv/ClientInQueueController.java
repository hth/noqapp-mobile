package com.noqapp.mobile.view.controller.api.tv;

import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.common.utils.CommonUtil.UNAUTHORIZED;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MOBILE_JSON;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.json.tv.JsonQueueTVList;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.domain.body.tv.QueueDetail;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.service.tv.QueueTVService;
import com.noqapp.service.BusinessUserStoreService;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
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
 * 10/10/18 9:26 AM
 */
@SuppressWarnings ({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/api/tv/queue")
public class ClientInQueueController {
    private static final Logger LOG = LoggerFactory.getLogger(ClientInQueueController.class);

    private QueueTVService queueTVService;
    private AuthenticateMobileService authenticateMobileService;
    private BusinessUserStoreService businessUserStoreService;
    private ApiHealthService apiHealthService;

    @Autowired
    public ClientInQueueController(
        QueueTVService queueTVService,
        AuthenticateMobileService authenticateMobileService,
        BusinessUserStoreService businessUserStoreService,
        ApiHealthService apiHealthService
    ) {
        this.queueTVService = queueTVService;
        this.authenticateMobileService = authenticateMobileService;
        this.businessUserStoreService = businessUserStoreService;
        this.apiHealthService = apiHealthService;
    }

    /**
     * List all clients in queue.
     */
    @PostMapping(
        value = "/toBeServedClients/{codeQR}",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String toBeServedClients(
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
        LOG.info("Clients shown for codeQR={} request from mail={} did={} deviceType={} auth={}",
            codeQR,
            mail,
            did,
            deviceType,
            AUTH_KEY_HIDDEN);

        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/tv/queue/toBeServedClients by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        if (StringUtils.isBlank(codeQR.getText())) {
            LOG.warn("Not a valid codeQR={} qid={}", codeQR.getText(), qid);
            return getErrorReason("Not a valid queue code.", MOBILE_JSON);
        } else if (!businessUserStoreService.hasAccess(qid, codeQR.getText())) {
            LOG.info("Un-authorized store access to /api/tv/queue/toBeServedClients by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            return queueTVService.findAllActiveInQueue(codeQR.getText()).asJson();
        } catch (Exception e) {
            LOG.error("Failed getting toBeServed clients reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/toBeServedClients/{codeQR}",
                "toBeServedClients",
                ClientInQueueController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /**
     * List all clients in queue.
     */
    @PostMapping(
        value = "/toBeServedClients",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String toBeServedClients(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        QueueDetail queueDetail,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Clients shown for all request from mail={} did={} deviceType={} auth={}",
            mail,
            did,
            deviceType,
            AUTH_KEY_HIDDEN);

        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/tv/queue/toBeServedClients by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        String codeQR;
        if (queueDetail.getCodeQRs().isEmpty()) {
            return new JsonQueueTVList().asJson();
        } else {
            codeQR = queueDetail.getCodeQRs().get(0);
        }

        if (StringUtils.isBlank(codeQR)) {
            LOG.warn("Not a valid codeQR={} qid={}", codeQR, qid);
            return getErrorReason("Not a valid queue code.", MOBILE_JSON);
        } else if (!businessUserStoreService.hasAccess(qid, codeQR)) {
            LOG.info("Un-authorized store access to /api/tv/queue/toBeServedClients by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            return queueTVService.findAllActiveInQueue(queueDetail.getCodeQRs());
        } catch (Exception e) {
            LOG.error("Failed getting toBeServed clients reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/toBeServedClients/{codeQR}",
                "toBeServedClients",
                ClientInQueueController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }
}
