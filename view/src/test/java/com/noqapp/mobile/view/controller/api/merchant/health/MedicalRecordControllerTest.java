package com.noqapp.mobile.view.controller.api.merchant.health;

import static org.junit.jupiter.api.Assertions.*;

import com.noqapp.common.utils.CommonUtil;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BizNameEntity;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.UserAccountEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.json.JsonResponse;
import com.noqapp.medical.domain.json.JsonMedicalRecord;
import com.noqapp.medical.domain.json.JsonMedicalRecordList;
import com.noqapp.mobile.domain.body.merchant.FindMedicalProfile;
import com.noqapp.mobile.view.ITest;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;

/**
 * hitender
 * 7/26/18 12:27 PM
 */
@DisplayName("Medical Record API")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("api")
class MedicalRecordControllerTest extends ITest {

    private MedicalRecordController medicalRecordController;

    private BizNameEntity bizName;
    private BizStoreEntity bizStore;
    private UserProfileEntity queueManagerUserProfile;

    @BeforeEach
    void setUp() {
        this.medicalRecordController = new MedicalRecordController(
            authenticateMobileService,
            apiHealthService,
            medicalRecordService,
            businessUserStoreService,
            bizService
        );

        bizName = bizService.findByPhone("9118000000000");
        bizStore = bizService.findOneBizStore(bizName.getId());
        queueManagerUserProfile = accountService.checkUserExistsByPhone("9118000000032");
    }

    @DisplayName("Add medical record")
    @Test
    void add() throws IOException {
        JsonResponse jsonResponse = new ObjectMapper().readValue(addMedicalRecord(), JsonResponse.class);
        assertEquals(1, jsonResponse.getResponse());
    }

    @Test
    void fetch() throws IOException {
        addMedicalRecord();

        UserAccountEntity userAccount = accountService.findByQueueUserId(queueManagerUserProfile.getQueueUserId());
        UserProfileEntity client = accountService.checkUserExistsByPhone("9118000000001");

        FindMedicalProfile findMedicalProfile = new FindMedicalProfile().setCodeQR(bizStore.getCodeQR()).setQueueUserId(client.getQueueUserId());
        String response = medicalRecordController.fetch(
            new ScrubbedInput(userAccount.getUserId()),
            new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
            findMedicalProfile.asJson(),
            httpServletResponse
        );

        JsonMedicalRecordList jsonMedicalRecordList = new ObjectMapper().readValue(response, JsonMedicalRecordList.class);
        assertEquals(2, jsonMedicalRecordList.getJsonMedicalRecords().size());
    }

    private String addMedicalRecord() throws IOException {
        UserAccountEntity userAccount = accountService.findByQueueUserId(queueManagerUserProfile.getQueueUserId());
        UserProfileEntity client = accountService.checkUserExistsByPhone("9118000000001");

        JsonMedicalRecord jsonMedicalRecord = new JsonMedicalRecord();
        jsonMedicalRecord.setCodeQR(bizStore.getCodeQR());
        jsonMedicalRecord.setDiagnosedById(queueManagerUserProfile.getQueueUserId());
        jsonMedicalRecord.setRecordReferenceId(CommonUtil.generateHexFromObjectId());
        jsonMedicalRecord.setQueueUserId(client.getQueueUserId());

        //TODO add medicine and pathology
        jsonMedicalRecord.setChiefComplain("Heart Ache")
            .setPastHistory("Some History")
            .setFamilyHistory("Family History")
            .setKnownAllergies("This is known allergy")
            .setClinicalFinding("I found this");

        return medicalRecordController.add(
            new ScrubbedInput(did),
            new ScrubbedInput(deviceType),
            new ScrubbedInput(userAccount.getUserId()),
            new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
            jsonMedicalRecord.asJson(),
            httpServletResponse
        );
    }
}