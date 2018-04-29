package com.noqapp.mobile.view.controller.open;

import com.noqapp.common.utils.ParseJsonStringToMap;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.common.util.ErrorEncounteredJson;
import com.noqapp.mobile.view.util.HttpRequestResponseParser;
import com.noqapp.search.elastic.domain.BizStoreElastic;
import com.noqapp.search.elastic.domain.BizStoreElasticList;
import com.noqapp.search.elastic.helper.GeoIP;
import com.noqapp.search.elastic.json.ElasticBizStoreSource;
import com.noqapp.search.elastic.service.BizStoreElasticService;
import com.noqapp.search.elastic.service.GeoIPLocationService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MOBILE_JSON;

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

    private BizStoreElasticService bizStoreElasticService;
    private GeoIPLocationService geoIPLocationService;
    private ApiHealthService apiHealthService;

    @Autowired
    public SearchBusinessStoreController(
            BizStoreElasticService bizStoreElasticService,
            GeoIPLocationService geoIPLocationService,
            ApiHealthService apiHealthService
    ) {
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
            String bodyJson,

            HttpServletRequest request
    ) {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Searching for did={} dt={}", did, dt);

        try {
            Map<String, ScrubbedInput> map;
            try {
                map = ParseJsonStringToMap.jsonStringToMap(bodyJson);
            } catch (IOException e) {
                LOG.error("Could not parse json={} reason={}", bodyJson, e.getLocalizedMessage(), e);
                return ErrorEncounteredJson.toJson("Could not parse JSON", MOBILE_JSON);
            }

            String query = map.get("q").getText();
            String cityName = null;
            if (map.containsKey("cityName")) {
                cityName = map.get("cityName").getText();
            }
            String lat = null;
            if (map.containsKey("lat")) {
                lat = map.get("lat").getText();
            }

            String lng  = null;
            if (map.containsKey("lng")) {
                lng = map.get("lng").getText();
            }

            String filters = null;
            if (map.containsKey("filters")) {
                filters = map.get("filters").getText();
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

            List<ElasticBizStoreSource> elasticBizStoreSources = bizStoreElasticService.createBizStoreSearchDSLQuery(
                    query,
                    geoHash);

            return bizStoreElasticList.populateBizStoreElasticList(elasticBizStoreSources).asJson();
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
            value = "/nearMe",
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
            if (map.containsKey("cityName") && map.get("cityName") != null) {
                cityName = map.get("cityName").getText();
            }

            String lat = null;
            if (map.containsKey("lat") && map.get("lng") != null) {
                lat = map.get("lat").getText();
            }

            String lng  = null;
            if (map.containsKey("lng") && map.get("lng") != null) {
                lng = map.get("lng").getText();
            }

            String filters = null;
            if (map.containsKey("filters") && map.get("filters") != null) {
                filters = map.get("filters").getText();
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

            List<ElasticBizStoreSource> elasticBizStoreSources = bizStoreElasticService.createBizStoreSearchDSLQuery(
                    null,
                    geoHash);

            BizStoreElasticList bizStoreElastics = bizStoreElasticList.populateBizStoreElasticList(elasticBizStoreSources);
            int hits = 0;
            while (bizStoreElastics.getBizStoreElastics().size() < 10 && hits < 3) {
                LOG.info("NearMe found size={}", bizStoreElastics.getBizStoreElastics().size());
                elasticBizStoreSources = bizStoreElasticService.createBizStoreSearchDSLQuery(
                        null,
                        geoHash);

                Collection<BizStoreElastic> additional = bizStoreElasticList.populateBizStoreElasticList(elasticBizStoreSources).getBizStoreElastics();
                bizStoreElastics.getBizStoreElastics().addAll(additional);
                hits ++;
            }

            return bizStoreElastics.asJson();
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
        }
        return geoIp;
    }
}
