package com.noqapp.mobile.view.validator;

import com.noqapp.domain.types.medical.BloodTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * hitender
 * 6/21/18 2:29 PM
 */
public class UserMedicalProfileValidator {
    private static final Logger LOG = LoggerFactory.getLogger(UserMedicalProfileValidator.class);

    public Map<String, String> validate(BloodTypeEnum bloodType) {
        LOG.debug("Validating user professional profile information bloodType={}", bloodType);

        Map<String, String> errors = new HashMap<>();

        bloodTypeValidation(bloodType, errors);
        return errors;
    }

    //TODO no need for blood type validation. But keeping it there for now.
    private void bloodTypeValidation(BloodTypeEnum bloodType, Map<String, String> errors) {

    }
}
