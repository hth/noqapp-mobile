package com.noqapp.mobile.view.controller.open;

import com.noqapp.health.domain.json.JsonSiteHealth;
import com.noqapp.health.domain.json.JsonSiteHealthService;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.service.SiteHealthService;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * hitender
 * 4/14/20 10:54 AM
 */
@SuppressWarnings({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@Controller
@RequestMapping(value = "/open/health/portal")
public class PortalHealthController {
    private static final Logger LOG = LoggerFactory.getLogger(PortalHealthController.class);

    /* Set cache parameters. */
    private final Cache<String, JsonSiteHealth> cache = Caffeine.newBuilder()
        .maximumSize(1)
        .expireAfterWrite(15, TimeUnit.MINUTES)
        .build();

    private SiteHealthService siteHealthService;
    private ApiHealthService apiHealthService;

    @Autowired
    public PortalHealthController(SiteHealthService siteHealthService, ApiHealthService apiHealthService) {
        this.siteHealthService = siteHealthService;
        this.apiHealthService = apiHealthService;
    }

    /**
     * Supports JSON call.
     *
     * @return
     */
    @CrossOrigin(origins = "http://localhost:4200")
    @GetMapping(
        value = "/status",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String status() {
        Instant start = Instant.now();
        LOG.info("Health check invoked");
        JsonSiteHealth jsonSiteHealth = cache.getIfPresent("siteHealth");
        if (null == jsonSiteHealth) {
            jsonSiteHealth = new JsonSiteHealth();
            JsonSiteHealthService jsonHealthService = new JsonSiteHealthService("sw");
            siteHealthService.doSiteHealthCheck(jsonSiteHealth);
            jsonHealthService.ended().setHealthStatus(HealthStatusEnum.G);
            jsonSiteHealth.increaseServiceUpCount();
            jsonSiteHealth.addJsonHealthServiceChecks(jsonHealthService);

            cache.put("siteHealth", jsonSiteHealth);
        }

        apiHealthService.insert(
            "/status",
            "status",
            PortalHealthController.class.getName(),
            Duration.between(start, Instant.now()),
            HealthStatusEnum.G);

        return jsonSiteHealth.asJson();
    }
}

