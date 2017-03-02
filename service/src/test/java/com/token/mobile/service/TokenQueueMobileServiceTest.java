package com.token.mobile.service;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import com.token.service.BizService;
import com.token.service.TokenQueueService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * User: hitender
 * Date: 11/20/16 6:46 PM
 */
public class TokenQueueMobileServiceTest {
    @Mock private BizService bizService;
    @Mock private TokenQueueService tokenQueueService;

    private TokenQueueMobileService tokenQueueMobileService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        this.tokenQueueMobileService = new TokenQueueMobileService(tokenQueueService, bizService, tokenQueueManager);
    }

    @Test
    public void isValidCodeQR() throws Exception {
        when(bizService.isValidCodeQR(anyString())).thenReturn(true);
        assertTrue(bizService.isValidCodeQR("code"));
    }
}