package com.noqapp.mobile.view.controller.open;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BizNameEntity;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.QueueEntity;
import com.noqapp.domain.TokenQueueEntity;
import com.noqapp.domain.json.JsonQueue;
import com.noqapp.domain.json.JsonResponse;
import com.noqapp.domain.json.JsonToken;
import com.noqapp.domain.types.QueueStatusEnum;
import com.noqapp.domain.types.QueueUserStateEnum;
import com.noqapp.domain.types.TokenServiceEnum;
import com.noqapp.mobile.domain.body.client.ReviewRating;
import com.noqapp.mobile.view.ITest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * hitender
 * 12/8/17 3:28 AM
 */
@DisplayName("Review API")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("api")
class ReviewControllerITest extends ITest {

    private ReviewController reviewController;
    private TokenQueueController tokenQueueController;

    @BeforeEach
    void setUp() {
        reviewController = new ReviewController(
                tokenQueueMobileService,
                queueMobileService,
                apiHealthService
        );

        tokenQueueController = new TokenQueueController(
            tokenQueueMobileService,
            queueMobileService,
            apiHealthService
        );
    }

    @Test
    @DisplayName("Service fails when code QR does not exists")
    void service_fails_when_codeQR_DoesNotExists() {
        ReviewRating reviewRating = new ReviewRating()
                .setCodeQR(did)
                .setToken(1)
                .setRatingCount("5")
                .setHoursSaved("1");

        String response = reviewController.service(
                new ScrubbedInput(did),
                new ScrubbedInput(deviceType),
                reviewRating.asJson(),
                httpServletResponse
        );

        assertNull(response);
    }

    @Test
    @DisplayName("Service when completed")
    void service() throws IOException {
        BizNameEntity bizName = bizService.findByPhone("9118000000000");
        BizStoreEntity bizStore = bizService.findOneBizStore(bizName.getId());

        String beforeJoin = tokenQueueController.getQueueState(
                new ScrubbedInput(did),
                new ScrubbedInput(deviceType),
                new ScrubbedInput(bizStore.getCodeQR()),
                httpServletResponse
        );
        JsonQueue jsonQueue = new ObjectMapper().readValue(beforeJoin, JsonQueue.class);

        String afterJoin = tokenQueueController.joinQueue(
                new ScrubbedInput(did),
                new ScrubbedInput(deviceType),
                new ScrubbedInput(jsonQueue.getCodeQR()),
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
        assertEquals(QueueUserStateEnum.S, queueAfterService.getQueueUserState());
        assertEquals(did, queueAfterService.getDid());
    }

    private void submitReview(BizStoreEntity bizStore, JsonToken jsonToken) throws IOException {
        ReviewRating reviewRating = new ReviewRating()
                .setCodeQR(bizStore.getCodeQR())
                .setToken(jsonToken.getToken())
                .setRatingCount("5")
                .setHoursSaved("1");

        /* Fails to update as its still under Queued state. */
        String response = reviewController.service(
                new ScrubbedInput(did),
                new ScrubbedInput(deviceType),
                reviewRating.asJson(),
                httpServletResponse
        );
        JsonResponse jsonResponse = new ObjectMapper().readValue(response, JsonResponse.class);
        assertEquals(1, jsonResponse.getResponse());
    }
}
