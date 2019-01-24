package com.noqapp.mobile.view.controller.api;

import com.noqapp.domain.UserProfileEntity;
import com.noqapp.mobile.service.AccountMobileService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * hitender
 * 2019-01-23 15:44
 */
abstract class CommonHelper {
    private static final Logger LOG = LoggerFactory.getLogger(CommonHelper.class);

    private AccountMobileService accountMobileService;

    CommonHelper(AccountMobileService accountMobileService) {
        this.accountMobileService = accountMobileService;
    }

    /**
     * Check if the person is self or a guardian of the dependent.
     *
     * @param qidOfSubmitter Guardian
     * @param qid            Qid of the person who's record is being modified
     * @return
     */
    UserProfileEntity checkSelfOrDependent(String qidOfSubmitter, String qid) {
        UserProfileEntity userProfile = accountMobileService.findProfileByQueueUserId(qid);
        if (!qidOfSubmitter.equalsIgnoreCase(userProfile.getQueueUserId())) {
            UserProfileEntity userProfileGuardian = accountMobileService.checkUserExistsByPhone(userProfile.getGuardianPhone());

            if (!qidOfSubmitter.equalsIgnoreCase(userProfileGuardian.getQueueUserId())) {
                LOG.info("Profile user does not match with QID of submitter nor is a guardian {} {}",
                    qidOfSubmitter,
                    userProfileGuardian.getQueueUserId());
                return null;
            }
        }
        return userProfile;
    }
}
