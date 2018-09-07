package com.noqapp.mobile.view;

import com.noqapp.common.utils.CommonUtil;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BizNameEntity;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.BusinessUserEntity;
import com.noqapp.domain.BusinessUserStoreEntity;
import com.noqapp.domain.ProfessionalProfileEntity;
import com.noqapp.domain.StoreHourEntity;
import com.noqapp.domain.UserAccountEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.helper.NameDatePair;
import com.noqapp.domain.types.AddressOriginEnum;
import com.noqapp.domain.types.BusinessTypeEnum;
import com.noqapp.domain.types.BusinessUserRegistrationStatusEnum;
import com.noqapp.domain.types.DeviceTypeEnum;
import com.noqapp.domain.types.UserLevelEnum;
import com.noqapp.domain.types.catgeory.MedicalDepartmentEnum;
import com.noqapp.health.repository.ApiHealthNowManager;
import com.noqapp.health.repository.ApiHealthNowManagerImpl;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.medical.repository.MedicalMedicationManager;
import com.noqapp.medical.repository.MedicalMedicationManagerImpl;
import com.noqapp.medical.repository.MedicalMedicineManager;
import com.noqapp.medical.repository.MedicalMedicineManagerImpl;
import com.noqapp.medical.repository.MedicalPathologyManager;
import com.noqapp.medical.repository.MedicalPathologyManagerImpl;
import com.noqapp.medical.repository.MedicalPathologyTestManager;
import com.noqapp.medical.repository.MedicalPathologyTestManagerImpl;
import com.noqapp.medical.repository.MedicalPhysicalManager;
import com.noqapp.medical.repository.MedicalPhysicalManagerImpl;
import com.noqapp.medical.repository.MedicalRadiologyManager;
import com.noqapp.medical.repository.MedicalRadiologyManagerImpl;
import com.noqapp.medical.repository.MedicalRadiologyTestManager;
import com.noqapp.medical.repository.MedicalRadiologyTestManagerImpl;
import com.noqapp.medical.repository.MedicalRecordManager;
import com.noqapp.medical.repository.MedicalRecordManagerImpl;
import com.noqapp.medical.repository.UserMedicalProfileManager;
import com.noqapp.medical.repository.UserMedicalProfileManagerImpl;
import com.noqapp.medical.service.MedicalRecordService;
import com.noqapp.medical.service.UserMedicalProfileService;
import com.noqapp.mobile.domain.body.client.Registration;
import com.noqapp.mobile.service.AccountMobileService;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.service.DeviceService;
import com.noqapp.mobile.service.QueueMobileService;
import com.noqapp.mobile.service.StoreDetailService;
import com.noqapp.mobile.service.TokenQueueMobileService;
import com.noqapp.mobile.service.WebConnectorService;
import com.noqapp.mobile.view.controller.open.AccountClientController;
import com.noqapp.mobile.view.validator.AccountClientValidator;
import com.noqapp.repository.BizNameManager;
import com.noqapp.repository.BizNameManagerImpl;
import com.noqapp.repository.BizStoreManager;
import com.noqapp.repository.BizStoreManagerImpl;
import com.noqapp.repository.BusinessCustomerManager;
import com.noqapp.repository.BusinessCustomerManagerImpl;
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
import com.noqapp.repository.PreferredBusinessManager;
import com.noqapp.repository.PreferredBusinessManagerImpl;
import com.noqapp.repository.ProfessionalProfileManager;
import com.noqapp.repository.ProfessionalProfileManagerImpl;
import com.noqapp.repository.PurchaseOrderManager;
import com.noqapp.repository.PurchaseOrderManagerImpl;
import com.noqapp.repository.PurchaseProductOrderManager;
import com.noqapp.repository.PurchaseProductOrderManagerImpl;
import com.noqapp.repository.QueueManager;
import com.noqapp.repository.QueueManagerImpl;
import com.noqapp.repository.QueueManagerJDBC;
import com.noqapp.repository.RegisteredDeviceManager;
import com.noqapp.repository.RegisteredDeviceManagerImpl;
import com.noqapp.repository.S3FileManager;
import com.noqapp.repository.S3FileManagerImpl;
import com.noqapp.repository.StoreCategoryManager;
import com.noqapp.repository.StoreCategoryManagerImpl;
import com.noqapp.repository.StoreHourManager;
import com.noqapp.repository.StoreHourManagerImpl;
import com.noqapp.repository.StoreProductManager;
import com.noqapp.repository.StoreProductManagerImpl;
import com.noqapp.repository.TokenQueueManager;
import com.noqapp.repository.TokenQueueManagerImpl;
import com.noqapp.repository.UserAccountManager;
import com.noqapp.repository.UserAccountManagerImpl;
import com.noqapp.repository.UserAddressManager;
import com.noqapp.repository.UserAddressManagerImpl;
import com.noqapp.repository.UserAuthenticationManager;
import com.noqapp.repository.UserAuthenticationManagerImpl;
import com.noqapp.repository.UserPreferenceManager;
import com.noqapp.repository.UserPreferenceManagerImpl;
import com.noqapp.repository.UserProfileManager;
import com.noqapp.repository.UserProfileManagerImpl;
import com.noqapp.service.AccountService;
import com.noqapp.service.BizService;
import com.noqapp.service.BusinessCustomerService;
import com.noqapp.service.BusinessUserService;
import com.noqapp.service.BusinessUserStoreService;
import com.noqapp.service.EmailValidateService;
import com.noqapp.service.ExternalService;
import com.noqapp.service.FileService;
import com.noqapp.service.FirebaseMessageService;
import com.noqapp.service.FtpService;
import com.noqapp.service.GenerateUserIdService;
import com.noqapp.service.InviteService;
import com.noqapp.service.PreferredBusinessService;
import com.noqapp.service.ProfessionalProfileService;
import com.noqapp.service.PurchaseOrderService;
import com.noqapp.service.QueueService;
import com.noqapp.service.StoreCategoryService;
import com.noqapp.service.StoreProductService;
import com.noqapp.service.TokenQueueService;
import com.noqapp.service.UserAddressService;
import com.noqapp.service.UserProfilePreferenceService;

import org.bson.types.ObjectId;

import org.springframework.mock.env.MockEnvironment;

import org.junit.jupiter.api.BeforeAll;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

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
    protected ProfessionalProfileService professionalProfileService;

    protected UserAddressManager userAddressManager;
    protected UserAddressService userAddressService;
    protected StoreProductManager storeProductManager;
    protected StoreProductService storeProductService;
    protected PurchaseOrderManager purchaseOrderManager;
    protected PurchaseProductOrderManager purchaseProductOrderManager;
    protected PurchaseOrderService purchaseOrderService;
    protected FileService fileService;
    protected S3FileManager s3FileManager;

    protected MedicalRecordManager medicalRecordManager;
    protected MedicalPhysicalManager medicalPhysicalManager;
    protected MedicalMedicationManager medicalMedicationManager;
    protected MedicalMedicineManager medicalMedicineManager;
    protected MedicalPathologyManager medicalPathologyManager;
    protected MedicalPathologyTestManager medicalPathologyTestManager;
    protected MedicalRadiologyManager medicalRadiologyManager;
    protected MedicalRadiologyTestManager medicalRadiologyTestManager;
    protected MedicalRecordService medicalRecordService;

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
    protected BusinessUserStoreManager businessUserStoreManager;
    protected BusinessUserService businessUserService;
    protected BusinessUserManager businessUserManager;
    protected ProfessionalProfileManager professionalProfileManager;
    protected UserMedicalProfileManager userMedicalProfileManager;
    protected StoreCategoryManager storeCategoryManager;
    protected PreferredBusinessManager preferredBusinessManager;
    protected PreferredBusinessService preferredBusinessService;

    protected BusinessCustomerManager businessCustomerManager;
    protected BusinessCustomerService businessCustomerService;
    protected UserMedicalProfileService userMedicalProfileService;
    protected StoreCategoryService storeCategoryService;
    protected StoreDetailService storeDetailService;

    protected ApiHealthService apiHealthService;
    protected ApiHealthNowManager apiHealthNowManager;

    protected GenerateUserIdManager generateUserIdManager;

    private AccountClientController accountClientController;
    private MockEnvironment mockEnvironment;

    private WebConnectorService webConnectorService;
    @Mock protected ExternalService externalService;
    @Mock protected QueueManagerJDBC queueManagerJDBC;
    @Mock protected HttpServletResponse httpServletResponse;
    @Mock protected FtpService ftpService;

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
        userMedicalProfileManager = new UserMedicalProfileManagerImpl(getMongoTemplate());
        userMedicalProfileService = new UserMedicalProfileService(userMedicalProfileManager);

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

        webConnectorService = new WebConnectorService(
            "/webapi/mobile/get.htm",
            "could not connect to server",
            "webApiAccessToken",
            "8080",
            "http",
            "localhost"
        );

        professionalProfileManager = new ProfessionalProfileManagerImpl(getMongoTemplate());
        professionalProfileService = new ProfessionalProfileService(professionalProfileManager, userProfileManager);

        accountMobileService = new AccountMobileService(
            "/webapi/mobile/mail/accountSignup.htm",
            "/webapi/mobile/mail/mailChange.htm",
            webConnectorService,
            accountService,
            userMedicalProfileService,
            professionalProfileService
        );

        userProfilePreferenceService = new UserProfilePreferenceService(
            userProfileManager,
            userPreferenceManager
        );

        accountClientValidator = new AccountClientValidator(4, 5, 1, 2, 6, 6);
        deviceService = new DeviceService(registeredDeviceManager);

        apiHealthNowManager = new ApiHealthNowManagerImpl(getMongoTemplate());
        apiHealthService = new ApiHealthService(apiHealthNowManager);

        queueManager = new QueueManagerImpl(getMongoTemplate());
        tokenQueueManager = new TokenQueueManagerImpl(getMongoTemplate());
        bizStoreManager = new BizStoreManagerImpl(getMongoTemplate());
        storeHourManager = new StoreHourManagerImpl(getMongoTemplate());
        businessCustomerManager = new BusinessCustomerManagerImpl(getMongoTemplate());
        s3FileManager = new S3FileManagerImpl(getMongoTemplate());
        businessCustomerService = new BusinessCustomerService(
            businessCustomerManager,
            bizStoreManager,
            userProfileManager,
            queueManager
        );

        tokenQueueService = new TokenQueueService(
            tokenQueueManager,
            firebaseMessageService,
            queueManager,
            accountService,
            registeredDeviceManager,
            queueManagerJDBC,
            storeHourManager,
            bizStoreManager,
            businessCustomerService,
            apiHealthService
        );

        storeProductManager = new StoreProductManagerImpl(getMongoTemplate());
        storeProductService = new StoreProductService(storeProductManager);
        purchaseOrderManager = new PurchaseOrderManagerImpl(getMongoTemplate());
        purchaseProductOrderManager = new PurchaseProductOrderManagerImpl(getMongoTemplate());

        userAddressManager = new UserAddressManagerImpl(5, getMongoTemplate());
        userAddressService = new UserAddressService(userAddressManager, externalService);

        purchaseOrderService = new PurchaseOrderService(
            bizStoreManager,
            tokenQueueService,
            storeHourManager,
            storeProductService,
            purchaseOrderManager,
            purchaseProductOrderManager,
            userAddressService,
            firebaseMessageService,
            registeredDeviceManager,
            tokenQueueManager,
            accountService
        );

        bizNameManager = new BizNameManagerImpl(getMongoTemplate());
        businessUserStoreManager = new BusinessUserStoreManagerImpl(getMongoTemplate());

        queueService = new QueueService(
            30,
            accountService,
            queueManager,
            queueManagerJDBC,
            tokenQueueService,
            businessUserStoreManager
        );

        bizService = new BizService(
            69.172,
            111.321,
            bizNameManager,
            bizStoreManager,
            storeHourManager,
            tokenQueueService,
            queueService,
            businessUserStoreManager
        );

        fileService = new FileService(
            192, 192, 300, 150,
            accountService,
            ftpService,
            s3FileManager,
            bizNameManager,
            bizStoreManager,
            storeProductManager
        );

        storeCategoryManager = new StoreCategoryManagerImpl(getMongoTemplate());
        storeCategoryService = new StoreCategoryService(storeCategoryManager, storeProductManager);

        tokenQueueMobileService = new TokenQueueMobileService(
            tokenQueueService,
            bizService,
            tokenQueueManager,
            queueManager,
            professionalProfileService,
            userProfileManager,
            businessUserStoreManager);

        storeDetailService = new StoreDetailService(bizService, tokenQueueMobileService, storeProductService, storeCategoryService);

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
            accountClientValidator,
            deviceService
        );

        authenticateMobileService = new AuthenticateMobileService(
            userAccountManager
        );

        preferredBusinessManager = new PreferredBusinessManagerImpl(getMongoTemplate());
        preferredBusinessService = new PreferredBusinessService(preferredBusinessManager, bizStoreManager);

        businessUserManager = new BusinessUserManagerImpl(getMongoTemplate());
        businessUserService = new BusinessUserService(businessUserManager);
        businessUserStoreService = new BusinessUserStoreService(
            10,
            businessUserStoreManager,
            preferredBusinessService,
            businessUserService,
            tokenQueueService,
            accountService,
            bizService
        );

        medicalRecordManager = new MedicalRecordManagerImpl(getMongoTemplate());
        medicalPhysicalManager = new MedicalPhysicalManagerImpl(getMongoTemplate());
        medicalMedicationManager = new MedicalMedicationManagerImpl(getMongoTemplate());
        medicalMedicineManager = new MedicalMedicineManagerImpl(getMongoTemplate());
        medicalPathologyManager = new MedicalPathologyManagerImpl(getMongoTemplate());
        medicalPathologyTestManager = new MedicalPathologyTestManagerImpl(getMongoTemplate());
        medicalRadiologyManager = new MedicalRadiologyManagerImpl(getMongoTemplate());
        medicalRadiologyTestManager = new MedicalRadiologyTestManagerImpl(getMongoTemplate());
        medicalRecordService = new MedicalRecordService(
            medicalRecordManager,
            medicalPhysicalManager,
            medicalMedicationManager,
            medicalMedicineManager,
            medicalPathologyManager,
            medicalPathologyTestManager,
            medicalRadiologyManager,
            medicalRadiologyTestManager,
            bizService,
            businessUserStoreService,
            userProfileManager,
            purchaseOrderService
        );

        registerUser();
        createBusinessDoctor("9118000000030");
        createBusinessPharmacy("9118000000060");
    }

    private void registerUser() throws IOException {
        /* System Users. Like Admin, Supervisor. */
        addSystemUsers();

        /* Clients. */
        addClients();

        /* Store Admin and Queue Supervisors. */
        addStoreUsersToDoctor();
        addStoreUsersToPharmacy();
    }

    private void addStoreUsersToDoctor() {
        Registration queueAdmin = new Registration()
            .setPhone("+9118000000030")
            .setFirstName("Diktaa D mA")
            .setMail("diktad@r.com")
            .setPassword("password")
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
        merchantUserProfile
            .setLevel(UserLevelEnum.M_ADMIN)
            .setBusinessType(BusinessTypeEnum.DO);
        accountService.save(merchantUserProfile);
        UserAccountEntity merchantUserAccount = accountService.changeAccountRolesToMatchUserLevel(
            merchantUserProfile.getQueueUserId(),
            merchantUserProfile.getLevel());
        accountService.save(merchantUserAccount);

        Registration queueSupervisor = new Registration()
            .setPhone("+9118000000031")
            .setFirstName("Fiktaa D mAn")
            .setMail("fiktad@r.com")
            .setPassword("password")
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
        queueSupervisorUserProfile
            .setLevel(UserLevelEnum.Q_SUPERVISOR)
            .setBusinessType(BusinessTypeEnum.DO);
        accountService.save(queueSupervisorUserProfile);
        UserAccountEntity queueSupervisorUserAccount = accountService.changeAccountRolesToMatchUserLevel(
            queueSupervisorUserProfile.getQueueUserId(),
            queueSupervisorUserProfile.getLevel());
        accountService.save(queueSupervisorUserAccount);

        Registration queueManager = new Registration()
            .setPhone("+9118000000032")
            .setFirstName("Manager Doctor")
            .setMail("manager_doctor@r.com")
            .setPassword("password")
            .setBirthday("2000-12-12")
            .setGender("F")
            .setCountryShortName("IN")
            .setTimeZoneId("Asia/Calcutta")
            .setInviteCode("");

        accountClientController.register(
            new ScrubbedInput(did),
            new ScrubbedInput(deviceType),
            queueManager.asJson(),
            httpServletResponse);

        UserProfileEntity storeManagerUserProfile = accountService.checkUserExistsByPhone("9118000000032");
        storeManagerUserProfile
            .setLevel(UserLevelEnum.S_MANAGER)
            .setBusinessType(BusinessTypeEnum.DO);
        accountService.save(storeManagerUserProfile);
        UserAccountEntity storeManagerUserAccount = accountService.changeAccountRolesToMatchUserLevel(
            storeManagerUserProfile.getQueueUserId(),
            storeManagerUserProfile.getLevel());
        accountService.save(storeManagerUserAccount);
    }

    private void addStoreUsersToPharmacy() {
        Registration queueAdmin = new Registration()
            .setPhone("+9118000000060")
            .setFirstName("Pharmacy Business")
            .setMail("pharmacy_business@r.com")
            .setPassword("password")
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

        UserProfileEntity merchantUserProfile = accountService.checkUserExistsByPhone("9118000000060");
        merchantUserProfile.setLevel(UserLevelEnum.M_ADMIN);
        accountService.save(merchantUserProfile);
        UserAccountEntity merchantUserAccount = accountService.changeAccountRolesToMatchUserLevel(
            merchantUserProfile.getQueueUserId(),
            merchantUserProfile.getLevel());
        accountService.save(merchantUserAccount);

        Registration queueSupervisor = new Registration()
            .setPhone("+9118000000061")
            .setFirstName("Pharmacy Store")
            .setMail("pharmacy_store_supervisor@r.com")
            .setPassword("password")
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

        UserProfileEntity queueSupervisorUserProfile = accountService.checkUserExistsByPhone("9118000000061");
        queueSupervisorUserProfile.setLevel(UserLevelEnum.Q_SUPERVISOR);
        accountService.save(queueSupervisorUserProfile);
        UserAccountEntity queueSupervisorUserAccount = accountService.changeAccountRolesToMatchUserLevel(
            queueSupervisorUserProfile.getQueueUserId(),
            queueSupervisorUserProfile.getLevel());
        accountService.save(queueSupervisorUserAccount);

        Registration queueManager = new Registration()
            .setPhone("+9118000000062")
            .setFirstName("Manager of Pharmacy")
            .setMail("manager_pharmacy@r.com")
            .setPassword("password")
            .setBirthday("2000-12-12")
            .setGender("F")
            .setCountryShortName("IN")
            .setTimeZoneId("Asia/Calcutta")
            .setInviteCode("");

        accountClientController.register(
            new ScrubbedInput(did),
            new ScrubbedInput(deviceType),
            queueManager.asJson(),
            httpServletResponse);

        UserProfileEntity storeManagerUserProfile = accountService.checkUserExistsByPhone("9118000000062");
        storeManagerUserProfile.setLevel(UserLevelEnum.S_MANAGER);
        accountService.save(storeManagerUserProfile);
        UserAccountEntity storeManagerUserAccount = accountService.changeAccountRolesToMatchUserLevel(
            storeManagerUserProfile.getQueueUserId(),
            storeManagerUserProfile.getLevel());
        accountService.save(storeManagerUserAccount);
    }

    private void addClients() {
        Registration client1 = new Registration()
            .setPhone("+9118000000001")
            .setFirstName("ROCKET Docket")
            .setMail("rocketd@r.com")
            .setPassword("password")
            .setBirthday("2000-12-12")
            .setGender("M")
            .setCountryShortName("IN")
            .setTimeZoneId("Asia/Calcutta")
            .setInviteCode("");

        Registration client2 = new Registration()
            .setPhone("+9118000000002")
            .setFirstName("Pintoa D mAni")
            .setMail("pintod@r.com")
            .setPassword("password")
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

    private void addSystemUsers() {
        Registration admin = new Registration()
            .setPhone("+9118000000101")
            .setFirstName("Admin Admin")
            .setMail("admin@r.com")
            .setPassword("password")
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
            .setPassword("password")
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

    private void createBusinessDoctor(String phone) {
        UserProfileEntity userProfile = accountService.checkUserExistsByPhone(phone);

        BizNameEntity bizName = BizNameEntity.newInstance(CommonUtil.generateCodeQR(mockEnvironment.getProperty("build.env")))
            .setBusinessName("Champ")
            .setBusinessType(BusinessTypeEnum.DO)
            .setPhone("9118000000000")
            .setPhoneRaw("18000000000")
            .setAddress("Shop NO RB.1, Haware's centurion Mall, 1st Floor, Sector No 19, Nerul - East, Seawoods, Navi Mumbai, Mumbai, 400706, India")
            .setTown("Vashi")
            .setStateShortName("MH")
            .setTimeZone("Asia/Calcutta")
            .setInviteeCode(userProfile.getInviteCode())
            .setAddressOrigin(AddressOriginEnum.G)
            .setCountryShortName("IN")
            .setCoordinate(new double[]{73.022498, 19.0244723});
        String webLocation = bizService.buildWebLocationForBiz(
            bizName.getTown(),
            bizName.getStateShortName(),
            bizName.getCountryShortName(),
            bizName.getBusinessName(),
            bizName.getId());

        bizName.setWebLocation(webLocation);
        bizName.setCodeQR(CommonUtil.generateCodeQR(mockEnvironment.getProperty("build.env")));
        bizService.saveName(bizName);

        BizStoreEntity bizStore = BizStoreEntity.newInstance()
            .setBizName(bizName)
            .setDisplayName("Dr Aaj Kal")
            .setBusinessType(bizName.getBusinessType())
            .setBizCategoryId(MedicalDepartmentEnum.CRD.getName())
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
            .setCoordinate(new double[]{73.022498, 19.0244723});
        bizStore.setWebLocation(webLocation);
        bizStore.setCodeQR(CommonUtil.generateCodeQR(mockEnvironment.getProperty("build.env")));
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
        tokenQueueService.createUpdate(bizStore.getCodeQR(), bizStore.getTopic(), bizStore.getDisplayName(), bizStore.getBusinessType());

        /* Add Queue Admin, Queue Supervisor, Queue Manager to Business and Store. */
        BusinessUserEntity businessUser = BusinessUserEntity.newInstance(
            userProfile.getQueueUserId(),
            UserLevelEnum.M_ADMIN
        );
        businessUser.setBusinessUserRegistrationStatus(BusinessUserRegistrationStatusEnum.V)
            .setValidateByQid(accountService.checkUserExistsByPhone("9118000000102").getQueueUserId())
            .setBizName(bizName);
        businessUserService.save(businessUser);

        UserProfileEntity queueSupervisorUserProfile = accountService.checkUserExistsByPhone("9118000000031");
        BusinessUserStoreEntity businessUserStore = new BusinessUserStoreEntity(
            queueSupervisorUserProfile.getQueueUserId(),
            bizStore.getId(),
            bizName.getId(),
            bizStore.getCodeQR(),
            queueSupervisorUserProfile.getLevel());
        businessUserStoreService.save(businessUserStore);

        UserProfileEntity queueManagerUserProfile = accountService.checkUserExistsByPhone("9118000000032");
        businessUserStore = new BusinessUserStoreEntity(
            queueManagerUserProfile.getQueueUserId(),
            bizStore.getId(),
            bizName.getId(),
            bizStore.getCodeQR(),
            queueManagerUserProfile.getLevel());
        businessUserStoreService.save(businessUserStore);
        professionalProfileService.createProfessionalProfile(queueManagerUserProfile.getQueueUserId());
        ProfessionalProfileEntity professionalProfile = professionalProfileService.findByQid(queueManagerUserProfile.getQueueUserId());
        NameDatePair nameDatePair1 = new NameDatePair().setName("MBBS").setMonthYear("1985-01-22");
        List<NameDatePair> nameDatePairs = new ArrayList<NameDatePair>() {{
            add(nameDatePair1);
        }};
        professionalProfile.setEducation(nameDatePairs).setAboutMe("About Me");
        professionalProfileService.save(professionalProfile);
    }

    private void createBusinessPharmacy(String phone) {
        UserProfileEntity userProfile = accountService.checkUserExistsByPhone(phone);

        BizNameEntity bizName = BizNameEntity.newInstance(CommonUtil.generateCodeQR(mockEnvironment.getProperty("build.env")))
            .setBusinessName("Pharmacy Business")
            .setBusinessType(BusinessTypeEnum.PH)
            .setPhone("9118000000011")
            .setPhoneRaw("18000000011")
            .setAddress("Shop No 10 Plot No 102 Sector 29, Vashi, Navi Mumbai, Maharashtra 400703")
            .setTown("Vashi")
            .setStateShortName("MH")
            .setTimeZone("Asia/Calcutta")
            .setInviteeCode(userProfile.getInviteCode())
            .setAddressOrigin(AddressOriginEnum.G)
            .setCountryShortName("IN")
            .setCoordinate(new double[]{71.022498, 18.0244723});
        String webLocation = bizService.buildWebLocationForBiz(
            bizName.getTown(),
            bizName.getStateShortName(),
            bizName.getCountryShortName(),
            bizName.getBusinessName(),
            bizName.getId());

        bizName.setWebLocation(webLocation);
        bizName.setCodeQR(CommonUtil.generateCodeQR(mockEnvironment.getProperty("build.env")));
        bizService.saveName(bizName);

        BizStoreEntity bizStore = BizStoreEntity.newInstance()
            .setBizName(bizName)
            .setDisplayName("Mera Pharmacy")
            .setBusinessType(bizName.getBusinessType())
            .setBizCategoryId(null)
            .setPhone("9118000000012")
            .setPhoneRaw("18000000012")
            .setAddress("Shop No 10 Plot No 102 Sector 29, Vashi, Navi Mumbai, Maharashtra 400703")
            .setTimeZone("Asia/Calcutta")
            .setCodeQR(ObjectId.get().toString())
            .setAddressOrigin(AddressOriginEnum.G)
            .setRemoteJoin(true)
            .setAllowLoggedInUser(false)
            .setAvailableTokenCount(0)
            .setAverageServiceTime(50000)
            .setCountryShortName("IN")
            .setCoordinate(new double[]{73.022498, 19.0244723});
        bizStore.setWebLocation(webLocation);
        bizStore.setCodeQR(CommonUtil.generateCodeQR(mockEnvironment.getProperty("build.env")));
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
        tokenQueueService.createUpdate(bizStore.getCodeQR(), bizStore.getTopic(), bizStore.getDisplayName(), bizStore.getBusinessType());

        /* Add Queue Admin, Queue Supervisor, Queue Manager to Business and Store. */
        BusinessUserEntity businessUser = BusinessUserEntity.newInstance(
            userProfile.getQueueUserId(),
            UserLevelEnum.M_ADMIN
        );
        businessUser.setBusinessUserRegistrationStatus(BusinessUserRegistrationStatusEnum.V)
            .setValidateByQid(accountService.checkUserExistsByPhone("9118000000060").getQueueUserId())
            .setBizName(bizName);
        businessUserService.save(businessUser);

        UserProfileEntity queueSupervisorUserProfile = accountService.checkUserExistsByPhone("9118000000061");
        BusinessUserStoreEntity businessUserStore = new BusinessUserStoreEntity(
            queueSupervisorUserProfile.getQueueUserId(),
            bizStore.getId(),
            bizName.getId(),
            bizStore.getCodeQR(),
            queueSupervisorUserProfile.getLevel());
        businessUserStoreService.save(businessUserStore);

        UserProfileEntity queueManagerUserProfile = accountService.checkUserExistsByPhone("9118000000062");
        businessUserStore = new BusinessUserStoreEntity(
            queueManagerUserProfile.getQueueUserId(),
            bizStore.getId(),
            bizName.getId(),
            bizStore.getCodeQR(),
            queueManagerUserProfile.getLevel());
        businessUserStoreService.save(businessUserStore);
    }
}
