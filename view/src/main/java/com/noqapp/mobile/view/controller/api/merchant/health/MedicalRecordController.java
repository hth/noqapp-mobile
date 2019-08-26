package com.noqapp.mobile.view.controller.api.merchant.health;

import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.common.utils.CommonUtil.UNAUTHORIZED;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.BUSINESS_NOT_AUTHORIZED;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MEDICAL_RECORD_ACCESS_DENIED;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MEDICAL_RECORD_DOES_NOT_EXISTS;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MEDICAL_RECORD_ENTRY_DENIED;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MEDICAL_RECORD_POPULATED_WITH_LAB;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MOBILE;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MOBILE_JSON;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.json.JsonResponse;
import com.noqapp.domain.json.medical.JsonHospitalVisitScheduleList;
import com.noqapp.domain.types.BusinessTypeEnum;
import com.noqapp.domain.types.catgeory.MedicalDepartmentEnum;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.medical.domain.HospitalVisitScheduleEntity;
import com.noqapp.medical.domain.json.JsonMedicalRecord;
import com.noqapp.medical.exception.ExistingLabResultException;
import com.noqapp.medical.service.HospitalVisitScheduleService;
import com.noqapp.medical.service.MedicalRecordService;
import com.noqapp.mobile.common.util.ErrorEncounteredJson;
import com.noqapp.mobile.domain.body.merchant.FindMedicalProfile;
import com.noqapp.mobile.domain.body.merchant.HospitalVisitFor;
import com.noqapp.mobile.domain.body.merchant.LabFile;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.service.MedicalRecordMobileService;
import com.noqapp.mobile.view.controller.api.ImageCommonHelper;
import com.noqapp.mobile.view.controller.api.client.health.MedicalRecordAPIController;
import com.noqapp.mobile.view.validator.ImageValidator;
import com.noqapp.service.BizService;
import com.noqapp.service.BusinessUserStoreService;

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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

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
    private HospitalVisitScheduleService hospitalVisitScheduleService;
    private ImageCommonHelper imageCommonHelper;
    private ImageValidator imageValidator;

    @Autowired
    public MedicalRecordController(
        AuthenticateMobileService authenticateMobileService,
        ApiHealthService apiHealthService,
        MedicalRecordService medicalRecordService,
        BusinessUserStoreService businessUserStoreService,
        BizService bizService,
        MedicalRecordMobileService medicalRecordMobileService,
        HospitalVisitScheduleService hospitalVisitScheduleService,
        ImageCommonHelper imageCommonHelper,
        ImageValidator imageValidator
    ) {
        this.authenticateMobileService = authenticateMobileService;
        this.apiHealthService = apiHealthService;
        this.medicalRecordService = medicalRecordService;
        this.businessUserStoreService = businessUserStoreService;
        this.bizService = bizService;
        this.medicalRecordMobileService = medicalRecordMobileService;
        this.hospitalVisitScheduleService = hospitalVisitScheduleService;
        this.imageCommonHelper = imageCommonHelper;
        this.imageValidator = imageValidator;
    }

    /**
     * When client is served by merchant.
     * And
     * When client starts to serve for first time or re-start after serving the last in the queue.
     */
    @PostMapping(
        value = "/update",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String update(
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
            if (StringUtils.isBlank(jsonMedicalRecord.getCodeQR())) {
                LOG.warn("Not a valid codeQR={} qid={}", jsonMedicalRecord.getCodeQR(), qid);
                return getErrorReason("Not a valid queue code.", MOBILE_JSON);
            } else if (!businessUserStoreService.hasAccess(qid, jsonMedicalRecord.getCodeQR())) {
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

            medicalRecordService.addMedicalRecord(jsonMedicalRecord, qid);
            return new JsonResponse(true).asJson();
        } catch (ExistingLabResultException e) {
            LOG.error("Failed updating medical record with lab result json={} qid={} message={}", jsonMedicalRecord, qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Medical record has lab result. Delete to update.", MEDICAL_RECORD_POPULATED_WITH_LAB);
        } catch (Exception e) {
            LOG.error("Failed processing medical record json={} qid={} message={}", jsonMedicalRecord, qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/update",
                "update",
                MedicalRecordController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Retrieve record before adding. */
    @PostMapping(
        value = "/retrieve",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String retrieve(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        JsonMedicalRecord mr,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Retrieve medical record mail={} did={} deviceType={} auth={}", mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/h/medicalRecord/retrieve by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            if (StringUtils.isBlank(mr.getCodeQR())) {
                LOG.warn("Not a valid codeQR={} qid={}", mr.getCodeQR(), qid);
                return getErrorReason("Not a valid queue code.", MOBILE_JSON);
            } else if (!businessUserStoreService.hasAccess(qid, mr.getCodeQR())) {
                LOG.info("Your are not authorized to add medical record mail={}", mail);
                return getErrorReason("Your are not authorized to access medical record", MEDICAL_RECORD_ACCESS_DENIED);
            }

            JsonMedicalRecord jsonMedicalRecord = medicalRecordService.retrieveMedicalRecord(mr.getCodeQR(), mr.getRecordReferenceId());
            if (null == jsonMedicalRecord) {
                return getErrorReason("Your are not authorized to access medical record", MEDICAL_RECORD_ACCESS_DENIED);
            }

            return jsonMedicalRecord.asJson();
        } catch (Exception e) {
            LOG.error("Failed accessing medical record json={} qid={} message={}", mr, qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/retrieve",
                "retrieve",
                MedicalRecordController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    @PostMapping(
        value = "/historical",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String historical(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput deviceType,

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
        LOG.debug("Client medical record fetch mail={} did={} deviceType={} auth={}", mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/h/medicalRecord/historical by mail={}", mail);
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
                "/historical",
                "historical",
                MedicalRecordAPIController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    @PostMapping(
        value = "/historical/{md}",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String historicalByMedicalDepartment(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader("X-R-AUTH")
        ScrubbedInput auth,

        @PathVariable ("md")
        MedicalDepartmentEnum medicalDepartment,

        @RequestBody
        FindMedicalProfile findMedicalProfile,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.debug("Client medical record fetch mail={} did={} deviceType={} auth={}", mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/h/medicalRecord/historical/{md} by mail={}", mail);
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

            return medicalRecordService.populateMedicalHistory(findMedicalProfile.getQueueUserId(), medicalDepartment).asJson();
        } catch (Exception e) {
            LOG.error("Failed getting medical record qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/historical/{md}",
                "historicalByMedicalDepartment",
                MedicalRecordAPIController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    @PostMapping(
        value = "/hospitalVisitSchedule",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String hospitalVisitSchedule(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput deviceType,

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
        LOG.debug("Client medical record fetch mail={} did={} deviceType={} auth={}", mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/h/medicalRecord/hospitalVisitSchedule by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            return new JsonHospitalVisitScheduleList()
                .setJsonHospitalVisitSchedules(hospitalVisitScheduleService.findAllAsJson(findMedicalProfile.getQueueUserId())).asJson();
        } catch (Exception e) {
            LOG.error("Failed getting hospital visit qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/hospitalVisitSchedule",
                "hospitalVisitSchedule",
                MedicalRecordAPIController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    @PostMapping(
        value = "/modifyVisitingFor",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String modifyVisitingFor(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        HospitalVisitFor hospitalVisitFor,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.debug("Client medical record fetch mail={} did={} deviceType={} auth={}", mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/h/medicalRecord/modifyVisitingFor by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            HospitalVisitScheduleEntity hospitalVisitSchedule = hospitalVisitScheduleService.modifyVisitingFor(
                hospitalVisitFor.getHospitalVisitScheduleId(),
                hospitalVisitFor.getQid(),
                hospitalVisitFor.getVisitingFor(),
                hospitalVisitFor.getBooleanReplacement(),
                qid);

            return hospitalVisitScheduleService.populateHospitalVisitScheduleAsJson(hospitalVisitSchedule).asJson();
        } catch (Exception e) {
            LOG.error("Failed modifyVisitingFor qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/modifyVisitingFor",
                "modifyVisitingFor",
                MedicalRecordAPIController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    @PostMapping(
        value = "/updateObservation",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String updateObservation(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        LabFile labFile,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Update observation for id={} lab={} from mail={} did={} deviceType={} auth={}",
            labFile.getRecordReferenceId(), labFile.getLabCategory(), mail, did, deviceType, AUTH_KEY_HIDDEN);

        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/h/medicalRecord/updateObservation by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            switch (labFile.getLabCategory()) {
                case SPEC:
                case SCAN:
                case XRAY:
                case SONO:
                case MRI:
                    medicalRecordService.updateRadiologyObservation(labFile.getRecordReferenceId(), labFile.getObservation());
                    break;
                case PATH:
                    medicalRecordService.updatePathologyObservation(labFile.getRecordReferenceId(), labFile.getObservation());
                    break;
                default:
                    LOG.error("Reached unsupported lab category {}", labFile.getLabCategory().getDescription());
                    throw new UnsupportedOperationException("Reached unsupported lab category " + labFile.getLabCategory().getDescription());
            }

            return new JsonResponse(true).asJson();
        } catch (Exception e) {
            LOG.error("Failed updating lab observation qid={}, id={} labCategory={} reason={}",
                qid, labFile.getRecordReferenceId(), labFile.getLabCategory(), e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/updateObservation",
                "updateObservation",
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

    /** Retrieve record before adding. */
    @GetMapping(
        value = "/exists/{codeQR}/{recordReferenceId}",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String exists(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @PathVariable("codeQR")
        ScrubbedInput codeQR,

        @PathVariable("recordReferenceId")
        ScrubbedInput recordReferenceId,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Exists medical record mail={} did={} deviceType={} auth={}", mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/h/medicalRecord/exists by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            if (StringUtils.isBlank(codeQR.getText())) {
                LOG.warn("Not a valid codeQR={} qid={}", codeQR.getText(), qid);
                return getErrorReason("Not a valid queue code.", MOBILE_JSON);
            } else if (!businessUserStoreService.hasAccess(qid, codeQR.getText())) {
                LOG.info("Your are not authorized to access medical record mail={}", mail);
                return getErrorReason("Your are not authorized to access medical record", MEDICAL_RECORD_ACCESS_DENIED);
            }

            JsonMedicalRecord jsonMedicalRecord = medicalRecordService.findMedicalRecord(codeQR.getText(), recordReferenceId.getText());
            if (null == jsonMedicalRecord) {
                return getErrorReason("No such medical record exists", MEDICAL_RECORD_DOES_NOT_EXISTS);
            }

            return jsonMedicalRecord.asJson();
        } catch (Exception e) {
            LOG.error("Failed accessing medical record codeQR={} recordReferenceId={} qid={} message={}",
                codeQR.getText(), recordReferenceId.getText(), qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/exists",
                "exists",
                MedicalRecordController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    @PostMapping (
        value = "/appendImage",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String appendImage(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput dt,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @RequestPart("file")
        MultipartFile multipartFile,

        @RequestPart("recordReferenceId")
        String recordReferenceId,

        HttpServletResponse response
    ) throws IOException {
        Map<String, String> errors = imageValidator.validate(multipartFile, ImageValidator.SUPPORTED_FILE.IMAGE);
        if (!errors.isEmpty()) {
            return ErrorEncounteredJson.toJson(errors);
        }

        return imageCommonHelper.uploadMedicalRecordImage(
            did.getText(),
            dt.getText(),
            mail.getText(),
            auth.getText(),
            new ScrubbedInput(recordReferenceId).getText(),
            multipartFile,
            response);
    }

    @PostMapping (
        value = "/removeImage",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String removeImage(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput dt,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        JsonMedicalRecord mr,

        HttpServletResponse response
    ) throws IOException {
        String filename;
        if (null == mr.getImages() || mr.getImages().isEmpty()) {
            return ErrorEncounteredJson.toJson("No image selected for deletion", MOBILE);
        } else {
            filename = mr.getImages().get(0);
        }

        return imageCommonHelper.removeMedicalImage(
            did.getText(),
            dt.getText(),
            mail.getText(),
            auth.getText(),
            new ScrubbedInput(mr.getRecordReferenceId()).getText(),
            filename,
            response);
    }
}
