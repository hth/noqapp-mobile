package com.noqapp.mobile.view;

import com.mongodb.MongoClient;
import com.noqapp.mobile.service.AccountMobileService;
import com.noqapp.mobile.service.DeviceService;
import com.noqapp.mobile.service.WebConnectorService;
import com.noqapp.mobile.view.validator.AccountClientValidator;
import com.noqapp.repository.EmailValidateManager;
import com.noqapp.repository.EmailValidateManagerImpl;
import com.noqapp.repository.ForgotRecoverManager;
import com.noqapp.repository.ForgotRecoverManagerImpl;
import com.noqapp.repository.GenerateUserIdManager;
import com.noqapp.repository.GenerateUserIdManagerImpl;
import com.noqapp.repository.InviteManager;
import com.noqapp.repository.InviteManagerImpl;
import com.noqapp.repository.RegisteredDeviceManager;
import com.noqapp.repository.RegisteredDeviceManagerImpl;
import com.noqapp.repository.UserAccountManager;
import com.noqapp.repository.UserAccountManagerImpl;
import com.noqapp.repository.UserAuthenticationManager;
import com.noqapp.repository.UserAuthenticationManagerImpl;
import com.noqapp.repository.UserPreferenceManager;
import com.noqapp.repository.UserPreferenceManagerImpl;
import com.noqapp.repository.UserProfileManager;
import com.noqapp.repository.UserProfileManagerImpl;
import com.noqapp.service.AccountService;
import com.noqapp.service.EmailValidateService;
import com.noqapp.service.GenerateUserIdService;
import com.noqapp.service.InviteService;
import com.noqapp.service.UserProfilePreferenceService;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * hitender
 * 12/7/17 11:53 AM
 */
public class ITest extends RealMongoForITest {

    public AccountService accountService;
    public AccountMobileService accountMobileService;
    public UserProfilePreferenceService userProfilePreferenceService;
    public InviteService inviteService;
    public AccountClientValidator accountClientValidator;
    public DeviceService deviceService;

    public UserAccountManager userAccountManager;
    public UserAuthenticationManager userAuthenticationManager;
    public UserPreferenceManager userPreferenceManager;
    public UserProfileManager userProfileManager;
    public GenerateUserIdService generateUserIdService;
    public EmailValidateManager emailValidateManager;
    public EmailValidateService emailValidateService;
    public ForgotRecoverManager forgotRecoverManager;
    public InviteManager inviteManager;
    public RegisteredDeviceManager registeredDeviceManager;

    public GenerateUserIdManager generateUserIdManager;

    @Mock public WebConnectorService webConnectorService;

    @BeforeEach
    public void globalISetup() {
        MockitoAnnotations.initMocks(this);

        userAccountManager = new UserAccountManagerImpl(getMongoTemplate());
        userAuthenticationManager = new UserAuthenticationManagerImpl(getMongoTemplate());
        userPreferenceManager = new UserPreferenceManagerImpl(getMongoTemplate());
        userProfileManager = new UserProfileManagerImpl(getMongoTemplate());
        generateUserIdManager = new GenerateUserIdManagerImpl(getMongoTemplate());
        generateUserIdService = new GenerateUserIdService(generateUserIdManager);
        emailValidateManager = new EmailValidateManagerImpl(getMongoTemplate());
        emailValidateService = new EmailValidateService(emailValidateManager);
        inviteManager = new InviteManagerImpl(getMongoTemplate());
        inviteService = new InviteService(inviteManager);
        forgotRecoverManager = new ForgotRecoverManagerImpl(getMongoTemplate());
        registeredDeviceManager = new RegisteredDeviceManagerImpl(getMongoTemplate());

        accountService = new AccountService(
            userAccountManager,
            userAuthenticationManager,
            userPreferenceManager,
            userProfileManager,
            generateUserIdService,
            emailValidateService,
            inviteService,
            forgotRecoverManager
        );

        accountMobileService = new AccountMobileService(
            "/webapi/mobile/mail/accountSignup.htm",
              webConnectorService,
              accountService
        );

        userProfilePreferenceService = new UserProfilePreferenceService(
                userProfileManager,
                userPreferenceManager
        );

        accountClientValidator = new AccountClientValidator(4, 5, 1, 2, 6);
        deviceService = new DeviceService(registeredDeviceManager);
    }
}
