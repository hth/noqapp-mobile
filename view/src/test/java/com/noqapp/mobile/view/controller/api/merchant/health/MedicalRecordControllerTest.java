package com.noqapp.mobile.view.controller.api.merchant.health;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

import com.noqapp.common.utils.CommonUtil;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BizNameEntity;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.UserAccountEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.json.JsonPurchaseOrderList;
import com.noqapp.domain.json.JsonResponse;
import com.noqapp.domain.types.DeviceTypeEnum;
import com.noqapp.domain.types.medical.DailyFrequencyEnum;
import com.noqapp.domain.types.medical.MedicineTypeEnum;
import com.noqapp.medical.domain.json.JsonMedicalMedicine;
import com.noqapp.medical.domain.json.JsonMedicalRecord;
import com.noqapp.medical.domain.json.JsonMedicalRecordList;
import com.noqapp.mobile.domain.body.merchant.FindMedicalProfile;
import com.noqapp.mobile.view.ITest;
import com.noqapp.mobile.view.controller.api.merchant.order.PurchaseOrderController;

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
    private PurchaseOrderController purchaseOrderController;

    private BizStoreEntity bizStore;
    private UserProfileEntity queueManager_Doctor_UserProfile;

    @BeforeEach
    void setUp() {
        this.medicalRecordController = new MedicalRecordController(
            authenticateMobileService,
            apiHealthService,
            medicalRecordService,
            businessUserStoreService,
            bizService
        );

        this.purchaseOrderController = new PurchaseOrderController(
            10,
            authenticateMobileService,
            businessUserStoreService,
            purchaseOrderService,
            queueMobileService,
            tokenQueueService,
            apiHealthService
        );

        BizNameEntity bizName = bizService.findByPhone("9118000000000");
        bizStore = bizService.findOneBizStore(bizName.getId());
        queueManager_Doctor_UserProfile = accountService.checkUserExistsByPhone("9118000000032");
    }

    @DisplayName("Add medical record")
    @Test
    void add() throws IOException {
        UserAccountEntity userAccount = accountService.findByQueueUserId(queueManager_Doctor_UserProfile.getQueueUserId());
        JsonMedicalRecord jsonMedicalRecord = populateForAMedicalVisit();
        String response = medicalRecordController.add(
            new ScrubbedInput(did),
            new ScrubbedInput(deviceType),
            new ScrubbedInput(userAccount.getUserId()),
            new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
            jsonMedicalRecord.asJson(),
            httpServletResponse
        );
        JsonResponse jsonResponse = new ObjectMapper().readValue(response, JsonResponse.class);
        assertEquals(1, jsonResponse.getResponse());

        await().atMost(10, SECONDS);
        UserProfileEntity client = accountService.checkUserExistsByPhone("9118000000061");
        UserAccountEntity clientUserAccount = accountService.findByQueueUserId(client.getQueueUserId());
        BizNameEntity bizName = bizService.findByPhone("9118000000011");
        BizStoreEntity bizStore = bizService.findOneBizStore(bizName.getId());
        response = purchaseOrderController.showOrders(
            new ScrubbedInput("12345-A"),
            new ScrubbedInput(DeviceTypeEnum.A.getName()),
            new ScrubbedInput(clientUserAccount.getUserId()),
            new ScrubbedInput(clientUserAccount.getUserAuthentication().getAuthenticationKey()),
            new ScrubbedInput(bizStore.getCodeQR()),
            httpServletResponse
        );

        JsonPurchaseOrderList jsonPurchaseOrderList = new ObjectMapper().readValue(response, JsonPurchaseOrderList.class);
        assertEquals(1, jsonPurchaseOrderList.getPurchaseOrders().size());
    }

    @DisplayName("Fetch existing medical records")
    @Test
    void fetch() throws IOException {
        UserAccountEntity userAccount = accountService.findByQueueUserId(queueManager_Doctor_UserProfile.getQueueUserId());
        UserProfileEntity client = accountService.checkUserExistsByPhone("9118000000001");

        FindMedicalProfile findMedicalProfile = new FindMedicalProfile().setCodeQR(bizStore.getCodeQR()).setQueueUserId(client.getQueueUserId());
        String response = medicalRecordController.fetch(
            new ScrubbedInput(userAccount.getUserId()),
            new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
            findMedicalProfile.asJson(),
            httpServletResponse
        );

        JsonMedicalRecordList jsonMedicalRecordList = new ObjectMapper().readValue(response, JsonMedicalRecordList.class);
        assertEquals(1, jsonMedicalRecordList.getJsonMedicalRecords().size());
    }

    private JsonMedicalRecord populateForAMedicalVisit() {
        UserProfileEntity client = accountService.checkUserExistsByPhone("9118000000001");
        BizNameEntity bizNameHospital = bizService.findByPhone("9118000000000");
        BizStoreEntity bizStoreDoctor = bizService.findOneBizStore(bizNameHospital.getId());
        BizNameEntity bizNamePharmacy = bizService.findByPhone("9118000000011");
        BizStoreEntity bizStorePharmacy = bizService.findOneBizStore(bizNamePharmacy.getId());

        JsonMedicalRecord jsonMedicalRecord = new JsonMedicalRecord();
        jsonMedicalRecord.setCodeQR(bizStoreDoctor.getCodeQR());
        jsonMedicalRecord.setDiagnosedById(queueManager_Doctor_UserProfile.getQueueUserId());
        jsonMedicalRecord.setRecordReferenceId(CommonUtil.generateHexFromObjectId());
        jsonMedicalRecord.setQueueUserId(client.getQueueUserId());

        //TODO add medicine and pathology
        jsonMedicalRecord.setChiefComplain("Heart Ache")
            .setPastHistory("Some History")
            .setFamilyHistory("Family History")
            .setKnownAllergies("This is known allergy")
            .setClinicalFinding("I found this");

        JsonMedicalMedicine jsonMedicalMedicine1 = new JsonMedicalMedicine()
            .setName("Amox")
            .setStrength("250mg")
            .setDailyFrequency(DailyFrequencyEnum.FD.getName())
            .setCourse("5")
            .setMedicationWithFood("With Food")
            .setMedicationType(MedicineTypeEnum.TA.getName());

        JsonMedicalMedicine jsonMedicalMedicine2 = new JsonMedicalMedicine()
            .setName("Water Saline")
            .setStrength("250 liter")
            .setDailyFrequency("10 times a day")
            .setCourse("5")
            .setMedicationWithFood("With Food")
            .setMedicationType("Hot Syrup");

        jsonMedicalRecord
            .addMedicine(jsonMedicalMedicine1)
            .addMedicine(jsonMedicalMedicine2)
            .setStoreIdPharmacy(bizStorePharmacy.getId());

        return jsonMedicalRecord;
    }
}
