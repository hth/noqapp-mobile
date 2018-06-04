package com.noqapp.mobile.view.controller.open;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.json.JsonResponse;
import com.noqapp.domain.json.medical.JsonHealthCareProfile;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.medical.service.HealthCareProfileService;
import com.noqapp.mobile.service.StoreDetailService;
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

/**
 * hitender
 * 5/31/18 1:27 PM
 */
@SuppressWarnings ({
        "PMD.BeanMembersShouldSerialize",
        "PMD.LocalVariableCouldBeFinal",
        "PMD.MethodArgumentCouldBeFinal",
        "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/open/healthCare")
public class HealthCareProfileController {
    private static final Logger LOG = LoggerFactory.getLogger(HealthCareProfileController.class);

    private HealthCareProfileService healthCareProfileService;
    private StoreDetailService storeDetailService;
    private ApiHealthService apiHealthService;

    @Autowired
    public HealthCareProfileController(
            HealthCareProfileService healthCareProfileService,
            StoreDetailService storeDetailService,
            ApiHealthService apiHealthService
    ) {
        this.healthCareProfileService = healthCareProfileService;
        this.storeDetailService = storeDetailService;
        this.apiHealthService = apiHealthService;
    }

    @GetMapping(
            value = "/profile/{codeQR}",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    public String profile(
            @RequestHeader("X-R-DID")
            ScrubbedInput did,

            @RequestHeader ("X-R-DT")
            ScrubbedInput dt,

            @PathVariable("codeQR")
            ScrubbedInput codeQR
    ) {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Get profile {} did={} dt={}", codeQR.getText(), did, dt);

        try {
            JsonHealthCareProfile jsonHealthCareProfile = healthCareProfileService.findByCodeQRAsJson(codeQR.getText());
            for (String storeCodeQR : jsonHealthCareProfile.getManagerAtStoreCodeQRs()) {
                jsonHealthCareProfile.addStore(storeDetailService.storeDetail(storeCodeQR));
            }

            return jsonHealthCareProfile.asJson();
        } catch (Exception e) {
            LOG.error("Failed getting profile {} reason={}", codeQR.getText(), e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return new JsonResponse(false).asJson();
        } finally {
            apiHealthService.insert(
                    "/profile/{codeQR}",
                    "profile",
                    HealthCareProfileController.class.getName(),
                    Duration.between(start, Instant.now()),
                    methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }
}
