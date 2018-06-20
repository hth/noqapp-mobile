package com.noqapp.mobile.domain.body.client;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.noqapp.common.utils.AbstractDomain;

/**
 * hitender
 * 12/7/17 1:33 PM
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
public class Registration extends AbstractDomain {
    @JsonProperty("QID")
    private String queueUserId;

    @JsonProperty("PH")
    private String phone;

    @JsonProperty("FN")
    private String firstName;

    @JsonProperty("EM")
    private String mail;

    @JsonProperty("PW")
    private String password;

    @JsonProperty("BD")
    private String birthday;

    @JsonProperty("GE")
    private String gender;

    @JsonProperty("CS")
    private String countryShortName;

    @JsonProperty("TZ")
    private String timeZoneId;

    @JsonProperty("IC")
    private String inviteCode;

    public String getQueueUserId() {
        return queueUserId;
    }

    public Registration setQueueUserId(String queueUserId) {
        this.queueUserId = queueUserId;
        return this;
    }

    public String getPhone() {
        return phone;
    }

    public Registration setPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public Registration setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public String getMail() {
        return mail;
    }

    public Registration setMail(String mail) {
        this.mail = mail;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public Registration setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getBirthday() {
        return birthday;
    }

    public Registration setBirthday(String birthday) {
        this.birthday = birthday;
        return this;
    }

    public String getGender() {
        return gender;
    }

    public Registration setGender(String gender) {
        this.gender = gender;
        return this;
    }

    public String getCountryShortName() {
        return countryShortName;
    }

    public Registration setCountryShortName(String countryShortName) {
        this.countryShortName = countryShortName;
        return this;
    }

    public String getTimeZoneId() {
        return timeZoneId;
    }

    public Registration setTimeZoneId(String timeZoneId) {
        this.timeZoneId = timeZoneId;
        return this;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public Registration setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
        return this;
    }
}
