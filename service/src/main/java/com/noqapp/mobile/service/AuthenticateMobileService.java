package com.noqapp.mobile.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.noqapp.domain.UserAccountEntity;
import com.noqapp.repository.UserAccountManager;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * User: hitender
 * Date: 1/9/17 12:00 PM
 */
@SuppressWarnings ({
        "PMD.BeanMembersShouldSerialize",
        "PMD.LocalVariableCouldBeFinal",
        "PMD.MethodArgumentCouldBeFinal",
        "PMD.LongVariable"
})
@Service
public class AuthenticateMobileService {
    private static final Logger LOG = LoggerFactory.getLogger(AuthenticateMobileService.class);

    private UserAccountManager userAccountManager;

    @Autowired
    public AuthenticateMobileService(UserAccountManager userAccountManager) {
        this.userAccountManager = userAccountManager;
    }

    public boolean hasAccess(String mail, String auth) {
        return findUserAccount(mail, auth) != null;
    }

    UserAccountEntity findUserAccount(String mail, String auth) {
        UserAccountEntity userAccount = userAccountManager.findByUserId(mail);
        try {
            if (userAccount == null) {
                return null;
            } else {
                return userAccount.getUserAuthentication().getAuthenticationKey().equals(
                        URLDecoder.decode(auth, "UTF-8")) ? userAccount : null;
            }
        } catch (UnsupportedEncodingException e) {
            LOG.error("Auth decoding issue for user={}, reason={}", mail, e.getLocalizedMessage(), e);
            return null;
        }
    }

    /**
     * Finds authenticated queue user id.
     *
     * @param mail
     * @param auth
     * @return
     */
    public String getQueueUserId(String mail, String auth) {
        UserAccountEntity userAccount = findUserAccount(mail, auth);
        if (null != userAccount) {
            return userAccount.getQueueUserId();
        }
        return null;
    }
}
