package com.noqapp.mobile.domain.body.merchant;

import com.noqapp.common.utils.AbstractDomain;
import com.noqapp.common.utils.ScrubbedInput;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

/**
 * hitender
 * 6/13/20 2:15 PM
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
public class MessageCustomer extends AbstractDomain {

    @JsonProperty("ti")
    private ScrubbedInput title;

    @JsonProperty("bd")
    private ScrubbedInput body;

    @JsonProperty("cqs")
    private List<ScrubbedInput> codeQRs;

    /**
     * Failed to deliver to codeQRs due to access restrictions or other reasons.
     * Note: This could be removed.
     */
    @JsonProperty("fcqs")
    private List<ScrubbedInput> failedCodeQRs;

    @JsonProperty("cn")
    private int sendMessageCount;

    public ScrubbedInput getTitle() {
        return title;
    }

    public MessageCustomer setTitle(ScrubbedInput title) {
        this.title = title;
        return this;
    }

    public ScrubbedInput getBody() {
        return body;
    }

    public MessageCustomer setBody(ScrubbedInput body) {
        this.body = body;
        return this;
    }

    public List<ScrubbedInput> getCodeQRs() {
        return codeQRs;
    }

    public MessageCustomer setCodeQRs(List<ScrubbedInput> codeQRs) {
        this.codeQRs = codeQRs;
        return this;
    }

    public List<ScrubbedInput> getFailedCodeQRs() {
        return failedCodeQRs;
    }

    public MessageCustomer setFailedCodeQRs(List<ScrubbedInput> failedCodeQRs) {
        this.failedCodeQRs = failedCodeQRs;
        return this;
    }

    public int getSendMessageCount() {
        return sendMessageCount;
    }

    public MessageCustomer setSendMessageCount(int sendMessageCount) {
        this.sendMessageCount = sendMessageCount;
        return this;
    }
}
