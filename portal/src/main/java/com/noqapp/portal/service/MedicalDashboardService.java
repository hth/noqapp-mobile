package com.noqapp.portal.service;

import com.noqapp.domain.json.JsonQueueList;
import com.noqapp.domain.json.JsonQueuePersonList;
import com.noqapp.mobile.service.TokenQueueMobileService;
import com.noqapp.portal.domain.InstantViewDashboard;
import com.noqapp.repository.QueueManager;
import com.noqapp.service.QueueService;

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
    private QueueService queueService;

    @Autowired
    public MedicalDashboardService(QueueManager queueManager, TokenQueueMobileService tokenQueueMobileService, QueueService queueService) {
        this.queueManager = queueManager;
        this.tokenQueueMobileService = tokenQueueMobileService;
        this.queueService = queueService;
    }

    public InstantViewDashboard populateInstantView(String bizNameId) {
        InstantViewDashboard instantViewDashboard = new InstantViewDashboard();
        Set<String> codeQRs = this.queueManager.filterByDistinctCodeQR(bizNameId);

        JsonQueueList jsonQueueList = currentUsersInQueue(codeQRs);
        instantViewDashboard.setJsonQueueList(jsonQueueList);

        return instantViewDashboard;
    }

    public JsonQueuePersonList findAllClient(String bizNameId) {
        Set<String> codeQRs = this.queueManager.filterByDistinctCodeQR(bizNameId);
        return findAllClient(codeQRs);
    }

    private JsonQueueList currentUsersInQueue(Set<String> codeQRs) {
        JsonQueueList jsonQueueList = new JsonQueueList();
        for (String codeQR : codeQRs) {
            jsonQueueList.getQueues().addAll(tokenQueueMobileService.findAllTokenState(codeQR).getQueues());
        }

        return jsonQueueList;
    }

    private JsonQueuePersonList findAllClient(Set<String> codeQRs) {
        JsonQueuePersonList jsonQueuePersonList = new JsonQueuePersonList();
        for (String codeQR : codeQRs) {
            jsonQueuePersonList.getQueuedPeople().addAll(queueService.findAllClient(codeQR).getQueuedPeople());
        }

        return jsonQueuePersonList;
    }
}
