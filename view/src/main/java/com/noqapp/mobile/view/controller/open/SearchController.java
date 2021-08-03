package com.noqapp.mobile.view.controller.open;

import static com.noqapp.common.utils.Constants.SUGGESTED_SEARCH;
import static org.apiguardian.api.API.Status.STABLE;

import com.noqapp.common.utils.AbstractDomain;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.UserSearchEntity;
import com.noqapp.domain.types.BusinessTypeEnum;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.domain.body.client.SearchQuery;
import com.noqapp.mobile.view.util.HttpRequestResponseParser;
import com.noqapp.search.elastic.domain.BizStoreElasticList;
import com.noqapp.search.elastic.domain.BizStoreSearchElasticList;
import com.noqapp.search.elastic.helper.GeoIP;
import com.noqapp.search.elastic.json.ElasticBizStoreSearchSource;
import com.noqapp.search.elastic.service.BizStoreSearchElasticService;
import com.noqapp.search.elastic.service.BizStoreSpatialElasticService;
import com.noqapp.search.elastic.service.GeoIPLocationService;
import com.noqapp.service.BizService;
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

import org.apiguardian.api.API;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

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
@RequestMapping(value = "/open/search")
public class SearchController {
    private static final Logger LOG = LoggerFactory.getLogger(SearchController.class);

    private boolean useRestHighLevel;

    private BizStoreSpatialElasticService bizStoreSpatialElasticService;
    private BizStoreSearchElasticService bizStoreSearchElasticService;
    private BizService bizService;
    private GeoIPLocationService geoIPLocationService;
    private UserSearchService userSearchService;
    private ApiHealthService apiHealthService;

    @Autowired
    public SearchController(
        @Value("${search.useRestHighLevel:false}")
        boolean useRestHighLevel,

        BizStoreSpatialElasticService bizStoreSpatialElasticService,
        BizStoreSearchElasticService bizStoreSearchElasticService,
        BizService bizService,
        GeoIPLocationService geoIPLocationService,
        UserSearchService userSearchService,
        ApiHealthService apiHealthService
    ) {
        this.useRestHighLevel = useRestHighLevel;

        this.bizStoreSpatialElasticService = bizStoreSpatialElasticService;
        this.bizStoreSearchElasticService = bizStoreSearchElasticService;
        this.bizService = bizService;
        this.geoIPLocationService = geoIPLocationService;
        this.userSearchService = userSearchService;
        this.apiHealthService = apiHealthService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public String searchHint(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader("X-R-DT")
        ScrubbedInput dt
    ) {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();

        LOG.info("Search Hint did={} dt={}", did, dt);
        try {
            return new SearchQuery().setSuggestedSearch(SUGGESTED_SEARCH).asJson();
        } catch (Exception e) {
            LOG.error("Failed search hint reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return new SearchQuery().setSuggestedSearch(SUGGESTED_SEARCH).asJson();
        } finally {
            apiHealthService.insert(
                "/searchHint",
                "searchHint",
                SearchController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public String search(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput dt,

        @RequestBody
        SearchQuery searchQuery,

        HttpServletRequest request
    ) {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Searching for query=\"{}\" {} did={} dt={}", searchQuery.getQuery(), searchQuery.getCityName(), did, dt);

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
                LOG.info("Search query=\"{}\" city=\"{}\" geoHash={} ip={} did={}", query, searchQuery.getCityName(), geoHash, ipAddress, did.getText());
                return bizStoreSearchElasticService.executeNearMeSearchOnBizStoreUsingRestClient(
                    query,
                    null == searchQuery.getCityName() ? "": searchQuery.getCityName().getText(),
                    geoHash,
                    searchQuery.getFilters().getText(),
                    searchQuery.getScrollId().getText()).asJson();
            } else {
                List<ElasticBizStoreSearchSource> elasticBizStoreSearchSources = bizStoreSearchElasticService.createBizStoreSearchDSLQuery(query, geoHash);
                LOG.info("Search query=\"{}\" city=\"{}\" geoHash={} ip={} did={} result={}", query, searchQuery.getCityName(), geoHash, ipAddress, did.getText(), elasticBizStoreSearchSources.size());
                UserSearchEntity userSearch = new UserSearchEntity()
                    .setQuery(searchQuery.getQuery().getText())
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
                SearchController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    @PostMapping(
        value = "/nearMe",
        produces = MediaType.APPLICATION_JSON_VALUE)
    public String nearMe(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput dt,

        @RequestBody
        SearchQuery searchQuery,

        HttpServletRequest request
    ) {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("NearMe invoked did={} dt={} businessType={} \"{}\"", did, dt, searchQuery.getSearchedOnBusinessType(), searchQuery.getCityName());

        try {
            String ipAddress = HttpRequestResponseParser.getClientIpAddress(request);
            LOG.debug("NearMe city=\"{}\" lat={} lng={} filters={} ip={} did={} bt={}",
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

            LOG.info("NearMe Business {} city=\"{}\" geoHash={} ip={} did={} bt={}", searchQuery.getSearchedOnBusinessType(), searchQuery.getCityName(), geoHash, ipAddress, did.getText(), searchQuery.getSearchedOnBusinessType());
            return bizStoreSpatialElasticService.nearMeExcludedBusinessTypes(geoHash, searchQuery.getScrollId().getText()).asJson();
        } catch (Exception e) {
            LOG.error("Failed listing nearMe business={} reason={}", searchQuery.getSearchedOnBusinessType(), e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return new BizStoreElasticList()
                .setSearchedOnBusinessType(searchQuery.getSearchedOnBusinessType())
                .asJson();
        } finally {
            apiHealthService.insert(
                "/nearMe",
                "nearMe",
                SearchController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Populated with lat and lng at the minimum, when missing uses IP address. */
    @API(status = STABLE, since = "1.3.112")
    @PostMapping(
        value = "/business",
        produces = MediaType.APPLICATION_JSON_VALUE)
    public String business(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput dt,

        @RequestBody
        SearchQuery searchQuery,

        HttpServletRequest request
    ) {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Business invoked did={} dt={} businessType={} cityName=\"{}\"", did, dt, searchQuery.getSearchedOnBusinessType(), searchQuery.getCityName());

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

            LOG.info("Business city=\"{}\" geoHash={} ip={} did={} bt={}", searchQuery.getCityName(), geoHash, ipAddress, did.getText(), searchQuery.getSearchedOnBusinessType());
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
                case ZZ:
                default:
                    return bizStoreSpatialElasticService.nearMeExcludedBusinessTypes(
                        new ArrayList<> () {
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
                SearchController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Populated with lat and lng at the minimum, when missing uses IP address. */
    @PostMapping(
        value = "/otherMerchant",
        produces = MediaType.APPLICATION_JSON_VALUE)
    public String otherMerchant(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput dt,

        @RequestBody
        SearchQuery searchQuery,

        HttpServletRequest request
    ) {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("OtherMerchant invoked did={} dt={} \"{}\"", did, dt, searchQuery.getCityName());

        try {
            String ipAddress = HttpRequestResponseParser.getClientIpAddress(request);
            LOG.debug("OtherMerchant city=\"{}\" lat={} lng={} filters={} ip={} did={}",
                searchQuery.getCityName(),
                searchQuery.getLatitude(),
                searchQuery.getLongitude(),
                searchQuery.getFilters(),
                ipAddress,
                did.getText());

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
            LOG.info("OtherMerchant city=\"{}\" geoHash={} ip={} did={}", searchQuery.getCityName(), geoHash, ipAddress, did.getText());
            return bizStoreSpatialElasticService.nearMeExcludedBusinessTypes(
                new ArrayList<> () {
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
                geoHash,
                searchQuery.getScrollId().getText()).asJson();
        } catch (Exception e) {
            LOG.error("Failed processing near me reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return new BizStoreElasticList().asJson();
        } finally {
            apiHealthService.insert(
                "/otherMerchant",
                "otherMerchant",
                SearchController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Populated with lat and lng at the minimum, when missing uses IP address. */
    @PostMapping(
        value = "/canteen",
        produces = MediaType.APPLICATION_JSON_VALUE)
    public String canteen(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput dt,

        @RequestBody
        SearchQuery searchQuery,

        HttpServletRequest request
    ) {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Canteen invoked did={} dt={} {}", did, dt, searchQuery.getCityName());

        try {
            String ipAddress = HttpRequestResponseParser.getClientIpAddress(request);
            LOG.debug("Canteen nearMe city=\"{}\" lat={} lng={} filters={} ip={} did={}",
                searchQuery.getCityName(),
                searchQuery.getLatitude(),
                searchQuery.getLongitude(),
                searchQuery.getFilters(),
                ipAddress,
                did.getText());

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
            LOG.info("Canteen NearMe city=\"{}\" geoHash={} ip={} did={}", searchQuery.getCityName(), geoHash, ipAddress, did.getText());
            return bizStoreSpatialElasticService.nearMeExcludedBusinessTypes(
                BusinessTypeEnum.excludeCanteen(),
                BusinessTypeEnum.includeCanteen(),
                geoHash,
                searchQuery.getScrollId().getText()).asJson();
        } catch (Exception e) {
            LOG.error("Failed processing near me reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return new BizStoreElasticList().asJson();
        } finally {
            apiHealthService.insert(
                "/canteen",
                "canteen",
                SearchController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Populated with lat and lng at the minimum, when missing uses IP address. */
    @PostMapping(
        value = "/restaurant",
        produces = MediaType.APPLICATION_JSON_VALUE)
    public String restaurant(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput dt,

        @RequestBody
        SearchQuery searchQuery,

        HttpServletRequest request
    ) {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Restaurant invoked did={} dt={} \"{}\"", did, dt, searchQuery.getCityName());

        try {
            String ipAddress = HttpRequestResponseParser.getClientIpAddress(request);
            LOG.debug("Restaurant nearMe city=\"{}\" lat={} lng={} filters={} ip={} did={}",
                searchQuery.getCityName(),
                searchQuery.getLatitude(),
                searchQuery.getLongitude(),
                searchQuery.getFilters(),
                ipAddress,
                did.getText());

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
            LOG.info("Restaurant NearMe city=\"{}\" geoHash={} ip={} did={}", searchQuery.getCityName(), geoHash, ipAddress, did.getText());
            return bizStoreSpatialElasticService.nearMeExcludedBusinessTypes(
                BusinessTypeEnum.excludeRestaurant(),
                BusinessTypeEnum.includeRestaurant(),
                geoHash,
                searchQuery.getScrollId().getText()).asJson();
        } catch (Exception e) {
            LOG.error("Failed processing near me reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return new BizStoreElasticList().asJson();
        } finally {
            apiHealthService.insert(
                "/restaurant",
                "restaurant",
                SearchController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Populated with lat and lng at the minimum, when missing uses IP address. */
    @PostMapping(
        value = "/placeOfWorship",
        produces = MediaType.APPLICATION_JSON_VALUE)
    public String placeOfWorship(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput dt,

        @RequestBody
        SearchQuery searchQuery,

        HttpServletRequest request
    ) {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("PlaceOfWorship invoked did={} dt={} \"{}\"", did, dt, searchQuery.getCityName());

        try {
            String ipAddress = HttpRequestResponseParser.getClientIpAddress(request);
            LOG.debug("PlaceOfWorship nearMe city=\"{}\" lat={} lng={} filters={} ip={} did={}",
                searchQuery.getCityName(),
                searchQuery.getLatitude(),
                searchQuery.getLongitude(),
                searchQuery.getFilters(),
                ipAddress,
                did.getText());

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
            LOG.info("PlaceOfWorship NearMe city=\"{}\" geoHash={} ip={} did={}", searchQuery.getCityName(), geoHash, ipAddress, did.getText());
            return bizStoreSpatialElasticService.nearMeExcludedBusinessTypes(
                BusinessTypeEnum.excludePlaceOfWorship(),
                BusinessTypeEnum.includePlaceOfWorship(),
                geoHash,
                searchQuery.getScrollId().getText()).asJson();
        } catch (Exception e) {
            LOG.error("Failed processing worship near me reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return new BizStoreElasticList().asJson();
        } finally {
            apiHealthService.insert(
                "/placeOfWorship",
                "placeOfWorship",
                SearchController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Populated with lat and lng at the minimum, when missing uses IP address. */
    @PostMapping(
        value = "/healthCare",
        produces = MediaType.APPLICATION_JSON_VALUE)
    public String healthCare(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput dt,

        @RequestBody
        SearchQuery searchQuery,

        HttpServletRequest request
    ) {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("HealthCare invoked did={} dt={} \"{}\"", did, dt, searchQuery.getCityName());

        try {
            String ipAddress = HttpRequestResponseParser.getClientIpAddress(request);
            LOG.debug("HealthCare city=\"{}\" lat={} lng={} filters={} ip={} did={}",
                searchQuery.getCityName(),
                searchQuery.getLatitude(),
                searchQuery.getLongitude(),
                searchQuery.getFilters(),
                ipAddress,
                did.getText());

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
            LOG.info("HealthCare city=\"{}\" geoHash={} ip={} did={}", searchQuery.getCityName(), geoHash, ipAddress, did.getText());
            return bizStoreSpatialElasticService.nearMeExcludedBusinessTypes(
                BusinessTypeEnum.excludeHospital(),
                BusinessTypeEnum.includeHospital(),
                geoHash,
                searchQuery.getScrollId().getText()).asJson();
        } catch (Exception e) {
            LOG.error("Failed processing near me reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return new BizStoreElasticList().asJson();
        } finally {
            apiHealthService.insert(
                "/healthCare",
                "healthCare",
                SearchController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    @PostMapping(
        value = "/kiosk",
        produces = MediaType.APPLICATION_JSON_VALUE)
    public String kiosk(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput dt,

        @RequestBody
        SearchQuery searchQuery,

        HttpServletRequest request
    ) {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Kiosk search query=\"{}\" qr=\"{}\" did={} dt={} \"{}\"", searchQuery.getQuery(), searchQuery.getCodeQR(), did, dt, searchQuery.getCityName());

        try {
            String query = searchQuery.getQuery().getText();
            String codeQR = searchQuery.getCodeQR().getText();
            String ipAddress = HttpRequestResponseParser.getClientIpAddress(request);
            LOG.debug("Kiosk search query=\"{}\" city=\"{}\" lat={} lng={} filters={} ip={}",
                query,
                searchQuery.getCityName(),
                searchQuery.getLatitude(),
                searchQuery.getLongitude(),
                searchQuery.getFilters(),
                ipAddress);

            BizStoreEntity bizStore = bizService.findByCodeQR(codeQR);
            if (null == bizStore) {
                LOG.error("Could not find store with codeQR={}", codeQR);
                return new BizStoreElasticList().asJson();
            } else {
                switch (bizStore.getBusinessType()) {
                    case DO:
                    case BK:
                        return bizStoreSearchElasticService.kioskSearchUsingRestClient(
                            query,
                            bizStore.getBizName().getId(),
                            searchQuery.getScrollId().getText()).asJson();
                    default:
                        LOG.error("Reached unsupported condition for bizNameId={} businessType={}",
                            bizStore.getBizName().getId(),
                            bizStore.getBizName().getBusinessType());

                        return new BizStoreElasticList().asJson();
                }
            }
        } catch (Exception e) {
            LOG.error("Failed processing search reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return new BizStoreElasticList().asJson();
        } finally {
            apiHealthService.insert(
                "/kiosk",
                "kiosk",
                SearchController.class.getName(),
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

        if (abstractDomain instanceof BizStoreElasticList) {
            ((BizStoreElasticList) abstractDomain).setCityName(geoIp.getCityName());
        } else if(abstractDomain instanceof BizStoreSearchElasticList) {
            ((BizStoreSearchElasticList) abstractDomain).setCityName(geoIp.getCityName());
        }

        LOG.info("city=\"{}\" based on ip={}", geoIp.getCityName(), ipAddress);
        return geoIp;
    }
}
