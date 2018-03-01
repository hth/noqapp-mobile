package com.noqapp.mobile.view.controller.api.client;

import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.json.JsonQueue;
import com.noqapp.domain.types.DeviceTypeEnum;
import com.noqapp.domain.types.TokenServiceEnum;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.service.QueueMobileService;
import com.noqapp.mobile.service.TokenQueueMobileService;
import com.noqapp.mobile.view.common.ParseTokenFCM;
import com.noqapp.service.InviteService;
import com.noqapp.common.utils.ScrubbedInput;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import static com.noqapp.common.utils.CommonUtil.UNAUTHORIZED;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.REMOTE_JOIN_EMPTY;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

/**
 * Remote scan of QR code is only available to registered user.
 *
 * User: hitender
 * Date: 3/31/17 7:23 PM
 */
@SuppressWarnings ({
        "PMD.BeanMembersShouldSerialize",
        "PMD.LocalVariableCouldBeFinal",
        "PMD.MethodArgumentCouldBeFinal",
        "PMD.LongVariable"
})
@RestController
@RequestMapping (value = "/api/c/token")
public class TokenQueueAPIController {
    private static final Logger LOG = LoggerFactory.getLogger(TokenQueueAPIController.class);

    private TokenQueueMobileService tokenQueueMobileService;
    private QueueMobileService queueMobileService;
    private InviteService inviteService;
    private AuthenticateMobileService authenticateMobileService;
    private ApiHealthService apiHealthService;

    @Autowired
    public TokenQueueAPIController(
            TokenQueueMobileService tokenQueueMobileService,
            QueueMobileService queueMobileService,
            InviteService inviteService,
            AuthenticateMobileService authenticateMobileService,
            ApiHealthService apiHealthService
    ) {
        this.tokenQueueMobileService = tokenQueueMobileService;
        this.queueMobileService = queueMobileService;
        this.inviteService = inviteService;
        this.authenticateMobileService = authenticateMobileService;
        this.apiHealthService = apiHealthService;
    }

    /**
     * Get state of queue at the store.
     *
     * @param did
     * @param deviceType
     * @param codeQR
     * @param response
     * @return
     * @throws IOException
     */
    @GetMapping(
            value = "/{codeQR}",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String getQueueState(
            @RequestHeader ("X-R-DID")
            ScrubbedInput did,

            @RequestHeader ("X-R-DT")
            ScrubbedInput deviceType,

            @RequestHeader ("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            @PathVariable ("codeQR")
            ScrubbedInput codeQR,

            HttpServletResponse response
    ) throws IOException {
        Instant start = Instant.now();
        LOG.info("On scan get state did={} dt={} codeQR={}", did, deviceType, codeQR);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        if (!tokenQueueMobileService.isValidCodeQR(codeQR.getText())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid token");
            return null;
        }

        try {
            return tokenQueueMobileService.findTokenState(codeQR.getText()).asJson();
        } catch (Exception e) {
            LOG.error("Failed getting queue state qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            apiHealthService.insert(
                    "/{codeQR}",
                    "getQueueState",
                    TokenQueueAPIController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.F);
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                    "/{codeQR}",
                    "getQueueState",
                    TokenQueueAPIController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.G);
        }
    }

    /**
     * Get all state of queue at a Business when one QR Code is scanned.
     *
     * @param did
     * @param deviceType
     * @param codeQR
     * @param response
     * @return
     * @throws IOException
     */
    @GetMapping (
            value = "/v1/{codeQR}",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String getAllQueueState(
            @RequestHeader ("X-R-DID")
            ScrubbedInput did,

            @RequestHeader ("X-R-DT")
            ScrubbedInput deviceType,

            @RequestHeader ("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            @PathVariable ("codeQR")
            ScrubbedInput codeQR,

            HttpServletResponse response
    ) throws IOException {
        Instant start = Instant.now();
        LOG.info("On scan get all state did={} dt={} codeQR={}", did, deviceType, codeQR);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        if (!tokenQueueMobileService.isValidCodeQR(codeQR.getText())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid token");
            return null;
        }

        try {
            return tokenQueueMobileService.findAllQueuesByBizNameCodeQR(codeQR.getText()).asJson();
        } catch (Exception e) {
            LOG.error("Failed getting all queue state qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            apiHealthService.insert(
                    "/v1/{codeQR}",
                    "getAllQueueState",
                    TokenQueueAPIController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.F);
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                    "/v1/{codeQR}",
                    "getAllQueueState",
                    TokenQueueAPIController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.G);
        }
    }

    /**
     * Get all the queues user has token from. In short all the queues user has joined.
     *
     * @param did
     * @param dt
     * @param response
     * @return
     * @throws IOException
     */
    @GetMapping (
            value = "/queues",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String getAllJoinedQueues(
            @RequestHeader ("X-R-DID")
            ScrubbedInput did,

            @RequestHeader ("X-R-DT")
            ScrubbedInput dt,

            @RequestHeader ("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            HttpServletResponse response
    ) throws IOException {
        Instant start = Instant.now();
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        try {
            return queueMobileService.findAllJoinedQueues(qid, did.getText()).asJson();
        } catch (Exception e) {
            LOG.error("Failed getting queues qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            apiHealthService.insert(
                    "/queues",
                    "getAllJoinedQueues",
                    TokenQueueAPIController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.F);
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                    "/queues",
                    "getAllJoinedQueues",
                    TokenQueueAPIController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.G);
        }
    }
    
    /**
     * Get all the historical queues user has token from. In short all the queues user has joined in past.
     *
     * @param did
     * @param deviceType
     * @param response
     * @return
     * @throws IOException
     */
    @PostMapping(
            value = "/historical",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String allHistoricalJoinedQueues(
            @RequestHeader ("X-R-DID")
            ScrubbedInput did,

            @RequestHeader ("X-R-DT")
            ScrubbedInput deviceType,

            @RequestHeader ("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            @RequestBody
            String tokenJson,

            HttpServletResponse response
    ) throws IOException {
        Instant start = Instant.now();
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        ParseTokenFCM parseTokenFCM = ParseTokenFCM.newInstance(tokenJson);
        if (StringUtils.isNotBlank(parseTokenFCM.getErrorResponse())) {
            return parseTokenFCM.getErrorResponse();
        }

        try {
            return queueMobileService.findHistoricalQueue(
                    qid,
                    did.getText(),
                    DeviceTypeEnum.valueOf(deviceType.getText()),
                    parseTokenFCM.getTokenFCM()).asJson();
        } catch (Exception e) {
            LOG.error("Failed getting history qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            apiHealthService.insert(
                    "/historical",
                    "allHistoricalJoinedQueues",
                    TokenQueueAPIController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.F);
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                    "/historical",
                    "allHistoricalJoinedQueues",
                    TokenQueueAPIController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.G);
        }
    }

    /**
     * Join the queue.
     *
     * @param did
     * @param deviceType
     * @param codeQR
     * @param response
     * @return
     * @throws IOException
     */
    @PostMapping (
            value = "/queue/{codeQR}",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String joinQueue(
            @RequestHeader ("X-R-DID")
            ScrubbedInput did,

            @RequestHeader ("X-R-DT")
            ScrubbedInput deviceType,

            @RequestHeader ("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            @PathVariable ("codeQR")
            ScrubbedInput codeQR,

            HttpServletResponse response
    ) throws IOException {
        Instant start = Instant.now();
        LOG.info("Join queue did={} dt={} codeQR={}", did, deviceType, codeQR);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        BizStoreEntity bizStore = tokenQueueMobileService.getBizService().findByCodeQR(codeQR.getText());
        if (null == bizStore) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid QR Code");
            return null;
        }

        try {
            return tokenQueueMobileService.joinQueue(
                    codeQR.getText(),
                    did.getText(),
                    qid,
                    bizStore.getAverageServiceTime(),
                    TokenServiceEnum.C).asJson();
        } catch (Exception e) {
            LOG.error("Failed joining queue qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            apiHealthService.insert(
                    "/queue/{codeQR}",
                    "joinQueue",
                    TokenQueueAPIController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.F);
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                    "/queue/{codeQR}",
                    "joinQueue",
                    TokenQueueAPIController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.G);
        }
    }

    /**
     * Abort the queue. App should un-subscribe user from topic.
     *
     * @param did
     * @param deviceType
     * @param codeQR
     * @param response
     * @return
     * @throws IOException
     */
    @PostMapping (
            value = "/abort/{codeQR}",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String abortQueue(
            @RequestHeader ("X-R-DID")
            ScrubbedInput did,

            @RequestHeader ("X-R-DT")
            ScrubbedInput deviceType,

            @RequestHeader ("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            @PathVariable ("codeQR")
            ScrubbedInput codeQR,

            HttpServletResponse response
    ) throws IOException {
        Instant start = Instant.now();
        LOG.info("Abort queue did={} dt={} codeQR={}", did, deviceType, codeQR);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        if (!tokenQueueMobileService.getBizService().isValidCodeQR(codeQR.getText())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid token");
            return null;
        }

        try {
            return tokenQueueMobileService.abortQueue(codeQR.getText(), did.getText(), qid).asJson();
        } catch (Exception e) {
            LOG.error("Failed aborting queue rid={}, reason={}", qid, e.getLocalizedMessage(), e);
            apiHealthService.insert(
                    "/abort/{codeQR}",
                    "abortQueue",
                    TokenQueueAPIController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.F);
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                    "/abort/{codeQR}",
                    "abortQueue",
                    TokenQueueAPIController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.G);
        }
    }

    /**
     * Remote scan of QR Code. 
     *
     * @param did
     * @param deviceType
     * @param codeQR
     * @param response
     * @return
     * @throws IOException
     */
    @PostMapping (
            value = "/remote/{codeQR}",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String remoteScanQueueState(
            @RequestHeader ("X-R-DID")
            ScrubbedInput did,

            @RequestHeader ("X-R-DT")
            ScrubbedInput deviceType,

            @RequestHeader ("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            @PathVariable ("codeQR")
            ScrubbedInput codeQR,

            HttpServletResponse response
    ) throws IOException {
        Instant start = Instant.now();
        LOG.info("On remote scan get state did={} dt={} codeQR={}", did, deviceType, codeQR);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        if (!tokenQueueMobileService.getBizService().isValidCodeQR(codeQR.getText())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid token");
            return null;
        }

        try {
            JsonQueue jsonQueue = tokenQueueMobileService.findTokenState(codeQR.getText());
            int remoteJoinCount = inviteService.getRemoteJoinCount(qid);
            LOG.info("Found available remote join for qid={} is remoteJoinCount={}", qid, remoteJoinCount);
            jsonQueue.setRemoteJoinCount(remoteJoinCount);
            return jsonQueue.asJson();
        } catch (Exception e) {
            LOG.error("Failed getting queue state qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            apiHealthService.insert(
                    "/remote/{codeQR}",
                    "remoteScanQueueState",
                    TokenQueueAPIController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.F);
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                    "/remote/{codeQR}",
                    "remoteScanQueueState",
                    TokenQueueAPIController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.G);
        }
    }

    /**
     * Join the queue remotely. Only for registered user and who has number of remote join available.
     *
     * @param did
     * @param deviceType
     * @param codeQR
     * @param response
     * @return
     * @throws IOException
     */
    @PostMapping (
            value = "/remote/queue/{codeQR}",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String remoteJoinQueue(
            @RequestHeader ("X-R-DID")
            ScrubbedInput did,

            @RequestHeader ("X-R-DT")
            ScrubbedInput deviceType,

            @RequestHeader ("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            @PathVariable ("codeQR")
            ScrubbedInput codeQR,

            HttpServletResponse response
    ) throws IOException {
        Instant start = Instant.now();
        LOG.info("Join queue did={} dt={} codeQR={}", did, deviceType, codeQR);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        BizStoreEntity bizStore = tokenQueueMobileService.getBizService().findByCodeQR(codeQR.getText());
        if (null == bizStore) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid token");
            return null;
        }

        try {
            if (0 < inviteService.getRemoteJoinCount(qid)) {
                String jsonToken = tokenQueueMobileService.joinQueue(
                        codeQR.getText(),
                        did.getText(),
                        qid,
                        bizStore.getAverageServiceTime(),
                        TokenServiceEnum.C).asJson();
                inviteService.deductRemoteJoinCount(qid);
                return jsonToken;
            } else {
                LOG.warn("Failed joining queue rid={}, remoteJoin={}, means not available", qid, 0);
                return getErrorReason("Remote Join not available.", REMOTE_JOIN_EMPTY);
            }
        } catch (Exception e) {
            LOG.error("Failed joining queue rid={}, reason={}", qid, e.getLocalizedMessage(), e);
            apiHealthService.insert(
                    "/remote/queue/{codeQR}",
                    "remoteJoinQueue",
                    TokenQueueAPIController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.F);
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                    "/remote/queue/{codeQR}",
                    "remoteJoinQueue",
                    TokenQueueAPIController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.G);
        }
    }

    static boolean authorizeRequest(HttpServletResponse response, String qid) throws IOException {
        if (null == qid) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return true;
        }
        return false;
    }
}
