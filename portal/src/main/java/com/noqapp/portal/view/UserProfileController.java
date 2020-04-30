package com.noqapp.portal.view;

import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.portal.view.medical.MedicalDashboardController.authorizeRequest;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.mobile.service.AccountMobileService;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.portal.body.Login;
import com.noqapp.portal.view.medical.MedicalDashboardController;

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
import java.time.Instant;

import javax.servlet.http.HttpServletResponse;

/**
 * hitender
 * 4/28/20 2:06 PM
 */
@SuppressWarnings({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/portal/view/user")
public class UserProfileController {
    private static final Logger LOG = LoggerFactory.getLogger(UserProfileController.class);

    private AccountMobileService accountMobileService;
    private AuthenticateMobileService authenticateMobileService;

    @Autowired
    public UserProfileController(AccountMobileService accountMobileService, AuthenticateMobileService authenticateMobileService) {
        this.accountMobileService = accountMobileService;
        this.authenticateMobileService = authenticateMobileService;
    }

    @PostMapping(
        value = "/searchByPhone",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String searchByPhone(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput dt,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        Login loginJson,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Populate medical dashboard coupon with mail={} did={} deviceType={} auth={}", mail, did, dt, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid, mail.getText(), did.getText(), "/portal/medical/dashboard")) return null;

        UserProfileEntity userProfile = accountMobileService.checkUserExistsByPhone(loginJson.getCountryShortName().getText() + loginJson.getPhone().getText());
        if (null != userProfile) {
            return accountMobileService.getProfileAsJson(userProfile.getQueueUserId()).asJson();
        }

        return "{}";
    }
}
