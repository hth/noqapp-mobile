package com.noqapp.mobile.view.controller.api.merchant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.json.JsonTopic;
import com.noqapp.domain.types.UserLevelEnum;
import com.noqapp.mobile.domain.JsonMerchant;
import com.noqapp.mobile.domain.JsonProfile;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.service.BusinessUserStoreService;
import com.noqapp.service.UserProfilePreferenceService;
import com.noqapp.utils.ScrubbedInput;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

/**
 * User: hitender
 * Date: 4/19/17 10:23 AM
 */
@SuppressWarnings ({
        "PMD.BeanMembersShouldSerialize",
        "PMD.LocalVariableCouldBeFinal",
        "PMD.MethodArgumentCouldBeFinal",
        "PMD.LongVariable"
})
@RestController
@RequestMapping (value = "/api/m/profile")
public class MerchantProfileController {
    private static final Logger LOG = LoggerFactory.getLogger(MerchantProfileController.class);

    private AuthenticateMobileService authenticateMobileService;
    private UserProfilePreferenceService userProfilePreferenceService;
    private BusinessUserStoreService businessUserStoreService;

    @Autowired
    public MerchantProfileController(
            AuthenticateMobileService authenticateMobileService,
            UserProfilePreferenceService userProfilePreferenceService,
            BusinessUserStoreService businessUserStoreService) {
        this.authenticateMobileService = authenticateMobileService;
        this.userProfilePreferenceService = userProfilePreferenceService;
        this.businessUserStoreService = businessUserStoreService;
    }

    @Timed
    @ExceptionMetered
    @RequestMapping (
            method = RequestMethod.GET,
            value = "/fetch",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String fetch(
            @RequestHeader ("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            HttpServletResponse response
    ) throws IOException {
        LOG.info("mail={}, auth={}", mail, ManageQueueController.AUTH_KEY_HIDDEN);
        String rid = authenticateMobileService.getReceiptUserId(mail.getText(), auth.getText());
        if (null == rid) {
            LOG.info("Could not find RID");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, ManageQueueController.UNAUTHORIZED);
            return null;
        }

        UserProfileEntity userProfile = userProfilePreferenceService.findByReceiptUserId(rid);
        if (UserLevelEnum.MER_ADMIN != userProfile.getLevel() || UserLevelEnum.MER_MANAGER != userProfile.getLevel()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, ManageQueueController.UNAUTHORIZED);
            return null;
        }

        /* For merchant profile no need to find remote scan. */
        JsonProfile jsonProfile = JsonProfile.newInstance(userProfile, 0);
        List<JsonTopic> jsonTopics = businessUserStoreService.getQueues(rid);
        JsonMerchant jsonMerchant = new JsonMerchant();
        jsonMerchant.setJsonProfile(jsonProfile);
        jsonMerchant.setTopics(jsonTopics);
        return jsonMerchant.asJson();
    }
}
