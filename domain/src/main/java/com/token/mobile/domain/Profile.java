package com.token.mobile.domain;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.token.domain.AbstractDomain;
import com.token.domain.UserProfileEntity;

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
public class Profile extends AbstractDomain {
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

    private Profile(
            String name,
            String mail,
            String countryShortName,
            String phoneRaw,
            String timeZone,
            String inviteCode,
            int remoteScanAvailable
    ) {
        this.name = name;
        this.mail = mail;
        this.countryShortName = countryShortName;
        this.phoneRaw = phoneRaw;
        this.timeZone = timeZone;
        this.inviteCode = inviteCode;
        this.remoteScanAvailable = remoteScanAvailable;
    }

    public static Profile newInstance(UserProfileEntity userProfile, int remoteScanAvailable) {
        return new Profile(
                userProfile.getName(),
                userProfile.getEmail(),
                userProfile.getCountryShortName(),
                userProfile.getPhoneRaw(),
                userProfile.getTimeZone(),
                userProfile.getInviteCode(),
                remoteScanAvailable
        );
    }
}
