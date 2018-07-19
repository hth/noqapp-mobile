package com.noqapp.mobile.common.util;

/**
 * Error code to share between APP and Mobile API.
 * User: hitender
 * Date: 1/10/17 10:57 PM
 */
public enum MobileSystemErrorCodeEnum {
    /** Can be user input or mobile submission. */
    USER_INPUT("1000"),

    /** Issue in mobile data submitted. */
    MOBILE("2000"),

    /** When cannot parse JSON sent to Mobile Server from mobile devices. */
    MOBILE_JSON("2010"),
    MOBILE_UPGRADE("2022"),
    MOBILE_UPLOAD("2023"),
    MOBILE_UPLOAD_NO_SIZE("2024"),
    MOBILE_UPLOAD_EXCEED_SIZE("2025"),
    MOBILE_UPLOAD_UNSUPPORTED_FORMAT("2026"),

    REMOTE_JOIN_EMPTY("3000"),
    MERCHANT_COULD_NOT_ACQUIRE("3050"),

    USER_EXISTING("4010"),
    USER_NOT_FOUND("4012"),
    USER_SOCIAL("4016"),

    /** Medical. */
    MEDICAL_RECORD_ENTRY_DENIED("4101"),
    MEDICAL_RECORD_ACCESS_DENIED("4102"),
    BUSINESS_NOT_AUTHORIZED("4120"),
    BUSINESS_CUSTOMER_ID_DOES_NOT_EXISTS("4121"),
    BUSINESS_CUSTOMER_ID_EXISTS("4122"),

    /** Mobile application related issue. */
    SEVERE("5000"),

    /** Not mobile web application. */
    WEB_APPLICATION("6000");

    private String code;

    MobileSystemErrorCodeEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
