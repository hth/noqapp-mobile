package com.noqapp.mobile.view.controller.api.merchant;


import static org.junit.jupiter.api.Assertions.assertEquals;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.UserAccountEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.json.JsonNameDatePair;
import com.noqapp.domain.json.JsonProfessionalProfile;
import com.noqapp.domain.types.UserLevelEnum;
import com.noqapp.mobile.domain.JsonMerchant;
import com.noqapp.mobile.domain.JsonProfile;
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
 * Date: 4/22/17 8:11 AM
 */
@DisplayName("Manage Queue API")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("api")
class MerchantProfileControllerITest extends ITest {

    private ImageValidator imageValidator;
    private ProfileCommonHelper profileCommonHelper;
    private MerchantProfileController merchantProfileController;

    @BeforeEach
    void setUp() {
        imageValidator = new ImageValidator();
        
        profileCommonHelper = new ProfileCommonHelper(
                authenticateMobileService,
                accountClientValidator,
                accountMobileService,
                fileService,
                apiHealthService
        );

        merchantProfileController = new MerchantProfileController(
                authenticateMobileService,
                userProfilePreferenceService,
                businessUserStoreService,
                profileCommonHelper,
                professionalProfileService,
                apiHealthService,
                imageValidator
        );
    }

    @Test
    public void fetch() throws Exception {
        UserProfileEntity queueManagerUserProfile = accountService.checkUserExistsByPhone("9118000000032");
        UserAccountEntity userAccount = accountService.findByQueueUserId(queueManagerUserProfile.getQueueUserId());
        String jsonMerchantAsString = merchantProfileController.fetch(
                new ScrubbedInput(userAccount.getUserId()),
                new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
                httpServletResponse
        );

        JsonMerchant jsonMerchant = new ObjectMapper().readValue(jsonMerchantAsString, JsonMerchant.class);
        assertEquals("manager_doctor@r.com", jsonMerchant.getJsonProfile().getMail());
        assertEquals(UserLevelEnum.S_MANAGER, jsonMerchant.getJsonProfile().getUserLevel());
        assertEquals("About Me", jsonMerchant.getJsonProfessionalProfile().getAboutMe());
    }

    @Test
    void update() throws IOException {
        UserProfileEntity queueManagerUserProfile = accountService.checkUserExistsByPhone("9118000000032");
        UserAccountEntity userAccount = accountService.findByQueueUserId(queueManagerUserProfile.getQueueUserId());
        String jsonMerchantAsString = merchantProfileController.fetch(
                new ScrubbedInput(userAccount.getUserId()),
                new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
                httpServletResponse
        );

        JsonMerchant jsonMerchant = new ObjectMapper().readValue(jsonMerchantAsString, JsonMerchant.class);
        JsonProfile jsonProfile = jsonMerchant.getJsonProfile();
        UpdateProfile updateProfile = new UpdateProfile()
                .setQueueUserId(jsonProfile.getQueueUserId())
                .setAddress(jsonProfile.getAddress())
                .setFirstName("My new Name")
                .setBirthday(jsonProfile.getBirthday())
                .setGender(jsonProfile.getGender().name())
                .setTimeZoneId(jsonProfile.getTimeZone());

        String jsonProfileAsString = merchantProfileController.update(
                new ScrubbedInput(userAccount.getUserId()),
                new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
                updateProfile.asJson(),
                httpServletResponse
        );

        JsonProfile jsonProfileUpdated = new ObjectMapper().readValue(jsonProfileAsString, JsonProfile.class);
        assertEquals("My New Name", jsonProfileUpdated.getName());
        assertEquals("", jsonProfileUpdated.getAddress());

        updateProfile = new UpdateProfile()
                .setQueueUserId(jsonProfile.getQueueUserId())
                .setAddress("Shop NO RB.1, Haware's centurion Mall, 1st Floor, Sector No 19, Nerul - East, Seawoods, Navi Mumbai, Mumbai, 400706, India")
                .setFirstName("My new Name")
                .setBirthday(jsonProfile.getBirthday())
                .setGender(jsonProfile.getGender().name())
                .setTimeZoneId(jsonProfile.getTimeZone());

        jsonProfileAsString = merchantProfileController.update(
                new ScrubbedInput(userAccount.getUserId()),
                new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
                updateProfile.asJson(),
                httpServletResponse
        );

        jsonProfileUpdated = new ObjectMapper().readValue(jsonProfileAsString, JsonProfile.class);
        assertEquals("My New Name", jsonProfileUpdated.getName());
        assertEquals("Shop NO RB.1, Haware's centurion Mall, 1st Floor, Sector No 19, Nerul - East, Seawoods, Navi Mumbai, Mumbai, 400706, India", jsonProfileUpdated.getAddress());

    }

    @Test
    void updateProfessionalProfile() throws IOException {
        UserProfileEntity queueManagerUserProfile = accountService.checkUserExistsByPhone("9118000000032");
        UserAccountEntity userAccount = accountService.findByQueueUserId(queueManagerUserProfile.getQueueUserId());
        String jsonMerchantAsString = merchantProfileController.fetch(
                new ScrubbedInput(userAccount.getUserId()),
                new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
                httpServletResponse
        );

        JsonMerchant jsonMerchant = new ObjectMapper().readValue(jsonMerchantAsString, JsonMerchant.class);
        jsonMerchant.getJsonProfessionalProfile().setAboutMe("About Myself");
        jsonMerchant.getJsonProfessionalProfile().getEducation().add(new JsonNameDatePair().setName("M.D").setMonthYear("12-12-1990"));

        String jsonProfessionalProfileAsString = merchantProfileController.updateProfessionalProfile(
                new ScrubbedInput(userAccount.getUserId()),
                new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
                jsonMerchant.getJsonProfessionalProfile().asJson(),
                httpServletResponse
        );

        JsonProfessionalProfile jsonProfessionalProfile = new ObjectMapper().readValue(jsonProfessionalProfileAsString, JsonProfessionalProfile.class);
        assertEquals("About Myself", jsonProfessionalProfile.getAboutMe());
        assertEquals(2, jsonProfessionalProfile.getEducation().size());
    }
}
