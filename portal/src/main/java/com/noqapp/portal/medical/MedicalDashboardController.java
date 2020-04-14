package com.noqapp.portal.medical;

import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.common.utils.CommonUtil.UNAUTHORIZED;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.portal.domain.InstantViewDashboard;
import com.noqapp.portal.service.MedicalDashboardService;
import com.noqapp.service.AccountService;
import com.noqapp.service.QueueService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Instant;

import javax.servlet.http.HttpServletResponse;

/**
 * hitender
 * 3/20/20 1:45 PM
 */
@SuppressWarnings({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/portal/medical/dashboard")
public class MedicalDashboardController {
    private static final Logger LOG = LoggerFactory.getLogger(MedicalDashboardController.class);

    private QueueService queueService;
    private MedicalDashboardService medicalDashBoardService;
    private AccountService accountService;
    private AuthenticateMobileService authenticateMobileService;

    @Autowired
    public MedicalDashboardController(
        QueueService queueService,
        MedicalDashboardService medicalDashBoardService,
        AccountService accountService,
        AuthenticateMobileService authenticateMobileService
    ) {
        this.queueService = queueService;
        this.medicalDashBoardService = medicalDashBoardService;
        this.accountService = accountService;
        this.authenticateMobileService = authenticateMobileService;
    }

    @GetMapping(
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String populateDashBoard(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput dt,

        @RequestHeader("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Populate medical dashboard coupon with mail={} did={} deviceType={} auth={}", mail, did, dt, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/coupon/available by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        InstantViewDashboard instantViewDashboard = medicalDashBoardService.populateInstantView("");
        return instantViewDashboard.asJson();
    }
}
