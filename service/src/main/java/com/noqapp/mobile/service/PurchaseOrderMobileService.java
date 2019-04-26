package com.noqapp.mobile.service;

import com.noqapp.service.PurchaseOrderService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * hitender
 * 10/9/18 1:01 PM
 */
@Service
public class PurchaseOrderMobileService {
    private static final Logger LOG = LoggerFactory.getLogger(PurchaseOrderMobileService.class);

    private PurchaseOrderService purchaseOrderService;

    @Autowired
    public PurchaseOrderMobileService(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    /**
     * Since review can be done in background. Moved logic to thread.
     */
    public boolean reviewService(String codeQR, int token, String did, String qid, int ratingCount, String review) {
        //TODO(hth) when adding new review increase ratingCount. Make sure when editing review, do not increase count.
        return purchaseOrderService.reviewService(codeQR, token, did, qid, ratingCount, review);
    }
}
