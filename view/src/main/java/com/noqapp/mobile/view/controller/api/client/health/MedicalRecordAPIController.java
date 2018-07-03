package com.noqapp.mobile.view.controller.api.client.health;

import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.common.utils.CommonUtil.UNAUTHORIZED;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

import com.noqapp.medical.domain.json.JsonMedicalPhysical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.noqapp.common.utils.DateUtil;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.types.catgeory.MedicalDepartmentEnum;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.medical.domain.MedicalMedicineEntity;
import com.noqapp.medical.domain.MedicalRecordEntity;
import com.noqapp.medical.domain.json.JsonMedicalMedicine;
import com.noqapp.medical.domain.json.JsonMedicalRecord;
import com.noqapp.medical.domain.json.JsonMedicalRecordList;
import com.noqapp.medical.domain.json.JsonRecordAccess;
import com.noqapp.medical.service.MedicalRecordService;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.service.AccountService;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

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
                        .setDiagnosedById(accountService.findProfileByQueueUserId(medicalRecord.getDiagnosedById()).getName())
                        .setCreateDate(DateUtil.dateToString(medicalRecord.getCreated()))
                        .setBusinessName(medicalRecord.getBusinessName())
                        .setBizCategoryName(medicalRecord.getBizCategoryId() == null
                                ? "NA"
                                : MedicalDepartmentEnum.valueOf(medicalRecord.getBizCategoryId()).getDescription());

                if (null != medicalRecord.getMedicalPhysical()) {
                    jsonMedicalRecord.setMedicalPhysical(
                            new JsonMedicalPhysical()
                                    .setBloodPressure(medicalRecord.getMedicalPhysical().getBloodPressure())
                                    .setPluse(medicalRecord.getMedicalPhysical().getPluse())
                                    .setWeight(medicalRecord.getMedicalPhysical().getWeight()));
                }

                if (null != medicalRecord.getMedicalMedication()) {
                    List<MedicalMedicineEntity> medicalMedicines = medicalRecordService.findByIds(String.join(",", medicalRecord.getMedicalMedication().getMedicineIds()));
                    for (MedicalMedicineEntity medicalMedicine : medicalMedicines) {
                        jsonMedicalRecord.addMedicine(JsonMedicalMedicine.fromMedicalMedicine(medicalMedicine));
                    }
                }

                List<JsonRecordAccess> jsonRecordAccesses = new ArrayList<>();
                for (Long date : medicalRecord.getRecordAccessed().keySet()) {
                    String accessedBy = medicalRecord.getRecordAccessed().get(date);
                    JsonRecordAccess jsonRecordAccess = new JsonRecordAccess()
                            .setRecordAccessedDate(DateUtil.dateToString(new Date(date)))
                            .setRecordAccessedQid("#######");

                    jsonRecordAccesses.add(jsonRecordAccess);
                }
                jsonMedicalRecord.setRecordAccess(jsonRecordAccesses);
                jsonMedicalRecordList.addJsonMedicalRecords(jsonMedicalRecord);
            }

            return jsonMedicalRecordList.asJson();
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
