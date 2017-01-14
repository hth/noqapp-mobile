package com.token.mobile.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.google.gson.annotations.SerializedName;

import com.token.domain.AbstractDomain;

/**
 * User: hitender
 * Date: 1/14/17 11:04 AM
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
public class AccountRecover extends AbstractDomain {

    @SuppressWarnings ({"unused"})
    @SerializedName ("userId")
    private String userId;

    private AccountRecover(String userId) {
        super();
        this.userId = userId;
    }

    public static AccountRecover newInstance(String userId) {
        return new AccountRecover(userId);
    }
}
