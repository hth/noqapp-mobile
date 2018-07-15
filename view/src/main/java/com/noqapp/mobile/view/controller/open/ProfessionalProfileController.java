package com.noqapp.mobile.view.controller.open;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.json.JsonProfessionalProfile;
import com.noqapp.domain.json.JsonResponse;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.service.StoreDetailService;
import com.noqapp.service.ProfessionalProfileService;

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
@RequestMapping(value = "/open/professional")
public class ProfessionalProfileController {
    private static final Logger LOG = LoggerFactory.getLogger(ProfessionalProfileController.class);

    private ProfessionalProfileService professionalProfileService;
    private StoreDetailService storeDetailService;
    private ApiHealthService apiHealthService;

    @Autowired
    public ProfessionalProfileController(
            ProfessionalProfileService professionalProfileService,
            StoreDetailService storeDetailService,
            ApiHealthService apiHealthService
    ) {
        this.professionalProfileService = professionalProfileService;
        this.storeDetailService = storeDetailService;
        this.apiHealthService = apiHealthService;
    }

    @GetMapping(
            value = "/profile/{webProfileId}",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    public String profile(
            @RequestHeader("X-R-DID")
            ScrubbedInput did,

            @RequestHeader ("X-R-DT")
            ScrubbedInput dt,

            @PathVariable("webProfileId")
            ScrubbedInput webProfileId
    ) {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Get profile {} did={} dt={}", webProfileId.getText(), did, dt);

        try {
            JsonProfessionalProfile jsonProfessionalProfile = professionalProfileService.findByWebProfileIdAsJson(webProfileId.getText());
            for (String storeCodeQR : jsonProfessionalProfile.getManagerAtStoreCodeQRs()) {
                jsonProfessionalProfile.addStore(storeDetailService.storeDetail(storeCodeQR));
            }

            return jsonProfessionalProfile.asJson();
        } catch (Exception e) {
            LOG.error("Failed getting profile {} reason={}", webProfileId.getText(), e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return new JsonResponse(false).asJson();
        } finally {
            apiHealthService.insert(
                    "/profile/{webProfileId}",
                    "profile",
                    ProfessionalProfileController.class.getName(),
                    Duration.between(start, Instant.now()),
                    methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }
}
