package com.noqapp.mobile.view.controller.api.client;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.types.AppFlavorEnum;
import com.noqapp.domain.types.DeviceTypeEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.domain.body.client.Feedback;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.service.FeedbackService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

/**
 * User: hitender
 * Date: 10/6/18 9:39 PM
 */
@SuppressWarnings ({
        "PMD.BeanMembersShouldSerialize",
        "PMD.LocalVariableCouldBeFinal",
        "PMD.MethodArgumentCouldBeFinal",
        "PMD.LongVariable"
})
@DisplayName("Feedback API")
class FeedbackAPIControllerTest {

    private FeedbackAPIController feedbackAPIController;

    @Mock private AuthenticateMobileService authenticateMobileService;
    @Mock private FeedbackService feedbackService;
    @Mock private ApiHealthService apiHealthService;

    @Mock private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        feedbackAPIController = new FeedbackAPIController(
                authenticateMobileService,
                feedbackService,
                apiHealthService
        );
    }

    @Test
    void feedback() throws IOException {
        when(authenticateMobileService.getQueueUserId(anyString(), anyString())).thenReturn("rid");

        feedbackAPIController.feedback(
                new ScrubbedInput(""),
                new ScrubbedInput(DeviceTypeEnum.A.getName()),
                new ScrubbedInput(AppFlavorEnum.NQCL.getName()),
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                new Feedback().setSubject(new ScrubbedInput("hi")).setBody(new ScrubbedInput("message")),
                response
        );
        verify(authenticateMobileService, times(1)).getQueueUserId(any(String.class), any(String.class));
        verify(feedbackService, times(1)).submitFeedback(any(String.class), any(Feedback.class));
    }
}
