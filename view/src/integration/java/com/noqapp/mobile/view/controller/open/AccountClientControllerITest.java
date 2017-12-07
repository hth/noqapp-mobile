package com.noqapp.mobile.view.controller.open;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.UserAccountEntity;
import com.noqapp.domain.UserAuthenticationEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.types.DeviceTypeEnum;
import com.noqapp.domain.types.GenderEnum;
import com.noqapp.domain.types.RoleEnum;
import com.noqapp.domain.types.UserLevelEnum;
import com.noqapp.mobile.domain.JsonProfile;
import com.noqapp.mobile.domain.body.Login;
import com.noqapp.mobile.domain.body.Registration;
import com.noqapp.mobile.service.AccountMobileService;
import com.noqapp.mobile.service.DeviceService;
import com.noqapp.mobile.view.ITest;
import com.noqapp.mobile.view.validator.AccountClientValidator;
import com.noqapp.service.AccountService;
import com.noqapp.service.InviteService;
import com.noqapp.service.UserProfilePreferenceService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * hitender
 * 12/6/17 7:07 PM
 */
@DisplayName("Create User Account and Login User")
class AccountClientControllerITest extends ITest {

    private AccountClientController accountClientController;

    @Mock private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        accountClientController = new AccountClientController(
                accountService,
                accountMobileService,
                userProfilePreferenceService,
                inviteService,
                accountClientValidator,
                deviceService
        );
    }

    @Test
    @DisplayName("Register And Login User")
    void register_and_login_success() throws IOException {
        String did = UUID.randomUUID().toString();
        String deviceType = DeviceTypeEnum.A.getName();

        register(did, deviceType);
        login(did, deviceType);
    }

    private void register(String did, String deviceType) throws IOException {
        Registration registration = new Registration()
                .setPhone("+9118000000000")
                .setFirstName("ROCKET mAniA")
                .setMail("rocket@r.com")
                .setBirthday("2000-12-12")
                .setGender("M")
                .setCountryShortName("IN")
                .setTimeZoneId("Asia/Calcutta")
                .setInviteCode("");

        String profile = accountClientController.register(
                new ScrubbedInput(did),
                new ScrubbedInput(deviceType),
                registration.asJson(),
                response);

        UserProfileEntity userProfile = accountService.checkUserExistsByPhone(registration.getPhone());
        assertEquals("Rocket", userProfile.getFirstName());
        assertEquals("Mania", userProfile.getLastName());
        assertEquals(UserLevelEnum.CLIENT, userProfile.getLevel());
        assertEquals("18000000000", userProfile.getPhoneRaw());
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
        assertEquals("1800 000 0000", jsonProfile.getPhoneRaw());
        assertEquals("Asia/Calcutta", jsonProfile.getTimeZone());
        assertNotNull(jsonProfile.getInviteCode());
        assertEquals(2, jsonProfile.getRemoteJoin());
        assertEquals("2000-12-12", jsonProfile.getBirthday());
        assertEquals(GenderEnum.M.name(), jsonProfile.getGender());
        assertEquals(UserLevelEnum.CLIENT, jsonProfile.getUserLevel());
    }

    private void login(String did, String deviceType) throws IOException {
        String profile;
        JsonProfile jsonProfile;Login login = new Login()
                .setPhone("+9118000000000")
                .setCountryShortName("IN");

        profile = accountClientController.login(
                new ScrubbedInput(did),
                new ScrubbedInput(deviceType),
                login.asJson(),
                response
        );

        jsonProfile = new ObjectMapper().readValue(profile, JsonProfile.class);
        assertEquals("Rocket Mania", jsonProfile.getName());
        assertEquals("rocket@r.com", jsonProfile.getMail());
        assertEquals("IN", jsonProfile.getCountryShortName());
        assertEquals("1800 000 0000", jsonProfile.getPhoneRaw());
        assertEquals("Asia/Calcutta", jsonProfile.getTimeZone());
        assertNotNull(jsonProfile.getInviteCode());
        assertEquals(2, jsonProfile.getRemoteJoin());
        assertEquals("2000-12-12", jsonProfile.getBirthday());
        assertEquals(GenderEnum.M.name(), jsonProfile.getGender());
        assertEquals(UserLevelEnum.CLIENT, jsonProfile.getUserLevel());
    }
}