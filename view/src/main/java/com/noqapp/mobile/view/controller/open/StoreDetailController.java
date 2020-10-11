package com.noqapp.mobile.view.controller.open;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.json.JsonHour;
import com.noqapp.domain.json.JsonHourList;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.service.StoreDetailService;
import com.noqapp.search.elastic.domain.BizStoreElasticList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

/**
 * hitender
 * 3/23/18 1:46 AM
 */
@SuppressWarnings ({
        "PMD.BeanMembersShouldSerialize",
        "PMD.LocalVariableCouldBeFinal",
        "PMD.MethodArgumentCouldBeFinal",
        "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/open/store")
public class StoreDetailController {
    private static final Logger LOG = LoggerFactory.getLogger(StoreDetailController.class);

    private StoreDetailService storeDetailService;
    private ApiHealthService apiHealthService;

    @Autowired
    public StoreDetailController(
        StoreDetailService storeDetailService,
        ApiHealthService apiHealthService
    ) {
        this.storeDetailService = storeDetailService;
        this.apiHealthService = apiHealthService;
    }

    @GetMapping(
        value = "/{codeQR}",
        produces = MediaType.APPLICATION_JSON_VALUE)
    public String storeDetail(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput dt,

        @PathVariable("codeQR")
        ScrubbedInput codeQR,

        HttpServletResponse response
    ) {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Store Detail for codeQR={} did={} dt={}", codeQR, did, dt);

        try {
            return storeDetailService.populateStoreDetail(codeQR.getText());
        } catch (Exception e) {
            LOG.error("Failed getting store detail reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return new BizStoreElasticList().asJson();
        } finally {
            apiHealthService.insert(
                "/{codeQR}",
                "storeDetail",
                StoreDetailController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    @GetMapping(
        value = "/hours/{codeQR}",
        produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonHourList storeHours(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput dt,

        @PathVariable("codeQR")
        ScrubbedInput codeQR,

        HttpServletResponse response
    ) {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Store hours for codeQR={} did={} dt={}", codeQR, did, dt);

        try {
            return storeDetailService.findAllStoreHoursAsJson(codeQR.getText());
        } catch (Exception e) {
            LOG.error("Failed getting store storeHours codeQR={} reason={}", codeQR, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return new JsonHourList();
        } finally {
            apiHealthService.insert(
                "/hours/{codeQR}",
                "storeHours",
                StoreDetailController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }
}
