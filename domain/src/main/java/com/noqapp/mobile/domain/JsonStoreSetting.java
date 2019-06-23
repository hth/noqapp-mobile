package com.noqapp.mobile.domain;

import com.noqapp.common.utils.AbstractDomain;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.ScheduledTaskEntity;
import com.noqapp.domain.StoreHourEntity;
import com.noqapp.domain.types.ActionTypeEnum;

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
public class JsonStoreSetting extends AbstractDomain {

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

    //*********************************/
    //*  Queue Price Setting Starts.  */
    //*********************************/
    @JsonProperty("ep")
    private boolean enabledPayment;

    @JsonProperty("pp")
    private int productPrice;

    @JsonProperty("cp")
    private int cancellationPrice;
    //*********************************/
    //*  Queue Price Settings Ends.   */
    //*********************************/

    @JsonProperty("fd")
    private int freeFollowupDays;

    @JsonProperty("df")
    private int discountedFollowupDays;

    @JsonProperty("dp")
    private int discountedFollowupProductPrice;

    //******************************************/
    //*  Queue Appointment Setting Starts.     */
    //******************************************/
    @JsonProperty("pe")
    private boolean appointmentEnable;

    @JsonProperty("pd")
    private int appointmentDuration;

    @JsonProperty("pf")
    private int appointmentOpenHowFar;
    //******************************************/
    //*  Queue Appointment Setting Ends.       */
    //******************************************/

    @JsonProperty("sa")
    private ActionTypeEnum storeActionType;

    public JsonStoreSetting() {
    }

    public JsonStoreSetting(
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
        this.freeFollowupDays = bizStore.getFreeFollowupDays();
        this.discountedFollowupDays = bizStore.getDiscountedFollowupDays();
        this.discountedFollowupProductPrice = bizStore.getDiscountedFollowupProductPrice();
        this.appointmentEnable = bizStore.isAppointmentEnable();
        this.appointmentDuration = bizStore.getAppointmentDuration();
        this.appointmentOpenHowFar = bizStore.getAppointmentOpenHowFar();

        if (null != scheduledTask) {
            scheduledFromDay = scheduledTask.getFrom();
            scheduledUntilDay = scheduledTask.getUntil();
        }
    }

    public JsonStoreSetting(
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
        this.freeFollowupDays = bizStore.getFreeFollowupDays();
        this.discountedFollowupDays = bizStore.getDiscountedFollowupDays();
        this.discountedFollowupProductPrice = bizStore.getDiscountedFollowupProductPrice();
        this.appointmentEnable = bizStore.isAppointmentEnable();
        this.appointmentDuration = bizStore.getAppointmentDuration();
        this.appointmentOpenHowFar = bizStore.getAppointmentOpenHowFar();

        if (null != scheduledTask) {
            scheduledFromDay = scheduledTask.getFrom();
            scheduledUntilDay = scheduledTask.getUntil();
        }
    }

    public String getCodeQR() {
        return codeQR;
    }

    public JsonStoreSetting setCodeQR(String codeQR) {
        this.codeQR = codeQR;
        return this;
    }

    public int getTokenAvailableFrom() {
        return tokenAvailableFrom;
    }

    public JsonStoreSetting setTokenAvailableFrom(int tokenAvailableFrom) {
        this.tokenAvailableFrom = tokenAvailableFrom;
        return this;
    }

    public int getStartHour() {
        return startHour;
    }

    public JsonStoreSetting setStartHour(int startHour) {
        this.startHour = startHour;
        return this;
    }

    public int getTokenNotAvailableFrom() {
        return tokenNotAvailableFrom;
    }

    public JsonStoreSetting setTokenNotAvailableFrom(int tokenNotAvailableFrom) {
        this.tokenNotAvailableFrom = tokenNotAvailableFrom;
        return this;
    }

    public int getEndHour() {
        return endHour;
    }

    public JsonStoreSetting setEndHour(int endHour) {
        this.endHour = endHour;
        return this;
    }

    public int getDelayedInMinutes() {
        return delayedInMinutes;
    }

    public JsonStoreSetting setDelayedInMinutes(int delayedInMinutes) {
        this.delayedInMinutes = delayedInMinutes;
        return this;
    }

    public boolean isDayClosed() {
        return dayClosed;
    }

    public JsonStoreSetting setDayClosed(boolean dayClosed) {
        this.dayClosed = dayClosed;
        return this;
    }

    public boolean isTempDayClosed() {
        return tempDayClosed;
    }

    public JsonStoreSetting setTempDayClosed(boolean tempDayClosed) {
        this.tempDayClosed = tempDayClosed;
        return this;
    }

    public boolean isPreventJoining() {
        return preventJoining;
    }

    public JsonStoreSetting setPreventJoining(boolean preventJoining) {
        this.preventJoining = preventJoining;
        return this;
    }

    public int getAvailableTokenCount() {
        return availableTokenCount;
    }

    public JsonStoreSetting setAvailableTokenCount(int availableTokenCount) {
        this.availableTokenCount = availableTokenCount;
        return this;
    }

    public String getFromDay() {
        return fromDay;
    }

    public JsonStoreSetting setFromDay(String fromDay) {
        this.fromDay = fromDay;
        return this;
    }

    public String getUntilDay() {
        return untilDay;
    }

    public JsonStoreSetting setUntilDay(String untilDay) {
        this.untilDay = untilDay;
        return this;
    }

    public ActionTypeEnum getStoreActionType() {
        return storeActionType;
    }

    public JsonStoreSetting setStoreActionType(ActionTypeEnum storeActionType) {
        this.storeActionType = storeActionType;
        return this;
    }

    public boolean isEnabledPayment() {
        return enabledPayment;
    }

    public JsonStoreSetting setEnabledPayment(boolean enabledPayment) {
        this.enabledPayment = enabledPayment;
        return this;
    }

    public int getProductPrice() {
        return productPrice;
    }

    public JsonStoreSetting setProductPrice(int productPrice) {
        this.productPrice = productPrice;
        return this;
    }

    public int getCancellationPrice() {
        return cancellationPrice;
    }

    public JsonStoreSetting setCancellationPrice(int cancellationPrice) {
        this.cancellationPrice = cancellationPrice;
        return this;
    }

    public int getFreeFollowupDays() {
        return freeFollowupDays;
    }

    public JsonStoreSetting setFreeFollowupDays(int freeFollowupDays) {
        this.freeFollowupDays = freeFollowupDays;
        return this;
    }

    public int getDiscountedFollowupDays() {
        return discountedFollowupDays;
    }

    public JsonStoreSetting setDiscountedFollowupDays(int discountedFollowupDays) {
        this.discountedFollowupDays = discountedFollowupDays;
        return this;
    }

    public int getDiscountedFollowupProductPrice() {
        return discountedFollowupProductPrice;
    }

    public JsonStoreSetting setDiscountedFollowupProductPrice(int discountedFollowupProductPrice) {
        this.discountedFollowupProductPrice = discountedFollowupProductPrice;
        return this;
    }

    public boolean isAppointmentEnable() {
        return appointmentEnable;
    }

    public JsonStoreSetting setAppointmentEnable(boolean appointmentEnable) {
        this.appointmentEnable = appointmentEnable;
        return this;
    }

    public int getAppointmentDuration() {
        return appointmentDuration;
    }

    public JsonStoreSetting setAppointmentDuration(int appointmentDuration) {
        this.appointmentDuration = appointmentDuration;
        return this;
    }

    public int getAppointmentOpenHowFar() {
        return appointmentOpenHowFar;
    }

    public JsonStoreSetting setAppointmentOpenHowFar(int appointmentOpenHowFar) {
        this.appointmentOpenHowFar = appointmentOpenHowFar;
        return this;
    }

    @Override
    public String toString() {
        return "JsonStoreSetting{" +
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
            ", storeActionType=" + storeActionType +
            '}';
    }
}
