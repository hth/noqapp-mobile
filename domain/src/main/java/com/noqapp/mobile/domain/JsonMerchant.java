package com.noqapp.mobile.domain;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.noqapp.common.utils.AbstractDomain;
import com.noqapp.domain.json.JsonProfessionalProfile;
import com.noqapp.domain.json.JsonTopic;

import java.util.ArrayList;
import java.util.List;

/**
 * User: hitender
 * Date: 4/22/17 10:49 AM
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
public class JsonMerchant extends AbstractDomain {
    @JsonProperty ("p")
    private JsonProfile jsonProfile;

    @JsonProperty ("pp")
    private JsonProfessionalProfile jsonProfessionalProfile;

    @JsonProperty ("ts")
    private List<JsonTopic> topics = new ArrayList<>();

    public JsonProfile getJsonProfile() {
        return jsonProfile;
    }

    public JsonMerchant setJsonProfile(JsonProfile jsonProfile) {
        this.jsonProfile = jsonProfile;
        return this;
    }

    public JsonProfessionalProfile getJsonProfessionalProfile() {
        return jsonProfessionalProfile;
    }

    public JsonMerchant setJsonProfessionalProfile(JsonProfessionalProfile jsonProfessionalProfile) {
        this.jsonProfessionalProfile = jsonProfessionalProfile;
        return this;
    }

    public List<JsonTopic> getTopics() {
        return topics;
    }

    public JsonMerchant setTopics(List<JsonTopic> topics) {
        this.topics = topics;
        return this;
    }
}
