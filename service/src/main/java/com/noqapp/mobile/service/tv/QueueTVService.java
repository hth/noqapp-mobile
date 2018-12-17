package com.noqapp.mobile.service.tv;

import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.BusinessUserStoreEntity;
import com.noqapp.domain.ProfessionalProfileEntity;
import com.noqapp.domain.annotation.Television;
import com.noqapp.domain.json.JsonQueuePersonList;
import com.noqapp.domain.json.tv.JsonQueueTV;
import com.noqapp.domain.json.tv.JsonQueueTVList;
import com.noqapp.service.BizService;
import com.noqapp.service.BusinessUserStoreService;
import com.noqapp.service.ProfessionalProfileService;
import com.noqapp.service.QueueService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * hitender
 * 2018-12-17 19:10
 */
@Service
public class QueueTVService {

    private QueueService queueService;
    private BusinessUserStoreService businessUserStoreService;
    private ProfessionalProfileService professionalProfileService;

    @Autowired
    public QueueTVService(
        QueueService queueService,
        BusinessUserStoreService businessUserStoreService,
        ProfessionalProfileService professionalProfileService
    ) {
        this.queueService = queueService;
        this.businessUserStoreService = businessUserStoreService;
        this.professionalProfileService = professionalProfileService;
    }

    public JsonQueuePersonList findAllActiveInQueue(String codeQR) {
        return queueService.findYetToBeServed(codeQR);
    }

    private BusinessUserStoreEntity findOneByCodeQR(String codeQR) {
        return businessUserStoreService.findOneByCodeQR(codeQR);
    }

    public String findAllActiveInQueue(List<String> codeQRs) {
        JsonQueueTVList jsonQueueTVList = new JsonQueueTVList();
        for (String codeQR : codeQRs) {
            BusinessUserStoreEntity businessUserStore = findOneByCodeQR(codeQR);
            ProfessionalProfileEntity professionalProfile = professionalProfileService.findByQid(businessUserStore.getQueueUserId());
            JsonQueueTV jsonQueue = new JsonQueueTV()
                .setCodeQR(codeQR)
                .setWebProfileId(professionalProfile.getWebProfileId())
                .setJsonQueuedPersonTVList(queueService.findYetToBeServedForTV(codeQR));
            jsonQueueTVList.addQueue(jsonQueue);
        }

        return jsonQueueTVList.toString();
    }
}
