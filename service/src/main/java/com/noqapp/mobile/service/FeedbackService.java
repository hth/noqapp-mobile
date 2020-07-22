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

    private String feedbackLink;

    private AccountMobileService accountMobileService;
    private JMSProducerService jmsProducerService;
    private WebConnectorService webConnectorService;

    private String buildEnvironment;

    @Autowired
    public FeedbackService(
        @Value("${feedback:/webapi/mobile/mail/feedback.htm}")
        String feedbackLink,

        Environment environment,
        AccountMobileService accountMobileService,
        JMSProducerService jmsProducerService,
        WebConnectorService webConnectorService
    ) {
        this.feedbackLink = feedbackLink;

        this.buildEnvironment = environment.getProperty("deployed.server");

        this.accountMobileService = accountMobileService;
        this.jmsProducerService = jmsProducerService;
        this.webConnectorService = webConnectorService;
    }

    public boolean submitFeedback(String qid, Feedback feedback) {
        UserAccountEntity userAccount = accountMobileService.findByQueueUserId(qid);
        if (buildEnvironment.equalsIgnoreCase("standalone-mobile")) {
            jmsProducerService.invokeMailOnFeedback(
                userAccount.getUserId(),
                qid,
                userAccount.getName(),
                feedback.getSubject().getText(),
                getFeedbackBody(feedback)
            );
            return true;
        } else {
            boolean mailStatus = sendMailFeedback(
                userAccount.getUserId(),
                qid,
                userAccount.getName(),
                feedback,
                HttpClientBuilder.create().build());

            LOG.info("mail sent={} to user={}", mailStatus, userAccount.getUserId());
            return mailStatus;
        }
    }

    private boolean sendMailFeedback(String userId, String qid, String name, Feedback feedback, HttpClient httpClient) {
        LOG.debug("userId={} name={} webApiAccessToken={}", userId, name, AUTH_KEY_HIDDEN);
        HttpPost httpPost = webConnectorService.getHttpPost(feedbackLink, httpClient);
        if (null == httpPost) {
            LOG.warn("failed connecting, reason={}", webConnectorService.getNoResponseFromWebServer());
            return false;
        }

        String body = getFeedbackBody(feedback);
        FeedbackMail feedbackMail = FeedbackMail.newInstance(userId, qid, name, feedback.getSubject().getText(), body);
        webConnectorService.setEntityWithGson(feedbackMail, httpPost);
        return webConnectorService.invokeHttpPost(httpClient, httpPost);
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
