package com.noqapp.mobile.view.controller.open;

import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.MOBILE_UPGRADE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.noqapp.common.errors.ErrorJsonList;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BizNameEntity;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.json.JsonQueue;
import com.noqapp.domain.json.JsonResponse;
import com.noqapp.domain.json.JsonToken;
import com.noqapp.mobile.domain.body.client.QueueReview;
import com.noqapp.mobile.view.ITest;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;

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
            reviewService,
            apiHealthService
        );

        tokenQueueController = new TokenQueueController(
            tokenQueueMobileService,
            joinAbortService,
            queueMobileService,
            geoIPLocationService,
            queueService,
            apiHealthService
        );
    }

    @Test
    @DisplayName("Service fails when code QR does not exists")
    void service_fails_when_codeQR_DoesNotExists() {
        QueueReview queueReview = new QueueReview()
            .setCodeQR(did)
            .setToken(1)
            .setRatingCount(5)
            .setHoursSaved(1)
            .setReview("This is review");

        String response = reviewController.queue(
            new ScrubbedInput(did),
            new ScrubbedInput(deviceType),
            queueReview,
            httpServletResponse
        );

        assertNull(response);
    }

    @Test
    @DisplayName("Review after service when completed")
    void queueReview() throws IOException {
        BizNameEntity bizName = bizService.findByPhone("9118000000000");
        BizStoreEntity bizStore = bizService.findOneBizStore(bizName.getId());

        String beforeJoin = tokenQueueController.getQueueState(
            new ScrubbedInput(did),
            new ScrubbedInput(deviceType),
            new ScrubbedInput(bizStore.getCodeQR()),
            httpServletResponse
        );
        JsonQueue jsonQueue = new ObjectMapper().readValue(beforeJoin, JsonQueue.class);

        String afterJoin = tokenQueueController.joinQueueObsolete(
            new ScrubbedInput(did),
            new ScrubbedInput(deviceType),
            new ScrubbedInput(jsonQueue.getCodeQR()),
            httpServletResponse
        );
        ErrorJsonList errorJsonList = new ObjectMapper().readValue(afterJoin, ErrorJsonList.class);
        assertEquals(MOBILE_UPGRADE.getCode(), errorJsonList.getError().getSystemErrorCode());
    }
}
