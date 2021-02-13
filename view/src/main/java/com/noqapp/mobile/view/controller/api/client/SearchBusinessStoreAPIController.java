package com.noqapp.mobile.view.controller.api.client;

import static com.noqapp.mobile.view.controller.api.client.TokenQueueAPIController.authorizeRequest;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.UserSearchEntity;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.domain.body.client.SearchStoreQuery;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.view.util.HttpRequestResponseParser;
import com.noqapp.search.elastic.domain.BizStoreElasticList;
import com.noqapp.search.elastic.domain.BizStoreSearchElasticList;
import com.noqapp.search.elastic.helper.GeoIP;
import com.noqapp.search.elastic.json.ElasticBizStoreSearchSource;
import com.noqapp.search.elastic.service.BizStoreSearchElasticService;
import com.noqapp.search.elastic.service.GeoIPLocationService;
import com.noqapp.service.UserSearchService;

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
public class SearchBusinessStoreAPIController {
    private static final Logger LOG = LoggerFactory.getLogger(SearchBusinessStoreAPIController.class);

    private boolean useRestHighLevel;

    private AuthenticateMobileService authenticateMobileService;
    private BizStoreSearchElasticService bizStoreSearchElasticService;
    private GeoIPLocationService geoIPLocationService;
    private UserSearchService userSearchService;
    private ApiHealthService apiHealthService;

    @Autowired
    public SearchBusinessStoreAPIController(
        @Value("${search.useRestHighLevel:false}")
        boolean useRestHighLevel,

        AuthenticateMobileService authenticateMobileService,
        BizStoreSearchElasticService bizStoreSearchElasticService,
        GeoIPLocationService geoIPLocationService,
        UserSearchService userSearchService,
        ApiHealthService apiHealthService
    ) {
        this.useRestHighLevel = useRestHighLevel;

        this.authenticateMobileService = authenticateMobileService;
        this.bizStoreSearchElasticService = bizStoreSearchElasticService;
        this.geoIPLocationService = geoIPLocationService;
        this.userSearchService = userSearchService;
        this.apiHealthService = apiHealthService;
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
        SearchStoreQuery searchStoreQuery,

        HttpServletRequest request,
        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        LOG.info("Searching for query=\"{}\" did={} dt={} mail={} auth={}", searchStoreQuery.getQuery(), did, dt, mail, auth);

        try {
            String query = searchStoreQuery.getQuery().getText();
            String ipAddress = HttpRequestResponseParser.getClientIpAddress(request);
            LOG.debug("Searching query=\"{}\" city=\"{}\" lat={} lng={} filters={} ip={}",
                query,
                searchStoreQuery.getCityName(),
                searchStoreQuery.getLatitude(),
                searchStoreQuery.getLongitude(),
                searchStoreQuery.getFilters(),
                ipAddress);

            BizStoreSearchElasticList bizStoreSearchElasticList = new BizStoreSearchElasticList();
            GeoIP geoIp = getGeoIP(
                searchStoreQuery.getCityName().getText(),
                searchStoreQuery.getLatitude().getText(),
                searchStoreQuery.getLongitude().getText(),
                ipAddress,
                bizStoreSearchElasticList);
            String geoHash = geoIp.getGeoHash();

            LOG.info("Search query=\"{}\" city=\"{}\" geoHash={} ip={} did={}", query, searchStoreQuery.getCityName(), geoHash, ipAddress, did.getText());
            if (StringUtils.isBlank(geoHash)) {
                /* Note: Fail safe when lat and lng are 0.0 and 0.0 */
                geoHash = "te7ut71tgd9n";
            }

            if (useRestHighLevel) {
                return bizStoreSearchElasticService.executeNearMeSearchOnBizStoreUsingRestClient(
                    query,
                    searchStoreQuery.getCityName().getText(),
                    geoHash,
                    searchStoreQuery.getFilters().getText(),
                    searchStoreQuery.getScrollId().getText()).asJson();
            } else {
                List<ElasticBizStoreSearchSource> elasticBizStoreSearchSources = bizStoreSearchElasticService.createBizStoreSearchDSLQuery(query, geoHash);
                UserSearchEntity userSearch = new UserSearchEntity()
                    .setQuery(searchStoreQuery.getQuery().getText())
                    .setQid(qid)
                    .setDid(did.getText())
                    .setCityName(searchStoreQuery.getCityName().getText())
                    .setGeoHash(geoHash)
                    .setResultCount(elasticBizStoreSearchSources.size());
                userSearchService.save(userSearch);
                return bizStoreSearchElasticList.populateSearchBizStoreElasticArray(elasticBizStoreSearchSources).asJson();
            }
        } catch (Exception e) {
            LOG.error("Failed processing search reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return new BizStoreElasticList().asJson();
        } finally {
            apiHealthService.insert(
                "/search",
                "search",
                SearchBusinessStoreAPIController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    private GeoIP getGeoIP(String cityName, String lat, String lng, String ipAddress, BizStoreSearchElasticList bizStoreSearchElasticList) {
        GeoIP geoIp;
        if (StringUtils.isNotBlank(cityName)) {
            //TODO search based on city when lat lng is disabled
            geoIp = new GeoIP(ipAddress, "", Double.parseDouble(lat), Double.parseDouble(lng));
            bizStoreSearchElasticList.setCityName(cityName);
        } else if (StringUtils.isNotBlank(lng) && StringUtils.isNotBlank(lat)) {
            geoIp = new GeoIP(ipAddress, "", Double.parseDouble(lat), Double.parseDouble(lng));
            bizStoreSearchElasticList.setCityName(geoIp.getCityName());
        } else {
            geoIp = geoIPLocationService.getLocation(ipAddress);
            bizStoreSearchElasticList.setCityName(geoIp.getCityName());
            LOG.info("city={} based on ip={}", geoIp.getCityName(), ipAddress);
        }
        return geoIp;
    }
}
