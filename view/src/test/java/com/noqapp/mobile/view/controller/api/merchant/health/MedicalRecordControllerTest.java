package com.noqapp.mobile.view.controller.api.merchant.health;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.noqapp.common.utils.CommonUtil;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BizNameEntity;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.UserAccountEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.json.JsonPurchaseOrderList;
import com.noqapp.domain.json.JsonResponse;
import com.noqapp.domain.json.medical.JsonUserMedicalProfile;
import com.noqapp.domain.types.DeviceTypeEnum;
import com.noqapp.domain.types.medical.DailyFrequencyEnum;
import com.noqapp.domain.types.medical.LabCategoryEnum;
import com.noqapp.domain.types.medical.MedicationIntakeEnum;
import com.noqapp.domain.types.medical.PharmacyCategoryEnum;
import com.noqapp.medical.domain.json.JsonMedicalMedicine;
import com.noqapp.medical.domain.json.JsonMedicalRadiology;
import com.noqapp.medical.domain.json.JsonMedicalRadiologyList;
import com.noqapp.medical.domain.json.JsonMedicalRecord;
import com.noqapp.medical.domain.json.JsonMedicalRecordList;
import com.noqapp.mobile.domain.body.merchant.FindMedicalProfile;
import com.noqapp.mobile.view.ITest;
import com.noqapp.mobile.view.controller.api.ImageCommonHelper;
import com.noqapp.mobile.view.controller.api.merchant.store.PurchaseOrderController;
import com.noqapp.mobile.view.validator.ImageValidator;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * hitender
 * 7/26/18 12:27 PM
 */
@DisplayName("Medical Record API")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("api")
class MedicalRecordControllerTest extends ITest {

    private ImageCommonHelper imageCommonHelper;
    private MedicalRecordController medicalRecordController;
    private PurchaseOrderController purchaseOrderController;

    private BizStoreEntity bizStore;
    private UserProfileEntity queueManager_Doctor_UserProfile;
    private ImageValidator imageValidator;

    @BeforeEach
    void setUp() {
        imageValidator = new ImageValidator();
        imageCommonHelper = new ImageCommonHelper(
            accountMobileService,
            authenticateMobileService,
            fileService,
            medicalFileService,
            apiHealthService
        );

        this.medicalRecordController = new MedicalRecordController(
            authenticateMobileService,
            apiHealthService,
            medicalRecordService,
            businessUserStoreService,
            bizService,
            medicalRecordMobileService,
            imageCommonHelper,
            imageValidator
        );

        this.purchaseOrderController = new PurchaseOrderController(
            10,
            bizStoreManager,
            medicalRadiologyManager,
            medicalPathologyManager,
            imageValidator,
            imageCommonHelper,
            authenticateMobileService,
            businessUserStoreService,
            purchaseOrderService,
            queueMobileService,
            tokenQueueService,
            medicalRecordService,
            apiHealthService
        );

        BizNameEntity bizName = bizService.findByPhone("9118000000000");
        bizStore = bizService.findOneBizStore(bizName.getId());
        queueManager_Doctor_UserProfile = accountService.checkUserExistsByPhone("9118000000032");
    }

    @DisplayName("Update medical record")
    @Test
    void update() throws IOException {
        UserAccountEntity userAccount = accountService.findByQueueUserId(queueManager_Doctor_UserProfile.getQueueUserId());
        JsonMedicalRecord jsonMedicalRecord = populateForMedicalVisit();
        String response = medicalRecordController.update(
            new ScrubbedInput(did),
            new ScrubbedInput(deviceType),
            new ScrubbedInput(userAccount.getUserId()),
            new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
            jsonMedicalRecord,
            httpServletResponse
        );
        JsonResponse jsonResponse = new ObjectMapper().readValue(response, JsonResponse.class);
        assertEquals(1, jsonResponse.getResponse());
    }

    @DisplayName("Check if order has been placed")
    @Test
    void checkOrderPlaced() throws IOException {
        UserProfileEntity supervisorProfile = accountService.checkUserExistsByPhone("9118000000061");
        UserAccountEntity supervisorUserAccount = accountService.findByQueueUserId(supervisorProfile.getQueueUserId());
        BizNameEntity bizName = bizService.findByPhone("9118000000011");
        BizStoreEntity bizStore = bizService.findOneBizStore(bizName.getId());
        await().atMost(60, SECONDS).until(awaitUntilPurchaseOrderIsPlaced(bizStore.getCodeQR()));
        String response = purchaseOrderController.showOrders(
            new ScrubbedInput("12345-A"),
            new ScrubbedInput(DeviceTypeEnum.A.getName()),
            new ScrubbedInput(supervisorUserAccount.getUserId()),
            new ScrubbedInput(supervisorUserAccount.getUserAuthentication().getAuthenticationKey()),
            new ScrubbedInput(bizStore.getCodeQR()),
            httpServletResponse
        );

        JsonPurchaseOrderList jsonPurchaseOrderList = new ObjectMapper().readValue(response, JsonPurchaseOrderList.class);
        assertEquals(1, jsonPurchaseOrderList.getPurchaseOrders().size());
    }

    @DisplayName("Historical existing medical records")
    @Test
    void historical() throws IOException {
        UserAccountEntity userAccount = accountService.findByQueueUserId(queueManager_Doctor_UserProfile.getQueueUserId());
        UserProfileEntity client = accountService.checkUserExistsByPhone("9118000000001");

        FindMedicalProfile findMedicalProfile = new FindMedicalProfile().setCodeQR(bizStore.getCodeQR()).setQueueUserId(client.getQueueUserId());
        String response = medicalRecordController.historical(
            new ScrubbedInput("12345-A"),
            new ScrubbedInput(DeviceTypeEnum.A.getName()),
            new ScrubbedInput(userAccount.getUserId()),
            new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
            findMedicalProfile,
            httpServletResponse
        );

        JsonMedicalRecordList jsonMedicalRecordList = new ObjectMapper().readValue(response, JsonMedicalRecordList.class);
        assertEquals(1, jsonMedicalRecordList.getJsonMedicalRecords().size());

        JsonMedicalRecord jsonMedicalRecord = jsonMedicalRecordList.getJsonMedicalRecords().get(0);
        assertEquals(1, jsonMedicalRecord.getMedicalRadiologyLists().size());
        assertEquals(2, jsonMedicalRecord.getMedicalMedicines().size());

        JsonMedicalRadiologyList jsonMedicalRadiologyList = jsonMedicalRecord.getMedicalRadiologyLists().get(0);
        assertEquals(2, jsonMedicalRadiologyList.getJsonMedicalRadiologies().size());
    }

    private JsonMedicalRecord populateForMedicalVisit() {
        UserProfileEntity client = accountService.checkUserExistsByPhone("9118000000001");
        BizNameEntity bizNameHospital = bizService.findByPhone("9118000000000");
        BizStoreEntity bizStoreDoctor = bizService.findOneBizStore(bizNameHospital.getId());
        BizNameEntity bizNamePharmacy = bizService.findByPhone("9118000000011");
        BizStoreEntity bizStorePharmacy = bizService.findOneBizStore(bizNamePharmacy.getId());
        BizNameEntity bizNameHealthService = bizService.findByPhone("9118000000161");
        BizStoreEntity bizStoreXRAY= bizService.findOneBizStore(bizNameHealthService.getId());

        JsonMedicalRecord jsonMedicalRecord = new JsonMedicalRecord()
            .setJsonUserMedicalProfile(new JsonUserMedicalProfile());

        jsonMedicalRecord.setCodeQR(bizStoreDoctor.getCodeQR());
        jsonMedicalRecord.setDiagnosedById(queueManager_Doctor_UserProfile.getQueueUserId());
        jsonMedicalRecord.setRecordReferenceId(CommonUtil.generateHexFromObjectId());
        jsonMedicalRecord.setQueueUserId(client.getQueueUserId());

        jsonMedicalRecord.getJsonUserMedicalProfile()
            .setPastHistory("Some History")
            .setFamilyHistory("Family History")
            .setKnownAllergies("This is known allergy");

        //TODO add medicine and pathology
        jsonMedicalRecord.setChiefComplain("Heart Ache")
            .setExamination("Examination is")
            .setClinicalFinding("I found this")
            .setProvisionalDifferentialDiagnosis("My finding is");

        JsonMedicalMedicine jsonMedicalMedicine1 = new JsonMedicalMedicine()
            .setName("Amox")
            .setStrength("250mg")
            .setDailyFrequency(DailyFrequencyEnum.FD.getName())
            .setCourse("5")
            .setMedicationIntake(MedicationIntakeEnum.AF.getDescription())
            .setPharmacyCategory(PharmacyCategoryEnum.TA.getName());

        JsonMedicalMedicine jsonMedicalMedicine2 = new JsonMedicalMedicine()
            .setName("Water Saline")
            .setStrength("250 liter")
            .setDailyFrequency("10 times a day")
            .setCourse("5")
            .setMedicationIntake(MedicationIntakeEnum.AF.getDescription())
            .setPharmacyCategory("Hot Syrup");

        jsonMedicalRecord
            .addMedicine(jsonMedicalMedicine1)
            .addMedicine(jsonMedicalMedicine2)
            .setStoreIdPharmacy(bizStorePharmacy.getId());

        JsonMedicalRadiologyList jsonMedicalRadiologyList = new JsonMedicalRadiologyList()
            .setLabCategory(LabCategoryEnum.XRAY)
            .setBizStoreId(bizStoreXRAY.getId());

        jsonMedicalRadiologyList
            .addJsonMedicalRadiologies(new JsonMedicalRadiology().setName("Bone XRAY"))
            .addJsonMedicalRadiologies(new JsonMedicalRadiology().setName("Head XRAY"));

        jsonMedicalRecord.addMedicalRadiologyLists(jsonMedicalRadiologyList);

        return jsonMedicalRecord;
    }

    private Callable<Boolean> awaitUntilPurchaseOrderIsPlaced(String codeQR) {
        return () -> purchaseOrderManager.findAllOpenOrderByCodeQR(codeQR).size() == 1;
    }
}
