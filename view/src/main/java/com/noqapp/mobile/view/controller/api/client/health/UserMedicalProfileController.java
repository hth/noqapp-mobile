package com.noqapp.mobile.view.controller.api.client.health;

import static com.noqapp.common.utils.AbstractDomain.ISO8601_FMT;
import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.common.utils.CommonUtil.UNAUTHORIZED;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.ACCOUNT_INACTIVE;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MEDICAL_PROFILE_CANNOT_BE_CHANGED;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MEDICAL_PROFILE_DOES_NOT_EXISTS;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.mobile.view.controller.api.client.TokenQueueAPIController.authorizeRequest;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.json.medical.JsonHospitalVisitSchedule;
import com.noqapp.domain.json.medical.JsonHospitalVisitScheduleList;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.medical.domain.MedicalPhysicalEntity;
import com.noqapp.medical.domain.UserMedicalProfileEntity;
import com.noqapp.medical.domain.json.JsonMedicalPhysical;
import com.noqapp.medical.domain.json.JsonMedicalProfile;
import com.noqapp.medical.service.HospitalVisitScheduleService;
import com.noqapp.medical.service.MedicalRecordService;
import com.noqapp.medical.service.UserMedicalProfileService;
import com.noqapp.mobile.common.util.ErrorEncounteredJson;
import com.noqapp.mobile.domain.body.client.MedicalProfile;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.view.controller.open.DeviceController;
import com.noqapp.mobile.view.validator.UserMedicalProfileValidator;
import com.noqapp.repository.UserProfileManager;
import com.noqapp.social.exception.AccountNotActiveException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

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
    private UserMedicalProfileService userMedicalProfileService;
    private MedicalRecordService medicalRecordService;
    private HospitalVisitScheduleService hospitalVisitScheduleService;
    private ApiHealthService apiHealthService;

    public UserMedicalProfileController(
        UserProfileManager userProfileManager,
        AuthenticateMobileService authenticateMobileService,
        UserMedicalProfileValidator userMedicalProfileValidator,
        UserMedicalProfileService userMedicalProfileService,
        MedicalRecordService medicalRecordService,
        HospitalVisitScheduleService hospitalVisitScheduleService,
        ApiHealthService apiHealthService
    ) {
        this.userProfileManager = userProfileManager;
        this.userMedicalProfileValidator = userMedicalProfileValidator;
        this.authenticateMobileService = authenticateMobileService;
        this.userMedicalProfileService = userMedicalProfileService;
        this.medicalRecordService = medicalRecordService;
        this.hospitalVisitScheduleService = hospitalVisitScheduleService;
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
        MedicalProfile medicalProfile,

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
            if (StringUtils.isBlank(medicalProfile.getGuardianQueueUserId()) && medicalProfile.getMedicalProfileOfQueueUserId().equalsIgnoreCase(qid)) {
                return medicalRecordService.populateMedicalProfileAndPhysicalHistory(qid).asJson();
            } else {
                UserProfileEntity userProfile = userProfileManager.findByQueueUserId(qid);
                if (userProfileManager.dependentExists(medicalProfile.getMedicalProfileOfQueueUserId(), userProfile.getPhone())) {
                    return medicalRecordService.populateMedicalProfileAndPhysicalHistory(medicalProfile.getMedicalProfileOfQueueUserId()).asJson();
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

    @PostMapping(value = "/hospitalVisitSchedule", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    public String hospitalVisitSchedule(
        @RequestHeader("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        MedicalProfile medicalProfile,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.debug("mail={}, auth={}", mail, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        try {
            UserProfileEntity userProfile = userProfileManager.findByQueueUserId(medicalProfile.getMedicalProfileOfQueueUserId());
            List<JsonHospitalVisitSchedule> jsonHospitalVisitSchedules = hospitalVisitScheduleService.findAllAsJson(userProfile.getQueueUserId());

            return new JsonHospitalVisitScheduleList()
                .addJsonHospitalVisitSchedule(
                    new JsonHospitalVisitSchedule()
                        .setName("Fake Name")
                        .setVisitedDate(DateFormatUtils.format(new Date(), ISO8601_FMT, TimeZone.getTimeZone("UTC")))
                        .setExpectedDate(DateFormatUtils.format(new Date(), ISO8601_FMT, TimeZone.getTimeZone("UTC")))
                        .setHeader("1 Week"))
                .addJsonHospitalVisitSchedule(
                    new JsonHospitalVisitSchedule()
                        .setName("Good Name")
                        .setVisitedDate(DateFormatUtils.format(new Date(), ISO8601_FMT, TimeZone.getTimeZone("UTC")))
                        .setExpectedDate(DateFormatUtils.format(new Date(), ISO8601_FMT, TimeZone.getTimeZone("UTC")))
                        .setHeader("1 Week"))
                .addJsonHospitalVisitSchedule(
                    new JsonHospitalVisitSchedule()
                        .setName("Done Name")
                        .setVisitedDate(DateFormatUtils.format(new Date(), ISO8601_FMT, TimeZone.getTimeZone("UTC")))
                        .setExpectedDate(DateFormatUtils.format(new Date(), ISO8601_FMT, TimeZone.getTimeZone("UTC")))
                        .setHeader("2 Week")
            ).asJson();
        } catch (Exception e) {
            LOG.error("Failed updating user medical profile qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/hospitalVisitSchedule",
                "hospitalVisitSchedule",
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
        MedicalProfile medicalProfile,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.debug("mail={}, auth={}", mail, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        Map<String, String> errors;
        try {

            String medicalProfileOfQueueUserId = null;
            if (StringUtils.isBlank(medicalProfile.getGuardianQueueUserId()) && medicalProfile.getMedicalProfileOfQueueUserId().equalsIgnoreCase(qid)) {
                medicalProfileOfQueueUserId = qid;
            } else {
                UserProfileEntity userProfile = userProfileManager.findByQueueUserId(qid);
                if (userProfileManager.dependentExists(medicalProfile.getMedicalProfileOfQueueUserId(), userProfile.getPhone())) {
                    medicalProfileOfQueueUserId = medicalProfile.getMedicalProfileOfQueueUserId();
                }
            }

            errors = userMedicalProfileValidator.validate(medicalProfile.getJsonUserMedicalProfile().getBloodType());
            if (!errors.isEmpty()) {
                return ErrorEncounteredJson.toJson(errors);
            }

            UserMedicalProfileEntity userMedicalProfile = userMedicalProfileService.findOne(medicalProfileOfQueueUserId);
            if (null == userMedicalProfile) {
                userMedicalProfile = new UserMedicalProfileEntity(medicalProfileOfQueueUserId);
            }

            if (userMedicalProfile.getBloodType() == null) {
                userMedicalProfile.setBloodType(medicalProfile.getJsonUserMedicalProfile().getBloodType());
            } else {
                if (userMedicalProfile.getBloodType() != medicalProfile.getJsonUserMedicalProfile().getBloodType()) {
                    return getErrorReason("Blood group cannot be changed. Contact medical practitioner.", MEDICAL_PROFILE_CANNOT_BE_CHANGED);
                }
            }
            userMedicalProfile.setFamilyHistory(medicalProfile.getJsonUserMedicalProfile().getFamilyHistory())
                .setKnownAllergies(medicalProfile.getJsonUserMedicalProfile().getKnownAllergies())
                .setPastHistory(medicalProfile.getJsonUserMedicalProfile().getPastHistory())
                .setMedicineAllergies(medicalProfile.getJsonUserMedicalProfile().getMedicineAllergies())
                .setOccupation(medicalProfile.getJsonUserMedicalProfile().getOccupation());
            userMedicalProfileService.save(userMedicalProfile);

            JsonMedicalProfile jsonMedicalProfile = new JsonMedicalProfile();
            List<MedicalPhysicalEntity> medicalPhysicals = medicalRecordService.findByQid(medicalProfileOfQueueUserId);
            for (MedicalPhysicalEntity medicalPhysical : medicalPhysicals) {
                jsonMedicalProfile.addJsonMedicalPhysical(JsonMedicalPhysical.populateJsonMedicalPhysical(medicalPhysical));
            }
            jsonMedicalProfile.setJsonUserMedicalProfile(userMedicalProfileService.findOneAsJson(qid));
            return jsonMedicalProfile.asJson();
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
