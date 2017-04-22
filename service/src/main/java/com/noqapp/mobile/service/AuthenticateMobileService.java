package com.noqapp.mobile.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.noqapp.domain.UserAccountEntity;
import com.noqapp.repository.UserAccountManager;

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
        UserAccountEntity userAccountEntity = userAccountManager.findByUserId(mail);
        if (userAccountEntity == null) {
            LOG.info("Found User Account NOPE");
            return null;
        } else {
            LOG.info("Found User Account rid={}", userAccountEntity.getReceiptUserId());
//                return userAccountEntity.getUserAuthentication().getAuthenticationKey().equals(
//                        URLDecoder.decode(auth, "UTF-8")) ? userAccountEntity : null;

            return userAccountEntity;
        }
    }

    /**
     * Finds authenticated receipt user id.
     *
     * @param mail
     * @param auth
     * @return
     */
    public String getReceiptUserId(String mail, String auth) {
        UserAccountEntity userAccount = findUserAccount(mail, auth);
        if (null != userAccount) {
            LOG.info("Found user");
            return userAccount.getReceiptUserId();
        }
        LOG.info("Returning null");
        return null;
    }
}
