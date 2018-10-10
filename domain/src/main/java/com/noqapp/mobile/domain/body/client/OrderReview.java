package com.noqapp.mobile.domain.body.client;

import com.noqapp.common.utils.AbstractDomain;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * hitender
 * 10/9/18 11:13 AM
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
public class OrderReview extends AbstractDomain {
    @JsonProperty("codeQR")
    private String codeQR;

    @JsonProperty("t")
    private int token;

    @JsonProperty("ra")
    private int ratingCount;

    @JsonProperty("rv")
    private String review;

    public String getCodeQR() {
        return codeQR;
    }

    public OrderReview setCodeQR(String codeQR) {
        this.codeQR = codeQR;
        return this;
    }

    public int getToken() {
        return token;
    }

    public OrderReview setToken(int token) {
        this.token = token;
        return this;
    }

    public int getRatingCount() {
        return ratingCount;
    }

    public OrderReview setRatingCount(int ratingCount) {
        this.ratingCount = ratingCount;
        return this;
    }

    public String getReview() {
        return review;
    }

    public OrderReview setReview(String review) {
        this.review = review;
        return this;
    }
}
