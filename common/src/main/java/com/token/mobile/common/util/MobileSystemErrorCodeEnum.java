package com.token.mobile.common.util;

/**
 * Error code to share between APP and Mobile API.
 * User: hitender
 * Date: 1/10/17 10:57 PM
 */
public enum MobileSystemErrorCodeEnum {
    /** Can be user input or mobile submission. */
    USER_INPUT("100"),

    /** Mobile data submitted. */
    MOBILE("200"),

    /** When cannot parse JSON sent to Mobile Server from mobile devices. */
    MOBILE_JSON("210"),

    DOCUMENT_UPLOAD("300"),

    AUTHENTICATION("400"),
    USER_EXISTING("410"),
    USER_NOT_FOUND("412"),
    USER_SOCIAL("416"),
    REGISTRATION_TURNED_OFF("430"),

    /** Mobile application related issue. */
    SEVERE("500"),

    /** Not mobile web application. */
    WEB_APPLICATION("600");

    private String code;

    MobileSystemErrorCodeEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
