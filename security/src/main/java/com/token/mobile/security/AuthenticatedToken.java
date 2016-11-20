package com.token.mobile.security;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.token.domain.UserAccountEntity;
import com.token.service.AccountService;

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

    protected String getUserAuthenticationKey(String receiptUserId) {
        UserAccountEntity userAccountEntity = accountService.findByUserId(receiptUserId);
        Assert.notNull(userAccountEntity);
        return userAccountEntity.getUserAuthentication().getAuthenticationKey();
    }
}
