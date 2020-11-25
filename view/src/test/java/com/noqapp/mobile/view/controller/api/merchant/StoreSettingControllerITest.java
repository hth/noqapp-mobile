package com.noqapp.mobile.view.controller.api.merchant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BizNameEntity;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.UserAccountEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.types.ActionTypeEnum;
import com.noqapp.mobile.domain.JsonStoreSetting;
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
 * 10/26/18 1:27 PM
 */
@DisplayName("Manage Queue Setting API")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("api")
class StoreSettingControllerITest extends ITest {

    private StoreSettingController storeSettingController;

    @BeforeEach
    void setUp() {
        storeSettingController = new StoreSettingController(
            bizService,
            accountService,
            queueMobileService,
            scheduledTaskManager,
            authenticateMobileService,
            businessUserStoreService,
            tokenQueueMobileService,
            bizStoreElasticService,
            storeHourService,
            apiHealthService
        );
    }


    @Test
    @DisplayName("Checks the state of a queue and the modify the state of it")
    void queueState_Modify_QueueState() throws IOException {
        queueState();
        queueStateModify();
    }

    private void queueState() throws IOException {
        BizNameEntity bizName = bizService.findByPhone("9118000000000");
        BizStoreEntity bizStore = bizService.findOneBizStore(bizName.getId());

        UserProfileEntity queueSupervisorUserProfile = accountService.checkUserExistsByPhone("9118000000031");
        UserAccountEntity queueUserAccount = accountService.findByQueueUserId(queueSupervisorUserProfile.getQueueUserId());
        String storeState = storeSettingController.storeState(
            new ScrubbedInput(did),
            new ScrubbedInput(deviceType),
            new ScrubbedInput(queueUserAccount.getUserId()),
            new ScrubbedInput(queueUserAccount.getUserAuthentication().getAuthenticationKey()),
            new ScrubbedInput(bizStore.getCodeQR()),
            httpServletResponse
        );
        JsonStoreSetting jsonStoreSetting = new ObjectMapper().readValue(storeState, JsonStoreSetting.class);
        assertFalse(jsonStoreSetting.isPreventJoining());
        assertFalse(jsonStoreSetting.isDayClosed());
        assertEquals(0, jsonStoreSetting.getAvailableTokenCount());
    }

    private void queueStateModify() throws IOException {
        BizNameEntity bizName = bizService.findByPhone("9118000000000");
        BizStoreEntity bizStore = bizService.findOneBizStore(bizName.getId());

        JsonStoreSetting jsonStoreSetting = new JsonStoreSetting()
            .setCodeQR(bizStore.getCodeQR())
            .setDayClosed(true)
            .setPreventJoining(true)
            .setAvailableTokenCount(0)
            .setStoreActionType(ActionTypeEnum.ACTIVE);

        UserProfileEntity queueSupervisorUserProfile = accountService.checkUserExistsByPhone("9118000000031");
        UserAccountEntity queueUserAccount = accountService.findByQueueUserId(queueSupervisorUserProfile.getQueueUserId());
        String queueStateResponse = storeSettingController.modify(
            new ScrubbedInput(did),
            new ScrubbedInput(deviceType),
            new ScrubbedInput(queueUserAccount.getUserId()),
            new ScrubbedInput(queueUserAccount.getUserAuthentication().getAuthenticationKey()),
            jsonStoreSetting,
            httpServletResponse
        );
        JsonStoreSetting jsonModifiedQueue = new ObjectMapper().readValue(queueStateResponse, JsonStoreSetting.class);
        assertTrue(jsonModifiedQueue.isPreventJoining());
        assertTrue(jsonModifiedQueue.isDayClosed());
        assertEquals(0, jsonModifiedQueue.getAvailableTokenCount());

        /* Reset State of Queue to Day Closed as False and Prevent Joining as False. */
        resetQueueAsOpen(bizStore, queueUserAccount);
    }

    private void resetQueueAsOpen(BizStoreEntity bizStore, UserAccountEntity queueUserAccount) throws IOException {
        JsonStoreSetting jsonStoreSetting = new JsonStoreSetting()
            .setCodeQR(bizStore.getCodeQR())
            .setDayClosed(false)
            .setPreventJoining(false)
            .setAvailableTokenCount(0)
            .setStoreActionType(ActionTypeEnum.ACTIVE);

        storeSettingController.modify(
            new ScrubbedInput(did),
            new ScrubbedInput(deviceType),
            new ScrubbedInput(queueUserAccount.getUserId()),
            new ScrubbedInput(queueUserAccount.getUserAuthentication().getAuthenticationKey()),
            jsonStoreSetting,
            httpServletResponse
        );
    }
}
