package com.noqapp.mobile.view.controller.api.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BizNameEntity;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.StoreProductEntity;
import com.noqapp.domain.UserAccountEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.json.JsonPurchaseOrder;
import com.noqapp.domain.json.JsonPurchaseOrderProduct;
import com.noqapp.domain.types.DeliveryTypeEnum;
import com.noqapp.domain.types.PaymentTypeEnum;
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
                jsonPurchaseOrder.asJson(),
                httpServletResponse
        );

        JsonPurchaseOrder jsonPurchaseOrderResponse = new ObjectMapper().readValue(jsonPurchaseOrderAsString, JsonPurchaseOrder.class);
        assertEquals(1, jsonPurchaseOrderResponse.getToken());
        assertEquals(900, jsonPurchaseOrderResponse.getOrderPrice());
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
            orderPrice = orderPrice + (pop.getProductPrice() - (pop.getProductPrice() * pop.getProductDiscount()/100)) * pop.getProductQuantity();
            jsonPurchaseOrderProducts.add(pop);
        }
        JsonPurchaseOrder jsonPurchaseOrder = new JsonPurchaseOrder()
                .setPurchaseOrderProducts(jsonPurchaseOrderProducts)
                .setBizStoreId(bizStore.getId())
                .setBusinessType(bizStore.getBusinessType())
                .setCustomerName(userProfile.getName())
                .setCustomerPhone(userProfile.getPhone())
                .setDeliveryType(DeliveryTypeEnum.TO)
                .setPaymentType(PaymentTypeEnum.CA)
                .setStoreDiscount(bizStore.getDiscount())
                .setOrderPrice(String.valueOf(orderPrice));

        return jsonPurchaseOrder;
    }
}