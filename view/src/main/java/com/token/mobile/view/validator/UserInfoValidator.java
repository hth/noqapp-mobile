package com.token.mobile.view.validator;

import static com.token.mobile.common.util.MobileSystemErrorCodeEnum.USER_INPUT;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.token.mobile.common.util.ErrorEncounteredJson;
import com.token.mobile.service.AccountMobileService;
import com.token.mobile.view.controller.api.ManageQueueController;
import com.token.utils.Constants;

import java.util.HashMap;
import java.util.Map;

/**
 * User: hitender
 * Date: 1/14/17 10:56 AM
 */
@Component
public class UserInfoValidator {
    private static final Logger LOG = LoggerFactory.getLogger(UserInfoValidator.class);
    public static final String EMPTY = "Empty";

    private int mailLength;
    private int nameLength;
    private int passwordLength;
    private int countryShortNameLength;

    @Autowired
    public UserInfoValidator(
            @Value ("${UserInfoValidator.mailLength:5}")
            int mailLength,

            @Value ("${UserInfoValidator.nameLength:3}")
            int nameLength,

            @Value ("${UserInfoValidator.passwordLength:6}")
            int passwordLength,

            @Value ("${UserInfoValidator.countryShortNameLength:2}")
            int countryShortNameLength
    ) {
        this.mailLength = mailLength;
        this.nameLength = nameLength;
        this.passwordLength = passwordLength;
        this.countryShortNameLength = countryShortNameLength;
    }

    public Map<String, String> validate(String mail, String firstName, String password, String birthday) {
        LOG.info("failed validation mail={} firstName={} password={}", mail, firstName, ManageQueueController.AUTH_KEY_HIDDEN);

        Map<String, String> errors = new HashMap<>();
        errors.put(ErrorEncounteredJson.REASON, "Failed data validation.");

        firstNameValidation(firstName, errors);
        mailValidation(mail, errors);
        passwordValidation(password, errors);
        if (StringUtils.isNotBlank(birthday)) {
            birthdayValidation(birthday, errors);
        }

        errors.put(ErrorEncounteredJson.SYSTEM_ERROR, USER_INPUT.name());
        errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, USER_INPUT.getCode());
        return errors;
    }

    public Map<String, String> validateFailureWhenEmpty() {
        Map<String, String> errors = new HashMap<>();
        mailValidation(null, errors);
        return errors;
    }

    public void passwordValidation(String password, Map<String, String> errors) {
        if (StringUtils.isBlank(password) || password.length() < passwordLength) {
            LOG.info("failed validation password={}", ManageQueueController.AUTH_KEY_HIDDEN);
            errors.put(ErrorEncounteredJson.REASON, "Failed data validation.");
            errors.put(AccountMobileService.REGISTRATION.PW.name(), StringUtils.isBlank(password) ? EMPTY : password);
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR, USER_INPUT.name());
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, USER_INPUT.getCode());
        }
    }

    public void mailValidation(String mail, Map<String, String> errors) {
        if (StringUtils.isBlank(mail) || mail.length() < mailLength) {
            LOG.info("failed validation mail={}", mail);
            errors.put(ErrorEncounteredJson.REASON, "Failed data validation.");
            errors.put(AccountMobileService.REGISTRATION.EM.name(), StringUtils.isBlank(mail) ? EMPTY : mail);
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR, USER_INPUT.name());
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, USER_INPUT.getCode());
        }
    }

    private void firstNameValidation(String firstName, Map<String, String> errors) {
        if (StringUtils.isBlank(firstName) || firstName.length() < nameLength) {
            LOG.info("failed validation firstName={}", firstName);
            errors.put(ErrorEncounteredJson.REASON, "Failed data validation.");
            errors.put(AccountMobileService.REGISTRATION.FN.name(), StringUtils.isBlank(firstName) ? EMPTY : firstName);
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR, USER_INPUT.name());
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, USER_INPUT.getCode());
        }
    }

    private void birthdayValidation(String birthday, Map<String, String> errors) {
        if (StringUtils.isNotBlank(birthday) && !Constants.AGE_RANGE.matcher(birthday).matches()) {
            LOG.info("failed validation birthday={}", birthday);
            errors.put(ErrorEncounteredJson.REASON, "Failed data validation.");
            errors.put(AccountMobileService.REGISTRATION.BD.name(), StringUtils.isBlank(birthday) ? EMPTY : birthday);
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR, USER_INPUT.name());
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, USER_INPUT.getCode());
        }
    }

    public void countryValidation(String countryShortName, Map<String, String> errors) {
        if (StringUtils.isBlank(countryShortName) || countryShortName.length() != countryShortNameLength) {
            LOG.info("failed validation countryShortName={}", countryShortName);
            errors.put(ErrorEncounteredJson.REASON, "Failed data validation.");
            errors.put(AccountMobileService.REGISTRATION.CS.name(), StringUtils.isBlank(countryShortName) ? EMPTY : countryShortName);
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR, USER_INPUT.name());
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, USER_INPUT.getCode());
        }
    }

    public int getMailLength() {
        return mailLength;
    }

    public int getNameLength() {
        return nameLength;
    }

    public int getPasswordLength() {
        return passwordLength;
    }

    public int getCountryShortNameLength() {
        return countryShortNameLength;
    }
}