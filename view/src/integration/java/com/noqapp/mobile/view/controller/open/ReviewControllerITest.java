package com.noqapp.mobile.view.controller.open;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BizNameEntity;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.types.DeviceTypeEnum;
import com.noqapp.mobile.domain.body.ReviewRating;
import com.noqapp.mobile.view.ITest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * hitender
 * 12/8/17 3:28 AM
 */
@DisplayName("Review Service")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("api")
class ReviewControllerITest extends ITest {

    private ReviewController reviewController;

    @Mock private HttpServletResponse httpServletResponse;

    private String did;

    @BeforeAll
    void testSetUp() {
        did = UUID.randomUUID().toString();
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        reviewController = new ReviewController(
                tokenQueueMobileService,
                queueMobileService,
                apiHealthService
        );
    }

    @Test
    void service_fails_when_codeQR_DoesNotExists() {
        String deviceType = DeviceTypeEnum.A.getName();

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
    void service() {
        BizNameEntity bizNameEntity = new BizNameEntity();
        BizStoreEntity bizStoreEntity = new BizStoreEntity();

//        bizService.
    }
}