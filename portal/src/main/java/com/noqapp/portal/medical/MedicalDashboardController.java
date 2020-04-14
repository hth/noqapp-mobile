package com.noqapp.portal.medical;

import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.common.utils.CommonUtil.UNAUTHORIZED;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BusinessUserEntity;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.portal.domain.InstantViewDashboard;
import com.noqapp.portal.service.MedicalDashboardService;
import com.noqapp.service.AccountService;
import com.noqapp.service.BusinessUserService;
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

    private BusinessUserService businessUserService;
    private MedicalDashboardService medicalDashBoardService;
    private AuthenticateMobileService authenticateMobileService;

    @Autowired
    public MedicalDashboardController(
        BusinessUserService businessUserService,
        MedicalDashboardService medicalDashBoardService,
        AuthenticateMobileService authenticateMobileService
    ) {
        this.businessUserService = businessUserService;
        this.medicalDashBoardService = medicalDashBoardService;
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
        if (authorizeRequest(response, qid, mail.getText(), did.getText(), "/portal/medical/dashboard")) return null;

        BusinessUserEntity businessUser = businessUserService.findByQid(qid);
        InstantViewDashboard instantViewDashboard = medicalDashBoardService.populateInstantView(businessUser.getBizName().getId());
        return instantViewDashboard.asJson();
    }

    public static boolean authorizeRequest(HttpServletResponse response, String qid, String mail, String did, String api) throws IOException {
        if (null == qid) {
            LOG.warn("Un-authorized access to {} by {} {}", api, mail, did);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return true;
        }
        return false;
    }
}
