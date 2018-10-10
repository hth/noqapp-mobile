package com.noqapp.mobile.service;

import static java.util.concurrent.Executors.newCachedThreadPool;

import com.noqapp.repository.PurchaseOrderManager;
import com.noqapp.repository.PurchaseOrderManagerJDBC;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;

/**
 * hitender
 * 10/9/18 1:01 PM
 */
@Service
public class PurchaseOrderMobileService {
    private static final Logger LOG = LoggerFactory.getLogger(PurchaseOrderMobileService.class);

    private PurchaseOrderManager purchaseOrderManager;
    private PurchaseOrderManagerJDBC purchaseOrderManagerJDBC;

    private ExecutorService executorService;

    @Autowired
    public PurchaseOrderMobileService(
        PurchaseOrderManager purchaseOrderManager,
        PurchaseOrderManagerJDBC purchaseOrderManagerJDBC
    ) {
        this.purchaseOrderManager = purchaseOrderManager;
        this.purchaseOrderManagerJDBC = purchaseOrderManagerJDBC;

        this.executorService = newCachedThreadPool();
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
        executorService.submit(() -> reviewingService(codeQR, token, did, qid, ratingCount, review));
        return true;
    }

    /**
     * Submitting review.
     */
    private void reviewingService(String codeQR, int token, String did, String qid, int ratingCount, String review) {
        boolean reviewSubmitStatus = purchaseOrderManager.reviewService(codeQR, token, did, qid, ratingCount, review);
        if (!reviewSubmitStatus) {
            //TODO(hth) make sure for Guardian this is taken care. Right now its ignore "GQ" add to MySQL Table
            reviewSubmitStatus = reviewHistoricalService(codeQR, token, did, qid, ratingCount, review);
        }

        LOG.info("Review update status={} codeQR={} token={} ratingCount={} hoursSaved={} did={} qid={} review={}",
            reviewSubmitStatus,
            codeQR,
            token,
            ratingCount,
            did,
            qid,
            review);
    }

    private boolean reviewHistoricalService(
        String codeQR,
        int token,
        String did,
        String qid,
        int ratingCount,
        String review
    ) {
        return purchaseOrderManagerJDBC.reviewService(codeQR, token, did, qid, ratingCount, review);
    }
}
