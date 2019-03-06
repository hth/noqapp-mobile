package com.noqapp.mobile.view.controller.api.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BizNameEntity;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.StoreProductEntity;
import com.noqapp.domain.UserAccountEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.json.JsonPurchaseOrder;
import com.noqapp.domain.json.JsonPurchaseOrderProduct;
import com.noqapp.domain.json.payment.cashfree.JsonCashfreeNotification;
import com.noqapp.domain.json.payment.cashfree.JsonResponseRefund;
import com.noqapp.domain.json.payment.cashfree.JsonResponseWithCFToken;
import com.noqapp.domain.types.DeliveryModeEnum;
import com.noqapp.domain.types.PaymentModeEnum;
import com.noqapp.domain.types.PurchaseOrderStateEnum;
import com.noqapp.domain.types.cashfree.PaymentModeCFEnum;
import com.noqapp.domain.types.cashfree.TxStatusEnum;
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
 * Date: 10/6/18 9:38 PM
 */
@DisplayName("Purchase Order API")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("api")
class PurchaseOrderAPIControllerITest extends ITest {

    private PurchaseOrderAPIController purchaseOrderAPIController;

    private UserProfileEntity userProfile;
    private UserAccountEntity userAccount;

    @BeforeEach
    void setUp() {
        purchaseOrderAPIController = new PurchaseOrderAPIController(
                purchaseOrderService,
                apiHealthService,
                authenticateMobileService
        );

        userProfile = userProfileManager.findOneByPhone("9118000000001");
        userAccount = userAccountManager.findByQueueUserId(userProfile.getQueueUserId());
    }

    @Test
    void purchase() throws IOException {
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
        assertEquals("990", jsonPurchaseOrderResponse.getOrderPrice());
        assertEquals(PurchaseOrderStateEnum.VB, jsonPurchaseOrderResponse.getPresentOrderState());
    }

    @Test
    void cancel() throws IOException {
        JsonPurchaseOrder jsonPurchaseOrder = createOrder();
        JsonResponseWithCFToken jsonResponseWithCFToken = new JsonResponseWithCFToken()
            .setStatus("Successful")
            .setCftoken("XXXX");
        when(cashfreeService.createTokenForPurchaseOrder(any())).thenReturn(jsonResponseWithCFToken);

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
            .setTxTime(null)
            .setTxMsg("Success")
            .setReferenceId("XXX-XXXX")
            .setPaymentMode(PaymentModeCFEnum.CREDIT_CARD.getName())
            .setSignature("XXXXX")
            .setOrderAmount(jsonPurchaseOrderResponse.getOrderPrice())
            .setTxStatus(TxStatusEnum.SUCCESS.getName())
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
        when(cashfreeService.refundInitiatedByClient(any())).thenReturn(new JsonResponseRefund().setStatus("OK"));
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
