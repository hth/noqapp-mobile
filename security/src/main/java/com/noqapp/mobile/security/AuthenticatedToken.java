package com.noqapp.mobile.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.noqapp.domain.UserAccountEntity;
import com.noqapp.service.AccountService;
import org.junit.jupiter.api.Assertions;

/**
 * User: hitender
 * Date: 6/7/14 1:12 AM
 */
@Component
@SuppressWarnings ({"PMD.BeanMembersShouldSerialize"})
public class AuthenticatedToken {
    private AccountService accountService;

    @Autowired
    public AuthenticatedToken(AccountService accountService) {
        this.accountService = accountService;
    }

    protected String getUserAuthenticationKey(String userId) {
        UserAccountEntity userAccount = accountService.findByUserId(userId);
        Assertions.assertNotNull(userAccount, "Not found UserAccount with userId=" + userId);
        return userAccount.getUserAuthentication().getAuthenticationKey();
    }
}
