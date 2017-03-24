package com.token.mobile.view.controller.open;

import static com.token.mobile.common.util.MobileSystemErrorCodeEnum.MOBILE_JSON;
import static com.token.mobile.common.util.MobileSystemErrorCodeEnum.SEVERE;
import static com.token.mobile.common.util.MobileSystemErrorCodeEnum.USER_EXISTING;
import static com.token.mobile.service.AccountMobileService.ACCOUNT_REGISTRATION;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.token.domain.UserAccountEntity;
import com.token.domain.UserProfileEntity;
import com.token.mobile.common.util.ErrorEncounteredJson;
import com.token.mobile.common.util.ExtractFirstLastName;
import com.token.mobile.service.AccountMobileService;
import com.token.mobile.view.validator.AccountClientValidator;
import com.token.service.AccountService;
import com.token.utils.DateUtil;
import com.token.utils.ParseJsonStringToMap;
import com.token.utils.ScrubbedInput;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

/**
 * User: hitender
 * Date: 3/22/17 12:29 PM
 */
@SuppressWarnings ({
        "PMD.BeanMembersShouldSerialize",
        "PMD.LocalVariableCouldBeFinal",
        "PMD.MethodArgumentCouldBeFinal",
        "PMD.LongVariable"
})
@RestController
@RequestMapping (value = "/open/client")
public class AccountClientController {
    private static final Logger LOG = LoggerFactory.getLogger(AccountClientController.class);

    private AccountService accountService;
    private AccountMobileService accountMobileService;
    private AccountClientValidator accountClientValidator;

    @Autowired
    public AccountClientController(
            AccountService accountService,
            AccountMobileService accountMobileService,
            AccountClientValidator accountClientValidator
    ) {
        this.accountService = accountService;
        this.accountMobileService = accountMobileService;
        this.accountClientValidator = accountClientValidator;
    }

    @Timed
    @ExceptionMetered
    @RequestMapping (
            value = "/registration.json",
            method = RequestMethod.POST,
            headers = "Accept=" + MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String register(
            @RequestBody
            String registrationJson,

            HttpServletResponse response
    ) throws IOException {
        String credential = "{}";
        Map<String, ScrubbedInput> map;
        try {
            map = ParseJsonStringToMap.jsonStringToMap(registrationJson);
        } catch (IOException e) {
            LOG.error("Could not parse json={} reason={}", registrationJson, e.getLocalizedMessage(), e);
            return ErrorEncounteredJson.toJson("Could not parse JSON", MOBILE_JSON);
        }

        if (map.isEmpty()) {
            /** Validation failure as there is no data in the map. */
            return ErrorEncounteredJson.toJson(accountClientValidator.validate(
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
                /** Validation failure as there are unknown keys. */
                return ErrorEncounteredJson.toJson("Could not parse " + unknownKeys, MOBILE_JSON);
            }

            /* Required. */
            String phone = map.get(ACCOUNT_REGISTRATION.PH.name()).getText();
            /* Required. */
            String firstName = WordUtils.capitalize(map.get(ACCOUNT_REGISTRATION.FN.name()).getText());
            String lastName = null;
            if (StringUtils.isNotBlank(firstName)) {
                ExtractFirstLastName extractFirstLastName = new ExtractFirstLastName(firstName).invoke();
                firstName = extractFirstLastName.getFirstName();
                lastName = extractFirstLastName.getLastName();
            }

            String mail = StringUtils.lowerCase(map.get(ACCOUNT_REGISTRATION.EM.name()).getText());
            String birthday = map.get(ACCOUNT_REGISTRATION.BD.name()).getText();
            /* Required. */
            String gender = map.get(ACCOUNT_REGISTRATION.GE.name()).getText();
            /* Required. */
            String countryShortName = map.get(ACCOUNT_REGISTRATION.CS.name()).getText();
            /* Required. */
            String timeZone = map.get(ACCOUNT_REGISTRATION.TZ.name()).getText();

            if (StringUtils.isBlank(phone) || StringUtils.isBlank(countryShortName) || StringUtils.isBlank(firstName)) {
                return ErrorEncounteredJson.toJson(
                        accountClientValidator.validate(
                            phone,
                            firstName,
                            mail,
                            birthday,
                            gender,
                            countryShortName,
                            timeZone
                        )
                );
            }

            birthday = DateUtil.parseAgeForBirthday(birthday);

            UserProfileEntity userProfile = accountService.checkUserExistsByPhone(phone, countryShortName);
            if (null != userProfile) {
                LOG.info("Failed user registration as already exists mail={}", mail);
                Map<String, String> errors = new HashMap<>();
                errors.put(ErrorEncounteredJson.REASON, "User already exists. Would you like to recover your account?");
                errors.put(ACCOUNT_REGISTRATION.PH.name(), phone);
                errors.put(ErrorEncounteredJson.SYSTEM_ERROR, USER_EXISTING.name());
                errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, USER_EXISTING.getCode());
                return ErrorEncounteredJson.toJson(errors);
            }

            try {
                UserAccountEntity userAccount = accountMobileService.createNewClientAccount(
                        phone,
                        firstName,
                        lastName,
                        mail,
                        birthday,
                        gender,
                        countryShortName,
                        timeZone
                );
                response.addHeader("X-R-MAIL", userAccount.getUserId());
                response.addHeader("X-R-AUTH", userAccount.getUserAuthentication().getAuthenticationKey());
            } catch (Exception e) {
                LOG.error("Failed signup for user={} reason={}", mail, e.getLocalizedMessage(), e);

                Map<String, String> errors = new HashMap<>();
                errors.put(ErrorEncounteredJson.REASON, "Something went wrong. Engineers are looking into this.");
                errors.put(ACCOUNT_REGISTRATION.PH.name(), mail);
                errors.put(ErrorEncounteredJson.SYSTEM_ERROR, SEVERE.name());
                errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, SEVERE.getCode());
                return ErrorEncounteredJson.toJson(errors);
            }
        }

        return credential;
    }

    //TODO recover is based on phone number. When number already exists then ask which of the stores the user visited.
    //on bad answer, reset account data instead of showing old data.

    private Set<String> invalidElementsInMapDuringRegistration(Map<String, ScrubbedInput> map) {
        Set<String> keys = new HashSet<>(map.keySet());
        List<ACCOUNT_REGISTRATION> enums = new ArrayList<>(Arrays.asList(ACCOUNT_REGISTRATION.values()));
        for (ACCOUNT_REGISTRATION registration : enums) {
            keys.remove(registration.name());
        }

        return keys;
    }
}
