package com.noqapp.mobile.domain.body.merchant;

import com.noqapp.common.utils.AbstractDomain;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * hitender
 * 2019-02-14 16:51
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
public class RemoveLabFile extends AbstractDomain {

    @JsonProperty("transactionId")
    String transactionId;

    @JsonProperty("filename")
    String filename;

    public String getTransactionId() {
        return transactionId;
    }

    public RemoveLabFile setTransactionId(String transactionId) {
        this.transactionId = transactionId;
        return this;
    }

    public String getFilename() {
        return filename;
    }

    public RemoveLabFile setFilename(String filename) {
        this.filename = filename;
        return this;
    }
}
