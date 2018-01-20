package com.noqapp.mobile.domain;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.noqapp.common.utils.AbstractDomain;
import com.noqapp.domain.StoreHourEntity;

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

    @JsonProperty ("c")
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

    @JsonProperty ("pj")
    private boolean preventJoining;

    @JsonProperty ("dc")
    private boolean dayClosed = false;

    @JsonProperty ("at")
    private int availableTokenCount;

    public JsonModifyQueue() {
    }

    public JsonModifyQueue(String codeQR, StoreHourEntity storeHour, int availableTokenCount) {
        this.codeQR = codeQR;
        this.tokenAvailableFrom = storeHour.getTokenAvailableFrom();
        this.startHour = storeHour.getStartHour();
        this.tokenNotAvailableFrom = storeHour.getTokenNotAvailableFrom();
        this.endHour = storeHour.getEndHour();
        this.delayedInMinutes = storeHour.getDelayedInMinutes();
        this.preventJoining = storeHour.isPreventJoining();
        this.dayClosed = storeHour.isDayClosed();
        this.availableTokenCount = availableTokenCount;
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

    public boolean isPreventJoining() {
        return preventJoining;
    }

    public JsonModifyQueue setPreventJoining(boolean preventJoining) {
        this.preventJoining = preventJoining;
        return this;
    }

    public boolean isDayClosed() {
        return dayClosed;
    }

    public JsonModifyQueue setDayClosed(boolean dayClosed) {
        this.dayClosed = dayClosed;
        return this;
    }

    public int getAvailableTokenCount() {
        return availableTokenCount;
    }

    public JsonModifyQueue setAvailableTokenCount(int availableTokenCount) {
        this.availableTokenCount = availableTokenCount;
        return this;
    }
}
