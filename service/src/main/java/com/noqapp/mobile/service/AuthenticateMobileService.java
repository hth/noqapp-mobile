package com.noqapp.mobile.service;

import com.noqapp.common.utils.Constants;
import com.noqapp.domain.UserAccountEntity;
import com.noqapp.repository.UserAccountManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

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

    UserAccountEntity findUserAccount(String mail, String auth) {
        UserAccountEntity userAccount = userAccountManager.findByUserId(mail);
        if (null == userAccount) {
            return null;
        } else {
            return userAccount.getUserAuthentication().getAuthenticationKey().equals(URLDecoder.decode(auth, Constants.CHAR_SET_UTF8))
                ? userAccount
                : null;
        }
    }

    /** Finds authenticated queue user id. */
    @Cacheable(value = "mail-auth")
    public String getQueueUserId(String mail, String auth) {
        UserAccountEntity userAccount = findUserAccount(mail, auth);
        if (null != userAccount) {
            return userAccount.getQueueUserId();
        }
        return null;
    }

    @CacheEvict(value = "mail-auth")
    public void evictExisting(String mail, String auth) {
        LOG.info("Removed cache from mail-auth {}", mail);
    }

    public UserAccountEntity findByQueueUserId(String qid) {
        return userAccountManager.findByQueueUserId(qid);
    }
}
