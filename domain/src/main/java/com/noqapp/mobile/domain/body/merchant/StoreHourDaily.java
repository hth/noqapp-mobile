package com.noqapp.mobile.domain.body.merchant;

import com.noqapp.common.utils.AbstractDomain;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * hitender
 * 8/8/20 11:20 PM
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
public class StoreHourDaily extends AbstractDomain {

    @JsonProperty("f")
    private int tokenAvailableFrom;

    /* Store business start hour. */
    @JsonProperty("b")
    private int startHour;

    @JsonProperty("m")
    private int tokenNotAvailableFrom;

    /* Store business end hour. */
    @JsonProperty("e")
    private int endHour;

    @JsonProperty("ls")
    private int lunchTimeStart;

    @JsonProperty("le")
    private int lunchTimeEnd;

    @JsonProperty("dc")
    private boolean dayClosed = false;

    @JsonProperty("dw")
    private int dayOfWeek;

    public int getTokenAvailableFrom() {
        return tokenAvailableFrom;
    }

    public StoreHourDaily setTokenAvailableFrom(int tokenAvailableFrom) {
        this.tokenAvailableFrom = tokenAvailableFrom;
        return this;
    }

    public int getStartHour() {
        return startHour;
    }

    public StoreHourDaily setStartHour(int startHour) {
        this.startHour = startHour;
        return this;
    }

    public int getTokenNotAvailableFrom() {
        return tokenNotAvailableFrom;
    }

    public StoreHourDaily setTokenNotAvailableFrom(int tokenNotAvailableFrom) {
        this.tokenNotAvailableFrom = tokenNotAvailableFrom;
        return this;
    }

    public int getEndHour() {
        return endHour;
    }

    public StoreHourDaily setEndHour(int endHour) {
        this.endHour = endHour;
        return this;
    }

    public int getLunchTimeStart() {
        return lunchTimeStart;
    }

    public StoreHourDaily setLunchTimeStart(int lunchTimeStart) {
        this.lunchTimeStart = lunchTimeStart;
        return this;
    }

    public int getLunchTimeEnd() {
        return lunchTimeEnd;
    }

    public StoreHourDaily setLunchTimeEnd(int lunchTimeEnd) {
        this.lunchTimeEnd = lunchTimeEnd;
        return this;
    }

    public boolean isDayClosed() {
        return dayClosed;
    }

    public StoreHourDaily setDayClosed(boolean dayClosed) {
        this.dayClosed = dayClosed;
        return this;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public StoreHourDaily setDayOfWeek(int dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
        return this;
    }
}
