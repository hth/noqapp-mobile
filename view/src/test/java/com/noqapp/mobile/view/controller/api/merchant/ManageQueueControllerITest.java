package com.noqapp.mobile.view.controller.api.merchant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BizNameEntity;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.UserAccountEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.json.JsonQueuePersonList;
import com.noqapp.domain.json.JsonToken;
import com.noqapp.domain.json.JsonTopic;
import com.noqapp.domain.json.JsonTopicList;
import com.noqapp.domain.types.QueueStatusEnum;
import com.noqapp.domain.types.QueueUserStateEnum;
import com.noqapp.mobile.domain.JsonModifyQueue;
import com.noqapp.mobile.domain.body.merchant.Served;
import com.noqapp.mobile.view.ITest;
import com.noqapp.mobile.view.controller.api.client.TokenQueueAPIController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * hitender
 * 12/10/17 9:15 PM
 */
@DisplayName("Manage Queue API")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("api")
class ManageQueueControllerITest extends ITest {

    private ManageQueueController manageQueueController;
    private TokenQueueAPIController tokenQueueAPIController;

    @BeforeEach
    void setUp() {
        manageQueueController = new ManageQueueController(
                20,
                authenticateMobileService,
                queueMobileService,
                businessUserStoreService,
                tokenQueueMobileService,
                apiHealthService
        );

        tokenQueueAPIController = new TokenQueueAPIController(
                tokenQueueMobileService,
                queueMobileService,
                inviteService,
                authenticateMobileService,
                apiHealthService
        );
    }

    /**
     * Clean up after each method is required to reset queue and tokens.
     */
    @AfterEach
    void cleanUp() {
        BizNameEntity bizName = bizService.findByPhone("9118000000000");
        BizStoreEntity bizStore = bizService.findOneBizStore(bizName.getId());
        tokenQueueManager.resetForNewDay(bizStore.getCodeQR());
        queueManager.deleteByCodeQR(bizStore.getCodeQR());
    }

    @Test
    @DisplayName("Get all the queues assigned")
    void getQueues() throws IOException {
        UserProfileEntity queueSupervisorUserProfile = accountService.checkUserExistsByPhone("9118000000031");
        UserAccountEntity userAccount = accountService.findByQueueUserId(queueSupervisorUserProfile.getQueueUserId());
        String topics = manageQueueController.getQueues(
                new ScrubbedInput(did),
                new ScrubbedInput(deviceType),
                new ScrubbedInput(userAccount.getUserId()),
                new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
                httpServletResponse
        );
        JsonTopicList jsonTopic = new ObjectMapper().readValue(topics, JsonTopicList.class);
        assertEquals(1, jsonTopic.getTopics().size());
        assertEquals("Food", jsonTopic.getTopics().iterator().next().getDisplayName());
        assertEquals(QueueStatusEnum.S, jsonTopic.getTopics().iterator().next().getQueueStatus());
        assertEquals(0, jsonTopic.getTopics().iterator().next().getServingNumber());
    }

    @Test
    @DisplayName("Serve clients")
    void served() throws IOException {
        BizNameEntity bizName = bizService.findByPhone("9118000000000");
        BizStoreEntity bizStore = bizService.findOneBizStore(bizName.getId());

        UserProfileEntity client1 = accountService.checkUserExistsByPhone("9118000000001");
        UserAccountEntity userAccount1 = accountService.findByQueueUserId(client1.getQueueUserId());
        tokenQueueAPIController.joinQueue(
            new ScrubbedInput(didClient1),
            new ScrubbedInput(deviceType),
            new ScrubbedInput(userAccount1.getUserId()),
            new ScrubbedInput(userAccount1.getUserAuthentication().getAuthenticationKey()),
            new ScrubbedInput(bizStore.getCodeQR()),
            httpServletResponse
        );

        UserProfileEntity client2 = accountService.checkUserExistsByPhone("9118000000002");
        UserAccountEntity userAccount2 = accountService.findByQueueUserId(client2.getQueueUserId());
        tokenQueueAPIController.joinQueue(
                new ScrubbedInput(didClient2),
                new ScrubbedInput(deviceType),
                new ScrubbedInput(userAccount2.getUserId()),
                new ScrubbedInput(userAccount2.getUserAuthentication().getAuthenticationKey()),
                new ScrubbedInput(bizStore.getCodeQR()),
                httpServletResponse
        );

        UserProfileEntity queueSupervisorUserProfile = accountService.checkUserExistsByPhone("9118000000031");
        UserAccountEntity queueUserAccount = accountService.findByQueueUserId(queueSupervisorUserProfile.getQueueUserId());
        String topics = manageQueueController.getQueues(
                new ScrubbedInput(did),
                new ScrubbedInput(deviceType),
                new ScrubbedInput(queueUserAccount.getUserId()),
                new ScrubbedInput(queueUserAccount.getUserAuthentication().getAuthenticationKey()),
                httpServletResponse
        );
        JsonTopicList jsonTopic = new ObjectMapper().readValue(topics, JsonTopicList.class);
        assertEquals(1, jsonTopic.getTopics().size());
        assertEquals("Food", jsonTopic.getTopics().iterator().next().getDisplayName());
        assertEquals(QueueStatusEnum.S, jsonTopic.getTopics().iterator().next().getQueueStatus());
        assertEquals(0, jsonTopic.getTopics().iterator().next().getServingNumber());
        assertEquals(2, jsonTopic.getTopics().iterator().next().getToken());

        /* Starting a queue. */
        Served served0 = new Served()
                .setCodeQR(bizStore.getCodeQR())
                .setServedNumber(0)
                .setQueueUserState(QueueUserStateEnum.S)
                .setQueueStatus(jsonTopic.getTopics().iterator().next().getQueueStatus())
                .setGoTo("Counter 1");

        String jsonTokenResponse0 = manageQueueController.served(
                new ScrubbedInput(didQueueSupervisor),
                new ScrubbedInput(deviceType),
                new ScrubbedInput(queueUserAccount.getUserId()),
                new ScrubbedInput(queueUserAccount.getUserAuthentication().getAuthenticationKey()),
                served0.asJson(),
                httpServletResponse
        );

        JsonToken jsonToken0 = new ObjectMapper().readValue(jsonTokenResponse0, JsonToken.class);
        assertEquals(QueueStatusEnum.N, jsonToken0.getQueueStatus());

        /* Serving first one in queue. */
        Served served1 = new Served()
                .setCodeQR(bizStore.getCodeQR())
                .setServedNumber(jsonToken0.getServingNumber())
                .setQueueUserState(QueueUserStateEnum.S)
                .setQueueStatus(jsonToken0.getQueueStatus())
                .setGoTo("Counter 1");

        String jsonTokenResponse1 = manageQueueController.served(
                new ScrubbedInput(didQueueSupervisor),
                new ScrubbedInput(deviceType),
                new ScrubbedInput(queueUserAccount.getUserId()),
                new ScrubbedInput(queueUserAccount.getUserAuthentication().getAuthenticationKey()),
                served1.asJson(),
                httpServletResponse
        );

        JsonToken jsonToken1 = new ObjectMapper().readValue(jsonTokenResponse1, JsonToken.class);
        assertEquals(QueueStatusEnum.N, jsonToken1.getQueueStatus());

        /* Serving second one in queue. */
        Served served2 = new Served()
                .setCodeQR(bizStore.getCodeQR())
                .setServedNumber(jsonToken1.getServingNumber())
                .setQueueUserState(QueueUserStateEnum.S)
                .setQueueStatus(jsonToken1.getQueueStatus())
                .setGoTo("Counter 1");

        String jsonTokenResponse2 = manageQueueController.served(
                new ScrubbedInput(didQueueSupervisor),
                new ScrubbedInput(deviceType),
                new ScrubbedInput(queueUserAccount.getUserId()),
                new ScrubbedInput(queueUserAccount.getUserAuthentication().getAuthenticationKey()),
                served2.asJson(),
                httpServletResponse
        );

        JsonToken jsonToken2 = new ObjectMapper().readValue(jsonTokenResponse2, JsonToken.class);
        assertEquals(QueueStatusEnum.D, jsonToken2.getQueueStatus());
    }

    @Test
    @DisplayName("Get latest state of a specific queue")
    void getQueue() throws IOException {
        BizNameEntity bizName = bizService.findByPhone("9118000000000");
        BizStoreEntity bizStore = bizService.findOneBizStore(bizName.getId());

        UserProfileEntity queueSupervisorUserProfile = accountService.checkUserExistsByPhone("9118000000031");
        UserAccountEntity queueUserAccount = accountService.findByQueueUserId(queueSupervisorUserProfile.getQueueUserId());
        String topic = manageQueueController.getQueue(
                new ScrubbedInput(did),
                new ScrubbedInput(deviceType),
                new ScrubbedInput(queueUserAccount.getUserId()),
                new ScrubbedInput(queueUserAccount.getUserAuthentication().getAuthenticationKey()),
                new ScrubbedInput(bizStore.getCodeQR()),
                httpServletResponse
        );

        JsonTopic jsonTopic = new ObjectMapper().readValue(topic, JsonTopic.class);
        assertEquals("Food", jsonTopic.getDisplayName());
        assertEquals(QueueStatusEnum.S, jsonTopic.getQueueStatus());
        assertEquals(0, jsonTopic.getServingNumber());
        assertEquals(0, jsonTopic.getToken());
    }

    @Test
    @DisplayName("Checks the state of a queue and the modify the state of it")
    void queueState_Modify_QueueState() throws IOException {
        queueState();
        queueStateModify();
    }

    private void queueState() throws IOException {
        BizNameEntity bizName = bizService.findByPhone("9118000000000");
        BizStoreEntity bizStore = bizService.findOneBizStore(bizName.getId());

        UserProfileEntity queueSupervisorUserProfile = accountService.checkUserExistsByPhone("9118000000031");
        UserAccountEntity queueUserAccount = accountService.findByQueueUserId(queueSupervisorUserProfile.getQueueUserId());
        String queueState = manageQueueController.queueState(
                new ScrubbedInput(did),
                new ScrubbedInput(deviceType),
                new ScrubbedInput(queueUserAccount.getUserId()),
                new ScrubbedInput(queueUserAccount.getUserAuthentication().getAuthenticationKey()),
                new ScrubbedInput(bizStore.getCodeQR()),
                httpServletResponse
        );
        JsonModifyQueue jsonModifyQueue = new ObjectMapper().readValue(queueState, JsonModifyQueue.class);
        assertEquals(false, jsonModifyQueue.isPreventJoining());
        assertEquals(false, jsonModifyQueue.isDayClosed());
        assertEquals(0, jsonModifyQueue.getAvailableTokenCount());
    }

    private void queueStateModify() throws IOException {
        BizNameEntity bizName = bizService.findByPhone("9118000000000");
        BizStoreEntity bizStore = bizService.findOneBizStore(bizName.getId());

        JsonModifyQueue jsonModifyQueue = new JsonModifyQueue()
                .setCodeQR(bizStore.getCodeQR())
                .setDayClosed(true)
                .setPreventJoining(true)
                .setAvailableTokenCount(0);

        UserProfileEntity queueSupervisorUserProfile = accountService.checkUserExistsByPhone("9118000000031");
        UserAccountEntity queueUserAccount = accountService.findByQueueUserId(queueSupervisorUserProfile.getQueueUserId());
        String queueStateResponse = manageQueueController.queueStateModify(
                new ScrubbedInput(did),
                new ScrubbedInput(deviceType),
                new ScrubbedInput(queueUserAccount.getUserId()),
                new ScrubbedInput(queueUserAccount.getUserAuthentication().getAuthenticationKey()),
                jsonModifyQueue,
                httpServletResponse
        );
        JsonModifyQueue jsonModifiedQueue = new ObjectMapper().readValue(queueStateResponse, JsonModifyQueue.class);
        assertEquals(true, jsonModifiedQueue.isPreventJoining());
        assertEquals(true, jsonModifiedQueue.isDayClosed());
        assertEquals(0, jsonModifiedQueue.getAvailableTokenCount());

        /* Reset State of Queue to Day Closed as False and Prevent Joining as False. */
        resetQueueAsOpen(bizStore, queueUserAccount);
    }

    private void resetQueueAsOpen(BizStoreEntity bizStore, UserAccountEntity queueUserAccount) throws IOException {
        JsonModifyQueue jsonModifyQueue;
        String queueStateResponse;
        jsonModifyQueue = new JsonModifyQueue()
                .setCodeQR(bizStore.getCodeQR())
                .setDayClosed(false)
                .setPreventJoining(false)
                .setAvailableTokenCount(0);

        queueStateResponse = manageQueueController.queueStateModify(
                new ScrubbedInput(did),
                new ScrubbedInput(deviceType),
                new ScrubbedInput(queueUserAccount.getUserId()),
                new ScrubbedInput(queueUserAccount.getUserAuthentication().getAuthenticationKey()),
                jsonModifyQueue,
                httpServletResponse
        );
    }

    @Test
    @DisplayName("Show all the queued clients")
    void showQueuedClients() throws IOException {
        BizNameEntity bizName = bizService.findByPhone("9118000000000");
        BizStoreEntity bizStore = bizService.findOneBizStore(bizName.getId());

        UserProfileEntity client1 = accountService.checkUserExistsByPhone("9118000000001");
        UserAccountEntity userAccount1 = accountService.findByQueueUserId(client1.getQueueUserId());
        tokenQueueAPIController.joinQueue(
                new ScrubbedInput(didClient1),
                new ScrubbedInput(deviceType),
                new ScrubbedInput(userAccount1.getUserId()),
                new ScrubbedInput(userAccount1.getUserAuthentication().getAuthenticationKey()),
                new ScrubbedInput(bizStore.getCodeQR()),
                httpServletResponse
        );

        UserProfileEntity client2 = accountService.checkUserExistsByPhone("9118000000002");
        UserAccountEntity userAccount2 = accountService.findByQueueUserId(client2.getQueueUserId());
        tokenQueueAPIController.joinQueue(
                new ScrubbedInput(didClient2),
                new ScrubbedInput(deviceType),
                new ScrubbedInput(userAccount2.getUserId()),
                new ScrubbedInput(userAccount2.getUserAuthentication().getAuthenticationKey()),
                new ScrubbedInput(bizStore.getCodeQR()),
                httpServletResponse
        );

        UserProfileEntity queueSupervisorUserProfile = accountService.checkUserExistsByPhone("9118000000031");
        UserAccountEntity queueUserAccount = accountService.findByQueueUserId(queueSupervisorUserProfile.getQueueUserId());
        String topics = manageQueueController.getQueues(
                new ScrubbedInput(did),
                new ScrubbedInput(deviceType),
                new ScrubbedInput(queueUserAccount.getUserId()),
                new ScrubbedInput(queueUserAccount.getUserAuthentication().getAuthenticationKey()),
                httpServletResponse
        );
        JsonTopicList jsonTopic = new ObjectMapper().readValue(topics, JsonTopicList.class);
        assertEquals(1, jsonTopic.getTopics().size());
        assertEquals("Food", jsonTopic.getTopics().iterator().next().getDisplayName());
        assertEquals(QueueStatusEnum.S, jsonTopic.getTopics().iterator().next().getQueueStatus());
        assertEquals(0, jsonTopic.getTopics().iterator().next().getServingNumber());
        assertEquals(2, jsonTopic.getTopics().iterator().next().getToken());

        /* Before start of the queue. Show all queued clients. */
        String jsonQueuePerson = manageQueueController.showQueuedClients(
                new ScrubbedInput(didQueueSupervisor),
                new ScrubbedInput(deviceType),
                new ScrubbedInput(queueUserAccount.getUserId()),
                new ScrubbedInput(queueUserAccount.getUserAuthentication().getAuthenticationKey()),
                new ScrubbedInput(bizStore.getCodeQR()),
                httpServletResponse
        );

        JsonQueuePersonList jsonQueuePersonList = new ObjectMapper().readValue(jsonQueuePerson, JsonQueuePersonList.class);
        assertEquals(2, jsonQueuePersonList.getQueuedPeople().size());
        assertEquals(client2.getName(), jsonQueuePersonList.getQueuedPeople().iterator().next().getCustomerName());
        assertEquals(client2.getPhone(), jsonQueuePersonList.getQueuedPeople().iterator().next().getCustomerPhone());
        assertEquals(2, jsonQueuePersonList.getQueuedPeople().iterator().next().getToken());
        assertNull(jsonQueuePersonList.getQueuedPeople().iterator().next().getServerDeviceId());
    }

    @Test
    @DisplayName("Cannot acquire client when Queue State is still Start")
    void acquireFails_When_QueueStatus_Is_Start() throws IOException {
        BizNameEntity bizName = bizService.findByPhone("9118000000000");
        BizStoreEntity bizStore = bizService.findOneBizStore(bizName.getId());

        UserProfileEntity client1 = accountService.checkUserExistsByPhone("9118000000001");
        UserAccountEntity userAccount1 = accountService.findByQueueUserId(client1.getQueueUserId());
        tokenQueueAPIController.joinQueue(
                new ScrubbedInput(didClient1),
                new ScrubbedInput(deviceType),
                new ScrubbedInput(userAccount1.getUserId()),
                new ScrubbedInput(userAccount1.getUserAuthentication().getAuthenticationKey()),
                new ScrubbedInput(bizStore.getCodeQR()),
                httpServletResponse
        );

        UserProfileEntity client2 = accountService.checkUserExistsByPhone("9118000000002");
        UserAccountEntity userAccount2 = accountService.findByQueueUserId(client2.getQueueUserId());
        tokenQueueAPIController.joinQueue(
                new ScrubbedInput(didClient2),
                new ScrubbedInput(deviceType),
                new ScrubbedInput(userAccount2.getUserId()),
                new ScrubbedInput(userAccount2.getUserAuthentication().getAuthenticationKey()),
                new ScrubbedInput(bizStore.getCodeQR()),
                httpServletResponse
        );

        UserProfileEntity queueSupervisorUserProfile = accountService.checkUserExistsByPhone("9118000000031");
        UserAccountEntity queueUserAccount = accountService.findByQueueUserId(queueSupervisorUserProfile.getQueueUserId());
        String topics = manageQueueController.getQueues(
                new ScrubbedInput(did),
                new ScrubbedInput(deviceType),
                new ScrubbedInput(queueUserAccount.getUserId()),
                new ScrubbedInput(queueUserAccount.getUserAuthentication().getAuthenticationKey()),
                httpServletResponse
        );
        JsonTopicList jsonTopic = new ObjectMapper().readValue(topics, JsonTopicList.class);
        assertEquals(1, jsonTopic.getTopics().size());
        assertEquals("Food", jsonTopic.getTopics().iterator().next().getDisplayName());
        assertEquals(QueueStatusEnum.S, jsonTopic.getTopics().iterator().next().getQueueStatus());
        assertEquals(0, jsonTopic.getTopics().iterator().next().getServingNumber());
        assertEquals(2, jsonTopic.getTopics().iterator().next().getToken());

        /* Acquire before starting a queue. Fails as the Queue State has to be Next. */
        Served served2Fail = new Served()
                .setCodeQR(bizStore.getCodeQR())
                .setServedNumber(2)
                .setQueueUserState(null) //No need for Queue User State as its being acquired.
                .setQueueStatus(jsonTopic.getTopics().iterator().next().getQueueStatus())
                .setGoTo("Counter 1");

        assertThrows(UnsupportedOperationException.class, () -> {
            manageQueueController.acquire(
                    new ScrubbedInput(didQueueSupervisor),
                    new ScrubbedInput(deviceType),
                    new ScrubbedInput(queueUserAccount.getUserId()),
                    new ScrubbedInput(queueUserAccount.getUserAuthentication().getAuthenticationKey()),
                    served2Fail.asJson(),
                    httpServletResponse
            );
        });
    }

    @Test
    @DisplayName("Acquire Client out of sequence when queue has begun")
    void acquire() throws IOException {
        BizNameEntity bizName = bizService.findByPhone("9118000000000");
        BizStoreEntity bizStore = bizService.findOneBizStore(bizName.getId());

        UserProfileEntity client1 = accountService.checkUserExistsByPhone("9118000000001");
        UserAccountEntity userAccount1 = accountService.findByQueueUserId(client1.getQueueUserId());
        tokenQueueAPIController.joinQueue(
                new ScrubbedInput(didClient1),
                new ScrubbedInput(deviceType),
                new ScrubbedInput(userAccount1.getUserId()),
                new ScrubbedInput(userAccount1.getUserAuthentication().getAuthenticationKey()),
                new ScrubbedInput(bizStore.getCodeQR()),
                httpServletResponse
        );

        UserProfileEntity client2 = accountService.checkUserExistsByPhone("9118000000002");
        UserAccountEntity userAccount2 = accountService.findByQueueUserId(client2.getQueueUserId());
        tokenQueueAPIController.joinQueue(
                new ScrubbedInput(didClient2),
                new ScrubbedInput(deviceType),
                new ScrubbedInput(userAccount2.getUserId()),
                new ScrubbedInput(userAccount2.getUserAuthentication().getAuthenticationKey()),
                new ScrubbedInput(bizStore.getCodeQR()),
                httpServletResponse
        );

        UserProfileEntity queueSupervisorUserProfile = accountService.checkUserExistsByPhone("9118000000031");
        UserAccountEntity queueUserAccount = accountService.findByQueueUserId(queueSupervisorUserProfile.getQueueUserId());
        String topics = manageQueueController.getQueues(
                new ScrubbedInput(did),
                new ScrubbedInput(deviceType),
                new ScrubbedInput(queueUserAccount.getUserId()),
                new ScrubbedInput(queueUserAccount.getUserAuthentication().getAuthenticationKey()),
                httpServletResponse
        );
        JsonTopicList jsonTopic = new ObjectMapper().readValue(topics, JsonTopicList.class);
        assertEquals(1, jsonTopic.getTopics().size());
        assertEquals("Food", jsonTopic.getTopics().iterator().next().getDisplayName());
        assertEquals(QueueStatusEnum.S, jsonTopic.getTopics().iterator().next().getQueueStatus());
        assertEquals(0, jsonTopic.getTopics().iterator().next().getServingNumber());
        assertEquals(2, jsonTopic.getTopics().iterator().next().getToken());

        /* Starting a queue. */
        Served served0 = new Served()
                .setCodeQR(bizStore.getCodeQR())
                .setServedNumber(0)
                .setQueueUserState(QueueUserStateEnum.N)
                .setQueueStatus(jsonTopic.getTopics().iterator().next().getQueueStatus())
                .setGoTo("Counter 1");

        String jsonTokenResponse0 = manageQueueController.served(
                new ScrubbedInput(didQueueSupervisor),
                new ScrubbedInput(deviceType),
                new ScrubbedInput(queueUserAccount.getUserId()),
                new ScrubbedInput(queueUserAccount.getUserAuthentication().getAuthenticationKey()),
                served0.asJson(),
                httpServletResponse
        );

        JsonToken jsonToken0 = new ObjectMapper().readValue(jsonTokenResponse0, JsonToken.class);
        assertEquals(QueueStatusEnum.N, jsonToken0.getQueueStatus());

        /* Acquire second in the queue. */
        Served served2 = new Served()
                .setCodeQR(bizStore.getCodeQR())
                .setServedNumber(2)
                .setQueueUserState(null) //No need for Queue User State as its being acquired.
                .setQueueStatus(jsonTopic.getTopics().iterator().next().getQueueStatus())
                .setGoTo("Counter 1");

        String jsonTokenResponse = manageQueueController.acquire(
                new ScrubbedInput(didQueueSupervisor),
                new ScrubbedInput(deviceType),
                new ScrubbedInput(queueUserAccount.getUserId()),
                new ScrubbedInput(queueUserAccount.getUserAuthentication().getAuthenticationKey()),
                served2.asJson(),
                httpServletResponse
        );
        JsonToken jsonToken =  new ObjectMapper().readValue(jsonTokenResponse, JsonToken.class);
        assertEquals(2, jsonToken.getServingNumber());
        assertEquals(client2.getName(), jsonToken.getCustomerName());
    }

    @Test
    @DisplayName("Dispense token when user walks-in or has no phone")
    void dispenseToken() throws IOException {
        BizNameEntity bizName = bizService.findByPhone("9118000000000");
        BizStoreEntity bizStore = bizService.findOneBizStore(bizName.getId());

        UserProfileEntity client1 = accountService.checkUserExistsByPhone("9118000000001");
        UserAccountEntity userAccount1 = accountService.findByQueueUserId(client1.getQueueUserId());
        tokenQueueAPIController.joinQueue(
                new ScrubbedInput(didClient1),
                new ScrubbedInput(deviceType),
                new ScrubbedInput(userAccount1.getUserId()),
                new ScrubbedInput(userAccount1.getUserAuthentication().getAuthenticationKey()),
                new ScrubbedInput(bizStore.getCodeQR()),
                httpServletResponse
        );

        UserProfileEntity queueSupervisorUserProfile = accountService.checkUserExistsByPhone("9118000000031");
        UserAccountEntity queueUserAccount = accountService.findByQueueUserId(queueSupervisorUserProfile.getQueueUserId());
        String topics = manageQueueController.getQueues(
                new ScrubbedInput(did),
                new ScrubbedInput(deviceType),
                new ScrubbedInput(queueUserAccount.getUserId()),
                new ScrubbedInput(queueUserAccount.getUserAuthentication().getAuthenticationKey()),
                httpServletResponse
        );
        JsonTopicList jsonTopic = new ObjectMapper().readValue(topics, JsonTopicList.class);
        assertEquals(1, jsonTopic.getTopics().size());
        assertEquals("Food", jsonTopic.getTopics().iterator().next().getDisplayName());
        assertEquals(QueueStatusEnum.S, jsonTopic.getTopics().iterator().next().getQueueStatus());
        assertEquals(0, jsonTopic.getTopics().iterator().next().getServingNumber());
        assertEquals(1, jsonTopic.getTopics().iterator().next().getToken());

        String dispenseToken1 = manageQueueController.dispenseToken(
                new ScrubbedInput(did),
                new ScrubbedInput(deviceType),
                new ScrubbedInput(queueUserAccount.getUserId()),
                new ScrubbedInput(queueUserAccount.getUserAuthentication().getAuthenticationKey()),
                new ScrubbedInput(jsonTopic.getTopics().iterator().next().getCodeQR()),
                httpServletResponse
        );
        JsonToken jsonToken1 = new ObjectMapper().readValue(dispenseToken1, JsonToken.class);
        assertEquals(QueueStatusEnum.S, jsonToken1.getQueueStatus());
        assertEquals(2, jsonToken1.getToken());

        UserProfileEntity client2 = accountService.checkUserExistsByPhone("9118000000002");
        UserAccountEntity userAccount2 = accountService.findByQueueUserId(client2.getQueueUserId());
        tokenQueueAPIController.joinQueue(
                new ScrubbedInput(didClient2),
                new ScrubbedInput(deviceType),
                new ScrubbedInput(userAccount2.getUserId()),
                new ScrubbedInput(userAccount2.getUserAuthentication().getAuthenticationKey()),
                new ScrubbedInput(bizStore.getCodeQR()),
                httpServletResponse
        );

        String dispenseToken2 = manageQueueController.dispenseToken(
                new ScrubbedInput(did),
                new ScrubbedInput(deviceType),
                new ScrubbedInput(queueUserAccount.getUserId()),
                new ScrubbedInput(queueUserAccount.getUserAuthentication().getAuthenticationKey()),
                new ScrubbedInput(jsonTopic.getTopics().iterator().next().getCodeQR()),
                httpServletResponse
        );
        JsonToken jsonToken2 = new ObjectMapper().readValue(dispenseToken2, JsonToken.class);
        assertEquals(QueueStatusEnum.S, jsonToken2.getQueueStatus());
        assertEquals(4, jsonToken2.getToken());
    }

    @Test
    @DisplayName("Dispense token fails when queue is closed")
    void dispenseTokenFailWhenStoreIsClosedOrPreventJoin() throws IOException {
        BizNameEntity bizName = bizService.findByPhone("9118000000000");
        BizStoreEntity bizStore = bizService.findOneBizStore(bizName.getId());

        JsonModifyQueue jsonModifyQueue = new JsonModifyQueue()
                .setCodeQR(bizStore.getCodeQR())
                .setDayClosed(true)
                .setPreventJoining(false)
                .setAvailableTokenCount(0);

        UserProfileEntity queueSupervisorUserProfile = accountService.checkUserExistsByPhone("9118000000031");
        UserAccountEntity queueUserAccount = accountService.findByQueueUserId(queueSupervisorUserProfile.getQueueUserId());
        String queueStateResponse = manageQueueController.queueStateModify(
                new ScrubbedInput(did),
                new ScrubbedInput(deviceType),
                new ScrubbedInput(queueUserAccount.getUserId()),
                new ScrubbedInput(queueUserAccount.getUserAuthentication().getAuthenticationKey()),
                jsonModifyQueue,
                httpServletResponse
        );
        JsonModifyQueue jsonModifiedQueue = new ObjectMapper().readValue(queueStateResponse, JsonModifyQueue.class);
        assertEquals(false, jsonModifiedQueue.isPreventJoining());
        assertEquals(true, jsonModifiedQueue.isDayClosed());
        assertEquals(0, jsonModifiedQueue.getAvailableTokenCount());
        /* Setup complete for test. */

        UserProfileEntity client1 = accountService.checkUserExistsByPhone("9118000000001");
        UserAccountEntity userAccount1 = accountService.findByQueueUserId(client1.getQueueUserId());
        String joinQueue = tokenQueueAPIController.joinQueue(
                new ScrubbedInput(didClient1),
                new ScrubbedInput(deviceType),
                new ScrubbedInput(userAccount1.getUserId()),
                new ScrubbedInput(userAccount1.getUserAuthentication().getAuthenticationKey()),
                new ScrubbedInput(bizStore.getCodeQR()),
                httpServletResponse
        );
        JsonToken jsonToken = new ObjectMapper().readValue(joinQueue, JsonToken.class);
        assertEquals(QueueStatusEnum.C, jsonToken.getQueueStatus());
        assertEquals(0, jsonToken.getToken());

        String topics = manageQueueController.getQueues(
                new ScrubbedInput(did),
                new ScrubbedInput(deviceType),
                new ScrubbedInput(queueUserAccount.getUserId()),
                new ScrubbedInput(queueUserAccount.getUserAuthentication().getAuthenticationKey()),
                httpServletResponse
        );
        JsonTopicList jsonTopic = new ObjectMapper().readValue(topics, JsonTopicList.class);
        assertEquals(1, jsonTopic.getTopics().size());
        assertEquals("Food", jsonTopic.getTopics().iterator().next().getDisplayName());
        assertEquals(QueueStatusEnum.S, jsonTopic.getTopics().iterator().next().getQueueStatus());
        assertEquals(0, jsonTopic.getTopics().iterator().next().getServingNumber());
        assertEquals(0, jsonTopic.getTopics().iterator().next().getToken());

        String dispenseToken1 = manageQueueController.dispenseToken(
                new ScrubbedInput(did),
                new ScrubbedInput(deviceType),
                new ScrubbedInput(queueUserAccount.getUserId()),
                new ScrubbedInput(queueUserAccount.getUserAuthentication().getAuthenticationKey()),
                new ScrubbedInput(jsonTopic.getTopics().iterator().next().getCodeQR()),
                httpServletResponse
        );
        JsonToken jsonToken1 = new ObjectMapper().readValue(dispenseToken1, JsonToken.class);
        assertEquals(QueueStatusEnum.C, jsonToken1.getQueueStatus());
        assertEquals(0, jsonToken1.getToken());

        String dispenseToken2 = manageQueueController.dispenseToken(
                new ScrubbedInput(did),
                new ScrubbedInput(deviceType),
                new ScrubbedInput(queueUserAccount.getUserId()),
                new ScrubbedInput(queueUserAccount.getUserAuthentication().getAuthenticationKey()),
                new ScrubbedInput(jsonTopic.getTopics().iterator().next().getCodeQR()),
                httpServletResponse
        );
        JsonToken jsonToken2 = new ObjectMapper().readValue(dispenseToken2, JsonToken.class);
        assertEquals(QueueStatusEnum.C, jsonToken2.getQueueStatus());
        assertEquals(0, jsonToken2.getToken());

        /* Reset State of Queue to Day Closed as False and Prevent Joining as False. */
        resetQueueAsOpen(bizStore, queueUserAccount);
    }
}
