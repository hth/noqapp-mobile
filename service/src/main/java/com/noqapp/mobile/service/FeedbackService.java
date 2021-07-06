package com.noqapp.mobile.service;

import com.noqapp.domain.UserAccountEntity;
import com.noqapp.mobile.domain.body.client.Feedback;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * hitender
 * 10/4/18 5:34 PM
 */
@SuppressWarnings ({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@Service
public class FeedbackService {
    private static final Logger LOG = LoggerFactory.getLogger(FeedbackService.class);

    private AccountMobileService accountMobileService;
    private JMSProducerMobileService jmsProducerMobileService;

    @Autowired
    public FeedbackService(
        AccountMobileService accountMobileService,
        JMSProducerMobileService jmsProducerMobileService
    ) {

        this.accountMobileService = accountMobileService;
        this.jmsProducerMobileService = jmsProducerMobileService;
    }

    public void submitFeedback(String qid, Feedback feedback) {
        UserAccountEntity userAccount = accountMobileService.findByQueueUserId(qid);
        jmsProducerMobileService.invokeMailOnFeedback(
            userAccount.getUserId(),
            qid,
            userAccount.getName(),
            feedback.getSubject().getText(),
            getFeedbackBody(feedback)
        );
    }

    private String getFeedbackBody(Feedback feedback) {
        String body;
        if (null != feedback.getCodeQR() && StringUtils.isNotBlank(feedback.getCodeQR().getText())) {
            body = "QR: " + feedback.getCodeQR().getText() + "; " +
                "Order(O)/Queue(Q): " + feedback.getMessageOrigin() + "; " +
                "Token No.: " + feedback.getToken() + "; " +
                "Message: " + feedback.getBody().getText();
        } else {
            body = feedback.getBody().getText();
        }
        return body;
    }
}
