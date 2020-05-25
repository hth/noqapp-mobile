package com.noqapp.mobile.domain.body.client;

import com.noqapp.common.utils.AbstractDomain;
import com.noqapp.common.utils.ScrubbedInput;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * hitender
 * 5/10/20 5:04 AM
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
public class QueueAuthorize extends AbstractDomain {
    private static final Logger LOG = LoggerFactory.getLogger(QueueAuthorize.class);

    @JsonProperty("codeQR")
    private ScrubbedInput codeQR;

    @JsonProperty("fci")
    private ScrubbedInput firstCustomerId;

    @JsonProperty("aci")
    private ScrubbedInput additionalCustomerId;

    public ScrubbedInput getCodeQR() {
        return codeQR;
    }

    public QueueAuthorize setCodeQR(ScrubbedInput codeQR) {
        this.codeQR = codeQR;
        return this;
    }

    public ScrubbedInput getFirstCustomerId() {
        return firstCustomerId;
    }

    public QueueAuthorize setFirstCustomerId(ScrubbedInput firstCustomerId) {
        this.firstCustomerId = firstCustomerId;
        return this;
    }

    public ScrubbedInput getAdditionalCustomerId() {
        return additionalCustomerId;
    }

    public QueueAuthorize setAdditionalCustomerId(ScrubbedInput additionalCustomerId) {
        this.additionalCustomerId = additionalCustomerId;
        return this;
    }
}
