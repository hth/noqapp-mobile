package com.noqapp.mobile.domain.body.merchant;

import com.noqapp.common.utils.AbstractDomain;
import com.noqapp.common.utils.ScrubbedInput;

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
    private ScrubbedInput couponId;

    @JsonProperty ("ti")
    private ScrubbedInput transactionId;

    @JsonProperty("qid")
    private ScrubbedInput queueUserId;

    @JsonProperty("qr")
    private ScrubbedInput codeQR;

    public ScrubbedInput getCouponId() {
        return couponId;
    }

    public CouponOnOrder setCouponId(ScrubbedInput couponId) {
        this.couponId = couponId;
        return this;
    }

    public ScrubbedInput getTransactionId() {
        return transactionId;
    }

    public CouponOnOrder setTransactionId(ScrubbedInput transactionId) {
        this.transactionId = transactionId;
        return this;
    }

    public ScrubbedInput getQueueUserId() {
        return queueUserId;
    }

    public CouponOnOrder setQueueUserId(ScrubbedInput queueUserId) {
        this.queueUserId = queueUserId;
        return this;
    }

    public ScrubbedInput getCodeQR() {
        return codeQR;
    }

    public CouponOnOrder setCodeQR(ScrubbedInput codeQR) {
        this.codeQR = codeQR;
        return this;
    }
}
