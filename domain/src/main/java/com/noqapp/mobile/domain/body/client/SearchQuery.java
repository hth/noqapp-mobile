package com.noqapp.mobile.domain.body.client;

import com.noqapp.common.utils.AbstractDomain;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.types.BusinessTypeEnum;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;
import java.util.StringJoiner;

/**
 * User: hitender
 * Date: 4/2/17 6:37 PM
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
public class SearchQuery extends AbstractDomain {

    @JsonProperty("q")
    private ScrubbedInput query;

    @JsonProperty("cityName")
    private ScrubbedInput cityName;

    @JsonProperty("lat")
    private ScrubbedInput latitude;

    @JsonProperty("lng")
    private ScrubbedInput longitude;

    @JsonProperty("scrollId")
    private ScrubbedInput scrollId;

    /* Apply specific filter on fields set on app, like city. */
    @JsonProperty("filters")
    private ScrubbedInput filters;

    @JsonProperty("qr")
    private ScrubbedInput codeQR;

    @JsonProperty("bt")
    private BusinessTypeEnum searchedOnBusinessType;

    @JsonProperty("ps")
    private List<String> pastSearch;

    @JsonProperty("ss")
    private List<String> suggestedSearch;

    public ScrubbedInput getQuery() {
        return query;
    }

    public SearchQuery setQuery(ScrubbedInput query) {
        this.query = query;
        return this;
    }

    public ScrubbedInput getCityName() {
        return cityName;
    }

    public SearchQuery setCityName(ScrubbedInput cityName) {
        this.cityName = cityName;
        return this;
    }

    public ScrubbedInput getLatitude() {
        return latitude;
    }

    public SearchQuery setLatitude(ScrubbedInput latitude) {
        this.latitude = latitude;
        return this;
    }

    public ScrubbedInput getLongitude() {
        return longitude;
    }

    public SearchQuery setLongitude(ScrubbedInput longitude) {
        this.longitude = longitude;
        return this;
    }

    public ScrubbedInput getFilters() {
        return filters;
    }

    public SearchQuery setFilters(ScrubbedInput filters) {
        this.filters = filters;
        return this;
    }

    public ScrubbedInput getScrollId() {
        return scrollId;
    }

    public SearchQuery setScrollId(ScrubbedInput scrollId) {
        this.scrollId = scrollId;
        return this;
    }

    public ScrubbedInput getCodeQR() {
        return codeQR;
    }

    public SearchQuery setCodeQR(ScrubbedInput codeQR) {
        this.codeQR = codeQR;
        return this;
    }

    public BusinessTypeEnum getSearchedOnBusinessType() {
        return searchedOnBusinessType;
    }

    public SearchQuery setSearchedOnBusinessType(BusinessTypeEnum searchedOnBusinessType) {
        this.searchedOnBusinessType = searchedOnBusinessType;
        return this;
    }

    public List<String> getPastSearch() {
        return pastSearch;
    }

    public SearchQuery setPastSearch(List<String> pastSearch) {
        this.pastSearch = pastSearch;
        return this;
    }

    public List<String> getSuggestedSearch() {
        return suggestedSearch;
    }

    public SearchQuery setSuggestedSearch(List<String> suggestedSearch) {
        this.suggestedSearch = suggestedSearch;
        return this;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SearchQuery.class.getSimpleName() + "[", "]")
            .add("query=" + query)
            .add("cityName=" + cityName)
            .add("latitude=" + latitude)
            .add("longitude=" + longitude)
            .add("codeQR=" + codeQR)
            .add("searchedOnBusinessType=" + searchedOnBusinessType)
            .toString();
    }
}
