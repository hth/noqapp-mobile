package com.noqapp.portal.service;

import com.noqapp.domain.BusinessUserEntity;
import com.noqapp.domain.BusinessUserStoreEntity;
import com.noqapp.domain.UserAccountEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.json.JsonProfile;
import com.noqapp.domain.json.JsonUserAddressList;
import com.noqapp.repository.BusinessUserManager;
import com.noqapp.repository.BusinessUserStoreManager;
import com.noqapp.service.AccountService;
import com.noqapp.service.UserAddressService;
import com.noqapp.service.UserProfilePreferenceService;
import com.noqapp.social.exception.AccountNotActiveException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * hitender
 * 3/23/20 5:02 PM
 */
@SuppressWarnings({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@Service
public class AccountPortalService {
    private static final Logger LOG = LoggerFactory.getLogger(AccountPortalService.class);

    private AccountService accountService;
    private UserAddressService userAddressService;
    private UserProfilePreferenceService userProfilePreferenceService;
    private BusinessUserManager businessUserManager;
    private BusinessUserStoreManager businessUserStoreManager;

    private int queueLimit;

    @Autowired
    public AccountPortalService(
        @Value("${BusinessUserStoreService.queue.limit}")
        int queueLimit,

        AccountService accountService,
        UserAddressService userAddressService,
        UserProfilePreferenceService userProfilePreferenceService,
        BusinessUserManager businessUserManager,
        BusinessUserStoreManager businessUserStoreManager
    ) {
        this.queueLimit = queueLimit;

        this.accountService = accountService;
        this.userAddressService = userAddressService;
        this.userProfilePreferenceService = userProfilePreferenceService;
        this.businessUserManager = businessUserManager;
        this.businessUserStoreManager = businessUserStoreManager;
    }

    public JsonProfile getProfileAsJson(String qid) {
        UserAccountEntity userAccount = accountService.findByQueueUserId(qid);
        if (!userAccount.isActive()) {
            LOG.warn("Account In Active {} qid={}", userAccount.getAccountInactiveReason(), qid);
            throw new AccountNotActiveException("Account is blocked. Contact support.");
        }
        return getProfileAsJson(qid, userAccount);
    }

    public JsonProfile getProfileAsJson(String qid, UserAccountEntity userAccount) {
        UserProfileEntity userProfile = accountService.findProfileByQueueUserId(qid);
        JsonUserAddressList jsonUserAddressList = userAddressService.getAllAsJson(qid);
        JsonProfile jsonProfile = JsonProfile.newInstance(userProfile, userAccount)
            .setJsonUserAddresses(jsonUserAddressList.getJsonUserAddresses())
            .setJsonUserPreference(userProfilePreferenceService.findUserPreferenceAsJson(qid));

        switch (userProfile.getLevel()) {
            case S_MANAGER:
            case Q_SUPERVISOR:
                BusinessUserEntity businessUser = businessUserManager.findByQid(userProfile.getQueueUserId());
                jsonProfile.setBizNameId(businessUser.getBizName().getId());
                List<BusinessUserStoreEntity> businessUserStores = businessUserStoreManager.getQueues(qid, queueLimit);
                for (BusinessUserStoreEntity businessUserStore : businessUserStores) {
                    jsonProfile.addCodeQRAndBizStoreId(businessUserStore.getCodeQR(), businessUserStore.getBizStoreId());
                }
                break;
            default:
                //Do not do anything otherwise
        }

        if (null != userProfile.getQidOfDependents()) {
            for (String qidOfDependent : userProfile.getQidOfDependents()) {
                jsonProfile.addDependents(
                    JsonProfile.newInstance(
                        accountService.findProfileByQueueUserId(qidOfDependent),
                        accountService.findByQueueUserId(qidOfDependent)));
            }
        }

        return jsonProfile;
    }
}
