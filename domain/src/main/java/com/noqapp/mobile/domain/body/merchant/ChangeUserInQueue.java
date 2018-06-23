package com.noqapp.mobile.domain.body.merchant;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.noqapp.common.utils.AbstractDomain;

/**
 * User: hitender
 * Date: 6/23/18 8:34 AM
 */
@SuppressWarnings({
        "PMD.BeanMembersShouldSerialize",
        "PMD.LocalVariableCouldBeFinal",
        "PMD.MethodArgumentCouldBeFinal",
        "PMD.LongVariable",
        "unused"
})
@JsonAutoDetect (
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
)
@JsonPropertyOrder (alphabetic = true)
@JsonIgnoreProperties (ignoreUnknown = true)
public class ChangeUserInQueue extends AbstractDomain {

    @JsonProperty ("eqid")
    private String existingQueueUserId;

    @JsonProperty("cqid")
    private String changeToQueueUserId;

    @JsonProperty("qr")
    private String codeQR;

    @JsonProperty ("t")
    private int tokenNumber;

    public String getExistingQueueUserId() {
        return existingQueueUserId;
    }

    public ChangeUserInQueue setExistingQueueUserId(String existingQueueUserId) {
        this.existingQueueUserId = existingQueueUserId;
        return this;
    }

    public String getChangeToQueueUserId() {
        return changeToQueueUserId;
    }

    public ChangeUserInQueue setChangeToQueueUserId(String changeToQueueUserId) {
        this.changeToQueueUserId = changeToQueueUserId;
        return this;
    }

    public String getCodeQR() {
        return codeQR;
    }

    public ChangeUserInQueue setCodeQR(String codeQR) {
        this.codeQR = codeQR;
        return this;
    }

    public int getTokenNumber() {
        return tokenNumber;
    }

    public ChangeUserInQueue setTokenNumber(int tokenNumber) {
        this.tokenNumber = tokenNumber;
        return this;
    }
}
