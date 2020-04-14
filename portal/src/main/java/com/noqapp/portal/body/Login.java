package com.noqapp.portal.body;

import com.noqapp.common.utils.ScrubbedInput;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * hitender
 * 3/23/20 4:40 PM
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
public class Login {

    @JsonProperty("PH")
    private ScrubbedInput phone;

    @JsonProperty("CS")
    private ScrubbedInput countryShortName;

    @JsonProperty("FI")
    private ScrubbedInput firebaseUid;

    public ScrubbedInput getPhone() {
        return phone;
    }

    public Login setPhone(ScrubbedInput phone) {
        this.phone = phone;
        return this;
    }

    public ScrubbedInput getCountryShortName() {
        return countryShortName;
    }

    public Login setCountryShortName(ScrubbedInput countryShortName) {
        this.countryShortName = countryShortName;
        return this;
    }

    public ScrubbedInput getFirebaseUid() {
        return firebaseUid;
    }

    public Login setFirebaseUid(ScrubbedInput firebaseUid) {
        this.firebaseUid = firebaseUid;
        return this;
    }
}
