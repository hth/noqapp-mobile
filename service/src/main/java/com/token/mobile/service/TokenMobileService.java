package com.token.mobile.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.token.domain.BizStoreEntity;
import com.token.repository.BizStoreManager;

/**
 * User: hitender
 * Date: 11/17/16 3:10 PM
 */
@Service
public class TokenMobileService {

    private BizStoreManager bizStoreManager;

    @Autowired
    public TokenMobileService(BizStoreManager bizStoreManager) {
        this.bizStoreManager = bizStoreManager;
    }

    public BizStoreEntity findByCodeQR(String codeQR) {
        return bizStoreManager.findByCodeQR(codeQR);
    }

    public boolean isValidCodeQR(String codeQR) {
        return bizStoreManager.isValidCodeQR(codeQR);
    }
}
