package com.noqapp.mobile.view.controller.api.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.UserAccountEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.types.GenderEnum;
import com.noqapp.mobile.domain.JsonProfile;
import com.noqapp.mobile.domain.body.client.MigrateProfile;
import com.noqapp.mobile.domain.body.client.UpdateProfile;
import com.noqapp.mobile.view.ITest;
import com.noqapp.mobile.view.controller.api.ProfileCommonHelper;
import com.noqapp.mobile.view.validator.ImageValidator;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;

/**
 * User: hitender
 * Date: 7/22/18 4:12 PM
 */
@DisplayName("Manage Queue API")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("api")
class ClientProfileAPIControllerITest extends ITest {

    private ProfileCommonHelper profileCommonHelper;
    private ClientProfileAPIController clientProfileAPIController;

    @BeforeEach
    void setUp() {
        profileCommonHelper = new ProfileCommonHelper(
                authenticateMobileService,
                accountClientValidator,
                accountMobileService,
                fileService,
                apiHealthService
        );

        clientProfileAPIController = new ClientProfileAPIController(
                authenticateMobileService,
                userProfilePreferenceService,
                apiHealthService,
                accountClientValidator,
                accountMobileService,
                userAddressService,
                userMedicalProfileService,
                profileCommonHelper,
                new ImageValidator()
        );
    }
    
    @Test
    void fetch() throws IOException {
        UserProfileEntity userProfile = userProfileManager.findOneByPhone("9118000000001");
        UserAccountEntity userAccount = userAccountManager.findByQueueUserId(userProfile.getQueueUserId());

        String jsonProfileAsJson = clientProfileAPIController.fetch(
                new ScrubbedInput(userProfile.getEmail()),
                new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
                httpServletResponse
        );

        JsonProfile jsonProfile = new ObjectMapper().readValue(
                jsonProfileAsJson,
                JsonProfile.class);

        assertEquals("1800 000 0001", jsonProfile.getPhoneRaw());
    }

    @Test
    void update() throws IOException {
        UserProfileEntity userProfile = userProfileManager.findOneByPhone("9118000000001");
        UserAccountEntity userAccount = userAccountManager.findByQueueUserId(userProfile.getQueueUserId());

        UpdateProfile updateProfile = new UpdateProfile()
                .setQueueUserId(userProfile.getQueueUserId())
                .setAddress("Navi Mumbai, India")
                .setBirthday("2000-01-31")
                .setFirstName("Shamsher Bhadur")
                .setGender(GenderEnum.F.name())
                .setTimeZoneId("Asia/Calcutta");

        String jsonProfileUpdatedAsJson = clientProfileAPIController.update(
                new ScrubbedInput(userProfile.getEmail()),
                new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
                updateProfile.asJson(),
                httpServletResponse
        );

        JsonProfile jsonProfile = new ObjectMapper().readValue(
                jsonProfileUpdatedAsJson,
                JsonProfile.class);

        assertEquals("Shamsher Bhadur", jsonProfile.getName());
        assertEquals("1800 000 0001", jsonProfile.getPhoneRaw());
        assertEquals(userProfile.getQueueUserId(), jsonProfile.getQueueUserId());
    }


    @Test
    void migrate() throws IOException {
        UserProfileEntity userProfile = userProfileManager.findOneByPhone("9118000000001");
        UserAccountEntity userAccount = userAccountManager.findByQueueUserId(userProfile.getQueueUserId());

        MigrateProfile migrateProfile = new MigrateProfile()
                .setPhone("91 900 400 5000")
                .setCountryShortName("IN")
                .setTimeZoneId("Asia/Calcutta");

        String jsonProfileAsJson = clientProfileAPIController.migrate(
                new ScrubbedInput(userProfile.getEmail()),
                new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
                migrateProfile.asJson(),
                httpServletResponse
        );

        JsonProfile jsonProfile = new ObjectMapper().readValue(
                jsonProfileAsJson,
                JsonProfile.class);

        assertEquals("090040 05000", jsonProfile.getPhoneRaw());
    }
}
