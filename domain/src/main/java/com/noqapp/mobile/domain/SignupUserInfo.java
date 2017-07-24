package com.noqapp.mobile.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.google.gson.annotations.SerializedName;

/**
 * User: hitender
 * Date: 1/14/17 12:06 PM
 */
@SuppressWarnings ({
        "PMD.BeanMembersShouldSerialize",
        "PMD.LocalVariableCouldBeFinal",
        "PMD.MethodArgumentCouldBeFinal",
        "PMD.LongVariable"
})
@JsonPropertyOrder (alphabetic = true)
@JsonIgnoreProperties (ignoreUnknown = true)
public class SignupUserInfo {

    @SuppressWarnings ({"unused"})
    @SerializedName ("userId")
    private String userId;

    @SuppressWarnings ({"unused"})
    @SerializedName ("rid")
    private String rid;

    @SuppressWarnings ({"unused"})
    @SerializedName ("name")
    private String name;

    private SignupUserInfo(String userId, String rid, String name) {
        this.userId = userId;
        this.rid = rid;
        this.name = name;
    }

    public static SignupUserInfo newInstance(String userId, String rid, String name) {
        return new SignupUserInfo(userId, rid, name);
    }
}