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
    @SerializedName ("name")
    private String name;

    @SuppressWarnings ({"unused"})
    @SerializedName ("auth")
    private String auth;

    private SignupUserInfo(String userId, String name, String auth) {
        this.userId = userId;
        this.name = name;
        this.auth = auth;
    }

    public static SignupUserInfo newInstance(String userId, String name, String auth) {
        return new SignupUserInfo(userId, name, auth);
    }
}