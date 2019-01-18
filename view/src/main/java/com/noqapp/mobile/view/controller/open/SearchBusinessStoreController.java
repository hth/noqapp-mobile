package com.noqapp.mobile.view.controller.open;

import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MOBILE_JSON;

import com.noqapp.common.utils.ParseJsonStringToMap;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.common.util.ErrorEncounteredJson;
import com.noqapp.mobile.domain.body.client.SearchStoreQuery;
import com.noqapp.mobile.view.util.HttpRequestResponseParser;
import com.noqapp.search.elastic.domain.BizStoreElasticList;
import com.noqapp.search.elastic.helper.GeoIP;
import com.noqapp.search.elastic.json.ElasticBizStoreSource;
import com.noqapp.search.elastic.service.BizStoreElasticService;
import com.noqapp.search.elastic.service.GeoIPLocationService;

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

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

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
    private GeoIPLocationService geoIPLocationService;
    private ApiHealthService apiHealthService;

    @Autowired
    public SearchBusinessStoreController(
            @Value("${Search.useRestHighLevel:false}")
            boolean useRestHighLevel,

            BizStoreElasticService bizStoreElasticService,
            GeoIPLocationService geoIPLocationService,
            ApiHealthService apiHealthService
    ) {
        this.useRestHighLevel = useRestHighLevel;

        this.bizStoreElasticService = bizStoreElasticService;
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
            String query = searchStoreQuery.getQuery();
            String cityName = null;
            if (StringUtils.isNotBlank(searchStoreQuery.getCityName())) {
                cityName = searchStoreQuery.getCityName();
            }

            String lat = null;
            if (StringUtils.isNotBlank(searchStoreQuery.getLatitude())) {
                lat = searchStoreQuery.getLatitude();
            }

            String lng  = null;
            if (StringUtils.isNotBlank(searchStoreQuery.getLongitude())) {
                lng = searchStoreQuery.getLongitude();
            }

            String filters = null;
            if (StringUtils.isNotBlank(searchStoreQuery.getFilters())) {
                filters = searchStoreQuery.getFilters();
            }

            String scrollId = null;
            if (StringUtils.isNotBlank(searchStoreQuery.getScrollId())) {
                scrollId = searchStoreQuery.getScrollId();
            }

            String ipAddress = HttpRequestResponseParser.getClientIpAddress(request);
            LOG.info("Searching query={} cityName={} lat={} lng={} filters={} ipAddress={}", query, cityName, lat, lng, filters, ipAddress);

            BizStoreElasticList bizStoreElasticList = new BizStoreElasticList();
            GeoIP geoIp = getGeoIP(cityName, lat, lng, ipAddress, bizStoreElasticList);
            String geoHash = geoIp.getGeoHash();
            if (StringUtils.isBlank(geoHash)) {
                /* Note: Fail safe when lat and lng are 0.0 and 0.0 */
                geoHash = "te7ut71tgd9n";
            }

            if (useRestHighLevel) {
                return bizStoreElasticService.executeSearchOnBizStoreUsingRestClient(query, cityName, geoHash, filters, scrollId).asJson();
            } else {
                List<ElasticBizStoreSource> elasticBizStoreSources = bizStoreElasticService.createBizStoreSearchDSLQuery(query, geoHash);
                return bizStoreElasticList.populateBizStoreElasticSet(elasticBizStoreSources).asJson();
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
            String bodyJson,

            HttpServletRequest request
    ) {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("NearMe invoked did={} dt={}", did, dt);

        try {
            Map<String, ScrubbedInput> map;
            try {
                map = ParseJsonStringToMap.jsonStringToMap(bodyJson);
            } catch (IOException e) {
                LOG.error("Could not parse json={} reason={}", bodyJson, e.getLocalizedMessage(), e);
                return ErrorEncounteredJson.toJson("Could not parse JSON", MOBILE_JSON);
            }

            String cityName = null;
            if (map.containsKey("cityName") && StringUtils.isNotBlank(map.get("cityName").getText())) {
                cityName = map.get("cityName").getText();
            }

            String lat = null;
            if (map.containsKey("lat") && StringUtils.isNotBlank(map.get("lng").getText())) {
                lat = map.get("lat").getText();
            }

            String lng  = null;
            if (map.containsKey("lng") && StringUtils.isNotBlank(map.get("lng").getText())) {
                lng = map.get("lng").getText();
            }

            String filters = null;
            if (map.containsKey("filters") && StringUtils.isNotBlank(map.get("filters").getText())) {
                filters = map.get("filters").getText();
            }

            String scrollId = null;
            if (map.containsKey("scrollId") && StringUtils.isNotBlank(map.get("scrollId").getText())) {
                scrollId = map.get("scrollId").getText();
            }

            String ipAddress = HttpRequestResponseParser.getClientIpAddress(request);
            LOG.info("NearMe cityName={} lat={} lng={} filters={} ipAddress={}", cityName, lat, lng, filters, ipAddress);

            BizStoreElasticList bizStoreElasticList = new BizStoreElasticList();
            GeoIP geoIp = getGeoIP(cityName, lat, lng, ipAddress, bizStoreElasticList);
            String geoHash = geoIp.getGeoHash();
            if (StringUtils.isBlank(geoHash)) {
                /* Note: Fail safe when lat and lng are 0.0 and 0.0 */
                geoHash = "te7ut71tgd9n";
            }
            LOG.debug("GeoIP={} geoHash={}", geoIp, geoHash);

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
            return bizStoreElasticService.nearMeSearch(geoHash, scrollId).asJson();
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
            geoIp = new GeoIP(ipAddress, "", Double.valueOf(lat), Double.valueOf(lng));
            bizStoreElasticList.setCityName(cityName);
        } else if (StringUtils.isNotBlank(lng) && StringUtils.isNotBlank(lat)) {
            geoIp = new GeoIP(ipAddress, "", Double.valueOf(lat), Double.valueOf(lng));
            bizStoreElasticList.setCityName(geoIp.getCityName());
        } else {
            geoIp = geoIPLocationService.getLocation(ipAddress);
            bizStoreElasticList.setCityName(geoIp.getCityName());
            LOG.info("City={} based on ip={}", geoIp.getCityName(), ipAddress);
        }
        return geoIp;
    }
}
