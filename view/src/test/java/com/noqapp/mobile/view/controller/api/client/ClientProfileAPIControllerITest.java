package com.noqapp.mobile.view.controller.api.client;

import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.USER_EXISTING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.noqapp.common.errors.ErrorEncounteredJson;
import com.noqapp.common.errors.ErrorJsonList;
import com.noqapp.common.errors.MobileSystemErrorCodeEnum;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.UserAccountEntity;
import com.noqapp.domain.UserAddressEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.json.JsonProfile;
import com.noqapp.domain.json.JsonResponse;
import com.noqapp.domain.json.JsonUserAddress;
import com.noqapp.domain.json.JsonUserAddressList;
import com.noqapp.domain.shared.DecodedAddress;
import com.noqapp.domain.shared.Geocode;
import com.noqapp.domain.types.GenderEnum;
import com.noqapp.mobile.domain.body.client.MigrateProfile;
import com.noqapp.mobile.domain.body.client.UpdateProfile;
import com.noqapp.mobile.service.AccountMobileService;
import com.noqapp.mobile.view.ITest;
import com.noqapp.mobile.view.controller.api.ImageCommonHelper;
import com.noqapp.mobile.view.controller.api.ProfileCommonHelper;
import com.noqapp.mobile.view.validator.ImageValidator;
import com.noqapp.mobile.view.validator.ProfessionalProfileValidator;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.apache.commons.lang3.StringUtils;

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
@DisplayName("Client Profile API")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("api")
class ClientProfileAPIControllerITest extends ITest {

    private ProfileCommonHelper profileCommonHelper;
    private ImageCommonHelper imageCommonHelper;
    private ClientProfileAPIController clientProfileAPIController;
    private ProfessionalProfileValidator professionalProfileValidator;

    @BeforeEach
    void setUp() {
        professionalProfileValidator = new ProfessionalProfileValidator(professionalProfileService);
        profileCommonHelper = new ProfileCommonHelper(
            authenticateMobileService,
            accountClientValidator,
            accountMobileService,
            externalService,
            professionalProfileValidator,
            apiHealthService
        );

        imageCommonHelper = new ImageCommonHelper(
            accountMobileService,
            authenticateMobileService,
            fileService,
            medicalFileService,
            apiHealthService
        );

        clientProfileAPIController = new ClientProfileAPIController(
            authenticateMobileService,
            apiHealthService,
            accountClientValidator,
            accountMobileService,
            userAddressService,
            profileCommonHelper,
            imageCommonHelper,
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
            .setBirthday("2000-01-31")
            .setFirstName("PankAj  KUMAr SingH ")
            .setGender(GenderEnum.F.name())
            .setTimeZoneId("Asia/Calcutta");

        String jsonProfileUpdatedAsJson = clientProfileAPIController.update(
            new ScrubbedInput(userProfile.getEmail()),
            new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
            updateProfile,
            httpServletResponse
        );

        JsonProfile jsonProfile = new ObjectMapper().readValue(
            jsonProfileUpdatedAsJson,
            JsonProfile.class);

        assertEquals("Pankaj Kumar Singh", jsonProfile.getName());
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

    @DisplayName("Add and Delete additional address")
    @Test
    void address_Add_Delete() throws IOException {
        UserProfileEntity userProfile = userProfileManager.findOneByPhone("9118000000001");
        UserAccountEntity userAccount = userAccountManager.findByQueueUserId(userProfile.getQueueUserId());
        UserAddressEntity userAddress = userAddressService.findOneUserAddress(userProfile.getQueueUserId());

        String addressJson = clientProfileAPIController.address(
            new ScrubbedInput(userProfile.getEmail()),
            new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
            httpServletResponse
        );

        JsonUserAddressList jsonUserAddressList = new ObjectMapper().readValue(
            addressJson,
            JsonUserAddressList.class);

        assertEquals(0, jsonUserAddressList.getJsonUserAddresses().size());

        String updatedAddressTo = "Chhatrapati Shivaji International Airport, Terminal 2, Navpada, Departures area, Andheri East, Mumbai, Maharashtra 400099, India";
        Geocode geocode = Geocode.newInstance(externalService.getGeocodingResults(updatedAddressTo), updatedAddressTo);
        DecodedAddress decodedAddress = DecodedAddress.newInstance(geocode.getResults(), 0);

        JsonUserAddress jsonUserAddress = new JsonUserAddress()
            .setAddress(updatedAddressTo)
            .setArea(decodedAddress.getArea())
            .setTown(decodedAddress.getTown())
            .setDistrict(decodedAddress.getDistrict())
            .setState(decodedAddress.getState())
            .setStateShortName(decodedAddress.getStateShortName())
            .setCountryShortName(decodedAddress.getCountryShortName())
            .setLatitude(String.valueOf(decodedAddress.getCoordinate()[1]))
            .setLongitude(String.valueOf(decodedAddress.getCoordinate()[0]));

        addressJson = clientProfileAPIController.addressAdd(
            new ScrubbedInput(userProfile.getEmail()),
            new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
            jsonUserAddress,
            httpServletResponse
        );

        jsonUserAddressList = new ObjectMapper().readValue(addressJson, JsonUserAddressList.class);
        /* Size of address list is now 1. */
        assertEquals(1, jsonUserAddressList.getJsonUserAddresses().size());
        assertEquals(updatedAddressTo, jsonUserAddressList.getJsonUserAddresses().get(0).getAddress());
        assertNotNull(jsonUserAddressList.getJsonUserAddresses().get(0).getGeoHash());
        assertEquals("IN", jsonUserAddressList.getJsonUserAddresses().get(0).getCountryShortName());

        /* Add address again. */
        decodedAddress = DecodedAddress.newInstance(geocode.getResults(), 0);
        jsonUserAddress = new JsonUserAddress()
            .setAddress(updatedAddressTo)
            .setArea(decodedAddress.getArea())
            .setTown(decodedAddress.getTown())
            .setDistrict(decodedAddress.getDistrict())
            .setState(decodedAddress.getState())
            .setStateShortName(decodedAddress.getStateShortName())
            .setCountryShortName(decodedAddress.getCountryShortName())
            .setLatitude(String.valueOf(decodedAddress.getCoordinate()[1]))
            .setLongitude(String.valueOf(decodedAddress.getCoordinate()[0]));

        addressJson = clientProfileAPIController.addressAdd(
            new ScrubbedInput(userProfile.getEmail()),
            new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
            jsonUserAddress,
            httpServletResponse
        );

        jsonUserAddressList = new ObjectMapper().readValue(addressJson, JsonUserAddressList.class);
        /* Size of address list is now 2. */
        assertEquals(2, jsonUserAddressList.getJsonUserAddresses().size());

        jsonUserAddress = new JsonUserAddress().setId(jsonUserAddressList.getJsonUserAddresses().get(0).getId());
        String id = jsonUserAddressList.getJsonUserAddresses().get(1).getId();
        addressJson = clientProfileAPIController.addressDelete(
            new ScrubbedInput(userProfile.getEmail()),
            new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
            jsonUserAddress,
            httpServletResponse
        );

        jsonUserAddressList = new ObjectMapper().readValue(addressJson, JsonUserAddressList.class);
        /* Size of address list is now 1. */
        assertEquals(1, jsonUserAddressList.getJsonUserAddresses().size());
        assertEquals(id, jsonUserAddressList.getJsonUserAddresses().get(0).getId());

        UserAddressEntity userAddressAfterAdding = userAddressService.findOneUserAddress(userProfile.getQueueUserId());
        assertEquals(updatedAddressTo, userAddressAfterAdding.getAddress());
        assertTrue(userAddressAfterAdding.isPrimaryAddress());
        assertNull(userAddress);
    }

    @Test
    void migrateMail_Fail_When_Email_Existing() throws IOException {
        //Rocket
        UserProfileEntity userProfile1 = userProfileManager.findOneByPhone("9118000000001");
        UserAccountEntity userAccount1 = userAccountManager.findByQueueUserId(userProfile1.getQueueUserId());
        userAccount1.setAccountValidated(true);
        userAccount1.setPhoneValidated(true);
        accountService.save(userAccount1);

        //Pintoa D
        UserProfileEntity userProfile2 = userProfileManager.findOneByPhone("9118000000002");
        UserAccountEntity userAccount2 = userAccountManager.findByQueueUserId(userProfile1.getQueueUserId());
        userAccount2.setAccountValidated(true);
        userAccount2.setPhoneValidated(true);
        accountService.save(userAccount2);

        JsonObject json = new JsonObject();
        json.addProperty(AccountMobileService.ACCOUNT_MAIL_MIGRATE.EM.name(), userProfile2.getEmail());
        String jsonRequest = new Gson().toJson(json);

        String response = clientProfileAPIController.changeMail(
            new ScrubbedInput(userProfile1.getEmail()),
            new ScrubbedInput(userAccount1.getUserAuthentication().getAuthenticationKey()),
            jsonRequest,
            httpServletResponse
        );
        JsonResponse jsonResponse = new ObjectMapper().readValue(response, JsonResponse.class);
        assertEquals(1, jsonResponse.getResponse());

        assertNull(userProfile1.getMailOTP());
        userProfile1 = userProfileManager.findOneByPhone("9118000000001");
        assertNotNull(userProfile1.getMailOTP());

        json = new JsonObject();
        json.addProperty("userId", userProfile2.getEmail());
        json.addProperty("mailOTP", userProfile1.getMailOTP());
        jsonRequest = new Gson().toJson(json);

        String errorResponse = clientProfileAPIController.migrateMail(
            new ScrubbedInput(userProfile1.getEmail()),
            new ScrubbedInput(userAccount1.getUserAuthentication().getAuthenticationKey()),
            jsonRequest,
            httpServletResponse
        );

        ErrorJsonList errorJsonList = new ObjectMapper().readValue(errorResponse, ErrorJsonList.class);
        assertEquals("User already exists. Cannot continue migration.", errorJsonList.getError().getReason());
        assertEquals(MobileSystemErrorCodeEnum.USER_EXISTING.getCode(), errorJsonList.getError().getSystemErrorCode());
        assertEquals(MobileSystemErrorCodeEnum.USER_EXISTING.name(), errorJsonList.getError().getSystemError());
    }
}
