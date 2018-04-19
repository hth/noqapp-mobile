package com.noqapp.mobile.view.controller.api.client;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.medical.domain.MedicalRecordEntity;
import com.noqapp.medical.domain.json.JsonMedicalRecord;
import com.noqapp.medical.domain.json.JsonMedicalRecordList;
import com.noqapp.medical.service.MedicalRecordService;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.service.AccountService;
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
import java.util.List;
import java.util.Set;

import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.common.utils.CommonUtil.UNAUTHORIZED;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

/**
 * hitender
 * 3/26/18 3:48 PM
 */
@SuppressWarnings ({
        "PMD.BeanMembersShouldSerialize",
        "PMD.LocalVariableCouldBeFinal",
        "PMD.MethodArgumentCouldBeFinal",
        "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/api/c/medicalRecord")
public class MedicalRecordAPIController {
    private static final Logger LOG = LoggerFactory.getLogger(MedicalRecordAPIController.class);

    private AuthenticateMobileService authenticateMobileService;
    private MedicalRecordService medicalRecordService;
    private ApiHealthService apiHealthService;
    private AccountService accountService;

    @Autowired
    public MedicalRecordAPIController(
            AuthenticateMobileService authenticateMobileService,
            MedicalRecordService medicalRecordService,
            ApiHealthService apiHealthService,
            AccountService accountService
    ) {
        this.authenticateMobileService = authenticateMobileService;
        this.medicalRecordService = medicalRecordService;
        this.apiHealthService = apiHealthService;
        this.accountService = accountService;
    }

    @GetMapping(
            value = "/fetch",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String fetch(
            @RequestHeader("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            HttpServletResponse response
    ) throws IOException {
        Instant start = Instant.now();
        LOG.debug("mail={}, auth={}", mail, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        JsonMedicalRecordList jsonMedicalRecordList = new JsonMedicalRecordList();
        try {
            List<MedicalRecordEntity> medicalRecords = medicalRecordService.historicalRecords(qid);
            for (MedicalRecordEntity medicalRecord : medicalRecords) {
                JsonMedicalRecord jsonMedicalRecord = new JsonMedicalRecord();
                jsonMedicalRecord
                        .setBusinessType(medicalRecord.getBusinessType())
                        .setQueueUserId(medicalRecord.getQueueUserId())
                        .setChiefComplain(medicalRecord.getChiefComplain())
                        .setPastHistory(medicalRecord.getPastHistory())
                        .setFamilyHistory(medicalRecord.getFamilyHistory())
                        .setKnownAllergies(medicalRecord.getKnownAllergies())
                        .setClinicalFinding(medicalRecord.getClinicalFinding())
                        .setProvisionalDifferentialDiagnosis(medicalRecord.getProvisionalDifferentialDiagnosis())
                        .setDiagnosedBy(accountService.findProfileByQueueUserId(medicalRecord.getDiagnosedById()).getName());

                Set<String> medicalPhysicalExaminationIds = medicalRecord.getMedicalPhysical().getMedicalPhysicalExaminationIds();
                for (String id : medicalPhysicalExaminationIds) {
                    //TODO something
                }

                jsonMedicalRecordList.addJsonMedicalRecords(jsonMedicalRecord);
            }

            return jsonMedicalRecordList.asJson();
        } catch(Exception e) {
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
