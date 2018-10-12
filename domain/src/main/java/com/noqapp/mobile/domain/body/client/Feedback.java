package com.noqapp.mobile.domain.body.client;

import com.noqapp.common.utils.AbstractDomain;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.types.MessageOriginEnum;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * hitender
 * 10/4/18 5:07 PM
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
public class Feedback extends AbstractDomain {

    @JsonProperty("s")
    private ScrubbedInput subject;

    @JsonProperty("b")
    private ScrubbedInput body;

    @JsonProperty("qr")
    private ScrubbedInput codeQR;

    @JsonProperty ("t")
    private int token;

    @JsonProperty ("mo")
    private MessageOriginEnum messageOrigin;

    public ScrubbedInput getSubject() {
        return subject;
    }

    public Feedback setSubject(ScrubbedInput subject) {
        this.subject = subject;
        return this;
    }

    public ScrubbedInput getBody() {
        return body;
    }

    public Feedback setBody(ScrubbedInput body) {
        this.body = body;
        return this;
    }

    public ScrubbedInput getCodeQR() {
        return codeQR;
    }

    public Feedback setCodeQR(ScrubbedInput codeQR) {
        this.codeQR = codeQR;
        return this;
    }

    public int getToken() {
        return token;
    }

    public Feedback setToken(int token) {
        this.token = token;
        return this;
    }

    public MessageOriginEnum getMessageOrigin() {
        return messageOrigin;
    }

    public Feedback setMessageOrigin(MessageOriginEnum messageOrigin) {
        this.messageOrigin = messageOrigin;
        return this;
    }
}
