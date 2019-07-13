package com.noqapp.mobile.service;

import com.noqapp.domain.PurchaseOrderEntity;
import com.noqapp.domain.QueueEntity;
import com.noqapp.domain.json.JsonPurchaseOrder;
import com.noqapp.repository.QueueManager;
import com.noqapp.repository.QueueManagerJDBC;
import com.noqapp.service.PurchaseOrderProductService;
import com.noqapp.service.PurchaseOrderService;

import org.apache.commons.lang3.StringUtils;

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

    private QueueManager queueManager;
    private QueueManagerJDBC queueManagerJDBC;
    private PurchaseOrderService purchaseOrderService;
    private PurchaseOrderProductService purchaseOrderProductService;

    @Autowired
    public PurchaseOrderMobileService(
        QueueManager queueManager,
        QueueManagerJDBC queueManagerJDBC,
        PurchaseOrderService purchaseOrderService,
        PurchaseOrderProductService purchaseOrderProductService
    ) {
        this.queueManager = queueManager;
        this.queueManagerJDBC = queueManagerJDBC;
        this.purchaseOrderService = purchaseOrderService;
        this.purchaseOrderProductService = purchaseOrderProductService;
    }

    /**
     * Since review can be done in background. Moved logic to thread.
     */
    public boolean reviewService(String codeQR, int token, String did, String qid, int ratingCount, String review) {
        //TODO(hth) when adding new review increase ratingCount. Make sure when editing review, do not increase count.
        return purchaseOrderService.reviewService(codeQR, token, did, qid, ratingCount, review);
    }

    public JsonPurchaseOrder findQueueThatHasTransaction(String codeQR, String qid, int token) {
        boolean historical = false;
        QueueEntity queue = queueManager.findQueueThatHasTransaction(codeQR, qid, token);
        if (null == queue) {
            queue = queueManagerJDBC.findQueueThatHasTransaction(codeQR, qid, token);
            historical = true;
        }

        if (queue == null || StringUtils.isBlank(queue.getTransactionId())) {
            return null;
        }

        JsonPurchaseOrder jsonPurchaseOrder;
        if (historical) {
            PurchaseOrderEntity purchaseOrder = purchaseOrderService.findHistoricalPurchaseOrder(qid, queue.getTransactionId());
            jsonPurchaseOrder = purchaseOrderProductService.populateHistoricalJsonPurchaseOrder(purchaseOrder);
        } else {
            PurchaseOrderEntity purchaseOrder = purchaseOrderService.findByTransactionId(queue.getTransactionId());
            jsonPurchaseOrder = purchaseOrderProductService.populateJsonPurchaseOrder(purchaseOrder);
        }

        LOG.debug("Found purchase order for {} {} {}", codeQR, qid, jsonPurchaseOrder);
        return jsonPurchaseOrder;
    }
}
