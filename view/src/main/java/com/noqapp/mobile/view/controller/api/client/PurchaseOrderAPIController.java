package com.noqapp.mobile.view.controller.api.client;

import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.COUPON_NOT_APPLICABLE;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.PURCHASE_ORDER_ALREADY_CANCELLED;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.PURCHASE_ORDER_CANNOT_ACTIVATE;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.PURCHASE_ORDER_FAILED_TO_CANCEL;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.PURCHASE_ORDER_FAILED_TO_CANCEL_AS_EXTERNALLY_PAID;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.PURCHASE_ORDER_FAILED_TO_CANCEL_PARTIAL_PAY;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.PURCHASE_ORDER_NEGATIVE;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.PURCHASE_ORDER_NOT_FOUND;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.PURCHASE_ORDER_PRICE_MISMATCH;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.QUEUE_ORDER_ABORT_EXPIRED_LIMITED_TIME;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.STORE_DAY_CLOSED;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.STORE_OFFLINE;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.STORE_PREVENT_JOIN;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.STORE_TEMP_DAY_CLOSED;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.TRANSACTION_GATEWAY_DEFAULT;
import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.common.utils.CommonUtil.UNAUTHORIZED;
import static com.noqapp.mobile.view.controller.api.client.TokenQueueAPIController.authorizeRequest;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

import com.noqapp.common.errors.ErrorEncounteredJson;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.CouponEntity;
import com.noqapp.domain.PurchaseOrderEntity;
import com.noqapp.domain.TokenQueueEntity;
import com.noqapp.domain.common.DomainCommonUtil;
import com.noqapp.domain.json.JsonPurchaseOrder;
import com.noqapp.domain.json.JsonPurchaseOrderHistorical;
import com.noqapp.domain.json.JsonResponse;
import com.noqapp.domain.json.payment.cashfree.JsonCashfreeNotification;
import com.noqapp.domain.types.PaymentModeEnum;
import com.noqapp.domain.types.PaymentStatusEnum;
import com.noqapp.domain.types.PurchaseOrderStateEnum;
import com.noqapp.domain.types.TokenServiceEnum;
import com.noqapp.domain.types.cashfree.TxStatusEnum;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.domain.body.client.OrderDetail;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.service.BizService;
import com.noqapp.service.CouponService;
import com.noqapp.service.PurchaseOrderService;
import com.noqapp.service.TokenQueueService;
import com.noqapp.service.exceptions.OrderFailedReActivationException;
import com.noqapp.service.exceptions.PriceMismatchException;
import com.noqapp.service.exceptions.PurchaseOrderCancelException;
import com.noqapp.service.exceptions.PurchaseOrderNegativeException;
import com.noqapp.service.exceptions.PurchaseOrderRefundExternalException;
import com.noqapp.service.exceptions.PurchaseOrderRefundPartialException;
import com.noqapp.service.exceptions.QueueAbortPaidPastDurationException;
import com.noqapp.service.exceptions.StoreDayClosedException;
import com.noqapp.service.exceptions.StoreInActiveException;
import com.noqapp.service.exceptions.StorePreventJoiningException;
import com.noqapp.service.exceptions.StoreTempDayClosedException;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import javax.servlet.http.HttpServletResponse;

/**
 * hitender
 * 3/30/18 2:05 PM
 */
@SuppressWarnings({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/api/c/purchaseOrder")
public class PurchaseOrderAPIController {
    private static final Logger LOG = LoggerFactory.getLogger(PurchaseOrderAPIController.class);

    private PurchaseOrderService purchaseOrderService;
    private CouponService couponService;
    private BizService bizService;
    private ApiHealthService apiHealthService;
    private AuthenticateMobileService authenticateMobileService;

    @Autowired
    public PurchaseOrderAPIController(
        PurchaseOrderService purchaseOrderService,
        CouponService couponService,
        BizService bizService,
        ApiHealthService apiHealthService,
        AuthenticateMobileService authenticateMobileService
    ) {
        this.purchaseOrderService = purchaseOrderService;
        this.bizService = bizService;
        this.couponService = couponService;
        this.apiHealthService = apiHealthService;
        this.authenticateMobileService = authenticateMobileService;
    }

    /** Add purchase when user presses confirm. */
    @PostMapping(
        value = "/purchase",
        produces = MediaType.APPLICATION_JSON_VALUE
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
        LOG.info("Purchase Order API for did={} dt={}", did, dt);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        try {
            //TODO simplify me using coupon manage to validate again db
            if (StringUtils.isNotBlank(jsonPurchaseOrder.getCouponId())) {
                CouponEntity coupon = couponService.findById(jsonPurchaseOrder.getCouponId());
                if (null == coupon) {
                    LOG.warn("Failed apply coupon {} {} ", jsonPurchaseOrder.getQueueUserId(), jsonPurchaseOrder.getCodeQR());
                    return getErrorReason("Cannot apply coupon", COUPON_NOT_APPLICABLE);
                } else if (!coupon.isActive()) {
                    LOG.warn("Failed apply coupon {} {} {} ", coupon.getId(), jsonPurchaseOrder.getQueueUserId(), jsonPurchaseOrder.getCodeQR());
                    return getErrorReason("Cannot apply coupon", COUPON_NOT_APPLICABLE);
                }

                BizStoreEntity bizStore = bizService.findByCodeQR(jsonPurchaseOrder.getCodeQR());
                if (null != bizStore && !bizStore.getBizName().getId().equalsIgnoreCase(coupon.getBizNameId())) {
                    LOG.warn("Failed apply coupon {} {} {} ", coupon.getId(), jsonPurchaseOrder.getQueueUserId(), jsonPurchaseOrder.getCodeQR());
                    return getErrorReason("Cannot apply coupon", COUPON_NOT_APPLICABLE);
                }
            }

            purchaseOrderService.createOrderWithCFToken(jsonPurchaseOrder, qid, did.getText(), TokenServiceEnum.C);
            LOG.info("Order Placed Successfully={}", jsonPurchaseOrder.getPresentOrderState());
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
        } catch (PurchaseOrderNegativeException e) {
            LOG.error("Order cannot be less than 1 reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return ErrorEncounteredJson.toJson("Minimum order price should be a positive amount", PURCHASE_ORDER_NEGATIVE);
        } catch (Exception e) {
            LOG.error("Failed processing purchase order reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return jsonPurchaseOrder.asJson();
        } finally {
            apiHealthService.insert(
                "/purchase",
                "purchase",
                PurchaseOrderAPIController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Cancel placed order. */
    @PostMapping(
        value = "/cancel",
        produces = MediaType.APPLICATION_JSON_VALUE
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
        JsonPurchaseOrder jsonPurchaseOrder,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Cancel Order API for did={} dt={}", did, dt);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        try {
            if (purchaseOrderService.isOrderCancelled(qid, jsonPurchaseOrder.getTransactionId())) {
                return getErrorReason("Order already cancelled", PURCHASE_ORDER_ALREADY_CANCELLED);
            }

            JsonPurchaseOrder jsonPurchaseOrderResponse = purchaseOrderService.cancelOrderByClient(qid, jsonPurchaseOrder.getTransactionId());
            LOG.info("Order Cancelled Successfully={}", jsonPurchaseOrderResponse.getPresentOrderState());
            return jsonPurchaseOrderResponse.asJson();
        } catch(PurchaseOrderRefundExternalException e) {
            LOG.warn("Failed cancelling purchase order reason={}", e.getLocalizedMessage());
            return getErrorReason(
                "Payment is performed outside of NoQueue. Go to merchant for cancellation.",
                PURCHASE_ORDER_FAILED_TO_CANCEL_AS_EXTERNALLY_PAID);
        } catch(PurchaseOrderRefundPartialException e) {
            LOG.warn("Failed cancelling partial purchase order reason={}", e.getLocalizedMessage());
            return getErrorReason(
                "Cannot cancel as partial payment is done via cash. Go to merchant for cancellation. Cash payment will be performed by merchant.",
                PURCHASE_ORDER_FAILED_TO_CANCEL_PARTIAL_PAY);
        } catch(PurchaseOrderCancelException e) {
            LOG.warn("Failed cancelling purchase order reason={}", e.getLocalizedMessage());
            return getErrorReason("Failed to cancel order", PURCHASE_ORDER_FAILED_TO_CANCEL);
        } catch (QueueAbortPaidPastDurationException e) {
            LOG.warn("Failed cancelling as the duration of cancellation has passed reason={}", e.getLocalizedMessage());
            return getErrorReason(
                "Cannot cancel as the duration of cancellation has passed. Please contact the business.",
                QUEUE_ORDER_ABORT_EXPIRED_LIMITED_TIME);
        } catch (Exception e) {
            LOG.error("Failed cancelling purchase order reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Failed to cancel order", PURCHASE_ORDER_FAILED_TO_CANCEL);
        } finally {
            apiHealthService.insert(
                "/cancel",
                "cancel",
                PurchaseOrderAPIController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Activate old placed order that is still in a valid state. */
    @PostMapping(
        value = "/activate",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String activate(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput dt,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        JsonPurchaseOrderHistorical jsonPurchaseOrderHistorical,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Activate Old Order API for did={} dt={}", did, dt);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        try {
            JsonPurchaseOrder jsonPurchaseOrderResponse = purchaseOrderService.activateOrderByClient(
                jsonPurchaseOrderHistorical.getQueueUserId(),
                jsonPurchaseOrderHistorical.getTransactionId(),
                did.getText());

            LOG.info("Order activated Successfully={}", jsonPurchaseOrderResponse.getPresentOrderState());
            return jsonPurchaseOrderResponse.asJson();
        } catch (OrderFailedReActivationException e) {
            LOG.error("Failed activating purchase order reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason(e.getLocalizedMessage(), PURCHASE_ORDER_CANNOT_ACTIVATE);
        } catch (Exception e) {
            LOG.error("Failed activating purchase order reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/activate",
                "activate",
                PurchaseOrderAPIController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Get order detail. */
    @PostMapping(
        value = "/orderDetail",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String orderDetail(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput dt,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        OrderDetail orderDetail,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Order detail request from mail={} auth={}", mail, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/c/purchaseOrder/orderDetail by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            JsonPurchaseOrder jsonPurchaseOrder = purchaseOrderService.findBy(qid, orderDetail.getCodeQR(), orderDetail.getToken());
            if (null == jsonPurchaseOrder) {
                LOG.error("No such order found {} codeQR={} token={}", qid, orderDetail.getCodeQR(), orderDetail.getToken());
                return getErrorReason("No such order found", PURCHASE_ORDER_NOT_FOUND);
            }

            return jsonPurchaseOrder.asJson();
        } catch (Exception e) {
            LOG.error("Failed getting order detail reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/orderDetail",
                "orderDetail",
                PurchaseOrderAPIController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Generate Token to initiate transaction. */
    @PostMapping(
        value = "/payNow",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String payNow(
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
        LOG.info("Populate payment gateway notification request from mail={} auth={}", mail, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/c/purchaseOrder/payNow by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        PurchaseOrderEntity purchaseOrder = purchaseOrderService.findByTransactionId(jsonPurchaseOrder.getTransactionId());
        if (null == purchaseOrder) {
            return getErrorReason("Order not found", PURCHASE_ORDER_NOT_FOUND);
        }

        try {
            LOG.info("Order qid={} codeQR={} transactionId={} amount={} transactionVia={} businessType={} orderId={} orderState={}",
                qid,
                purchaseOrder.getCodeQR(),
                purchaseOrder.getTransactionId(),
                purchaseOrder.getOrderPrice(),
                purchaseOrder.getTransactionVia(),
                purchaseOrder.getBusinessType(),
                purchaseOrder.getId(),
                purchaseOrder.getPresentOrderState());
            JsonPurchaseOrder jsonPurchaseOrderPopulated = new JsonPurchaseOrder(purchaseOrder);
            purchaseOrderService.populateWithCFToken(jsonPurchaseOrderPopulated, purchaseOrder);
            return jsonPurchaseOrderPopulated.asJson();
        } catch (Exception e) {
            LOG.error("Failed populating with cashfree token reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/payNow",
                "payNow",
                PurchaseOrderAPIController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** When customer decides to not pay, the order is deleted and there is no reference. */
    @PostMapping(
        value = "/cancelPayBeforeOrder",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String cancelPayBeforeOrder(
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
        LOG.info("No payment made and hence removing the reference to order mail={} auth={}", mail, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/c/purchaseOrder/cancelPayBeforeOrder by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            if (StringUtils.isBlank(jsonPurchaseOrder.getTransactionId())) {
                return getErrorReason("Order not found", PURCHASE_ORDER_NOT_FOUND);
            }

            purchaseOrderService.cancelOrderWhenBackedAwayFromGatewayForOrder(jsonPurchaseOrder.getTransactionId());
            return new JsonResponse(true).asJson();
        } catch (Exception e) {
            LOG.error("Failed updating with cashfree notification qid={} reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/cancelPayBeforeOrder",
                "cancelPayBeforeOrder",
                PurchaseOrderAPIController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Cashfree transaction response sent to server. Based on cashfree, server updates order status */
    @PostMapping(
        value = "/cf/notify",
        produces = MediaType.APPLICATION_JSON_VALUE
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
            LOG.warn("Un-authorized access to /api/c/purchaseOrder/cf/notify by mail={}", mail);
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

            PaymentModeEnum paymentMode = DomainCommonUtil.derivePaymentMode(jsonCashfreeNotification.getPaymentMode());
            JsonPurchaseOrder jsonPurchaseOrder = purchaseOrderService.updateOnPaymentGatewayNotificationAsJsonPurchaseOrder(
                transactionId,
                jsonCashfreeNotification.getTxMsg(),
                jsonCashfreeNotification.getReferenceId(),
                paymentStatus,
                purchaseOrderState,
                paymentMode
            );
            LOG.info("Order updated qid={} codeQR={} transactionId={} amount={} transactionVia={} businessType={} orderState={}",
                qid,
                jsonPurchaseOrder.getCodeQR(),
                jsonPurchaseOrder.getTransactionId(),
                jsonPurchaseOrder.getOrderPrice(),
                jsonPurchaseOrder.getTransactionVia(),
                jsonPurchaseOrder.getBusinessType(),
                jsonPurchaseOrder.getPresentOrderState());
            return jsonPurchaseOrder.asJson();
        } catch (Exception e) {
            LOG.error("Failed updating with cashfree notification qid={} reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/cf/notify",
                "cashfreeNotify",
                PurchaseOrderAPIController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Cashfree transaction response sent to server. Based on cashfree, server updates order status */
    @PostMapping(
        value = "/payCash",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String payCash(
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
        LOG.info("Pay as cash request from mail={} auth={}", mail, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/c/purchaseOrder/payCash by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            if (StringUtils.isBlank(jsonPurchaseOrder.getTransactionId())) {
                return getErrorReason("Order not found", PURCHASE_ORDER_NOT_FOUND);
            }

            return purchaseOrderService.updateOnCashPayment(
                jsonPurchaseOrder.getTransactionId(),
                "Cash Pay Client",
                PaymentStatusEnum.PP,
                PurchaseOrderStateEnum.PO,
                PaymentModeEnum.CA
            ).asJson();
        } catch (Exception e) {
            LOG.error("Failed updating when client paying cash reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/payCash",
                "payCash",
                PurchaseOrderAPIController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }

    }
}
