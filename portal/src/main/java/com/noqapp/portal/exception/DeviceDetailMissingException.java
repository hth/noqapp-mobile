package com.noqapp.portal.exception;

/**
 * hitender
 * 9/24/18 12:55 PM
 */
public class DeviceDetailMissingException extends RuntimeException {

    public DeviceDetailMissingException(String message) {
        super(message);
    }

    public DeviceDetailMissingException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
