package com.token.mobile.view.validator;

import static com.token.mobile.common.util.MobileSystemErrorCodeEnum.USER_INPUT;
import static com.token.mobile.service.AccountMobileService.ACCOUNT_REGISTRATION;
import static com.token.mobile.service.AccountMobileService.ACCOUNT_REGISTRATION_MERCHANT;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.token.domain.types.GenderEnum;
import com.token.mobile.common.util.ErrorEncounteredJson;
import com.token.mobile.view.controller.api.ManageQueueController;
import com.token.utils.Constants;

import java.util.HashMap;
import java.util.Map;

/**
 * User: hitender
 * Date: 3/22/17 9:33 PM
 */
@Component
abstract class AccountValidator {
    private static final Logger LOG = LoggerFactory.getLogger(AccountValidator.class);
    public static final String EMPTY = "Empty";

    @Value ("${AccountValidator.nameLength:4}")
    private int mailLength;

    @Value ("${AccountValidator.mailLength:5}")
    private int nameLength;

    @Value ("${AccountValidator.genderLength:1}")
    private int genderLength;

    @Value ("${AccountValidator.countryShortNameLength:2}")
    private int countryShortNameLength;

    @Value ("${AccountValidator.passwordLength:6}")
    private int passwordLength;

    void phoneValidation(String phone, Map<String, String> errors) {
        if (StringUtils.isBlank(phone)) {
            LOG.info("failed validation phone={}", phone);
            errors.put(ErrorEncounteredJson.REASON, "Failed data validation.");
            errors.put(ACCOUNT_REGISTRATION.PH.name(), phone);
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR, USER_INPUT.name());
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, USER_INPUT.getCode());
        }
    }

    void firstNameValidation(String firstName, Map<String, String> errors) {
        if (StringUtils.isBlank(firstName) || firstName.length() < nameLength) {
            LOG.info("failed validation firstName={}", firstName);
            errors.put(ErrorEncounteredJson.REASON, "Failed data validation.");
            errors.put(ACCOUNT_REGISTRATION.FN.name(), StringUtils.isBlank(firstName) ? EMPTY : firstName);
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR, USER_INPUT.name());
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, USER_INPUT.getCode());
        }
    }

    void mailValidation(String mail, Map<String, String> errors) {
        if (StringUtils.isBlank(mail) || mail.length() < mailLength) {
            LOG.info("failed validation mail={}", mail);
            errors.put(ErrorEncounteredJson.REASON, "Failed data validation.");
            errors.put(ACCOUNT_REGISTRATION.EM.name(), StringUtils.isBlank(mail) ? EMPTY : mail);
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR, USER_INPUT.name());
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, USER_INPUT.getCode());
        }
    }

    void birthdayValidation(String birthday, Map<String, String> errors) {
        if (StringUtils.isNotBlank(birthday) && !Constants.AGE_RANGE.matcher(birthday).matches()) {
            LOG.info("failed validation birthday={}", birthday);
            errors.put(ErrorEncounteredJson.REASON, "Failed data validation.");
            errors.put(ACCOUNT_REGISTRATION.BD.name(), StringUtils.isBlank(birthday) ? EMPTY : birthday);
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR, USER_INPUT.name());
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, USER_INPUT.getCode());
        }
    }

    void genderValidation(String gender, Map<String, String> errors) {
        try {
            if (StringUtils.isBlank(gender) || gender.length() != genderLength) {
                LOG.info("failed validation countryShortName={}", gender);
                errors.put(ErrorEncounteredJson.REASON, "Failed data validation.");
                errors.put(ACCOUNT_REGISTRATION.GE.name(), StringUtils.isBlank(gender) ? EMPTY : gender);
                errors.put(ErrorEncounteredJson.SYSTEM_ERROR, USER_INPUT.name());
                errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, USER_INPUT.getCode());
            } else {
                GenderEnum.valueOf(gender);
            }
        } catch (IllegalArgumentException e) {
            LOG.error("Unsupported Gender type gender={} reason={}", gender, e.getLocalizedMessage(), e);
            errors.put(ErrorEncounteredJson.REASON, "Failed data validation.");
            errors.put(ACCOUNT_REGISTRATION.GE.name(), StringUtils.isBlank(gender) ? EMPTY : gender);
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR, USER_INPUT.name());
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, USER_INPUT.getCode());
        }
    }

    void countryShortNameValidation(String countryShortName, Map<String, String> errors) {
        if (StringUtils.isBlank(countryShortName) || countryShortName.length() != countryShortNameLength) {
            LOG.info("failed validation countryShortName={}", countryShortName);
            errors.put(ErrorEncounteredJson.REASON, "Failed data validation.");
            errors.put(ACCOUNT_REGISTRATION.CS.name(), StringUtils.isBlank(countryShortName) ? EMPTY : countryShortName);
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR, USER_INPUT.name());
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, USER_INPUT.getCode());
        }
    }

    void timeZoneValidation(String timeZone, Map<String, String> errors) {
        if (StringUtils.isBlank(timeZone)) {
            LOG.info("failed validation timeZone={}", timeZone);
            errors.put(ErrorEncounteredJson.REASON, "Failed data validation.");
            errors.put(ACCOUNT_REGISTRATION.TZ.name(), StringUtils.isBlank(timeZone) ? EMPTY : timeZone);
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR, USER_INPUT.name());
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, USER_INPUT.getCode());
        }
    }

    void passwordValidation(String password, Map<String, String> errors) {
        if (StringUtils.isBlank(password) || password.length() < passwordLength) {
            LOG.info("failed validation password={}", ManageQueueController.AUTH_KEY_HIDDEN);
            errors.put(ErrorEncounteredJson.REASON, "Failed data validation.");
            /* Do not send password back. Hidden for security reason. */
            errors.put(ACCOUNT_REGISTRATION_MERCHANT.PW.name(), StringUtils.isBlank(password) ? EMPTY : "********");
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR, USER_INPUT.name());
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, USER_INPUT.getCode());
        }
    }

    public Map<String, String> validateFailureWhenEmpty() {
        Map<String, String> errors = new HashMap<>();
        mailValidation(null, errors);
        return errors;
    }

    public int getMailLength() {
        return mailLength;
    }

    public int getNameLength() {
        return nameLength;
    }

    public int getGenderLength() {
        return genderLength;
    }

    public int getCountryShortNameLength() {
        return countryShortNameLength;
    }

    public int getPasswordLength() {
        return passwordLength;
    }
}
