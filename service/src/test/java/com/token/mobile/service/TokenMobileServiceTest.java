package com.token.mobile.service;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import com.token.repository.BizStoreManager;
import com.token.service.TokenQueueService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * User: hitender
 * Date: 11/20/16 6:46 PM
 */
public class TokenMobileServiceTest {
    @Mock private BizStoreManager bizStoreManager;
    @Mock private TokenQueueService tokenService;

    private TokenMobileService tokenMobileService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        this.tokenMobileService = new TokenMobileService(bizStoreManager, tokenService);
    }

    @Test
    public void isValidCodeQR() throws Exception {
        when(bizStoreManager.isValidCodeQR(anyString())).thenReturn(true);
        assertTrue(tokenMobileService.isValidCodeQR("code"));
    }
}