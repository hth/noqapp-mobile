package com.noqapp.mobile.domain.body.client;

import com.noqapp.common.utils.AbstractDomain;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * hitender
 * 6/19/18 10:25 AM
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
public class JoinQueue extends AbstractDomain {

    @JsonProperty("qid")
    private String queueUserId;

    @JsonProperty("gq")
    private String guardianQid;

    @JsonProperty("qr")
    private String codeQR;

    public String getQueueUserId() {
        return queueUserId;
    }

    public JoinQueue setQueueUserId(String queueUserId) {
        this.queueUserId = queueUserId;
        return this;
    }

    public String getGuardianQid() {
        return guardianQid;
    }

    public JoinQueue setGuardianQid(String guardianQid) {
        this.guardianQid = guardianQid;
        return this;
    }

    public String getCodeQR() {
        return codeQR;
    }

    public JoinQueue setCodeQR(String codeQR) {
        this.codeQR = codeQR;
        return this;
    }
}
