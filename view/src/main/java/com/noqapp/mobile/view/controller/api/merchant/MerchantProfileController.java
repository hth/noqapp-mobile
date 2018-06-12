package com.noqapp.mobile.view.controller.api.merchant;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.json.JsonTopic;
import com.noqapp.mobile.domain.JsonMerchant;
import com.noqapp.mobile.domain.JsonProfile;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.view.controller.api.ProfileCommonHelper;
import com.noqapp.service.BusinessUserStoreService;
import com.noqapp.service.UserProfilePreferenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.common.utils.CommonUtil.UNAUTHORIZED;

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
    private ProfileCommonHelper profileCommonHelper;

    @Autowired
    public MerchantProfileController(
            AuthenticateMobileService authenticateMobileService,
            UserProfilePreferenceService userProfilePreferenceService,
            BusinessUserStoreService businessUserStoreService,
            ProfileCommonHelper profileCommonHelper
    ) {
        this.authenticateMobileService = authenticateMobileService;
        this.userProfilePreferenceService = userProfilePreferenceService;
        this.businessUserStoreService = businessUserStoreService;
        this.profileCommonHelper = profileCommonHelper;
    }

    @GetMapping(
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
        LOG.debug("mail={}, auth={}", mail, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        UserProfileEntity userProfile = userProfilePreferenceService.findByQueueUserId(qid);
        switch (userProfile.getLevel()) {
            case M_ADMIN:
                LOG.info("Cannot login through Client App");
                break;
            case S_MANAGER:
            case Q_SUPERVISOR:
                LOG.info("Has access in Client App");
                break;
            case ADMIN:
            case CLIENT:
            case TECHNICIAN:
            case SUPERVISOR:
            case ANALYSIS:
                LOG.info("Has no access");
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
                return null;
            default:
                LOG.error("Reached unsupported user level");
                throw new UnsupportedOperationException("Reached unsupported user level " + userProfile.getLevel().getDescription());
        }

        /* For merchant profile no need to find remote scan. */
        JsonProfile jsonProfile = JsonProfile.newInstance(userProfile, 0);
        List<JsonTopic> jsonTopics = businessUserStoreService.getQueues(qid);
        JsonMerchant jsonMerchant = new JsonMerchant();
        jsonMerchant.setJsonProfile(jsonProfile);
        jsonMerchant.setTopics(jsonTopics);
        return jsonMerchant.asJson();
    }

    @PostMapping(
            value = "/update",
            headers = "Accept=" + MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String update(
            @RequestHeader ("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            @RequestBody
            String registrationJson,

            HttpServletResponse response
    ) throws IOException {
        return profileCommonHelper.updateProfile(mail, auth, registrationJson, response);
    }

    @PostMapping(
            value = "/updateHealthCareProfile",
            headers = "Accept=" + MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String updateHealthCareProfile(
            @RequestHeader ("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            @RequestBody
            String registrationJson,

            HttpServletResponse response
    ) throws IOException {
        return profileCommonHelper.updateProfile(mail, auth, registrationJson, response);
    }

    @RequestMapping (
            method = RequestMethod.POST,
            value = "/upload",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String upload(
            @RequestHeader("X-R-DID")
            ScrubbedInput did,

            @RequestHeader ("X-R-DT")
            ScrubbedInput dt,

            @RequestHeader ("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            @RequestPart("file")
            MultipartFile multipartFile,

            HttpServletResponse response
    ) throws IOException {
        return profileCommonHelper.uploadProfileImage(did, dt, mail, auth, multipartFile, response);
    }
}
