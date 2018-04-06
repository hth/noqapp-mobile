package com.noqapp.mobile.service;

import com.noqapp.domain.BizCategoryEntity;
import com.noqapp.domain.BizNameEntity;
import com.noqapp.domain.json.JsonCategory;
import com.noqapp.domain.json.JsonQueueList;
import com.noqapp.domain.types.TokenServiceEnum;
import com.noqapp.repository.QueueManager;
import com.noqapp.search.elastic.domain.BizStoreElastic;
import com.noqapp.search.elastic.domain.BizStoreElasticList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.StoreHourEntity;
import com.noqapp.domain.TokenQueueEntity;
import com.noqapp.domain.json.JsonQueue;
import com.noqapp.domain.json.JsonResponse;
import com.noqapp.domain.json.JsonToken;
import com.noqapp.domain.types.QueueStatusEnum;
import com.noqapp.repository.TokenQueueManager;
import com.noqapp.service.BizService;
import com.noqapp.service.TokenQueueService;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
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

    @Autowired
    public TokenQueueMobileService(
            TokenQueueService tokenQueueService,
            BizService bizService,
            TokenQueueManager tokenQueueManager,
            QueueManager queueManager
    ) {
        this.tokenQueueService = tokenQueueService;
        this.bizService = bizService;
        this.tokenQueueManager = tokenQueueManager;
        this.queueManager = queueManager;
    }

    public JsonQueue findTokenState(String codeQR) {
        try {
            BizStoreEntity bizStore = bizService.findByCodeQR(codeQR);
            StoreHourEntity storeHour = getStoreHours(codeQR, bizStore);
            TokenQueueEntity tokenQueue = findByCodeQR(codeQR);
            LOG.info("TokenState bizStore={} averageServiceTime={} tokenQueue={}",
                    bizStore.getBizName(),
                    bizStore.getAverageServiceTime(),
                    tokenQueue.getCurrentlyServing());

            return getJsonQueue(bizStore, storeHour, tokenQueue);
        } catch (Exception e) {
            //TODO remove this catch
            LOG.error("Failed getting state codeQR={} reason={}", codeQR, e.getLocalizedMessage(), e);
            return null;
        }
    }

    private JsonQueue getJsonQueue(BizStoreEntity bizStore, StoreHourEntity storeHour, TokenQueueEntity tokenQueue) {
        return new JsonQueue(bizStore.getId(), bizStore.getCodeQR())
                .setBusinessName(bizStore.getBizName().getBusinessName())
                .setDisplayName(bizStore.getDisplayName())
                .setBusinessType(bizStore.getBusinessType())
                .setStoreAddress(bizStore.getAddress())
                .setCountryShortName(bizStore.getCountryShortName())
                .setStorePhone(bizStore.getPhoneFormatted())
                .setRating(bizStore.getRating())
                .setRatingCount(bizStore.getRatingCount())
                .setAverageServiceTime(bizStore.getAverageServiceTime())
                .setTokenAvailableFrom(storeHour.getTokenAvailableFrom())
                .setStartHour(storeHour.getStartHour())
                .setTokenNotAvailableFrom(storeHour.getTokenNotAvailableFrom())
                .setEndHour(storeHour.getEndHour())
                .setDelayedInMinutes(storeHour.getDelayedInMinutes())
                .setPreventJoining(storeHour.isPreventJoining())
                .setDayClosed(storeHour.isDayClosed())
                .setTopic(bizStore.getTopic())
                .setCoordinate(bizStore.getCoordinate())
                .setServingNumber(tokenQueue.getCurrentlyServing())
                .setLastNumber(tokenQueue.getLastNumber())
                .setQueueStatus(tokenQueue.getQueueStatus())
                .setCreated(tokenQueue.getCreated())
                .setRemoteJoinAvailable(bizStore.isRemoteJoin())
                .setAllowLoggedInUser(bizStore.isAllowLoggedInUser())
                .setAvailableTokenCount(bizStore.getAvailableTokenCount())
                .setBizCategoryId(bizStore.getBizCategoryId());
    }

    public JsonQueueList findAllTokenState(String codeQR) {
        try {
            BizStoreEntity bizStoreForCodeQR = bizService.findByCodeQR(codeQR);
            List<BizCategoryEntity> bizCategories = bizService.getBusinessCategories(bizStoreForCodeQR.getBizName().getId());
            JsonQueueList jsonQueues = new JsonQueueList();
            for (BizCategoryEntity bizCategory : bizCategories) {
                JsonCategory jsonCategory = new JsonCategory()
                        .setBizCategoryId(bizCategory.getId())
                        .setCategoryName(bizCategory.getCategoryName());
                jsonQueues.addCategories(jsonCategory);
            }

            List<BizStoreEntity> stores = bizService.getAllBizStores(bizStoreForCodeQR.getBizName().getId());
            for (BizStoreEntity bizStore : stores) {
                StoreHourEntity storeHour = getStoreHours(bizStore.getCodeQR(), bizStore);
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
            List<BizCategoryEntity> bizCategories = bizService.getBusinessCategories(bizName.getId());
            JsonQueueList jsonQueues = new JsonQueueList();
            for (BizCategoryEntity bizCategory : bizCategories) {
                JsonCategory jsonCategory = new JsonCategory()
                        .setBizCategoryId(bizCategory.getId())
                        .setCategoryName(bizCategory.getCategoryName());
                jsonQueues.addCategories(jsonCategory);
            }

            List<BizStoreEntity> stores = bizService.getAllBizStores(bizName.getId());
            for (BizStoreEntity bizStore : stores) {
                StoreHourEntity storeHour = getStoreHours(bizStore.getCodeQR(), bizStore);
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
            List<BizCategoryEntity> bizCategories = bizService.getBusinessCategories(bizName.getId());
            BizStoreElasticList bizStoreElasticList = new BizStoreElasticList();
            for (BizCategoryEntity bizCategory : bizCategories) {
                JsonCategory jsonCategory = new JsonCategory()
                        .setBizCategoryId(bizCategory.getId())
                        .setCategoryName(bizCategory.getCategoryName());
                bizStoreElasticList.addJsonCategory(jsonCategory);
            }

            List<BizStoreEntity> stores = bizService.getAllBizStores(bizName.getId());
            for (BizStoreEntity bizStore : stores) {
                BizStoreElastic bizStoreElastic = BizStoreElastic.getThisFromBizStore(bizStore);
                bizStoreElasticList.addBizStoreElastic(bizStoreElastic);

            }

            return bizStoreElasticList;
        } catch (Exception e) {
            //TODO remove this catch
            LOG.error("Failed getting bizName for codeQR={} reason={}", codeQR, e.getLocalizedMessage(), e);
            return null;
        }
    }

    //TODO instead send all the hours of the store and let App figure out which one to show.
    private StoreHourEntity getStoreHours(String codeQR, BizStoreEntity bizStore) {
        DayOfWeek dayOfWeek = ZonedDateTime.now(TimeZone.getTimeZone(bizStore.getTimeZone()).toZoneId()).getDayOfWeek();
        LOG.debug("codeQR={} dayOfWeek={}", codeQR, dayOfWeek);

        StoreHourEntity storeHour = bizService.findStoreHour(bizStore.getId(), dayOfWeek);
        LOG.debug("StoreHour={}", storeHour);
        return storeHour;
    }

    public JsonToken joinQueue(String codeQR, String did, String qid, long averageServiceTime, TokenServiceEnum tokenService) {
        LOG.info("joinQueue codeQR={} did={} rid={}", codeQR, did, qid);
        return tokenQueueService.getNextToken(codeQR, did, qid, averageServiceTime, tokenService);
    }

    public JsonResponse abortQueue(String codeQR, String did, String qid) {
        LOG.info("abortQueue codeQR={} did={} rid={}", codeQR, did, qid);
        return tokenQueueService.abortQueue(codeQR, did, qid);
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
        tokenQueueService.sendMessageToAllOnSpecificTopic(
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

        tokenQueueService.sendMessageToAllOnSpecificTopic(
                tokenQueue.getDisplayName(),
                "Delayed by " + delayed +  ". Sorry for inconvenience.",
                tokenQueue,
                /* Using queue state C so that message goes to Client and Merchant. This setting if for broadcast. */
                QueueStatusEnum.C);
    }
}
