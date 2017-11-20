package com.noqapp.mobile.view.controller.api.client;

import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.domain.JsonRemoteJoin;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.view.controller.api.merchant.ManageQueueController;
import com.noqapp.service.InviteService;
import com.noqapp.utils.ScrubbedInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
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
 * Date: 4/11/17 10:13 AM
 */
@SuppressWarnings ({
        "PMD.BeanMembersShouldSerialize",
        "PMD.LocalVariableCouldBeFinal",
        "PMD.MethodArgumentCouldBeFinal",
        "PMD.LongVariable"
})
@RestController
@RequestMapping (value = "/api/c/remote")
public class RemoteJoinController {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteJoinController.class);

    private AuthenticateMobileService authenticateMobileService;
    private InviteService inviteService;
    private ApiHealthService apiHealthService;

    @Autowired
    public RemoteJoinController(
            AuthenticateMobileService authenticateMobileService,
            InviteService inviteService,
            ApiHealthService apiHealthService
    ) {
        this.authenticateMobileService = authenticateMobileService;
        this.inviteService = inviteService;
        this.apiHealthService = apiHealthService;
    }

    @RequestMapping (
            method = RequestMethod.GET,
            value = "/join",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String joinAvailable(
            @RequestHeader ("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            HttpServletResponse response
    ) throws IOException {
        Instant start = Instant.now();
        LOG.debug("mail={}, auth={}", mail, ManageQueueController.AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, ManageQueueController.UNAUTHORIZED);
            return null;
        }

        try {
            return JsonRemoteJoin.newInstance(inviteService.getRemoteJoinCount(qid)).asJson();
        } catch (Exception e) {
            LOG.error("Failed getting remote join qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            apiHealthService.insert(
                    "/join",
                    "joinAvailable",
                    RemoteJoinController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.F);
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                    "/join",
                    "joinAvailable",
                    RemoteJoinController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.G);
        }
    }
}
