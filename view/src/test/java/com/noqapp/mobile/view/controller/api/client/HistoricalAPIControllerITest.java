package com.noqapp.mobile.view.controller.api.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BizNameEntity;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.UserAccountEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.json.JsonPurchaseOrderHistoricalList;
import com.noqapp.domain.json.JsonQueueHistoricalList;
import com.noqapp.domain.json.JsonResponse;
import com.noqapp.domain.json.JsonToken;
import com.noqapp.mobile.domain.body.client.JoinQueue;
import com.noqapp.mobile.view.ITest;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
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

        userProfile = userProfileManager.findOneByPhone("9118000000001");
        userAccount = userAccountManager.findByQueueUserId(userProfile.getQueueUserId());
    }

    @Test
    void orders() throws IOException {
        String orders = historicalAPIController.orders(
                new ScrubbedInput(userProfile.getEmail()),
                new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
                httpServletResponse
        );

        JsonPurchaseOrderHistoricalList jsonPurchaseOrderHistoricalList = new ObjectMapper().readValue(orders, JsonPurchaseOrderHistoricalList.class);
        assertEquals(0, jsonPurchaseOrderHistoricalList.getJsonPurchaseOrderHistoricals().size());
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
}