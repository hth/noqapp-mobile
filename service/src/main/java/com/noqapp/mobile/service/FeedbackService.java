package com.noqapp.mobile.service;

import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;

import com.noqapp.domain.UserAccountEntity;
import com.noqapp.mobile.domain.body.client.Feedback;
import com.noqapp.mobile.domain.mail.FeedbackMail;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    private WebConnectorService webConnectorService;

    @Autowired
    public FeedbackService(
        @Value("${feedback:/webapi/mobile/mail/feedback.htm}")
        String feedbackLink,

        AccountMobileService accountMobileService,
        WebConnectorService webConnectorService
    ) {
        this.feedbackLink = feedbackLink;

        this.accountMobileService = accountMobileService;
        this.webConnectorService = webConnectorService;
    }

    public boolean submitFeedback(String qid, Feedback feedback) {
        UserAccountEntity userAccount = accountMobileService.findByQueueUserId(qid);
        boolean mailStatus = sendMailFeedback(
            userAccount.getUserId(),
            qid,
            userAccount.getName(),
            feedback,
            HttpClientBuilder.create().build());

        LOG.info("mail sent={} to user={}", mailStatus, userAccount.getUserId());
        return mailStatus;
    }

    private boolean sendMailFeedback(String userId, String qid, String name, Feedback feedback, HttpClient httpClient) {
        LOG.debug("userId={} name={} webApiAccessToken={}", userId, name, AUTH_KEY_HIDDEN);
        HttpPost httpPost = webConnectorService.getHttpPost(feedbackLink, httpClient);
        if (null == httpPost) {
            LOG.warn("failed connecting, reason={}", webConnectorService.getNoResponseFromWebServer());
            return false;
        }

        String body;
        if (null != feedback.getCodeQR() && StringUtils.isNotBlank(feedback.getCodeQR().getText())) {
            body = "QR: " + feedback.getCodeQR().getText() + "; " +
                "Order(O)/Queue(Q): " + feedback.getMessageOrigin() + "; " +
                "Token No.: " + feedback.getToken() + "; " +
                "Message: " + feedback.getBody().getText();
        } else {
            body = feedback.getBody().getText();
        }
        FeedbackMail feedbackMail = FeedbackMail.newInstance(userId, qid, name, feedback.getSubject().getText(), body);
        webConnectorService.setEntityWithGson(feedbackMail, httpPost);
        return webConnectorService.invokeHttpPost(httpClient, httpPost);
    }
}
