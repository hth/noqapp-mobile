package com.noqapp.mobile.domain;

import com.noqapp.common.utils.AbstractDomain;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * hitender
 * 3/1/20 4:50 AM
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
public class JsonCoronaStatElement extends AbstractDomain {
    private String country;

    private String officialFS;
    private String officialSS;
    private String officialCC;
    private String trackedFS;
    private String trackedSS;
    private String trackedCC;

    public String getCountry() {
        return country;
    }

    public JsonCoronaStatElement setCountry(String country) {
        this.country = country;
        return this;
    }

    public String getOfficialFS() {
        return officialFS;
    }

    public JsonCoronaStatElement setOfficialFS(String officialFS) {
        this.officialFS = officialFS;
        return this;
    }

    public String getOfficialSS() {
        return officialSS;
    }

    public JsonCoronaStatElement setOfficialSS(String officialSS) {
        this.officialSS = officialSS;
        return this;
    }

    public String getOfficialCC() {
        return officialCC;
    }

    public JsonCoronaStatElement setOfficialCC(String officialCC) {
        this.officialCC = officialCC;
        return this;
    }

    public String getTrackedFS() {
        return trackedFS;
    }

    public JsonCoronaStatElement setTrackedFS(String trackedFS) {
        this.trackedFS = trackedFS;
        return this;
    }

    public String getTrackedSS() {
        return trackedSS;
    }

    public JsonCoronaStatElement setTrackedSS(String trackedSS) {
        this.trackedSS = trackedSS;
        return this;
    }

    public String getTrackedCC() {
        return trackedCC;
    }

    public JsonCoronaStatElement setTrackedCC(String trackedCC) {
        this.trackedCC = trackedCC;
        return this;
    }
}
