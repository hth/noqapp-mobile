package com.noqapp.portal.service;

import com.noqapp.domain.json.JsonQueueList;
import com.noqapp.mobile.service.TokenQueueMobileService;
import com.noqapp.portal.domain.InstantViewDashboard;
import com.noqapp.repository.QueueManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * hitender
 * 4/14/20 11:55 AM
 */
@Service
public class MedicalDashboardService {
    private static final Logger LOG = LoggerFactory.getLogger(MedicalDashboardService.class);

    private QueueManager queueManager;
    private TokenQueueMobileService tokenQueueMobileService;

    @Autowired
    public MedicalDashboardService(QueueManager queueManager, TokenQueueMobileService tokenQueueMobileService) {
        this.queueManager = queueManager;
        this.tokenQueueMobileService = tokenQueueMobileService;
    }

    public InstantViewDashboard populateInstantView(String bizNameId) {
        InstantViewDashboard instantViewDashboard = new InstantViewDashboard();
        JsonQueueList jsonQueueList = currentUsersInQueue(bizNameId);
        instantViewDashboard.setJsonQueueList(jsonQueueList);

        return instantViewDashboard;
    }

    private JsonQueueList currentUsersInQueue(String bizNameId) {
        JsonQueueList jsonQueueList = new JsonQueueList();
        Set<String> codeQRs = this.queueManager.filterByDistinctCodeQR(bizNameId);

        for (String codeQR : codeQRs) {
            jsonQueueList.getQueues().addAll(tokenQueueMobileService.findAllTokenState(codeQR).getQueues());
        }

        return jsonQueueList;
    }
}
