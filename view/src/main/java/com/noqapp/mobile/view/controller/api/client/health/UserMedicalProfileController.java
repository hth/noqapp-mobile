package com.noqapp.mobile.view.controller.api.client.health;

import com.noqapp.common.utils.ParseJsonStringToMap;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.types.medical.BloodTypeEnum;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.medical.domain.UserMedicalProfileEntity;
import com.noqapp.medical.service.UserMedicalProfileService;
import com.noqapp.mobile.common.util.ErrorEncounteredJson;
import com.noqapp.mobile.service.AccountMobileService;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.view.controller.api.ProfileCommonHelper;
import com.noqapp.mobile.view.validator.UserMedicalProfileValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.common.utils.CommonUtil.UNAUTHORIZED;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MOBILE_JSON;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

/**
 * User: hitender
 * Date: 7/14/18 12:09 AM
 */
@SuppressWarnings ({
        "PMD.BeanMembersShouldSerialize",
        "PMD.LocalVariableCouldBeFinal",
        "PMD.MethodArgumentCouldBeFinal",
        "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/api/c/h/medicalProfile")
public class UserMedicalProfileController {
    private static final Logger LOG = LoggerFactory.getLogger(UserMedicalProfileController.class);

    private AuthenticateMobileService authenticateMobileService;
    private UserMedicalProfileValidator userMedicalProfileValidator;
    private AccountMobileService accountMobileService;
    private UserMedicalProfileService userMedicalProfileService;
    private ApiHealthService apiHealthService;

    public UserMedicalProfileController(
            AuthenticateMobileService authenticateMobileService,
            UserMedicalProfileValidator userMedicalProfileValidator,
            AccountMobileService accountMobileService,
            UserMedicalProfileService userMedicalProfileService,
            ApiHealthService apiHealthService
    ) {
        this.userMedicalProfileValidator = userMedicalProfileValidator;
        this.authenticateMobileService = authenticateMobileService;
        this.accountMobileService = accountMobileService;
        this.userMedicalProfileService = userMedicalProfileService;
        this.apiHealthService = apiHealthService;
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

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            @RequestBody
            String userMedicalProfileJson,

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

        Map<String, ScrubbedInput> map;
        try {
            map = ParseJsonStringToMap.jsonStringToMap(userMedicalProfileJson);
        } catch (IOException e) {
            LOG.error("Could not parse json={} reason={}", userMedicalProfileJson, e.getLocalizedMessage(), e);
            return ErrorEncounteredJson.toJson("Could not parse JSON", MOBILE_JSON);
        }

        Map<String, String> errors;
        try {
            if (map.isEmpty()) {
                /* Validation failure as there is no data in the map. */
                return ErrorEncounteredJson.toJson(userMedicalProfileValidator.validate(null));
            } else {
                Set<String> unknownKeys = invalidElementsInMapDuringUpdateUserMedicalProfile(map);
                if (!unknownKeys.isEmpty()) {
                    /* Validation failure as there are unknown keys. */
                    return ErrorEncounteredJson.toJson("Could not parse " + unknownKeys, MOBILE_JSON);
                }

                /* Required. */
                BloodTypeEnum bloodType = BloodTypeEnum.valueOf(map.get(AccountMobileService.MEDICAL_PROFILE.BT.name().toLowerCase()).getText());

                errors = userMedicalProfileValidator.validate(bloodType);

                if (!errors.isEmpty()) {
                    return ErrorEncounteredJson.toJson(errors);
                }

                UserMedicalProfileEntity userMedicalProfile = userMedicalProfileService.findOne(qid);
                userMedicalProfile.setBloodType(bloodType);
                userMedicalProfileService.save(userMedicalProfile);
            }

            return accountMobileService.getProfileAsJson(qid);
        } catch (Exception e) {
            LOG.error("Failed updating user medical profile qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                    "/updateUserMedicalProfile",
                    "updateUserMedicalProfile",
                    ProfileCommonHelper.class.getName(),
                    Duration.between(start, Instant.now()),
                    methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    private Set<String> invalidElementsInMapDuringUpdateUserMedicalProfile(Map<String, ScrubbedInput> map) {
        Set<String> keys = new HashSet<>(map.keySet());
        List<AccountMobileService.MEDICAL_PROFILE> enums = new ArrayList<>(Arrays.asList(AccountMobileService.MEDICAL_PROFILE.values()));
        for (AccountMobileService.MEDICAL_PROFILE medicalProfile : enums) {
            keys.remove(medicalProfile.name().toLowerCase());
        }

        return keys;
    }
}
