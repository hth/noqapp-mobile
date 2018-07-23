package com.noqapp.mobile.view.controller.open;

import static org.junit.jupiter.api.Assertions.*;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.BusinessUserEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.json.JsonStore;
import com.noqapp.mobile.domain.JsonProfile;
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
 * 7/23/18 12:00 PM
 */
@DisplayName("Store Detail API")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("api")
class StoreDetailControllerTest extends ITest {

    private StoreDetailController storeDetailController;

    @BeforeEach
    void setUp() {
        storeDetailController = new StoreDetailController(storeDetailService, apiHealthService);
    }

    @Test
    void getStoreDetail() throws IOException {
        UserProfileEntity userProfile = userProfileManager.findOneByPhone("9118000000030");
        BusinessUserEntity businessUser = businessUserService.findByQid(userProfile.getQueueUserId());
        BizStoreEntity bizStore = bizService.findOneBizStore(businessUser.getBizName().getId());

        String jsonStoreAsString = storeDetailController.getStoreDetail(
            new ScrubbedInput(did),
            new ScrubbedInput(deviceType),
            new ScrubbedInput(bizStore.getCodeQR()),
            httpServletResponse
        );

        JsonStore jsonStore = new ObjectMapper().readValue(
            jsonStoreAsString,
            JsonStore.class);

        assertEquals(bizStore.getCodeQR(), jsonStore.getJsonQueue().getCodeQR());
        assertEquals(bizStore.getBizCategoryId(), jsonStore.getJsonQueue().getBizCategoryId());
    }
}