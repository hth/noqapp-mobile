package com.noqapp.mobile.view.controller.open;

import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.DEVICE_DETAIL_MISSING;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

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
    private QueueMobileService queueMobileService;
    private ApiHealthService apiHealthService;

    @Autowired
    public TokenQueueController(
            TokenQueueMobileService tokenQueueMobileService,
            QueueMobileService queueMobileService,
            ApiHealthService apiHealthService
    ) {
        this.tokenQueueMobileService = tokenQueueMobileService;
        this.queueMobileService = queueMobileService;
        this.apiHealthService = apiHealthService;
    }

    /** Get state of queue at the store. */
    @GetMapping (
            value = "/{codeQR}",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
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
        Instant start = Instant.now();
        LOG.info("On scan get state did={} dt={} codeQR={}", did, deviceType, codeQR);
        if (!tokenQueueMobileService.getBizService().isValidCodeQR(codeQR.getText())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid token");
            return null;
        }

        try {
            return tokenQueueMobileService.findTokenState(codeQR.getText()).asJson();
        } catch (Exception e) {
            LOG.error("Failed getting queue state did={} reason={}", did, e.getLocalizedMessage(), e);
            apiHealthService.insert(
                    "/{codeQR}",
                    "getQueueState",
                    TokenQueueController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.F);

            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                    "/{codeQR}",
                    "getQueueState",
                    TokenQueueController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.G);
        }
    }

    /** Get all state of queue at a Business when one QR Code is scanned. */
    @GetMapping(
            value = "/v1/{codeQR}",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
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
            apiHealthService.insert(
                    "/v1/{codeQR}",
                    "getAllQueueState",
                    TokenQueueController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.F);

            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                    "/v1/{codeQR}",
                    "getAllQueueState",
                    TokenQueueController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.G);
        }
    }

    /** Get all state of queue at a Business when one QR Code of store is scanned. */
    @GetMapping(
            value = "/levelUp/{codeQR}",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
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
            apiHealthService.insert(
                    "/levelUp/{codeQR}",
                    "levelUp",
                    TokenQueueController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.F);

            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                    "/levelUp/{codeQR}",
                    "levelUp",
                    TokenQueueController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.G);
        }
    }

    /** Get all the queues user has token from. In short all the queues user has joined. */
    @GetMapping (
            value = "/queues",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String getAllJoinedQueues(
            @RequestHeader ("X-R-DID")
            ScrubbedInput did,

            @RequestHeader ("X-R-DT")
            ScrubbedInput deviceType,

            HttpServletResponse response
    ) {
        Instant start = Instant.now();
        LOG.info("Queues for did={} dt={}", did.getText(), deviceType.getText());
        try {
            return queueMobileService.findAllJoinedQueues(did.getText()).asJson();
        } catch (Exception e) {
            LOG.error("Failed getting queues did={}, reason={}", did, e.getLocalizedMessage(), e);
            apiHealthService.insert(
                    "/queues",
                    "getAllJoinedQueues",
                    TokenQueueController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.F);

            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                    "/queues",
                    "getAllJoinedQueues",
                    TokenQueueController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.G);
        }
    }


    /** Get all the historical queues user has token from. In short all the queues user has joined in past. */
    @PostMapping(
            value = "/historical",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
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

            HttpServletResponse response
    ) {
        Instant start = Instant.now();
        LOG.info("Queues historical for did={} dt={}", did.getText(), deviceType.getText());
        ParseTokenFCM parseTokenFCM = ParseTokenFCM.newInstance(tokenJson);
        if (StringUtils.isNotBlank(parseTokenFCM.getErrorResponse())) {
            return parseTokenFCM.getErrorResponse();
        }

        try {
            return queueMobileService.findHistoricalQueue(
                    did.getText(),
                    DeviceTypeEnum.valueOf(deviceType.getText()),
                    AppFlavorEnum.valueOf(appFlavor.getText()),
                    parseTokenFCM.getTokenFCM(),
                    parseTokenFCM.getModel(),
                    parseTokenFCM.getOsVersion(),
                    parseTokenFCM.getAppVersion()).asJson();
        } catch (DeviceDetailMissingException e) {
            LOG.error("Failed registering deviceType={}, reason={}", deviceType, e.getLocalizedMessage(), e);
            return getErrorReason("Missing device details", DEVICE_DETAIL_MISSING);
        } catch (Exception e) {
            LOG.error("Failed getting history did={}, reason={}", did, e.getLocalizedMessage(), e);
            apiHealthService.insert(
                    "/historical",
                    "getAllHistoricalJoinedQueues",
                    TokenQueueController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.F);

            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                    "/historical",
                    "getAllHistoricalJoinedQueues",
                    TokenQueueController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.G);
        }
    }

    /** Join the queue. */
    @PostMapping (
            value = "/queue/{codeQR}",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String joinQueue(
            @RequestHeader ("X-R-DID")
            ScrubbedInput did,

            @RequestHeader ("X-R-DT")
            ScrubbedInput deviceType,

            @PathVariable ("codeQR")
            ScrubbedInput codeQR,

            HttpServletResponse response
    ) throws IOException {
        Instant start = Instant.now();
        LOG.info("Join queue did={} deviceType={} codeQR={}", did, deviceType, codeQR);

        BizStoreEntity bizStore = tokenQueueMobileService.getBizService().findByCodeQR(codeQR.getText());
        if (null == bizStore) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid token");
            return null;
        }

        try {
            return tokenQueueMobileService.joinQueue(
                    codeQR.getText(),
                    did.getText(),
                    null,
                    null,
                    bizStore.getAverageServiceTime(),
                    TokenServiceEnum.C).asJson();
        } catch (Exception e) {
            LOG.error("Failed joining queue did={}, reason={}", did, e.getLocalizedMessage(), e);
            apiHealthService.insert(
                    "/queue/{codeQR}",
                    "joinQueue",
                    TokenQueueController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.F);

            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                    "/queue/{codeQR}",
                    "joinQueue",
                    TokenQueueController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.G);
        }
    }

    /** Abort the queue. App should un-subscribe user from topic. */
    @PostMapping (
            value = "/abort/{codeQR}",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String abortQueue(
            @RequestHeader ("X-R-DID")
            ScrubbedInput did,

            @RequestHeader ("X-R-DT")
            ScrubbedInput deviceType,

            @PathVariable ("codeQR")
            ScrubbedInput codeQR,

            HttpServletResponse response
    ) throws IOException {
        Instant start = Instant.now();
        LOG.info("Abort queue did={} deviceType={} codeQR={}", did, deviceType, codeQR);
        if (!tokenQueueMobileService.getBizService().isValidCodeQR(codeQR.getText())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid token");
            return null;
        }

        try {
            return tokenQueueMobileService.abortQueue(codeQR.getText(), did.getText(), null).asJson();
        } catch (Exception e) {
            LOG.error("Failed aborting queue did={}, reason={}", did, e.getLocalizedMessage(), e);
            apiHealthService.insert(
                    "/abort/{codeQR}",
                    "abortQueue",
                    TokenQueueController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.F);

            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                    "/abort/{codeQR}",
                    "abortQueue",
                    TokenQueueController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.G);
        }
    }
}
