package com.noqapp.mobile.view.controller.api.client.health;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.medical.service.MedicalRecordService;
import com.noqapp.mobile.service.AuthenticateMobileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.common.utils.CommonUtil.UNAUTHORIZED;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

/**
 * hitender
 * 3/26/18 3:48 PM
 */
@SuppressWarnings({
        "PMD.BeanMembersShouldSerialize",
        "PMD.LocalVariableCouldBeFinal",
        "PMD.MethodArgumentCouldBeFinal",
        "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/api/c/h/medicalRecord")
public class MedicalRecordAPIController {
    private static final Logger LOG = LoggerFactory.getLogger(MedicalRecordAPIController.class);

    private AuthenticateMobileService authenticateMobileService;
    private MedicalRecordService medicalRecordService;
    private ApiHealthService apiHealthService;

    @Autowired
    public MedicalRecordAPIController(
            AuthenticateMobileService authenticateMobileService,
            MedicalRecordService medicalRecordService,
            ApiHealthService apiHealthService
    ) {
        this.authenticateMobileService = authenticateMobileService;
        this.medicalRecordService = medicalRecordService;
        this.apiHealthService = apiHealthService;
    }

    @GetMapping(
            value = "/fetch",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String fetch(
            @RequestHeader("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader("X-R-AUTH")
            ScrubbedInput auth,

            HttpServletResponse response
    ) throws IOException {
        Instant start = Instant.now();
        LOG.debug("Medical Record Fetch mail={}, auth={}", mail, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            return medicalRecordService.populateMedicalHistory(qid).asJson();
        } catch (Exception e) {
            LOG.error("Failed getting medical record qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            apiHealthService.insert(
                    "/fetch",
                    "fetch",
                    MedicalRecordAPIController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.F);
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                    "/fetch",
                    "fetch",
                    MedicalRecordAPIController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.G);
        }
    }
}
