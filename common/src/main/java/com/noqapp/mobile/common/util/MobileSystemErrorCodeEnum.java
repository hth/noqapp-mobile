package com.noqapp.mobile.common.util;

import static com.noqapp.mobile.common.util.ErrorTypeEnum.*;

/**
 * Error code to share between APP and Mobile API.
 * User: hitender
 * Date: 1/10/17 10:57 PM
 */
public enum MobileSystemErrorCodeEnum {
    /** Can be user input or mobile submission. */
    USER_INPUT("1000", ERROR),

    /** Issue in mobile data submitted. */
    MOBILE("2000", ERROR),

    /** When cannot parse JSON sent to Mobile Server from mobile devices. */
    MOBILE_JSON("2010", ERROR),
    MOBILE_UPGRADE("2022", ALERT),
    MOBILE_UPLOAD("2023", ERROR),
    MOBILE_UPLOAD_NO_SIZE("2024", ALERT),
    MOBILE_UPLOAD_EXCEED_SIZE("2025", ALERT),
    MOBILE_UPLOAD_UNSUPPORTED_FORMAT("2026", ALERT),

    MOBILE_ACTION_NOT_PERMITTED("2101", ALERT),

    USER_ALREADY_IN_QUEUE("3030", ALERT),
    MERCHANT_COULD_NOT_ACQUIRE("3050", ALERT),

    USER_EXISTING("4010", ALERT),
    USER_NOT_FOUND("4012", ALERT),
    USER_SOCIAL("4016", ALERT),
    MAIL_OTP_FAILED("4020", ERROR),
    USER_MAX_DEPENDENT("4050", ALERT),

    /** Medical. */
    MEDICAL_RECORD_ENTRY_DENIED("4101", ERROR),
    MEDICAL_RECORD_ACCESS_DENIED("4102", ERROR),
    BUSINESS_NOT_AUTHORIZED("4120", ERROR),
    BUSINESS_CUSTOMER_ID_DOES_NOT_EXISTS("4121", ALERT),
    BUSINESS_CUSTOMER_ID_EXISTS("4122", ALERT),

    /** Orders. */
    PURCHASE_ORDER_NOT_FOUND("4204", ALERT),
    PURCHASE_ORDER_FAILED_TO_CANCEL("4208", ALERT),

    /** Mobile application related issue. */
    SEVERE("5000", ERROR),
    DEVICE_DETAIL_MISSING("5010", ERROR),

    /** Not mobile web application. */
    WEB_APPLICATION("6000", ERROR);

    private String code;
    private ErrorTypeEnum errorType;

    MobileSystemErrorCodeEnum(String code, ErrorTypeEnum errorType) {
        this.code = code;
        this.errorType = errorType;
    }

    public String getCode() {
        return code;
    }

    public ErrorTypeEnum getErrorType() {
        return errorType;
    }
}
