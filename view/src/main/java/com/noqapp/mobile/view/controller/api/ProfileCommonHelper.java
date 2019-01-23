package com.noqapp.mobile.view.controller.api;

import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.common.utils.CommonUtil.UNAUTHORIZED;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.ACCOUNT_INACTIVE;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MOBILE_JSON;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.SEVERE;

import com.noqapp.common.utils.ParseJsonStringToMap;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.flow.RegisterUser;
import com.noqapp.domain.json.JsonProfessionalProfile;
import com.noqapp.domain.types.AddressOriginEnum;
import com.noqapp.domain.types.GenderEnum;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.common.util.ErrorEncounteredJson;
import com.noqapp.mobile.common.util.ExtractFirstLastName;
import com.noqapp.mobile.service.AccountMobileService;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.view.controller.open.DeviceController;
import com.noqapp.mobile.view.validator.AccountClientValidator;
import com.noqapp.mobile.view.validator.ProfessionalProfileValidator;
import com.noqapp.social.exception.AccountNotActiveException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

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
 * Common code shared between Client and Merchant to update profile.
 * hitender
 * 6/12/18 1:59 AM
 */
@Controller
public class ProfileCommonHelper extends CommonHelper {
    private static final Logger LOG = LoggerFactory.getLogger(ProfileCommonHelper.class);

    private AuthenticateMobileService authenticateMobileService;
    private AccountClientValidator accountClientValidator;
    private AccountMobileService accountMobileService;
    private ProfessionalProfileValidator professionalProfileValidator;
    private ApiHealthService apiHealthService;

    @Autowired
    public ProfileCommonHelper(
        AuthenticateMobileService authenticateMobileService,
        AccountClientValidator accountClientValidator,
        AccountMobileService accountMobileService,
        ProfessionalProfileValidator professionalProfileValidator,
        ApiHealthService apiHealthService
    ) {
        super(accountMobileService);
        this.authenticateMobileService = authenticateMobileService;
        this.accountClientValidator = accountClientValidator;
        this.accountMobileService = accountMobileService;
        this.professionalProfileValidator = professionalProfileValidator;
        this.apiHealthService = apiHealthService;
    }

    /**
     * Update profile does not change phone number or email address.
     *
     * @param qidOfSubmitter
     * @param updateProfileJson
     * @param response
     * @return
     */
    private String updateProfile(String qidOfSubmitter, String updateProfileJson, HttpServletResponse response) {
        LOG.info("Update Profile qidOfSubmitter={} updateProfileJson={}", qidOfSubmitter, updateProfileJson);
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();

        Map<String, ScrubbedInput> map;
        try {
            map = ParseJsonStringToMap.jsonStringToMap(updateProfileJson);
        } catch (IOException e) {
            LOG.error("Could not parse json={} reason={}", updateProfileJson, e.getLocalizedMessage(), e);
            return ErrorEncounteredJson.toJson("Could not parse JSON", MOBILE_JSON);
        }

        Map<String, String> errors;
        String qid = null;
        try {
            if (map.isEmpty()) {
                /* Validation failure as there is no data in the map. */
                return ErrorEncounteredJson.toJson(accountClientValidator.validate(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null));
            } else {
                Set<String> unknownKeys = invalidElementsInMapDuringUpdate(map);
                if (!unknownKeys.isEmpty()) {
                    /* Validation failure as there are unknown keys. */
                    return ErrorEncounteredJson.toJson("Could not parse " + unknownKeys, MOBILE_JSON);
                }

                /* Required. But not changing the phone number. For that we have phone migrate API. */
                qid = map.get(AccountMobileService.ACCOUNT_UPDATE.QID.name()).getText();
                UserProfileEntity userProfile = checkSelfOrDependent(qidOfSubmitter, qid);
                if (null == userProfile) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
                    return null;
                }

                String phone = userProfile.getPhone();
                /* Required. */
                String firstName = WordUtils.capitalize(map.get(AccountMobileService.ACCOUNT_UPDATE.FN.name()).getText());
                String lastName = null;
                if (StringUtils.isNotBlank(firstName)) {
                    ExtractFirstLastName extractFirstLastName = new ExtractFirstLastName(firstName);
                    firstName = extractFirstLastName.getFirstName();
                    lastName = extractFirstLastName.getLastName();
                }

                ScrubbedInput address = map.get(AccountMobileService.ACCOUNT_UPDATE.AD.name()) == null ? new ScrubbedInput("") : map.get(AccountMobileService.ACCOUNT_UPDATE.AD.name());
                ScrubbedInput birthday = map.get(AccountMobileService.ACCOUNT_UPDATE.BD.name());
                /* Required. */
                String gender = map.get(AccountMobileService.ACCOUNT_UPDATE.GE.name()).getText();
                /* Required. */
                String countryShortName = userProfile.getCountryShortName();
                /* Required. */
                ScrubbedInput timeZone = map.get(AccountMobileService.ACCOUNT_UPDATE.TZ.name());

                errors = accountClientValidator.validate(
                    phone,
                    map.get(AccountMobileService.ACCOUNT_UPDATE.FN.name()).getText(),
                    userProfile.getEmail(),
                    birthday.getText(),
                    gender,
                    countryShortName,
                    timeZone.getText());

                if (!errors.isEmpty()) {
                    return ErrorEncounteredJson.toJson(errors);
                }

                RegisterUser registerUser = new RegisterUser()
                    .setEmail(new ScrubbedInput(userProfile.getEmail()))
                    .setQueueUserId(qid)
                    .setFirstName(new ScrubbedInput(firstName))
                    .setLastName(new ScrubbedInput(lastName))
                    .setAddress(address)
                    .setAddressOrigin(StringUtils.isBlank(address.getText()) ? null : AddressOriginEnum.S)
                    .setBirthday(birthday)
                    .setGender(GenderEnum.valueOf(gender))
                    .setCountryShortName(new ScrubbedInput(countryShortName))
                    .setTimeZone(timeZone)
                    .setPhone(new ScrubbedInput(phone));
                accountMobileService.updateUserProfile(registerUser, userProfile.getEmail());
            }

            return accountMobileService.getProfileAsJson(qidOfSubmitter).asJson();
        } catch(AccountNotActiveException e) {
            LOG.error("Failed getting profile qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return DeviceController.getErrorReason("Please contact support related to your account", ACCOUNT_INACTIVE);
        } catch (Exception e) {
            LOG.error("Failed updating profile qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return DeviceController.getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/updateProfile",
                "updateProfile",
                ProfileCommonHelper.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }



    public String updateProfile(
        ScrubbedInput mail,
        ScrubbedInput auth,
        String updateProfileJson,
        HttpServletResponse response
    ) throws IOException {
        LOG.debug("mail={}, auth={}", mail, AUTH_KEY_HIDDEN);
        String qidOfSubmitter = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qidOfSubmitter) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        LOG.info("Profile update being performed by qidOfSubmitter={}", qidOfSubmitter);
        return updateProfile(qidOfSubmitter, updateProfileJson, response);
    }

    public String updateProfessionalProfile(
        ScrubbedInput mail,
        ScrubbedInput auth,
        JsonProfessionalProfile jsonProfessionalProfile,
        HttpServletResponse response
    ) throws IOException {
        LOG.debug("mail={}, auth={}", mail, AUTH_KEY_HIDDEN);
        String qidOfSubmitter = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qidOfSubmitter) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        Map<String, String> errors = professionalProfileValidator.validate(jsonProfessionalProfile);
        if (!errors.isEmpty()) {
            return ErrorEncounteredJson.toJson(errors);
        }

        LOG.info("Professional profile update being performed by qidOfSubmitter={}", qidOfSubmitter);
        return accountMobileService.updateProfessionalProfile(qidOfSubmitter, jsonProfessionalProfile);
    }



    private Set<String> invalidElementsInMapDuringUpdate(Map<String, ScrubbedInput> map) {
        Set<String> keys = new HashSet<>(map.keySet());
        List<AccountMobileService.ACCOUNT_UPDATE> enums = new ArrayList<>(Arrays.asList(AccountMobileService.ACCOUNT_UPDATE.values()));
        for (AccountMobileService.ACCOUNT_UPDATE registration : enums) {
            keys.remove(registration.name());
        }

        return keys;
    }
}
