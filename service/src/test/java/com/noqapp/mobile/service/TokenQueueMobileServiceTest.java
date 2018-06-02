package com.noqapp.mobile.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.noqapp.medical.service.HealthCareProfileService;
import com.noqapp.repository.QueueManager;
import com.noqapp.repository.TokenQueueManager;
import com.noqapp.service.BizService;
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
    @Mock private HealthCareProfileService healthCareProfileService;

    private TokenQueueMobileService tokenQueueMobileService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.initMocks(this);
        this.tokenQueueMobileService = new TokenQueueMobileService(tokenQueueService, bizService, tokenQueueManager, queueManager, healthCareProfileService);
    }

    @Test
    void isValidCodeQR() {
        when(bizService.isValidCodeQR(anyString())).thenReturn(true);
        assertTrue(tokenQueueMobileService.isValidCodeQR(anyString()));
    }
}