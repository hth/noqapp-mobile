package com.noqapp.portal.domain;

import com.noqapp.common.utils.AbstractDomain;
import com.noqapp.domain.json.JsonQueueList;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * hitender
 * 4/14/20 9:08 AM
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
public class InstantViewDashboard extends AbstractDomain {
    private static final Logger LOG = LoggerFactory.getLogger(InstantViewDashboard.class);

    private JsonQueueList jsonQueueList;
    private String amount;

    public JsonQueueList getJsonQueueList() {
        return jsonQueueList;
    }

    public InstantViewDashboard setJsonQueueList(JsonQueueList jsonQueueList) {
        this.jsonQueueList = jsonQueueList;
        return this;
    }

    public String getAmount() {
        return amount;
    }

    public InstantViewDashboard setAmount(String amount) {
        this.amount = amount;
        return this;
    }
}
