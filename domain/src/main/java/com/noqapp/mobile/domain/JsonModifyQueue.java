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
