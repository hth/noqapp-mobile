package com.noqapp.mobile.view.controller.open;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.UserAccountEntity;
import com.noqapp.domain.UserAuthenticationEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.json.JsonProfile;
import com.noqapp.domain.types.DeviceTypeEnum;
import com.noqapp.domain.types.GenderEnum;
import com.noqapp.domain.types.RoleEnum;
import com.noqapp.domain.types.UserLevelEnum;
import com.noqapp.mobile.common.util.ErrorJsonList;
import com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum;
import com.noqapp.mobile.domain.body.client.Login;
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
 * 12/6/17 7:07 PM
 */
@DisplayName("Create User Account and Login User API")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("api")
class AccountClientControllerITest extends ITest {
    private AccountClientController accountClientController;

    @BeforeEach
    void setUp() {
        accountClientController = new AccountClientController(
            accountService,
            accountMobileService,
            accountClientValidator,
            deviceMobileService
        );
    }

    @Test
    @DisplayName("Register User")
    void register() throws IOException {
        String deviceType = DeviceTypeEnum.A.getName();

        Registration registration = new Registration()
            .setPhone("+9118000000010")
            .setFirstName("ROCKET mAniA")
            .setMail("rocket@r.com")
            .setPassword("password")
            .setBirthday("2000-12-12")
            .setGender("M")
            .setCountryShortName("IN")
            .setTimeZoneId("Asia/Calcutta")
            .setInviteCode("");

        String profile = accountClientController.register(
            new ScrubbedInput(did),
            new ScrubbedInput(deviceType),
            registration.asJson(),
            httpServletResponse);

        UserProfileEntity userProfile = accountService.checkUserExistsByPhone(registration.getPhone());
        assertEquals("Rocket", userProfile.getFirstName());
        assertEquals("Mania", userProfile.getLastName());
        assertEquals(UserLevelEnum.CLIENT, userProfile.getLevel());
        assertEquals("18000000010", userProfile.getPhoneRaw());
        assertNotNull(userProfile.getInviteCode());

        UserAccountEntity userAccount = accountMobileService.findByQueueUserId(userProfile.getQueueUserId());
        assertEquals("Rocket", userAccount.getFirstName());
        assertEquals("Mania", userAccount.getLastName());
        assertEquals(RoleEnum.ROLE_CLIENT, userAccount.getRoles().iterator().next());

        UserAuthenticationEntity userAuthentication = userAccount.getUserAuthentication();
        assertTrue(userAuthentication.getAuthenticationKey().startsWith("$2a$"));
        assertTrue(userAuthentication.getPassword().startsWith("$e0801$"));

        JsonProfile jsonProfile = new ObjectMapper().readValue(profile, JsonProfile.class);
        assertEquals("Rocket Mania", jsonProfile.getName());
        assertEquals("rocket@r.com", jsonProfile.getMail());
        assertEquals("IN", jsonProfile.getCountryShortName());
        assertEquals("1800 000 0010", jsonProfile.getPhoneRaw());
        assertEquals("Asia/Calcutta", jsonProfile.getTimeZone());
        assertNotNull(jsonProfile.getInviteCode());
        assertEquals("2000-12-12", jsonProfile.getBirthday());
        assertEquals(GenderEnum.M, jsonProfile.getGender());
        assertEquals(UserLevelEnum.CLIENT, jsonProfile.getUserLevel());
    }

    @Test
    @DisplayName("Login User")
    void login() throws IOException {
        String deviceType = DeviceTypeEnum.A.getName();

        Login login = new Login()
            .setPhone("+9118000000010")
            .setCountryShortName("IN");

        String profile = accountClientController.login(
            new ScrubbedInput(did),
            new ScrubbedInput(deviceType),
            login.asJson(),
            httpServletResponse
        );

        JsonProfile jsonProfile = new ObjectMapper().readValue(profile, JsonProfile.class);
        assertEquals("Rocket Mania", jsonProfile.getName());
        assertEquals("rocket@r.com", jsonProfile.getMail());
        assertEquals("IN", jsonProfile.getCountryShortName());
        assertEquals("1800 000 0010", jsonProfile.getPhoneRaw());
        assertEquals("Asia/Calcutta", jsonProfile.getTimeZone());
        assertNotNull(jsonProfile.getInviteCode());
        assertEquals("2000-12-12", jsonProfile.getBirthday());
        assertEquals(GenderEnum.M, jsonProfile.getGender());
        assertEquals(UserLevelEnum.CLIENT, jsonProfile.getUserLevel());
    }

    @Test
    @DisplayName("Login Fail when user does not exists")
    void login_not_found_user() throws IOException {
        String deviceType = DeviceTypeEnum.A.getName();

        Login login = new Login()
            .setPhone("+9118000000011")
            .setCountryShortName("IN");

        String profile = accountClientController.login(
            new ScrubbedInput(did),
            new ScrubbedInput(deviceType),
            login.asJson(),
            httpServletResponse
        );

        ErrorJsonList errorJsonList = new ObjectMapper().readValue(profile, ErrorJsonList.class);
        assertEquals("No user found. Would you like to register?", errorJsonList.getError().getReason());
        assertEquals(MobileSystemErrorCodeEnum.USER_NOT_FOUND.getCode(), errorJsonList.getError().getSystemErrorCode());
        assertEquals(MobileSystemErrorCodeEnum.USER_NOT_FOUND.name(), errorJsonList.getError().getSystemError());
    }
}
