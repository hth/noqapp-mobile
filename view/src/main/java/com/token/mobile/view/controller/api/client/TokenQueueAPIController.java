package com.token.mobile.view.controller.api.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.token.domain.json.JsonQueue;
import com.token.domain.json.JsonResponse;
import com.token.domain.json.JsonToken;
import com.token.domain.json.JsonTokenAndQueue;
import com.token.mobile.service.AuthenticateMobileService;
import com.token.mobile.service.QueueMobileService;
import com.token.mobile.service.TokenQueueMobileService;
import com.token.mobile.view.controller.api.merchant.ManageQueueController;
import com.token.service.InviteService;
import com.token.utils.ScrubbedInput;

import java.io.IOException;
import java.util.List;

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
    private static final Logger LOG = LoggerFactory.getLogger(com.token.mobile.view.controller.open.TokenQueueController.class);

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
    public JsonQueue getQueueState(
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

        return tokenQueueMobileService.findTokenState(codeQR.getText());
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
    public List<JsonTokenAndQueue> getAllJoinedQueues(
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

        return queueMobileService.findAllJoinedQueues(did.getText());
    }


    /**
     * Get all the historical queues user has token from. In short all the queues user has joined in past.
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
            value = "/historical",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public List<JsonTokenAndQueue> getAllHistoricalJoinedQueues(
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

        return queueMobileService.findHistoricalQueue(did.getText());
    }

    /**
     * Join the queue.
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
            method = RequestMethod.POST,
            value = "/queue/{codeQR}",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public JsonToken joinQueue(
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
        LOG.info("Join queue did={} dt={} codeQR={}", did, dt, codeQR);
        String rid = authenticateMobileService.getReceiptUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, rid)) return null;

        if (!tokenQueueMobileService.getBizService().isValidCodeQR(codeQR.getText())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid token");
            return null;
        }

        return tokenQueueMobileService.joinQueue(codeQR.getText(), did.getText(), null);
    }

    /**
     * Abort the queue. App should un-subscribe user from topic.
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
            method = RequestMethod.POST,
            value = "/abort/{codeQR}",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public JsonResponse abortQueue(
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
        LOG.info("Abort queue did={} dt={} codeQR={}", did, dt, codeQR);
        String rid = authenticateMobileService.getReceiptUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, rid)) return null;

        if (!tokenQueueMobileService.getBizService().isValidCodeQR(codeQR.getText())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid token");
            return null;
        }

        return tokenQueueMobileService.abortQueue(codeQR.getText(), did.getText(), null);
    }

    /**
     * Remote scan of QR Code. 
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
            method = RequestMethod.POST,
            value = "/remote/{codeQR}",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public JsonQueue remoteScanQueue(
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
        LOG.info("On remote scan get state did={} dt={} codeQR={}", did, dt, codeQR);

        String rid = authenticateMobileService.getReceiptUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, rid)) return null;

        if (!tokenQueueMobileService.getBizService().isValidCodeQR(codeQR.getText())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid token");
            return null;
        }

        if (inviteService.getRemoteScanCount(rid) > 0) {
            JsonQueue jsonQueue = tokenQueueMobileService.findTokenState(codeQR.getText());
            if (jsonQueue != null) {
                inviteService.deductRemoteScanCount(rid);
                return jsonQueue;
            }
        } else {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, ManageQueueController.UNAUTHORIZED);
            return null;
        }

        return null;
    }

    private boolean authorizeRequest(HttpServletResponse response, String rid) throws IOException {
        if (null == rid) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, ManageQueueController.UNAUTHORIZED);
            return true;
        }
        return false;
    }
}
