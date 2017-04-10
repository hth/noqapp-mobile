package com.noqapp.mobile.common.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Converts error message to JSON.
 * User: hitender
 * Date: 1/10/17 10:57 PM
 */
@SuppressWarnings ({
        "PMD.BeanMembersShouldSerialize",
        "PMD.LocalVariableCouldBeFinal",
        "PMD.MethodArgumentCouldBeFinal",
        "PMD.LongVariable"
})
public class ErrorEncounteredJson {

    public static final String SYSTEM_ERROR = "systemError";
    public static final String SYSTEM_ERROR_CODE = "systemErrorCode";
    public static final String REASON = "reason";
    public static final String ERROR = "error";

    @SuppressWarnings ("unused")
    private ErrorEncounteredJson() {
    }

    public static String toJson(Map<String, String> errors) {
        JsonObject error = new JsonObject();
        for (String key : errors.keySet()) {
            error.addProperty(key, errors.get(key));
        }

        JsonObject result = new JsonObject();
        result.add(ERROR, error);

        return new Gson().toJson(result);
    }

    public static String toJson(String reason, MobileSystemErrorCodeEnum systemErrorCode) {
        Map<String, String> errors = new HashMap<>();
        errors.put("reason", reason);
        errors.put("systemError", systemErrorCode.name());
        errors.put("systemErrorCode", systemErrorCode.getCode());
        return toJson(errors);
    }
}