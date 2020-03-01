package com.noqapp.mobile.domain;

import com.noqapp.common.utils.AbstractDomain;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

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

    public Map<String, List<JsonCoronaStatElement>> coronaStat = new HashMap<>();

    public Map<String, List<JsonCoronaStatElement>> getCoronaStat() {
        return coronaStat;
    }

    public JsonCoronaStat setCoronaStat(Map<String, List<JsonCoronaStatElement>> coronaStat) {
        this.coronaStat = coronaStat;
        return this;
    }

    public JsonCoronaStat addCoronaStat(String country, List<JsonCoronaStatElement> coronaStat) {
        this.coronaStat.put(country, coronaStat);
        return this;
    }
}
