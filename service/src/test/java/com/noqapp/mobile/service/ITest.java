package com.noqapp.mobile.service;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.UserAccountEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.types.GenderEnum;
import com.noqapp.domain.types.UserLevelEnum;
import com.noqapp.mobile.domain.body.client.Registration;
import com.noqapp.repository.EmailValidateManager;
import com.noqapp.repository.EmailValidateManagerImpl;
import com.noqapp.repository.ForgotRecoverManager;
import com.noqapp.repository.ForgotRecoverManagerImpl;
import com.noqapp.repository.GenerateUserIdManager;
import com.noqapp.repository.GenerateUserIdManagerImpl;
import com.noqapp.repository.PointEarnedManager;
import com.noqapp.repository.UserAccountManager;
import com.noqapp.repository.UserAddressManager;
import com.noqapp.repository.UserAddressManagerImpl;
import com.noqapp.repository.UserAuthenticationManager;
import com.noqapp.repository.UserPreferenceManager;
import com.noqapp.repository.UserProfileManager;
import com.noqapp.service.AccountService;
import com.noqapp.service.EmailValidateService;
import com.noqapp.service.GenerateUserIdService;
import com.noqapp.service.UserAddressService;

import org.springframework.data.redis.core.StringRedisTemplate;

import org.junit.jupiter.api.BeforeAll;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

/**
 * hitender
 * 8/6/21 11:17 PM
 */
public class ITest extends RealMongoForITest {

    protected AccountService accountService;
    protected GenerateUserIdService generateUserIdService;
    protected EmailValidateService emailValidateService;
    protected UserAddressService userAddressService;

    protected UserAccountManager userAccountManager;
    protected UserAuthenticationManager userAuthenticationManager;
    protected UserPreferenceManager userPreferenceManager;
    protected UserProfileManager userProfileManager;
    protected PointEarnedManager pointEarnedManager;
    protected EmailValidateManager emailValidateManager;
    protected ForgotRecoverManager forgotRecoverManager;
    protected GenerateUserIdManager generateUserIdManager;
    protected UserAddressManager userAddressManager;

    @Mock protected StringRedisTemplate stringRedisTemplate;

    @BeforeAll
    public void globalISetup() throws IOException {
        MockitoAnnotations.openMocks(this);

        forgotRecoverManager = new ForgotRecoverManagerImpl(getMongoTemplate());
        generateUserIdManager = new GenerateUserIdManagerImpl(getMongoTemplate());
        emailValidateManager = new EmailValidateManagerImpl(getMongoTemplate());
        userAddressManager = new UserAddressManagerImpl(5, getMongoTemplate());

        generateUserIdService = new GenerateUserIdService(generateUserIdManager);
        emailValidateService = new EmailValidateService(emailValidateManager);
        userAddressService = new UserAddressService(5, userAddressManager);

        accountService = new AccountService(
            userAccountManager,
            userAuthenticationManager,
            userPreferenceManager,
            userProfileManager,
            pointEarnedManager,
            generateUserIdService,
            emailValidateService,
            forgotRecoverManager,
            userAddressService,
            stringRedisTemplate
        );

        registerUser();
    }

    private void registerUser() {
        /* System Users. Like Admin, Supervisor. */
        addSystemUsers();

        /* Clients. */
        addClients();
    }

    private void addSystemUsers() {

    }

    private void addClients() {

    }
}
