package com.noqapp.mobile.domain;

import com.noqapp.common.utils.AbstractDomain;
import com.noqapp.domain.json.JsonQueuedPerson;
import com.noqapp.domain.types.CustomerPriorityLevelEnum;
import com.noqapp.domain.types.QueueUserStateEnum;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.apache.commons.lang3.time.DateFormatUtils;

import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;
import java.util.TimeZone;

/**
 * For authenticating person through Code QR.
 * hitender
 * 6/8/20 5:31 AM
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
//@JsonInclude (JsonInclude.Include.NON_NULL)
public class JsonInQueuePerson extends AbstractDomain {

    @JsonProperty("t")
    private int token;

    @JsonProperty ("dt")
    private String displayToken;

    @JsonProperty("n")
    private String customerName = "";

    @JsonProperty("p")
    private String customerPhone = "";

    @JsonProperty("bc")
    private String businessCustomerId;

    @JsonProperty("d")
    private String displayName;

    @JsonProperty("qu")
    private QueueUserStateEnum queueUserState;

    @JsonProperty ("e")
    private String expectedServiceBegin;

    @JsonProperty("pl")
    private CustomerPriorityLevelEnum customerPriorityLevel = CustomerPriorityLevelEnum.I;

    @JsonProperty("ti")
    private String transactionId;

    @JsonProperty("sl")
    private String timeSlotMessage;

    @JsonProperty("c")
    private String created;

    public int getToken() {
        return token;
    }

    public JsonInQueuePerson setToken(int token) {
        this.token = token;
        return this;
    }

    public String getDisplayToken() {
        return displayToken;
    }

    public JsonInQueuePerson setDisplayToken(String displayToken) {
        this.displayToken = displayToken;
        return this;
    }

    public String getCustomerName() {
        return customerName;
    }

    public JsonInQueuePerson setCustomerName(String customerName) {
        this.customerName = customerName;
        return this;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public JsonInQueuePerson setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
        return this;
    }

    public String getBusinessCustomerId() {
        return businessCustomerId;
    }

    public JsonInQueuePerson setBusinessCustomerId(String businessCustomerId) {
        this.businessCustomerId = businessCustomerId;
        return this;
    }

    public String getDisplayName() {
        return displayName;
    }

    public JsonInQueuePerson setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public QueueUserStateEnum getQueueUserState() {
        return queueUserState;
    }

    public JsonInQueuePerson setQueueUserState(QueueUserStateEnum queueUserState) {
        this.queueUserState = queueUserState;
        return this;
    }

    public String getExpectedServiceBegin() {
        return expectedServiceBegin;
    }

    public JsonInQueuePerson setExpectedServiceBegin(Date expectedServiceBegin) {
        this.expectedServiceBegin = DateFormatUtils.format(expectedServiceBegin, ISO8601_FMT, TimeZone.getTimeZone("UTC"));;
        return this;
    }

    public CustomerPriorityLevelEnum getCustomerPriorityLevel() {
        return customerPriorityLevel;
    }

    public JsonInQueuePerson setCustomerPriorityLevel(CustomerPriorityLevelEnum customerPriorityLevel) {
        this.customerPriorityLevel = customerPriorityLevel;
        return this;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public JsonInQueuePerson setTransactionId(String transactionId) {
        this.transactionId = transactionId;
        return this;
    }

    public String getTimeSlotMessage() {
        return timeSlotMessage;
    }

    public JsonInQueuePerson setTimeSlotMessage(String timeSlotMessage) {
        this.timeSlotMessage = timeSlotMessage;
        return this;
    }

    public String getCreated() {
        return created;
    }

    public JsonInQueuePerson setCreated(Date created) {
        this.created = DateFormatUtils.format(created, ISO8601_FMT, TimeZone.getTimeZone("UTC"));
        return this;
    }
}
