package com.noqapp.mobile.domain.body.merchant;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.noqapp.common.utils.AbstractDomain;
import com.noqapp.domain.types.QueueStatusEnum;
import com.noqapp.domain.types.QueueUserStateEnum;

/**
 * hitender
 * 12/11/17 11:35 AM
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
public class Served extends AbstractDomain {
    @JsonProperty("c")
    private String codeQR;

    @JsonProperty("t")
    private int servedNumber;

    @JsonProperty("q")
    private QueueUserStateEnum queueUserState;

    @JsonProperty("s")
    private QueueStatusEnum queueStatus;

    @JsonProperty("g")
    private String goTo;

    public String getCodeQR() {
        return codeQR;
    }

    public Served setCodeQR(String codeQR) {
        this.codeQR = codeQR;
        return this;
    }

    public int getServedNumber() {
        return servedNumber;
    }

    public Served setServedNumber(int servedNumber) {
        this.servedNumber = servedNumber;
        return this;
    }

    public QueueUserStateEnum getQueueUserState() {
        return queueUserState;
    }

    public Served setQueueUserState(QueueUserStateEnum queueUserState) {
        this.queueUserState = queueUserState;
        return this;
    }

    public QueueStatusEnum getQueueStatus() {
        return queueStatus;
    }

    public Served setQueueStatus(QueueStatusEnum queueStatus) {
        this.queueStatus = queueStatus;
        return this;
    }

    public String getGoTo() {
        return goTo;
    }

    public Served setGoTo(String goTo) {
        this.goTo = goTo;
        return this;
    }
}