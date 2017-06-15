package com.noqapp.mobile.domain;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.noqapp.domain.AbstractDomain;
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

    public JsonModifyQueue() {
    }

    public JsonModifyQueue(String codeQR, StoreHourEntity storeHour) {
        this.codeQR = codeQR;
        this.preventJoining = storeHour.isPreventJoining();
        this.dayClosed = storeHour.isDayClosed();
    }

    public String getCodeQR() {
        return codeQR;
    }

    public void setCodeQR(String codeQR) {
        this.codeQR = codeQR;
    }

    public boolean isPreventJoining() {
        return preventJoining;
    }

    public void setPreventJoining(boolean preventJoining) {
        this.preventJoining = preventJoining;
    }

    public boolean isDayClosed() {
        return dayClosed;
    }

    public void setDayClosed(boolean dayClosed) {
        this.dayClosed = dayClosed;
    }
}
