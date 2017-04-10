package com.noqapp.mobile.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.google.gson.annotations.SerializedName;

/**
 * Hold pid and access token.
 * User: hitender
 * Date: 1/14/17 12:21 PM
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
public class ProviderAndAccessToken {

    @SuppressWarnings ({"unused"})
    @SerializedName ("pid")
    private String providerId;

    @SuppressWarnings ({"unused"})
    @SerializedName ("at")
    private String accessToken;

    private ProviderAndAccessToken(String providerId, String accessToken) {
        this.providerId = providerId;
        this.accessToken = accessToken;
    }

    public static ProviderAndAccessToken newInstance(String provider, String accessToken) {
        return new ProviderAndAccessToken(provider, accessToken);
    }
}
