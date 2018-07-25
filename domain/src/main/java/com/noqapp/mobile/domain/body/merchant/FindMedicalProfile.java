package com.noqapp.mobile.domain.body.merchant;

import com.noqapp.common.utils.AbstractDomain;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * hitender
 * 7/25/18 5:23 PM
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
public class FindMedicalProfile extends AbstractDomain {

    @JsonProperty("qid")
    private String queueUserId;

    @JsonProperty("qr")
    private String codeQR;

    public String getQueueUserId() {
        return queueUserId;
    }

    public FindMedicalProfile setQueueUserId(String queueUserId) {
        this.queueUserId = queueUserId;
        return this;
    }

    public String getCodeQR() {
        return codeQR;
    }

    public FindMedicalProfile setCodeQR(String codeQR) {
        this.codeQR = codeQR;
        return this;
    }
}
