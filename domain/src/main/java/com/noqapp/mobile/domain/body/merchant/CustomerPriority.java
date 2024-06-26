package com.noqapp.mobile.domain.body.merchant;

import com.noqapp.common.utils.AbstractDomain;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.types.ActionTypeEnum;
import com.noqapp.domain.types.BusinessCustomerAttributeEnum;
import com.noqapp.domain.types.CustomerPriorityLevelEnum;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * hitender
 * 5/24/20 3:43 PM
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
public class CustomerPriority extends AbstractDomain {

    @JsonProperty("qid")
    private ScrubbedInput queueUserId;

    @JsonProperty("at")
    private ActionTypeEnum actionType;

    @JsonProperty("qr")
    private ScrubbedInput codeQR;

    @JsonProperty("pl")
    private CustomerPriorityLevelEnum customerPriorityLevel = CustomerPriorityLevelEnum.I;

    @JsonProperty("ca")
    private List<BusinessCustomerAttributeEnum> businessCustomerAttributes = new ArrayList<>();

    public ScrubbedInput getQueueUserId() {
        return queueUserId;
    }

    public CustomerPriority setQueueUserId(ScrubbedInput queueUserId) {
        this.queueUserId = queueUserId;
        return this;
    }

    public ActionTypeEnum getActionType() {
        return actionType;
    }

    public CustomerPriority setActionType(ActionTypeEnum actionType) {
        this.actionType = actionType;
        return this;
    }

    public ScrubbedInput getCodeQR() {
        return codeQR;
    }

    public CustomerPriority setCodeQR(ScrubbedInput codeQR) {
        this.codeQR = codeQR;
        return this;
    }

    public CustomerPriorityLevelEnum getCustomerPriorityLevel() {
        return customerPriorityLevel;
    }

    public CustomerPriority setCustomerPriorityLevel(CustomerPriorityLevelEnum customerPriorityLevel) {
        this.customerPriorityLevel = customerPriorityLevel;
        return this;
    }

    public List<BusinessCustomerAttributeEnum> getBusinessCustomerAttributes() {
        return businessCustomerAttributes;
    }

    public CustomerPriority setBusinessCustomerAttributes(List<BusinessCustomerAttributeEnum> businessCustomerAttributes) {
        this.businessCustomerAttributes = businessCustomerAttributes;
        return this;
    }

    public boolean isValid() {
        switch (actionType) {
            case APPROVE:
                return queueUserId != null && StringUtils.isNotBlank(queueUserId.getText())
                    && actionType != null
                    && codeQR != null && StringUtils.isNotBlank(codeQR.getText())
                    && customerPriorityLevel != null;
            case REJECT:
            case CLEAR:
                return queueUserId != null && StringUtils.isNotBlank(queueUserId.getText())
                    && actionType != null
                    && codeQR != null && StringUtils.isNotBlank(codeQR.getText());
            default:
                throw new UnsupportedOperationException("Reached not supported condition");
        }
    }
}
