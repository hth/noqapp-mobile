package com.noqapp.mobile.view.validator;

import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.USER_INPUT;

import com.noqapp.common.utils.DateUtil;
import com.noqapp.domain.types.GenderEnum;
import com.noqapp.mobile.common.util.ErrorEncounteredJson;
import com.noqapp.mobile.service.AccountMobileService;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * User: hitender
 * Date: 3/22/17 12:33 PM
 */
@Component
public class AccountClientValidator {
    private static final Logger LOG = LoggerFactory.getLogger(AccountClientValidator.class);
    private static final String EMPTY = "Empty";

    private int nameLength;
    private int mailLength;
    private int genderLength;
    private int countryShortNameLength;
    private int passwordLength;
    private int mailOTPLength;

    public AccountClientValidator(
        @Value("${AccountValidator.nameLength}")
        int nameLength,

        @Value("${AccountValidator.mailLength}")
        int mailLength,

        @Value("${AccountValidator.genderLength}")
        int genderLength,

        @Value("${AccountValidator.countryShortNameLength}")
        int countryShortNameLength,

        @Value("${AccountValidator.passwordLength}")
        int passwordLength,

        @Value("${AccountValidator.mailOTPLength}")
        int mailOTPLength
    ) {
        this.nameLength = nameLength;
        this.mailLength = mailLength;
        this.genderLength = genderLength;
        this.countryShortNameLength = countryShortNameLength;
        this.passwordLength = passwordLength;
        this.mailOTPLength = mailOTPLength;
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
        mailValidation(mail, errors);
        birthdayValidation(birthday, errors);
        genderValidation(gender, errors);
        countryShortNameValidation(countryShortName, errors);
        timeZoneValidation(timeZone, errors);

        return errors;
    }

    public Map<String, String> validateWithPassword(
        String phone,
        String firstName,
        String mail,
        String birthday,
        String gender,
        String countryShortName,
        String timeZone,
        String password
    ) {
        LOG.debug("Validating client information phone={} cs={}", phone, countryShortName);

        Map<String, String> errors = validate(phone, firstName, mail, birthday, gender, countryShortName, timeZone);
        passwordValidation(mail, password, errors);
        return errors;
    }

    /* Validation of migration of phone in account. */
    public Map<String, String> validateForPhoneMigration(
        String phone,
        String countryShortName
    ) {
        LOG.info("Validating client information phone={} cs={}", phone, countryShortName);

        Map<String, String> errors = new HashMap<>();

        phoneValidation(phone, errors);
        countryShortNameValidation(countryShortName, errors);

        return errors;
    }

    /* Validation of migration of mail in account. */
    public Map<String, String> validateForMailMigration(String mail) {
        LOG.info("Validating client information mail={}", mail);

        Map<String, String> errors = new HashMap<>();
        mailValidation(mail, errors);
        return errors;
    }

    /* Validation of migration of mail in account. */
    public Map<String, String> validateForMailMigrationAndMailOTP(String mail, String mailOTP) {
        LOG.info("Validating client information mail={}", mail);

        Map<String, String> errors = new HashMap<>();
        mailValidation(mail, errors);
        mailOTPValidation(mailOTP, errors);
        return errors;
    }

    /**
     * Validate password when email exists.
     *
     * @param phone
     * @param errors
     */
    private void phoneValidation(String phone, Map<String, String> errors) {
        if (StringUtils.isBlank(phone)) {
            LOG.info("failed validation phone={}", phone);
            errors.put(ErrorEncounteredJson.REASON, "Phone validation failed.");
            errors.put(AccountMobileService.ACCOUNT_REGISTRATION.PH.name(), phone);
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR, USER_INPUT.name());
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, USER_INPUT.getCode());
        }
    }

    private void firstNameValidation(String firstName, Map<String, String> errors) {
        if (StringUtils.isBlank(firstName) || firstName.length() < nameLength) {
            LOG.info("failed validation firstName={}", firstName);
            errors.put(ErrorEncounteredJson.REASON, "Name validation failed. Minimum length '" + nameLength + "' characters");
            errors.put(AccountMobileService.ACCOUNT_REGISTRATION.FN.name(), StringUtils.isBlank(firstName) ? EMPTY : firstName);
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR, USER_INPUT.name());
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, USER_INPUT.getCode());
        }
    }

    private void mailValidation(String mail, Map<String, String> errors) {
        if (StringUtils.isBlank(mail) || mail.length() < mailLength) {
            LOG.info("failed validation mail={}", mail);
            errors.put(ErrorEncounteredJson.REASON, "Mail validation failed. Minimum length '" + mailLength + "' characters");
            errors.put(AccountMobileService.ACCOUNT_REGISTRATION.EM.name(), StringUtils.isBlank(mail) ? EMPTY : mail);
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR, USER_INPUT.name());
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, USER_INPUT.getCode());
        }
    }

    private void mailOTPValidation(String mailOTP, Map<String, String> errors) {
        if (StringUtils.isBlank(mailOTP) || mailOTP.length() != mailOTPLength) {
            LOG.info("failed validation mail={}", mailOTP);
            errors.put(ErrorEncounteredJson.REASON, "Mail OTP validation failed. Minimum length '" + mailOTPLength + "' characters");
            errors.put("OTP", StringUtils.isBlank(mailOTP) ? EMPTY : mailOTP);
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR, USER_INPUT.name());
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, USER_INPUT.getCode());
        }
    }

    private void birthdayValidation(String birthday, Map<String, String> errors) {
        if (StringUtils.isBlank(birthday) || !DateUtil.DOB_PATTERN.matcher(birthday).matches()) {
            LOG.info("failed validation birthday={}", birthday);
            errors.put(ErrorEncounteredJson.REASON, "Date of birth validation failed.");
            errors.put(AccountMobileService.ACCOUNT_REGISTRATION.BD.name(), StringUtils.isBlank(birthday) ? EMPTY : birthday);
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR, USER_INPUT.name());
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, USER_INPUT.getCode());
        }
    }

    private void genderValidation(String gender, Map<String, String> errors) {
        try {
            if (StringUtils.isBlank(gender) || gender.length() != genderLength) {
                LOG.info("failed validation gender={}", gender);
                errors.put(ErrorEncounteredJson.REASON, "Gender validation failed.");
                errors.put(AccountMobileService.ACCOUNT_REGISTRATION.GE.name(), StringUtils.isBlank(gender) ? EMPTY : gender);
                errors.put(ErrorEncounteredJson.SYSTEM_ERROR, USER_INPUT.name());
                errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, USER_INPUT.getCode());
            } else {
                GenderEnum.valueOf(gender);
            }
        } catch (IllegalArgumentException e) {
            LOG.error("Unsupported Gender type gender={} reason={}", gender, e.getLocalizedMessage(), e);
            errors.put(ErrorEncounteredJson.REASON, "Unsupported gender found.");
            errors.put(AccountMobileService.ACCOUNT_REGISTRATION.GE.name(), StringUtils.isBlank(gender) ? EMPTY : gender);
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR, USER_INPUT.name());
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, USER_INPUT.getCode());
        }
    }

    private void countryShortNameValidation(String countryShortName, Map<String, String> errors) {
        if (StringUtils.isBlank(countryShortName) || countryShortName.length() != countryShortNameLength) {
            LOG.info("failed validation countryShortName={}", countryShortName);
            errors.put(ErrorEncounteredJson.REASON, "Country name validation failed.");
            errors.put(AccountMobileService.ACCOUNT_REGISTRATION.CS.name(), StringUtils.isBlank(countryShortName) ? EMPTY : countryShortName);
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR, USER_INPUT.name());
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, USER_INPUT.getCode());
        }
    }

    private void timeZoneValidation(String timeZone, Map<String, String> errors) {
        if (StringUtils.isBlank(timeZone)) {
            LOG.info("failed validation timeZone={}", timeZone);
            errors.put(ErrorEncounteredJson.REASON, "Timezone validation failed.");
            errors.put(AccountMobileService.ACCOUNT_REGISTRATION.TZ.name(), StringUtils.isBlank(timeZone) ? EMPTY : timeZone);
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR, USER_INPUT.name());
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, USER_INPUT.getCode());
        }
    }

    private void passwordValidation(String mail, String password, Map<String, String> errors) {
        if (StringUtils.isNotBlank(mail)) {
            if (StringUtils.isBlank(password) || password.length() < passwordLength) {
                LOG.info("failed validation password={}", AUTH_KEY_HIDDEN);
                errors.put(ErrorEncounteredJson.REASON, "Password validation failed.");
                /* Do not send password back. Hidden for security reason. */
                errors.put(AccountMobileService.ACCOUNT_REGISTRATION_MERCHANT.PW.name(), StringUtils.isBlank(password) ? EMPTY : "********");
                errors.put(ErrorEncounteredJson.SYSTEM_ERROR, USER_INPUT.name());
                errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, USER_INPUT.getCode());
            }
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
