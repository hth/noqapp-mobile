package com.noqapp.mobile.domain.body.client;

import com.noqapp.common.utils.ScrubbedInput;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * User: hitender
 * Date: 2019-06-18 12:12
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
public class Location {

    @JsonProperty("cityName")
    private ScrubbedInput cityName;

    @JsonProperty("lat")
    private ScrubbedInput latitude;

    @JsonProperty("lng")
    private ScrubbedInput longitude;

    public ScrubbedInput getCityName() {
        return cityName;
    }

    public Location setCityName(ScrubbedInput cityName) {
        this.cityName = cityName;
        return this;
    }

    public ScrubbedInput getLatitude() {
        return latitude;
    }

    public Location setLatitude(ScrubbedInput latitude) {
        this.latitude = latitude;
        return this;
    }

    public ScrubbedInput getLongitude() {
        return longitude;
    }

    public Location setLongitude(ScrubbedInput longitude) {
        this.longitude = longitude;
        return this;
    }

    @Override
    public String toString() {
        return "Location{" +
            "cityName=" + cityName +
            ", latitude=" + latitude +
            ", longitude=" + longitude +
            '}';
    }
}
