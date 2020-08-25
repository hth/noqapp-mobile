package com.noqapp.mobile.service;

import com.noqapp.common.utils.FileUtil;
import com.noqapp.domain.BizNameEntity;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.BusinessUserStoreEntity;
import com.noqapp.domain.ProfessionalProfileEntity;
import com.noqapp.domain.QueueEntity;
import com.noqapp.domain.StoreHourEntity;
import com.noqapp.domain.TokenQueueEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.helper.CommonHelper;
import com.noqapp.domain.json.JsonCategory;
import com.noqapp.domain.json.JsonQueue;
import com.noqapp.domain.json.JsonQueueList;
import com.noqapp.domain.types.InvocationByEnum;
import com.noqapp.domain.types.MessageOriginEnum;
import com.noqapp.domain.types.QueueStatusEnum;
import com.noqapp.domain.types.UserLevelEnum;
import com.noqapp.mobile.service.exception.StoreNoLongerExistsException;
import com.noqapp.repository.BusinessUserStoreManager;
import com.noqapp.repository.QueueManager;
import com.noqapp.repository.TokenQueueManager;
import com.noqapp.repository.UserProfileManager;
import com.noqapp.search.elastic.domain.BizStoreElastic;
import com.noqapp.search.elastic.domain.BizStoreElasticList;
import com.noqapp.search.elastic.helper.DomainConversion;
import com.noqapp.service.BizService;
import com.noqapp.service.ProfessionalProfileService;
import com.noqapp.service.TokenQueueService;
import com.noqapp.service.exceptions.ExpectedServiceBeyondStoreClosingHour;
import com.noqapp.service.utils.ServiceUtils;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

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

    @Autowired
    public TokenQueueMobileService(
        TokenQueueService tokenQueueService,
        BizService bizService,
        TokenQueueManager tokenQueueManager,
        QueueManager queueManager,
        ProfessionalProfileService professionalProfileService,
        UserProfileManager userProfileManager,
        BusinessUserStoreManager businessUserStoreManager
    ) {
        this.tokenQueueService = tokenQueueService;
        this.bizService = bizService;
        this.tokenQueueManager = tokenQueueManager;
        this.queueManager = queueManager;
        this.professionalProfileService = professionalProfileService;
        this.userProfileManager = userProfileManager;
        this.businessUserStoreManager = businessUserStoreManager;
    }

    public JsonQueue findTokenState(String codeQR) {
        try {
            BizStoreEntity bizStore = bizService.findByCodeQR(codeQR);
            if (bizStore.isDeleted()) {
                LOG.info("Store has been deleted id={} displayName=\"{}\"", bizStore.getId(), bizStore.getDisplayName());
                throw new StoreNoLongerExistsException("Store no longer exists");
            }

            StoreHourEntity storeHour = bizService.getStoreHours(codeQR, bizStore);
            TokenQueueEntity tokenQueue = findByCodeQR(codeQR);
            LOG.info("TokenState bizStore=\"{}\" businessType={} averageServiceTime={} tokenQueue={}",
                bizStore.getBizName().getBusinessName(),
                bizStore.getBusinessType().name(),
                bizStore.getAverageServiceTime(),
                tokenQueue.getCurrentlyServing());

            return getJsonQueue(bizStore, storeHour, tokenQueue);
        } catch (StoreNoLongerExistsException e) {
            throw e;
        } catch (Exception e) {
            //TODO remove this catch
            LOG.error("Failed getting state codeQR={} reason={}", codeQR, e.getLocalizedMessage(), e);
            return null;
        }
    }

    /**
     * Store Service Image and Store Interior Image are as is. Its not being appended with code QR like
     * for BizStoreElastic.
     *
     * @param bizStore
     * @param storeHour
     * @param tokenQueue
     * @return
     */
    private JsonQueue getJsonQueue(BizStoreEntity bizStore, StoreHourEntity storeHour, TokenQueueEntity tokenQueue) {
        JsonQueue jsonQueue = new JsonQueue(bizStore.getId(), bizStore.getCodeQR())
            .setBusinessName(bizStore.getBizName().getBusinessName())
            .setDisplayName(bizStore.getDisplayName())
            .setBusinessType(bizStore.getBusinessType())
            .setStoreAddress(bizStore.getAddress())
            .setArea(bizStore.getArea())
            .setTown(bizStore.getTown())
            .setCountryShortName(bizStore.getCountryShortName())
            .setStorePhone(bizStore.getPhoneFormatted())
            .setRating(bizStore.getRating())
            .setReviewCount(bizStore.getReviewCount())
            .setAverageServiceTime(bizStore.getAverageServiceTime())
            .setLimitServiceByDays(bizStore.getBizName().getLimitServiceByDays())
            .setPriorityAccess(bizStore.getBizName().getPriorityAccess())
            .setTokenAvailableFrom(storeHour.getTokenAvailableFrom())
            .setStartHour(storeHour.getStartHour())
            .setTokenNotAvailableFrom(storeHour.getTokenNotAvailableFrom())
            .setEndHour(storeHour.getEndHour())
            .setLunchTimeStart(storeHour.getLunchTimeStart())
            .setLunchTimeEnd(storeHour.getLunchTimeEnd())
            .setDelayedInMinutes(storeHour.getDelayedInMinutes())
            .setPreventJoining(storeHour.isPreventJoining())
            .setDayClosed(bizStore.getBizName().isDayClosed() || storeHour.isDayClosed() || storeHour.isTempDayClosed())
            .setTopic(bizStore.getTopic())
            .setGeoHash(bizStore.getGeoPoint().getGeohash())
            .setServingNumber(tokenQueue.getCurrentlyServing())
            .setLastNumber(tokenQueue.getLastNumber())
            .setQueueStatus(tokenQueue.getQueueStatus())
            .setCreated(tokenQueue.getCreated())
            .setRemoteJoinAvailable(bizStore.isRemoteJoin())
            .setAllowLoggedInUser(bizStore.isAllowLoggedInUser())
            .setAvailableTokenCount(bizStore.getAvailableTokenCount())
            .setEnabledPayment(bizStore.isEnabledPayment())
            .setProductPrice(bizStore.getProductPrice())
            .setCancellationPrice(bizStore.getCancellationPrice())
            .setBizCategoryId(bizStore.getBizCategoryId())
            .setFamousFor(bizStore.getFamousFor())
            .setDiscount(bizStore.getDiscount())
            .setMinimumDeliveryOrder(bizStore.getMinimumDeliveryOrder())
            .setDeliveryRange(bizStore.getDeliveryRange())
            .setStoreServiceImages(bizStore.getStoreServiceImages())
            .setStoreInteriorImages(bizStore.getStoreInteriorImages())
            .setAmenities(bizStore.getAmenities())
            .setFacilities(bizStore.getFacilities());

        String timeSlotMessage;
        switch (bizStore.getBusinessType()) {
            case CD:
            case CDQ:
                jsonQueue.setStoreAddress(FileUtil.DASH);
                jsonQueue.setArea(FileUtil.DASH);
                jsonQueue.setTown(FileUtil.DASH);

                try {
                    ZonedDateTime zonedDateTime = tokenQueueService.computeExpectedServiceBeginTime(
                        bizStore.getAverageServiceTime(),
                        TimeZone.getTimeZone(bizStore.getTimeZone()).toZoneId(),
                        storeHour,
                        tokenQueue.getLastNumber());

                    timeSlotMessage = ServiceUtils.timeSlot(
                        zonedDateTime,
                        bizStore.getTimeZone(),
                        storeHour);

                    LOG.info("ZonedDateTime {} timeSlotMessage={}", zonedDateTime, timeSlotMessage);
                } catch (ExpectedServiceBeyondStoreClosingHour e) {
                    timeSlotMessage = "Closed now";
                }
                break;
            default:
                timeSlotMessage = ServiceUtils.calculateEstimatedWaitTime(
                    bizStore.getAverageServiceTime(),
                    tokenQueue.getLastNumber() - tokenQueue.getCurrentlyServing(),
                    tokenQueue.getQueueStatus(),
                    storeHour.getStartHour(),
                    bizStore.getTimeZone()
                );
                LOG.info("timeSlotMessage={}", timeSlotMessage);
        }
        jsonQueue.setTimeSlotMessage(timeSlotMessage == null ? "Not Available" : timeSlotMessage);
        return jsonQueue;
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
                StoreHourEntity storeHour = bizService.getStoreHours(bizStore.getCodeQR(), bizStore);
                TokenQueueEntity tokenQueue = findByCodeQR(bizStore.getCodeQR());
                jsonQueues.addQueues(getJsonQueue(bizStore, storeHour, tokenQueue));
            }

            return jsonQueues;
        } catch (Exception e) {
            //TODO remove this catch
            LOG.error("Failed getting state codeQR={} reason={}", codeQR, e.getLocalizedMessage(), e);
            return null;
        }
    }

    /**
     * //TODO(hth) add GPS co-ordinate to this query for limiting data.
     * Refer findAllBizStoreByBizNameCodeQR as simplified for using BizStoreElastic.
     *
     * @param codeQR
     * @return
     * @deprecated
     */
    public JsonQueueList findAllQueuesByBizNameCodeQR(String codeQR) {
        try {
            BizNameEntity bizName = bizService.findBizNameByCodeQR(codeQR);
            if (null == bizName) {
                BizStoreEntity bizStoreForCodeQR = bizService.findByCodeQR(codeQR);
                bizName = bizStoreForCodeQR.getBizName();
            }
            Map<String, String> bizCategories = CommonHelper.getCategories(bizName.getBusinessType(), InvocationByEnum.BUSINESS);
            JsonQueueList jsonQueues = new JsonQueueList();
            for (String bizCategoryId : bizCategories.keySet()) {
                JsonCategory jsonCategory = new JsonCategory()
                    .setBizCategoryId(bizCategoryId)
                    .setCategoryName(bizCategories.get(bizCategoryId))
                    .setDisplayImage("");
                jsonQueues.addCategories(jsonCategory);
            }

            List<BizStoreEntity> stores = bizService.getAllBizStores(bizName.getId());
            for (BizStoreEntity bizStore : stores) {
                StoreHourEntity storeHour = bizService.getStoreHours(bizStore.getCodeQR(), bizStore);
                TokenQueueEntity tokenQueue = findByCodeQR(bizStore.getCodeQR());
                jsonQueues.addQueues(getJsonQueue(bizStore, storeHour, tokenQueue));
            }

            return jsonQueues;
        } catch (Exception e) {
            //TODO remove this catch
            LOG.error("Failed getting bizName for codeQR={} reason={}", codeQR, e.getLocalizedMessage(), e);
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
                List<StoreHourEntity> storeHours = bizService.findAllStoreHours(bizStore.getId());
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
                        DomainConversion.getStoreHourElasticsWithClosedAsDefault(bizService.findAllStoreHours(bizStore.getId())));
                } else {
                    bizStoreElastic.setStoreHourElasticList(
                        DomainConversion.getStoreHourElastics(bizService.findAllStoreHours(bizStore.getId())));
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

    public long notifyAllInQueueWhenStoreClosesForTheDay(String codeQR, String serverDeviceId) {
        TokenQueueEntity tokenQueue = tokenQueueManager.findByCodeQR(codeQR);
        tokenQueueService.sendAlertMessageToAllOnSpecificTopic(
            tokenQueue.getDisplayName(),
            "Is Closed Today. We are informing you to not visit today. Sorry for inconvenience.",
            tokenQueue,
            QueueStatusEnum.C);

        /* Mark all of the people in queue as aborted. */
        return queueManager.markAllAbortWhenQueueClosed(codeQR, serverDeviceId);
    }

    public void notifyAllInQueueAboutDelay(String codeQR, int delayInMinutes) {
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

        tokenQueueService.sendAlertMessageToAllOnSpecificTopic(
            tokenQueue.getDisplayName(),
            "Delayed by " + delayed + ". Sorry for inconvenience.",
            tokenQueue,
            /* Using queue state C so that message goes to Client and Merchant. This setting if for broadcast. */
            QueueStatusEnum.C);
    }

    @Async
    public void sendMessageToSpecificUser(String title, String body, String qid, MessageOriginEnum messageOrigin) {
        tokenQueueService.sendMessageToSpecificUser(title, body, qid, messageOrigin);
    }
}
