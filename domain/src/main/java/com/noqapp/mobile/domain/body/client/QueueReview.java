package com.noqapp.mobile.domain.body.client;

import com.noqapp.common.utils.AbstractDomain;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * hitender
 * 12/8/17 12:23 PM
 */
@SuppressWarnings({
        "PMD.BeanMembersShouldSerialize",
        "PMD.LocalVariableCouldBeFinal",
        "PMD.MethodArgumentCouldBeFinal",
        "PMD.LongVariable",
        "unused"
})
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
)
@JsonPropertyOrder(alphabetic = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class QueueReview extends AbstractDomain {
    @JsonProperty("codeQR")
    private String codeQR;

    @JsonProperty("t")
    private int token;

    @JsonProperty("ra")
    private int ratingCount;

    @JsonProperty("hr")
    private int hoursSaved;

    @JsonProperty("rv")
    private String review;

    public String getCodeQR() {
        return codeQR;
    }

    public QueueReview setCodeQR(String codeQR) {
        this.codeQR = codeQR;
        return this;
    }

    public int getToken() {
        return token;
    }

    public QueueReview setToken(int token) {
        this.token = token;
        return this;
    }

    public int getRatingCount() {
        return ratingCount;
    }

    public QueueReview setRatingCount(int ratingCount) {
        this.ratingCount = ratingCount;
        return this;
    }

    public int getHoursSaved() {
        return hoursSaved;
    }

    public QueueReview setHoursSaved(int hoursSaved) {
        this.hoursSaved = hoursSaved;
        return this;
    }

    public String getReview() {
        return review;
    }

    public QueueReview setReview(String review) {
        this.review = review;
        return this;
    }
}
