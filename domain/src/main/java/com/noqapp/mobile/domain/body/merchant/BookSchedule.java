package com.noqapp.mobile.domain.body.merchant;

import com.noqapp.common.utils.AbstractDomain;
import com.noqapp.domain.json.JsonBusinessCustomer;
import com.noqapp.domain.json.JsonSchedule;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * User: hitender
 * Date: 2019-06-03 17:00
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
public class BookSchedule extends AbstractDomain {

    @JsonProperty("sc")
    private JsonSchedule jsonSchedule;

    @JsonProperty("bc")
    private JsonBusinessCustomer businessCustomer;

    public JsonSchedule getJsonSchedule() {
        return jsonSchedule;
    }

    public BookSchedule setJsonSchedule(JsonSchedule jsonSchedule) {
        this.jsonSchedule = jsonSchedule;
        return this;
    }

    public JsonBusinessCustomer getBusinessCustomer() {
        return businessCustomer;
    }

    public BookSchedule setBusinessCustomer(JsonBusinessCustomer businessCustomer) {
        this.businessCustomer = businessCustomer;
        return this;
    }
}
