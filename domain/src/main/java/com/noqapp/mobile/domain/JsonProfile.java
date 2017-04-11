package com.noqapp.mobile.domain;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.noqapp.domain.AbstractDomain;
import com.noqapp.domain.UserProfileEntity;

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
    @JsonProperty ("nm")
    private String name;

    @JsonProperty ("em")
    private String mail;

    @JsonProperty ("cs")
    private String countryShortName;

    @JsonProperty ("pr")
    private String phoneRaw;

    @JsonProperty ("tz")
    private String timeZone;

    @JsonProperty ("ic")
    private String inviteCode;

    @JsonProperty ("rs")
    private int remoteScanAvailable;

    @JsonProperty ("bd")
    private String birthday;

    @JsonProperty ("ge")
    private String gender;

    private JsonProfile(
            String name,
            String mail,
            String countryShortName,
            String phoneRaw,
            String timeZone,
            String inviteCode,
            String birthday,
            String gender,
            int remoteScanAvailable
    ) {
        this.name = name;
        this.mail = mail;
        this.countryShortName = countryShortName;
        this.phoneRaw = phoneRaw;
        this.timeZone = timeZone;
        this.inviteCode = inviteCode;
        this.birthday = birthday;
        this.gender = gender;
        this.remoteScanAvailable = remoteScanAvailable;
    }

    public static JsonProfile newInstance(UserProfileEntity userProfile, int remoteScanAvailable) {
        return new JsonProfile(
                userProfile.getName(),
                userProfile.getEmail(),
                userProfile.getCountryShortName(),
                userProfile.getPhoneRaw(),
                userProfile.getTimeZone(),
                userProfile.getInviteCode(),
                userProfile.getBirthday(),
                userProfile.getGender(),
                remoteScanAvailable
        );
    }
}
