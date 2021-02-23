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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * hitender
 * 2/19/21 5:43 PM
 */
@RestController
@RequestMapping(value = "/open")
public class HealthCheckController {
    private static final Logger LOG = LoggerFactory.getLogger(HealthCheckController.class);

    /* Set cache parameters. */
    private final Cache<String, JsonSiteHealth> cache = Caffeine.newBuilder()
        .maximumSize(1)
        .expireAfterWrite(15, TimeUnit.MINUTES)
        .build();

    private SiteHealthService siteHealthService;
    private ApiHealthService apiHealthService;

    @Autowired
    public HealthCheckController(SiteHealthService siteHealthService, ApiHealthService apiHealthService) {
        this.siteHealthService = siteHealthService;
        this.apiHealthService = apiHealthService;
    }

    /** Supports JSON call. Check the health and connectivity of server. */
    @GetMapping(value = "/healthCheck")
    public String healthCheck() {
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
            "/healthCheck",
            "healthCheck",
            HealthCheckController.class.getName(),
            Duration.between(start, Instant.now()),
            HealthStatusEnum.G);

        return jsonSiteHealth.asJson();
    }
}
