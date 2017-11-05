package com.noqapp.mobile.view.controller.open;

import com.noqapp.domain.types.DeviceTypeEnum;
import com.noqapp.mobile.service.QueueMobileService;
import com.noqapp.mobile.service.TokenQueueMobileService;
import com.noqapp.mobile.view.common.ParseTokenFCM;
import com.noqapp.utils.ScrubbedInput;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

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

    @Autowired
    public TokenQueueController(
            TokenQueueMobileService tokenQueueMobileService,
            QueueMobileService queueMobileService
    ) {
        this.tokenQueueMobileService = tokenQueueMobileService;
        this.queueMobileService = queueMobileService;
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
    @RequestMapping (
            method = RequestMethod.GET,
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
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            LOG.info("Execution in nano time={}", Duration.between(start, Instant.now()));
        }
    }

    /**
     * Get all the queues user has token from. In short all the queues user has joined.
     *
     * @param did
     * @param deviceType
     * @param response
     * @return
     * @throws IOException
     */
    @RequestMapping (
            method = RequestMethod.GET,
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
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            LOG.info("Execution in nano time={}", Duration.between(start, Instant.now()));
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
    @RequestMapping (
            method = RequestMethod.POST,
            value = "/historical",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String getAllHistoricalJoinedQueues(
            @RequestHeader ("X-R-DID")
            ScrubbedInput did,

            @RequestHeader ("X-R-DT")
            ScrubbedInput deviceType,

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
                    parseTokenFCM.getTokenFCM()).asJson();
        } catch (Exception e) {
            LOG.error("Failed getting history did={}, reason={}", did, e.getLocalizedMessage(), e);
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            LOG.info("Execution in nano time={}", Duration.between(start, Instant.now()));
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
    @RequestMapping (
            method = RequestMethod.POST,
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
        if (!tokenQueueMobileService.getBizService().isValidCodeQR(codeQR.getText())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid token");
            return null;
        }

        try {
            return tokenQueueMobileService.joinQueue(codeQR.getText(), did.getText(), null).asJson();
        } catch (Exception e) {
            LOG.error("Failed joining queue did={}, reason={}", did, e.getLocalizedMessage(), e);
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            LOG.info("Execution in nano time={}", Duration.between(start, Instant.now()));
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
    @RequestMapping (
            method = RequestMethod.POST,
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
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            LOG.info("Execution in nano time={}", Duration.between(start, Instant.now()));
        }
    }
}
