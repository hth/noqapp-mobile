package com.noqapp.mobile.domain.body.merchant;

import com.noqapp.common.utils.AbstractDomain;
import com.noqapp.domain.types.BooleanReplacementEnum;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * User: hitender
 * Date: 2019-07-23 00:11
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
public class HospitalVisitFor extends AbstractDomain {

    @JsonProperty("id")
    private String hospitalVisitScheduleId;

    @JsonProperty("qid")
    private String qid;

    @JsonProperty("vf")
    private String visitingFor;

    @JsonProperty("br")
    private BooleanReplacementEnum booleanReplacement;

    public String getHospitalVisitScheduleId() {
        return hospitalVisitScheduleId;
    }

    public HospitalVisitFor setHospitalVisitScheduleId(String hospitalVisitScheduleId) {
        this.hospitalVisitScheduleId = hospitalVisitScheduleId;
        return this;
    }

    public String getQid() {
        return qid;
    }

    public HospitalVisitFor setQid(String qid) {
        this.qid = qid;
        return this;
    }

    public String getVisitingFor() {
        return visitingFor;
    }

    public HospitalVisitFor setVisitingFor(String visitingFor) {
        this.visitingFor = visitingFor;
        return this;
    }

    public BooleanReplacementEnum getBooleanReplacement() {
        return booleanReplacement;
    }

    public HospitalVisitFor setBooleanReplacement(BooleanReplacementEnum booleanReplacement) {
        this.booleanReplacement = booleanReplacement;
        return this;
    }
}
