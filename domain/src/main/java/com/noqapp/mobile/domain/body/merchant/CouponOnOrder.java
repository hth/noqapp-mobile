package com.noqapp.mobile.domain.body.merchant;

import com.noqapp.common.utils.AbstractDomain;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * User: hitender
 * Date: 2019-06-10 17:11
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
public class CouponOnOrder extends AbstractDomain {

    @JsonProperty("ci")
    private String couponId;

    @JsonProperty ("ti")
    private String transactionId;

    @JsonProperty("qid")
    private String queueUserId;

    public String getCouponId() {
        return couponId;
    }

    public CouponOnOrder setCouponId(String couponId) {
        this.couponId = couponId;
        return this;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public CouponOnOrder setTransactionId(String transactionId) {
        this.transactionId = transactionId;
        return this;
    }

    public String getQueueUserId() {
        return queueUserId;
    }

    public CouponOnOrder setQueueUserId(String queueUserId) {
        this.queueUserId = queueUserId;
        return this;
    }
}
