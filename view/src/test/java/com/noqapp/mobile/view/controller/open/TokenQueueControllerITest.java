package com.noqapp.mobile.view.controller.open;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BizNameEntity;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.json.JsonQueue;
import com.noqapp.domain.json.JsonToken;
import com.noqapp.domain.json.JsonTokenAndQueue;
import com.noqapp.domain.json.JsonTokenAndQueueList;
import com.noqapp.domain.types.QueueStatusEnum;
import com.noqapp.mobile.domain.body.DeviceToken;
import com.noqapp.mobile.view.ITest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * hitender
 * 12/9/17 9:10 AM
 */
@DisplayName("TokenQueue API")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("api")
class TokenQueueControllerITest extends ITest {

    private TokenQueueController tokenQueueController;

    @BeforeEach
    void setUp() {
        this.tokenQueueController = new TokenQueueController(
                tokenQueueMobileService,
                queueMobileService,
                apiHealthService
        );
    }

    @Test
    @DisplayName("Token Queue API executes all APIs")
    void runThroughAllConditions() throws IOException {
        getQueueState();
        getAllJoinedQueues_Before_Join();
        getAllHistoricalJoinedQueues_Before_Join();
        joinQueue();
        getAllJoinedQueues_After_Joined();
        abortQueue();
        getAllJoinedQueues_After_Abort();
        getAllHistoricalJoinedQueues_After_Abort();
    }

    @DisplayName("Get Queue State")
    private void getQueueState() throws IOException {
        BizNameEntity bizName = bizService.findByPhone("9118000000000");
        BizStoreEntity bizStore = bizService.findOneBizStore(bizName.getId());

        String queueState = tokenQueueController.getQueueState(
                new ScrubbedInput(did),
                new ScrubbedInput(deviceType),
                new ScrubbedInput(bizStore.getCodeQR()),
                httpServletResponse
        );

        JsonQueue jsonQueue = new ObjectMapper().readValue(queueState, JsonQueue.class);
        assertEquals(QueueStatusEnum.S, jsonQueue.getQueueStatus());
    }

    @DisplayName("Get all joined queues before join")
    private void getAllJoinedQueues_Before_Join() throws IOException {
        String allJoinedQueues = tokenQueueController.getAllJoinedQueues(
                new ScrubbedInput(did),
                new ScrubbedInput(deviceType),
                httpServletResponse
        );
        JsonTokenAndQueueList jsonTokenAndQueueList = new ObjectMapper().readValue(allJoinedQueues, JsonTokenAndQueueList.class);
        assertFalse(jsonTokenAndQueueList.isSinceBeginning());
        assertEquals(0, jsonTokenAndQueueList.getTokenAndQueues().size());
    }

    @DisplayName("Get Historical Queues before join")
    private void getAllHistoricalJoinedQueues_Before_Join() throws IOException {
        String allJoinedQueues = tokenQueueController.getAllHistoricalJoinedQueues(
                new ScrubbedInput(did),
                new ScrubbedInput(deviceType),
                new DeviceToken(fcmToken).asJson(),
                httpServletResponse
        );
        JsonTokenAndQueueList jsonTokenAndQueueList = new ObjectMapper().readValue(allJoinedQueues, JsonTokenAndQueueList.class);
        assertFalse(jsonTokenAndQueueList.isSinceBeginning());
        assertEquals(0, jsonTokenAndQueueList.getTokenAndQueues().size());
    }

    @DisplayName("Join a Queue")
    private void joinQueue() throws IOException {
        BizNameEntity bizName = bizService.findByPhone("9118000000000");
        BizStoreEntity bizStore = bizService.findOneBizStore(bizName.getId());

        String afterJoin = tokenQueueController.joinQueue(
                new ScrubbedInput(did),
                new ScrubbedInput(deviceType),
                new ScrubbedInput(bizStore.getCodeQR()),
                httpServletResponse
        );
        JsonToken jsonToken = new ObjectMapper().readValue(afterJoin, JsonToken.class);
        assertEquals(QueueStatusEnum.S, jsonToken.getQueueStatus());
    }

    @DisplayName("Get all joined queues after join")
    private void getAllJoinedQueues_After_Joined() throws IOException {
        String allJoinedQueues = tokenQueueController.getAllJoinedQueues(
                new ScrubbedInput(did),
                new ScrubbedInput(deviceType),
                httpServletResponse
        );
        JsonTokenAndQueueList jsonTokenAndQueueList = new ObjectMapper().readValue(allJoinedQueues, JsonTokenAndQueueList.class);
        assertFalse(jsonTokenAndQueueList.isSinceBeginning());
        assertEquals(1, jsonTokenAndQueueList.getTokenAndQueues().size());
    }

    @DisplayName("Abort Queue")
    private void abortQueue() throws IOException {
        BizNameEntity bizName = bizService.findByPhone("9118000000000");
        BizStoreEntity bizStore = bizService.findOneBizStore(bizName.getId());

        tokenQueueController.abortQueue(
                new ScrubbedInput(did),
                new ScrubbedInput(deviceType),
                new ScrubbedInput(bizStore.getCodeQR()),
                httpServletResponse
        );
    }

    @DisplayName("Get all currently joined queues after join")
    private void getAllJoinedQueues_After_Abort() throws IOException {
        String allJoinedQueues = tokenQueueController.getAllJoinedQueues(
                new ScrubbedInput(did),
                new ScrubbedInput(deviceType),
                httpServletResponse
        );
        JsonTokenAndQueueList jsonTokenAndQueueList = new ObjectMapper().readValue(allJoinedQueues, JsonTokenAndQueueList.class);
        assertFalse(jsonTokenAndQueueList.isSinceBeginning());
        assertEquals(0, jsonTokenAndQueueList.getTokenAndQueues().size());
    }

    @DisplayName("Get Historical Queues any joined")
    private void getAllHistoricalJoinedQueues_After_Abort() throws IOException {
        /* For the first time fetches from beginning. */
        String allJoinedQueues = tokenQueueController.getAllHistoricalJoinedQueues(
                new ScrubbedInput(did),
                new ScrubbedInput(deviceType),
                new DeviceToken(fcmToken).asJson(),
                httpServletResponse
        );
        JsonTokenAndQueueList jsonTokenAndQueueList = new ObjectMapper().readValue(allJoinedQueues, JsonTokenAndQueueList.class);
        assertTrue(jsonTokenAndQueueList.isSinceBeginning());
        assertEquals(1, jsonTokenAndQueueList.getTokenAndQueues().size());
        JsonTokenAndQueue jsonTokenAndQueue = jsonTokenAndQueueList.getTokenAndQueues().iterator().next();
        assertEquals("Food", jsonTokenAndQueue.getDisplayName());
        assertEquals(1, jsonTokenAndQueue.getToken());
        assertEquals(0, jsonTokenAndQueue.getRatingCount());

        /* On second fetch, its not complete history, gets latest. */
        allJoinedQueues = tokenQueueController.getAllHistoricalJoinedQueues(
                new ScrubbedInput(did),
                new ScrubbedInput(deviceType),
                new DeviceToken(fcmToken).asJson(),
                httpServletResponse
        );
        jsonTokenAndQueueList = new ObjectMapper().readValue(allJoinedQueues, JsonTokenAndQueueList.class);
        assertFalse(jsonTokenAndQueueList.isSinceBeginning());
        assertEquals(1, jsonTokenAndQueueList.getTokenAndQueues().size());

        /* After changing device Id, it is assumed to be a new user. This person has no history. */
        allJoinedQueues = tokenQueueController.getAllHistoricalJoinedQueues(
                new ScrubbedInput(UUID.randomUUID().toString()),
                new ScrubbedInput(deviceType),
                new DeviceToken(fcmToken).asJson(),
                httpServletResponse
        );
        jsonTokenAndQueueList = new ObjectMapper().readValue(allJoinedQueues, JsonTokenAndQueueList.class);
        assertFalse(jsonTokenAndQueueList.isSinceBeginning());
        assertEquals(0, jsonTokenAndQueueList.getTokenAndQueues().size());
    }
}
