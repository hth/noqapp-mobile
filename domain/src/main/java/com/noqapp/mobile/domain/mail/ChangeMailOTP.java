package com.noqapp.mobile.domain.mail;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.google.gson.annotations.SerializedName;

/**
 * hitender
 * 9/3/18 4:28 PM
 */
@SuppressWarnings ({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@JsonPropertyOrder(alphabetic = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChangeMailOTP {

    @SuppressWarnings ({"unused"})
    @SerializedName("userId")
    private String userId;

    @SuppressWarnings ({"unused"})
    @SerializedName ("name")
    private String name;

    @SuppressWarnings ({"unused"})
    @SerializedName ("mailOTP")
    private String mailOTP;

    private ChangeMailOTP(String userId, String name, String mailOTP) {
        this.userId = userId;
        this.name = name;
        this.mailOTP = mailOTP;
    }

    public static ChangeMailOTP newInstance(String userId, String name, String mailOTP) {
        return new ChangeMailOTP(userId, name, mailOTP);
    }
}