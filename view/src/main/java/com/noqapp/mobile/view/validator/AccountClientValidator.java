package com.noqapp.mobile.view.validator;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * User: hitender
 * Date: 3/22/17 12:33 PM
 */
@Component
public class AccountClientValidator extends AccountValidator {
    private static final Logger LOG = LoggerFactory.getLogger(AccountClientValidator.class);

    public AccountClientValidator() {
        super();
    }

    public Map<String, String> validate(
            String phone,
            String firstName,
            String mail,
            String birthday,
            String gender,
            String countryShortName,
            String timeZone
    ) {
        LOG.debug("Validating client information phone={} cs={}", phone, countryShortName);

        Map<String, String> errors = new HashMap<>();

        phoneValidation(phone, errors);
        firstNameValidation(firstName, errors);
        if (StringUtils.isNotBlank(mail)) {
            mailValidation(mail, errors);
        }
        if (StringUtils.isNotBlank(birthday)) {
            birthdayValidation(birthday, errors);
        }
        genderValidation(gender, errors);
        countryShortNameValidation(countryShortName, errors);
        timeZoneValidation(timeZone, errors);

        return errors;
    }

    public Map<String, String> validate(
            String phone,
            String countryShortName
    ) {
        LOG.info("Validating client information phone={} cs={}", phone, countryShortName);

        Map<String, String> errors = new HashMap<>();

        phoneValidation(phone, errors);
        countryShortNameValidation(countryShortName, errors);

        return errors;
    }
}
