package com.noqapp.mobile.view.controller.api.merchant.health;

import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.common.utils.CommonUtil.UNAUTHORIZED;
import static com.noqapp.domain.types.UserLevelEnum.*;
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
import com.noqapp.mobile.domain.body.merchant.FindMedicalProfile;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.service.MedicalRecordMobileService;
import com.noqapp.mobile.view.controller.api.client.health.MedicalRecordAPIController;
import com.noqapp.mobile.view.controller.api.merchant.ManageQueueController;
import com.noqapp.service.BizService;
import com.noqapp.service.BusinessUserStoreService;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private MedicalRecordMobileService medicalRecordMobileService;

    @Autowired
    public MedicalRecordController(
            AuthenticateMobileService authenticateMobileService,
            ApiHealthService apiHealthService,
            MedicalRecordService medicalRecordService,
            BusinessUserStoreService businessUserStoreService,
            BizService bizService,
            MedicalRecordMobileService medicalRecordMobileService
    ) {
        this.authenticateMobileService = authenticateMobileService;
        this.apiHealthService = apiHealthService;
        this.medicalRecordService = medicalRecordService;
        this.businessUserStoreService = businessUserStoreService;
        this.bizService = bizService;
        this.medicalRecordMobileService = medicalRecordMobileService;
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
            JsonMedicalRecord jsonMedicalRecord,

            HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Add medical record mail={} did={} deviceType={} auth={}", mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/h/medicalRecord/add by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            jsonMedicalRecord.setDiagnosedById(qid);
            if (StringUtils.isBlank(jsonMedicalRecord.getCodeQR())) {
                LOG.warn("Not a valid codeQR={} qid={}", jsonMedicalRecord.getCodeQR(), qid);
                return getErrorReason("Not a valid queue code.", MOBILE_JSON);
            } else if (!businessUserStoreService.hasAccess(jsonMedicalRecord.getDiagnosedById(), jsonMedicalRecord.getCodeQR())) {
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
        } catch (Exception e) {
            LOG.error("Failed processing medical record json={} qid={} message={}", jsonMedicalRecord, qid, e.getLocalizedMessage(), e);
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

    @PostMapping(
        value = "/fetch",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String fetch(
        @RequestHeader("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        FindMedicalProfile findMedicalProfile,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.debug("Client medical record fetch mail={}, auth={}", mail, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            if (StringUtils.isBlank(findMedicalProfile.getCodeQR())) {
                LOG.warn("Not a valid codeQR={} qid={}", findMedicalProfile.getCodeQR(), qid);
                return getErrorReason("Not a valid queue code.", MOBILE_JSON);
            } else if (!businessUserStoreService.hasAccess(qid, findMedicalProfile.getCodeQR())) {
                LOG.info("Your are not authorized to see medical history of client mail={}", mail);
                return getErrorReason("Your are not authorized to see medical profile of client", MEDICAL_RECORD_ENTRY_DENIED);
            }

            return medicalRecordService.populateMedicalHistory(findMedicalProfile.getQueueUserId()).asJson();
        } catch (Exception e) {
            LOG.error("Failed getting medical record qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/fetch",
                "fetch",
                MedicalRecordAPIController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** //TODO In future move this to its own class .*/
    @GetMapping(
        value = "/{codeQR}/followup",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String followUp(
        @RequestHeader("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader("X-R-AUTH")
        ScrubbedInput auth,

        @PathVariable("codeQR")
        ScrubbedInput codeQR,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Follow up shown for codeQR={} request from mail={} auth={}", codeQR, mail, AUTH_KEY_HIDDEN);

        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/h/medicalRecord/{codeQR}/followup by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        if (StringUtils.isBlank(codeQR.getText())) {
            LOG.warn("Not a valid codeQR={} qid={}", codeQR.getText(), qid);
            return getErrorReason("Not a valid queue code.", MOBILE_JSON);
        } else if (!businessUserStoreService.hasAccess(qid, codeQR.getText())) {
            LOG.info("Un-authorized store access to /api/m/h/medicalRecord/{codeQR}/followup by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            return medicalRecordMobileService.findAllFollowUp(codeQR.getText());
        } catch (Exception e) {
            LOG.error("Failed getting follow up clients reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/{codeQR}/followup",
                "followUp",
                MedicalRecordController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }
}
