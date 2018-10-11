package com.noqapp.mobile.service;

import com.noqapp.domain.json.JsonReviewList;
import com.noqapp.service.PurchaseOrderService;
import com.noqapp.service.ReviewService;

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
     *
     * @param codeQR
     * @param token
     * @param did
     * @param qid
     * @param ratingCount
     */
    public boolean reviewService(String codeQR, int token, String did, String qid, int ratingCount, String review) {
        return purchaseOrderService.reviewService(codeQR, token, did, qid, ratingCount, review);
    }
}
