package com.noqapp.mobile.view.controller.open;

import com.noqapp.common.utils.Formatter;
import com.noqapp.common.utils.ParseJsonStringToMap;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.UserAccountEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.types.DeviceTypeEnum;
import com.noqapp.domain.types.GenderEnum;
import com.noqapp.mobile.common.util.ErrorEncounteredJson;
import com.noqapp.mobile.common.util.ExtractFirstLastName;
import com.noqapp.mobile.service.AccountMobileService;
import com.noqapp.mobile.service.AccountMobileService.ACCOUNT_REGISTRATION_CLIENT;
import com.noqapp.mobile.service.AccountMobileService.ACCOUNT_REGISTRATION_MERCHANT;
import com.noqapp.mobile.service.DeviceService;
import com.noqapp.mobile.view.validator.AccountClientValidator;
import com.noqapp.service.AccountService;
import com.noqapp.service.exceptions.DuplicateAccountException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MOBILE;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MOBILE_JSON;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.USER_EXISTING;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.USER_INPUT;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.USER_NOT_FOUND;
import static com.noqapp.mobile.service.AccountMobileService.ACCOUNT_REGISTRATION;

/**
 * User: hitender
 * Date: 3/22/17 12:29 PM
 */
@SuppressWarnings({
        "PMD.BeanMembersShouldSerialize",
        "PMD.LocalVariableCouldBeFinal",
        "PMD.MethodArgumentCouldBeFinal",
        "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/open/client")
public class AccountClientController {
    private static final Logger LOG = LoggerFactory.getLogger(AccountClientController.class);

    private AccountService accountService;
    private AccountMobileService accountMobileService;
    private AccountClientValidator accountClientValidator;
    private DeviceService deviceService;

    @Autowired
    public AccountClientController(
            AccountService accountService,
            AccountMobileService accountMobileService,
            AccountClientValidator accountClientValidator,
            DeviceService deviceService
    ) {
        this.accountService = accountService;
        this.accountMobileService = accountMobileService;
        this.accountClientValidator = accountClientValidator;
        this.deviceService = deviceService;
    }

    @PostMapping(
            value = "/registration",
            headers = "Accept=" + MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String register(
            @RequestHeader ("X-R-DID")
            ScrubbedInput did,

            @RequestHeader ("X-R-DT")
            ScrubbedInput deviceType,

            @RequestBody
            String registrationJson,

            HttpServletResponse response
    ) {
        Map<String, ScrubbedInput> map;
        try {
            map = ParseJsonStringToMap.jsonStringToMap(registrationJson);
        } catch (IOException e) {
            LOG.error("Could not parse json={} reason={}", registrationJson, e.getLocalizedMessage(), e);
            return ErrorEncounteredJson.toJson("Could not parse JSON", MOBILE_JSON);
        }

        Map<String, String> errors;
        try {
            if (map.isEmpty()) {
                /* Validation failure as there is no data in the map. */
                return ErrorEncounteredJson.toJson(accountClientValidator.validateWithPassword(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null));
            } else {
                Set<String> unknownKeys = invalidElementsInMapDuringRegistration(map);
                if (!unknownKeys.isEmpty()) {
                    /* Validation failure as there are unknown keys. */
                    return ErrorEncounteredJson.toJson("Could not parse " + unknownKeys, MOBILE_JSON);
                }

                /* Required. */
                String phone = StringUtils.deleteWhitespace(map.get(ACCOUNT_REGISTRATION.PH.name()).getText());
                /* Required. */
                String firstName = WordUtils.capitalize(map.get(ACCOUNT_REGISTRATION.FN.name()).getText());
                String lastName = null;
                if (StringUtils.isNotBlank(firstName)) {
                    ExtractFirstLastName extractFirstLastName = new ExtractFirstLastName(firstName);
                    firstName = extractFirstLastName.getFirstName();
                    lastName = extractFirstLastName.getLastName();
                }

                String mail = StringUtils.lowerCase(map.get(ACCOUNT_REGISTRATION.EM.name()).getText());
                String password = StringUtils.lowerCase(map.get(ACCOUNT_REGISTRATION_MERCHANT.PW.name()).getText());
                String birthday = map.get(ACCOUNT_REGISTRATION.BD.name()).getText();
                /* Required. */
                String gender = map.get(ACCOUNT_REGISTRATION.GE.name()).getText();
                /* Required. */
                String countryShortName = Formatter.getCountryShortNameFromInternationalPhone(phone);
                /* Required. */
                String timeZone = map.get(ACCOUNT_REGISTRATION.TZ.name()).getText();

                String inviteCode = map.get(ACCOUNT_REGISTRATION_CLIENT.IC.name()).getText();
                if (StringUtils.isNotBlank(inviteCode)) {
                    UserProfileEntity userProfileOfInvitee = accountService.findProfileByInviteCode(inviteCode);
                    if (null == userProfileOfInvitee) {
                        return ErrorEncounteredJson.toJson("Invalid invite code " + inviteCode, MOBILE);
                    }
                }

                errors = accountClientValidator.validateWithPassword(
                        phone,
                        map.get(ACCOUNT_REGISTRATION.FN.name()).getText(),
                        mail,
                        birthday,
                        gender,
                        countryShortName,
                        timeZone,
                        password
                );

                if (!errors.isEmpty()) {
                    return ErrorEncounteredJson.toJson(errors);
                }

                LOG.debug("Check by phone={}", phone);
                UserProfileEntity userProfile = accountService.checkUserExistsByPhone(phone);
                if (null != userProfile) {
                    LOG.info("Failed user registration as already exists phone={}", phone);
                    errors = new HashMap<>();
                    errors.put(ErrorEncounteredJson.REASON, "User already exists. Would you like to recover your account?");
                    errors.put(ACCOUNT_REGISTRATION.PH.name(), "+" + phone);
                    errors.put(ErrorEncounteredJson.SYSTEM_ERROR, USER_EXISTING.name());
                    errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, USER_EXISTING.getCode());
                    return ErrorEncounteredJson.toJson(errors);
                }

                userProfile = accountService.doesUserExists(mail);
                if (null != userProfile) {
                    LOG.info("Failed user registration as already exists mail={}", mail);
                    errors = new HashMap<>();
                    errors.put(ErrorEncounteredJson.REASON, "User already exists. Would you like to recover your account?");
                    errors.put(ACCOUNT_REGISTRATION.EM.name(), mail);
                    errors.put(ErrorEncounteredJson.SYSTEM_ERROR, USER_EXISTING.name());
                    errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, USER_EXISTING.getCode());
                    return ErrorEncounteredJson.toJson(errors);
                }
                //TODO add account migration support when duplicate

                try {
                    UserAccountEntity userAccount = accountMobileService.createNewClientAccount(
                            phone,
                            firstName,
                            lastName,
                            mail,
                            birthday,
                            GenderEnum.valueOf(gender),
                            countryShortName,
                            timeZone,
                            password,
                            inviteCode,
                            false
                    );
                    response.addHeader("X-R-MAIL", userAccount.getUserId());
                    response.addHeader("X-R-AUTH", userAccount.getUserAuthentication().getAuthenticationKey());

                    DeviceTypeEnum deviceTypeEnum;
                    try {
                        deviceTypeEnum = DeviceTypeEnum.valueOf(deviceType.getText());
                    } catch (Exception e) {
                        LOG.error("Failed parsing deviceType, reason={}", e.getLocalizedMessage(), e);
                        return DeviceController.getErrorReason("Incorrect device type.", USER_INPUT);
                    }
                    deviceService.updateRegisteredDevice(userAccount.getQueueUserId(), did.getText(), deviceTypeEnum);
                    return accountMobileService.getProfileAsJson(userAccount.getQueueUserId());
                } catch (DuplicateAccountException e) {
                    LOG.info("Failed user registration as already exists phone={} mail={}", phone, mail);
                    errors = new HashMap<>();
                    errors.put(ErrorEncounteredJson.REASON, "User already exists. Would you like to recover your account?");
                    errors.put(ACCOUNT_REGISTRATION.EM.name(), mail);
                    errors.put(ErrorEncounteredJson.SYSTEM_ERROR, USER_EXISTING.name());
                    errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, USER_EXISTING.getCode());
                    return ErrorEncounteredJson.toJson(errors);
                } catch (Exception e) {
                    LOG.error("Failed signup for user={} reason={}", mail, e.getLocalizedMessage(), e);

                    errors = new HashMap<>();
                    errors.put(ErrorEncounteredJson.REASON, "Something went wrong. Engineers are looking into this.");
                    errors.put(ACCOUNT_REGISTRATION.PH.name(), phone);
                    errors.put(ErrorEncounteredJson.SYSTEM_ERROR, SEVERE.name());
                    errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, SEVERE.getCode());
                    return ErrorEncounteredJson.toJson(errors);
                }
            }
        } catch (Exception e) {
            LOG.error("Failed signup when parsing for reason={}", e.getLocalizedMessage(), e);

            errors = new HashMap<>();
            errors.put(ErrorEncounteredJson.REASON, "Something went wrong. Engineers are looking into this.");
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR, MOBILE_JSON.name());
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, MOBILE_JSON.getCode());
            return ErrorEncounteredJson.toJson(errors);
        }
    }

    //TODO recover is based on phone number. When number already exists then ask which of the stores the user visited.
    //on bad answer, mark account data old with some number and let the user create new data.

    @PostMapping(
            value = "/login",
            headers = "Accept=" + MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String login(
            @RequestHeader ("X-R-DID")
            ScrubbedInput did,

            @RequestHeader ("X-R-DT")
            ScrubbedInput deviceType,

            @RequestBody
            String loginJson,

            HttpServletResponse response
    ) {

        Map<String, ScrubbedInput> map;
        try {
            map = ParseJsonStringToMap.jsonStringToMap(loginJson);
        } catch (IOException e) {
            LOG.error("Could not parse json={} reason={}", loginJson, e.getLocalizedMessage(), e);
            return ErrorEncounteredJson.toJson("Could not parse JSON", MOBILE_JSON);
        }

        Map<String, String> errors;
        try {
            if (map.isEmpty()) {
                /* Validation failure as there is no data in the map. */
                return ErrorEncounteredJson.toJson(accountClientValidator.validateForPhoneMigration(
                        null,
                        null));
            } else {
                Set<String> unknownKeys = invalidElementsInMapDuringLogin(map);
                if (!unknownKeys.isEmpty()) {
                    /* Validation failure as there are unknown keys. */
                    return ErrorEncounteredJson.toJson("Could not parse " + unknownKeys, MOBILE_JSON);
                }

                /* Required. */
                String phone = StringUtils.deleteWhitespace(map.get(ACCOUNT_REGISTRATION.PH.name()).getText());

                /* Required. */
                String countryShortName = Formatter.getCountryShortNameFromInternationalPhone(phone);

                /* Doing phone validation for login. */
                errors = accountClientValidator.validateForPhoneMigration(
                        phone,
                        countryShortName
                );

                if (!errors.isEmpty()) {
                    return ErrorEncounteredJson.toJson(errors);
                }

                try {
                    UserProfileEntity userProfile = accountService.checkUserExistsByPhone(phone);
                    if (null == userProfile) {
                        LOG.info("Failed user login as no user found with phone={} cs={}", phone, countryShortName);
                        errors = new HashMap<>();
                        errors.put(ErrorEncounteredJson.REASON, "No user found. Would you like to register?");
                        errors.put(ACCOUNT_REGISTRATION.PH.name(), phone);
                        errors.put(ErrorEncounteredJson.SYSTEM_ERROR, USER_NOT_FOUND.name());
                        errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, USER_NOT_FOUND.getCode());
                        return ErrorEncounteredJson.toJson(errors);
                    }

                    UserAccountEntity userAccount = accountMobileService.findByQueueUserId(userProfile.getQueueUserId());
                    if (!userAccount.isPhoneValidated()) {
                        //TODO mark otp validated after verifying with FB server with token received
                        userAccount.setPhoneValidated(true);
                        accountService.save(userAccount);
                    }

                    /* Update authentication key after login. */
                    String updatedAuthenticationKey = accountService.updateAuthenticationKey(userAccount.getUserAuthentication().getId());
                    response.addHeader("X-R-MAIL", userAccount.getUserId());
                    response.addHeader("X-R-AUTH", updatedAuthenticationKey);

                    DeviceTypeEnum deviceTypeEnum;
                    try {
                        deviceTypeEnum = DeviceTypeEnum.valueOf(deviceType.getText());
                    } catch (Exception e) {
                        LOG.error("Failed parsing deviceType, reason={}", e.getLocalizedMessage(), e);
                        return DeviceController.getErrorReason("Incorrect device type.", USER_INPUT);
                    }
                    deviceService.updateRegisteredDevice(userAccount.getQueueUserId(), did.getText(), deviceTypeEnum);
                    return accountMobileService.getProfileAsJson(userAccount.getQueueUserId());
                } catch (Exception e) {
                    LOG.error("Failed login for phone={} cs={} reason={}", phone, countryShortName, e.getLocalizedMessage(), e);

                    errors = new HashMap<>();
                    errors.put(ErrorEncounteredJson.REASON, "Something went wrong. Engineers are looking into this.");
                    errors.put(ACCOUNT_REGISTRATION.PH.name(), phone);
                    errors.put(ErrorEncounteredJson.SYSTEM_ERROR, SEVERE.name());
                    errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, SEVERE.getCode());
                    return ErrorEncounteredJson.toJson(errors);
                }
            }
        } catch (Exception e) {
            LOG.error("Failed login when parsing for reason={}", e.getLocalizedMessage(), e);

            errors = new HashMap<>();
            errors.put(ErrorEncounteredJson.REASON, "Something went wrong. Engineers are looking into this.");
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR, MOBILE_JSON.name());
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, MOBILE_JSON.getCode());
            return ErrorEncounteredJson.toJson(errors);
        }
    }

    public static Set<String> invalidElementsInMapDuringRegistration(Map<String, ScrubbedInput> map) {
        Set<String> keys = new HashSet<>(map.keySet());
        List<ACCOUNT_REGISTRATION> enums = new ArrayList<>(Arrays.asList(ACCOUNT_REGISTRATION.values()));
        for (ACCOUNT_REGISTRATION registration : enums) {
            keys.remove(registration.name());
        }

        List<ACCOUNT_REGISTRATION_CLIENT> client = new ArrayList<>(Arrays.asList(ACCOUNT_REGISTRATION_CLIENT.values()));
        for (ACCOUNT_REGISTRATION_CLIENT registration_client : client) {
            keys.remove(registration_client.name());
        }

        List<ACCOUNT_REGISTRATION_MERCHANT> merchant = new ArrayList<>(Arrays.asList(ACCOUNT_REGISTRATION_MERCHANT.values()));
        for (ACCOUNT_REGISTRATION_MERCHANT account_registration_merchant : merchant) {
            keys.remove(account_registration_merchant.name());
        }

        //Remove ACCOUNT_UPDATE.QID for registration
        keys.remove(AccountMobileService.ACCOUNT_UPDATE.QID.name());

        return keys;
    }

    private Set<String> invalidElementsInMapDuringLogin(Map<String, ScrubbedInput> map) {
        Set<String> keys = new HashSet<>(map.keySet());
        List<ACCOUNT_REGISTRATION> enums = new ArrayList<>(Arrays.asList(ACCOUNT_REGISTRATION.PH, ACCOUNT_REGISTRATION.CS));
        for (ACCOUNT_REGISTRATION registration : enums) {
            keys.remove(registration.name());
        }

        return keys;
    }
}
