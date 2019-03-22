package com.noqapp.mobile.domain;

import com.noqapp.common.utils.AbstractDomain;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.ScheduledTaskEntity;
import com.noqapp.domain.StoreHourEntity;
import com.noqapp.domain.types.ActionTypeEnum;
import com.noqapp.domain.types.ServicePaymentEnum;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * User: hitender
 * Date: 6/15/17 9:03 AM
 */
@SuppressWarnings ({
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
//@JsonInclude (JsonInclude.Include.NON_NULL)
public class JsonModifyQueue extends AbstractDomain {

    @JsonProperty ("qr")
    private String codeQR;

    @JsonProperty ("f")
    private int tokenAvailableFrom;

    /* Store business start hour. */
    @JsonProperty ("b")
    private int startHour;

    @JsonProperty ("m")
    private int tokenNotAvailableFrom;

    /* Store business end hour. */
    @JsonProperty ("e")
    private int endHour;

    @JsonProperty ("de")
    private int delayedInMinutes;

    @JsonProperty ("dc")
    private boolean dayClosed = false;

    @JsonProperty("tc")
    private boolean tempDayClosed;

    @JsonProperty ("pj")
    private boolean preventJoining;

    @JsonProperty ("at")
    private int availableTokenCount;

    @JsonProperty ("fr")
    private String fromDay;

    @JsonProperty ("un")
    private String untilDay;

    @JsonProperty ("scFr")
    private String scheduledFromDay;

    @JsonProperty ("scUn")
    private String scheduledUntilDay;

    @JsonProperty("ep")
    private boolean enabledPayment;

    @JsonProperty("pp")
    private int productPrice;

    @JsonProperty("cp")
    private int cancellationPrice;

    @JsonProperty("sp")
    private ServicePaymentEnum servicePayment;

    @JsonProperty("sa")
    private ActionTypeEnum storeActionType;

    public JsonModifyQueue() {
    }

    public JsonModifyQueue(
        String codeQR,
        StoreHourEntity storeHour,
        int availableTokenCount,
        ActionTypeEnum storeActionType,
        BizStoreEntity bizStore,
        ScheduledTaskEntity scheduledTask
    ) {
        this.codeQR = codeQR;
        this.tokenAvailableFrom = storeHour.getTokenAvailableFrom();
        this.startHour = storeHour.getStartHour();
        this.tokenNotAvailableFrom = storeHour.getTokenNotAvailableFrom();
        this.endHour = storeHour.getEndHour();
        this.delayedInMinutes = storeHour.getDelayedInMinutes();
        this.dayClosed = storeHour.isDayClosed();
        this.tempDayClosed = storeHour.isTempDayClosed();
        this.preventJoining = storeHour.isPreventJoining();
        this.availableTokenCount = availableTokenCount;
        this.storeActionType = storeActionType;
        this.enabledPayment = bizStore.isEnabledPayment();
        this.productPrice = bizStore.getProductPrice();
        this.cancellationPrice = bizStore.getCancellationPrice();
        this.servicePayment = bizStore.getServicePayment();

        if (null != scheduledTask) {
            scheduledFromDay = scheduledTask.getFrom();
            scheduledUntilDay = scheduledTask.getUntil();
        }
    }

    public JsonModifyQueue(
        String codeQR,
        StoreHourEntity storeHour,
        BizStoreEntity bizStore,
        ScheduledTaskEntity scheduledTask
    ) {
        this.codeQR = codeQR;
        this.tokenAvailableFrom = storeHour.getTokenAvailableFrom();
        this.startHour = storeHour.getStartHour();
        this.tokenNotAvailableFrom = storeHour.getTokenNotAvailableFrom();
        this.endHour = storeHour.getEndHour();
        this.delayedInMinutes = storeHour.getDelayedInMinutes();
        this.dayClosed = storeHour.isDayClosed();
        this.tempDayClosed = storeHour.isTempDayClosed();
        this.preventJoining = storeHour.isPreventJoining();
        this.availableTokenCount = bizStore.getAvailableTokenCount();
        this.storeActionType = bizStore.isActive() ? ActionTypeEnum.ACTIVE : ActionTypeEnum.INACTIVE;
        this.enabledPayment = bizStore.isEnabledPayment();
        this.productPrice = bizStore.getProductPrice();
        this.cancellationPrice = bizStore.getCancellationPrice();
        this.servicePayment = bizStore.getServicePayment();

        if (null != scheduledTask) {
            scheduledFromDay = scheduledTask.getFrom();
            scheduledUntilDay = scheduledTask.getUntil();
        }
    }

    public String getCodeQR() {
        return codeQR;
    }

    public JsonModifyQueue setCodeQR(String codeQR) {
        this.codeQR = codeQR;
        return this;
    }

    public int getTokenAvailableFrom() {
        return tokenAvailableFrom;
    }

    public JsonModifyQueue setTokenAvailableFrom(int tokenAvailableFrom) {
        this.tokenAvailableFrom = tokenAvailableFrom;
        return this;
    }

    public int getStartHour() {
        return startHour;
    }

    public JsonModifyQueue setStartHour(int startHour) {
        this.startHour = startHour;
        return this;
    }

    public int getTokenNotAvailableFrom() {
        return tokenNotAvailableFrom;
    }

    public JsonModifyQueue setTokenNotAvailableFrom(int tokenNotAvailableFrom) {
        this.tokenNotAvailableFrom = tokenNotAvailableFrom;
        return this;
    }

    public int getEndHour() {
        return endHour;
    }

    public JsonModifyQueue setEndHour(int endHour) {
        this.endHour = endHour;
        return this;
    }

    public int getDelayedInMinutes() {
        return delayedInMinutes;
    }

    public JsonModifyQueue setDelayedInMinutes(int delayedInMinutes) {
        this.delayedInMinutes = delayedInMinutes;
        return this;
    }

    public boolean isDayClosed() {
        return dayClosed;
    }

    public JsonModifyQueue setDayClosed(boolean dayClosed) {
        this.dayClosed = dayClosed;
        return this;
    }

    public boolean isTempDayClosed() {
        return tempDayClosed;
    }

    public JsonModifyQueue setTempDayClosed(boolean tempDayClosed) {
        this.tempDayClosed = tempDayClosed;
        return this;
    }

    public boolean isPreventJoining() {
        return preventJoining;
    }

    public JsonModifyQueue setPreventJoining(boolean preventJoining) {
        this.preventJoining = preventJoining;
        return this;
    }

    public int getAvailableTokenCount() {
        return availableTokenCount;
    }

    public JsonModifyQueue setAvailableTokenCount(int availableTokenCount) {
        this.availableTokenCount = availableTokenCount;
        return this;
    }

    public String getFromDay() {
        return fromDay;
    }

    public JsonModifyQueue setFromDay(String fromDay) {
        this.fromDay = fromDay;
        return this;
    }

    public String getUntilDay() {
        return untilDay;
    }

    public JsonModifyQueue setUntilDay(String untilDay) {
        this.untilDay = untilDay;
        return this;
    }

    public ActionTypeEnum getStoreActionType() {
        return storeActionType;
    }

    public JsonModifyQueue setStoreActionType(ActionTypeEnum storeActionType) {
        this.storeActionType = storeActionType;
        return this;
    }

    public boolean isEnabledPayment() {
        return enabledPayment;
    }

    public JsonModifyQueue setEnabledPayment(boolean enabledPayment) {
        this.enabledPayment = enabledPayment;
        return this;
    }

    public int getProductPrice() {
        return productPrice;
    }

    public JsonModifyQueue setProductPrice(int productPrice) {
        this.productPrice = productPrice;
        return this;
    }

    public int getCancellationPrice() {
        return cancellationPrice;
    }

    public JsonModifyQueue setCancellationPrice(int cancellationPrice) {
        this.cancellationPrice = cancellationPrice;
        return this;
    }

    public ServicePaymentEnum getServicePayment() {
        return servicePayment;
    }

    public JsonModifyQueue setServicePayment(ServicePaymentEnum servicePayment) {
        this.servicePayment = servicePayment;
        return this;
    }

    @Override
    public String toString() {
        return "JsonModifyQueue{" +
            "codeQR='" + codeQR + '\'' +
            ", tokenAvailableFrom=" + tokenAvailableFrom +
            ", startHour=" + startHour +
            ", tokenNotAvailableFrom=" + tokenNotAvailableFrom +
            ", endHour=" + endHour +
            ", delayedInMinutes=" + delayedInMinutes +
            ", dayClosed=" + dayClosed +
            ", tempDayClosed=" + tempDayClosed +
            ", preventJoining=" + preventJoining +
            ", availableTokenCount=" + availableTokenCount +
            ", fromDay='" + fromDay + '\'' +
            ", untilDay='" + untilDay + '\'' +
            ", scheduledFromDay='" + scheduledFromDay + '\'' +
            ", scheduledUntilDay='" + scheduledUntilDay + '\'' +
            ", enabledPayment=" + enabledPayment +
            ", productPrice=" + productPrice +
            ", cancellationPrice=" + cancellationPrice +
            ", servicePayment=" + servicePayment +
            ", storeActionType=" + storeActionType +
            '}';
    }
}
