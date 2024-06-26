package com.noqapp.mobile.view.controller.open;

import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.DEVICE_DETAIL_MISSING;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.MOBILE_UPGRADE;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.QUEUE_ORDER_ABORT_EXPIRED_LIMITED_TIME;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.STORE_NO_LONGER_EXISTS;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;
import static org.apiguardian.api.API.Status.DEPRECATED;

import com.noqapp.common.utils.CommonUtil;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.types.AppFlavorEnum;
import com.noqapp.domain.types.DeviceTypeEnum;
import com.noqapp.domain.types.TokenServiceEnum;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.service.QueueMobileService;
import com.noqapp.mobile.service.TokenQueueMobileService;
import com.noqapp.mobile.service.exception.DeviceDetailMissingException;
import com.noqapp.mobile.view.common.ParseTokenFCM;
import com.noqapp.mobile.view.util.HttpRequestResponseParser;
import com.noqapp.search.elastic.helper.IpCoordinate;
import com.noqapp.search.elastic.service.GeoIPLocationService;
import com.noqapp.service.JoinAbortService;
import com.noqapp.service.QueueService;
import com.noqapp.service.exceptions.QueueAbortPaidPastDurationException;
import com.noqapp.service.exceptions.StoreNoLongerExistsException;

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

import org.apiguardian.api.API;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * User: hitender
 * Date: 11/17/16 3:12 PM
 */
@SuppressWarnings ({
        "PMD.BeanMembersShouldSerialize",
        "PMD.LocalVariableCouldBeFinal",
        "PMD.MethodArgumentCouldBeFinal",
        "PMD.LongVariable"
})
@RestController
@RequestMapping (value = "/open/token")
public class TokenQueueController {
    private static final Logger LOG = LoggerFactory.getLogger(TokenQueueController.class);

    private TokenQueueMobileService tokenQueueMobileService;
    private JoinAbortService joinAbortService;
    private QueueMobileService queueMobileService;
    private GeoIPLocationService geoIPLocationService;
    private QueueService queueService;
    private ApiHealthService apiHealthService;

    @Autowired
    public TokenQueueController(
        TokenQueueMobileService tokenQueueMobileService,
        JoinAbortService joinAbortService,
        QueueMobileService queueMobileService,
        GeoIPLocationService geoIPLocationService,
        QueueService queueService,
        ApiHealthService apiHealthService
    ) {
        this.tokenQueueMobileService = tokenQueueMobileService;
        this.joinAbortService = joinAbortService;
        this.queueMobileService = queueMobileService;
        this.geoIPLocationService = geoIPLocationService;
        this.queueService = queueService;
        this.apiHealthService = apiHealthService;
    }

    /** Get state of queue at the store. */
    @GetMapping (
        value = "/{codeQR}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String getQueueState(
        @RequestHeader ("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput deviceType,

        @PathVariable ("codeQR")
        ScrubbedInput codeQR,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("On scan get state did={} dt={} codeQR={}", did, deviceType, codeQR);
        if (!tokenQueueMobileService.getBizService().isValidCodeQR(codeQR.getText())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid token");
            return null;
        }

        try {
            return queueService.findTokenState(codeQR.getText()).asJson();
        } catch (StoreNoLongerExistsException e) {
            LOG.info("Store no longer exists reason={}", e.getLocalizedMessage(), e);
            apiHealthService.insert(
                "/{codeQR}",
                "getQueueState",
                TokenQueueController.class.getName(),
                Duration.between(start, Instant.now()),
                HealthStatusEnum.G);
            return getErrorReason("Store is not available.", STORE_NO_LONGER_EXISTS);
        } catch (Exception e) {
            LOG.error("Failed getting queue state did={} reason={}", did, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/{codeQR}",
                "getQueueState",
                TokenQueueController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Get all state of queue at a Business when one QR Code is scanned. */
    @GetMapping(
        value = "/v1/{codeQR}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String getAllQueueState(
        @RequestHeader ("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput deviceType,

        @PathVariable ("codeQR")
        ScrubbedInput codeQR,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("On scan get all state did={} dt={} codeQR={}", did, deviceType, codeQR);
        if (!tokenQueueMobileService.getBizService().isValidBizNameCodeQR(codeQR.getText()) && !tokenQueueMobileService.getBizService().isValidCodeQR(codeQR.getText())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid token");
            return null;
        }

        try {
            return tokenQueueMobileService.findAllBizStoreByBizNameCodeQR(codeQR.getText()).asJson();
        } catch (Exception e) {
            LOG.error("Failed getting all queue state did={} reason={}", did, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/v1/{codeQR}",
                "getAllQueueState",
                TokenQueueController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Get all state of queue at a Business when one QR Code of store is scanned. */
    @GetMapping(
        value = "/levelUp/{codeQR}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String levelUp(
        @RequestHeader ("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput deviceType,

        @PathVariable ("codeQR")
        ScrubbedInput codeQR,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("On scan get all state from store codeQR did={} dt={} codeQR={}", did, deviceType, codeQR);
        if (!tokenQueueMobileService.getBizService().isValidBizNameCodeQR(codeQR.getText()) && !tokenQueueMobileService.getBizService().isValidCodeQR(codeQR.getText())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid token");
            return null;
        }

        try {
            BizStoreEntity bizStore = tokenQueueMobileService.getBizService().findByCodeQR(codeQR.getText());
            switch (bizStore.getBusinessType()) {
                case BK:
                    return tokenQueueMobileService.findAllBizStoreByAddress(bizStore).asJson();
                default:
                    return tokenQueueMobileService.findAllBizStoreByBizNameCodeQR(bizStore.getBizName().getCodeQR()).asJson();
            }
        } catch (Exception e) {
            LOG.error("Failed getting all queue state did={} reason={}", did, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/levelUp/{codeQR}",
                "levelUp",
                TokenQueueController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Get all the queues' user has token from. In short all the queues' user has joined. */
    @GetMapping (
        value = "/queues",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String getAllJoinedQueues(
        @RequestHeader ("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput deviceType
    ) {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Queues for did={} dt={}", did.getText(), deviceType.getText());
        try {
            return queueMobileService.findAllJoinedQueues(did.getText()).asJson();
        } catch (DeviceDetailMissingException e) {
            LOG.error("Failed getting queues did missing reason={}", e.getLocalizedMessage());
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } catch (Exception e) {
            LOG.error("Failed getting queues did={}, reason={}", did, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/queues",
                "getAllJoinedQueues",
                TokenQueueController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    @API(status = DEPRECATED, since = "1.3.122")
    @Deprecated
    /** Get all the historical queues' user has token from. In short all the queues' user has joined in the past. */
    @PostMapping(
        value = "/historical",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String allHistoricalJoinedQueues(
        @RequestHeader ("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader (value = "X-R-AF", required = false, defaultValue = "NQMT")
        ScrubbedInput appFlavor,

        @RequestBody
        String tokenJson,

        HttpServletRequest request
    ) {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Queues historical for did={} dt={}", did.getText(), deviceType.getText());
        ParseTokenFCM parseTokenFCM = ParseTokenFCM.newInstance(tokenJson, request);
        if (StringUtils.isNotBlank(parseTokenFCM.getErrorResponse())) {
            return parseTokenFCM.getErrorResponse();
        }

        try {
            LOG.warn("Sent warning to upgrade did={}", did);
            return getErrorReason("To continue, please upgrade to latest version", MOBILE_UPGRADE);
        } catch (Exception e) {
            LOG.error("Failed getting history did={}, reason={}", did, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/historical",
                "getAllHistoricalJoinedQueues",
                TokenQueueController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Join the queue. */
    @API(status = DEPRECATED, since = "1.3.121")
    @PostMapping (
        value = "/queue/{codeQR}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Deprecated
    public String joinQueueObsolete(
        @RequestHeader ("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput deviceType,

        @PathVariable ("codeQR")
        ScrubbedInput codeQR,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Join queue did={} deviceType={} codeQR={}", did, deviceType, codeQR);

        BizStoreEntity bizStore = tokenQueueMobileService.getBizService().findByCodeQR(codeQR.getText());
        if (null == bizStore) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid token");
            return null;
        }

        try {
            LOG.warn("Obsolete joinQueue called for {} please upgrade", codeQR.getText());
            return getErrorReason("To continue, please upgrade to latest version", MOBILE_UPGRADE);
        } catch (Exception e) {
            LOG.error("Failed joining queue did={}, reason={}", did, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/queue/{codeQR}",
                "joinQueueObsolete",
                TokenQueueController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Abort the queue. App should un-subscribe user from topic. */
    @PostMapping (
        value = "/abort/{codeQR}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String abortQueue(
        @RequestHeader ("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput deviceType,

        @PathVariable ("codeQR")
        ScrubbedInput codeQR,

        HttpServletRequest request,
        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Abort queue did={} deviceType={} codeQR={}", did, deviceType, codeQR);
        if (!tokenQueueMobileService.getBizService().isValidCodeQR(codeQR.getText())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid token");
            return null;
        }

        try {
            int requesterTime = geoIPLocationService.requestOriginatorTime(HttpRequestResponseParser.getClientIpAddress(request));
            return joinAbortService.abortQueue(codeQR.getText(), did.getText(), null, requesterTime).asJson();
        } catch(QueueAbortPaidPastDurationException e) {
            LOG.warn("Failed cancelling as the duration of cancellation has passed reason={}", e.getLocalizedMessage());
            return getErrorReason(
                "Cannot cancel as the duration of cancellation has passed. Please contact the business.",
                QUEUE_ORDER_ABORT_EXPIRED_LIMITED_TIME);
        } catch (Exception e) {
            LOG.error("Failed aborting queue did={}, reason={}", did, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/abort/{codeQR}",
                "abortQueue",
                TokenQueueController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }
}
