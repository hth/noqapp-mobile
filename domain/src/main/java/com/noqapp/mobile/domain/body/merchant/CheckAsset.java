package com.noqapp.mobile.domain.body.merchant;

import com.noqapp.common.utils.AbstractDomain;
import com.noqapp.common.utils.ScrubbedInput;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

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
    private ScrubbedInput bizNameId;

    @JsonProperty("fl")
    private ScrubbedInput floor;

    @JsonProperty("rn")
    private ScrubbedInput roomNumber;

    @JsonProperty("an")
    private ScrubbedInput assetName;

    public ScrubbedInput getBizNameId() {
        return bizNameId;
    }

    public CheckAsset setBizNameId(ScrubbedInput bizNameId) {
        this.bizNameId = bizNameId;
        return this;
    }

    public ScrubbedInput getFloor() {
        return floor;
    }

    public CheckAsset setFloor(ScrubbedInput floor) {
        this.floor = floor;
        return this;
    }

    public ScrubbedInput getRoomNumber() {
        return roomNumber;
    }

    public CheckAsset setRoomNumber(ScrubbedInput roomNumber) {
        this.roomNumber = roomNumber;
        return this;
    }

    public ScrubbedInput getAssetName() {
        return assetName;
    }

    public CheckAsset setAssetName(ScrubbedInput assetName) {
        this.assetName = assetName;
        return this;
    }
}
