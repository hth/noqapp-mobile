package com.noqapp.mobile.view.controller.api.client;

import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

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

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.noqapp.domain.json.JsonQueue;
import com.noqapp.domain.types.DeviceTypeEnum;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.service.QueueMobileService;
import com.noqapp.mobile.service.TokenQueueMobileService;
import com.noqapp.mobile.view.common.ParseTokenFCM;
import com.noqapp.mobile.view.controller.api.merchant.ManageQueueController;
import com.noqapp.mobile.view.controller.open.TokenQueueController;
import com.noqapp.service.InviteService;
import com.noqapp.utils.ScrubbedInput;

import java.io.IOException;

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
    private static final Logger LOG = LoggerFactory.getLogger(TokenQueueController.class);

    private TokenQueueMobileService tokenQueueMobileService;
    private QueueMobileService queueMobileService;
    private InviteService inviteService;
    private AuthenticateMobileService authenticateMobileService;

    @Autowired
    public TokenQueueAPIController(
            TokenQueueMobileService tokenQueueMobileService,
            QueueMobileService queueMobileService,
            InviteService inviteService,
            AuthenticateMobileService authenticateMobileService
    ) {
        this.tokenQueueMobileService = tokenQueueMobileService;
        this.queueMobileService = queueMobileService;
        this.inviteService = inviteService;
        this.authenticateMobileService = authenticateMobileService;
    }

    /**
     * Get state of queue at the store.
     *
     * @param did
     * @param dt
     * @param codeQR
     * @param response
     * @return
     * @throws IOException
     */
    @Timed
    @ExceptionMetered
    @RequestMapping (
            method = RequestMethod.GET,
            value = "/{codeQR}",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String getQueueState(
            @RequestHeader ("X-R-DID")
            ScrubbedInput did,

            @RequestHeader ("X-R-DT")
            ScrubbedInput dt,

            @RequestHeader ("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            @PathVariable ("codeQR")
            ScrubbedInput codeQR,

            HttpServletResponse response
    ) throws IOException {
        LOG.info("On scan get state did={} dt={} codeQR={}", did, dt, codeQR);
        String rid = authenticateMobileService.getReceiptUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, rid)) return null;

        if (!tokenQueueMobileService.getBizService().isValidCodeQR(codeQR.getText())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid token");
            return null;
        }

        try {
            return tokenQueueMobileService.findTokenState(codeQR.getText()).asJson();
        } catch (Exception e) {
            LOG.error("Failed getting queue state rid={}, reason={}", rid, e.getLocalizedMessage(), e);
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
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
    @Timed
    @ExceptionMetered
    @RequestMapping (
            method = RequestMethod.GET,
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
        String rid = authenticateMobileService.getReceiptUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, rid)) return null;

        try {
            return queueMobileService.findAllJoinedQueues(rid, did.getText()).asJson();
        } catch (Exception e) {
            LOG.error("Failed getting queues rid={}, reason={}", rid, e.getLocalizedMessage(), e);
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
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
    @Timed
    @ExceptionMetered
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

            @RequestHeader ("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            @RequestBody
            String tokenJson,

            HttpServletResponse response
    ) throws IOException {
        String rid = authenticateMobileService.getReceiptUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, rid)) return null;

        ParseTokenFCM parseTokenFCM = ParseTokenFCM.newInstance(tokenJson);
        if (StringUtils.isNotBlank(parseTokenFCM.getErrorResponse())) {
            return parseTokenFCM.getErrorResponse();
        }

        try {
            return queueMobileService.findHistoricalQueue(
                    rid,
                    did.getText(),
                    DeviceTypeEnum.valueOf(deviceType.getText()),
                    parseTokenFCM.getTokenFCM()).asJson();
        } catch (Exception e) {
            LOG.error("Failed getting history rid={}, reason={}", rid, e.getLocalizedMessage(), e);
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
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
    @Timed
    @ExceptionMetered
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

            @RequestHeader ("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            @PathVariable ("codeQR")
            ScrubbedInput codeQR,

            HttpServletResponse response
    ) throws IOException {
        LOG.info("Join queue did={} dt={} codeQR={}", did, deviceType, codeQR);
        String rid = authenticateMobileService.getReceiptUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, rid)) return null;

        if (!tokenQueueMobileService.getBizService().isValidCodeQR(codeQR.getText())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid token");
            return null;
        }

        try {
            return tokenQueueMobileService.joinQueue(codeQR.getText(), did.getText(), rid).asJson();
        } catch (Exception e) {
            LOG.error("Failed joining queue rid={}, reason={}", rid, e.getLocalizedMessage(), e);
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
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
    @Timed
    @ExceptionMetered
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

            @RequestHeader ("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            @PathVariable ("codeQR")
            ScrubbedInput codeQR,

            HttpServletResponse response
    ) throws IOException {
        LOG.info("Abort queue did={} dt={} codeQR={}", did, deviceType, codeQR);
        String rid = authenticateMobileService.getReceiptUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, rid)) return null;

        if (!tokenQueueMobileService.getBizService().isValidCodeQR(codeQR.getText())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid token");
            return null;
        }

        try {
            return tokenQueueMobileService.abortQueue(codeQR.getText(), did.getText(), rid).asJson();
        } catch (Exception e) {
            LOG.error("Failed aborting queue rid={}, reason={}", rid, e.getLocalizedMessage(), e);
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
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
    @Timed
    @ExceptionMetered
    @RequestMapping (
            method = RequestMethod.POST,
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
        LOG.info("On remote scan get state did={} dt={} codeQR={}", did, deviceType, codeQR);
        String rid = authenticateMobileService.getReceiptUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, rid)) return null;

        if (!tokenQueueMobileService.getBizService().isValidCodeQR(codeQR.getText())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid token");
            return null;
        }

        try {
            JsonQueue jsonQueue = tokenQueueMobileService.findTokenState(codeQR.getText());
            int remoteScanCount = inviteService.getRemoteScanCount(rid);
            jsonQueue.setRemoteJoin(remoteScanCount);
            return jsonQueue.asJson();
        } catch (Exception e) {
            LOG.error("Failed getting queue state rid={}, reason={}", rid, e.getLocalizedMessage(), e);
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
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
    @Timed
    @ExceptionMetered
    @RequestMapping (
            method = RequestMethod.POST,
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
        LOG.info("Join queue did={} dt={} codeQR={}", did, deviceType, codeQR);
        String rid = authenticateMobileService.getReceiptUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, rid)) return null;

        if (!tokenQueueMobileService.getBizService().isValidCodeQR(codeQR.getText())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid token");
            return null;
        }

        try {
            String jsonQueue = tokenQueueMobileService.joinQueue(codeQR.getText(), did.getText(), rid).asJson();
            inviteService.deductRemoteScanCount(rid);
            return jsonQueue;
        } catch (Exception e) {
            LOG.error("Failed joining queue rid={}, reason={}", rid, e.getLocalizedMessage(), e);
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        }
    }

    static boolean authorizeRequest(HttpServletResponse response, String rid) throws IOException {
        if (null == rid) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, ManageQueueController.UNAUTHORIZED);
            return true;
        }
        return false;
    }
}
