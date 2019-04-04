package com.noqapp.mobile.view.controller.api.merchant;

import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.common.utils.CommonUtil.UNAUTHORIZED;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.ACCOUNT_INACTIVE;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;
import static com.noqapp.service.ProfessionalProfileService.POPULATE_PROFILE.*;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.ProfessionalProfileEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.json.JsonProfessionalProfile;
import com.noqapp.domain.json.JsonResponse;
import com.noqapp.domain.json.JsonReview;
import com.noqapp.domain.json.JsonTopic;
import com.noqapp.domain.types.DeviceTypeEnum;
import com.noqapp.domain.types.UserLevelEnum;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.common.util.ErrorEncounteredJson;
import com.noqapp.mobile.domain.JsonMerchant;
import com.noqapp.mobile.domain.JsonProfile;
import com.noqapp.mobile.domain.body.client.QueueReview;
import com.noqapp.mobile.domain.body.client.UpdateProfile;
import com.noqapp.mobile.service.AccountMobileService;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.service.DeviceService;
import com.noqapp.mobile.service.PurchaseOrderMobileService;
import com.noqapp.mobile.service.QueueMobileService;
import com.noqapp.mobile.view.controller.api.ImageCommonHelper;
import com.noqapp.mobile.view.controller.api.ProfileCommonHelper;
import com.noqapp.mobile.view.controller.open.DeviceController;
import com.noqapp.mobile.view.validator.ImageValidator;
import com.noqapp.service.BizService;
import com.noqapp.service.BusinessUserStoreService;
import com.noqapp.service.ProfessionalProfileService;
import com.noqapp.service.ReviewService;
import com.noqapp.service.UserProfilePreferenceService;
import com.noqapp.social.exception.AccountNotActiveException;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

/**
 * User: hitender
 * Date: 4/19/17 10:23 AM
 */
@SuppressWarnings({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/api/m/profile")
public class MerchantProfileController {
    private static final Logger LOG = LoggerFactory.getLogger(MerchantProfileController.class);

    private AuthenticateMobileService authenticateMobileService;
    private UserProfilePreferenceService userProfilePreferenceService;
    private BusinessUserStoreService businessUserStoreService;
    private ProfileCommonHelper profileCommonHelper;
    private ProfessionalProfileService professionalProfileService;
    private ApiHealthService apiHealthService;
    private ImageCommonHelper imageCommonHelper;
    private ImageValidator imageValidator;
    private DeviceService deviceService;
    private BizService bizService;
    private ReviewService reviewService;
    private AccountMobileService accountMobileService;

    @Autowired
    public MerchantProfileController(
        AuthenticateMobileService authenticateMobileService,
        UserProfilePreferenceService userProfilePreferenceService,
        BusinessUserStoreService businessUserStoreService,
        ProfileCommonHelper profileCommonHelper,
        ProfessionalProfileService professionalProfileService,
        ApiHealthService apiHealthService,
        ImageCommonHelper imageCommonHelper,
        ImageValidator imageValidator,
        DeviceService deviceService,
        BizService bizService,
        ReviewService reviewService,
        AccountMobileService accountMobileService
    ) {
        this.authenticateMobileService = authenticateMobileService;
        this.userProfilePreferenceService = userProfilePreferenceService;
        this.businessUserStoreService = businessUserStoreService;
        this.profileCommonHelper = profileCommonHelper;
        this.professionalProfileService = professionalProfileService;
        this.apiHealthService = apiHealthService;
        this.imageCommonHelper = imageCommonHelper;
        this.imageValidator = imageValidator;
        this.deviceService = deviceService;
        this.bizService = bizService;
        this.reviewService = reviewService;
        this.accountMobileService = accountMobileService;
    }

    /** Fetch merchant profile also register device with qid after login. */
    @GetMapping(
        value = "/fetch",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String fetch(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader("X-R-DT")
        ScrubbedInput dt,

        @RequestHeader(value = "X-R-AF", required = false, defaultValue = "NQMT")
        ScrubbedInput appFlavor,

        @RequestHeader("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader("X-R-AUTH")
        ScrubbedInput auth,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.debug("mail={}, auth={}", mail, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            UserProfileEntity userProfile = userProfilePreferenceService.findByQueueUserId(qid);
            switch (userProfile.getLevel()) {
                case M_ADMIN:
                    LOG.info("Cannot login through Merchant App qid={}", qid);
                    break;
                case S_MANAGER:
                case Q_SUPERVISOR:
                    LOG.info("Has access in Merchant App qid={}", qid);
                    break;
                case ADMIN:
                case CLIENT:
                case TECHNICIAN:
                case SUPERVISOR:
                case ANALYSIS:
                    LOG.info("Has no access to Merchant App={}", qid);
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
                    return null;
                default:
                    LOG.error("Reached unsupported user level");
                    throw new UnsupportedOperationException("Reached unsupported user level " + userProfile.getLevel().getDescription());
            }

            /* Register Merchant device after login. */
            if (!deviceService.isDeviceRegistered(qid, did.getText())) {
                LOG.info("Registering device during profile fetch for appFlavor={}", appFlavor);
                deviceService.updateRegisteredDevice(qid, did.getText(), DeviceTypeEnum.valueOf(dt.getText()));
            }

            /* For merchant profile no need to find remote scan. */
            JsonProfile jsonProfile = accountMobileService.getProfileAsJson(qid);
            List<JsonTopic> jsonTopics = businessUserStoreService.getAssignedTokenAndQueues(qid);
            JsonProfessionalProfile jsonProfessionalProfile = null;
            if (UserLevelEnum.S_MANAGER == jsonProfile.getUserLevel()) {
                switch (userProfile.getBusinessType()) {
                    case DO:
                        jsonProfessionalProfile = professionalProfileService.getJsonProfessionalProfile(jsonProfile.getQueueUserId(), SELF);
                        break;
                    default:
                        //Skip getting Professional Profile. Profiles only for Doctor, Mechanic, Nurse, Advocate.
                }
            }

            return new JsonMerchant()
                .setJsonProfile(jsonProfile)
                .setJsonProfessionalProfile(jsonProfessionalProfile)
                .setTopics(jsonTopics).asJson();
        } catch (JsonMappingException e) {
            LOG.error("Failed fetching profile qid={} reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } catch (AccountNotActiveException e) {
            LOG.error("Failed getting profile qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return DeviceController.getErrorReason("Please contact support related to your account", ACCOUNT_INACTIVE);
        } catch (Exception e) {
            LOG.error("Failed getting profile qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/fetch",
                "fetch",
                MerchantProfileController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    @PostMapping(
        value = "/update",
        headers = "Accept=" + MediaType.APPLICATION_JSON_VALUE,
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String update(
        @RequestHeader("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        String updateProfileJson,

        HttpServletResponse response
    ) throws IOException {
        return profileCommonHelper.updateProfile(mail, auth, updateProfileJson, response);
    }

    @PostMapping(
        value = "/updateProfessionalProfile",
        headers = "Accept=" + MediaType.APPLICATION_JSON_VALUE,
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String updateProfessionalProfile(
        @RequestHeader("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        JsonProfessionalProfile jsonProfessionalProfile,

        HttpServletResponse response
    ) {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        try {
            return profileCommonHelper.updateProfessionalProfile(mail, auth, jsonProfessionalProfile, response);
        } catch (Exception e) {
            LOG.error("Failed updating professional profile reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/updateProfessionalProfile",
                "updateProfessionalProfile",
                MerchantProfileController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/upload",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String upload(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader("X-R-DT")
        ScrubbedInput dt,

        @RequestHeader("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader("X-R-AUTH")
        ScrubbedInput auth,

        @RequestPart("file")
        MultipartFile multipartFile,

        @RequestPart("profileImageOfQid")
        String profileImageOfQid,

        HttpServletResponse response
    ) throws IOException {
        Map<String, String> errors = imageValidator.validate(multipartFile, ImageValidator.SUPPORTED_FILE.IMAGE);
        if (!errors.isEmpty()) {
            return ErrorEncounteredJson.toJson(errors);
        }

        return imageCommonHelper.uploadProfileImage(
            did.getText(),
            dt.getText(),
            mail.getText(),
            auth.getText(),
            new ScrubbedInput(profileImageOfQid).getText(),
            multipartFile,
            response);
    }

    @PostMapping(
        value = "/remove",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String remove(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader("X-R-DT")
        ScrubbedInput dt,

        @RequestHeader("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        UpdateProfile updateProfile,

        HttpServletResponse response
    ) throws IOException {
        return imageCommonHelper.removeProfileImage(
            did.getText(),
            dt.getText(),
            mail.getText(),
            auth.getText(),
            new ScrubbedInput(updateProfile.getQueueUserId()).getText(),
            response
        );
    }

    /** Add suggestions back to merchant's professional profile. */
    @PostMapping(
        value = "/intellisense",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String intellisense(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        String professionalProfileJson,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Served mail={} did={} deviceType={} auth={}", mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/profile/intellisense by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            LOG.info("{}", professionalProfileJson);
            JsonProfessionalProfile jsonProfessionalProfile = new ObjectMapper().readValue(professionalProfileJson, JsonProfessionalProfile.class);
            ProfessionalProfileEntity professionalProfile = professionalProfileService.findByQid(qid);
            professionalProfile.setDataDictionary(jsonProfessionalProfile.getDataDictionary());
            professionalProfileService.save(professionalProfile);

            return new JsonResponse(true).asJson();
        } catch (JsonMappingException e) {
            LOG.error("Failed parsing json={} qid={} reason={}", professionalProfileJson, qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } catch (Exception e) {
            LOG.error("Failed updating intellisense qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/intellisense",
                "intellisense",
                MerchantProfileController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    @GetMapping(
        value = "/reviews/{codeQR}",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String reviews(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader("X-R-AUTH")
        ScrubbedInput auth,

        @PathVariable("codeQR")
        ScrubbedInput codeQR,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Served mail={} did={} deviceType={} auth={}", mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/profile/reviews/${codeQR} by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            if (!businessUserStoreService.hasAccessWithUserLevel(qid, codeQR.getText(), UserLevelEnum.S_MANAGER)) {
                LOG.warn("Un-authorized access to /api/m/profile/reviews/${codeQR} by mail={}", mail);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
                return null;
            }

            BizStoreEntity bizStore = bizService.findByCodeQR(codeQR.getText());
            switch (bizStore.getBusinessType().getMessageOrigin()) {
                case O:
                    return reviewService.findOrderReviews(codeQR.getText()).asJson();
                case Q:
                    return reviewService.findQueueReviews(codeQR.getText()).asJson();
            }

            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } catch (Exception e) {
            LOG.error("Failed updating intellisense qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/reviews/${codeQR}",
                "reviews",
                MerchantProfileController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    @PostMapping(
        value = "/flagReview/{codeQR}",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String flagReview(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader("X-R-AUTH")
        ScrubbedInput auth,

        @PathVariable("codeQR")
        ScrubbedInput codeQR,

        @RequestBody
        JsonReview jsonReview,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Served mail={} did={} deviceType={} auth={}", mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/profile/flagReview/${codeQR} by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            if (!businessUserStoreService.hasAccessWithUserLevel(qid, codeQR.getText(), UserLevelEnum.S_MANAGER)) {
                LOG.warn("Un-authorized access to /api/m/profile/flagReview/${codeQR} by mail={}", mail);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
                return null;
            }

            BizStoreEntity bizStore = bizService.findByCodeQR(codeQR.getText());
            switch (bizStore.getBusinessType().getMessageOrigin()) {
                case O:
                    reviewService.findOrderReviews(codeQR.getText()).asJson();
                case Q:
                    reviewService.findQueueReviews(codeQR.getText()).asJson();
            }

            jsonReview.setReviewShow(false);
            return jsonReview.asJson();
        } catch (Exception e) {
            LOG.error("Failed updating intellisense qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/flagReview/${codeQR}",
                "flagReview",
                MerchantProfileController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }
}
