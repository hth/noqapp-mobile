package com.noqapp.mobile.service.exception;

/**
 * hitender
 * 2018-12-05 18:07
 */
public class AccountNotActiveException extends RuntimeException {

    public AccountNotActiveException(String message) {
        super(message);
    }

    public AccountNotActiveException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
