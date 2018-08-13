package com.noqapp.mobile.view.validator;

import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.USER_INPUT;

import com.noqapp.common.utils.DateUtil;
import com.noqapp.domain.json.JsonNameDatePair;
import com.noqapp.domain.json.JsonProfessionalProfile;
import com.noqapp.mobile.common.util.ErrorEncounteredJson;
import com.noqapp.service.ProfessionalProfileService;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * hitender
 * 6/12/18 3:57 PM
 */
@SuppressWarnings({
        "PMD.BeanMembersShouldSerialize",
        "PMD.LocalVariableCouldBeFinal",
        "PMD.MethodArgumentCouldBeFinal",
        "PMD.LongVariable"
})
@Component
public class ProfessionalProfileValidator {
    private static final Logger LOG = LoggerFactory.getLogger(ProfessionalProfileValidator.class);

    private ProfessionalProfileService professionalProfileService;

    @Autowired
    public ProfessionalProfileValidator(ProfessionalProfileService professionalProfileService) {
        this.professionalProfileService = professionalProfileService;
    }

    public Map<String, String> validate(JsonProfessionalProfile jsonProfessionalProfile) {
        Map<String, String> errors = new HashMap<>();

        if (jsonProfessionalProfile.getLicenses().isEmpty() && jsonProfessionalProfile.getEducation().isEmpty()) {
            LOG.error("Education or License in professional profile cannot be empty. Please fill these up first.");
            errors.put(ErrorEncounteredJson.REASON, "Education or License in professional profile cannot be empty. Please fill these up first.");
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR, USER_INPUT.name());
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, USER_INPUT.getCode());
        }

        if (StringUtils.isNotBlank(jsonProfessionalProfile.getPracticeStart()) && !DateUtil.DOB_PATTERN.matcher(jsonProfessionalProfile.getPracticeStart()).matches()) {
            LOG.error("Practicing Since should be of format " + DateUtil.SDF_YYYY_MM_DD.toPattern());
            errors.put(ErrorEncounteredJson.REASON, "Practicing Since should be of format " + DateUtil.SDF_YYYY_MM_DD.toPattern());
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR, USER_INPUT.name());
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, USER_INPUT.getCode());
        }

        if (StringUtils.isNotBlank(jsonProfessionalProfile.getAboutMe()) && jsonProfessionalProfile.getAboutMe().length() > 256) {
            LOG.error("About me exceeds length {} > 256 " + jsonProfessionalProfile.getAboutMe().length());
            errors.put(ErrorEncounteredJson.REASON, "About me should not exceed 256 characters");
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR, USER_INPUT.name());
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, USER_INPUT.getCode());
        }

        for (JsonNameDatePair jsonNameDatePair : jsonProfessionalProfile.getEducation()) {
            validateNameDatePair(errors, jsonNameDatePair, "Education");
        }

        for(JsonNameDatePair jsonNameDatePair : jsonProfessionalProfile.getLicenses()) {
            validateNameDatePair(errors, jsonNameDatePair, "Licenses");
        }

        for(JsonNameDatePair jsonNameDatePair : jsonProfessionalProfile.getAwards()) {
            validateNameDatePair(errors, jsonNameDatePair, "Awards");
        }

        return errors;
    }

    private void validateNameDatePair(Map<String, String> errors, JsonNameDatePair jsonNameDatePair, String fieldName) {
        if (StringUtils.isNotBlank(jsonNameDatePair.getName()) && jsonNameDatePair.getName().length() > 60) {
            LOG.error(fieldName + " Name exceeds length {} > 60 " + jsonNameDatePair.getName().length());
            errors.put(ErrorEncounteredJson.REASON, fieldName + " About me should not exceed 60 characters");
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR, USER_INPUT.name());
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, USER_INPUT.getCode());
        }

        if (StringUtils.isBlank(jsonNameDatePair.getMonthYear()) && !DateUtil.DOB_PATTERN.matcher(jsonNameDatePair.getMonthYear()).matches()) {
            LOG.error(fieldName + " Date should be of format " + DateUtil.SDF_YYYY_MM_DD.toPattern());
            errors.put(ErrorEncounteredJson.REASON, fieldName + " Date should be of format " + DateUtil.SDF_YYYY_MM_DD.toPattern());
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR, USER_INPUT.name());
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, USER_INPUT.getCode());
        }
    }
}
