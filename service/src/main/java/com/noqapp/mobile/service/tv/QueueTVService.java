package com.noqapp.mobile.service.tv;

import com.noqapp.domain.BusinessUserStoreEntity;
import com.noqapp.domain.ProfessionalProfileEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.json.JsonQueuePersonList;
import com.noqapp.domain.json.tv.JsonQueueTV;
import com.noqapp.domain.json.tv.JsonQueueTVList;
import com.noqapp.domain.types.UserLevelEnum;
import com.noqapp.mobile.service.AccountMobileService;
import com.noqapp.service.BusinessUserStoreService;
import com.noqapp.service.ProfessionalProfileService;
import com.noqapp.service.QueueService;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * hitender
 * 2018-12-17 19:10
 */
@Service
public class QueueTVService {
    private static final Logger LOG = LoggerFactory.getLogger(QueueTVService.class);

    private AccountMobileService accountMobileService;
    private QueueService queueService;
    private BusinessUserStoreService businessUserStoreService;
    private ProfessionalProfileService professionalProfileService;

    @Autowired
    public QueueTVService(
        AccountMobileService accountMobileService,
        QueueService queueService,
        BusinessUserStoreService businessUserStoreService,
        ProfessionalProfileService professionalProfileService
    ) {
        this.accountMobileService = accountMobileService;
        this.queueService = queueService;
        this.businessUserStoreService = businessUserStoreService;
        this.professionalProfileService = professionalProfileService;
    }

    public JsonQueuePersonList findAllActiveInQueue(String codeQR) {
        return queueService.findYetToBeServed(codeQR);
    }

    private BusinessUserStoreEntity findUserManagingStoreWithCodeQRAndUserLevel(String codeQR) {
        return businessUserStoreService.findUserManagingStoreWithCodeQRAndUserLevel(codeQR);
    }

    public String findAllActiveInQueue(List<String> codeQRs) {
        JsonQueueTVList jsonQueueTVList = new JsonQueueTVList();
        for (String codeQR : codeQRs) {
            LOG.info("Lookup for codeQR={}", codeQR);
            try {
                BusinessUserStoreEntity businessUserStore = findUserManagingStoreWithCodeQRAndUserLevel(codeQR);
                JsonQueueTV jsonQueueTV = new JsonQueueTV()
                    .setCodeQR(codeQR)
                    .setJsonQueuedPersonTVList(queueService.findYetToBeServedForTV(codeQR));

                ProfessionalProfileEntity professionalProfile = professionalProfileService.findByQid(businessUserStore.getQueueUserId());
                if (null != professionalProfile) {
                    jsonQueueTV.setEducation(professionalProfile.getEducationAsJson());
                }

                UserProfileEntity userProfile = accountMobileService.findProfileByQueueUserId(businessUserStore.getQueueUserId());
                if (StringUtils.isNotBlank(userProfile.getProfileImage())) {
                    jsonQueueTV.setProfileImage(userProfile.getProfileImage());
                }

                jsonQueueTVList.addQueue(jsonQueueTV);
            } catch (Exception e) {
                LOG.error("Failed to fetch reason={}", e.getLocalizedMessage(), e);
            }
        }
        LOG.info("Returned queue size={}", jsonQueueTVList.getQueues().size());
        return jsonQueueTVList.asJson();
    }
}
