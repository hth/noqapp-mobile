package com.noqapp.mobile.service;

import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;

import com.noqapp.domain.UserAccountEntity;
import com.noqapp.domain.jms.FeedbackMail;
import com.noqapp.mobile.domain.body.client.Feedback;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
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
    private JMSProducerService jmsProducerService;

    @Autowired
    public FeedbackService(
        AccountMobileService accountMobileService,
        JMSProducerService jmsProducerService
    ) {

        this.accountMobileService = accountMobileService;
        this.jmsProducerService = jmsProducerService;
    }

    public void submitFeedback(String qid, Feedback feedback) {
        UserAccountEntity userAccount = accountMobileService.findByQueueUserId(qid);
        jmsProducerService.invokeMailOnFeedback(
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
