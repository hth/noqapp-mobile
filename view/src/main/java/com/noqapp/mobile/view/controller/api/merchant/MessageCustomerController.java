package com.noqapp.mobile.view.controller.api.merchant;

import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.mobile.view.controller.api.client.TokenQueueAPIController.authorizeRequest;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.json.JsonResponse;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.domain.body.merchant.MessageCustomer;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.service.MessageCustomerService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
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
 * 6/13/20 1:40 PM
 */
@SuppressWarnings ({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/api/m/message")
public class MessageCustomerController {
    private static final Logger LOG = LoggerFactory.getLogger(MessageCustomerController.class);

    private MessageCustomerService messageCustomerService;
    private AuthenticateMobileService authenticateMobileService;
    private ApiHealthService apiHealthService;

    @Autowired
    public MessageCustomerController(
        MessageCustomerService messageCustomerService,
        AuthenticateMobileService authenticateMobileService,
        ApiHealthService apiHealthService
    ) {
        this.messageCustomerService = messageCustomerService;
        this.authenticateMobileService = authenticateMobileService;
        this.apiHealthService = apiHealthService;
    }

    @PostMapping(
        value = "/customer",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String messageCustomer(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        MessageCustomer messageCustomer,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.debug("Message customer {} {} mail={}, auth={}", messageCustomer.getTitle().getText(), deviceType.getText(), mail, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid, mail.getText(), did.getText(), "/api/m/message/customer")) return null;

        try {
            messageCustomerService.sendMessageToSubscribers(
                messageCustomer.getTitle().getText(),
                messageCustomer.getBody().getText(),
                messageCustomer.getCodeQRs(),
                qid
            );

            return new JsonResponse(true).asJson();
        } catch (Exception e) {
            LOG.error("Failed sending message qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/messageCustomer",
                "messageCustomer",
                MessageCustomerController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }
}
