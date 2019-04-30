package com.noqapp.mobile.view.controller.api.merchant.store;

import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.common.utils.CommonUtil.UNAUTHORIZED;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.FAILED_PLACING_MEDICAL_ORDER_AS_INCORRECT_BUSINESS;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MERCHANT_COULD_NOT_ACQUIRE;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MOBILE_JSON;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.ORDER_PAYMENT_UPDATE_FAILED;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.PURCHASE_ORDER_ALREADY_CANCELLED;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.PURCHASE_ORDER_FAILED_TO_CANCEL;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.PURCHASE_ORDER_PRICE_MISMATCH;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.PURCHASE_ORDER_PRODUCT_NOT_FOUND;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.QUEUE_NOT_RE_STARTED;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.QUEUE_NOT_STARTED;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.STORE_DAY_CLOSED;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.STORE_OFFLINE;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.STORE_PREVENT_JOIN;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.STORE_TEMP_DAY_CLOSED;
import static com.noqapp.mobile.view.controller.api.client.TokenQueueAPIController.authorizeRequest;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

import com.noqapp.common.utils.ParseJsonStringToMap;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.PurchaseOrderEntity;
import com.noqapp.domain.RegisteredDeviceEntity;
import com.noqapp.domain.TokenQueueEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.json.JsonPurchaseOrder;
import com.noqapp.domain.json.JsonPurchaseOrderList;
import com.noqapp.domain.json.JsonPurchaseOrderProduct;
import com.noqapp.domain.json.JsonResponse;
import com.noqapp.domain.json.JsonToken;
import com.noqapp.domain.types.BusinessTypeEnum;
import com.noqapp.domain.types.DeliveryModeEnum;
import com.noqapp.domain.types.PurchaseOrderStateEnum;
import com.noqapp.domain.types.QueueStatusEnum;
import com.noqapp.domain.types.TokenServiceEnum;
import com.noqapp.domain.types.TransactionViaEnum;
import com.noqapp.domain.types.medical.FormVersionEnum;
import com.noqapp.domain.types.medical.LabCategoryEnum;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.medical.domain.MedicalPathologyEntity;
import com.noqapp.medical.domain.MedicalRadiologyEntity;
import com.noqapp.medical.domain.json.JsonMedicalPathology;
import com.noqapp.medical.domain.json.JsonMedicalPathologyList;
import com.noqapp.medical.domain.json.JsonMedicalRadiology;
import com.noqapp.medical.domain.json.JsonMedicalRadiologyList;
import com.noqapp.medical.domain.json.JsonMedicalRecord;
import com.noqapp.medical.repository.MedicalPathologyManager;
import com.noqapp.medical.repository.MedicalRadiologyManager;
import com.noqapp.medical.service.MedicalRecordService;
import com.noqapp.mobile.common.util.ErrorEncounteredJson;
import com.noqapp.mobile.domain.body.merchant.LabFile;
import com.noqapp.mobile.domain.body.merchant.OrderServed;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.service.DeviceService;
import com.noqapp.mobile.service.QueueMobileService;
import com.noqapp.mobile.view.controller.api.ImageCommonHelper;
import com.noqapp.mobile.view.controller.api.merchant.queue.QueueController;
import com.noqapp.mobile.view.validator.ImageValidator;
import com.noqapp.repository.BizStoreManager;
import com.noqapp.repository.UserProfileManager;
import com.noqapp.service.BusinessUserStoreService;
import com.noqapp.service.ProfessionalProfileService;
import com.noqapp.service.PurchaseOrderProductService;
import com.noqapp.service.PurchaseOrderService;
import com.noqapp.service.TokenQueueService;
import com.noqapp.service.exceptions.PriceMismatchException;
import com.noqapp.service.exceptions.PurchaseOrderProductNFException;
import com.noqapp.service.exceptions.StoreDayClosedException;
import com.noqapp.service.exceptions.StoreInActiveException;
import com.noqapp.service.exceptions.StorePreventJoiningException;
import com.noqapp.service.exceptions.StoreTempDayClosedException;

import com.fasterxml.jackson.databind.JsonMappingException;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.servlet.http.HttpServletResponse;

/**
 * hitender
 * 7/27/18 3:13 PM
 */
@SuppressWarnings ({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/api/m/s/purchaseOrder")
public class PurchaseOrderController {
    private static final Logger LOG = LoggerFactory.getLogger(PurchaseOrderController.class);

    private int counterNameLength;

    private ImageValidator imageValidator;
    private ImageCommonHelper imageCommonHelper;
    private AuthenticateMobileService authenticateMobileService;
    private BusinessUserStoreService businessUserStoreService;
    private PurchaseOrderService purchaseOrderService;
    private QueueMobileService queueMobileService;
    private TokenQueueService tokenQueueService;
    private MedicalRecordService medicalRecordService;
    private DeviceService deviceService;
    private ApiHealthService apiHealthService;

    private UserProfileManager userProfileManager;
    private BizStoreManager bizStoreManager;
    private MedicalRadiologyManager medicalRadiologyManager;
    private MedicalPathologyManager medicalPathologyManager;

    private ExecutorService executorService;

    @Autowired
    public PurchaseOrderController(
        @Value("${ManageQueueController.counterNameLength}")
        int counterNameLength,

        UserProfileManager userProfileManager,
        BizStoreManager bizStoreManager,
        MedicalRadiologyManager medicalRadiologyManager,
        MedicalPathologyManager medicalPathologyManager,
        ImageValidator imageValidator,
        ImageCommonHelper imageCommonHelper,
        AuthenticateMobileService authenticateMobileService,
        BusinessUserStoreService businessUserStoreService,
        PurchaseOrderService purchaseOrderService,
        QueueMobileService queueMobileService,
        TokenQueueService tokenQueueService,
        MedicalRecordService medicalRecordService,
        DeviceService deviceService,
        ProfessionalProfileService professionalProfileService,
        PurchaseOrderProductService purchaseOrderProductService,
        ApiHealthService apiHealthService
    ) {
        this.counterNameLength = counterNameLength;

        this.userProfileManager = userProfileManager;
        this.bizStoreManager = bizStoreManager;
        this.medicalRadiologyManager = medicalRadiologyManager;
        this.medicalPathologyManager = medicalPathologyManager;
        this.imageValidator = imageValidator;
        this.imageCommonHelper = imageCommonHelper;
        this.authenticateMobileService = authenticateMobileService;
        this.businessUserStoreService = businessUserStoreService;
        this.purchaseOrderService = purchaseOrderService;
        this.queueMobileService = queueMobileService;
        this.tokenQueueService = tokenQueueService;
        this.medicalRecordService = medicalRecordService;
        this.deviceService = deviceService;
        this.apiHealthService = apiHealthService;

        /* For executing in order of sequence. */
        this.executorService = newSingleThreadExecutor();
    }

    /** List all orders. */
    @PostMapping(
        value = "/showOrders/{codeQR}",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String showOrders(
        @RequestHeader ("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @PathVariable ("codeQR")
        ScrubbedInput codeQR,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Show orders for codeQR={} request from mail={} did={} deviceType={}", codeQR, mail, did, deviceType);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid, mail.getText(), did.getText(), "/api/m/s/purchaseOrder/showOrders")) return null;

        if (StringUtils.isBlank(codeQR.getText())) {
            LOG.warn("Not a valid codeQR={} qid={}", codeQR.getText(), qid);
            return getErrorReason("Not a valid queue code.", MOBILE_JSON);
        } else if (!businessUserStoreService.hasAccess(qid, codeQR.getText())) {
            LOG.info("Un-authorized store access to /api/m/s/purchaseOrder/showOrders by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            return purchaseOrderService.findAllOrderByCodeAsJson(codeQR.getText());
        } catch (Exception e) {
            LOG.error("Failed getting order clients reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/showOrders/{codeQR}",
                "showOrders",
                PurchaseOrderController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    //TODO add for history

    /**
     * When client is served by merchant.
     * And
     * When client starts to serve for first time or re-start after serving the last in the queue.
     */
    @PostMapping(
        value = "/served",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String served(
        @RequestHeader ("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        String requestBodyJson,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Served mail={} did={} deviceType={} auth={}", mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid, mail.getText(), did.getText(), "/api/m/s/purchaseOrder/served")) return null;

        try {
            Map<String, ScrubbedInput> map = ParseJsonStringToMap.jsonStringToMap(requestBodyJson);
            String codeQR = map.containsKey("qr") ? map.get("qr").getText() : null;

            if (StringUtils.isBlank(codeQR)) {
                LOG.warn("Not a valid codeQR={} qid={}", codeQR, qid);
                return getErrorReason("Not a valid queue code.", MOBILE_JSON);
            } else if (!businessUserStoreService.hasAccess(qid, codeQR)) {
                LOG.info("Un-authorized store access to /api/m/s/purchaseOrder/served by mail={}", mail);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
                return null;
            }

            String serveTokenString = map.containsKey("t") ? map.get("t").getText() : null;
            int servedNumber;
            if (StringUtils.isNumeric(serveTokenString)) {
                servedNumber = Integer.parseInt(serveTokenString);
            } else {
                LOG.warn("Not a valid number={} codeQR={} qid={}", serveTokenString, codeQR, qid);
                return getErrorReason("Not a valid number.", MOBILE_JSON);
            }

            PurchaseOrderStateEnum purchaseOrderState;
            try {
                purchaseOrderState = map.containsKey("p") ? PurchaseOrderStateEnum.valueOf(map.get("p").getText()) : null;
            } catch (IllegalArgumentException e) {
                LOG.error("Failed finding PurchaseOrderState reason={}", e.getLocalizedMessage(), e);
                return getErrorReason("Not a valid purchase order state.", MOBILE_JSON);
            }

            QueueStatusEnum queueStatus;
            try {
                queueStatus = map.containsKey("s") ? QueueStatusEnum.valueOf(map.get("s").getText()) : null;
                Assert.notNull(queueStatus, "Queue Status cannot be null");
            } catch (IllegalArgumentException e) {
                LOG.error("Failed finding QueueStatus reason={}", e.getLocalizedMessage(), e);
                return getErrorReason("Not a valid queue status.", MOBILE_JSON);
            }

            String goTo = map.containsKey("g") ? map.get("g").getText() : null;
            if (StringUtils.isBlank(goTo)) {
                return getErrorReason("Counter name cannot be empty.", MOBILE_JSON);
            } else {
                if (goTo.length() > counterNameLength) {
                    return getErrorReason("Counter name cannot exceed character size of 20.", MOBILE_JSON);
                }
            }

            TokenQueueEntity tokenQueue = queueMobileService.getTokenQueueByCodeQR(codeQR);
            LOG.info("queueStatus received={} found={}", queueStatus, tokenQueue.getQueueStatus());

            JsonToken jsonToken;
            switch (tokenQueue.getQueueStatus()) {
                case C:
                case D:
                case N:
                    jsonToken = purchaseOrderService.updateAndGetNextInQueue(codeQR, servedNumber, purchaseOrderState, goTo, did.getText(), TokenServiceEnum.M);
                    break;
                case R:
                case S:
                    jsonToken = purchaseOrderService.getNextInQueue(codeQR, goTo, did.getText());

                    /* Remove delay or any setting associated before starting of queue. */
                    LOG.info("Resetting queue when queue status={}", tokenQueue.getQueueStatus());
                    tokenQueueService.resetQueueSettingWhenQueueStarts(codeQR);
                    break;
                default:
                    LOG.error("Reached unsupported condition queueStatus={}", tokenQueue.getQueueStatus());
                    throw new UnsupportedOperationException("Reached unsupported condition for QueueStatus " + tokenQueue.getQueueStatus().getDescription());
            }

            if (null == jsonToken) {
                LOG.error("Could not find queue codeQR={} servedNumber={} queueUserState={}", codeQR, servedNumber, purchaseOrderState);
                return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
            }

            LOG.info("On served response servedNumber={} nowServicing={} jsonToken={}", servedNumber, jsonToken.getServingNumber(), jsonToken);
            return jsonToken.asJson();
        } catch (JsonMappingException e) {
            LOG.error("Failed parsing json={} qid={} message={}", requestBodyJson, qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/served",
                "served",
                QueueController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /**
     * Acquire specific token not in order. Send message of being served next to the owner of the token.
     */
    @PostMapping (
        value = "/acquire",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String acquire(
        @RequestHeader ("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        String requestBodyJson,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Acquired mail={} did={} deviceType={} auth={}", mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid, mail.getText(), did.getText(), "/api/m/s/purchaseOrder/acquire")) return null;

        try {
            Map<String, ScrubbedInput> map = ParseJsonStringToMap.jsonStringToMap(requestBodyJson);
            String codeQR = map.containsKey("qr") ? map.get("qr").getText() : null;

            if (StringUtils.isBlank(codeQR)) {
                LOG.warn("Not a valid codeQR={} qid={}", codeQR, qid);
                return getErrorReason("Not a valid queue code.", MOBILE_JSON);
            } else if (!businessUserStoreService.hasAccess(qid, codeQR)) {
                LOG.info("Un-authorized store access to /api/m/s/purchaseOrder/acquire by mail={}", mail);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
                return null;
            }

            String serveTokenString = map.containsKey("t") ? map.get("t").getText() : null;
            int servedNumber;
            if (StringUtils.isNumeric(serveTokenString)) {
                servedNumber = Integer.parseInt(serveTokenString);
            } else {
                LOG.warn("Not a valid number={} codeQR={} qid={}", serveTokenString, codeQR, qid);
                return getErrorReason("Not a valid number.", MOBILE_JSON);
            }

            QueueStatusEnum queueStatus;
            try {
                queueStatus = map.containsKey("s") ? QueueStatusEnum.valueOf(map.get("s").getText()) : null;
                Assert.notNull(queueStatus, "Queue Status cannot be null");
            } catch (IllegalArgumentException e) {
                LOG.error("Failed finding QueueStatus reason={}", e.getLocalizedMessage(), e);
                return getErrorReason("Not a valid queue status.", MOBILE_JSON);
            }

            String goTo = map.containsKey("g") ? map.get("g").getText() : "";
            if (StringUtils.isBlank(goTo)) {
                return getErrorReason("Counter name cannot be empty.", MOBILE_JSON);
            } else {
                if (counterNameLength < goTo.length()) {
                    return getErrorReason("Counter name cannot exceed character size of 20.", MOBILE_JSON);
                }
            }

            TokenQueueEntity tokenQueue = queueMobileService.getTokenQueueByCodeQR(codeQR);
            LOG.info("queueStatus received={} found={}, supports only queueStatus=Next", queueStatus, tokenQueue.getQueueStatus());

            JsonToken jsonToken;
            switch (tokenQueue.getQueueStatus()) {
                case N:
                    jsonToken = purchaseOrderService.getThisAsNextInQueue(codeQR, goTo, did.getText(), servedNumber);
                    break;
                case S:
                    LOG.info("Queue has not started qid={} queueStatus={} displayName={}", qid, queueStatus, tokenQueue.getDisplayName());
                    return getErrorReason("Click start button to begin", QUEUE_NOT_STARTED);
                case R:
                    LOG.info("Queue has not re-started qid={} queueStatus={} displayName={}", qid, queueStatus, tokenQueue.getDisplayName());
                    return getErrorReason("Click continue button to begin", QUEUE_NOT_RE_STARTED);
                default:
                    //TODO(hth) remind apps to call state of the queue when failure is encountered as state might have changed. Update app with this state.
                    LOG.error("Un-supported condition reached for acquiring token={} when queueStatus={}, supports only queueStatus=Next", servedNumber, tokenQueue.getQueueStatus());
                    throw new UnsupportedOperationException("Reached unsupported condition for QueueStatus " + tokenQueue.getQueueStatus().getDescription());
            }

            if (null == jsonToken) {
                LOG.warn("Failed to acquire client={} qid={} did={}", serveTokenString, qid, did);
                return getErrorReason("Could not acquire client " + serveTokenString, MERCHANT_COULD_NOT_ACQUIRE);
            }
            LOG.info("On served response servedNumber={} nowServicing={} jsonToken={}", servedNumber, jsonToken.getServingNumber(), jsonToken);
            return jsonToken.asJson();
        } catch (JsonMappingException e) {
            LOG.error("Failed parsing json={} qid={} message={}", requestBodyJson, qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/acquire",
                "acquire",
                QueueController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Merchant's different actions on order like processing, processed and delivered. */
    @PostMapping(
        value = "/actionOnOrder",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String actionOnOrder(
        @RequestHeader ("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        OrderServed orderServed,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Action on order mail={} did={} deviceType={} auth={}", mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid, mail.getText(), did.getText(), "/api/m/s/purchaseOrder/actionOnOrder")) return null;

        try {
            if (StringUtils.isBlank(orderServed.getCodeQR().getText())) {
                LOG.warn("Not a valid codeQR={} qid={}", orderServed.getCodeQR().getText(), qid);
                return getErrorReason("Not a valid queue code.", MOBILE_JSON);
            } else if (!businessUserStoreService.hasAccess(qid, orderServed.getCodeQR().getText())) {
                LOG.info("Un-authorized store access to /api/m/s/purchaseOrder/actionOnOrder by mail={}", mail);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
                return null;
            }

            PurchaseOrderStateEnum purchaseOrderState;
            try {
                purchaseOrderState = orderServed.getPurchaseOrderState();
            } catch (IllegalArgumentException e) {
                LOG.error("Failed finding PurchaseOrderState reason={}", e.getLocalizedMessage(), e);
                return getErrorReason("Not a valid purchase order state.", MOBILE_JSON);
            }

            try {
                Assert.notNull(orderServed.getQueueStatus(), "Queue Status cannot be null");
            } catch (IllegalArgumentException e) {
                LOG.error("Failed finding QueueStatus reason={}", e.getLocalizedMessage(), e);
                return getErrorReason("Not a valid queue status.", MOBILE_JSON);
            }

            if (StringUtils.isBlank(orderServed.getGoTo().getText())) {
                return getErrorReason("Counter name cannot be empty.", MOBILE_JSON);
            } else {
                if (orderServed.getGoTo().getText().length() > counterNameLength) {
                    return getErrorReason("Counter name cannot exceed character size of 20.", MOBILE_JSON);
                }
            }

            TokenQueueEntity tokenQueue = queueMobileService.getTokenQueueByCodeQR(orderServed.getCodeQR().getText());
            LOG.info("queueStatus received={} found={} orderServed={}", orderServed.getQueueStatus(), tokenQueue.getQueueStatus(), orderServed);

            JsonPurchaseOrderList jsonPurchaseOrderList;
            switch (purchaseOrderState) {
                case OP:
                    jsonPurchaseOrderList = purchaseOrderService.processedOrderService(
                        orderServed.getCodeQR().getText(),
                        orderServed.getServedNumber(),
                        orderServed.getPurchaseOrderState(),
                        orderServed.getGoTo().getText(),
                        did.getText(),
                        TokenServiceEnum.M);
                    break;
                case RP:
                case RD:
                    jsonPurchaseOrderList = purchaseOrderService.processedOrderService(
                        orderServed.getCodeQR().getText(),
                        orderServed.getServedNumber(),
                        orderServed.getPurchaseOrderState(),
                        orderServed.getGoTo().getText(),
                        did.getText(),
                        TokenServiceEnum.M);
                    break;
                default:
                    LOG.error("Reached unsupported condition queueStatus={}", tokenQueue.getQueueStatus());
                    throw new UnsupportedOperationException("Reached unsupported condition for QueueStatus " + tokenQueue.getQueueStatus().getDescription());

            }

            if (null == jsonPurchaseOrderList) {
                LOG.error("Could not find queue codeQR={} servedNumber={} queueUserState={}", orderServed.getCodeQR(), orderServed.getServedNumber(), purchaseOrderState);
                return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
            }

            LOG.info("On served response servedNumber={} jsonPurchaseOrderList={}", orderServed.getServedNumber(), jsonPurchaseOrderList);
            return jsonPurchaseOrderList.asJson();
        } catch (JsonMappingException e) {
            LOG.error("Failed parsing json qid={} message={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/actionOnOrder",
                "actionOnOrder",
                QueueController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Add purchase when merchant presses confirm. */
    @PostMapping(
        value = "/purchase",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String purchase(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput dt,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        JsonPurchaseOrder jsonPurchaseOrder,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Purchase order for did={} dt={}", did, dt);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid, mail.getText(), did.getText(), "/api/m/s/purchaseOrder/purchase")) return null;

        if (!businessUserStoreService.hasAccess(qid, jsonPurchaseOrder.getCodeQR())) {
            LOG.info("Un-authorized store access to /api/m/s/purchaseOrder/purchase by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            RegisteredDeviceEntity registeredDevice = deviceService.findRecentDevice(jsonPurchaseOrder.getQueueUserId());
            purchaseOrderService.createOrder(jsonPurchaseOrder, DeviceService.getExistingDeviceId(registeredDevice, did.getText()), TokenServiceEnum.M);
            LOG.info("Order Placed Successfully={}", jsonPurchaseOrder.getPresentOrderState());

            /* Send notification to all merchant. As there can be multiple merchants that needs notification for update. */
            executorService.execute(() -> purchaseOrderService.forceRefreshOnSomeActivity(jsonPurchaseOrder.getCodeQR(), jsonPurchaseOrder.getTransactionId()));
            if (null != registeredDevice) {
                /* Subscribe and Notify client. */
                BizStoreEntity bizStore = bizStoreManager.findByCodeQR(jsonPurchaseOrder.getCodeQR());
                executorService.execute(() -> queueMobileService.autoSubscribeClientToTopic(jsonPurchaseOrder.getCodeQR(), registeredDevice.getToken(), registeredDevice.getDeviceType()));
                executorService.execute(() -> queueMobileService.notifyClient(
                    registeredDevice,
                    "Order placed at " + bizStore.getDisplayName(),
                    "Your order number is " + jsonPurchaseOrder.getToken(),
                    bizStore.getCodeQR()));
            }

            return jsonPurchaseOrder.asJson();
        } catch (StoreInActiveException e) {
            LOG.warn("Failed placing order reason={}", e.getLocalizedMessage());
            return ErrorEncounteredJson.toJson("Store is offline", STORE_OFFLINE);
        } catch (StoreDayClosedException e) {
            LOG.warn("Failed placing order reason={}", e.getLocalizedMessage());
            return ErrorEncounteredJson.toJson("Store is closed today", STORE_DAY_CLOSED);
        } catch (StoreTempDayClosedException e) {
            LOG.warn("Failed placing order reason={}", e.getLocalizedMessage());
            return ErrorEncounteredJson.toJson("Store is temporary closed", STORE_TEMP_DAY_CLOSED);
        } catch (StorePreventJoiningException e) {
            LOG.warn("Failed placing order reason={}", e.getLocalizedMessage());
            return ErrorEncounteredJson.toJson("Store is not accepting new orders", STORE_PREVENT_JOIN);
        } catch(PriceMismatchException e) {
            LOG.error("Prices have changed since added to cart reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return ErrorEncounteredJson.toJson("Prices have changed since added to cart", PURCHASE_ORDER_PRICE_MISMATCH);
        } catch (Exception e) {
            LOG.error("Failed processing purchase order reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/purchase",
                "purchase",
                PurchaseOrderController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Add purchase when merchant presses confirm. */
    @PostMapping(
        value = "/medical/purchase",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String medicalPurchase(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput dt,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        JsonPurchaseOrder jsonPurchaseOrder,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Purchase order for did={} dt={}", did, dt);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid, mail.getText(), did.getText(), "/api/m/s/purchaseOrder/medical/purchase")) return null;

        if (!businessUserStoreService.hasAccess(qid, jsonPurchaseOrder.getCodeQR())) {
            LOG.info("Un-authorized store access to /api/m/s/purchaseOrder/medical/purchase by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            BizStoreEntity bizStore = bizStoreManager.findByCodeQR(jsonPurchaseOrder.getCodeQR());
            if (bizStore.getBusinessType() != BusinessTypeEnum.HS) {
                LOG.error("Store of type {}, cannot create health care order", bizStore.getBusinessType());
                return ErrorEncounteredJson.toJson("Cannot create this order", FAILED_PLACING_MEDICAL_ORDER_AS_INCORRECT_BUSINESS);
            }

            UserProfileEntity userProfile = userProfileManager.findByQueueUserId(jsonPurchaseOrder.getQueueUserId());
            jsonPurchaseOrder
                .setDeliveryAddress(userProfile.getAddress())
                .setDeliveryMode(DeliveryModeEnum.TO);

            RegisteredDeviceEntity registeredDevice;
            if (StringUtils.isNotBlank(userProfile.getGuardianPhone())) {
                String guardianQid = userProfileManager.findOneByPhone(userProfile.getGuardianPhone()).getQueueUserId();
                registeredDevice = deviceService.findRecentDevice(guardianQid);
            } else {
                registeredDevice = deviceService.findRecentDevice(userProfile.getQueueUserId());
            }

            purchaseOrderService.createOrder(jsonPurchaseOrder, DeviceService.getExistingDeviceId(registeredDevice, did.getText()), TokenServiceEnum.M);
            PurchaseOrderEntity purchaseOrder = purchaseOrderService.findByTransactionId(jsonPurchaseOrder.getTransactionId());

            JsonMedicalRecord jsonMedicalRecord = new JsonMedicalRecord()
                .setRecordReferenceId(purchaseOrder.getId())
                .setFormVersion(FormVersionEnum.MFD1)
                .setQueueUserId(jsonPurchaseOrder.getQueueUserId())
                .setCodeQR(bizStore.getCodeQR());

            switch (LabCategoryEnum.valueOf(bizStore.getBizCategoryId())) {
                case SPEC:
                case SCAN:
                case XRAY:
                case SONO:
                case MRI:
                    JsonMedicalRadiologyList jsonMedicalRadiologyList = new JsonMedicalRadiologyList();
                    for (JsonPurchaseOrderProduct jsonPurchaseOrderProduct : jsonPurchaseOrder.getJsonPurchaseOrderProducts()) {
                        JsonMedicalRadiology jsonMedicalRadiology = new JsonMedicalRadiology()
                            .setName(jsonPurchaseOrderProduct.getProductName());
                        jsonMedicalRadiologyList.addJsonMedicalRadiologies(jsonMedicalRadiology);
                    }
                    jsonMedicalRecord.addMedicalRadiologyLists(jsonMedicalRadiologyList);
                    break;
                case PATH:
                    JsonMedicalPathologyList jsonMedicalPathologyList = new JsonMedicalPathologyList();
                    for (JsonPurchaseOrderProduct jsonPurchaseOrderProduct : jsonPurchaseOrder.getJsonPurchaseOrderProducts()) {
                        JsonMedicalPathology jsonMedicalPathology = new JsonMedicalPathology()
                            .setName(jsonPurchaseOrderProduct.getProductName());
                        jsonMedicalPathologyList.addJsonMedicalPathologies(jsonMedicalPathology);
                    }
                    jsonMedicalRecord.addMedicalPathologiesLists(jsonMedicalPathologyList);
                    break;
                default:
                    LOG.error("Reached unsupported lab category {}", bizStore.getBizCategoryId());
                    throw new UnsupportedOperationException("Reached unsupported lab category " + bizStore.getBizCategoryId());
            }

            medicalRecordService.addMedicalRecordWhenExternal(jsonMedicalRecord, purchaseOrder.getTransactionId());
            LOG.info("Order Placed Successfully={}", jsonPurchaseOrder.getPresentOrderState());

            /* Send notification to all merchant. As there can be multiple merchants that needs notification for update. */
            executorService.execute(() -> purchaseOrderService.forceRefreshOnSomeActivity(jsonPurchaseOrder.getCodeQR(), jsonPurchaseOrder.getTransactionId()));
            if (null != registeredDevice) {
                /* Subscribe and Notify client. */
                executorService.execute(() -> queueMobileService.autoSubscribeClientToTopic(jsonPurchaseOrder.getCodeQR(), registeredDevice.getToken(), registeredDevice.getDeviceType()));
                executorService.execute(() -> queueMobileService.notifyClient(registeredDevice,
                    "Order placed at " + bizStore.getDisplayName(),
                    "Your order number is " + jsonPurchaseOrder.getToken(),
                    bizStore.getCodeQR()));
            }

            return jsonPurchaseOrder.asJson();
        } catch (StoreInActiveException e) {
            LOG.warn("Failed placing order reason={}", e.getLocalizedMessage());
            return ErrorEncounteredJson.toJson("Store is offline", STORE_OFFLINE);
        } catch (StoreDayClosedException e) {
            LOG.warn("Failed placing order reason={}", e.getLocalizedMessage());
            return ErrorEncounteredJson.toJson("Store is closed today", STORE_DAY_CLOSED);
        } catch (StoreTempDayClosedException e) {
            LOG.warn("Failed placing order reason={}", e.getLocalizedMessage());
            return ErrorEncounteredJson.toJson("Store is temporary closed", STORE_TEMP_DAY_CLOSED);
        } catch (StorePreventJoiningException e) {
            LOG.warn("Failed placing order reason={}", e.getLocalizedMessage());
            return ErrorEncounteredJson.toJson("Store is not accepting new orders", STORE_PREVENT_JOIN);
        } catch(PriceMismatchException e) {
            LOG.error("Prices have changed since added to cart reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return ErrorEncounteredJson.toJson("Prices have changed since added to cart", PURCHASE_ORDER_PRICE_MISMATCH);
        } catch (Exception e) {
            LOG.error("Failed processing purchase order reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/medical/purchase",
                "medicalPurchase",
                PurchaseOrderController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Modify order that was zero value. */
    @PostMapping(
        value = "/modify",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String modify(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput dt,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        JsonPurchaseOrder jsonPurchaseOrder,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Modify purchase order for did={} dt={}", did, dt);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid, mail.getText(), did.getText(), "/api/m/s/purchaseOrder/modify")) return null;

        if (!businessUserStoreService.hasAccess(qid, jsonPurchaseOrder.getCodeQR())) {
            LOG.info("Un-authorized store access to /api/m/s/purchaseOrder/modify by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            JsonPurchaseOrder jsonPurchaseOrderUpdated = purchaseOrderService.modifyOrder(jsonPurchaseOrder, did.getText(), TokenServiceEnum.M);
            LOG.info("Order modified Successfully={}", jsonPurchaseOrderUpdated.getPresentOrderState());
            return jsonPurchaseOrderUpdated.asJson();
        } catch (PurchaseOrderProductNFException e) {
            LOG.warn("Purchase Order Product not found reason={}", e.getLocalizedMessage());
            return ErrorEncounteredJson.toJson("Purchase Order Product Not Found", PURCHASE_ORDER_PRODUCT_NOT_FOUND);
        } catch (Exception e) {
            LOG.error("Failed modifying purchase order product reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/modify",
                "modify",
                PurchaseOrderController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** When merchant accepts partial payment at counter. */
    @PostMapping(
        value = "/partialCounterPayment",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String partialCounterPayment(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput dt,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        JsonPurchaseOrder jsonPurchaseOrder,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Purchase order partial payment for did={} dt={}", did, dt);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid, mail.getText(), did.getText(), "/api/m/s/purchaseOrder/partialCounterPayment")) return null;

        if (!businessUserStoreService.hasAccess(qid, jsonPurchaseOrder.getCodeQR())) {
            LOG.info("Un-authorized store access to /api/m/s/purchaseOrder/partialCounterPayment by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            JsonPurchaseOrder jsonPurchaseOrderUpdated = purchaseOrderService.partialCounterPayment(jsonPurchaseOrder, qid);
            LOG.info("Order partial payment updated successfully={}", jsonPurchaseOrderUpdated);

            /* Send notification to all merchant. As there can be multiple merchants that needs notification for update. */
            executorService.execute(() -> purchaseOrderService.forceRefreshOnSomeActivity(jsonPurchaseOrder.getCodeQR(), jsonPurchaseOrder.getTransactionId()));

            RegisteredDeviceEntity registeredDevice = deviceService.findRecentDevice(jsonPurchaseOrder.getQueueUserId());
            if (null != registeredDevice) {
                TokenQueueEntity tokenQueue = tokenQueueService.findByCodeQR(jsonPurchaseOrder.getCodeQR());
                executorService.execute(() -> queueMobileService.notifyClient(registeredDevice,
                    "Partial payment applied for " + tokenQueue.getDisplayName(),
                    "Your order at " + tokenQueue.getDisplayName() + " number " + jsonPurchaseOrder.getToken()
                        + " has been partially paid at the counter via " + jsonPurchaseOrder.getPaymentMode().getDescription(),
                    jsonPurchaseOrder.getCodeQR()));
            }

            return jsonPurchaseOrderUpdated.asJson();
        } catch (Exception e) {
            LOG.error("Failed processing partial payment on order reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return ErrorEncounteredJson.toJson("Failed Updating Order Payment", ORDER_PAYMENT_UPDATE_FAILED);
        } finally {
            apiHealthService.insert(
                "/partialCounterPayment",
                "partialCounterPayment",
                PurchaseOrderController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** When payment is performed at counter via external means. */
    @PostMapping(
        value = "/counterPayment",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String counterPayment(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput dt,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        JsonPurchaseOrder jsonPurchaseOrder,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Purchase Order Cash Payment API for did={} dt={}", did, dt);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid, mail.getText(), did.getText(), "/api/m/s/purchaseOrder/counterPayment")) return null;

        if (!businessUserStoreService.hasAccess(qid, jsonPurchaseOrder.getCodeQR())) {
            LOG.info("Un-authorized store access to /api/m/s/purchaseOrder/counterPayment by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            JsonPurchaseOrder jsonPurchaseOrderUpdated = purchaseOrderService.counterPayment(jsonPurchaseOrder, qid);
            LOG.info("Order counter payment updated successfully={}", jsonPurchaseOrderUpdated);

            /* Send notification to all merchant. As there can be multiple merchants that needs notification for update. */
            executorService.execute(() -> purchaseOrderService.forceRefreshOnSomeActivity(jsonPurchaseOrder.getCodeQR(), jsonPurchaseOrder.getTransactionId()));

            RegisteredDeviceEntity registeredDevice = deviceService.findRecentDevice(jsonPurchaseOrder.getQueueUserId());
            if (null != registeredDevice) {
                TokenQueueEntity tokenQueue = tokenQueueService.findByCodeQR(jsonPurchaseOrder.getCodeQR());
                executorService.execute(() -> queueMobileService.notifyClient(registeredDevice,
                    "Paid at counter for " + tokenQueue.getDisplayName(),
                    "Your order at " + tokenQueue.getDisplayName() + " number " + jsonPurchaseOrder.getToken()
                        + " has been paid at the counter via " + jsonPurchaseOrder.getPaymentMode().getDescription(),
                    jsonPurchaseOrder.getCodeQR()));
            }

            return jsonPurchaseOrderUpdated.asJson();
        } catch (Exception e) {
            LOG.error("Failed processing cash payment on order reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return ErrorEncounteredJson.toJson("Failed Updating Order Payment", ORDER_PAYMENT_UPDATE_FAILED);
        } finally {
            apiHealthService.insert(
                "/counterPayment",
                "counterPayment",
                PurchaseOrderController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Cancel placed order. */
    @PostMapping(
        value = "/cancel",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String cancel(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput dt,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        OrderServed orderServed,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Cancel order mail={} did={} deviceType={} auth={}", mail, did, dt, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid, mail.getText(), did.getText(), "/api/m/s/purchaseOrder/cancel")) return null;

        if (!businessUserStoreService.hasAccess(qid, orderServed.getCodeQR().getText())) {
            LOG.info("Un-authorized store access to /api/m/s/purchaseOrder/cancel by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            if (purchaseOrderService.isOrderCancelled(orderServed.getCodeQR().getText(), orderServed.getServedNumber())) {
                return getErrorReason("Order already cancelled", PURCHASE_ORDER_ALREADY_CANCELLED);
            }

            JsonPurchaseOrderList jsonPurchaseOrderList = purchaseOrderService.cancelOrderByMerchant(orderServed.getQueueUserId(), orderServed.getTransactionId());
            JsonPurchaseOrder jsonPurchaseOrderUpdated = jsonPurchaseOrderList.getPurchaseOrders().get(0);
            if (BusinessTypeEnum.HS == jsonPurchaseOrderUpdated.getBusinessType()) {
                medicalRecordService.deleteReminisceOfTransactionId(jsonPurchaseOrderUpdated.getTransactionId());
            }

            UserProfileEntity userProfile = userProfileManager.findByQueueUserId(jsonPurchaseOrderUpdated.getQueueUserId());
            RegisteredDeviceEntity registeredDevice;
            if (StringUtils.isNotBlank(userProfile.getGuardianPhone())) {
                String guardianQid = userProfileManager.findOneByPhone(userProfile.getGuardianPhone()).getQueueUserId();
                registeredDevice = deviceService.findRecentDevice(guardianQid);
            } else {
                registeredDevice = deviceService.findRecentDevice(userProfile.getQueueUserId());
            }

            if (null != registeredDevice) {
                TokenQueueEntity tokenQueue = tokenQueueService.findByCodeQR(jsonPurchaseOrderUpdated.getCodeQR());
                String title, body;
                if (new BigDecimal(jsonPurchaseOrderUpdated.getOrderPriceForDisplay()).intValue() > 0) {
                    title = "Refund initiated by " + tokenQueue.getDisplayName();
                    body = "You have been refunded net total of " + jsonPurchaseOrderUpdated.getOrderPriceForDisplay()
                        + (jsonPurchaseOrderUpdated.getTransactionVia() == TransactionViaEnum.I
                        ? " to your " + jsonPurchaseOrderUpdated.getPaymentMode().getDescription()
                        : " at counter");
                } else {
                    title = "Cancelled order by "  + tokenQueue.getDisplayName();
                    body = "Your order was cancelled by merchant";
                }

                executorService.execute(() -> queueMobileService.notifyClient(registeredDevice,
                    title,
                    body,
                    jsonPurchaseOrderUpdated.getCodeQR()));
            }

            LOG.info("Order Cancelled Successfully={}", jsonPurchaseOrderList.getPurchaseOrders().get(0).getPresentOrderState());
            return jsonPurchaseOrderList.asJson();
        } catch (Exception e) {
            LOG.error("Failed cancelling purchase order orderNumber={} codeQR={} purchaseOrderState={} reason={}",
                orderServed.getServedNumber(),
                orderServed.getCodeQR(),
                orderServed.getPurchaseOrderState(),
                e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Failed to cancel order", PURCHASE_ORDER_FAILED_TO_CANCEL);
        } finally {
            apiHealthService.insert(
                "/cancel",
                "cancel",
                PurchaseOrderController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /**
     * @since 1.2.235. Stopped upload of file from Merchant Store.
     */
    @PostMapping (
        value = "/addAttachment",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    @Deprecated
    public String addAttachment(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput dt,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @RequestPart("file")
        MultipartFile multipartFile,

        @RequestPart("ti")
        String transactionId,

        HttpServletResponse response
    ) throws IOException {
        LOG.info("Add attachment mail={} did={} deviceType={} auth={}", mail, did, dt, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid, mail.getText(), did.getText(), "/api/m/s/purchaseOrder/addAttachment")) return null;

        Map<String, String> errors = imageValidator.validate(multipartFile, ImageValidator.SUPPORTED_FILE.IMAGE_AND_PDF);
        if (!errors.isEmpty()) {
            return ErrorEncounteredJson.toJson(errors);
        }

        PurchaseOrderEntity purchaseOrder = purchaseOrderService.findByTransactionId(transactionId);
        if (null == purchaseOrder) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid token");
            return null;
        }

        BizStoreEntity bizStore = bizStoreManager.getById(purchaseOrder.getBizStoreId());
        if (bizStore.getBusinessType() == BusinessTypeEnum.HS) {
            LabCategoryEnum labCategory = LabCategoryEnum.valueOf(bizStore.getBizCategoryId());
            return imageCommonHelper.processReport(
                did.getText(),
                dt.getText(),
                mail.getText(),
                auth.getText(),
                transactionId,
                labCategory,
                multipartFile,
                response);
        }

        return new JsonResponse(false, null).asJson();
    }

    /**
     * @since 1.2.235. Stopped upload of file from Merchant Store.
     */
    @PostMapping (
        value = "/removeAttachment",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    @Deprecated
    public String removeAttachment(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput dt,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        LabFile labFile,

        HttpServletResponse response
    ) throws IOException {
        LOG.info("Removed image mail={} did={} deviceType={} auth={}", mail, did, dt, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid, mail.getText(), did.getText(), "/api/m/s/purchaseOrder/removeAttachment")) return null;

        PurchaseOrderEntity purchaseOrder = purchaseOrderService.findByTransactionId(labFile.getTransactionId());
        if (null == purchaseOrder) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid token");
            return null;
        }

        BizStoreEntity bizStore = bizStoreManager.getById(purchaseOrder.getBizStoreId());
        if (bizStore.getBusinessType() == BusinessTypeEnum.HS) {
            LabCategoryEnum labCategory = LabCategoryEnum.valueOf(bizStore.getBizCategoryId());
            return imageCommonHelper.removeReport(
                did.getText(),
                dt.getText(),
                mail.getText(),
                auth.getText(),
                labFile.getTransactionId(),
                labFile.getDeleteAttachment(),
                labCategory,
                response);
        }

        return new JsonResponse(false).asJson();
    }

    /**
     * Retrieve record before adding.
     * @since 1.2.235. Stopped upload of file from Merchant Store.
     */
    @PostMapping(
        value = "/showAttachment",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    @Deprecated
    public String showAttachment(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        LabFile labFile,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Show attachment mail={} did={} deviceType={} auth={}", mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid, mail.getText(), did.getText(), "/api/m/s/purchaseOrder/showAttachment")) return null;

        try {
            PurchaseOrderEntity purchaseOrder = purchaseOrderService.findByTransactionId(labFile.getTransactionId());
            if (null == purchaseOrder) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid token");
                return null;
            }

            BizStoreEntity bizStore = bizStoreManager.getById(purchaseOrder.getBizStoreId());
            if(bizStore.getBusinessType() == BusinessTypeEnum.HS) {
                LabCategoryEnum labCategory = LabCategoryEnum.valueOf(bizStore.getBizCategoryId());
                switch (labCategory) {
                    case MRI:
                    case SONO:
                    case XRAY:
                    case SCAN:
                    case SPEC:
                        MedicalRadiologyEntity medicalRadiology = medicalRadiologyManager.findByTransactionId(labFile.getTransactionId());
                        labFile
                            .setRecordReferenceId(medicalRadiology.getId())
                            .setFiles(medicalRadiology.getImages());
                        break;
                    case PATH:
                        MedicalPathologyEntity medicalPathology = medicalPathologyManager.findByTransactionId(labFile.getTransactionId());
                        labFile
                            .setRecordReferenceId(medicalPathology.getId())
                            .setFiles(medicalPathology.getImages());
                        break;
                    default:
                        LOG.error("Reached unreachable condition {}", labCategory);
                        throw new UnsupportedOperationException("Reached unreachable condition");
                }
            }

            return labFile.asJson();
        } catch (Exception e) {
            LOG.error("Failed showing attachment record json={} qid={} message={}", labFile.asJson(), qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/showAttachment",
                "showAttachment",
                PurchaseOrderController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }
}
