package com.noqapp.mobile.view.controller.api.client;

import static com.noqapp.common.utils.Constants.SUGGESTED_SEARCH;
import static com.noqapp.mobile.view.controller.api.client.TokenQueueAPIController.authorizeRequest;

import com.noqapp.common.utils.AbstractDomain;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.UserSearchEntity;
import com.noqapp.domain.types.BusinessTypeEnum;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.domain.body.client.SearchQuery;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.view.util.HttpRequestResponseParser;
import com.noqapp.search.elastic.domain.BizStoreElasticList;
import com.noqapp.search.elastic.domain.BizStoreSearchElasticList;
import com.noqapp.search.elastic.helper.GeoIP;
import com.noqapp.search.elastic.json.ElasticBizStoreSearchSource;
import com.noqapp.search.elastic.service.BizStoreSearchElasticService;
import com.noqapp.search.elastic.service.BizStoreSpatialElasticService;
import com.noqapp.search.elastic.service.GeoIPLocationService;
import com.noqapp.service.UserSearchService;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * hitender
 * 3/20/18 5:57 PM
 */
@SuppressWarnings({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/api/c/search")
public class SearchAPIController {
    private static final Logger LOG = LoggerFactory.getLogger(SearchAPIController.class);

    private boolean useRestHighLevel;

    private AuthenticateMobileService authenticateMobileService;
    private BizStoreSpatialElasticService bizStoreSpatialElasticService;
    private BizStoreSearchElasticService bizStoreSearchElasticService;
    private GeoIPLocationService geoIPLocationService;
    private UserSearchService userSearchService;
    private ApiHealthService apiHealthService;

    @Autowired
    public SearchAPIController(
        @Value("${search.useRestHighLevel:false}")
        boolean useRestHighLevel,

        AuthenticateMobileService authenticateMobileService,
        BizStoreSpatialElasticService bizStoreSpatialElasticService,
        BizStoreSearchElasticService bizStoreSearchElasticService,
        GeoIPLocationService geoIPLocationService,
        UserSearchService userSearchService,
        ApiHealthService apiHealthService
    ) {
        this.useRestHighLevel = useRestHighLevel;

        this.authenticateMobileService = authenticateMobileService;
        this.bizStoreSpatialElasticService = bizStoreSpatialElasticService;
        this.bizStoreSearchElasticService = bizStoreSearchElasticService;
        this.geoIPLocationService = geoIPLocationService;
        this.userSearchService = userSearchService;
        this.apiHealthService = apiHealthService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public String searchHint(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader("X-R-DT")
        ScrubbedInput dt,

        @RequestHeader("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader("X-R-AUTH")
        ScrubbedInput auth,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        LOG.info("Search Hint did={} dt={} mail={}", did, dt, mail);
        try {
            SearchQuery searchQuery = new SearchQuery()
                .setPastSearch(userSearchService.lastFewSearches(qid, 5))
                .setSuggestedSearch(SUGGESTED_SEARCH);
            return searchQuery.asJson();
        } catch (Exception e) {
            LOG.error("Failed search hint reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return new SearchQuery().setSuggestedSearch(SUGGESTED_SEARCH).asJson();
        } finally {
            apiHealthService.insert(
                "/searchHint",
                "searchHint",
                SearchAPIController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public String search(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader("X-R-DT")
        ScrubbedInput dt,

        @RequestHeader("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        SearchQuery searchQuery,

        HttpServletRequest request,
        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        LOG.info("Searching for query=\"{}\" {} did={} dt={} mail={}", searchQuery.getQuery(), searchQuery.getCityName(), did, dt, mail);
        try {
            String query = searchQuery.getQuery().getText();
            String ipAddress = HttpRequestResponseParser.getClientIpAddress(request);
            LOG.debug("Searching query=\"{}\" city=\"{}\" lat={} lng={} filters={} ip={}",
                query,
                searchQuery.getCityName(),
                searchQuery.getLatitude(),
                searchQuery.getLongitude(),
                searchQuery.getFilters(),
                ipAddress);

            BizStoreSearchElasticList bizStoreSearchElasticList = new BizStoreSearchElasticList();
            GeoIP geoIp = getGeoIP(
                null == searchQuery.getCityName() ? "": searchQuery.getCityName().getText(),
                searchQuery.getLatitude().getText(),
                searchQuery.getLongitude().getText(),
                ipAddress,
                bizStoreSearchElasticList);
            String geoHash = geoIp.getGeoHash();
            if (StringUtils.isBlank(geoHash) || geoHash.equalsIgnoreCase("s00000000000")) {
                /* Note: Fail safe when lat and lng are 0.0 and 0.0 */
                geoHash = "te7ut71tgd9n";
            }

            if (useRestHighLevel) {
                LOG.info("Search query=\"{}\" city=\"{}\" geoHash={} ip={} did={} qid={}", query, searchQuery.getCityName(), geoHash, ipAddress, did.getText(), qid);
                return bizStoreSearchElasticService.executeNearMeSearchOnBizStoreUsingRestClient(
                    query,
                    null == searchQuery.getCityName() ? "": searchQuery.getCityName().getText(),
                    geoHash,
                    searchQuery.getFilters().getText(),
                    searchQuery.getScrollId().getText()).asJson();
            } else {
                List<ElasticBizStoreSearchSource> elasticBizStoreSearchSources = bizStoreSearchElasticService.createBizStoreSearchDSLQuery(query, geoHash);
                LOG.info("Search query=\"{}\" city=\"{}\" geoHash={} ip={} did={} qid={} result={}", query, searchQuery.getCityName(), geoHash, ipAddress, did.getText(), qid, elasticBizStoreSearchSources.size());
                UserSearchEntity userSearch = new UserSearchEntity()
                    .setQuery(query)
                    .setQid(qid)
                    .setDid(did.getText())
                    .setCityName(null == searchQuery.getCityName() ? "": searchQuery.getCityName().getText())
                    .setGeoHash(geoHash)
                    .setResultCount(elasticBizStoreSearchSources.size());
                userSearchService.save(userSearch);
                return bizStoreSearchElasticList.populateSearchBizStoreElasticArray(elasticBizStoreSearchSources).asJson();
            }
        } catch (Exception e) {
            LOG.error("Failed search reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return new BizStoreElasticList().asJson();
        } finally {
            apiHealthService.insert(
                "/search",
                "search",
                SearchAPIController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Populated with lat and lng at the minimum, when missing uses IP address. */
    @PostMapping(
        value = "/business",
        produces = MediaType.APPLICATION_JSON_VALUE)
    public String business(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput dt,

        @RequestHeader("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        SearchQuery searchQuery,

        HttpServletRequest request,
        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        LOG.info("Business businessType={} \"{}\" did={} dt={} mail={}", searchQuery.getSearchedOnBusinessType(),  searchQuery.getCityName(), did, dt, mail);
        try {
            String ipAddress = HttpRequestResponseParser.getClientIpAddress(request);
            LOG.debug("Business city=\"{}\" lat={} lng={} filters={} ip={} did={} bt={}",
                searchQuery.getCityName(),
                searchQuery.getLatitude(),
                searchQuery.getLongitude(),
                searchQuery.getFilters(),
                ipAddress,
                did.getText(),
                searchQuery.getSearchedOnBusinessType());

            BizStoreElasticList bizStoreElasticList = new BizStoreElasticList();
            GeoIP geoIp = getGeoIP(
                null == searchQuery.getCityName() ? "": searchQuery.getCityName().getText(),
                searchQuery.getLatitude().getText(),
                searchQuery.getLongitude().getText(),
                ipAddress,
                bizStoreElasticList);
            String geoHash = geoIp.getGeoHash();
            if (StringUtils.isBlank(geoHash)) {
                /* Note: Fail safe when lat and lng are 0.0 and 0.0 */
                geoHash = "te7ut71tgd9n";
            }

            LOG.info("Business {} city=\"{}\" geoHash={} ip={} did={} bt={}", searchQuery.getSearchedOnBusinessType(), searchQuery.getCityName(), geoHash, ipAddress, did.getText(), searchQuery.getSearchedOnBusinessType());
            switch (searchQuery.getSearchedOnBusinessType()) {
                case CD:
                case CDQ:
                    return bizStoreSpatialElasticService.nearMeExcludedBusinessTypes(
                        BusinessTypeEnum.excludeCanteen(),
                        BusinessTypeEnum.includeCanteen(),
                        searchQuery.getSearchedOnBusinessType(),
                        geoHash,
                        searchQuery.getScrollId().getText()).asJson();
                case RS:
                case RSQ:
                    return bizStoreSpatialElasticService.nearMeExcludedBusinessTypes(
                        BusinessTypeEnum.excludeRestaurant(),
                        BusinessTypeEnum.includeRestaurant(),
                        searchQuery.getSearchedOnBusinessType(),
                        geoHash,
                        searchQuery.getScrollId().getText()).asJson();
                case HS:
                case DO:
                    return bizStoreSpatialElasticService.nearMeExcludedBusinessTypes(
                        BusinessTypeEnum.excludeHospital(),
                        BusinessTypeEnum.includeHospital(),
                        searchQuery.getSearchedOnBusinessType(),
                        geoHash,
                        searchQuery.getScrollId().getText()).asJson();
                case PW:
                    return bizStoreSpatialElasticService.nearMeExcludedBusinessTypes(
                        BusinessTypeEnum.excludePlaceOfWorship(),
                        BusinessTypeEnum.includePlaceOfWorship(),
                        searchQuery.getSearchedOnBusinessType(),
                        geoHash,
                        searchQuery.getScrollId().getText()).asJson();
                case PR:
                    return bizStoreSpatialElasticService.nearMeExcludedBusinessTypes(
                        BusinessTypeEnum.excludePropertyRental(),
                        BusinessTypeEnum.includePropertyRental(),
                        searchQuery.getSearchedOnBusinessType(),
                        geoHash,
                        searchQuery.getScrollId().getText()).asJson();
                case HI:
                    return bizStoreSpatialElasticService.nearMeExcludedBusinessTypes(
                        BusinessTypeEnum.excludeHouseholdItem(),
                        BusinessTypeEnum.includeHouseholdItem(),
                        searchQuery.getSearchedOnBusinessType(),
                        geoHash,
                        searchQuery.getScrollId().getText()).asJson();
                case ZZ:
                default:
                    return bizStoreSpatialElasticService.nearMeExcludedBusinessTypes(
                        new ArrayList<>() {
                            private static final long serialVersionUID = -1371033286799633594L;

                            {
                                add(BusinessTypeEnum.CD);
                                add(BusinessTypeEnum.CDQ);
                                add(BusinessTypeEnum.RS);
                                add(BusinessTypeEnum.RSQ);
                                add(BusinessTypeEnum.DO);
                                add(BusinessTypeEnum.HS);
                                add(BusinessTypeEnum.PW);
                            }
                        },
                        new ArrayList<> () {
                            private static final long serialVersionUID = 6730480722223803218L;

                            {
                                add(BusinessTypeEnum.CD);
                                add(BusinessTypeEnum.CDQ);
                                add(BusinessTypeEnum.RS);
                                add(BusinessTypeEnum.RSQ);
                                add(BusinessTypeEnum.DO);
                                add(BusinessTypeEnum.HS);
                                add(BusinessTypeEnum.PW);
                            }
                        },
                        searchQuery.getSearchedOnBusinessType(),
                        geoHash,
                        searchQuery.getScrollId().getText()).asJson();
            }
        } catch (Exception e) {
            LOG.error("Failed listing business={} reason={}", searchQuery.getSearchedOnBusinessType(), e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return new BizStoreElasticList()
                .setSearchedOnBusinessType(searchQuery.getSearchedOnBusinessType())
                .asJson();
        } finally {
            apiHealthService.insert(
                "/business",
                "business",
                SearchAPIController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    private GeoIP getGeoIP(String cityName, String lat, String lng, String ipAddress, AbstractDomain abstractDomain) {
        GeoIP geoIp;
        if (StringUtils.isNotBlank(lng) && StringUtils.isNotBlank(lat)) {
            geoIp = new GeoIP(ipAddress, cityName, Double.parseDouble(lat), Double.parseDouble(lng));
        } else {
            geoIp = geoIPLocationService.getLocation(ipAddress);
        }

        if (abstractDomain instanceof BizStoreSearchElasticList) {
            ((BizStoreSearchElasticList) abstractDomain).setCityName(geoIp.getCityName());
        }

        LOG.info("city=\"{}\" based on ip={}", geoIp.getCityName(), ipAddress);
        return geoIp;
    }
}
