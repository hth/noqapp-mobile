package com.noqapp.mobile.service;

import static com.noqapp.common.utils.Constants.UNDER_SCORE;

import com.noqapp.domain.BizNameEntity;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.BusinessUserStoreEntity;
import com.noqapp.domain.NotificationMessageEntity;
import com.noqapp.domain.ProfessionalProfileEntity;
import com.noqapp.domain.StoreHourEntity;
import com.noqapp.domain.TokenQueueEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.helper.CommonHelper;
import com.noqapp.domain.json.JsonCategory;
import com.noqapp.domain.json.JsonQueueList;
import com.noqapp.domain.types.BusinessTypeEnum;
import com.noqapp.domain.types.DeviceTypeEnum;
import com.noqapp.domain.types.InvocationByEnum;
import com.noqapp.domain.types.MessageOriginEnum;
import com.noqapp.domain.types.QueueStatusEnum;
import com.noqapp.domain.types.UserLevelEnum;
import com.noqapp.repository.BusinessUserStoreManager;
import com.noqapp.repository.NotificationMessageManager;
import com.noqapp.repository.QueueManager;
import com.noqapp.repository.TokenQueueManager;
import com.noqapp.repository.UserProfileManager;
import com.noqapp.search.elastic.domain.BizStoreElastic;
import com.noqapp.search.elastic.domain.BizStoreElasticList;
import com.noqapp.search.elastic.helper.DomainConversion;
import com.noqapp.service.BizService;
import com.noqapp.service.NotifyMobileService;
import com.noqapp.service.ProfessionalProfileService;
import com.noqapp.service.QueueService;
import com.noqapp.service.StoreHourService;
import com.noqapp.service.TokenQueueService;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * User: hitender
 * Date: 11/17/16 3:10 PM
 */
@Service
public class TokenQueueMobileService {
    private static final Logger LOG = LoggerFactory.getLogger(TokenQueueMobileService.class);

    private TokenQueueService tokenQueueService;
    private BizService bizService;
    private TokenQueueManager tokenQueueManager;
    private QueueManager queueManager;
    private ProfessionalProfileService professionalProfileService;
    private UserProfileManager userProfileManager;
    private BusinessUserStoreManager businessUserStoreManager;
    private NotificationMessageManager notificationMessageManager;
    private StoreHourService storeHourService;
    private QueueService queueService;

    @Autowired
    public TokenQueueMobileService(
        TokenQueueService tokenQueueService,
        BizService bizService,
        TokenQueueManager tokenQueueManager,
        QueueManager queueManager,
        ProfessionalProfileService professionalProfileService,
        UserProfileManager userProfileManager,
        BusinessUserStoreManager businessUserStoreManager,
        NotificationMessageManager notificationMessageManager,
        StoreHourService storeHourService,
        QueueService queueService
    ) {
        this.tokenQueueService = tokenQueueService;
        this.bizService = bizService;
        this.tokenQueueManager = tokenQueueManager;
        this.queueManager = queueManager;
        this.professionalProfileService = professionalProfileService;
        this.userProfileManager = userProfileManager;
        this.businessUserStoreManager = businessUserStoreManager;
        this.notificationMessageManager = notificationMessageManager;
        this.storeHourService = storeHourService;
        this.queueService = queueService;
    }

    public JsonQueueList findAllTokenState(String codeQR) {
        try {
            BizStoreEntity bizStoreForCodeQR = bizService.findByCodeQR(codeQR);
            Map<String, String> bizCategories = CommonHelper.getCategories(bizStoreForCodeQR.getBizName().getBusinessType(), InvocationByEnum.STORE);
            JsonQueueList jsonQueues = new JsonQueueList();

            if (null != bizCategories) {
                for (String bizCategoryId : bizCategories.keySet()) {
                    JsonCategory jsonCategory = new JsonCategory()
                        .setBizCategoryId(bizCategoryId)
                        .setCategoryName(bizCategories.get(bizCategoryId))
                        .setDisplayImage("");
                    jsonQueues.addCategories(jsonCategory);
                }
            }

            List<BizStoreEntity> stores = bizService.getAllBizStores(bizStoreForCodeQR.getBizName().getId());
            for (BizStoreEntity bizStore : stores) {
                StoreHourEntity storeHour = storeHourService.getStoreHours(bizStore.getCodeQR(), bizStore);
                TokenQueueEntity tokenQueue = findByCodeQR(bizStore.getCodeQR());
                jsonQueues.addQueues(queueService.getJsonQueue(bizStore, storeHour, tokenQueue));
            }

            return jsonQueues;
        } catch (Exception e) {
            //TODO remove this catch
            LOG.error("Failed getting state codeQR={} reason={}", codeQR, e.getLocalizedMessage(), e);
            return null;
        }
    }

    /**
     * It populates all the stores with BizName amenities and facilities.
     * Note: Store level facilities and amenities are ignored. When business is Hospital/Doctor, then it gets
     * BizName amenities and facilities.
     *
     * @param codeQR
     * @return
     */
    public BizStoreElasticList findAllBizStoreByBizNameCodeQR(String codeQR) {
        try {
            BizNameEntity bizName = bizService.findBizNameByCodeQR(codeQR);
            if (null == bizName) {
                BizStoreEntity bizStoreForCodeQR = bizService.findByCodeQR(codeQR);
                bizName = bizStoreForCodeQR.getBizName();
            }
            BizStoreElasticList bizStoreElasticList = new BizStoreElasticList().setCityName(bizName.getArea());
            Map<String, String> bizCategories = CommonHelper.getCategories(bizName.getBusinessType(), InvocationByEnum.BUSINESS);
            for (String bizCategoryId : bizCategories.keySet()) {
                JsonCategory jsonCategory = new JsonCategory()
                    .setBizCategoryId(bizCategoryId)
                    .setCategoryName(bizCategories.get(bizCategoryId));
                bizStoreElasticList.addJsonCategory(jsonCategory);
            }

            List<BizStoreEntity> stores = bizService.getAllBizStores(bizName.getId());
            for (BizStoreEntity bizStore : stores) {
                BizStoreElastic bizStoreElastic = BizStoreElastic.getThisFromBizStore(bizStore);
                List<StoreHourEntity> storeHours = storeHourService.findAllStoreHours(bizStore.getId());
                if (bizName.isDayClosed()) {
                    bizStoreElastic.setStoreHourElasticList(DomainConversion.getStoreHourElasticsWithClosedAsDefault(storeHours));
                } else {
                    bizStoreElastic.setStoreHourElasticList(DomainConversion.getStoreHourElastics(storeHours));
                }

                if (StringUtils.isNotBlank(bizStore.getBizCategoryId())) {
                    bizStoreElastic.setBizCategoryName(bizCategories.get(bizStore.getBizCategoryId()));
                } else {
                    LOG.warn("No Category defined for bizStore name={} id={}", bizStore.getBizName(), bizStore.getId());
                }

                switch (bizName.getBusinessType()) {
                    case DO:
                        bizStoreElastic.setAmenities(bizName.getAmenities());
                        bizStoreElastic.setFacilities(bizName.getFacilities());

                        List<BusinessUserStoreEntity> businessUsers = businessUserStoreManager.findAllManagingStoreWithUserLevel(
                            bizStore.getId(),
                            UserLevelEnum.S_MANAGER);

                        if (!businessUsers.isEmpty()) {
                            BusinessUserStoreEntity businessUserStore = businessUsers.get(0);
                            ProfessionalProfileEntity professionalProfile = professionalProfileService.findByQid(businessUserStore.getQueueUserId());
                            bizStoreElastic
                                .setWebProfileId(professionalProfile.getWebProfileId())
                                .setEducation(professionalProfile.getEducationAsJson());

                            /*
                             * Since all bizStore are clubbed for this business type,
                             * we are putting profile image in bizStore instead of display image of store.
                             * Client needs to show first one from service image as banner image and not bizStore
                             * display image.
                             */
                            UserProfileEntity userProfile = userProfileManager.findByQueueUserId(businessUserStore.getQueueUserId());
                            bizStoreElastic.setDisplayImage(userProfile.getProfileImage());
                        }
                        break;
                    default:
                        break;
                }
                bizStoreElasticList.addBizStoreElastic(bizStoreElastic);
            }

            LOG.debug("{}", bizStoreElasticList);
            return bizStoreElasticList;
        } catch (Exception e) {
            //TODO remove this catch
            LOG.error("Failed getting bizName for codeQR={} reason={}", codeQR, e.getLocalizedMessage(), e);
            return null;
        }
    }

    /**
     * It populates all the stores for business type bank.
     * Note: Store level facilities and amenities are NOT ignored, when business is Bank.
     *
     * @param matchedStore
     * @return
     */
    public BizStoreElasticList findAllBizStoreByAddress(BizStoreEntity matchedStore) {
        try {
            BizStoreElasticList bizStoreElasticList = new BizStoreElasticList().setCityName(matchedStore.getArea());
            Map<String, String> bizCategories = CommonHelper.getCategories(matchedStore.getBizName().getBusinessType(), InvocationByEnum.STORE);
            for (String bizCategoryId : bizCategories.keySet()) {
                JsonCategory jsonCategory = new JsonCategory()
                    .setBizCategoryId(bizCategoryId)
                    .setCategoryName(bizCategories.get(bizCategoryId));
                bizStoreElasticList.addJsonCategory(jsonCategory);
            }

            List<BizStoreEntity> stores = bizService.getAllBizStoresMatchingAddress(matchedStore.getAddress(), matchedStore.getBizName().getId());
            for (BizStoreEntity bizStore : stores) {
                BizStoreElastic bizStoreElastic = BizStoreElastic.getThisFromBizStore(bizStore);
                if (matchedStore.getBizName().isDayClosed()) {
                    bizStoreElastic.setStoreHourElasticList(
                        DomainConversion.getStoreHourElasticsWithClosedAsDefault(storeHourService.findAllStoreHours(bizStore.getId())));
                } else {
                    bizStoreElastic.setStoreHourElasticList(
                        DomainConversion.getStoreHourElastics(storeHourService.findAllStoreHours(bizStore.getId())));
                }

                if (StringUtils.isNotBlank(bizStore.getBizCategoryId())) {
                    bizStoreElastic.setBizCategoryName(bizCategories.get(bizStore.getBizCategoryId()));
                } else {
                    LOG.warn("No Category defined for bizStore name=\"{}\" id={}", bizStore.getBizName(), bizStore.getId());
                }

                switch (bizStore.getBusinessType()) {
                    case BK:
                        String bannerImage = bizStore.getStoreServiceImages().isEmpty() ? null : bizStore.getStoreServiceImages().iterator().next();
                        if (StringUtils.isBlank(bannerImage)) {
                            bannerImage = bizStore.getBizName().getBusinessServiceImages().isEmpty() ? null : bizStore.getBizName().getBusinessServiceImages().iterator().next();
                        }

                        bizStoreElastic.setDisplayImage(bannerImage);
                        bizStoreElastic.setBizServiceImages(bizStore.getStoreServiceImages());
                        bizStoreElastic.setAmenities(bizStore.getAmenities());
                        bizStoreElastic.setFacilities(bizStore.getFacilities());
                        break;
                    default:
                        break;
                }
                bizStoreElasticList.addBizStoreElastic(bizStoreElastic);
            }

            LOG.debug("{}", bizStoreElasticList);
            return bizStoreElasticList;
        } catch (Exception e) {
            //TODO remove this catch
            LOG.error("Failed populating bizStoreElastic for store codeQR={} reason={}",
                matchedStore.getCodeQR(),
                e.getLocalizedMessage(),
                e);

            return null;
        }
    }

    public BizService getBizService() {
        return bizService;
    }

    TokenQueueEntity findByCodeQR(String codeQR) {
        return tokenQueueService.findByCodeQR(codeQR);
    }

    public boolean isValidCodeQR(String codeQR) {
        return bizService.isValidCodeQR(codeQR);
    }

    public long notifyAllInQueueWhenStoreClosesForTheDay(String qid, String codeQR, String serverDeviceId) {
        TokenQueueEntity tokenQueue = tokenQueueManager.findByCodeQR(codeQR);

        String title = tokenQueue.getDisplayName();
        String body = "Is Closed Today. We are informing you to not visit today. Sorry for inconvenience.";

        NotificationMessageEntity notificationMessage = new NotificationMessageEntity()
            .setTitle(title)
            .setBody(body)
            .setTopic(tokenQueue.getCorrectTopic(QueueStatusEnum.C) + UNDER_SCORE + DeviceTypeEnum.onlyForLogging())
            .setQueueUserId(qid)
            .setMessageSendCount(tokenQueue.getLastNumber());
        notificationMessageManager.save(notificationMessage);

        /* Using queue state QueueStatusEnum.C so that message goes to Client and Merchant. This setting if for broadcast. */
        tokenQueueService.sendAlertMessageToAllOnSpecificTopic(notificationMessage.getId(), title, body, tokenQueue, QueueStatusEnum.C);

        /* Mark all of the people in queue as aborted. */
        return queueManager.markAllAbortWhenQueueClosed(codeQR, serverDeviceId);
    }

    public void notifyAllInQueueAboutDelay(String qid, String codeQR, int delayInMinutes) {
        TokenQueueEntity tokenQueue = tokenQueueManager.findByCodeQR(codeQR);
        String delayed;
        if (delayInMinutes > 59) {
            Duration duration = Duration.ofMinutes(delayInMinutes);
            if (duration.toHours() > 1) {
                delayed = duration.toHours() + " hours and " + (duration.toMinutes() - duration.toHours() * 60) + " minutes";
            } else {
                delayed = duration.toHours() + " hour and " + (duration.toMinutes() - duration.toHours() * 60) + " minutes";
            }
        } else {
            delayed = delayInMinutes + " minutes";
        }

        String title = tokenQueue.getDisplayName();
        String body = "Delayed by " + delayed + ". Sorry for inconvenience.";

        NotificationMessageEntity notificationMessage = new NotificationMessageEntity()
            .setTitle(title)
            .setBody(body)
            .setTopic(tokenQueue.getCorrectTopic(QueueStatusEnum.C) + UNDER_SCORE + DeviceTypeEnum.onlyForLogging())
            .setQueueUserId(qid)
            .setMessageSendCount(tokenQueue.getLastNumber());
        notificationMessageManager.save(notificationMessage);

        /* Using queue state QueueStatusEnum.C so that message goes to Client and Merchant. This setting if for broadcast. */
        tokenQueueService.sendAlertMessageToAllOnSpecificTopic(notificationMessage.getId(), title, body, tokenQueue, QueueStatusEnum.C);
    }

    @Async
    public void sendMessageToSpecificUser(String title, String body, String qid, MessageOriginEnum messageOrigin, BusinessTypeEnum businessType) {
        tokenQueueService.sendMessageToSpecificUser(title, body, qid, messageOrigin, businessType);
    }
}
