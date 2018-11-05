package com.noqapp.mobile.service.exception;

/**
 * hitender
 * 11/5/18 1:05 PM
 */
public class StoreNoLongerExistsException extends RuntimeException {
    public StoreNoLongerExistsException(String message) {
        super(message);
    }

    public StoreNoLongerExistsException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
