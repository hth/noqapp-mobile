package com.noqapp.mobile.view.validator;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * User: hitender
 * Date: 1/14/17 10:56 AM
 */
@Component
public class AccountMerchantValidator extends AccountValidator {
    private static final Logger LOG = LoggerFactory.getLogger(AccountMerchantValidator.class);

    public Map<String, String> validate(
            String phone,
            String firstName,
            String mail,
            String birthday,
            String gender,
            String countryShortName,
            String timeZone,
            String password
    ) {
        LOG.info("Validating merchant information phone={} cs={}", phone, countryShortName);

        Map<String, String> errors = new HashMap<>();

        phoneValidation(phone, errors);
        firstNameValidation(firstName, errors);
        mailValidation(mail, errors);
        if (StringUtils.isNotBlank(birthday)) {
            birthdayValidation(birthday, errors);
        }
        genderValidation(gender, errors);
        countryShortNameValidation(countryShortName, errors);
        timeZoneValidation(timeZone, errors);
        passwordValidation(password, errors);

        return errors;
    }
}