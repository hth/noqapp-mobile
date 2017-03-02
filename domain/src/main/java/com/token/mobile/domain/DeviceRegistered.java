package com.token.mobile.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.token.domain.AbstractDomain;

/**
 * User: hitender
 * Date: 3/1/17 12:59 PM
 */
@SuppressWarnings ({
        "PMD.BeanMembersShouldSerialize",
        "PMD.LocalVariableCouldBeFinal",
        "PMD.MethodArgumentCouldBeFinal",
        "PMD.LongVariable"
})
@JsonPropertyOrder (alphabetic = true)
@JsonIgnoreProperties (ignoreUnknown = true)
//@JsonInclude (JsonInclude.Include.NON_NULL)
public class DeviceRegistered extends AbstractDomain {

    @SuppressWarnings ({"unused"})
    @JsonProperty ("r")
    private int registered;

    private DeviceRegistered(boolean registered) {
        this.registered = registered ? 1 : 0;
    }

    public static DeviceRegistered newInstance(boolean registered) {
        return new DeviceRegistered(registered);
    }

    public int getRegistered() {
        return registered;
    }
}
