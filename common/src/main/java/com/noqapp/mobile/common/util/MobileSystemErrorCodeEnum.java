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

    /** System alerts or warning when something out of ordinary happens. */
    USER_ALREADY_IN_QUEUE("3030", ALERT),
    MERCHANT_COULD_NOT_ACQUIRE("3050", ALERT),
    STORE_OFFLINE("3060", ALERT),
    STORE_DAY_CLOSED("3061", ALERT),
    STORE_TEMP_DAY_CLOSED("3062", ALERT),
    STORE_PREVENT_JOIN("3063", ALERT),
    STORE_NO_LONGER_EXISTS("3064", ALERT),
    QUEUE_NOT_STARTED("3101", ALERT),
    QUEUE_NOT_RE_STARTED("3102", ALERT),

    /** User related. */
    USER_EXISTING("4010", ALERT),
    USER_NOT_FOUND("4012", ALERT),
    USER_SOCIAL("4016", ALERT),
    MAIL_OTP_FAILED("4020", ERROR),
    USER_MAX_DEPENDENT("4050", ALERT),
    CHANGE_USER_IN_QUEUE("4060", ALERT),
    FAILED_FINDING_ADDRESS("4070", ALERT),

    /** Medical. */
    MEDICAL_RECORD_ENTRY_DENIED("4101", ERROR),
    MEDICAL_RECORD_ACCESS_DENIED("4102", ERROR),
    MEDICAL_RECORD_DOES_NOT_EXISTS("4104", ERROR),
    MEDICAL_RECORD_POPULATED_WITH_LAB("4105", ERROR),
    BUSINESS_NOT_AUTHORIZED("4120", ERROR),
    BUSINESS_CUSTOMER_ID_DOES_NOT_EXISTS("4121", ALERT),
    BUSINESS_CUSTOMER_ID_EXISTS("4122", ALERT),
    MEDICAL_PROFILE_DOES_NOT_EXISTS("4204", ALERT),
    MEDICAL_PROFILE_CANNOT_BE_CHANGED("4206", ERROR),

    /** Orders. */
    PURCHASE_ORDER_PRICE_MISMATCH("4203", ALERT),
    PURCHASE_ORDER_NOT_FOUND("4204", ALERT),
    PRODUCT_PRICE_CANNOT_BE_ZERO("4205", ALERT),
    PURCHASE_ORDER_FAILED_TO_CANCEL_AS_EXTERNALLY_PAID("4206", ALERT),
    PURCHASE_ORDER_FAILED_TO_CANCEL_PARTIAL_PAY("4207", ALERT),
    PURCHASE_ORDER_FAILED_TO_CANCEL("4208", ALERT),
    PURCHASE_ORDER_ALREADY_CANCELLED("4209", ALERT),
    PURCHASE_ORDER_CANNOT_ACTIVATE("4210", ALERT),
    ORDER_PAYMENT_UPDATE_FAILED("4211", ALERT),
    ORDER_PAYMENT_PAID_ALREADY_FAILED("4212", ALERT),
    PURCHASE_ORDER_PRODUCT_NOT_FOUND("4215", ALERT),
    FAILED_PLACING_MEDICAL_ORDER_AS_INCORRECT_BUSINESS("4216", ALERT),

    /** Queue. */
    QUEUE_JOIN_FAILED_PAYMENT_CALL_REQUEST("4304", ERROR),
    QUEUE_JOIN_PAYMENT_FAILED("4305", ERROR),
    QUEUE_NO_SERVICE_NO_PAY("4306", ERROR),

    /** Survey. */
    SURVEY_NOT_FOUND("4404", ALERT),

    /** Transaction. */
    TRANSACTION_GATEWAY_DEFAULT("4500", ALERT),
    SERVICE_PAYMENT_NOT_ALLOWED_FOR_THIS_BUSINESS_TYPE("4501", ALERT),

    /* Appointments. */
    CANNOT_ACCEPT_APPOINTMENT("4701", ALERT),
    CANNOT_BOOK_APPOINTMENT("4702", ALERT),
    FAILED_TO_FIND_APPOINTMENT("4704", ALERT),
    FAILED_TO_CANCEL_APPOINTMENT("4705", ALERT),
    FAILED_TO_RESCHEDULE_APPOINTMENT("4706", ALERT),
    APPOINTMENT_ALREADY_EXISTS("4710", ALERT),
    APPOINTMENT_ACTION_NOT_PERMITTED("4720", ALERT),

    /** Mobile application related issue. */
    SEVERE("5000", ERROR),
    DEVICE_DETAIL_MISSING("5010", ERROR),
    ACCOUNT_INACTIVE("5015", ERROR),

    /** Promotions. */
    PROMOTION_ACCESS_DENIED("7001", ALERT),
    COUPON_NOT_APPLICABLE("7003", ALERT),
    COUPON_REMOVAL_FAILED("7006", ALERT),

    /** Not mobile web application. */
    WEB_APPLICATION("9000", ERROR);

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
