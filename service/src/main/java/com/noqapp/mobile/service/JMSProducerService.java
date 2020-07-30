package com.noqapp.mobile.service;

import com.noqapp.domain.UserAccountEntity;
import com.noqapp.domain.jms.ChangeMailOTP;
import com.noqapp.domain.jms.FeedbackMail;
import com.noqapp.domain.jms.ReviewSentiment;
import com.noqapp.domain.jms.SignupUserInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

/**
 * hitender
 * 7/20/20 7:41 PM
 */
@Service
public class JMSProducerService {
    private String activemqDestinationMailSignUp;
    private String activemqDestinationMailChange;
    private String activemqDestinationFeedback;
    private String activemqDestinationReviewNegative;

    private JmsTemplate jmsMailSignUpTemplate;
    private JmsTemplate jmsMailChangeTemplate;
    private JmsTemplate jmsFeedbackTemplate;
    private JmsTemplate jmsReviewNegativeTemplate;

    @Autowired
    public JMSProducerService(
        @Value("${activemq.destination.mail.signup}")
        String activemqDestinationMailSignUp,

        @Value("${activemq.destination.mail.change}")
        String activemqDestinationMailChange,

        @Value("${activemq.destination.feedback}")
        String activemqDestinationFeedback,

        @Value("${activemq.destination.review.negative}")
        String activemqDestinationReviewNegative,

        @Qualifier("jmsMailSignUpTemplate")
        JmsTemplate jmsMailSignUpTemplate,

        @Qualifier("jmsMailChangeTemplate")
        JmsTemplate jmsMailChangeTemplate,

        @Qualifier("jmsFeedbackTemplate")
        JmsTemplate jmsFeedbackTemplate,

        @Qualifier("jmsReviewNegativeTemplate")
        JmsTemplate jmsReviewNegativeTemplate
    ) {
        this.activemqDestinationMailSignUp = activemqDestinationMailSignUp;
        this.activemqDestinationMailChange = activemqDestinationMailChange;
        this.activemqDestinationFeedback = activemqDestinationFeedback;
        this.activemqDestinationReviewNegative = activemqDestinationReviewNegative;

        this.jmsMailSignUpTemplate = jmsMailSignUpTemplate;
        this.jmsMailChangeTemplate = jmsMailChangeTemplate;
        this.jmsFeedbackTemplate = jmsFeedbackTemplate;
        this.jmsReviewNegativeTemplate = jmsReviewNegativeTemplate;
    }

    public void invokeMailOnSignUp(UserAccountEntity userAccount) {
        SignupUserInfo signupUserInfo = SignupUserInfo.newInstance(userAccount.getUserId(), userAccount.getQueueUserId(), userAccount.getName());
        jmsMailSignUpTemplate.send(activemqDestinationMailSignUp, session -> session.createObjectMessage(signupUserInfo));
    }

    public void invokeMailOnMailChange(String changedMail, String name, String mailOTP) {
        ChangeMailOTP changeMailOTP = ChangeMailOTP.newInstance(changedMail, name, mailOTP);
        jmsMailChangeTemplate.send(activemqDestinationMailChange, session -> session.createObjectMessage(changeMailOTP));
    }

    public void invokeMailOnFeedback(String userId, String qid, String name, String subject, String body) {
        FeedbackMail feedbackMail = FeedbackMail.newInstance(userId, qid, name, subject, body);
        jmsFeedbackTemplate.send(activemqDestinationFeedback, session -> session.createObjectMessage(feedbackMail));
    }

    public void invokeMailOnReviewNegative(
        String storeName,
        String reviewerName,
        String reviewerPhone,
        int ratingCount,
        int hourSaved,
        String review,
        String sentiment,
        String sentimentWatcherEmail
    ) {
        ReviewSentiment reviewSentiment = ReviewSentiment.newInstance(storeName, reviewerName, reviewerPhone, ratingCount, hourSaved, review, sentiment, sentimentWatcherEmail);
        jmsReviewNegativeTemplate.send(activemqDestinationReviewNegative, session -> session.createObjectMessage(reviewSentiment));
    }
}
