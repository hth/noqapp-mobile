package com.noqapp.mobile.view.controller.api.client.health;

import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.common.utils.CommonUtil.UNAUTHORIZED;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.ACCOUNT_INACTIVE;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MEDICAL_PROFILE_DOES_NOT_EXISTS;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MOBILE_JSON;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.mobile.view.controller.api.client.TokenQueueAPIController.authorizeRequest;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

import com.noqapp.common.utils.ParseJsonStringToMap;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.json.medical.JsonUserMedicalProfile;
import com.noqapp.domain.types.medical.BloodTypeEnum;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.medical.domain.UserMedicalProfileEntity;
import com.noqapp.medical.service.MedicalRecordService;
import com.noqapp.medical.service.UserMedicalProfileService;
import com.noqapp.mobile.common.util.ErrorEncounteredJson;
import com.noqapp.mobile.domain.body.client.UserMedicalProfile;
import com.noqapp.mobile.service.AccountMobileService;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.view.controller.open.DeviceController;
import com.noqapp.mobile.view.validator.UserMedicalProfileValidator;
import com.noqapp.repository.UserProfileManager;
import com.noqapp.social.exception.AccountNotActiveException;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

/**
 * User: hitender
 * Date: 7/14/18 12:09 AM
 */
@SuppressWarnings({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/api/c/h/medicalProfile")
public class UserMedicalProfileController {
    private static final Logger LOG = LoggerFactory.getLogger(UserMedicalProfileController.class);

    private UserProfileManager userProfileManager;

    private AuthenticateMobileService authenticateMobileService;
    private UserMedicalProfileValidator userMedicalProfileValidator;
    private AccountMobileService accountMobileService;
    private UserMedicalProfileService userMedicalProfileService;
    private MedicalRecordService medicalRecordService;
    private ApiHealthService apiHealthService;

    public UserMedicalProfileController(
        UserProfileManager userProfileManager,
        AuthenticateMobileService authenticateMobileService,
        UserMedicalProfileValidator userMedicalProfileValidator,
        AccountMobileService accountMobileService,
        UserMedicalProfileService userMedicalProfileService,
        MedicalRecordService medicalRecordService,
        ApiHealthService apiHealthService
    ) {
        this.userProfileManager = userProfileManager;
        this.userMedicalProfileValidator = userMedicalProfileValidator;
        this.authenticateMobileService = authenticateMobileService;
        this.accountMobileService = accountMobileService;
        this.userMedicalProfileService = userMedicalProfileService;
        this.medicalRecordService = medicalRecordService;
        this.apiHealthService = apiHealthService;
    }

    @PostMapping(
        value = "/profile",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String profile(
        @RequestHeader("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        UserMedicalProfile userMedicalProfile,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.debug("Medical Record Fetch mail={}, auth={}", mail, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            if (StringUtils.isBlank(userMedicalProfile.getGuardianQueueUserId()) && userMedicalProfile.getMedicalProfileOfQueueUserId().equalsIgnoreCase(qid)) {
                return medicalRecordService.populateMedicalProfileAndPhysicalHistory(qid).asJson();
            } else {
                UserProfileEntity userProfile = userProfileManager.findByQueueUserId(qid);
                if (userProfileManager.dependentExists(userMedicalProfile.getMedicalProfileOfQueueUserId(), userProfile.getPhone())) {
                    return medicalRecordService.populateMedicalProfileAndPhysicalHistory(userMedicalProfile.getMedicalProfileOfQueueUserId()).asJson();
                }
            }

            return getErrorReason("No medical profile exists", MEDICAL_PROFILE_DOES_NOT_EXISTS);
        } catch (Exception e) {
            LOG.error("Failed getting medical physical record qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/profile",
                "profile",
                UserMedicalProfileController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /* Update Medical Profile of user. */
    @PostMapping(
        value = "/updateUserMedicalProfile",
        headers = "Accept=" + MediaType.APPLICATION_JSON_VALUE,
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String updateUserMedicalProfile(
        @RequestHeader("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        JsonUserMedicalProfile jsonUserMedicalProfile,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.debug("mail={}, auth={}", mail, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        Map<String, String> errors;
        try {
            errors = userMedicalProfileValidator.validate(jsonUserMedicalProfile.getBloodType());
            if (!errors.isEmpty()) {
                return ErrorEncounteredJson.toJson(errors);
            }

            UserMedicalProfileEntity userMedicalProfile = userMedicalProfileService.findOne(qid);
            if (null == userMedicalProfile) {
                userMedicalProfile = new UserMedicalProfileEntity(qid);
            }
            userMedicalProfile.setBloodType(jsonUserMedicalProfile.getBloodType());
            userMedicalProfileService.save(userMedicalProfile);
            return accountMobileService.getProfileAsJson(qid).asJson();
        } catch (AccountNotActiveException e) {
            LOG.error("Failed getting profile qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return DeviceController.getErrorReason("Please contact support related to your account", ACCOUNT_INACTIVE);
        } catch (Exception e) {
            LOG.error("Failed updating user medical profile qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/updateUserMedicalProfile",
                "updateUserMedicalProfile",
                UserMedicalProfileController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }
}
