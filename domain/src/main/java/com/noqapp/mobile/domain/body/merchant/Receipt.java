package com.noqapp.mobile.domain.body.merchant;

import com.noqapp.common.utils.AbstractDomain;
import com.noqapp.domain.json.JsonNameDatePair;
import com.noqapp.domain.json.JsonProfile;
import com.noqapp.domain.json.JsonPurchaseOrder;
import com.noqapp.domain.types.BusinessTypeEnum;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

/**
 * User: hitender
 * Date: 2019-04-29 14:10
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
public class Receipt extends AbstractDomain {

    @JsonProperty("qr")
    private String codeQR;

    @JsonProperty("bt")
    private BusinessTypeEnum businessType;

    @JsonProperty("n")
    private String businessName;

    @JsonProperty("sa")
    private String storeAddress;

    @JsonProperty("p")
    private String storePhone;

    @JsonProperty("bc")
    private String businessCustomerId;

    @JsonProperty("qid")
    private String queueUserId;

    @JsonProperty ("ti")
    private String transactionId;

    @JsonProperty ("po")
    private JsonPurchaseOrder jsonPurchaseOrder;

    /* Professional Name. */
    @JsonProperty("nm")
    private String name;

    /* Required to mark as a valid profile. */
    @JsonProperty("ed")
    private List<JsonNameDatePair> education;

    /* Required to mark as a valid profile. */
    @JsonProperty("li")
    private List<JsonNameDatePair> licenses;

    /* Customer Detail. */
    @JsonProperty("jp")
    private JsonProfile jsonProfile;

    public String getCodeQR() {
        return codeQR;
    }

    public Receipt setCodeQR(String codeQR) {
        this.codeQR = codeQR;
        return this;
    }

    public BusinessTypeEnum getBusinessType() {
        return businessType;
    }

    public Receipt setBusinessType(BusinessTypeEnum businessType) {
        this.businessType = businessType;
        return this;
    }

    public String getBusinessName() {
        return businessName;
    }

    public Receipt setBusinessName(String businessName) {
        this.businessName = businessName;
        return this;
    }

    public String getStoreAddress() {
        return storeAddress;
    }

    public Receipt setStoreAddress(String storeAddress) {
        this.storeAddress = storeAddress;
        return this;
    }

    public String getStorePhone() {
        return storePhone;
    }

    public Receipt setStorePhone(String storePhone) {
        this.storePhone = storePhone;
        return this;
    }

    public String getBusinessCustomerId() {
        return businessCustomerId;
    }

    public Receipt setBusinessCustomerId(String businessCustomerId) {
        this.businessCustomerId = businessCustomerId;
        return this;
    }

    public String getQueueUserId() {
        return queueUserId;
    }

    public Receipt setQueueUserId(String queueUserId) {
        this.queueUserId = queueUserId;
        return this;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public Receipt setTransactionId(String transactionId) {
        this.transactionId = transactionId;
        return this;
    }

    public JsonPurchaseOrder getJsonPurchaseOrder() {
        return jsonPurchaseOrder;
    }

    public Receipt setJsonPurchaseOrder(JsonPurchaseOrder jsonPurchaseOrder) {
        this.jsonPurchaseOrder = jsonPurchaseOrder;
        return this;
    }

    public String getName() {
        return name;
    }

    public Receipt setName(String name) {
        this.name = name;
        return this;
    }

    public List<JsonNameDatePair> getEducation() {
        return education;
    }

    public Receipt setEducation(List<JsonNameDatePair> education) {
        this.education = education;
        return this;
    }

    public List<JsonNameDatePair> getLicenses() {
        return licenses;
    }

    public Receipt setLicenses(List<JsonNameDatePair> licenses) {
        this.licenses = licenses;
        return this;
    }

    public JsonProfile getJsonProfile() {
        return jsonProfile;
    }

    public Receipt setJsonProfile(JsonProfile jsonProfile) {
        this.jsonProfile = jsonProfile;
        return this;
    }
}
