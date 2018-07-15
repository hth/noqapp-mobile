package com.noqapp.mobile.view.controller.api.client;

import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.common.utils.CommonUtil.UNAUTHORIZED;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MOBILE;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MOBILE_JSON;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.USER_EXISTING;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.USER_INPUT;
import static com.noqapp.mobile.view.controller.open.AccountClientController.invalidElementsInMapDuringRegistration;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

import com.noqapp.common.utils.Formatter;
import com.noqapp.common.utils.ParseJsonStringToMap;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.UserAccountEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.types.DeviceTypeEnum;
import com.noqapp.domain.types.GenderEnum;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.common.util.ErrorEncounteredJson;
import com.noqapp.mobile.common.util.ExtractFirstLastName;
import com.noqapp.mobile.service.AccountMobileService;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.service.DeviceService;
import com.noqapp.mobile.view.controller.open.DeviceController;
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

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

/**
 * hitender
 * 6/19/18 6:05 PM
 */
@SuppressWarnings ({
        "PMD.BeanMembersShouldSerialize",
        "PMD.LocalVariableCouldBeFinal",
        "PMD.MethodArgumentCouldBeFinal",
        "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/api/c/dependent")
public class DependentAPIController {
    private static final Logger LOG = LoggerFactory.getLogger(DependentAPIController.class);

    private AccountService accountService;
    private AccountMobileService accountMobileService;
    private AccountClientValidator accountClientValidator;
    private DeviceService deviceService;
    private AuthenticateMobileService authenticateMobileService;
    private ApiHealthService apiHealthService;

    @Autowired
    public DependentAPIController(
            AccountService accountService,
            AccountMobileService accountMobileService,
            AccountClientValidator accountClientValidator,
            DeviceService deviceService,
            AuthenticateMobileService authenticateMobileService,
            ApiHealthService apiHealthService
    ) {
        this.accountService = accountService;
        this.accountMobileService = accountMobileService;
        this.accountClientValidator = accountClientValidator;
        this.deviceService = deviceService;
        this.authenticateMobileService = authenticateMobileService;
        this.apiHealthService = apiHealthService;
    }

    /** Add dependent. */
    @PostMapping(
            value = "/add",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String add(
            @RequestHeader("X-R-DID")
            ScrubbedInput did,

            @RequestHeader ("X-R-DT")
            ScrubbedInput deviceType,

            @RequestHeader ("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            @RequestBody
            String registrationJson,

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
                    String phone = StringUtils.deleteWhitespace(map.get(AccountMobileService.ACCOUNT_REGISTRATION.PH.name()).getText());
                    /* Required. */
                    String firstName = WordUtils.capitalize(map.get(AccountMobileService.ACCOUNT_REGISTRATION.FN.name()).getText());
                    String lastName = null;
                    if (StringUtils.isNotBlank(firstName)) {
                        ExtractFirstLastName extractFirstLastName = new ExtractFirstLastName(firstName);
                        firstName = extractFirstLastName.getFirstName();
                        lastName = extractFirstLastName.getLastName();
                    }

                    String dependentMail = StringUtils.lowerCase(map.get(AccountMobileService.ACCOUNT_REGISTRATION.EM.name()).getText());
                    String password = StringUtils.lowerCase(map.get(AccountMobileService.ACCOUNT_REGISTRATION_MERCHANT.PW.name()).getText());
                    String birthday = map.get(AccountMobileService.ACCOUNT_REGISTRATION.BD.name()).getText();
                    /* Required. */
                    String gender = map.get(AccountMobileService.ACCOUNT_REGISTRATION.GE.name()).getText();
                    /* Required. */
                    String countryShortName = Formatter.getCountryShortNameFromInternationalPhone(phone);
                    /* Required. */
                    String timeZone = map.get(AccountMobileService.ACCOUNT_REGISTRATION.TZ.name()).getText();

                    String inviteCode = map.get(AccountMobileService.ACCOUNT_REGISTRATION_CLIENT.IC.name()).getText();
                    if (StringUtils.isNotBlank(inviteCode)) {
                        UserProfileEntity userProfileOfInvitee = accountService.findProfileByInviteCode(inviteCode);
                        if (null == userProfileOfInvitee) {
                            return ErrorEncounteredJson.toJson("Invalid invite code " + inviteCode, MOBILE);
                        }
                    }

                    errors = accountClientValidator.validateWithPassword(
                            phone,
                            map.get(AccountMobileService.ACCOUNT_REGISTRATION.FN.name()).getText(),
                            dependentMail,
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
                    UserProfileEntity userProfile = accountService.doesUserExists(dependentMail);
                    if (null != userProfile) {
                        LOG.info("Failed dependent user registration as already exists mail={}", dependentMail);
                        errors = new HashMap<>();
                        errors.put(ErrorEncounteredJson.REASON, "User already exists. Would you like to recover your account?");
                        errors.put(AccountMobileService.ACCOUNT_REGISTRATION.EM.name(), dependentMail);
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
                                dependentMail,
                                birthday,
                                GenderEnum.valueOf(gender),
                                countryShortName,
                                timeZone,
                                password,
                                inviteCode,
                                true
                        );
                        DeviceTypeEnum deviceTypeEnum;
                        try {
                            deviceTypeEnum = DeviceTypeEnum.valueOf(deviceType.getText());
                        } catch (Exception e) {
                            LOG.error("Failed dependent parsing deviceType, reason={}", e.getLocalizedMessage(), e);
                            return DeviceController.getErrorReason("Incorrect device type.", USER_INPUT);
                        }
                        deviceService.updateRegisteredDevice(qid, did.getText(), deviceTypeEnum);
                        return accountMobileService.getProfileAsJson(qid);
                    } catch (DuplicateAccountException e) {
                        LOG.info("Failed dependent user registration as already exists phone={} mail={}", phone, dependentMail);
                        errors = new HashMap<>();
                        errors.put(ErrorEncounteredJson.REASON, "User already exists. Would you like to recover your account?");
                        errors.put(AccountMobileService.ACCOUNT_REGISTRATION.EM.name(), dependentMail);
                        errors.put(ErrorEncounteredJson.SYSTEM_ERROR, USER_EXISTING.name());
                        errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, USER_EXISTING.getCode());
                        return ErrorEncounteredJson.toJson(errors);
                    } catch (Exception e) {
                        LOG.error("Failed dependent signup for user={} reason={}", dependentMail, e.getLocalizedMessage(), e);

                        errors = new HashMap<>();
                        errors.put(ErrorEncounteredJson.REASON, "Something went wrong. Engineers are looking into this.");
                        errors.put(AccountMobileService.ACCOUNT_REGISTRATION.PH.name(), phone);
                        errors.put(ErrorEncounteredJson.SYSTEM_ERROR, SEVERE.name());
                        errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, SEVERE.getCode());
                        return ErrorEncounteredJson.toJson(errors);
                    }
                }
            } catch (Exception e) {
                LOG.error("Failed dependent signup when parsing for reason={}", e.getLocalizedMessage(), e);

                errors = new HashMap<>();
                errors.put(ErrorEncounteredJson.REASON, "Something went wrong. Engineers are looking into this.");
                errors.put(ErrorEncounteredJson.SYSTEM_ERROR, MOBILE_JSON.name());
                errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, MOBILE_JSON.getCode());
                return ErrorEncounteredJson.toJson(errors);
            }
        } catch(Exception e) {
            LOG.error("Failed adding dependent for qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                    "/add",
                    "add",
                    DependentAPIController.class.getName(),
                    Duration.between(start, Instant.now()),
                    methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }
}
