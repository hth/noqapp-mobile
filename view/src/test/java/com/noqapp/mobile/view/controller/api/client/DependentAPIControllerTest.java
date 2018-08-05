package com.noqapp.mobile.view.controller.api.client;

import static org.junit.jupiter.api.Assertions.*;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.UserAccountEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.types.DeviceTypeEnum;
import com.noqapp.domain.types.GenderEnum;
import com.noqapp.domain.types.UserLevelEnum;
import com.noqapp.medical.domain.json.JsonMedicalRecordList;
import com.noqapp.mobile.domain.JsonProfile;
import com.noqapp.mobile.domain.body.client.Registration;
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
 * 8/5/18 12:24 AM
 */
@DisplayName("Add dependent API")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("api")
class DependentAPIControllerTest extends ITest {

    private DependentAPIController dependentAPIController;

    @BeforeEach
    void setUp() {
        dependentAPIController = new DependentAPIController(
            accountService,
            accountMobileService,
            accountClientValidator,
            deviceService,
            authenticateMobileService,
            apiHealthService
        );
    }

    @DisplayName("Add dependent")
    @Test
    void add() throws IOException {
        UserProfileEntity userProfile = accountService.checkUserExistsByPhone("9118000000002");
        UserAccountEntity userAccount = userAccountManager.findByQueueUserId(userProfile.getQueueUserId());
        Registration registration = new Registration()
            .setQueueUserId(userProfile.getQueueUserId())
            .setBirthday("2002-02-02")
            .setFirstName("Name First")
            .setGender(GenderEnum.F.name())
            .setPhone("9118000000002")
            .setMail("")
            .setPassword("")
            .setInviteCode("")
            .setCountryShortName(userProfile.getCountryShortName())
            .setTimeZoneId(userProfile.getTimeZone());

        String response = dependentAPIController.add(
            new ScrubbedInput("12345-A"),
            new ScrubbedInput(DeviceTypeEnum.A.getName()),
            new ScrubbedInput(userProfile.getEmail()),
            new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
            registration.asJson(),
            httpServletResponse
        );

        JsonProfile jsonProfile = new ObjectMapper().readValue(response, JsonProfile.class);
        assertEquals("1800 000 0002", jsonProfile.getPhoneRaw());
        assertEquals("Pintoa D Mani", jsonProfile.getName());
        assertEquals("pintod@r.com", jsonProfile.getMail());
        assertEquals("Name First", jsonProfile.getDependents().get(0).getName());
        assertEquals(UserLevelEnum.CLIENT, jsonProfile.getDependents().get(0).getUserLevel());
    }
}