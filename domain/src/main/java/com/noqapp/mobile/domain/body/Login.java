package com.noqapp.mobile.domain.body;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.noqapp.common.utils.AbstractDomain;

/**
 * hitender
 * 12/7/17 6:58 PM
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
public class Login extends AbstractDomain {

    @JsonProperty("PH")
    private String phone;

    @JsonProperty("CS")
    private String countryShortName;

    public String getPhone() {
        return phone;
    }

    public Login setPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public String getCountryShortName() {
        return countryShortName;
    }

    public Login setCountryShortName(String countryShortName) {
        this.countryShortName = countryShortName;
        return this;
    }
}
