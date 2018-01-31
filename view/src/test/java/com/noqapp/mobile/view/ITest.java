package com.noqapp.mobile.view;

import com.noqapp.common.utils.CommonUtil;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BizNameEntity;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.BusinessUserEntity;
import com.noqapp.domain.BusinessUserStoreEntity;
import com.noqapp.domain.StoreHourEntity;
import com.noqapp.domain.UserAccountEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.types.AddressOriginEnum;
import com.noqapp.domain.types.BusinessTypeEnum;
import com.noqapp.domain.types.BusinessUserRegistrationStatusEnum;
import com.noqapp.domain.types.DeviceTypeEnum;
import com.noqapp.domain.types.UserLevelEnum;
import com.noqapp.health.repository.ApiHealthNowManager;
import com.noqapp.health.repository.ApiHealthNowManagerImpl;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.domain.body.client.Registration;
import com.noqapp.mobile.service.AccountMobileService;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.service.DeviceService;
import com.noqapp.mobile.service.QueueMobileService;
import com.noqapp.mobile.service.TokenQueueMobileService;
import com.noqapp.mobile.service.WebConnectorService;
import com.noqapp.mobile.view.controller.open.AccountClientController;
import com.noqapp.mobile.view.validator.AccountClientValidator;
import com.noqapp.repository.BizCategoryManager;
import com.noqapp.repository.BizCategoryManagerImpl;
import com.noqapp.repository.BizNameManager;
import com.noqapp.repository.BizNameManagerImpl;
import com.noqapp.repository.BizStoreManager;
import com.noqapp.repository.BizStoreManagerImpl;
import com.noqapp.repository.BusinessUserManager;
import com.noqapp.repository.BusinessUserManagerImpl;
import com.noqapp.repository.BusinessUserStoreManager;
import com.noqapp.repository.BusinessUserStoreManagerImpl;
import com.noqapp.repository.EmailValidateManager;
import com.noqapp.repository.EmailValidateManagerImpl;
import com.noqapp.repository.ForgotRecoverManager;
import com.noqapp.repository.ForgotRecoverManagerImpl;
import com.noqapp.repository.GenerateUserIdManager;
import com.noqapp.repository.GenerateUserIdManagerImpl;
import com.noqapp.repository.InviteManager;
import com.noqapp.repository.InviteManagerImpl;
import com.noqapp.repository.QueueManager;
import com.noqapp.repository.QueueManagerImpl;
import com.noqapp.repository.QueueManagerJDBC;
import com.noqapp.repository.RegisteredDeviceManager;
import com.noqapp.repository.RegisteredDeviceManagerImpl;
import com.noqapp.repository.StoreHourManager;
import com.noqapp.repository.StoreHourManagerImpl;
import com.noqapp.repository.TokenQueueManager;
import com.noqapp.repository.TokenQueueManagerImpl;
import com.noqapp.repository.UserAccountManager;
import com.noqapp.repository.UserAccountManagerImpl;
import com.noqapp.repository.UserAuthenticationManager;
import com.noqapp.repository.UserAuthenticationManagerImpl;
import com.noqapp.repository.UserPreferenceManager;
import com.noqapp.repository.UserPreferenceManagerImpl;
import com.noqapp.repository.UserProfileManager;
import com.noqapp.repository.UserProfileManagerImpl;
import com.noqapp.service.AccountService;
import com.noqapp.service.BizService;
import com.noqapp.service.BusinessUserService;
import com.noqapp.service.BusinessUserStoreService;
import com.noqapp.service.EmailValidateService;
import com.noqapp.service.FirebaseMessageService;
import com.noqapp.service.GenerateUserIdService;
import com.noqapp.service.InviteService;
import com.noqapp.service.QueueService;
import com.noqapp.service.TokenQueueService;
import com.noqapp.service.UserProfilePreferenceService;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeAll;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.env.MockEnvironment;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * hitender
 * 12/7/17 11:53 AM
 */
public class ITest extends RealMongoForITest {

    protected String did;
    protected String didClient1;
    protected String didClient2;
    protected String didQueueSupervisor;
    protected String fcmToken;
    protected String deviceType;

    protected AccountService accountService;
    protected AccountMobileService accountMobileService;
    protected UserProfilePreferenceService userProfilePreferenceService;
    protected InviteService inviteService;
    protected AccountClientValidator accountClientValidator;
    protected DeviceService deviceService;
    protected TokenQueueMobileService tokenQueueMobileService;
    protected TokenQueueService tokenQueueService;
    protected BizService bizService;
    protected QueueMobileService queueMobileService;
    protected QueueService queueService;
    protected AuthenticateMobileService authenticateMobileService;
    protected BusinessUserStoreService businessUserStoreService;

    protected TokenQueueManager tokenQueueManager;
    protected FirebaseMessageService firebaseMessageService;
    protected QueueManager queueManager;

    protected UserAccountManager userAccountManager;
    protected UserAuthenticationManager userAuthenticationManager;
    protected UserPreferenceManager userPreferenceManager;
    protected UserProfileManager userProfileManager;
    protected GenerateUserIdService generateUserIdService;
    protected EmailValidateManager emailValidateManager;
    protected EmailValidateService emailValidateService;
    protected ForgotRecoverManager forgotRecoverManager;
    protected InviteManager inviteManager;
    protected RegisteredDeviceManager registeredDeviceManager;
    protected BizNameManager bizNameManager;
    protected BizStoreManager bizStoreManager;
    protected StoreHourManager storeHourManager;
    protected BizCategoryManager bizCategoryManager;
    protected BusinessUserStoreManager businessUserStoreManager;
    protected BusinessUserService businessUserService;
    protected BusinessUserManager businessUserManager;

    protected ApiHealthService apiHealthService;
    protected ApiHealthNowManager apiHealthNowManager;

    protected GenerateUserIdManager generateUserIdManager;

    private AccountClientController accountClientController;
    private MockEnvironment mockEnvironment;

    @Mock protected WebConnectorService webConnectorService;
    @Mock protected QueueManagerJDBC queueManagerJDBC;
    @Mock protected HttpServletResponse httpServletResponse;

    @BeforeAll
    public void globalISetup() throws IOException {
        MockitoAnnotations.initMocks(this);

        did = UUID.randomUUID().toString();
        didClient1 = UUID.randomUUID().toString();
        didClient2 = UUID.randomUUID().toString();
        didQueueSupervisor = UUID.randomUUID().toString();
        mockEnvironment = new MockEnvironment();
        mockEnvironment.setProperty("build.env", "sandbox");


        fcmToken = UUID.randomUUID().toString();
        deviceType = DeviceTypeEnum.A.getName();

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
                5,
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

        apiHealthNowManager = new ApiHealthNowManagerImpl(getMongoTemplate());
        apiHealthService = new ApiHealthService(apiHealthNowManager);

        queueManager = new QueueManagerImpl(getMongoTemplate());
        tokenQueueManager = new TokenQueueManagerImpl(getMongoTemplate());
        bizStoreManager = new BizStoreManagerImpl(getMongoTemplate());
        storeHourManager = new StoreHourManagerImpl(getMongoTemplate());
        tokenQueueService = new TokenQueueService(
            tokenQueueManager,
            firebaseMessageService,
            queueManager,
            accountService,
            registeredDeviceManager,
            queueManagerJDBC,
            storeHourManager,
            bizStoreManager
        );

        queueService = new QueueService(
                queueManager,
                queueManagerJDBC
        );

        bizNameManager = new BizNameManagerImpl(getMongoTemplate());
        bizCategoryManager = new BizCategoryManagerImpl(getMongoTemplate());
        businessUserStoreManager = new BusinessUserStoreManagerImpl(getMongoTemplate());

        bizService = new BizService(
                69.172,
                111.321,
                bizNameManager,
                bizStoreManager,
                storeHourManager,
                bizCategoryManager,
                tokenQueueService,
                queueService,
                businessUserStoreManager
        );

        tokenQueueMobileService = new TokenQueueMobileService(
                tokenQueueService,
                bizService,
                tokenQueueManager
        );

        queueMobileService = new QueueMobileService(
                queueManager,
                tokenQueueMobileService,
                bizService,
                deviceService,
                queueManagerJDBC,
                storeHourManager,
                queueService
        );

        accountClientController = new AccountClientController(
                accountService,
                accountMobileService,
                userProfilePreferenceService,
                inviteService,
                accountClientValidator,
                deviceService
        );

        authenticateMobileService = new AuthenticateMobileService(
                userAccountManager
        );

        businessUserManager = new BusinessUserManagerImpl(getMongoTemplate());
        businessUserService = new BusinessUserService(businessUserManager);
        businessUserStoreService = new BusinessUserStoreService(
                10,
                businessUserStoreManager,
                businessUserService,
                tokenQueueService,
                accountService
        );

        registerUser();
        createBusiness("9118000000030");
    }

    private void registerUser() throws IOException {
        /* System Users. Like Admin, Supervisor. */
        addSystemUsers();

        /* Clients. */
        addClients();

        /* Store Admin and Queue Supervisors. */
        addStoreUsers();
    }

    private void addStoreUsers() throws IOException {
        Registration queueAdmin = new Registration()
                .setPhone("+9118000000030")
                .setFirstName("Diktaa D mA")
                .setMail("diktad@r.com")
                .setBirthday("2000-12-12")
                .setGender("M")
                .setCountryShortName("IN")
                .setTimeZoneId("Asia/Calcutta")
                .setInviteCode("");

        accountClientController.register(
                new ScrubbedInput(did),
                new ScrubbedInput(deviceType),
                queueAdmin.asJson(),
                httpServletResponse);

        UserProfileEntity merchantUserProfile = accountService.checkUserExistsByPhone("9118000000030");
        merchantUserProfile.setLevel(UserLevelEnum.M_ADMIN);
        accountService.save(merchantUserProfile);
        UserAccountEntity merchantUserAccount = accountService.changeAccountRolesToMatchUserLevel(
                merchantUserProfile.getQueueUserId(),
                merchantUserProfile.getLevel());
        accountService.save(merchantUserAccount);

        Registration queueSupervisor = new Registration()
                .setPhone("+9118000000031")
                .setFirstName("Fiktaa D mAn")
                .setMail("fiktad@r.com")
                .setBirthday("2000-12-12")
                .setGender("M")
                .setCountryShortName("IN")
                .setTimeZoneId("Asia/Calcutta")
                .setInviteCode("");

        accountClientController.register(
                new ScrubbedInput(did),
                new ScrubbedInput(deviceType),
                queueSupervisor.asJson(),
                httpServletResponse);

        UserProfileEntity queueSupervisorUserProfile = accountService.checkUserExistsByPhone("9118000000031");
        queueSupervisorUserProfile.setLevel(UserLevelEnum.Q_SUPERVISOR);
        accountService.save(queueSupervisorUserProfile);
        UserAccountEntity queueSupervisorUserAccount = accountService.changeAccountRolesToMatchUserLevel(
                queueSupervisorUserProfile.getQueueUserId(),
                queueSupervisorUserProfile.getLevel());
        accountService.save(queueSupervisorUserAccount);
    }

    private void addClients() throws IOException {
        Registration client1 = new Registration()
                .setPhone("+9118000000001")
                .setFirstName("ROCKET Docket")
                .setMail("rocketd@r.com")
                .setBirthday("2000-12-12")
                .setGender("M")
                .setCountryShortName("IN")
                .setTimeZoneId("Asia/Calcutta")
                .setInviteCode("");

        Registration client2 = new Registration()
                .setPhone("+9118000000002")
                .setFirstName("Pintoa D mAni")
                .setMail("pintod@r.com")
                .setBirthday("2000-12-12")
                .setGender("M")
                .setCountryShortName("IN")
                .setTimeZoneId("Asia/Calcutta")
                .setInviteCode("");

        accountClientController.register(
                new ScrubbedInput(did),
                new ScrubbedInput(deviceType),
                client1.asJson(),
                httpServletResponse);

        accountClientController.register(
                new ScrubbedInput(did),
                new ScrubbedInput(deviceType),
                client2.asJson(),
                httpServletResponse);
    }

    private void addSystemUsers() throws IOException {
        Registration admin = new Registration()
                .setPhone("+9118000000101")
                .setFirstName("Admin Admin")
                .setMail("admin@r.com")
                .setBirthday("2000-12-12")
                .setGender("M")
                .setCountryShortName("IN")
                .setTimeZoneId("Asia/Calcutta")
                .setInviteCode("");

        accountClientController.register(
                new ScrubbedInput(did),
                new ScrubbedInput(deviceType),
                admin.asJson(),
                httpServletResponse);

        UserProfileEntity adminUserProfile = accountService.checkUserExistsByPhone("9118000000101");
        adminUserProfile.setLevel(UserLevelEnum.ADMIN);
        accountService.save(adminUserProfile);
        UserAccountEntity adminUserAccount = accountService.changeAccountRolesToMatchUserLevel(
                adminUserProfile.getQueueUserId(),
                adminUserProfile.getLevel());
        accountService.save(adminUserAccount);

        Registration supervisor = new Registration()
                .setPhone("+9118000000102")
                .setFirstName("Supervisor Supervisor")
                .setMail("super@r.com")
                .setBirthday("2000-12-12")
                .setGender("M")
                .setCountryShortName("IN")
                .setTimeZoneId("Asia/Calcutta")
                .setInviteCode("");

        accountClientController.register(
                new ScrubbedInput(did),
                new ScrubbedInput(deviceType),
                supervisor.asJson(),
                httpServletResponse);

        UserProfileEntity supervisorUserProfile = accountService.checkUserExistsByPhone("9118000000102");
        supervisorUserProfile.setLevel(UserLevelEnum.SUPERVISOR);
        accountService.save(supervisorUserProfile);
        UserAccountEntity supervisorUserAccount = accountService.changeAccountRolesToMatchUserLevel(
                supervisorUserProfile.getQueueUserId(),
                supervisorUserProfile.getLevel());
        accountService.save(supervisorUserAccount);
    }

    private void createBusiness(String phone) {
        UserProfileEntity userProfile = accountService.checkUserExistsByPhone(phone);

        BizNameEntity bizName = BizNameEntity.newInstance(CommonUtil.generateCodeQR(mockEnvironment.getProperty("build.env")))
                .setBusinessName("Champ")
                .setBusinessTypes(Arrays.asList(BusinessTypeEnum.AT, BusinessTypeEnum.BA))
                .setPhone("9118000000000")
                .setPhoneRaw("18000000000")
                .setAddress("Shop NO RB.1, Haware's centurion Mall, 1st Floor, Sector No 19, Nerul - East, Seawoods, Navi Mumbai, Mumbai, 400706, India")
                .setTimeZone("Asia/Calcutta")
                .setInviteeCode(userProfile.getInviteCode())
                .setAddressOrigin(AddressOriginEnum.G)
                .setCountryShortName("IN")
                .setCoordinate(new double[] {73.022498, 19.0244723});
        bizService.saveName(bizName);

        BizStoreEntity bizStore = BizStoreEntity.newInstance()
                .setBizName(bizName)
                .setDisplayName("Food")
                .setBusinessType(BusinessTypeEnum.AT)
                .setPhone("9118000000000")
                .setPhoneRaw("18000000000")
                .setAddress("Shop NO RB.1, Haware's centurion Mall, 1st Floor, Sector No 19, Nerul - East, Seawoods, Navi Mumbai, Mumbai, 400706, India")
                .setTimeZone("Asia/Calcutta")
                .setCodeQR(ObjectId.get().toString())
                .setAddressOrigin(AddressOriginEnum.G)
                .setRemoteJoin(true)
                .setAllowLoggedInUser(false)
                .setAvailableTokenCount(0)
                .setAverageServiceTime(50000)
                .setCountryShortName("IN")
                .setCoordinate(new double[] {73.022498, 19.0244723});
        bizService.saveStore(bizStore);

        List<StoreHourEntity> storeHours = new LinkedList<>();
        for (int i = 1; i <= 7; i++) {
            StoreHourEntity storeHour = new StoreHourEntity(bizStore.getId(), DayOfWeek.of(i).getValue());
            storeHour.setStartHour(1)
                    .setTokenAvailableFrom(1)
                    .setTokenNotAvailableFrom(2359)
                    .setEndHour(2359);

            storeHours.add(storeHour);
        }

        /* Add store hours. */
        bizService.insertAll(storeHours);

        /* Create Queue. */
        tokenQueueService.createUpdate(bizStore.getCodeQR(), bizStore.getTopic(), bizStore.getDisplayName());

        /* Add Queue Admin and Queue Supervisor to Business and Store. */
        BusinessUserEntity businessUser = BusinessUserEntity.newInstance(
                userProfile.getQueueUserId(),
                UserLevelEnum.M_ADMIN
        );
        businessUser.setBusinessUserRegistrationStatus(BusinessUserRegistrationStatusEnum.V);
        businessUser.setValidateByQid(accountService.checkUserExistsByPhone("9118000000102").getQueueUserId());
        businessUserService.save(businessUser);

        UserProfileEntity queueSupervisorUserProfile = accountService.checkUserExistsByPhone("9118000000031");
        BusinessUserStoreEntity businessUserStore = new BusinessUserStoreEntity(
                queueSupervisorUserProfile.getQueueUserId(),
                bizStore.getId(),
                bizName.getId(),
                bizStore.getCodeQR());
        businessUserStoreService.save(businessUserStore);
    }
}
