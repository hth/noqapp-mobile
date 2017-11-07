package com.noqapp.mobile.view.controller.open;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.noqapp.health.domain.json.JsonSiteHealth;
import com.noqapp.health.domain.json.JsonSiteHealthService;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.services.SiteHealthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.concurrent.TimeUnit;

/**
 * User: hitender
 * Date: 11/19/16 1:47 PM
 */
@SuppressWarnings({
        "PMD.BeanMembersShouldSerialize",
        "PMD.LocalVariableCouldBeFinal",
        "PMD.MethodArgumentCouldBeFinal",
        "PMD.LongVariable"
})
@Controller
@RequestMapping(value = "/open")
public class IsWorkingController {
    private static final Logger LOG = LoggerFactory.getLogger(IsWorkingController.class);

    private final Cache<String, JsonSiteHealth> cache = CacheBuilder.newBuilder()
            .maximumSize(1)
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .build();

    private SiteHealthService siteHealthService;

    @Autowired
    public IsWorkingController(SiteHealthService siteHealthService) {
        this.siteHealthService = siteHealthService;
    }

    /**
     * Supports HTML call.
     * <p>
     * During application start up a call is made to show index page. Hence this method and only this controller
     * contains support for request type HEAD.
     * <p>
     * We have added support for HEAD request in filter to prevent failing on HEAD request. As of now there is no valid
     * reason why filter contains this HEAD request as everything is secure after login and there are no bots or
     * crawlers when a valid user has logged in.
     * <p>
     *
     * @return
     * @see <a href="http://axelfontaine.com/blog/http-head.html">http://axelfontaine.com/blog/http-head.html</a>
     */
    @RequestMapping(
            value = "/isWorking",
            method = {RequestMethod.GET, RequestMethod.HEAD},
            produces = {
                    MediaType.TEXT_HTML_VALUE + ";charset=UTF-8",
            }
    )
    public String isWorking() {
        LOG.info("isWorking");
        return "isWorking";
    }

    /**
     * Supports JSON call.
     *
     * @return
     */
    @RequestMapping(
            value = "/healthCheck",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String healthCheck() {
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
        return jsonSiteHealth.asJson();
    }
}
