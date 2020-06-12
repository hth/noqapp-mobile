package com.noqapp.mobile.view.validator;

import static org.junit.jupiter.api.Assertions.*;

import com.noqapp.mobile.service.AccountMobileService;
import com.noqapp.mobile.view.controller.open.StoreDetailController;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * hitender
 * 6/12/20 3:18 AM
 */
class AccountClientValidatorTest {

    private AccountClientValidator accountClientValidator;

    @BeforeEach
    void setUp() {
        accountClientValidator = new AccountClientValidator(3, 5, 1, 2, 6, 6);
    }

    @Test
    void validate() {
        Map<String, String> responseMailValidationFailure = accountClientValidator.validate(
            "19234234",
            "First Name",
            "m r@r.com",
            "1900-01-01",
            "M",
            "IN",
            "Asia/Calcutta"
        );

        assertEquals(true, responseMailValidationFailure.containsKey(AccountMobileService.ACCOUNT_REGISTRATION.EM.name()));
    }
}
