package com.noqapp.mobile.view.controller.api.merchant.health;

import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.common.utils.CommonUtil.UNAUTHORIZED;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.BUSINESS_NOT_AUTHORIZED;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MEDICAL_RECORD_ENTRY_DENIED;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MOBILE_JSON;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.json.JsonResponse;
import com.noqapp.domain.types.BusinessTypeEnum;
import com.noqapp.domain.types.UserLevelEnum;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.medical.domain.json.JsonMedicalRecord;
import com.noqapp.medical.service.MedicalRecordService;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.service.BizService;
import com.noqapp.service.BusinessUserStoreService;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import javax.servlet.http.HttpServletResponse;

/**
 * hitender
 * 6/13/18 12:06 AM
 */
@SuppressWarnings ({
        "PMD.BeanMembersShouldSerialize",
        "PMD.LocalVariableCouldBeFinal",
        "PMD.MethodArgumentCouldBeFinal",
        "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/api/m/h/medicalRecord")
public class MedicalRecordController {
    private static final Logger LOG = LoggerFactory.getLogger(MedicalRecordController.class);

    private AuthenticateMobileService authenticateMobileService;
    private ApiHealthService apiHealthService;
    private MedicalRecordService medicalRecordService;
    private BusinessUserStoreService businessUserStoreService;
    private BizService bizService;

    @Autowired
    public MedicalRecordController(
            AuthenticateMobileService authenticateMobileService,
            ApiHealthService apiHealthService,
            MedicalRecordService medicalRecordService,
            BusinessUserStoreService businessUserStoreService,
            BizService bizService
    ) {
        this.authenticateMobileService = authenticateMobileService;
        this.apiHealthService = apiHealthService;
        this.medicalRecordService = medicalRecordService;
        this.businessUserStoreService = businessUserStoreService;
        this.bizService = bizService;
    }

    /**
     * When client is served by merchant.
     * And
     * When client starts to serve for first time or re-start after serving the last in the queue.
     */
    @PostMapping(
            value = "/add",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String add(
            @RequestHeader("X-R-DID")
            ScrubbedInput did,

            @RequestHeader ("X-R-DT")
            ScrubbedInput deviceType,

            @RequestHeader ("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            @RequestBody
            String requestBodyJson,

            HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Served mail={} did={} deviceType={} auth={}", mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/h/medicalRecord/add by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            JsonMedicalRecord jsonMedicalRecord = new ObjectMapper().readValue(requestBodyJson, JsonMedicalRecord.class);
            jsonMedicalRecord.setDiagnosedById(qid);

            if (StringUtils.isBlank(jsonMedicalRecord.getCodeQR())) {
                LOG.warn("Not a valid codeQR={} qid={}", jsonMedicalRecord.getCodeQR(), qid);
                return getErrorReason("Not a valid queue code.", MOBILE_JSON);
            } else if (!businessUserStoreService.hasAccessWithUserLevel(
                    jsonMedicalRecord.getDiagnosedById(),
                    jsonMedicalRecord.getCodeQR(),
                    UserLevelEnum.S_MANAGER)) {
                LOG.info("Your are not authorized to add medical record mail={}", mail);
                return getErrorReason("Your are not authorized to add medical record", MEDICAL_RECORD_ENTRY_DENIED);
            }

            /* Check if business type is of Hospital or Doctor to allow adding record. */
            BizStoreEntity bizStore = bizService.findByCodeQR(jsonMedicalRecord.getCodeQR());
            if (bizStore.getBusinessType() != BusinessTypeEnum.DO && bizStore.getBizName().getBusinessType() != BusinessTypeEnum.DO) {
                LOG.error("Failed as its not a Doctor or Hospital business type, found store={} biz={}",
                        bizStore.getBusinessType(),
                        bizStore.getBizName().getBusinessType());
                return getErrorReason("Business not authorized to add medical record", BUSINESS_NOT_AUTHORIZED);
            }

            medicalRecordService.addMedicalRecord(jsonMedicalRecord);
            return new JsonResponse(true).asJson();
        } catch (JsonMappingException e) {
            LOG.error("Failed parsing json={} qid={} message={}", requestBodyJson, qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                    "/add",
                    "add",
                    MedicalRecordController.class.getName(),
                    Duration.between(start, Instant.now()),
                    methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }
}
