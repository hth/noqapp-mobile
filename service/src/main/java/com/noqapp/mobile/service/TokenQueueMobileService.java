package com.noqapp.mobile.service;

import com.noqapp.domain.BizNameEntity;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.BusinessUserStoreEntity;
import com.noqapp.domain.ProfessionalProfileEntity;
import com.noqapp.domain.PurchaseOrderEntity;
import com.noqapp.domain.QueueEntity;
import com.noqapp.domain.StoreHourEntity;
import com.noqapp.domain.TokenQueueEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.helper.CommonHelper;
import com.noqapp.domain.json.JsonCategory;
import com.noqapp.domain.json.JsonPurchaseOrder;
import com.noqapp.domain.json.JsonPurchaseOrderProduct;
import com.noqapp.domain.json.JsonQueue;
import com.noqapp.domain.json.JsonQueueList;
import com.noqapp.domain.json.JsonResponse;
import com.noqapp.domain.json.JsonToken;
import com.noqapp.domain.json.payment.cashfree.JsonResponseWithCFToken;
import com.noqapp.domain.types.DeliveryModeEnum;
import com.noqapp.domain.types.InvocationByEnum;
import com.noqapp.domain.types.QueueStatusEnum;
import com.noqapp.domain.types.SkipPaymentGatewayEnum;
import com.noqapp.domain.types.TokenServiceEnum;
import com.noqapp.domain.types.UserLevelEnum;
import com.noqapp.mobile.service.exception.StoreNoLongerExistsException;
import com.noqapp.repository.BusinessUserStoreManager;
import com.noqapp.repository.QueueManager;
import com.noqapp.repository.QueueManagerJDBC;
import com.noqapp.repository.TokenQueueManager;
import com.noqapp.repository.UserProfileManager;
import com.noqapp.search.elastic.domain.BizStoreElastic;
import com.noqapp.search.elastic.domain.BizStoreElasticList;
import com.noqapp.search.elastic.helper.DomainConversion;
import com.noqapp.service.BizService;
import com.noqapp.service.ProfessionalProfileService;
import com.noqapp.service.PurchaseOrderProductService;
import com.noqapp.service.PurchaseOrderService;
import com.noqapp.service.TokenQueueService;
import com.noqapp.service.exceptions.PurchaseOrderCancelException;
import com.noqapp.service.exceptions.PurchaseOrderFailException;
import com.noqapp.service.exceptions.PurchaseOrderRefundExternalException;
import com.noqapp.service.exceptions.PurchaseOrderRefundPartialException;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
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
    private QueueManagerJDBC queueManagerJDBC;
    private ProfessionalProfileService professionalProfileService;
    private UserProfileManager userProfileManager;
    private BusinessUserStoreManager businessUserStoreManager;
    private PurchaseOrderService purchaseOrderService;
    private PurchaseOrderProductService purchaseOrderProductService;

    @Autowired
    public TokenQueueMobileService(
        TokenQueueService tokenQueueService,
        BizService bizService,
        TokenQueueManager tokenQueueManager,
        QueueManager queueManager,
        QueueManagerJDBC queueManagerJDBC,
        ProfessionalProfileService professionalProfileService,
        UserProfileManager userProfileManager,
        BusinessUserStoreManager businessUserStoreManager,
        PurchaseOrderService purchaseOrderService,
        PurchaseOrderProductService purchaseOrderProductService
    ) {
        this.tokenQueueService = tokenQueueService;
        this.bizService = bizService;
        this.tokenQueueManager = tokenQueueManager;
        this.queueManager = queueManager;
        this.queueManagerJDBC = queueManagerJDBC;
        this.professionalProfileService = professionalProfileService;
        this.userProfileManager = userProfileManager;
        this.businessUserStoreManager = businessUserStoreManager;
        this.purchaseOrderService = purchaseOrderService;
        this.purchaseOrderProductService = purchaseOrderProductService;
    }

    public JsonQueue findTokenState(String codeQR) {
        try {
            BizStoreEntity bizStore = bizService.findByCodeQR(codeQR);
            if (bizStore.isDeleted()) {
                LOG.info("Store has been deleted id={} displayName={}", bizStore.getId(), bizStore.getDisplayName());
                throw new StoreNoLongerExistsException("Store no longer exists");
            }

            StoreHourEntity storeHour = getStoreHours(codeQR, bizStore);
            TokenQueueEntity tokenQueue = findByCodeQR(codeQR);
            LOG.info("TokenState bizStore={} businessType={} averageServiceTime={} tokenQueue={}",
                bizStore.getBizName(),
                bizStore.getBusinessType().getDescription(),
                bizStore.getAverageServiceTime(),
                tokenQueue.getCurrentlyServing());

            return getJsonQueue(bizStore, storeHour, tokenQueue);
        } catch(StoreNoLongerExistsException e) {
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
        return new JsonQueue(bizStore.getId(), bizStore.getCodeQR())
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
            .setTokenAvailableFrom(storeHour.getTokenAvailableFrom())
            .setStartHour(storeHour.getStartHour())
            .setTokenNotAvailableFrom(storeHour.getTokenNotAvailableFrom())
            .setEndHour(storeHour.getEndHour())
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
            .setServicePayment(bizStore.getServicePayment())
            .setBizCategoryId(bizStore.getBizCategoryId())
            .setFamousFor(bizStore.getFamousFor())
            .setDiscount(bizStore.getDiscount())
            .setMinimumDeliveryOrder(bizStore.getMinimumDeliveryOrder())
            .setDeliveryRange(bizStore.getDeliveryRange())
            .setStoreServiceImages(bizStore.getStoreServiceImages())
            .setStoreInteriorImages(bizStore.getStoreInteriorImages())
            .setAmenities(bizStore.getAmenities())
            .setFacilities(bizStore.getFacilities());
    }

    public JsonQueueList findAllTokenState(String codeQR) {
        try {
            BizStoreEntity bizStoreForCodeQR = bizService.findByCodeQR(codeQR);
            Map<String, String> bizCategories = CommonHelper.getCategories(bizStoreForCodeQR.getBizName().getBusinessType(), InvocationByEnum.STORE);
            JsonQueueList jsonQueues = new JsonQueueList();
            for (String bizCategoryId : bizCategories.keySet()) {
                JsonCategory jsonCategory = new JsonCategory()
                    .setBizCategoryId(bizCategoryId)
                    .setCategoryName(bizCategories.get(bizCategoryId))
                    .setDisplayImage("");
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
                if (bizName.isDayClosed()) {
                    bizStoreElastic.setStoreHourElasticList(
                        DomainConversion.getStoreHourElasticsWithClosedAsDefault(bizService.findAllStoreHours(bizStore.getId())));
                } else {
                    bizStoreElastic.setStoreHourElasticList(
                        DomainConversion.getStoreHourElastics(bizService.findAllStoreHours(bizStore.getId())));
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
                    LOG.warn("No Category defined for bizStore name={} id={}", bizStore.getBizName(), bizStore.getId());
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

    //TODO instead send all the hours of the store and let App figure out which one to show.
    private StoreHourEntity getStoreHours(String codeQR, BizStoreEntity bizStore) {
        DayOfWeek dayOfWeek = ZonedDateTime.now(TimeZone.getTimeZone(bizStore.getTimeZone()).toZoneId()).getDayOfWeek();
        LOG.debug("codeQR={} dayOfWeek={}", codeQR, dayOfWeek);

        StoreHourEntity storeHour = bizService.findStoreHour(bizStore.getId(), dayOfWeek);
        LOG.debug("StoreHour={}", storeHour);
        return storeHour;
    }

    public JsonToken joinQueue(String codeQR, String did, String qid, String guardianQid, long averageServiceTime, TokenServiceEnum tokenService) {
        LOG.info("joinQueue codeQR={} did={} qid={} guardianQid={}", codeQR, did, qid, guardianQid);
        return tokenQueueService.getNextToken(codeQR, did, qid, guardianQid, averageServiceTime, tokenService);
    }

    /** Invoke by client and hence has a token service as Client. */
    public JsonToken payBeforeJoinQueue(String codeQR, String did, String qid, String guardianQid, BizStoreEntity bizStore, TokenServiceEnum tokenService) {
        String purchaserQid = StringUtils.isBlank(guardianQid) ? qid : guardianQid;

        JsonToken jsonToken = tokenQueueService.getPaidNextToken(codeQR, did, qid, guardianQid, bizStore.getAverageServiceTime(), tokenService);

        JsonPurchaseOrder jsonPurchaseOrder;
        PurchaseOrderEntity purchaseOrder = purchaseOrderService.findByTransactionId(jsonToken.getTransactionId());
        if (null == purchaseOrder) {
            jsonPurchaseOrder = createNewJsonPurchaseOrder(purchaserQid, jsonToken, bizStore);
            LOG.info("joinQueue codeQR={} did={} qid={} guardianQid={}", codeQR, did, qid, guardianQid);
            purchaseOrderService.createOrder(jsonPurchaseOrder, purchaserQid, did, TokenServiceEnum.C);
            queueManager.updateWithTransactionId(codeQR, qid, jsonToken.getToken(), jsonPurchaseOrder.getTransactionId());
        } else {
            LOG.info("Found exists purchaseOrder with transactionId={}", purchaseOrder.getTransactionId());
            jsonPurchaseOrder = new JsonPurchaseOrder(purchaseOrder);
            JsonResponseWithCFToken jsonResponseWithCFToken = new JsonResponseWithCFToken()
                .setSkipPaymentGateway(SkipPaymentGatewayEnum.YES);
            jsonPurchaseOrder.setJsonResponseWithCFToken(jsonResponseWithCFToken);
        }
        jsonToken.setJsonPurchaseOrder(jsonPurchaseOrder);
        return jsonToken;
    }

    /**
     * Invoke by client and hence has a token service as Client.
     * Note: When client skips, the state is VB (Valid before purchase). PO when Paid. After skip, if client make a Paid API request, server
     * sends VB then client is trying to pay when its should skip. Hence send SKIP CFToken.
     */
    public JsonToken skipPayBeforeJoinQueue(String codeQR, String did, String qid, String guardianQid, BizStoreEntity bizStore, TokenServiceEnum tokenService) {
        JsonToken jsonToken = payBeforeJoinQueue(codeQR, did, qid, guardianQid, bizStore, tokenService);

        if (!purchaseOrderService.existsTransactionId(jsonToken.getTransactionId())) {
            JsonToken jsonTokenUpdatedWithPayment = updateWhenPaymentSuccessful(codeQR, jsonToken.getJsonPurchaseOrder().getTransactionId());
            jsonTokenUpdatedWithPayment.setJsonPurchaseOrder(jsonToken.getJsonPurchaseOrder());
            return jsonTokenUpdatedWithPayment;
        }
        return jsonToken;
    }

    public JsonToken updateWhenPaymentSuccessful(String codeQR, String transactionId) {
        JsonToken jsonToken = tokenQueueService.updateJsonToken(codeQR, transactionId);
        purchaseOrderService.updatePurchaseOrderWithToken(jsonToken.getToken(), jsonToken.getExpectedServiceBegin(), transactionId);
        return jsonToken;
    }

    public JsonResponseWithCFToken createTokenForPaymentGateway(String qid, String codeQR, String transactionId) {
        QueueEntity queue = queueManager.findByTransactionId(codeQR, transactionId);
        switch (queue.getQueueUserState()) {
            case I:
            case N:
            case A:
                LOG.error("Trying to make payment on non serviced by qid={} for {} {}", qid, queue.getTransactionId(), queue.getQueueUserId());
                throw new PurchaseOrderFailException("No payment needed when not served");
        }
        String purchaserQid = StringUtils.isBlank(queue.getGuardianQid()) ? queue.getQueueUserId() : queue.getGuardianQid();
        if (!qid.equalsIgnoreCase(purchaserQid)) {
            LOG.error("Something is not right for {} {} {}", qid, codeQR, transactionId);
            return null;
        }

        PurchaseOrderEntity purchaseOrder = purchaseOrderService.findByTransactionId(transactionId);
        if (purchaseOrder.getQueueUserId().equalsIgnoreCase(qid)) {
            return purchaseOrderService.createTokenForPurchaseOrder(purchaseOrder.orderPriceForTransaction(), purchaseOrder.getTransactionId());
        }

        LOG.error("Purchase Order qid mis-match for {} {} {}", qid, codeQR, transactionId);
        return null;
    }

    private JsonPurchaseOrder createNewJsonPurchaseOrder(String purchaserQid, JsonToken jsonToken, BizStoreEntity bizStore) {
        UserProfileEntity userProfile = userProfileManager.findByQueueUserId(purchaserQid);
        JsonPurchaseOrder jsonPurchaseOrder = new JsonPurchaseOrder()
            .setBizStoreId(bizStore.getId())
            .setCodeQR(bizStore.getCodeQR())
            .setBusinessType(bizStore.getBusinessType())
            .setOrderPrice(String.valueOf(bizStore.getProductPrice()))
            .setQueueUserId(purchaserQid)
            .setExpectedServiceBegin(jsonToken.getExpectedServiceBegin())
            .setToken(jsonToken.getToken())
            .setDeliveryMode(DeliveryModeEnum.QS)
            .setDeliveryAddress(userProfile.getAddress());

        jsonPurchaseOrder.addJsonPurchaseOrderProduct(new JsonPurchaseOrderProduct()
            .setProductId(bizStore.getId())
            .setProductPrice(bizStore.getProductPrice())
            .setProductQuantity(1)
            .setProductName(bizStore.getDisplayName()));

        return jsonPurchaseOrder;
    }

    public JsonToken joinQueue(String codeQR, String did, long averageServiceTime, TokenServiceEnum tokenService) {
        return joinQueue(codeQR, did, null, null, averageServiceTime, tokenService);
    }

    public JsonPurchaseOrder findQueueThatHasTransaction(String codeQR, String qid, int token) {
        boolean historical = false;
        QueueEntity queue = queueManager.findQueueThatHasTransaction(codeQR, qid, token);
        if (null == queue) {
            queue = queueManagerJDBC.findQueueThatHasTransaction(codeQR, qid, token);
            historical = true;
        }

        if (queue == null || StringUtils.isBlank(queue.getTransactionId())) {
            return null;
        }

        JsonPurchaseOrder jsonPurchaseOrder;
        if (historical) {
            PurchaseOrderEntity purchaseOrder = purchaseOrderService.findHistoricalPurchaseOrder(qid, queue.getTransactionId());
            jsonPurchaseOrder = purchaseOrderProductService.populateHistoricalJsonPurchaseOrder(purchaseOrder);
        } else {
            PurchaseOrderEntity purchaseOrder = purchaseOrderService.findByTransactionId(queue.getTransactionId());
            jsonPurchaseOrder = purchaseOrderProductService.populateJsonPurchaseOrder(purchaseOrder);
        }

        LOG.debug("Found purchase order for {} {} {}", codeQR, qid, jsonPurchaseOrder);
        return jsonPurchaseOrder;
    }

    public JsonResponse abortQueue(String codeQR, String did, String qid) {
        LOG.info("abortQueue codeQR={} did={} qid={}", codeQR, did, qid);
        QueueEntity queue = queueManager.findToAbort(codeQR, qid);
        if (queue == null) {
            LOG.error("Not joined to queue qid={}, ignore abort", qid);
            return new JsonResponse(false);
        }

        try {
            if (StringUtils.isNotBlank(queue.getTransactionId())) {
                purchaseOrderService.cancelOrderByClient(queue.getGuardianQid() == null ? qid : queue.getGuardianQid(), queue.getTransactionId());
            }
            queueManager.abort(queue.getId());
            /* Irrespective of Queue with order or without order, notify merchant of abort by just sending a refresh notification. */
            tokenQueueService.forceRefreshOnSomeActivity(codeQR);
            return new JsonResponse(true);
        } catch (PurchaseOrderRefundPartialException | PurchaseOrderRefundExternalException | PurchaseOrderCancelException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Abort failed reason={}", e.getLocalizedMessage(), e);
            return new JsonResponse(false);
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

    public void deleteReferenceToTransactionId(String codeQR, String transactionId) {
        queueManager.deleteReferenceToTransactionId(codeQR, transactionId);
        purchaseOrderService.deleteReferenceToTransactionId(transactionId);
    }
}
