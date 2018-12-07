package com.noqapp.mobile.domain;

import com.noqapp.common.utils.AbstractDomain;
import com.noqapp.medical.domain.json.JsonMedicalRecord;
import com.noqapp.medical.domain.json.JsonMedicalRecordList;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.LinkedList;
import java.util.List;

/**
 * hitender
 * 2018-12-07 12:35
 */
@SuppressWarnings ({
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
public class JsonFeedList extends AbstractDomain {

    @JsonProperty("fds")
    private List<JsonFeed> jsonFeeds = new LinkedList<>();

    public List<JsonFeed> getJsonFeeds() {
        return jsonFeeds;
    }

    public JsonFeedList setJsonFeeds(List<JsonFeed> jsonFeeds) {
        this.jsonFeeds = jsonFeeds;
        return this;
    }

    public JsonFeedList addJsonFeed(JsonFeed jsonFeed) {
        this.jsonFeeds.add(jsonFeed);
        return this;
    }

}
