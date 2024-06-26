package com.noqapp.mobile.domain.body.merchant;

import com.noqapp.common.utils.AbstractDomain;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.shared.GeoPointOfQ;
import com.noqapp.domain.types.PurchaseOrderStateEnum;
import com.noqapp.domain.types.QueueStatusEnum;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * hitender
 * 8/7/18 11:37 PM
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
public class OrderServed extends AbstractDomain {
    @JsonProperty("qr")
    private ScrubbedInput codeQR;

    @JsonProperty("t")
    private int servedNumber;

    @JsonProperty ("ti")
    private String transactionId;

    @JsonProperty("qid")
    private String queueUserId;

    @JsonProperty("p")
    private PurchaseOrderStateEnum purchaseOrderState;

    @JsonProperty("s")
    private QueueStatusEnum queueStatus;

    @JsonProperty("g")
    private ScrubbedInput goTo;

    @JsonProperty("cor")
    private GeoPointOfQ geoPointOfQ;

    public ScrubbedInput getCodeQR() {
        return codeQR;
    }

    public void setCodeQR(ScrubbedInput codeQR) {
        this.codeQR = codeQR;
    }

    public int getServedNumber() {
        return servedNumber;
    }

    public void setServedNumber(int servedNumber) {
        this.servedNumber = servedNumber;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public OrderServed setTransactionId(String transactionId) {
        this.transactionId = transactionId;
        return this;
    }

    public String getQueueUserId() {
        return queueUserId;
    }

    public OrderServed setQueueUserId(String queueUserId) {
        this.queueUserId = queueUserId;
        return this;
    }

    public PurchaseOrderStateEnum getPurchaseOrderState() {
        return purchaseOrderState;
    }

    public OrderServed setPurchaseOrderState(PurchaseOrderStateEnum purchaseOrderState) {
        this.purchaseOrderState = purchaseOrderState;
        return this;
    }

    public QueueStatusEnum getQueueStatus() {
        return queueStatus;
    }

    public OrderServed setQueueStatus(QueueStatusEnum queueStatus) {
        this.queueStatus = queueStatus;
        return this;
    }

    public ScrubbedInput getGoTo() {
        return goTo;
    }

    public void setGoTo(ScrubbedInput goTo) {
        this.goTo = goTo;
    }

    public GeoPointOfQ getGeoPointOfQ() {
        return geoPointOfQ;
    }

    public OrderServed setGeoPointOfQ(GeoPointOfQ geoPointOfQ) {
        this.geoPointOfQ = geoPointOfQ;
        return this;
    }

    @Override
    public String toString() {
        return "OrderServed{" +
            "codeQR=" + codeQR +
            ", servedNumber=" + servedNumber +
            ", purchaseOrderState=" + purchaseOrderState +
            ", queueStatus=" + queueStatus +
            ", goTo=" + goTo +
            '}';
    }
}

