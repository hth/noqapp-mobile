package com.token.mobile.service;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

/**
 * User: hitender
 * Date: 11/20/16 6:46 PM
 */
public class TokenServiceTest {

    private TokenService tokenService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        this.tokenService = new TokenService();
    }


    @Test
    public void isValid() throws Exception {
        assertTrue(tokenService.isValid("code"));
    }

}