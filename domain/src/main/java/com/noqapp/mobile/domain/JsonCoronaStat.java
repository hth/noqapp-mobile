package com.noqapp.mobile.domain;

import com.noqapp.common.utils.AbstractDomain;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * hitender
 * 3/1/20 4:51 AM
 */
@SuppressWarnings ({
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
public class JsonCoronaStat extends AbstractDomain {

    public List<JsonCoronaStatElement> coronaStats = new ArrayList<>();

    public List<JsonCoronaStatElement> getCoronaStats() {
        return coronaStats;
    }

    public JsonCoronaStat setCoronaStats(List<JsonCoronaStatElement> coronaStats) {
        this.coronaStats = coronaStats;
        return this;
    }

    public JsonCoronaStat addCoronaStat(JsonCoronaStatElement coronaStats) {
        this.coronaStats.add(coronaStats);
        return this;
    }
}
