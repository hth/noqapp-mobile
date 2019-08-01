package com.noqapp.mobile.domain.body.merchant;

import com.noqapp.common.utils.AbstractDomain;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.medical.domain.json.JsonMedicalRecord;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.springframework.data.annotation.Transient;

/**
 * User: hitender
 * Date: 2019-07-30 10:15
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
public class CheckAsset extends AbstractDomain {

    @JsonProperty("bn")
    private String bizNameId;

    @JsonProperty("fl")
    private String floor;

    @JsonProperty("rn")
    private String roomNumber;

    @JsonProperty("an")
    private String assetName;

    @JsonProperty("n")
    private String businessName;

    @Transient
    @JsonProperty("at")
    private String areaAndTown;

    public String getBizNameId() {
        return bizNameId;
    }

    public CheckAsset setBizNameId(String bizNameId) {
        this.bizNameId = bizNameId;
        return this;
    }

    public String getFloor() {
        return floor;
    }

    public CheckAsset setFloor(String floor) {
        this.floor = floor;
        return this;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public CheckAsset setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
        return this;
    }

    public String getAssetName() {
        return assetName;
    }

    public CheckAsset setAssetName(String assetName) {
        this.assetName = assetName;
        return this;
    }

    public String getBusinessName() {
        return businessName;
    }

    public CheckAsset setBusinessName(String businessName) {
        this.businessName = businessName;
        return this;
    }

    public String getAreaAndTown() {
        return areaAndTown;
    }

    public CheckAsset setAreaAndTown(String areaAndTown) {
        this.areaAndTown = areaAndTown;
        return this;
    }
}
