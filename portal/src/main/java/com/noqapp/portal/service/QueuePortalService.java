package com.noqapp.portal.service;

import com.noqapp.domain.QueueEntity;
import com.noqapp.repository.QueueManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * hitender
 * 3/24/20 1:04 PM
 */
@SuppressWarnings({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@Service
public class QueuePortalService {
    private static final Logger LOG = LoggerFactory.getLogger(QueuePortalService.class);

    private QueueManager queueManager;

    @Autowired
    public QueuePortalService(QueueManager queueManager) {
        this.queueManager = queueManager;
    }

    public String currentUsersInQueue(String bizNameId) {
        List<QueueEntity> queues = this.queueManager.findByBizNameId(bizNameId);


        return "";
    }
}
