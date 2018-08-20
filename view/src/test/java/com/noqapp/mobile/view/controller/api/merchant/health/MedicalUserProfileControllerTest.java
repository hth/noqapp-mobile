package com.noqapp.mobile.view.controller.api.merchant.health;

import static org.junit.jupiter.api.Assertions.*;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BizNameEntity;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.UserAccountEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.json.JsonPurchaseOrderList;
import com.noqapp.mobile.domain.JsonProfile;
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
@DisplayName("Medical Profile of a Client API")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("api")
class MedicalUserProfileControllerTest extends ITest {

    private MedicalUserProfileController medicalUserProfileController;

    private BizStoreEntity bizStore;
    private UserProfileEntity queueManagerUserProfile;

    @BeforeEach
    void setUp() {
        medicalUserProfileController = new MedicalUserProfileController(
            authenticateMobileService,
            accountMobileService,
            businessUserStoreService,
            apiHealthService
        );

        BizNameEntity bizName = bizService.findByPhone("9118000000000");
        bizStore = bizService.findOneBizStore(bizName.getId());
        queueManagerUserProfile = accountService.checkUserExistsByPhone("9118000000032");
    }

    @DisplayName("Fetch patient medical profile")
    @Test
    void fetch() throws IOException {
        UserAccountEntity userAccount = accountService.findByQueueUserId(queueManagerUserProfile.getQueueUserId());
        UserProfileEntity client = accountService.checkUserExistsByPhone("9118000000001");

        FindMedicalProfile findMedicalProfile = new FindMedicalProfile().setCodeQR(bizStore.getCodeQR()).setQueueUserId(client.getQueueUserId());
        String response = medicalUserProfileController.fetch(
            new ScrubbedInput(did),
            new ScrubbedInput(deviceType),
            new ScrubbedInput(userAccount.getUserId()),
            new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
            findMedicalProfile.asJson(),
            httpServletResponse
        );

        JsonProfile jsonProfile = new ObjectMapper().readValue(response, JsonProfile.class);
        assertEquals("1800 000 0001", jsonProfile.getPhoneRaw());
        assertEquals("Rocket Docket", jsonProfile.getName());
    }
}
