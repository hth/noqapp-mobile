package com.noqapp.mobile.domain;

import com.noqapp.common.utils.AbstractDomain;
import com.noqapp.domain.shared.GeoPointOfQ;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * User: hitender
 * Date: 3/1/17 12:59 PM
 */
@SuppressWarnings({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@JsonPropertyOrder(alphabetic = true)
@JsonIgnoreProperties(ignoreUnknown = true)
//@JsonInclude (JsonInclude.Include.NON_NULL)
public class DeviceRegistered extends AbstractDomain {

    @SuppressWarnings({"unused"})
    @JsonProperty("r")
    private int registered;

    @JsonProperty("did")
    private String deviceId;

    @JsonProperty("cor")
    private GeoPointOfQ geoPointOfQ;

    public DeviceRegistered() {
        //Required Default Constructor
    }

    private DeviceRegistered(boolean registered) {
        this.registered = registered ? 1 : 0;
    }

    private DeviceRegistered(boolean registered, String deviceId) {
        this.registered = registered ? 1 : 0;
        this.deviceId = deviceId;
    }

    public static DeviceRegistered newInstance(boolean registered) {
        return new DeviceRegistered(registered);
    }

    public static DeviceRegistered newInstance(boolean registered, String deviceId) {
        return new DeviceRegistered(registered, deviceId);
    }

    public int getRegistered() {
        return registered;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public GeoPointOfQ getGeoPointOfQ() {
        return geoPointOfQ;
    }

    public DeviceRegistered setGeoPointOfQ(GeoPointOfQ geoPointOfQ) {
        this.geoPointOfQ = geoPointOfQ;
        return this;
    }
}
