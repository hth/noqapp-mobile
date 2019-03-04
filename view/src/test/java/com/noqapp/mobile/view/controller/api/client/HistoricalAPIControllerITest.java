package com.noqapp.mobile.view.controller.api.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BizNameEntity;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.StoreProductEntity;
import com.noqapp.domain.UserAccountEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.json.JsonPurchaseOrder;
import com.noqapp.domain.json.JsonPurchaseOrderHistoricalList;
import com.noqapp.domain.json.JsonPurchaseOrderProduct;
import com.noqapp.domain.json.JsonQueueHistoricalList;
import com.noqapp.domain.json.JsonResponse;
import com.noqapp.domain.json.JsonToken;
import com.noqapp.domain.json.payment.cashfree.JsonCashfreeNotification;
import com.noqapp.domain.types.DeliveryModeEnum;
import com.noqapp.domain.types.PaymentModeEnum;
import com.noqapp.domain.types.PurchaseOrderStateEnum;
import com.noqapp.mobile.domain.body.client.JoinQueue;
import com.noqapp.mobile.view.ITest;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * User: hitender
 * Date: 10/6/18 8:36 PM
 */
@DisplayName("Historical API")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("api")
class HistoricalAPIControllerITest extends ITest {

    private HistoricalAPIController historicalAPIController;
    private TokenQueueAPIController tokenQueueAPIController;
    private PurchaseOrderAPIController purchaseOrderAPIController;

    private UserProfileEntity userProfile;
    private UserAccountEntity userAccount;

    @BeforeEach
    void setUp() {
        historicalAPIController = new HistoricalAPIController(
                authenticateMobileService,
                purchaseOrderService,
                queueMobileService,
                apiHealthService
        );

        tokenQueueAPIController = new TokenQueueAPIController(
                tokenQueueMobileService,
                queueMobileService,
                authenticateMobileService,
                purchaseOrderService,
                apiHealthService
        );

        purchaseOrderAPIController = new PurchaseOrderAPIController(
                purchaseOrderService,
                apiHealthService,
                authenticateMobileService
        );

        userProfile = userProfileManager.findOneByPhone("9118000000001");
        userAccount = userAccountManager.findByQueueUserId(userProfile.getQueueUserId());
    }

    @Test
    void orders() throws IOException {
        JsonPurchaseOrder jsonPurchaseOrder = createOrder();
        String jsonPurchaseOrderAsString = purchaseOrderAPIController.purchase(
                new ScrubbedInput(did),
                new ScrubbedInput(deviceType),
                new ScrubbedInput(userProfile.getEmail()),
                new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
                jsonPurchaseOrder,
                httpServletResponse
        );
        JsonPurchaseOrder jsonPurchaseOrderResponse = new ObjectMapper().readValue(jsonPurchaseOrderAsString, JsonPurchaseOrder.class);

        JsonCashfreeNotification jsonCashfreeNotification = new JsonCashfreeNotification()
            .setxTime(null)
            .setTxMsg("Success")
            .setReferenceId("XXX-XXXX")
            .setPaymentMode("CREDIT_CARD")
            .setSignature("XXXXX")
            .setOrderAmount(jsonPurchaseOrderResponse.getOrderPrice())
            .setTxStatus("SUCCESS")
            .setOrderId(jsonPurchaseOrderResponse.getTransactionId());

        String jsonPurchaseOrderAsStringAfterNotifyingCF = purchaseOrderAPIController.cashfreeNotify(
            new ScrubbedInput(did),
            new ScrubbedInput(deviceType),
            new ScrubbedInput(userProfile.getEmail()),
            new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
            jsonCashfreeNotification,
            httpServletResponse
        );

        JsonPurchaseOrder jsonPurchaseOrderCFResponse = new ObjectMapper().readValue(jsonPurchaseOrderAsStringAfterNotifyingCF, JsonPurchaseOrder.class);
        String jsonPurchaseOrderCancelAsString = purchaseOrderAPIController.cancel(
                new ScrubbedInput(did),
                new ScrubbedInput(deviceType),
                new ScrubbedInput(userProfile.getEmail()),
                new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
                jsonPurchaseOrderResponse,
                httpServletResponse
        );

        JsonPurchaseOrder jsonPurchaseOrderCancelResponse = new ObjectMapper().readValue(jsonPurchaseOrderCancelAsString, JsonPurchaseOrder.class);
        assertEquals("990", jsonPurchaseOrderCancelResponse.getOrderPrice());
        assertEquals(PurchaseOrderStateEnum.CO, jsonPurchaseOrderCancelResponse.getPresentOrderState());

        String orders = historicalAPIController.orders(
                new ScrubbedInput(userProfile.getEmail()),
                new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
                httpServletResponse
        );

        JsonPurchaseOrderHistoricalList jsonPurchaseOrderHistoricalList = new ObjectMapper().readValue(orders, JsonPurchaseOrderHistoricalList.class);
        assertEquals(1, jsonPurchaseOrderHistoricalList.getJsonPurchaseOrderHistoricals().size());
    }

    @Test
    void queues() throws IOException {
        JsonToken jsonToken = joinQueue();
        abortQueue(jsonToken);

        String queues = historicalAPIController.queues(
                new ScrubbedInput(userProfile.getEmail()),
                new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
                httpServletResponse
        );

        JsonQueueHistoricalList jsonQueueHistoricalList = new ObjectMapper().readValue(queues, JsonQueueHistoricalList.class);
        assertEquals(1, jsonQueueHistoricalList.getQueueHistoricals().size());
    }

    private void abortQueue(JsonToken jsonToken) throws IOException {
        String abortResponse = tokenQueueAPIController.abortQueue(
                new ScrubbedInput(did),
                new ScrubbedInput(deviceType),
                new ScrubbedInput(userProfile.getEmail()),
                new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
                new ScrubbedInput(jsonToken.getCodeQR()),
                httpServletResponse
        );
        JsonResponse jsonResponse = new ObjectMapper().readValue(abortResponse, JsonResponse.class);
        assertEquals(1, jsonResponse.getResponse());
    }

    private JsonToken joinQueue() throws IOException {
        BizNameEntity bizName = bizNameManager.findByPhone("9118000000000");
        List<BizStoreEntity> bizStores = bizStoreManager.getAllBizStores(bizName.getId());
        JoinQueue joinQueue = new JoinQueue()
                .setCodeQR(bizStores.get(0).getCodeQR())
                .setQueueUserId(userProfile.getQueueUserId());

        String jsonTokenAsString = tokenQueueAPIController.joinQueue(
                new ScrubbedInput(did),
                new ScrubbedInput(deviceType),
                new ScrubbedInput(userProfile.getEmail()),
                new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
                joinQueue,
                httpServletResponse
        );
        return new ObjectMapper().readValue(jsonTokenAsString, JsonToken.class);
    }

    private JsonPurchaseOrder createOrder() {
        BizNameEntity bizName = bizNameManager.findByPhone("9118000000021");
        List<BizStoreEntity> bizStores = bizStoreManager.getAllBizStores(bizName.getId());
        BizStoreEntity bizStore = bizStores.get(0);
        List<StoreProductEntity> storeProducts = storeProductService.findAll(bizStore.getId());

        int orderPrice = 0;
        List<JsonPurchaseOrderProduct> jsonPurchaseOrderProducts = new ArrayList<>();
        for (StoreProductEntity storeProduct : storeProducts) {
            JsonPurchaseOrderProduct pop = new JsonPurchaseOrderProduct()
                    .setProductId(storeProduct.getId())
                    .setProductName(storeProduct.getProductName())
                    .setProductPrice(storeProduct.getProductPrice())
                    .setProductDiscount(storeProduct.getProductDiscount())
                    .setProductQuantity(1);


            orderPrice = computePrice(orderPrice, pop);
            jsonPurchaseOrderProducts.add(pop);
        }

        return new JsonPurchaseOrder()
                .setJsonPurchaseOrderProducts(jsonPurchaseOrderProducts)
                .setBizStoreId(bizStore.getId())
                .setBusinessType(bizStore.getBusinessType())
                .setCustomerName(userProfile.getName())
                .setCustomerPhone(userProfile.getPhone())
                .setDeliveryMode(DeliveryModeEnum.TO)
                .setPaymentMode(PaymentModeEnum.CA)
                .setStoreDiscount(bizStore.getDiscount())
                .setOrderPrice(String.valueOf(orderPrice));
    }

    private int computePrice(int orderPrice, JsonPurchaseOrderProduct pop) {
        return orderPrice + (pop.getProductPrice() - pop.getProductDiscount()) * pop.getProductQuantity();
    }
}
