package com.noqapp.mobile.domain.body.merchant;

import com.noqapp.common.utils.AbstractDomain;
import com.noqapp.common.utils.ScrubbedInput;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.StringJoiner;

/**
 * User: hitender
 * Date: 9/13/19 10:40 AM
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
public class CodeQRDateRangeLookup extends AbstractDomain {

    @JsonProperty("codeQR")
    private ScrubbedInput codeQR;

    @JsonProperty("from")
    private ScrubbedInput from;

    @JsonProperty("until")
    private ScrubbedInput until;

    public ScrubbedInput getCodeQR() {
        return codeQR;
    }

    public CodeQRDateRangeLookup setCodeQR(ScrubbedInput codeQR) {
        this.codeQR = codeQR;
        return this;
    }

    public ScrubbedInput getFrom() {
        return from;
    }

    public CodeQRDateRangeLookup setFrom(ScrubbedInput from) {
        this.from = from;
        return this;
    }

    public ScrubbedInput getUntil() {
        return until;
    }

    public CodeQRDateRangeLookup setUntil(ScrubbedInput until) {
        this.until = until;
        return this;
    }

    @Override
    public String toString() {
        return "CodeQRDateRangeLookup{" +
            "codeQR=" + codeQR +
            ", from=" + from +
            ", until=" + until +
            '}';
    }
}
