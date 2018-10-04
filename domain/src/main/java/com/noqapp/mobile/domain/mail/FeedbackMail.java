package com.noqapp.mobile.domain.mail;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.google.gson.annotations.SerializedName;

/**
 * User: hitender
 * Date: 10/4/18 9:19 PM
 */
@SuppressWarnings ({
        "PMD.BeanMembersShouldSerialize",
        "PMD.LocalVariableCouldBeFinal",
        "PMD.MethodArgumentCouldBeFinal",
        "PMD.LongVariable"
})
@JsonPropertyOrder(alphabetic = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FeedbackMail {
    @SuppressWarnings ({"unused"})
    @SerializedName("userId")
    private String userId;

    @SuppressWarnings ({"unused"})
    @SerializedName ("qid")
    private String qid;

    @SuppressWarnings ({"unused"})
    @SerializedName ("name")
    private String name;

    @SuppressWarnings ({"unused"})
    @SerializedName ("subject")
    private String subject;
    
    @SuppressWarnings ({"unused"})
    @SerializedName ("body")
    private String body;

    private FeedbackMail(String userId, String qid, String name, String subject, String body) {
        this.userId = userId;
        this.qid = qid;
        this.name = name;
        this.subject = subject;
        this.body = body;
    }

    public static FeedbackMail newInstance(String userId, String qid, String name, String subject, String body) {
        return new FeedbackMail(userId, qid, name, subject, body);
    }
}
