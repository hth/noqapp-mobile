package com.noqapp.mobile.view.controller.open;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.domain.body.client.SearchStoreQuery;
import com.noqapp.mobile.view.util.HttpRequestResponseParser;
import com.noqapp.search.elastic.domain.BizStoreElasticList;
import com.noqapp.search.elastic.domain.SearchBizStoreElasticList;
import com.noqapp.search.elastic.helper.GeoIP;
import com.noqapp.search.elastic.json.SearchElasticBizStoreSource;
import com.noqapp.search.elastic.service.BizStoreElasticService;
import com.noqapp.search.elastic.service.GeoIPLocationService;
import com.noqapp.search.elastic.service.SearchBizStoreElasticService;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

/**
 * hitender
 * 3/20/18 5:57 PM
 */
@SuppressWarnings ({
        "PMD.BeanMembersShouldSerialize",
        "PMD.LocalVariableCouldBeFinal",
        "PMD.MethodArgumentCouldBeFinal",
        "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/open/search")
public class SearchBusinessStoreController {
    private static final Logger LOG = LoggerFactory.getLogger(SearchBusinessStoreController.class);

    private boolean useRestHighLevel;
    private BizStoreElasticService bizStoreElasticService;
    private SearchBizStoreElasticService searchBizStoreElasticService;
    private GeoIPLocationService geoIPLocationService;
    private ApiHealthService apiHealthService;

    @Autowired
    public SearchBusinessStoreController(
        @Value("${Search.useRestHighLevel:false}")
        boolean useRestHighLevel,

        BizStoreElasticService bizStoreElasticService,
        SearchBizStoreElasticService searchBizStoreElasticService,
        GeoIPLocationService geoIPLocationService,
        ApiHealthService apiHealthService
    ) {
        this.useRestHighLevel = useRestHighLevel;

        this.bizStoreElasticService = bizStoreElasticService;
        this.searchBizStoreElasticService = searchBizStoreElasticService;
        this.geoIPLocationService = geoIPLocationService;
        this.apiHealthService = apiHealthService;
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    public String search(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput dt,

        @RequestBody
        SearchStoreQuery searchStoreQuery,

        HttpServletRequest request
    ) {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Searching for {} did={} dt={}", searchStoreQuery.getQuery(), did, dt);

        try {
            String query = searchStoreQuery.getQuery().getText();
            String ipAddress = HttpRequestResponseParser.getClientIpAddress(request);
            LOG.debug("Searching query={} city={} lat={} lng={} filters={} ip={}",
                query,
                searchStoreQuery.getCityName(),
                searchStoreQuery.getLatitude(),
                searchStoreQuery.getLongitude(),
                searchStoreQuery.getFilters(),
                ipAddress);

            SearchBizStoreElasticList searchBizStoreElasticList = new SearchBizStoreElasticList();
            GeoIP geoIp = getGeoIP(
                searchStoreQuery.getCityName().getText(),
                searchStoreQuery.getLatitude().getText(),
                searchStoreQuery.getLongitude().getText(),
                ipAddress,
                searchBizStoreElasticList);
            String geoHash = geoIp.getGeoHash();

            LOG.info("Search query=\"{}\" city=\"{}\" geoHash=\"{}\" ip=\"{}\"", query, searchStoreQuery.getCityName(), geoHash, ipAddress);
            if (StringUtils.isBlank(geoHash)) {
                /* Note: Fail safe when lat and lng are 0.0 and 0.0 */
                geoHash = "te7ut71tgd9n";
            }

            if (useRestHighLevel) {
                return bizStoreElasticService.executeNearMeSearchOnBizStoreUsingRestClient(
                    query,
                    searchStoreQuery.getCityName().getText(),
                    geoHash,
                    searchStoreQuery.getFilters().getText(),
                    searchStoreQuery.getScrollId().getText()).asJson();
            } else {
                List<SearchElasticBizStoreSource> elasticBizStoreSources = searchBizStoreElasticService.createBizStoreSearchDSLQuery(query, geoHash);
                return searchBizStoreElasticList.populateSearchBizStoreElasticArray(elasticBizStoreSources).asJson();
            }
        } catch (Exception e) {
            LOG.error("Failed processing search reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return new BizStoreElasticList().asJson();
        } finally {
            apiHealthService.insert(
                "/search",
                "search",
                SearchBusinessStoreController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Populated with lat and lng at the minimum, when missing uses IP address. */
    @PostMapping(
        value = {"/nearMe", "/healthCare", "/otherMerchant"},
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    public String nearMe(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput dt,

        @RequestBody
        SearchStoreQuery searchStoreQuery,

        HttpServletRequest request
    ) {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("NearMe invoked did={} dt={}", did, dt);

        try {
            String ipAddress = HttpRequestResponseParser.getClientIpAddress(request);
            LOG.debug("NearMe city={} lat={} lng={} filters={} ip={}",
                searchStoreQuery.getCityName(),
                searchStoreQuery.getLatitude(),
                searchStoreQuery.getLongitude(),
                searchStoreQuery.getFilters(),
                ipAddress);

            BizStoreElasticList bizStoreElasticList = new BizStoreElasticList();
            GeoIP geoIp = getGeoIP(
                searchStoreQuery.getCityName().getText(),
                searchStoreQuery.getLatitude().getText(),
                searchStoreQuery.getLongitude().getText(),
                ipAddress,
                bizStoreElasticList);
            String geoHash = geoIp.getGeoHash();
            if (StringUtils.isBlank(geoHash)) {
                /* Note: Fail safe when lat and lng are 0.0 and 0.0 */
                geoHash = "te7ut71tgd9n";
            }
            LOG.info("NearMe city=\"{}\" geoHash=\"{}\" ip=\"{}\"", searchStoreQuery.getCityName(), geoHash, ipAddress);

            /* Start of DSL query. */
//            List<ElasticBizStoreSource> elasticBizStoreSources = bizStoreElasticService.createBizStoreSearchDSLQuery(
//                    null,
//                    geoHash);
//
//            BizStoreElasticList bizStoreElastics = bizStoreElasticList.populateBizStoreElasticList(elasticBizStoreSources);
//            int hits = 0;
//            while (bizStoreElastics.getBizStoreElastics().size() < 10 && hits < 3) {
//                LOG.info("NearMe found size={}", bizStoreElastics.getBizStoreElastics().size());
//                elasticBizStoreSources = bizStoreElasticService.createBizStoreSearchDSLQuery(
//                        null,
//                        geoHash);
//
//                Collection<BizStoreElastic> additional = bizStoreElasticList.populateBizStoreElasticList(elasticBizStoreSources).getBizStoreElastics();
//                bizStoreElastics.getBizStoreElastics().addAll(additional);
//                hits ++;
//            }
            /* End of DSL query. */
            return bizStoreElasticService.nearMeSearch(geoHash, searchStoreQuery.getScrollId().getText()).asJson();
        } catch (Exception e) {
            LOG.error("Failed processing near me reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return new BizStoreElasticList().asJson();
        } finally {
            apiHealthService.insert(
                "/nearMe",
                "nearMe",
                SearchBusinessStoreController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    private GeoIP getGeoIP(String cityName, String lat, String lng, String ipAddress, BizStoreElasticList bizStoreElasticList) {
        GeoIP geoIp;
        if (StringUtils.isNotBlank(cityName)) {
            //TODO search based on city when lat lng is disabled
            geoIp = new GeoIP(ipAddress, "", Double.parseDouble(lat), Double.parseDouble(lng));
            bizStoreElasticList.setCityName(cityName);
        } else if (StringUtils.isNotBlank(lng) && StringUtils.isNotBlank(lat)) {
            geoIp = new GeoIP(ipAddress, "", Double.parseDouble(lat), Double.parseDouble(lng));
            bizStoreElasticList.setCityName(geoIp.getCityName());
        } else {
            geoIp = geoIPLocationService.getLocation(ipAddress);
            bizStoreElasticList.setCityName(geoIp.getCityName());
            LOG.info("City={} based on ip={}", geoIp.getCityName(), ipAddress);
        }
        return geoIp;
    }

    private GeoIP getGeoIP(String cityName, String lat, String lng, String ipAddress, SearchBizStoreElasticList searchBizStoreElasticList) {
        GeoIP geoIp;
        if (StringUtils.isNotBlank(cityName)) {
            //TODO search based on city when lat lng is disabled
            geoIp = new GeoIP(ipAddress, "", Double.parseDouble(lat), Double.parseDouble(lng));
            searchBizStoreElasticList.setCityName(cityName);
        } else if (StringUtils.isNotBlank(lng) && StringUtils.isNotBlank(lat)) {
            geoIp = new GeoIP(ipAddress, "", Double.parseDouble(lat), Double.parseDouble(lng));
            searchBizStoreElasticList.setCityName(geoIp.getCityName());
        } else {
            geoIp = geoIPLocationService.getLocation(ipAddress);
            searchBizStoreElasticList.setCityName(geoIp.getCityName());
            LOG.info("City={} based on ip={}", geoIp.getCityName(), ipAddress);
        }
        return geoIp;
    }
}
