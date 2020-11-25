package com.noqapp.mobile.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.noqapp.repository.BusinessUserStoreManager;
import com.noqapp.repository.QueueManager;
import com.noqapp.repository.TokenQueueManager;
import com.noqapp.repository.UserProfileManager;
import com.noqapp.service.BizService;
import com.noqapp.service.NotifyMobileService;
import com.noqapp.service.ProfessionalProfileService;
import com.noqapp.service.QueueService;
import com.noqapp.service.StoreHourService;
import com.noqapp.service.TokenQueueService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * User: hitender
 * Date: 11/20/16 6:46 PM
 */
class TokenQueueMobileServiceTest {
    @Mock private BizService bizService;
    @Mock private TokenQueueService tokenQueueService;
    @Mock private TokenQueueManager tokenQueueManager;
    @Mock private QueueManager queueManager;
    @Mock private ProfessionalProfileService professionalProfileService;
    @Mock private UserProfileManager userProfileManager;
    @Mock private BusinessUserStoreManager businessUserStoreManager;
    @Mock private NotifyMobileService notifyMobileService;
    @Mock private StoreHourService storeHourService;
    @Mock private QueueService queueService;

    private TokenQueueMobileService tokenQueueMobileService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        this.tokenQueueMobileService = new TokenQueueMobileService(
            tokenQueueService,
            bizService,
            tokenQueueManager,
            queueManager,
            professionalProfileService,
            userProfileManager,
            businessUserStoreManager,
            notifyMobileService,
            storeHourService,
            queueService
        );
    }

    @Test
    void isValidCodeQR() {
        when(bizService.isValidCodeQR(anyString())).thenReturn(true);
        assertTrue(tokenQueueMobileService.isValidCodeQR(anyString()));
    }
}
