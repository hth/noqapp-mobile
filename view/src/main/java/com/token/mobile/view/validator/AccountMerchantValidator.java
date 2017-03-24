package com.token.mobile.view.validator;

import static com.token.mobile.common.util.MobileSystemErrorCodeEnum.USER_INPUT;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

import com.token.mobile.common.util.ErrorEncounteredJson;
import com.token.mobile.view.controller.api.ManageQueueController;

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
            String mail,
            String firstName,
            String password,
            String birthday
    ) {
        LOG.info("failed validation mail={} firstName={} password={}", mail, firstName, ManageQueueController.AUTH_KEY_HIDDEN);

        Map<String, String> errors = new HashMap<>();
        errors.put(ErrorEncounteredJson.REASON, "Failed data validation.");

        firstNameValidation(firstName, errors);
        if (StringUtils.isNotBlank(mail)) {
            mailValidation(mail, errors);
        }
        if (StringUtils.isNotBlank(birthday)) {
            birthdayValidation(birthday, errors);
        }
        passwordValidation(password, errors);

        errors.put(ErrorEncounteredJson.SYSTEM_ERROR, USER_INPUT.name());
        errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, USER_INPUT.getCode());
        return errors;
    }
}