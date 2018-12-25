package com.noqapp.mobile.view.controller.api.merchant;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.UserAccountEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.json.JsonNameDatePair;
import com.noqapp.domain.json.JsonProfessionalProfile;
import com.noqapp.domain.json.JsonResponse;
import com.noqapp.domain.types.AppFlavorEnum;
import com.noqapp.domain.types.UserLevelEnum;
import com.noqapp.mobile.common.util.ErrorJsonList;
import com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum;
import com.noqapp.mobile.domain.JsonMerchant;
import com.noqapp.mobile.domain.JsonProfile;
import com.noqapp.mobile.domain.body.client.UpdateProfile;
import com.noqapp.mobile.view.ITest;
import com.noqapp.mobile.view.controller.api.ProfileCommonHelper;
import com.noqapp.mobile.view.validator.ImageValidator;
import com.noqapp.mobile.view.validator.ProfessionalProfileValidator;

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
    private ProfessionalProfileValidator professionalProfileValidator;

    @BeforeEach
    void setUp() {
        imageValidator = new ImageValidator();
        professionalProfileValidator = new ProfessionalProfileValidator(professionalProfileService);

        profileCommonHelper = new ProfileCommonHelper(
            authenticateMobileService,
            accountClientValidator,
            accountMobileService,
            fileService,
            professionalProfileValidator,
            apiHealthService
        );

        merchantProfileController = new MerchantProfileController(
            authenticateMobileService,
            userProfilePreferenceService,
            businessUserStoreService,
            profileCommonHelper,
            professionalProfileService,
            apiHealthService,
            imageValidator,
            deviceService,
            accountMobileService
        );
    }

    @Test
    public void fetch() throws Exception {
        UserProfileEntity queueManagerUserProfile = accountService.checkUserExistsByPhone("9118000000032");
        UserAccountEntity userAccount = accountService.findByQueueUserId(queueManagerUserProfile.getQueueUserId());
        String jsonMerchantAsString = merchantProfileController.fetch(
            new ScrubbedInput(did),
            new ScrubbedInput(deviceType),
            new ScrubbedInput(AppFlavorEnum.NQMH.getName()),
            new ScrubbedInput(userAccount.getUserId()),
            new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
            httpServletResponse
        );

        JsonMerchant jsonMerchant = new ObjectMapper().readValue(jsonMerchantAsString, JsonMerchant.class);
        assertEquals("manager_doctor@r.com", jsonMerchant.getJsonProfile().getMail());
        assertEquals(UserLevelEnum.S_MANAGER, jsonMerchant.getJsonProfile().getUserLevel());
        assertEquals("About Me", jsonMerchant.getJsonProfessionalProfile().getAboutMe());
        assertEquals(queueManagerUserProfile.getName(), jsonMerchant.getJsonProfessionalProfile().getName());
    }

    @Test
    void update() throws IOException {
        UserProfileEntity queueManagerUserProfile = accountService.checkUserExistsByPhone("9118000000032");
        UserAccountEntity userAccount = accountService.findByQueueUserId(queueManagerUserProfile.getQueueUserId());
        String jsonMerchantAsString = merchantProfileController.fetch(
            new ScrubbedInput(did),
            new ScrubbedInput(deviceType),
            new ScrubbedInput(AppFlavorEnum.NQMH.getName()),
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
        assertEquals(null, jsonProfileUpdated.getAddress());

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
            new ScrubbedInput(did),
            new ScrubbedInput(deviceType),
            new ScrubbedInput(AppFlavorEnum.NQMH.getName()),
            new ScrubbedInput(userAccount.getUserId()),
            new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
            httpServletResponse
        );

        JsonMerchant jsonMerchant = new ObjectMapper().readValue(jsonMerchantAsString, JsonMerchant.class);
        jsonMerchant.getJsonProfessionalProfile().setAboutMe("About Myself");
        jsonMerchant.getJsonProfessionalProfile().getEducation().add(new JsonNameDatePair().setName("M.D").setMonthYear("1990-12-12"));

        String jsonProfessionalProfileAsString = merchantProfileController.updateProfessionalProfile(
            new ScrubbedInput(userAccount.getUserId()),
            new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
            jsonMerchant.getJsonProfessionalProfile(),
            httpServletResponse
        );

        JsonProfessionalProfile jsonProfessionalProfile = new ObjectMapper().readValue(jsonProfessionalProfileAsString, JsonProfessionalProfile.class);
        assertEquals("About Myself", jsonProfessionalProfile.getAboutMe());
        assertEquals(2, jsonProfessionalProfile.getEducation().size());
        assertEquals(
            jsonMerchant.getJsonProfessionalProfile().getEducation().get(1).getMonthYear(),
            jsonProfessionalProfile.getEducation().get(1).getMonthYear());
    }

    @Test
    void updateProfessionalProfile_Fail() throws IOException {
        UserProfileEntity queueManagerUserProfile = accountService.checkUserExistsByPhone("9118000000032");
        UserAccountEntity userAccount = accountService.findByQueueUserId(queueManagerUserProfile.getQueueUserId());
        String jsonMerchantAsString = merchantProfileController.fetch(
            new ScrubbedInput(did),
            new ScrubbedInput(deviceType),
            new ScrubbedInput(AppFlavorEnum.NQMH.getName()),
            new ScrubbedInput(userAccount.getUserId()),
            new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
            httpServletResponse
        );

        JsonMerchant jsonMerchant = new ObjectMapper().readValue(jsonMerchantAsString, JsonMerchant.class);
        jsonMerchant.getJsonProfessionalProfile().setAboutMe("About Myself");
        jsonMerchant.getJsonProfessionalProfile().getEducation().add(new JsonNameDatePair().setName("M.D").setMonthYear("Dec 06, 2018"));

        String responseJson = merchantProfileController.updateProfessionalProfile(
            new ScrubbedInput(userAccount.getUserId()),
            new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
            jsonMerchant.getJsonProfessionalProfile(),
            httpServletResponse
        );

        ErrorJsonList errorJsonList = new ObjectMapper().readValue(responseJson, ErrorJsonList.class);
        assertEquals(errorJsonList.getError().getSystemError(), MobileSystemErrorCodeEnum.USER_INPUT.name());
    }

    @Test
    void intellisense() throws IOException {
        UserProfileEntity queueManagerUserProfile = accountService.checkUserExistsByPhone("9118000000032");
        UserAccountEntity userAccount = accountService.findByQueueUserId(queueManagerUserProfile.getQueueUserId());
        String jsonMerchantAsString = merchantProfileController.fetch(
            new ScrubbedInput(did),
            new ScrubbedInput(deviceType),
            new ScrubbedInput(AppFlavorEnum.NQMH.getName()),
            new ScrubbedInput(userAccount.getUserId()),
            new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
            httpServletResponse
        );

        JsonMerchant jsonMerchant = new ObjectMapper().readValue(jsonMerchantAsString, JsonMerchant.class);
        JsonProfessionalProfile jsonProfessionalProfile = jsonMerchant.getJsonProfessionalProfile();
        assertNull(jsonProfessionalProfile.getDataDictionary());

        jsonProfessionalProfile.setDataDictionary("Setting Data Dictionary");
        String response = merchantProfileController.intellisense(
            new ScrubbedInput(did),
            new ScrubbedInput(deviceType),
            new ScrubbedInput(userAccount.getUserId()),
            new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
            jsonProfessionalProfile.asJson(),
            httpServletResponse
        );

        JsonResponse jsonResponse = new ObjectMapper().readValue(response, JsonResponse.class);
        assertEquals(1, jsonResponse.getResponse());

        jsonMerchantAsString = merchantProfileController.fetch(
            new ScrubbedInput(did),
            new ScrubbedInput(deviceType),
            new ScrubbedInput(AppFlavorEnum.NQMH.getName()),
            new ScrubbedInput(userAccount.getUserId()),
            new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
            httpServletResponse
        );

        jsonMerchant = new ObjectMapper().readValue(jsonMerchantAsString, JsonMerchant.class);
        jsonProfessionalProfile = jsonMerchant.getJsonProfessionalProfile();
        assertEquals("Setting Data Dictionary", jsonProfessionalProfile.getDataDictionary());
    }
}
