package com.noqapp.mobile.view;

import com.noqapp.common.config.FirebaseConfig;
import com.noqapp.common.config.TextToSpeechConfiguration;
import com.noqapp.common.utils.CommonUtil;
import com.noqapp.common.utils.Constants;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BizNameEntity;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.BusinessUserEntity;
import com.noqapp.domain.BusinessUserStoreEntity;
import com.noqapp.domain.ProfessionalProfileEntity;
import com.noqapp.domain.StoreCategoryEntity;
import com.noqapp.domain.StoreHourEntity;
import com.noqapp.domain.StoreProductEntity;
import com.noqapp.domain.UserAccountEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.helper.NameDatePair;
import com.noqapp.domain.site.QueueUser;
import com.noqapp.domain.types.AddressOriginEnum;
import com.noqapp.domain.types.BusinessTypeEnum;
import com.noqapp.domain.types.BusinessUserRegistrationStatusEnum;
import com.noqapp.domain.types.DeviceTypeEnum;
import com.noqapp.domain.types.GenderEnum;
import com.noqapp.domain.types.OnOffEnum;
import com.noqapp.domain.types.ProductTypeEnum;
import com.noqapp.domain.types.RoleEnum;
import com.noqapp.domain.types.TaxEnum;
import com.noqapp.domain.types.UnitOfMeasurementEnum;
import com.noqapp.domain.types.UserLevelEnum;
import com.noqapp.domain.types.catgeory.CanteenStoreDepartmentEnum;
import com.noqapp.domain.types.catgeory.HealthCareServiceEnum;
import com.noqapp.domain.types.catgeory.MedicalDepartmentEnum;
import com.noqapp.health.repository.ApiHealthNowManager;
import com.noqapp.health.repository.ApiHealthNowManagerImpl;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.medical.repository.HospitalVisitScheduleManager;
import com.noqapp.medical.repository.HospitalVisitScheduleManagerImpl;
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
import com.noqapp.medical.repository.UserMedicalProfileHistoryManager;
import com.noqapp.medical.repository.UserMedicalProfileManager;
import com.noqapp.medical.repository.UserMedicalProfileManagerImpl;
import com.noqapp.medical.service.HospitalVisitScheduleService;
import com.noqapp.medical.service.MedicalFileService;
import com.noqapp.medical.service.MedicalRecordService;
import com.noqapp.medical.service.UserMedicalProfileService;
import com.noqapp.mobile.domain.body.client.Registration;
import com.noqapp.mobile.service.AccountMobileService;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.service.DeviceRegistrationService;
import com.noqapp.mobile.service.JMSProducerMobileService;
import com.noqapp.mobile.service.MedicalRecordMobileService;
import com.noqapp.mobile.service.MerchantExtendingJoinService;
import com.noqapp.mobile.service.PurchaseOrderMobileService;
import com.noqapp.mobile.service.QueueMobileService;
import com.noqapp.mobile.service.StoreDetailService;
import com.noqapp.mobile.service.TokenQueueMobileService;
import com.noqapp.mobile.view.controller.open.AccountClientController;
import com.noqapp.mobile.view.validator.AccountClientValidator;
import com.noqapp.repository.AdvertisementManager;
import com.noqapp.repository.AdvertisementManagerImpl;
import com.noqapp.repository.BizNameManager;
import com.noqapp.repository.BizNameManagerImpl;
import com.noqapp.repository.BizStoreManager;
import com.noqapp.repository.BizStoreManagerImpl;
import com.noqapp.repository.BusinessCustomerManager;
import com.noqapp.repository.BusinessCustomerManagerImpl;
import com.noqapp.repository.BusinessCustomerPriorityManager;
import com.noqapp.repository.BusinessCustomerPriorityManagerImpl;
import com.noqapp.repository.BusinessUserManager;
import com.noqapp.repository.BusinessUserManagerImpl;
import com.noqapp.repository.BusinessUserStoreManager;
import com.noqapp.repository.BusinessUserStoreManagerImpl;
import com.noqapp.repository.CouponManager;
import com.noqapp.repository.CouponManagerImpl;
import com.noqapp.repository.CustomTextToSpeechManager;
import com.noqapp.repository.EmailValidateManager;
import com.noqapp.repository.EmailValidateManagerImpl;
import com.noqapp.repository.ForgotRecoverManager;
import com.noqapp.repository.ForgotRecoverManagerImpl;
import com.noqapp.repository.GenerateUserIdManager;
import com.noqapp.repository.GenerateUserIdManagerImpl;
import com.noqapp.repository.NotificationMessageManager;
import com.noqapp.repository.NotificationMessageManagerImpl;
import com.noqapp.repository.PointEarnedManager;
import com.noqapp.repository.PointEarnedManagerImpl;
import com.noqapp.repository.PreferredBusinessManager;
import com.noqapp.repository.PreferredBusinessManagerImpl;
import com.noqapp.repository.ProfessionalProfileManager;
import com.noqapp.repository.ProfessionalProfileManagerImpl;
import com.noqapp.repository.PublishArticleManager;
import com.noqapp.repository.PublishArticleManagerImpl;
import com.noqapp.repository.PurchaseOrderManager;
import com.noqapp.repository.PurchaseOrderManagerImpl;
import com.noqapp.repository.PurchaseOrderManagerJDBC;
import com.noqapp.repository.PurchaseOrderProductManager;
import com.noqapp.repository.PurchaseOrderProductManagerImpl;
import com.noqapp.repository.PurchaseOrderProductManagerJDBC;
import com.noqapp.repository.QueueManager;
import com.noqapp.repository.QueueManagerImpl;
import com.noqapp.repository.QueueManagerJDBC;
import com.noqapp.repository.RegisteredDeviceManager;
import com.noqapp.repository.RegisteredDeviceManagerImpl;
import com.noqapp.repository.S3FileManager;
import com.noqapp.repository.S3FileManagerImpl;
import com.noqapp.repository.ScheduleAppointmentManager;
import com.noqapp.repository.ScheduleAppointmentManagerImpl;
import com.noqapp.repository.ScheduledTaskManager;
import com.noqapp.repository.ScheduledTaskManagerImpl;
import com.noqapp.repository.StatsBizStoreDailyManager;
import com.noqapp.repository.StatsBizStoreDailyManagerImpl;
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
import com.noqapp.repository.market.HouseholdItemManager;
import com.noqapp.repository.market.HouseholdItemManagerImpl;
import com.noqapp.repository.market.PropertyRentalManager;
import com.noqapp.repository.market.PropertyRentalManagerImpl;
import com.noqapp.repository.neo4j.AnomalyN4jManager;
import com.noqapp.repository.neo4j.BusinessCustomerN4jManager;
import com.noqapp.repository.neo4j.NotificationN4jManager;
import com.noqapp.repository.neo4j.PersonN4jManager;
import com.noqapp.search.elastic.config.ElasticsearchClientConfiguration;
import com.noqapp.search.elastic.domain.BizStoreElastic;
import com.noqapp.search.elastic.repository.BizStoreElasticManager;
import com.noqapp.search.elastic.repository.BizStoreElasticManagerImpl;
import com.noqapp.search.elastic.repository.BizStoreSpatialElasticManager;
import com.noqapp.search.elastic.repository.BizStoreSpatialElasticManagerImpl;
import com.noqapp.search.elastic.service.BizStoreElasticService;
import com.noqapp.search.elastic.service.BizStoreSpatialElasticService;
import com.noqapp.search.elastic.service.ElasticAdministrationService;
import com.noqapp.search.elastic.service.GeoIPLocationService;
import com.noqapp.service.AccountService;
import com.noqapp.service.BizService;
import com.noqapp.service.BusinessCustomerPriorityService;
import com.noqapp.service.BusinessCustomerService;
import com.noqapp.service.BusinessUserService;
import com.noqapp.service.BusinessUserStoreService;
import com.noqapp.service.CouponService;
import com.noqapp.service.CustomTextToSpeechService;
import com.noqapp.service.DeviceService;
import com.noqapp.service.EmailValidateService;
import com.noqapp.service.ExternalService;
import com.noqapp.service.FileService;
import com.noqapp.service.FirebaseMessageService;
import com.noqapp.service.FirebaseService;
import com.noqapp.service.FtpService;
import com.noqapp.service.GenerateUserIdService;
import com.noqapp.service.JMSProducerService;
import com.noqapp.service.JoinAbortService;
import com.noqapp.service.LanguageTranslationService;
import com.noqapp.service.MailService;
import com.noqapp.service.MessageCustomerService;
import com.noqapp.service.NotifyMobileService;
import com.noqapp.service.PreferredBusinessService;
import com.noqapp.service.ProfessionalProfileService;
import com.noqapp.service.PurchaseOrderProductService;
import com.noqapp.service.PurchaseOrderService;
import com.noqapp.service.QueueService;
import com.noqapp.service.ReviewService;
import com.noqapp.service.ScheduleAppointmentService;
import com.noqapp.service.SmsService;
import com.noqapp.service.StoreCategoryService;
import com.noqapp.service.StoreHourService;
import com.noqapp.service.StoreProductService;
import com.noqapp.service.SubscribeTopicService;
import com.noqapp.service.TextToSpeechService;
import com.noqapp.service.TokenQueueService;
import com.noqapp.service.UserAddressService;
import com.noqapp.service.UserProfilePreferenceService;
import com.noqapp.service.graph.GraphBusinessCustomer;
import com.noqapp.service.graph.GraphDetailOfPerson;
import com.noqapp.service.graph.GraphQueue;
import com.noqapp.service.nlp.NLPService;
import com.noqapp.service.payment.CashfreeService;
import com.noqapp.service.transaction.TransactionService;

import com.maxmind.geoip2.DatabaseReader;

import org.bson.types.ObjectId;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import org.junit.jupiter.api.BeforeAll;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import okhttp3.OkHttpClient;

import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
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
    protected String model;
    protected String osVersion;
    protected String appVersion;
    protected String deviceType;

    protected AccountService accountService;
    protected AccountMobileService accountMobileService;
    protected UserProfilePreferenceService userProfilePreferenceService;
    protected AccountClientValidator accountClientValidator;
    protected DeviceRegistrationService deviceRegistrationService;
    protected TokenQueueMobileService tokenQueueMobileService;
    protected TokenQueueService tokenQueueService;
    protected BizService bizService;
    protected QueueMobileService queueMobileService;
    protected QueueService queueService;
    protected AuthenticateMobileService authenticateMobileService;
    protected BusinessUserStoreService businessUserStoreService;
    protected ProfessionalProfileService professionalProfileService;
    protected ScheduleAppointmentService scheduleAppointmentService;
    protected NotifyMobileService notifyMobileService;

    protected UserAddressManager userAddressManager;
    protected UserAddressService userAddressService;
    protected StoreProductManager storeProductManager;
    protected StoreProductService storeProductService;
    protected PurchaseOrderManager purchaseOrderManager;
    protected PurchaseOrderProductManager purchaseOrderProductManager;
    protected PurchaseOrderService purchaseOrderService;
    protected PurchaseOrderProductService purchaseOrderProductService;
    protected PurchaseOrderMobileService purchaseOrderMobileService;
    protected FileService fileService;
    protected S3FileManager s3FileManager;
    protected ReviewService reviewService;
    protected CouponService couponService;
    protected MerchantExtendingJoinService merchantExtendingJoinService;
    protected JoinAbortService joinAbortService;
    protected DeviceService deviceService;
    protected TextToSpeechService textToSpeechService;
    protected CustomTextToSpeechService customTextToSpeechService;

    protected MedicalRecordManager medicalRecordManager;
    protected MedicalPhysicalManager medicalPhysicalManager;
    protected MedicalMedicationManager medicalMedicationManager;
    protected MedicalMedicineManager medicalMedicineManager;
    protected MedicalPathologyManager medicalPathologyManager;
    protected MedicalPathologyTestManager medicalPathologyTestManager;
    protected MedicalRadiologyManager medicalRadiologyManager;
    protected MedicalRadiologyTestManager medicalRadiologyTestManager;
    protected HospitalVisitScheduleManager hospitalVisitScheduleManager;
    protected MedicalRecordService medicalRecordService;
    protected MedicalFileService medicalFileService;
    protected MessageCustomerService messageCustomerService;

    protected TokenQueueManager tokenQueueManager;
    protected FirebaseMessageService firebaseMessageService;
    protected FirebaseService firebaseService;
    protected QueueManager queueManager;

    protected UserAccountManager userAccountManager;
    protected UserAuthenticationManager userAuthenticationManager;
    protected UserPreferenceManager userPreferenceManager;
    protected UserProfileManager userProfileManager;
    protected GenerateUserIdService generateUserIdService;
    protected EmailValidateManager emailValidateManager;
    protected EmailValidateService emailValidateService;
    protected ForgotRecoverManager forgotRecoverManager;
    protected RegisteredDeviceManager registeredDeviceManager;
    protected NotificationMessageManager notificationMessageManager;
    protected BizNameManager bizNameManager;
    protected BusinessCustomerPriorityManager businessCustomerPriorityManager;
    protected BizStoreManager bizStoreManager;
    protected StoreHourManager storeHourManager;
    protected BusinessUserStoreManager businessUserStoreManager;
    protected BusinessUserService businessUserService;
    protected BusinessUserManager businessUserManager;
    protected ProfessionalProfileManager professionalProfileManager;
    protected UserMedicalProfileManager userMedicalProfileManager;
    protected UserMedicalProfileHistoryManager userMedicalProfileHistoryManager;
    protected StoreCategoryManager storeCategoryManager;
    protected PreferredBusinessManager preferredBusinessManager;
    protected PreferredBusinessService preferredBusinessService;
    protected ScheduledTaskManager scheduledTaskManager;
    protected PublishArticleManager publishArticleManager;
    protected AdvertisementManager advertisementManager;
    protected PropertyRentalManager propertyRentalManager;
    protected HouseholdItemManager householdItemManager;
    protected ScheduleAppointmentManager scheduleAppointmentManager;
    protected PointEarnedManager pointEarnedManager;
    protected CouponManager couponManager;
    protected CustomTextToSpeechManager customTextToSpeechManager;

    protected BusinessCustomerManager businessCustomerManager;
    protected BusinessCustomerService businessCustomerService;
    protected UserMedicalProfileService userMedicalProfileService;
    protected StoreCategoryService storeCategoryService;
    protected StoreDetailService storeDetailService;
    protected BizStoreElasticManager bizStoreElasticManager;
    protected BizStoreSpatialElasticManager<BizStoreElastic> bizStoreSpatialElasticManager;
    protected BizStoreElasticService bizStoreElasticService;
    protected BizStoreSpatialElasticService bizStoreSpatialElasticService;
    protected TransactionService transactionService;
    protected StoreHourService storeHourService;
    protected MedicalRecordMobileService medicalRecordMobileService;
    protected HospitalVisitScheduleService hospitalVisitScheduleService;
    protected NLPService nlpService;
    protected BusinessCustomerPriorityService businessCustomerPriorityService;

    protected ApiHealthService apiHealthService;
    protected ApiHealthNowManager apiHealthNowManager;
    protected StatsBizStoreDailyManager statsBizStoreDailyManager;

    protected GenerateUserIdManager generateUserIdManager;
    private AccountClientController accountClientController;

    protected GeoIPLocationService geoIPLocationService;

    @Mock protected PersonN4jManager personN4jManager;
    @Mock protected BusinessCustomerN4jManager businessCustomerN4jManager;
    @Mock protected AnomalyN4jManager anomalyN4jManager;
    @Mock protected NotificationN4jManager notificationN4jManager;

    @Mock protected GraphQueue graphQueue;
    @Mock protected GraphBusinessCustomer graphBusinessCustomer;

    protected GraphDetailOfPerson graphDetailOfPerson;

    private StanfordCoreNLP stanfordCoreNLP;
    private MaxentTagger maxentTagger;
    protected ExternalService externalService;
    @Mock protected QueueManagerJDBC queueManagerJDBC;
    @Mock protected PurchaseOrderManagerJDBC purchaseOrderManagerJDBC;
    @Mock protected PurchaseOrderProductManagerJDBC purchaseOrderProductManagerJDBC;
    @Mock protected HttpServletResponse httpServletResponse;
    @Mock protected HttpServletRequest httpServletRequest;
    @Mock protected FtpService ftpService;
    @Mock protected MailService mailService;
    @Mock protected ElasticsearchClientConfiguration elasticsearchClientConfiguration;
    @Mock protected ElasticAdministrationService elasticAdministrationService;
    @Mock protected OkHttpClient okHttpClient;
    @Mock protected CashfreeService cashfreeService;
    @Mock protected FirebaseConfig firebaseConfig;
    @Mock protected TextToSpeechConfiguration textToSpeechConfiguration;
    @Mock protected SmsService smsService;
    @Mock protected JMSProducerMobileService jmsProducerMobileService;
    @Mock protected JMSProducerService jmsProducerService;;
    @Mock private SubscribeTopicService subscribeTopicService;
    @Mock protected LanguageTranslationService languageTranslationService;
    @Mock protected StringRedisTemplate stringRedisTemplate;

    private MockEnvironment mockEnvironment;

    @BeforeAll
    public void globalISetup() throws IOException {
        MockitoAnnotations.openMocks(this);

        did = UUID.randomUUID().toString();
        didClient1 = UUID.randomUUID().toString();
        didClient2 = UUID.randomUUID().toString();
        didQueueSupervisor = UUID.randomUUID().toString();
        mockEnvironment = new MockEnvironment();
        mockEnvironment.setProperty("build.env", "sandbox");

        Mockito.when(httpServletRequest.getRemoteAddr()).thenReturn("223.165.26.161");
        File database = new ClassPathResource("/geo_db/GeoLite2-City.mmdb").getFile();
        DatabaseReader reader = new DatabaseReader.Builder(database).build();
        geoIPLocationService = new GeoIPLocationService(reader);

        Properties nlpProperties = new Properties();
        nlpProperties.setProperty("annotators", "tokenize, ssplit, pos, lemma, parse, sentiment");
        stanfordCoreNLP = new StanfordCoreNLP(nlpProperties);

        fcmToken = UUID.randomUUID().toString();
        deviceType = DeviceTypeEnum.A.getName();
        model = "Model";
        osVersion = "OS-Version";
        appVersion = "1.3.150";

        userAccountManager = new UserAccountManagerImpl(getMongoTemplate());
        userAuthenticationManager = new UserAuthenticationManagerImpl(getMongoTemplate());
        userPreferenceManager = new UserPreferenceManagerImpl(getMongoTemplate());
        userProfileManager = new UserProfileManagerImpl(getMongoTemplate());
        generateUserIdManager = new GenerateUserIdManagerImpl(getMongoTemplate());
        emailValidateManager = new EmailValidateManagerImpl(getMongoTemplate());
        forgotRecoverManager = new ForgotRecoverManagerImpl(getMongoTemplate());
        registeredDeviceManager = new RegisteredDeviceManagerImpl(getMongoTemplate());
        userMedicalProfileManager = new UserMedicalProfileManagerImpl(getMongoTemplate());
        purchaseOrderManager = new PurchaseOrderManagerImpl(getMongoTemplate());
        purchaseOrderProductManager = new PurchaseOrderProductManagerImpl(getMongoTemplate());
        storeProductManager = new StoreProductManagerImpl(getMongoTemplate());
        scheduledTaskManager = new ScheduledTaskManagerImpl(getMongoTemplate());
        bizNameManager = new BizNameManagerImpl(getMongoTemplate());
        businessCustomerPriorityManager = new BusinessCustomerPriorityManagerImpl(getMongoTemplate());
        businessUserStoreManager = new BusinessUserStoreManagerImpl(getMongoTemplate());
        businessUserManager = new BusinessUserManagerImpl(getMongoTemplate());
        queueManager = new QueueManagerImpl(getMongoTemplate());
        bizStoreManager = new BizStoreManagerImpl(getMongoTemplate());
        statsBizStoreDailyManager = new StatsBizStoreDailyManagerImpl(getMongoTemplate());
        scheduleAppointmentManager = new ScheduleAppointmentManagerImpl(getMongoTemplate());
        pointEarnedManager = new PointEarnedManagerImpl(getMongoTemplate());
        couponManager = new CouponManagerImpl(getMongoTemplate());
        apiHealthNowManager = new ApiHealthNowManagerImpl(getMongoTemplate());
        userAddressManager = new UserAddressManagerImpl(5, getMongoTemplate());

        generateUserIdService = new GenerateUserIdService(generateUserIdManager);
        emailValidateService = new EmailValidateService(emailValidateManager);
        userMedicalProfileService = new UserMedicalProfileService(userMedicalProfileManager, userMedicalProfileHistoryManager);
        nlpService = new NLPService(stanfordCoreNLP, maxentTagger);
        businessCustomerPriorityService = new BusinessCustomerPriorityService(businessCustomerPriorityManager, bizNameManager);
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

        reviewService = new ReviewService(
            180,
            queueManager,
            queueManagerJDBC,
            purchaseOrderManager,
            purchaseOrderManagerJDBC,
            userProfileManager
        );

        professionalProfileManager = new ProfessionalProfileManagerImpl(getMongoTemplate());
        professionalProfileService = new ProfessionalProfileService(
            reviewService,
            professionalProfileManager,
            userProfileManager,
            businessUserStoreManager,
            bizStoreManager);

        externalService = new ExternalService("AIzaSyDUM3yIIrwrx3ciwZ57O9YamC4uISWAlAk", 0, bizStoreManager);
        userProfilePreferenceService = new UserProfilePreferenceService(
            userProfileManager,
            userPreferenceManager,
            userAddressManager
        );

        accountMobileService = new AccountMobileService(
            10,
            accountService,
            userProfilePreferenceService,
            userMedicalProfileService,
            professionalProfileService,
            userAddressService,
            businessUserManager,
            businessUserStoreManager,
            jmsProducerMobileService
        );

        accountClientValidator = new AccountClientValidator(4, 5, 1, 2, 6, 6);
        deviceService = new DeviceService(registeredDeviceManager, userProfileManager);
        notificationMessageManager = new NotificationMessageManagerImpl(getMongoTemplate());
        messageCustomerService = new MessageCustomerService(
            1,
            notificationMessageManager,
            registeredDeviceManager,
            bizStoreManager,
            bizNameManager,
            queueManagerJDBC,
            tokenQueueManager,
            userProfileManager,
            firebaseService,
            firebaseMessageService,
            languageTranslationService,
            graphDetailOfPerson,
            notificationN4jManager
        );

        firebaseService = new FirebaseService(firebaseConfig, userProfileManager);
        deviceRegistrationService = new DeviceRegistrationService("information", registeredDeviceManager, firebaseService, userProfilePreferenceService, geoIPLocationService);
        apiHealthService = new ApiHealthService(apiHealthNowManager);
        tokenQueueManager = new TokenQueueManagerImpl(getMongoTemplate());
        storeHourManager = new StoreHourManagerImpl(getMongoTemplate());
        businessCustomerManager = new BusinessCustomerManagerImpl(getMongoTemplate());
        s3FileManager = new S3FileManagerImpl(getMongoTemplate());
        firebaseMessageService = new FirebaseMessageService("", okHttpClient);
        businessCustomerService = new BusinessCustomerService(
            businessCustomerManager,
            userProfileManager,
            queueManager,
            messageCustomerService
        );

        couponService = new CouponService(couponManager, bizStoreManager, userProfileManager);
        purchaseOrderProductService = new PurchaseOrderProductService(couponService, purchaseOrderProductManager, purchaseOrderProductManagerJDBC, userAddressService);

        customTextToSpeechService = new CustomTextToSpeechService(customTextToSpeechManager);
        textToSpeechService = new TextToSpeechService(
            queueManager,
            bizStoreManager,
            textToSpeechConfiguration,
            customTextToSpeechService
        );

        tokenQueueService = new TokenQueueService(
            1,
            tokenQueueManager,
            firebaseMessageService,
            queueManager,
            accountService,
            registeredDeviceManager,
            queueManagerJDBC,
            storeHourManager,
            bizStoreManager,
            businessCustomerService,
            textToSpeechService,
            messageCustomerService,
            jmsProducerService,
            subscribeTopicService,
            apiHealthService
        );

        transactionService = new TransactionService(
            2,
            getMongoTemplate(),
            transactionManager(),
            purchaseOrderManager,
            purchaseOrderProductManager,
            storeProductManager,
            cashfreeService,
            mongoHosts()
        );

        storeHourService = new StoreHourService(storeHourManager);

        queueService = new QueueService(
            userProfileManager,
            bizStoreManager,
            queueManager,
            queueManagerJDBC,
            businessUserStoreManager,
            statsBizStoreDailyManager,
            purchaseOrderManager,
            purchaseOrderManagerJDBC,
            businessCustomerService,
            tokenQueueService,
            purchaseOrderProductService,
            storeHourService,
            couponService
        );

        bizService = new BizService(
            69.172,
            111.321,
            bizNameManager,
            bizStoreManager,
            storeHourManager,
            tokenQueueService,
            queueService,
            businessUserManager,
            businessUserStoreManager,
            mailService,
            userProfileManager,
            userPreferenceManager,
            scheduledTaskManager,
            storeHourService
        );

        storeCategoryManager = new StoreCategoryManagerImpl(getMongoTemplate());
        storeCategoryService = new StoreCategoryService(storeCategoryManager, storeProductManager);
        publishArticleManager = new PublishArticleManagerImpl(getMongoTemplate());
        advertisementManager = new AdvertisementManagerImpl(getMongoTemplate());
        propertyRentalManager = new PropertyRentalManagerImpl(getMongoTemplate());
        householdItemManager = new HouseholdItemManagerImpl(getMongoTemplate());

        fileService = new FileService(
            192, 192, 300, 150,
            accountService,
            ftpService,
            s3FileManager,
            bizNameManager,
            bizStoreManager,
            storeProductManager,
            publishArticleManager,
            advertisementManager,
            propertyRentalManager,
            householdItemManager,
            bizService,
            storeCategoryService
        );

        storeProductService = new StoreProductService(storeProductManager, bizStoreManager, fileService, transactionService);
        purchaseOrderService = new PurchaseOrderService(
            5,
            bizStoreManager,
            businessUserManager,
            storeHourManager,
            purchaseOrderManager,
            purchaseOrderManagerJDBC,
            purchaseOrderProductManager,
            purchaseOrderProductManagerJDBC,
            queueManager,
            queueManagerJDBC,
            pointEarnedManager,
            couponService,
            userAddressService,
            firebaseMessageService,
            registeredDeviceManager,
            tokenQueueManager,
            storeProductService,
            tokenQueueService,
            accountService,
            transactionService,
            nlpService,
            mailService,
            cashfreeService,
            purchaseOrderProductService,
            subscribeTopicService
        );
        purchaseOrderMobileService = new PurchaseOrderMobileService(queueManager, queueManagerJDBC, purchaseOrderService, purchaseOrderProductService);
        notifyMobileService = new NotifyMobileService(purchaseOrderService, firebaseMessageService, firebaseService, tokenQueueManager, queueService);
        tokenQueueMobileService = new TokenQueueMobileService(
            tokenQueueService,
            bizService,
            tokenQueueManager,
            queueManager,
            professionalProfileService,
            userProfileManager,
            businessUserStoreManager,
            notificationMessageManager,
            storeHourService,
            queueService,
            messageCustomerService);

        storeDetailService = new StoreDetailService(bizService, storeProductService, storeCategoryService, queueService, storeHourService);
        bizStoreElasticManager = new BizStoreElasticManagerImpl(restHighLevelClient);
        bizStoreSpatialElasticManager = new BizStoreSpatialElasticManagerImpl(restHighLevelClient);
        bizStoreSpatialElasticService = new BizStoreSpatialElasticService(bizStoreSpatialElasticManager, elasticsearchClientConfiguration);
        bizStoreElasticService = new BizStoreElasticService(
            bizStoreElasticManager,
            elasticAdministrationService,
            bizStoreManager,
            storeHourManager,
            bizStoreSpatialElasticService,
            apiHealthService);

        joinAbortService = new JoinAbortService(
            2,
            deviceService,
            tokenQueueService,
            purchaseOrderService,
            queueManager,
            purchaseOrderProductService,
            bizService,
            businessCustomerService,
            firebaseMessageService,
            storeHourService);

        queueMobileService = new QueueMobileService(
            queueManager,
            queueManagerJDBC,
            storeHourManager,
            businessUserManager,
            userProfileManager,
            scheduleAppointmentManager,
            pointEarnedManager,
            bizService,
            deviceRegistrationService,
            nlpService,
            purchaseOrderService,
            purchaseOrderProductService,
            queueService,
            joinAbortService,
            tokenQueueMobileService,
            jmsProducerMobileService,
            storeHourService
        );

        merchantExtendingJoinService = new MerchantExtendingJoinService(
            joinAbortService,
            queueMobileService,
            smsService,
            accountService,
            businessCustomerService,
            deviceService,
            notifyMobileService,
            storeHourService
        );

        scheduleAppointmentService = new ScheduleAppointmentService(
            60,
            2,
            24,
            "no-reply@noqapp.com",
            "NoQueue",
            scheduleAppointmentManager,
            storeHourManager,
            userProfileManager,
            userAccountManager,
            userPreferenceManager,
            registeredDeviceManager,
            tokenQueueManager,
            scheduledTaskManager,
            bizService,
            firebaseMessageService,
            mailService,
            storeHourService
        );

        hospitalVisitScheduleManager = new HospitalVisitScheduleManagerImpl(getMongoTemplate());
        hospitalVisitScheduleService = new HospitalVisitScheduleService(hospitalVisitScheduleManager, userProfileManager);
        authenticateMobileService = new AuthenticateMobileService(
            userAccountManager
        );

        accountClientController = new AccountClientController(
            accountService,
            accountMobileService,
            accountClientValidator,
            deviceRegistrationService,
            hospitalVisitScheduleService,
            authenticateMobileService
        );

        preferredBusinessManager = new PreferredBusinessManagerImpl(getMongoTemplate());
        preferredBusinessService = new PreferredBusinessService(preferredBusinessManager, bizStoreManager);

        businessUserService = new BusinessUserService(businessUserManager);
        businessUserStoreService = new BusinessUserStoreService(
            10,
            businessUserStoreManager,
            preferredBusinessService,
            businessUserService,
            tokenQueueService,
            accountService,
            bizService,
            professionalProfileService,
            storeHourService,
            userAddressService
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
            10,
            medicalRecordManager,
            medicalPhysicalManager,
            medicalMedicationManager,
            medicalMedicineManager,
            medicalPathologyManager,
            medicalPathologyTestManager,
            medicalRadiologyManager,
            medicalRadiologyTestManager,
            userProfileManager,
            bizStoreManager,
            queueManager,
            registeredDeviceManager,
            businessUserStoreService,
            purchaseOrderService,
            userMedicalProfileService,
            userAddressManager
        );

        medicalRecordMobileService = new MedicalRecordMobileService(medicalRecordService);
        medicalFileService = new MedicalFileService(
            medicalRecordManager,
            medicalPathologyManager,
            medicalRadiologyManager,
            s3FileManager,
            fileService,
            ftpService
        );

        graphDetailOfPerson = new GraphDetailOfPerson(
            personN4jManager,
            businessCustomerN4jManager,
            anomalyN4jManager,
            notificationN4jManager,
            graphQueue,
            graphBusinessCustomer,
            bizNameManager,
            queueManagerJDBC
        );

        registerUser();
        addSecurityContextHolder("9118000000030");
        createBusinessDoctor("9118000000030");

        addSecurityContextHolder("9118000000060");
        createBusinessPharmacy("9118000000060");

        addSecurityContextHolder("9118000000061");
        createBusinessHealthCareService("9118000000061");

        addSecurityContextHolder("9118000000090");
        createBusinessRestaurant("9118000000090");
        populateRestaurantWithProducts("9118000000021");

        addSecurityContextHolder("9118000001100");
        createBusinessCSD("9118000001100");
    }

    private void registerUser() {
        /* System Users. Like Admin, Supervisor. */
        addSystemUsers();

        /* Clients. */
        addClients();

        /* Store Admin and Queue Supervisors. */
        addStoreUsersToDoctor();
        addStoreUsersToPharmacy();
        addStoreUsersToHealthCare();
        addStoreUsersToRestaurant();
        addStoreUsersToCSD();
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

    private void addStoreUsersToHealthCare() {
        Registration queueAdmin = new Registration()
            .setPhone("+9118000001161")
            .setFirstName("Health Service Admin")
            .setMail("healthcare@r.com")
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

        UserProfileEntity merchantUserProfile = accountService.checkUserExistsByPhone("9118000001161");
        merchantUserProfile.setLevel(UserLevelEnum.M_ADMIN);
        accountService.save(merchantUserProfile);
        UserAccountEntity merchantUserAccount = accountService.changeAccountRolesToMatchUserLevel(
            merchantUserProfile.getQueueUserId(),
            merchantUserProfile.getLevel());
        accountService.save(merchantUserAccount);

        Registration queueSupervisor = new Registration()
            .setPhone("+9118000001162")
            .setFirstName("Health Service Super")
            .setMail("healthcare_supervisor@r.com")
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

        UserProfileEntity queueSupervisorUserProfile = accountService.checkUserExistsByPhone("9118000001162");
        queueSupervisorUserProfile.setLevel(UserLevelEnum.Q_SUPERVISOR);
        accountService.save(queueSupervisorUserProfile);
        UserAccountEntity queueSupervisorUserAccount = accountService.changeAccountRolesToMatchUserLevel(
            queueSupervisorUserProfile.getQueueUserId(),
            queueSupervisorUserProfile.getLevel());
        accountService.save(queueSupervisorUserAccount);

        Registration queueManager = new Registration()
            .setPhone("+9118000001163")
            .setFirstName("Health Service Manager")
            .setMail("healthcare_manager@r.com")
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

        UserProfileEntity storeManagerUserProfile = accountService.checkUserExistsByPhone("9118000001163");
        storeManagerUserProfile.setLevel(UserLevelEnum.S_MANAGER);
        accountService.save(storeManagerUserProfile);
        UserAccountEntity storeManagerUserAccount = accountService.changeAccountRolesToMatchUserLevel(
            storeManagerUserProfile.getQueueUserId(),
            storeManagerUserProfile.getLevel());
        accountService.save(storeManagerUserAccount);
    }

    private void addStoreUsersToRestaurant() {
        Registration queueAdmin = new Registration()
            .setPhone("+9118000000090")
            .setFirstName("Restaurant Business")
            .setMail("restaurant_business@r.com")
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

        UserProfileEntity merchantUserProfile = accountService.checkUserExistsByPhone("9118000000090");
        merchantUserProfile.setLevel(UserLevelEnum.M_ADMIN);
        accountService.save(merchantUserProfile);
        UserAccountEntity merchantUserAccount = accountService.changeAccountRolesToMatchUserLevel(
            merchantUserProfile.getQueueUserId(),
            merchantUserProfile.getLevel());
        accountService.save(merchantUserAccount);

        Registration queueSupervisor = new Registration()
            .setPhone("+9118000000091")
            .setFirstName("Restaurant Store")
            .setMail("restaurant_store_supervisor@r.com")
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

        UserProfileEntity queueSupervisorUserProfile = accountService.checkUserExistsByPhone("9118000000091");
        queueSupervisorUserProfile.setLevel(UserLevelEnum.Q_SUPERVISOR);
        accountService.save(queueSupervisorUserProfile);
        UserAccountEntity queueSupervisorUserAccount = accountService.changeAccountRolesToMatchUserLevel(
            queueSupervisorUserProfile.getQueueUserId(),
            queueSupervisorUserProfile.getLevel());
        accountService.save(queueSupervisorUserAccount);

        Registration queueManager = new Registration()
            .setPhone("+9118000000092")
            .setFirstName("Manager of Restaurant")
            .setMail("manager_restaurant@r.com")
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

        UserProfileEntity storeManagerUserProfile = accountService.checkUserExistsByPhone("9118000000092");
        storeManagerUserProfile.setLevel(UserLevelEnum.S_MANAGER);
        accountService.save(storeManagerUserProfile);
        UserAccountEntity storeManagerUserAccount = accountService.changeAccountRolesToMatchUserLevel(
            storeManagerUserProfile.getQueueUserId(),
            storeManagerUserProfile.getLevel());
        accountService.save(storeManagerUserAccount);
    }

    private void addStoreUsersToCSD() {
        Registration queueAdmin = new Registration()
            .setPhone("+9118000001100")
            .setFirstName("CSD Gurugram")
            .setMail("csd_business@r.com")
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

        UserProfileEntity merchantUserProfile = accountService.checkUserExistsByPhone("9118000001100");
        merchantUserProfile.setLevel(UserLevelEnum.M_ADMIN);
        accountService.save(merchantUserProfile);
        UserAccountEntity merchantUserAccount = accountService.changeAccountRolesToMatchUserLevel(
            merchantUserProfile.getQueueUserId(),
            merchantUserProfile.getLevel());
        accountService.save(merchantUserAccount);

        Registration queueSupervisor = new Registration()
            .setPhone("+9118000001101")
            .setFirstName("CSD Gurugram")
            .setMail("csd_store_supervisor@r.com")
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

        UserProfileEntity queueSupervisorUserProfile = accountService.checkUserExistsByPhone("9118000001101");
        queueSupervisorUserProfile.setLevel(UserLevelEnum.Q_SUPERVISOR);
        accountService.save(queueSupervisorUserProfile);
        UserAccountEntity queueSupervisorUserAccount = accountService.changeAccountRolesToMatchUserLevel(
            queueSupervisorUserProfile.getQueueUserId(),
            queueSupervisorUserProfile.getLevel());
        accountService.save(queueSupervisorUserAccount);

        Registration queueManager = new Registration()
            .setPhone("+9118000001102")
            .setFirstName("Manager of CSD")
            .setMail("manager_csd@r.com")
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

        UserProfileEntity storeManagerUserProfile = accountService.checkUserExistsByPhone("9118000001102");
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

        Registration client3 = new Registration()
            .setPhone("+9118000001111")
            .setFirstName("Damuscus")
            .setMail("damuscus@r.com")
            .setPassword("password")
            .setBirthday("2000-12-12")
            .setGender(GenderEnum.M.name())
            .setCountryShortName("IN")
            .setTimeZoneId("Asia/Calcutta")
            .setInviteCode("");

        Registration client4 = new Registration()
            .setPhone("+9118000001112")
            .setFirstName("Sita")
            .setMail("sita@r.com")
            .setPassword("password")
            .setBirthday("2000-12-12")
            .setGender(GenderEnum.F.name())
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

        accountClientController.register(
            new ScrubbedInput(did),
            new ScrubbedInput(deviceType),
            client3.asJson(),
            httpServletResponse);

        accountClientController.register(
            new ScrubbedInput(did),
            new ScrubbedInput(deviceType),
            client4.asJson(),
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

        BizNameEntity bizName = BizNameEntity.newInstance(CommonUtil.generateCodeQR(Objects.requireNonNull(mockEnvironment.getProperty("build.env"))))
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
        bizName.setCodeQR(CommonUtil.generateCodeQR(Objects.requireNonNull(mockEnvironment.getProperty("build.env"))));
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
        bizStore.setCodeQR(CommonUtil.generateCodeQR(Objects.requireNonNull(mockEnvironment.getProperty("build.env"))));
        bizService.saveStore(bizStore, "Created New Store");

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
        tokenQueueService.createUpdate(bizStore, Constants.appendPrefix);

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
        businessUserService.save(
            BusinessUserEntity.newInstance(queueManagerUserProfile.getQueueUserId(), queueManagerUserProfile.getLevel())
                .setBizName(bizStore.getBizName())
                .setBusinessUserRegistrationStatus(BusinessUserRegistrationStatusEnum.V)
                .setValidateByQid(accountService.checkUserExistsByPhone("9118000000102").getQueueUserId()));

        professionalProfileService.createProfessionalProfile(queueManagerUserProfile.getQueueUserId());
        ProfessionalProfileEntity professionalProfile = professionalProfileService.findByQid(queueManagerUserProfile.getQueueUserId());
        NameDatePair nameDatePair1 = new NameDatePair().setName("MBBS").setMonthYear("1985-01-22");
        List<NameDatePair> nameDatePairs = new ArrayList<NameDatePair>() {
            private static final long serialVersionUID = 2822784240200676022L;

            {
                add(nameDatePair1);
            }
        };
        professionalProfile.setEducation(nameDatePairs).setAboutMe("About Me");
        professionalProfileService.save(professionalProfile);
    }

    private void createBusinessPharmacy(String phone) {
        UserProfileEntity userProfile = accountService.checkUserExistsByPhone(phone);

        BizNameEntity bizName = BizNameEntity.newInstance(CommonUtil.generateCodeQR(Objects.requireNonNull(mockEnvironment.getProperty("build.env"))))
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
        bizName.setCodeQR(CommonUtil.generateCodeQR(Objects.requireNonNull(mockEnvironment.getProperty("build.env"))));
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
        bizStore.setCodeQR(CommonUtil.generateCodeQR(Objects.requireNonNull(mockEnvironment.getProperty("build.env"))));
        bizService.saveStore(bizStore, "Created New Store");

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
        tokenQueueService.createUpdate(bizStore, Constants.appendPrefix);

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

    private void createBusinessHealthCareService(String phone) {
        UserProfileEntity userProfile = accountService.checkUserExistsByPhone(phone);

        BizNameEntity bizName = BizNameEntity.newInstance(CommonUtil.generateCodeQR(Objects.requireNonNull(mockEnvironment.getProperty("build.env"))))
            .setBusinessName("Health Care Service")
            .setBusinessType(BusinessTypeEnum.HS)
            .setPhone("9118000000161")
            .setPhoneRaw("18000000161")
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
        bizName.setCodeQR(CommonUtil.generateCodeQR(Objects.requireNonNull(mockEnvironment.getProperty("build.env"))));
        bizService.saveName(bizName);

        BizStoreEntity bizStore = BizStoreEntity.newInstance()
            .setBizName(bizName)
            .setDisplayName("XRAY Service")
            .setBusinessType(bizName.getBusinessType())
            .setBizCategoryId(HealthCareServiceEnum.XRAY.getName())
            .setPhone("9118000000162")
            .setPhoneRaw("18000000162")
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
        bizStore.setCodeQR(CommonUtil.generateCodeQR(Objects.requireNonNull(mockEnvironment.getProperty("build.env"))));
        bizService.saveStore(bizStore, "Created New Store");

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
        tokenQueueService.createUpdate(bizStore, Constants.appendPrefix);

        /* Add Queue Admin, Queue Supervisor, Queue Manager to Business and Store. */
        BusinessUserEntity businessUser = BusinessUserEntity.newInstance(
            userProfile.getQueueUserId(),
            UserLevelEnum.M_ADMIN
        );
        businessUser.setBusinessUserRegistrationStatus(BusinessUserRegistrationStatusEnum.V)
            .setValidateByQid(accountService.checkUserExistsByPhone("9118000000102").getQueueUserId())
            .setBizName(bizName);
        businessUserService.save(businessUser);

        UserProfileEntity queueSupervisorUserProfile = accountService.checkUserExistsByPhone("9118000001162");
        BusinessUserStoreEntity businessUserStore = new BusinessUserStoreEntity(
            queueSupervisorUserProfile.getQueueUserId(),
            bizStore.getId(),
            bizName.getId(),
            bizStore.getCodeQR(),
            queueSupervisorUserProfile.getLevel());
        businessUserStoreService.save(businessUserStore);

        UserProfileEntity queueManagerUserProfile = accountService.checkUserExistsByPhone("9118000001163");
        businessUserStore = new BusinessUserStoreEntity(
            queueManagerUserProfile.getQueueUserId(),
            bizStore.getId(),
            bizName.getId(),
            bizStore.getCodeQR(),
            queueManagerUserProfile.getLevel());
        businessUserStoreService.save(businessUserStore);
    }

    private void createBusinessRestaurant(String phone) {
        UserProfileEntity userProfile = accountService.checkUserExistsByPhone(phone);

        BizNameEntity bizName = BizNameEntity.newInstance(CommonUtil.generateCodeQR(Objects.requireNonNull(mockEnvironment.getProperty("build.env"))))
            .setBusinessName("Restaurant Business")
            .setBusinessType(BusinessTypeEnum.RS)
            .setPhone("9118000000021")
            .setPhoneRaw("18000000021")
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
        bizName.setCodeQR(CommonUtil.generateCodeQR(Objects.requireNonNull(mockEnvironment.getProperty("build.env"))));
        bizService.saveName(bizName);

        BizStoreEntity bizStore = BizStoreEntity.newInstance()
            .setBizName(bizName)
            .setDisplayName("Mera Restaurant")
            .setBusinessType(bizName.getBusinessType())
            .setBizCategoryId(null)
            .setPhone("9118000000022")
            .setPhoneRaw("18000000022")
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
        bizStore.setCodeQR(CommonUtil.generateCodeQR(Objects.requireNonNull(mockEnvironment.getProperty("build.env"))));
        bizService.saveStore(bizStore, "Created New Store");

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
        tokenQueueService.createUpdate(bizStore, Constants.appendPrefix);

        /* Add Queue Admin, Queue Supervisor, Queue Manager to Business and Store. */
        BusinessUserEntity businessUser = BusinessUserEntity.newInstance(
            userProfile.getQueueUserId(),
            UserLevelEnum.M_ADMIN
        );
        businessUser.setBusinessUserRegistrationStatus(BusinessUserRegistrationStatusEnum.V)
            .setValidateByQid(accountService.checkUserExistsByPhone("9118000000090").getQueueUserId())
            .setBizName(bizName);
        businessUserService.save(businessUser);

        UserProfileEntity queueSupervisorUserProfile = accountService.checkUserExistsByPhone("9118000000091");
        BusinessUserStoreEntity businessUserStore = new BusinessUserStoreEntity(
            queueSupervisorUserProfile.getQueueUserId(),
            bizStore.getId(),
            bizName.getId(),
            bizStore.getCodeQR(),
            queueSupervisorUserProfile.getLevel());
        businessUserStoreService.save(businessUserStore);

        UserProfileEntity queueManagerUserProfile = accountService.checkUserExistsByPhone("9118000000092");
        businessUserStore = new BusinessUserStoreEntity(
            queueManagerUserProfile.getQueueUserId(),
            bizStore.getId(),
            bizName.getId(),
            bizStore.getCodeQR(),
            queueManagerUserProfile.getLevel());
        businessUserStoreService.save(businessUserStore);
    }

    private void populateRestaurantWithProducts(String phone) {
        BizNameEntity bizName = bizNameManager.findByPhone(phone);
        List<BizStoreEntity> bizStores = bizStoreManager.getAllBizStores(bizName.getId());

        StoreCategoryEntity storeCategory = new StoreCategoryEntity()
            .setCategoryName("Chat Food")
            .setBizStoreId(bizStores.get(0).getId())
            .setBizNameId(bizName.getId());
        storeCategoryManager.save(storeCategory);

        StoreProductEntity storeProduct1 = new StoreProductEntity()
            .setBizStoreId(bizStores.get(0).getId())
            .setProductName("Alloo Tikkii")
            .setProductPrice(1000)
            .setTax(TaxEnum.FI)
            .setProductDiscount(100)
            .setProductInfo("Made from Alloo")
            .setStoreCategoryId(storeCategory.getId())
            .setProductType(ProductTypeEnum.VE)
            .setUnitValue(1)
            .setPackageSize(1)
            .setUnitOfMeasurement(UnitOfMeasurementEnum.CN);
        storeProductManager.save(storeProduct1);
    }

    private void createBusinessCSD(String phone) {
        UserProfileEntity userProfile = accountService.checkUserExistsByPhone(phone);

        BizNameEntity bizName = BizNameEntity.newInstance(CommonUtil.generateCodeQR(Objects.requireNonNull(mockEnvironment.getProperty("build.env"))))
            .setBusinessName("CSD Business")
            .setBusinessType(BusinessTypeEnum.CDQ)
            .setPhone("9118000000041")
            .setPhoneRaw("18000000041")
            .setAddress("Shop No 10 Plot No 102 Sector 29, Vashi, Navi Mumbai, Maharashtra 400703")
            .setTown("Vashi")
            .setStateShortName("MH")
            .setTimeZone("Asia/Calcutta")
            .setInviteeCode(userProfile.getInviteCode())
            .setAddressOrigin(AddressOriginEnum.G)
            .setCountryShortName("IN")
            .setCoordinate(new double[]{71.022498, 18.0244723})
            .setPriorityAccess(OnOffEnum.O);
        String webLocation = bizService.buildWebLocationForBiz(
            bizName.getTown(),
            bizName.getStateShortName(),
            bizName.getCountryShortName(),
            bizName.getBusinessName(),
            bizName.getId());

        bizName.setWebLocation(webLocation);
        bizName.setCodeQR(CommonUtil.generateCodeQR(Objects.requireNonNull(mockEnvironment.getProperty("build.env"))));
        bizService.saveName(bizName);

        BizStoreEntity bizStore = BizStoreEntity.newInstance()
            .setBizName(bizName)
            .setDisplayName("CSD Grocery for Ex-Servicemen")
            .setBusinessType(bizName.getBusinessType())
            .setBizCategoryId(CanteenStoreDepartmentEnum.EG.getName())
            .setPhone("9118000000042")
            .setPhoneRaw("18000000042")
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
        webLocation = bizService.buildWebLocationForBiz(
            bizStore.getTown(),
            bizStore.getStateShortName(),
            bizStore.getCountryShortName(),
            bizStore.getDisplayName(),
            bizStore.getId());
        bizStore.setWebLocation(webLocation);
        bizStore.setCodeQR(CommonUtil.generateCodeQR(Objects.requireNonNull(mockEnvironment.getProperty("build.env"))));
        bizService.saveStore(bizStore, "Created New Store");

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
        tokenQueueService.createUpdate(bizStore, Constants.appendPrefix);

        /* Add Queue Admin, Queue Supervisor, Queue Manager to Business and Store. */
        BusinessUserEntity businessUser = BusinessUserEntity.newInstance(
            userProfile.getQueueUserId(),
            UserLevelEnum.M_ADMIN
        );
        businessUser.setBusinessUserRegistrationStatus(BusinessUserRegistrationStatusEnum.V)
            .setValidateByQid(accountService.checkUserExistsByPhone("9118000001100").getQueueUserId())
            .setBizName(bizName);
        businessUserService.save(businessUser);

        UserProfileEntity queueSupervisorUserProfile = accountService.checkUserExistsByPhone("9118000001101");
        BusinessUserStoreEntity businessUserStore = new BusinessUserStoreEntity(
            queueSupervisorUserProfile.getQueueUserId(),
            bizStore.getId(),
            bizName.getId(),
            bizStore.getCodeQR(),
            queueSupervisorUserProfile.getLevel());
        businessUserStoreService.save(businessUserStore);

        UserProfileEntity queueManagerUserProfile = accountService.checkUserExistsByPhone("9118000001102");
        businessUserStore = new BusinessUserStoreEntity(
            queueManagerUserProfile.getQueueUserId(),
            bizStore.getId(),
            bizName.getId(),
            bizStore.getCodeQR(),
            queueManagerUserProfile.getLevel());
        businessUserStoreService.save(businessUserStore);

        bizStore = BizStoreEntity.newInstance()
            .setBizName(bizName)
            .setDisplayName("CSD Liquor for Ex-Servicemen")
            .setBusinessType(bizName.getBusinessType())
            .setBizCategoryId(CanteenStoreDepartmentEnum.EL.getName())
            .setPhone("9118000000042")
            .setPhoneRaw("18000000042")
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
        webLocation = bizService.buildWebLocationForBiz(
            bizStore.getTown(),
            bizStore.getStateShortName(),
            bizStore.getCountryShortName(),
            bizStore.getDisplayName(),
            bizStore.getId());
        bizStore.setWebLocation(webLocation);
        bizStore.setCodeQR(CommonUtil.generateCodeQR(Objects.requireNonNull(mockEnvironment.getProperty("build.env"))));
        bizService.saveStore(bizStore, "Created New Store");

        storeHours = new LinkedList<>();
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
        tokenQueueService.createUpdate(bizStore, Constants.appendPrefix);

        queueSupervisorUserProfile = accountService.checkUserExistsByPhone("9118000001101");
        businessUserStore = new BusinessUserStoreEntity(
            queueSupervisorUserProfile.getQueueUserId(),
            bizStore.getId(),
            bizName.getId(),
            bizStore.getCodeQR(),
            queueSupervisorUserProfile.getLevel());
        businessUserStoreService.save(businessUserStore);

        queueManagerUserProfile = accountService.checkUserExistsByPhone("9118000001102");
        businessUserStore = new BusinessUserStoreEntity(
            queueManagerUserProfile.getQueueUserId(),
            bizStore.getId(),
            bizName.getId(),
            bizStore.getCodeQR(),
            queueManagerUserProfile.getLevel());
        businessUserStoreService.save(businessUserStore);
    }

    public void addSecurityContextHolder(String phone) {
        Authentication authentication = Mockito.mock(Authentication.class);
        // Mockito.whens() for your authorization object
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);

        List<GrantedAuthority> updatedAuthorities = new ArrayList<>();
        updatedAuthorities.add(new SimpleGrantedAuthority(RoleEnum.ROLE_CLIENT.name()));
        updatedAuthorities.add(new SimpleGrantedAuthority(RoleEnum.ROLE_Q_SUPERVISOR.name()));
        updatedAuthorities.add(new SimpleGrantedAuthority(RoleEnum.ROLE_S_MANAGER.name()));
        updatedAuthorities.add(new SimpleGrantedAuthority(RoleEnum.ROLE_M_ADMIN.name()));

        UserProfileEntity merchantUserProfile = accountService.checkUserExistsByPhone(phone);
        QueueUser queueUser = new QueueUser(merchantUserProfile.getEmail(), "XXX", updatedAuthorities, merchantUserProfile.getQueueUserId(), merchantUserProfile.getLevel(), true, true, merchantUserProfile.getCountryShortName(), "XYZ");
        Mockito.when(securityContext.getAuthentication().getPrincipal()).thenReturn(queueUser);
        SecurityContextHolder.setContext(securityContext);
    }
}
