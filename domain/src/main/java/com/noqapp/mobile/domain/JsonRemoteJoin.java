package com.noqapp.mobile.domain;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.noqapp.domain.AbstractDomain;

/**
 * User: hitender
 * Date: 4/11/17 10:20 AM
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
public class JsonRemoteJoin extends AbstractDomain {
    @JsonProperty ("rj")
    private int remoteJoinAvailable;

    private JsonRemoteJoin(int remoteJoinAvailable) {
        this.remoteJoinAvailable = remoteJoinAvailable;
    }

    public static JsonRemoteJoin newInstance(int remoteScanAvailable) {
        return new JsonRemoteJoin(remoteScanAvailable);
    }
}
