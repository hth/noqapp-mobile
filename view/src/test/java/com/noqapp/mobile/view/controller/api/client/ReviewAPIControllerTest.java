package com.noqapp.mobile.view.controller.api.client;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BizNameEntity;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.QueueEntity;
import com.noqapp.domain.TokenQueueEntity;
import com.noqapp.domain.UserAccountEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.json.JsonQueue;
import com.noqapp.domain.json.JsonResponse;
import com.noqapp.domain.json.JsonToken;
import com.noqapp.domain.types.QueueStatusEnum;
import com.noqapp.domain.types.QueueUserStateEnum;
import com.noqapp.domain.types.SentimentTypeEnum;
import com.noqapp.domain.types.TokenServiceEnum;
import com.noqapp.mobile.domain.body.client.JoinQueue;
import com.noqapp.mobile.domain.body.client.QueueReview;
import com.noqapp.mobile.view.ITest;
import com.noqapp.mobile.view.controller.open.ReviewController;
import com.noqapp.mobile.view.controller.open.TokenQueueController;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

/**
 * hitender
 * 7/15/21 10:30 PM
 */
@DisplayName("Historical API")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("api")
class ReviewAPIControllerTest extends ITest {

    private ReviewAPIController reviewAPIController;
    private TokenQueueAPIController tokenQueueAPIController;

    private UserProfileEntity userProfile;
    private UserAccountEntity userAccount;

    @BeforeEach
    void setUp() {
        reviewAPIController = new ReviewAPIController(
            authenticateMobileService,
            tokenQueueMobileService,
            queueMobileService,
            purchaseOrderMobileService,
            apiHealthService
        );

        tokenQueueAPIController = new TokenQueueAPIController(
            tokenQueueMobileService,
            joinAbortService,
            queueMobileService,
            authenticateMobileService,
            purchaseOrderService,
            purchaseOrderMobileService,
            scheduleAppointmentService,
            geoIPLocationService,
            businessCustomerService,
            queueService,
            userAddressManager,
            graphDetailOfPerson,
            apiHealthService
        );

        userProfile = userProfileManager.findOneByPhone("9118000000001");
        userAccount = userAccountManager.findByQueueUserId(userProfile.getQueueUserId());
    }

    @Test
    @DisplayName("Service fails when code QR does not exists")
    void service_fails_when_codeQR_DoesNotExists() throws IOException {
        QueueReview queueReview = new QueueReview()
            .setCodeQR(did)
            .setToken(1)
            .setRatingCount(5)
            .setHoursSaved(1)
            .setReview("This is review");

        String response = reviewAPIController.queue(
            new ScrubbedInput(did),
            new ScrubbedInput(deviceType),
            new ScrubbedInput(userProfile.getEmail()),
            new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
            queueReview,
            httpServletResponse
        );

        assertNull(response);
    }

    @Test
    void queue() throws IOException {
        BizNameEntity bizName = bizService.findByPhone("9118000000000");
        BizStoreEntity bizStore = bizService.findOneBizStore(bizName.getId());

        String beforeJoin = tokenQueueAPIController.getQueueState(
            new ScrubbedInput(did),
            new ScrubbedInput(deviceType),
            new ScrubbedInput(userProfile.getEmail()),
            new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
            new ScrubbedInput(bizStore.getCodeQR()),
            httpServletResponse
        );
        JsonQueue jsonQueue = new ObjectMapper().readValue(beforeJoin, JsonQueue.class);
        assertEquals(QueueStatusEnum.S, jsonQueue.getQueueStatus());

        JoinQueue joinQueue = new JoinQueue().setQueueUserId(userAccount.getQueueUserId()).setCodeQR(bizStore.getCodeQR());
        String afterJoin = tokenQueueAPIController.joinQueue(
            new ScrubbedInput(did),
            new ScrubbedInput(deviceType),
            new ScrubbedInput(userProfile.getEmail()),
            new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
            joinQueue,
            httpServletResponse
        );
        JsonToken jsonToken = new ObjectMapper().readValue(afterJoin, JsonToken.class);
        assertEquals(QueueStatusEnum.S, jsonToken.getQueueStatus());

        submitReview(bizStore, jsonToken);

        /* Submit review fails when person is not served. */
        QueueEntity queue = queueManager.findOne(bizStore.getCodeQR(), jsonToken.getToken());
        assertEquals(QueueUserStateEnum.Q, queue.getQueueUserState());
        assertEquals(did, queue.getDid());

        /* Initial state of Queue. */
        TokenQueueEntity tokenQueue = queueMobileService.getTokenQueueByCodeQR(bizStore.getCodeQR());
        assertEquals(QueueStatusEnum.S, tokenQueue.getQueueStatus());

        String sid = UUID.randomUUID().toString();

        /* After starting queue state. */
        JsonToken nextInQueue = queueService.getNextInQueue(bizStore.getCodeQR(), "Go To Counter", sid);
        assertEquals(jsonToken.getToken(), nextInQueue.getToken());
        assertNotEquals(jsonToken.getServingNumber(), nextInQueue.getServingNumber());
        assertEquals(QueueStatusEnum.N, nextInQueue.getQueueStatus());
        TokenQueueEntity tokenQueueAfterPressingNext = queueMobileService.getTokenQueueByCodeQR(bizStore.getCodeQR());
        assertEquals(QueueStatusEnum.N, tokenQueueAfterPressingNext.getQueueStatus());

        /* When no more to serve, service is done. Queue state is set to Done. */
        nextInQueue = queueService.updateAndGetNextInQueue(
            bizStore.getCodeQR(),
            queue.getTokenNumber(),
            QueueUserStateEnum.S,
            "Go To Counter",
            sid,
            TokenServiceEnum.M);
        assertEquals(QueueStatusEnum.D, nextInQueue.getQueueStatus());
        TokenQueueEntity tokenQueueAfterReachingDoneWhenThereIsNoNext = queueMobileService.getTokenQueueByCodeQR(bizStore.getCodeQR());
        assertEquals(QueueStatusEnum.D, tokenQueueAfterReachingDoneWhenThereIsNoNext.getQueueStatus());

        /* Review after user has been serviced. */
        submitReview(bizStore, jsonToken);

        /* Check for submitted review. */
        QueueEntity queueAfterService = queueManager.findOne(bizStore.getCodeQR(), jsonToken.getToken());
        while (queueAfterService.getSentimentType() == null) {
            await()
                .atLeast(Duration.ofMillis(500))
                .atMost(Duration.ofSeconds(5));
            queueAfterService = queueManager.findOne(bizStore.getCodeQR(), jsonToken.getToken());
        }
        assertEquals(QueueUserStateEnum.S, queueAfterService.getQueueUserState());
        assertEquals(did, queueAfterService.getDid());
        assertEquals(SentimentTypeEnum.P, queueAfterService.getSentimentType());
    }

//    @Test
//    void order() {
//    }

    private void submitReview(BizStoreEntity bizStore, JsonToken jsonToken) throws IOException {
        QueueReview queueReview = new QueueReview()
            .setCodeQR(bizStore.getCodeQR())
            .setToken(jsonToken.getToken())
            .setRatingCount(5)
            .setHoursSaved(1)
            .setReview("This is a good review");

        /* Response is always true. */
        String response = reviewAPIController.queue(
            new ScrubbedInput(did),
            new ScrubbedInput(deviceType),
            new ScrubbedInput(userProfile.getEmail()),
            new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
            queueReview,
            httpServletResponse
        );
        JsonResponse jsonResponse = new ObjectMapper().readValue(response, JsonResponse.class);
        assertEquals(1, jsonResponse.getResponse());
    }
}
