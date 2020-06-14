package com.noqapp.mobile.view.controller.api.client;

import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.QRCODE_DENIED_ACCESS;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.QRCODE_INVALID;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.common.utils.CommonUtil.UNAUTHORIZED;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.QueueEntity;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.domain.JsonInQueuePerson;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.repository.QueueManager;
import com.noqapp.service.BusinessUserStoreService;
import com.noqapp.service.JoinAbortService;

import org.apache.commons.lang3.StringUtils;

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
 * hitender
 * 6/4/20 11:36 AM
 */
@SuppressWarnings ({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/api/c/a")
public class AuthenticateClientInQueue {
    private static final Logger LOG = LoggerFactory.getLogger(AuthenticateClientInQueue.class);

    private QueueManager queueManager;
    private AuthenticateMobileService authenticateMobileService;
    private BusinessUserStoreService businessUserStoreService;
    private JoinAbortService joinAbortService;
    private ApiHealthService apiHealthService;

    @Autowired
    public AuthenticateClientInQueue(
        QueueManager queueManager,
        AuthenticateMobileService authenticateMobileService,
        BusinessUserStoreService businessUserStoreService,
        JoinAbortService joinAbortService,
        ApiHealthService apiHealthService
    ) {
        this.queueManager = queueManager;
        this.authenticateMobileService = authenticateMobileService;
        this.businessUserStoreService = businessUserStoreService;
        this.joinAbortService = joinAbortService;
        this.apiHealthService = apiHealthService;
    }

    @GetMapping(
        value = "/inQueue/{codeQR}/{token}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String clientInQueue(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput dt,

        @RequestHeader("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @PathVariable("codeQR")
        ScrubbedInput codeQR,

        @PathVariable("token")
        ScrubbedInput token,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Queue state associated with mail={} did={} deviceType={} auth={}", mail, did, dt, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/c/a/inQueue/{codeQR}/{token} by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        if (StringUtils.isBlank(codeQR.getText())) {
            LOG.warn("Not a valid codeQR={} qid={}", codeQR.getText(), qid);
            return getErrorReason("Not a valid code.", QRCODE_INVALID);
        } else if (!businessUserStoreService.hasAccess(qid, codeQR.getText())) {
            LOG.info("Un-authorized store access to /api/c/a/inQueue/{codeQR}/{token} by mail={}", mail);
            return getErrorReason("Ask admin to correctly set permission to scan this code", QRCODE_DENIED_ACCESS);
        }

        try {
            JsonInQueuePerson jsonInQueuePerson = new JsonInQueuePerson();
            QueueEntity queue = queueManager.findOne(codeQR.getText(), Integer.parseInt(token.getText()));
            if (null == queue) {
                LOG.warn("Valid but no data associated in queue for codeQR={} qid={}", codeQR.getText(), qid);
                return getErrorReason("Not a valid code.", QRCODE_INVALID);
            }

            jsonInQueuePerson
                .setToken(queue.getTokenNumber())
                .setCustomerName(queue.getCustomerName())
                .setCustomerPhone(queue.getCustomerPhone())
                .setBusinessCustomerId(queue.getBusinessCustomerId())
                .setDisplayName(queue.getDisplayName())
                .setQueueUserState(queue.getQueueUserState())
                .setExpectedServiceBegin(queue.getExpectedServiceBegin())
                .setCustomerPriorityLevel(queue.getCustomerPriorityLevel())
                .setTransactionId(queue.getTransactionId())
                .setCreated(queue.getCreated());

            joinAbortService.authenticateMessageToClient(
                StringUtils.isBlank(queue.getGuardianQid()) ? queue.getQueueUserId() : queue.getGuardianQid(),
                "Your token " + queue.getTokenNumber(),
                "Current state " + queue.getQueueUserState(),
                queue.getCodeQR()
            );

            return jsonInQueuePerson.asJson();
        } catch (Exception e) {
            LOG.error("Failed authenticating client in queue for reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/inQueue/{codeQR}/{token}",
                "clientInQueue",
                AuthenticateClientInQueue.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }
}
