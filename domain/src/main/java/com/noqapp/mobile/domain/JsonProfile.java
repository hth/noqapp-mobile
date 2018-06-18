package com.noqapp.mobile.domain;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.noqapp.common.utils.AbstractDomain;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.json.medical.JsonUserMedicalProfile;
import com.noqapp.domain.types.GenderEnum;
import com.noqapp.domain.types.UserLevelEnum;
import com.noqapp.common.utils.Formatter;

import java.util.ArrayList;
import java.util.List;

/**
 * User: hitender
 * Date: 3/25/17 2:05 AM
 */
@SuppressWarnings ({
        "PMD.BeanMembersShouldSerialize",
        "PMD.LocalVariableCouldBeFinal",
        "PMD.MethodArgumentCouldBeFinal",
        "PMD.LongVariable",
        "unused"
})
@JsonAutoDetect (
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
)
@JsonPropertyOrder (alphabetic = true)
@JsonIgnoreProperties (ignoreUnknown = true)
//@JsonInclude (JsonInclude.Include.NON_NULL)
public class JsonProfile extends AbstractDomain {
    @JsonProperty ("pi")
    private String profileImage;

    @JsonProperty ("nm")
    private String name;

    @JsonProperty ("em")
    private String mail;

    @JsonProperty ("cs")
    private String countryShortName;

    @JsonProperty ("ad")
    private String address;

    @JsonProperty ("pr")
    private String phoneRaw;

    @JsonProperty ("tz")
    private String timeZone;

    @JsonProperty ("ic")
    private String inviteCode;

    @JsonProperty ("rj")
    private int remoteJoin;

    @JsonProperty ("bd")
    private String birthday;

    @JsonProperty ("ge")
    private GenderEnum gender;

    @JsonProperty("ul")
    private UserLevelEnum userLevel;

    @JsonProperty ("mp")
    private JsonUserMedicalProfile jsonUserMedicalProfile;

    /* Dependents can be anyone minor or other elderly family members. */
    @JsonProperty ("dp")
    private List<JsonProfile> dependents = new ArrayList<>();

    public JsonProfile() {
        //Required Default Constructor
    }

    private JsonProfile(
            String profileImage,
            String name,
            String mail,
            String countryShortName,
            String address,
            String phoneRaw,
            String timeZone,
            String inviteCode,
            String birthday,
            GenderEnum gender,
            UserLevelEnum userLevel,
            int remoteJoin
    ) {
        this.profileImage = profileImage;
        this.name = name;
        this.mail = mail;
        this.countryShortName = countryShortName;
        this.address = address;
        this.phoneRaw = Formatter.phoneFormatter(phoneRaw, countryShortName);
        this.timeZone = timeZone;
        this.inviteCode = inviteCode;
        this.birthday = birthday;
        this.gender = gender;
        this.userLevel = userLevel;
        this.remoteJoin = remoteJoin;
    }

    public static JsonProfile newInstance(UserProfileEntity userProfile, int remoteJoin) {
        return new JsonProfile(
                userProfile.getProfileImage(),
                userProfile.getName(),
                userProfile.getEmail(),
                userProfile.getCountryShortName(),
                userProfile.getAddress(),
                userProfile.getPhoneRaw(),
                userProfile.getTimeZone(),
                userProfile.getInviteCode(),
                userProfile.getBirthday(),
                userProfile.getGender(),
                userProfile.getLevel(),
                remoteJoin
        );
    }

    public String getProfileImage() {
        return profileImage;
    }

    public String getName() {
        return name;
    }

    public String getMail() {
        return mail;
    }

    public String getCountryShortName() {
        return countryShortName;
    }

    public String getAddress() {
        return address;
    }

    public String getPhoneRaw() {
        return phoneRaw;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public int getRemoteJoin() {
        return remoteJoin;
    }

    public String getBirthday() {
        return birthday;
    }

    public GenderEnum getGender() {
        return gender;
    }

    public UserLevelEnum getUserLevel() {
        return userLevel;
    }

    public JsonUserMedicalProfile getJsonUserMedicalProfile() {
        return jsonUserMedicalProfile;
    }

    public JsonProfile setJsonUserMedicalProfile(JsonUserMedicalProfile jsonUserMedicalProfile) {
        this.jsonUserMedicalProfile = jsonUserMedicalProfile;
        return this;
    }

    public List<JsonProfile> getDependents() {
        return dependents;
    }

    public JsonProfile addDependents(JsonProfile dependent) {
        this.dependents.add(dependent);
        return this;
    }
}
