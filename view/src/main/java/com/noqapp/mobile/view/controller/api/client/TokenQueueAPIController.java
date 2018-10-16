package com.noqapp.mobile.view.controller.api.client;

import static com.noqapp.common.utils.CommonUtil.UNAUTHORIZED;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.DEVICE_DETAIL_MISSING;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.json.JsonTokenAndQueueList;
import com.noqapp.domain.types.AppFlavorEnum;
import com.noqapp.domain.types.DeviceTypeEnum;
import com.noqapp.domain.types.TokenServiceEnum;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.domain.body.client.JoinQueue;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.service.QueueMobileService;
import com.noqapp.mobile.service.TokenQueueMobileService;
import com.noqapp.mobile.service.exception.DeviceDetailMissingException;
import com.noqapp.mobile.view.common.ParseTokenFCM;
import com.noqapp.service.PurchaseOrderService;

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

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import javax.servlet.http.HttpServletResponse;

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
    private AuthenticateMobileService authenticateMobileService;
    private PurchaseOrderService purchaseOrderService;
    private ApiHealthService apiHealthService;

    @Autowired
    public TokenQueueAPIController(
            TokenQueueMobileService tokenQueueMobileService,
            QueueMobileService queueMobileService,
            AuthenticateMobileService authenticateMobileService,
            PurchaseOrderService purchaseOrderService,
            ApiHealthService apiHealthService
    ) {
        this.tokenQueueMobileService = tokenQueueMobileService;
        this.queueMobileService = queueMobileService;
        this.authenticateMobileService = authenticateMobileService;
        this.purchaseOrderService = purchaseOrderService;
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
            return tokenQueueMobileService.findAllBizStoreByBizNameCodeQR(codeQR.getText()).asJson();
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
     * Get all the queues user has token from. In short all the queues user has joined AND/OR all placed orders.
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
            JsonTokenAndQueueList jsonTokenAndQueues = queueMobileService.findAllJoinedQueues(qid, did.getText());
            jsonTokenAndQueues.getTokenAndQueues().addAll(purchaseOrderService.findAllOpenOrderAsJson(qid));
            return jsonTokenAndQueues.asJson();
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
     * Get all the historical queues user has token from. In short all the queues and order user has joined in past.
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
    @Deprecated
    public String allHistoricalJoinedQueues(
            @RequestHeader ("X-R-DID")
            ScrubbedInput did,

            @RequestHeader ("X-R-DT")
            ScrubbedInput deviceType,

            @RequestHeader (value = "X-R-AF", required = false, defaultValue = "NQMT")
            ScrubbedInput appFlavor,

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
            JsonTokenAndQueueList jsonTokenAndQueues = queueMobileService.findHistoricalQueue(
                    qid,
                    did.getText(),
                    DeviceTypeEnum.valueOf(deviceType.getText()),
                    AppFlavorEnum.valueOf(appFlavor.getText()),
                    parseTokenFCM.getTokenFCM(),
                    parseTokenFCM.getModel(),
                    parseTokenFCM.getOsVersion());
            //TODO(hth) get old historical order, it just gets todays historical order
            jsonTokenAndQueues.getTokenAndQueues().addAll(purchaseOrderService.findAllDeliveredHistoricalOrderAsJson(qid));
            return jsonTokenAndQueues.asJson();
        } catch (DeviceDetailMissingException e) {
            LOG.error("Failed registering deviceType={}, reason={}", deviceType, e.getLocalizedMessage(), e);
            return getErrorReason("Missing device details", DEVICE_DETAIL_MISSING);
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
     */
    @PostMapping (
            value = "/queue",
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

            @RequestBody
            JoinQueue joinQueue,

            HttpServletResponse response
    ) throws IOException {
        Instant start = Instant.now();
        LOG.info("Join queue did={} dt={}", did, deviceType);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        BizStoreEntity bizStore = tokenQueueMobileService.getBizService().findByCodeQR(joinQueue.getCodeQR());
        if (null == bizStore) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid QR Code");
            return null;
        }

        try {
            LOG.info("codeQR={} qid={} guardianQid={}", joinQueue.getCodeQR(), joinQueue.getQueueUserId(), joinQueue.getGuardianQid());
            return tokenQueueMobileService.joinQueue(
                    joinQueue.getCodeQR(),
                    did.getText(),
                    joinQueue.getQueueUserId(),
                    joinQueue.getGuardianQid(),
                    bizStore.getAverageServiceTime(),
                    TokenServiceEnum.C).asJson();
        } catch (Exception e) {
            LOG.error("Failed joining queue qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            apiHealthService.insert(
                    "/queue",
                    "joinQueue",
                    TokenQueueAPIController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.F);
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                    "/queue",
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
            LOG.error("Failed aborting queue qid={}, reason={}", qid, e.getLocalizedMessage(), e);
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

    static boolean authorizeRequest(HttpServletResponse response, String qid) throws IOException {
        if (null == qid) {
            LOG.warn("Login required qid={}", qid);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return true;
        }
        return false;
    }
}
