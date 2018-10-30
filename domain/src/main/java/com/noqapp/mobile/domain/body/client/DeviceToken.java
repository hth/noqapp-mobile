package com.noqapp.mobile.domain.body.client;

import com.noqapp.common.utils.AbstractDomain;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * hitender
 * 12/8/17 1:08 AM
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
public class DeviceToken extends AbstractDomain {

    @JsonProperty("tk")
    private String fcmToken;

    @JsonProperty("mo")
    private String model;

    @JsonProperty("os")
    private String osVersion;

    @JsonProperty("av")
    private String appVersion;

    public DeviceToken(String fcmToken, String model, String osVersion, String appVersion) {
        this.fcmToken = fcmToken;
        this.model = model;
        this.osVersion = osVersion;
        this.appVersion = appVersion;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public String getModel() {
        return model;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public String getAppVersion() {
        return appVersion;
    }
}
