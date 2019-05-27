package com.noqapp.mobile.view.controller.api.client;

import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.common.utils.CommonUtil.UNAUTHORIZED;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.DEVICE_DETAIL_MISSING;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.PURCHASE_ORDER_FAILED_TO_CANCEL;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.PURCHASE_ORDER_FAILED_TO_CANCEL_AS_EXTERNALLY_PAID;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.PURCHASE_ORDER_FAILED_TO_CANCEL_PARTIAL_PAY;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.PURCHASE_ORDER_NOT_FOUND;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.QUEUE_JOIN_FAILED_PAYMENT_CALL_REQUEST;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.QUEUE_JOIN_PAYMENT_FAILED;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.QUEUE_NO_SERVICE_NO_PAY;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.STORE_NO_LONGER_EXISTS;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.TRANSACTION_GATEWAY_DEFAULT;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.PurchaseOrderEntity;
import com.noqapp.domain.common.DomainCommonUtil;
import com.noqapp.domain.json.JsonPurchaseOrder;
import com.noqapp.domain.json.JsonResponse;
import com.noqapp.domain.json.JsonToken;
import com.noqapp.domain.json.JsonTokenAndQueueList;
import com.noqapp.domain.json.payment.cashfree.JsonCashfreeNotification;
import com.noqapp.domain.json.payment.cashfree.JsonResponseWithCFToken;
import com.noqapp.domain.types.AppFlavorEnum;
import com.noqapp.domain.types.DeviceTypeEnum;
import com.noqapp.domain.types.PaymentModeEnum;
import com.noqapp.domain.types.PaymentStatusEnum;
import com.noqapp.domain.types.PurchaseOrderStateEnum;
import com.noqapp.domain.types.TokenServiceEnum;
import com.noqapp.domain.types.cashfree.TxStatusEnum;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.common.util.ErrorEncounteredJson;
import com.noqapp.mobile.domain.body.client.JoinQueue;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.service.QueueMobileService;
import com.noqapp.mobile.service.TokenQueueMobileService;
import com.noqapp.mobile.service.exception.DeviceDetailMissingException;
import com.noqapp.mobile.service.exception.StoreNoLongerExistsException;
import com.noqapp.mobile.view.common.ParseTokenFCM;
import com.noqapp.repository.ScheduleAppointmentManager;
import com.noqapp.service.PurchaseOrderService;
import com.noqapp.service.ScheduleAppointmentService;
import com.noqapp.service.exceptions.PurchaseOrderCancelException;
import com.noqapp.service.exceptions.PurchaseOrderFailException;
import com.noqapp.service.exceptions.PurchaseOrderRefundExternalException;
import com.noqapp.service.exceptions.PurchaseOrderRefundPartialException;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import javax.servlet.http.HttpServletResponse;

/**
 * Remote scan of QR code is only available to registered user.
 *
 * User: hitender
 * Date: 3/31/17 7:23 PM
 */
@SuppressWarnings ({
        "PMD.BeanMembersShouldSerialize",
        "PMD.LocalVariableCouldBeFinal",
        "PMD.MethodArgumentCouldBeFinal",
        "PMD.LongVariable"
})
@RestController
@RequestMapping (value = "/api/c/token")
public class TokenQueueAPIController {
    private static final Logger LOG = LoggerFactory.getLogger(TokenQueueAPIController.class);

    private TokenQueueMobileService tokenQueueMobileService;
    private QueueMobileService queueMobileService;
    private AuthenticateMobileService authenticateMobileService;
    private PurchaseOrderService purchaseOrderService;
    private ScheduleAppointmentService scheduleAppointmentService;
    private ApiHealthService apiHealthService;

    @Autowired
    public TokenQueueAPIController(
        TokenQueueMobileService tokenQueueMobileService,
        QueueMobileService queueMobileService,
        AuthenticateMobileService authenticateMobileService,
        PurchaseOrderService purchaseOrderService,
        ScheduleAppointmentService scheduleAppointmentService,
        ApiHealthService apiHealthService
    ) {
        this.tokenQueueMobileService = tokenQueueMobileService;
        this.queueMobileService = queueMobileService;
        this.authenticateMobileService = authenticateMobileService;
        this.purchaseOrderService = purchaseOrderService;
        this.scheduleAppointmentService = scheduleAppointmentService;
        this.apiHealthService = apiHealthService;
    }

    /** Get state of queue at the store. */
    @GetMapping(
        value = "/{codeQR}",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String getQueueState(
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
        LOG.info("On scan get state did={} dt={} codeQR={}", did, deviceType, codeQR);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        if (!tokenQueueMobileService.isValidCodeQR(codeQR.getText())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid token");
            return null;
        }

        try {
            return tokenQueueMobileService.findTokenState(codeQR.getText()).asJson();
        } catch (StoreNoLongerExistsException e) {
            LOG.info("Store no longer exists qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            apiHealthService.insert(
                "/{codeQR}",
                "getQueueState",
                TokenQueueAPIController.class.getName(),
                Duration.between(start, Instant.now()),
                HealthStatusEnum.G);
            return getErrorReason("Store is not available.", STORE_NO_LONGER_EXISTS);
        } catch (Exception e) {
            LOG.error("Failed getting queue state qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/{codeQR}",
                "getQueueState",
                TokenQueueAPIController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Get all state of queue at a Business when one QR Code is scanned. */
    @GetMapping (
        value = "/v1/{codeQR}",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String getAllQueueState(
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
        LOG.info("On scan get all state did={} dt={} codeQR={}", did, deviceType, codeQR);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        if (!tokenQueueMobileService.isValidCodeQR(codeQR.getText())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid token");
            return null;
        }

        try {
            return tokenQueueMobileService.findAllBizStoreByBizNameCodeQR(codeQR.getText()).asJson();
        } catch (Exception e) {
            LOG.error("Failed getting all queue state qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/v1/{codeQR}",
                "getAllQueueState",
                TokenQueueAPIController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Get all the queues user has token from. In short all the queues user has joined AND/OR all placed orders. */
    @GetMapping (
        value = "/queues",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String getAllJoinedQueues(
        @RequestHeader ("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("All joined queue did={} dt={} mail={}", did, deviceType, mail);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        try {
            JsonTokenAndQueueList jsonTokenAndQueues = queueMobileService.findAllJoinedQueues(qid, did.getText());
            jsonTokenAndQueues.getTokenAndQueues().addAll(purchaseOrderService.findAllOpenOrderAsJson(qid));
            jsonTokenAndQueues.setJsonScheduleList(scheduleAppointmentService.findAllUpComingAppointments(qid));
            return jsonTokenAndQueues.asJson();
        } catch (Exception e) {
            LOG.error("Failed getting queues qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/queues",
                "getAllJoinedQueues",
                TokenQueueAPIController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Get all pending orders. */
    @GetMapping (
        value = "/pendingOrder",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String pendingPurchaseOrder(
        @RequestHeader ("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("All pending purchase order did={} dt={} mail={}", did, deviceType, mail);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        try {
            return new JsonTokenAndQueueList().setTokenAndQueues(purchaseOrderService.findPendingPurchaseOrderAsJson(qid)).asJson();
        } catch (Exception e) {
            LOG.error("Failed getting pendingOrder qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/pendingOrder",
                "pendingPurchaseOrder",
                TokenQueueAPIController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }
    
    /** Get all the historical queues user has token from. In short all the queues and order user has joined in past. */
    @PostMapping(
            value = "/historical",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String allHistoricalJoinedQueues(
            @RequestHeader ("X-R-DID")
            ScrubbedInput did,

            @RequestHeader ("X-R-DT")
            ScrubbedInput deviceType,

            @RequestHeader (value = "X-R-AF", required = false, defaultValue = "NQMT")
            ScrubbedInput appFlavor,

            @RequestHeader ("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            @RequestBody
            String tokenJson,

            HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("All historical joined queue did={} dt={} mail={}", did, deviceType, mail);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        ParseTokenFCM parseTokenFCM = ParseTokenFCM.newInstance(tokenJson);
        if (StringUtils.isNotBlank(parseTokenFCM.getErrorResponse())) {
            return parseTokenFCM.getErrorResponse();
        }

        try {
            JsonTokenAndQueueList jsonTokenAndQueues = queueMobileService.findHistoricalQueue(
                qid,
                did.getText(),
                DeviceTypeEnum.valueOf(deviceType.getText()),
                AppFlavorEnum.valueOf(appFlavor.getText()),
                parseTokenFCM.getTokenFCM(),
                parseTokenFCM.getModel(),
                parseTokenFCM.getOsVersion(),
                parseTokenFCM.getAppVersion());
            //TODO(hth) get old historical order, it just gets todays historical order
            jsonTokenAndQueues.getTokenAndQueues().addAll(purchaseOrderService.findAllDeliveredHistoricalOrderAsJson(qid));
            return jsonTokenAndQueues.asJson();
        } catch (DeviceDetailMissingException e) {
            LOG.error("Failed registering deviceType={}, reason={}", deviceType, e.getLocalizedMessage(), e);
            return getErrorReason("Missing device details", DEVICE_DETAIL_MISSING);
        } catch (Exception e) {
            LOG.error("Failed getting history qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/historical",
                "allHistoricalJoinedQueues",
                TokenQueueAPIController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Join the queue. */
    @PostMapping (
        value = "/queue",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String joinQueue(
        @RequestHeader ("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        JoinQueue joinQueue,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Join queue did={} dt={}", did, deviceType);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        BizStoreEntity bizStore = tokenQueueMobileService.getBizService().findByCodeQR(joinQueue.getCodeQR());
        if (null == bizStore) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid QR Code");
            return null;
        }

        try {
            LOG.info("codeQR={} qid={} guardianQid={}", joinQueue.getCodeQR(), joinQueue.getQueueUserId(), joinQueue.getGuardianQid());

            if (!bizStore.isEnabledPayment()) {
                return tokenQueueMobileService.joinQueue(
                    joinQueue.getCodeQR(),
                    did.getText(),
                    joinQueue.getQueueUserId(),
                    joinQueue.getGuardianQid(),
                    bizStore.getAverageServiceTime(),
                    TokenServiceEnum.C).asJson();
            }

            return getErrorReason("Missing Payment For Service", QUEUE_JOIN_FAILED_PAYMENT_CALL_REQUEST);
        } catch (Exception e) {
            LOG.error("Failed joining queue qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/queue",
                "joinQueue",
                TokenQueueAPIController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Join the queue when their is pre-payment. */
    @PostMapping (
        value = "/payBeforeQueue",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String payBeforeQueue(
        @RequestHeader ("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        JoinQueue joinQueue,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Join payBeforeQueue did={} dt={}", did, deviceType);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        BizStoreEntity bizStore = tokenQueueMobileService.getBizService().findByCodeQR(joinQueue.getCodeQR());
        if (null == bizStore) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid QR Code");
            return null;
        }

        try {
            LOG.info("Pay Before codeQR={} qid={} guardianQid={}", joinQueue.getCodeQR(), joinQueue.getQueueUserId(), joinQueue.getGuardianQid());
            JsonToken jsonToken =  tokenQueueMobileService.payBeforeJoinQueue(
                joinQueue.getCodeQR(),
                did.getText(),
                joinQueue.getQueueUserId(),
                joinQueue.getGuardianQid(),
                bizStore,
                TokenServiceEnum.C);

            LOG.info("Pay Before Join Response {}", jsonToken.getJsonPurchaseOrder());
            return jsonToken.asJson();
        } catch (Exception e) {
            LOG.error("Failed joining payBeforeQueue qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/payBeforeQueue",
                "payBeforeQueue",
                TokenQueueAPIController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    @PostMapping (
        value = "/skipPayBeforeQueue",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String skipPayBeforeQueue(
        @RequestHeader ("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        JoinQueue joinQueue,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Join skipPayBeforeQueue did={} dt={}", did, deviceType);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        BizStoreEntity bizStore = tokenQueueMobileService.getBizService().findByCodeQR(joinQueue.getCodeQR());
        if (null == bizStore) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid QR Code");
            return null;
        }

        try {
            LOG.info("Skip Pay codeQR={} qid={} guardianQid={}", joinQueue.getCodeQR(), joinQueue.getQueueUserId(), joinQueue.getGuardianQid());
            JsonToken jsonToken = tokenQueueMobileService.skipPayBeforeJoinQueue(
                joinQueue.getCodeQR(),
                did.getText(),
                joinQueue.getQueueUserId(),
                joinQueue.getGuardianQid(),
                bizStore,
                TokenServiceEnum.C);

            LOG.info("Skip Pay Before Join {} {} {}", jsonToken.getToken(), jsonToken.getCustomerName(), jsonToken.getDisplayName());
            return jsonToken.asJson();
        } catch (Exception e) {
            LOG.error("Failed joining payBeforeQueue qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/skipPayBeforeQueue",
                "skipPayBeforeQueue",
                TokenQueueAPIController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Cashfree transaction response sent to server. Based on cashfree, server updates order status */
    @PostMapping(
        value = "/cf/notify",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String cashfreeNotify(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput dt,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        JsonCashfreeNotification jsonCashfreeNotification,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Cashfree notification request from mail={} auth={}", mail, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/c/token/cf/notify by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            if (StringUtils.isBlank(jsonCashfreeNotification.getOrderId())) {
                return getErrorReason("Order not found", PURCHASE_ORDER_NOT_FOUND);
            }

            String transactionId = jsonCashfreeNotification.getOrderId();
            PaymentStatusEnum paymentStatus;
            PurchaseOrderStateEnum purchaseOrderState;
            switch (TxStatusEnum.valueOf(jsonCashfreeNotification.getTxStatus())) {
                case SUCCESS:
                    paymentStatus = PaymentStatusEnum.PA;
                    purchaseOrderState = PurchaseOrderStateEnum.PO;
                    break;
                case FAILED:
                    paymentStatus = PaymentStatusEnum.PF;
                    purchaseOrderState = PurchaseOrderStateEnum.FO;
                    break;
                case FLAGGED:
                    paymentStatus = PaymentStatusEnum.FP;
                    purchaseOrderState = PurchaseOrderStateEnum.FO;
                    break;
                case PENDING:
                    paymentStatus = PaymentStatusEnum.PP;
                    purchaseOrderState = PurchaseOrderStateEnum.FO;
                    break;
                case CANCELLED:
                    paymentStatus = PaymentStatusEnum.PC;
                    purchaseOrderState = PurchaseOrderStateEnum.FO;
                    break;
                default:
                    LOG.error("Unknown field {}", jsonCashfreeNotification.getTxStatus());
                    return getErrorReason("Unknown Transaction Field", TRANSACTION_GATEWAY_DEFAULT);
            }

            PaymentModeEnum paymentMode;
            if (new BigDecimal(jsonCashfreeNotification.getOrderAmount()).intValue() > 0) {
                paymentMode = DomainCommonUtil.derivePaymentMode(jsonCashfreeNotification.getPaymentMode());
            } else {
                paymentMode = PaymentModeEnum.CA;
                jsonCashfreeNotification.setTxMsg("Cash Payment At Counter");
            }

            //TODO try appending transaction message
            PurchaseOrderEntity purchaseOrder = purchaseOrderService.updateOnPaymentGatewayNotification(
                transactionId,
                jsonCashfreeNotification.getTxMsg(),
                jsonCashfreeNotification.getReferenceId(),
                paymentStatus,
                purchaseOrderState,
                paymentMode
            );

            if (paymentStatus != PaymentStatusEnum.PA || purchaseOrder.getPresentOrderState() != PurchaseOrderStateEnum.PO) {
                tokenQueueMobileService.deleteReferenceToTransactionId(purchaseOrder.getCodeQR(), purchaseOrder.getTransactionId());
                return getErrorReason("Payment Failed", QUEUE_JOIN_PAYMENT_FAILED);
            }

            JsonToken jsonToken = tokenQueueMobileService.updateWhenPaymentSuccessful(purchaseOrder.getCodeQR(), purchaseOrder.getTransactionId())
                .setJsonPurchaseOrder(new JsonPurchaseOrder(purchaseOrder));

            return jsonToken.asJson();
        } catch (Exception e) {
            LOG.error("Failed updating with cashfree notification reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/cf/notify",
                "cashfreeNotify",
                TokenQueueAPIController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    @PostMapping(
        value = "/paymentInitiate",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String paymentInitiate(
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
        LOG.info("Gateway token for initiating payment transaction through gateway from mail={} auth={}", mail, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/c/token/paymentInitiate by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            if (StringUtils.isBlank(jsonPurchaseOrder.getTransactionId())) {
                return getErrorReason("Order not found", PURCHASE_ORDER_NOT_FOUND);
            }

            if (!tokenQueueMobileService.getBizService().isValidCodeQR(jsonPurchaseOrder.getCodeQR())) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid QR Code");
                return null;
            }

            JsonResponseWithCFToken jsonResponseWithCFToken = tokenQueueMobileService.createTokenForPaymentGateway(qid, jsonPurchaseOrder.getCodeQR(), jsonPurchaseOrder.getTransactionId());
            if (null == jsonResponseWithCFToken) {
                return getErrorReason("Order not found", PURCHASE_ORDER_NOT_FOUND);
            }

            return jsonResponseWithCFToken.asJson();
        } catch (PurchaseOrderFailException e) {
            LOG.error("No payment needed when service not performed");
            methodStatusSuccess = false;
            return ErrorEncounteredJson.toJson("Cannot accept payment when not serviced", QUEUE_NO_SERVICE_NO_PAY);
        } catch (Exception e) {
            LOG.error("Failed gateway token for payment reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/paymentInitiate",
                "paymentInitiate",
                TokenQueueAPIController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** When customer decides to not pay, the order is deleted and there is no reference. */
    @PostMapping(
        value = "/cancelPayBeforeQueue",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String cancelPayBeforeQueue(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput dt,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        JsonToken jsonToken,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("No payment made and hence removing the reference to order mail={} auth={}", mail, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/c/token/cancelPayBeforeQueue by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            if (StringUtils.isBlank(jsonToken.getJsonPurchaseOrder().getTransactionId())) {
                return getErrorReason("Order not found", PURCHASE_ORDER_NOT_FOUND);
            }

            tokenQueueMobileService.deleteReferenceToTransactionId(jsonToken.getCodeQR(), jsonToken.getJsonPurchaseOrder().getTransactionId());
            return new JsonResponse(true).asJson();
        } catch (Exception e) {
            LOG.error("Failed updating with cashfree notification reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/cancelPayBeforeQueue",
                "cancelPayBeforeQueue",
                TokenQueueAPIController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Shows purchase order for queue in after join or at any time. */
    @GetMapping(
        value = "/purchaseOrder/{token}/{codeQR}",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String purchaseOrder(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput dt,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @PathVariable ("token")
        ScrubbedInput token,

        @PathVariable ("codeQR")
        ScrubbedInput codeQR,

        HttpServletResponse response
    ) throws IOException {
        Instant start = Instant.now();
        LOG.info("Find purchase order queue did={} dt={} codeQR={} token={}", did, dt, codeQR, token);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        if (!tokenQueueMobileService.getBizService().isValidCodeQR(codeQR.getText())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid token");
            return null;
        }

        try {
            JsonPurchaseOrder jsonPurchaseOrder = tokenQueueMobileService.findQueueThatHasTransaction(codeQR.getText(), qid, Integer.parseInt(token.getText()));
            if (null == jsonPurchaseOrder) {
                LOG.warn("No order found for codeQR={} qid={} token={}", codeQR, qid, token);
                getErrorReason("Could not find any order associated", PURCHASE_ORDER_NOT_FOUND);
            }

            return jsonPurchaseOrder.asJson();
        } catch (Exception e) {
            LOG.error("Failed aborting queue qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            apiHealthService.insert(
                "/purchaseOrder/{token}/{codeQR}",
                "purchaseOrder",
                TokenQueueAPIController.class.getName(),
                Duration.between(start, Instant.now()),
                HealthStatusEnum.F);
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/purchaseOrder/{token}/{codeQR}",
                "purchaseOrder",
                TokenQueueAPIController.class.getName(),
                Duration.between(start, Instant.now()),
                HealthStatusEnum.G);
        }
    }

    /** Abort the queue. App should un-subscribe user from topic. */
    @PostMapping (
        value = "/abort/{codeQR}",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String abortQueue(
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
        Instant start = Instant.now();
        LOG.info("Abort queue did={} dt={} codeQR={}", did, deviceType, codeQR);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        if (!tokenQueueMobileService.getBizService().isValidCodeQR(codeQR.getText())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid token");
            return null;
        }

        try {
            return tokenQueueMobileService.abortQueue(codeQR.getText(), did.getText(), qid).asJson();
        } catch (PurchaseOrderRefundExternalException e) {
            LOG.warn("Failed cancelling purchase order reason={}", e.getLocalizedMessage(), e);
            return getErrorReason(
                "Payment is performed outside of NoQueue. Go to merchant for cancellation.",
                PURCHASE_ORDER_FAILED_TO_CANCEL_AS_EXTERNALLY_PAID);
        } catch (PurchaseOrderRefundPartialException e) {
            LOG.warn("Failed cancelling purchase order reason={}", e.getLocalizedMessage(), e);
            return getErrorReason(
                "Cannot cancel cash payment. Go to merchant for cancellation. Cash payment will be performed by merchant.",
                PURCHASE_ORDER_FAILED_TO_CANCEL_PARTIAL_PAY);
        } catch(PurchaseOrderCancelException e) {
            LOG.warn("Failed cancelling purchase order reason={}", e.getLocalizedMessage(), e);
            return getErrorReason("Failed to cancel order", PURCHASE_ORDER_FAILED_TO_CANCEL);
        } catch (Exception e) {
            LOG.error("Failed aborting queue qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            apiHealthService.insert(
                "/abort/{codeQR}",
                "abortQueue",
                TokenQueueAPIController.class.getName(),
                Duration.between(start, Instant.now()),
                HealthStatusEnum.F);
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/abort/{codeQR}",
                "abortQueue",
                TokenQueueAPIController.class.getName(),
                Duration.between(start, Instant.now()),
                HealthStatusEnum.G);
        }
    }

    public static boolean authorizeRequest(HttpServletResponse response, String qid) throws IOException {
        if (null == qid) {
            LOG.warn("Login required qid is blank");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return true;
        }
        return false;
    }

    public static boolean authorizeRequest(HttpServletResponse response, String qid, String mail, String did, String api) throws IOException {
        if (null == qid) {
            LOG.warn("Un-authorized access to {} by {} {}", api, mail, did);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return true;
        }
        return false;
    }
}
