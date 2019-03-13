package com.noqapp.mobile.view.controller.api.client;

import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.common.utils.CommonUtil.UNAUTHORIZED;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.PURCHASE_ORDER_ALREADY_CANCELLED;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.PURCHASE_ORDER_CANNOT_ACTIVATE;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.PURCHASE_ORDER_FAILED_TO_CANCEL;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.PURCHASE_ORDER_FAILED_TO_CANCEL_PARTIAL_PAY;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.PURCHASE_ORDER_NOT_FOUND;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.PURCHASE_ORDER_PRICE_MISMATCH;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.STORE_DAY_CLOSED;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.STORE_OFFLINE;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.STORE_PREVENT_JOIN;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.STORE_TEMP_DAY_CLOSED;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.TRANSACTION_GATEWAY_DEFAULT;
import static com.noqapp.mobile.view.controller.api.client.TokenQueueAPIController.authorizeRequest;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.PurchaseOrderEntity;
import com.noqapp.domain.json.JsonPurchaseOrder;
import com.noqapp.domain.json.JsonPurchaseOrderHistorical;
import com.noqapp.domain.json.payment.cashfree.JsonCashfreeNotification;
import com.noqapp.domain.types.PaymentModeEnum;
import com.noqapp.domain.types.PaymentStatusEnum;
import com.noqapp.domain.types.PurchaseOrderStateEnum;
import com.noqapp.domain.types.TokenServiceEnum;
import com.noqapp.domain.types.cashfree.PaymentModeCFEnum;
import com.noqapp.domain.types.cashfree.TxStatusEnum;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.common.util.ErrorEncounteredJson;
import com.noqapp.mobile.domain.body.client.OrderDetail;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.service.PurchaseOrderService;
import com.noqapp.service.exceptions.OrderFailedReActivationException;
import com.noqapp.service.exceptions.PriceMismatchException;
import com.noqapp.service.exceptions.PurchaseOrderPartialException;
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
@SuppressWarnings ({
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
    private ApiHealthService apiHealthService;
    private AuthenticateMobileService authenticateMobileService;

    @Autowired
    public PurchaseOrderAPIController(
        PurchaseOrderService purchaseOrderService,
        ApiHealthService apiHealthService,
        AuthenticateMobileService authenticateMobileService
    ) {
        this.purchaseOrderService = purchaseOrderService;
        this.apiHealthService = apiHealthService;
        this.authenticateMobileService = authenticateMobileService;
    }

    /** Add purchase when user presses confirm. */
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
        LOG.info("Purchase Order API for did={} dt={}", did, dt);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        try {
            purchaseOrderService.createOrder(jsonPurchaseOrder, qid, did.getText(), TokenServiceEnum.C);
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
        } catch(PurchaseOrderPartialException e) {
            LOG.warn("Failed cancelling purchase order reason={}", e.getLocalizedMessage(), e);
            return getErrorReason(
                "Cannot cancel as partial payment is done via cash. Go to merchant for cancellation. Cash payment will be performed by merchant.",
                PURCHASE_ORDER_FAILED_TO_CANCEL_PARTIAL_PAY);
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

    /** Activate old placed order that is still in a valids state. */
    @PostMapping(
        value = "/activate",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
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
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
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
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
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
        LOG.info("Cashfree notification request from mail={} auth={}", mail, AUTH_KEY_HIDDEN);
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

            PaymentModeEnum paymentMode;
            switch (PaymentModeCFEnum.valueOf(jsonCashfreeNotification.getPaymentMode())) {
                case DEBIT_CARD:
                    paymentMode = PaymentModeEnum.DC;
                    break;
                case CREDIT_CARD:
                    paymentMode = PaymentModeEnum.CC;
                    break;
                case CREDIT_CARD_EMI:
                    paymentMode = PaymentModeEnum.CCE;
                    break;
                case NET_BANKING:
                    paymentMode = PaymentModeEnum.NTB;
                    break;
                case UPI:
                    paymentMode = PaymentModeEnum.UPI;
                    break;
                case Paypal:
                    paymentMode = PaymentModeEnum.PAL;
                    break;
                case PhonePe:
                    paymentMode = PaymentModeEnum.PPE;
                    break;
                case Paytm:
                    paymentMode = PaymentModeEnum.PTM;
                    break;
                case AmazonPay:
                    paymentMode = PaymentModeEnum.AMZ;
                    break;
                case AIRTEL_MONEY:
                    paymentMode = PaymentModeEnum.AIR;
                    break;
                case FreeCharge:
                    paymentMode = PaymentModeEnum.FCH;
                    break;
                case MobiKwik:
                    paymentMode = PaymentModeEnum.MKK;
                    break;
                case OLA:
                    paymentMode = PaymentModeEnum.OLA;
                    break;
                case JioMoney:
                    paymentMode = PaymentModeEnum.JIO;
                    break;
                case ZestMoney:
                    paymentMode = PaymentModeEnum.ZST;
                    break;
                case Instacred:
                    paymentMode = PaymentModeEnum.INS;
                    break;
                case LazyPay:
                    paymentMode = PaymentModeEnum.LPY;
                    break;
                default:
                    LOG.error("Unknown field {}", jsonCashfreeNotification.getPaymentMode());
                    throw new UnsupportedOperationException("Reached unsupported payment mode");
            }
            PurchaseOrderEntity purchaseOrder = purchaseOrderService.updateOnPaymentGatewayNotification(
                transactionId,
                jsonCashfreeNotification.getTxMsg(),
                jsonCashfreeNotification.getReferenceId(),
                paymentStatus,
                purchaseOrderState,
                paymentMode
            );
            return new JsonPurchaseOrder(purchaseOrder).asJson();
        } catch (Exception e) {
            LOG.error("Failed updating with cashfree notification reason={}", e.getLocalizedMessage(), e);
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
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
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

            PurchaseOrderEntity purchaseOrder = purchaseOrderService.updateOnCashPayment(
                jsonPurchaseOrder.getTransactionId(),
                "Cash Pay Client",
                PaymentStatusEnum.PP,
                PurchaseOrderStateEnum.PO,
                PaymentModeEnum.CA
            );
            return new JsonPurchaseOrder(purchaseOrder).asJson();
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
