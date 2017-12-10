package com.noqapp.mobile.view.controller.open;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BizNameEntity;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.QueueEntity;
import com.noqapp.domain.StoreHourEntity;
import com.noqapp.domain.TokenQueueEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.json.JsonQueue;
import com.noqapp.domain.json.JsonResponse;
import com.noqapp.domain.json.JsonToken;
import com.noqapp.domain.types.AddressOriginEnum;
import com.noqapp.domain.types.BusinessTypeEnum;
import com.noqapp.domain.types.DeviceTypeEnum;
import com.noqapp.domain.types.QueueStatusEnum;
import com.noqapp.domain.types.QueueUserStateEnum;
import com.noqapp.mobile.domain.body.Registration;
import com.noqapp.mobile.domain.body.ReviewRating;
import com.noqapp.mobile.view.ITest;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * hitender
 * 12/8/17 3:28 AM
 */
@DisplayName("Review Service")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("api")
class ReviewControllerITest extends ITest {
    private String did;
    private String deviceType;

    private ReviewController reviewController;
    private AccountClientController accountClientController;
    private TokenQueueController tokenQueueController;

    @Mock private HttpServletResponse httpServletResponse;

    @BeforeAll
    void testSetUp() {
        did = UUID.randomUUID().toString();
        deviceType = DeviceTypeEnum.A.getName();
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        reviewController = new ReviewController(
                tokenQueueMobileService,
                queueMobileService,
                apiHealthService
        );

        accountClientController = new AccountClientController(
                accountService,
                accountMobileService,
                userProfilePreferenceService,
                inviteService,
                accountClientValidator,
                deviceService
        );

        tokenQueueController = new TokenQueueController(
            tokenQueueMobileService,
            queueMobileService,
            apiHealthService
        );
    }

    @Test
    @DisplayName("Service fails when code QR does not exists")
    void service_fails_when_codeQR_DoesNotExists() {
        ReviewRating reviewRating = new ReviewRating()
                .setCodeQR(did)
                .setToken(1)
                .setRatingCount("5")
                .setHoursSaved("1");

        String response = reviewController.service(
                new ScrubbedInput(did),
                new ScrubbedInput(deviceType),
                reviewRating.asJson(),
                httpServletResponse
        );

        assertNull(response);
    }

    @Test
    @DisplayName("Service when completed")
    void service() throws IOException {
        UserProfileEntity userProfile = registerUser();
        BizStoreEntity bizStore = registerBusiness(userProfile);

        String beforeJoin = tokenQueueController.getQueueState(
                new ScrubbedInput(did),
                new ScrubbedInput(deviceType),
                new ScrubbedInput(bizStore.getCodeQR()),
                httpServletResponse
        );
        JsonQueue jsonQueue = new ObjectMapper().readValue(beforeJoin, JsonQueue.class);

        String afterJoin = tokenQueueController.joinQueue(
                new ScrubbedInput(did),
                new ScrubbedInput(deviceType),
                new ScrubbedInput(jsonQueue.getCodeQR()),
                httpServletResponse
        );
        JsonToken jsonToken = new ObjectMapper().readValue(afterJoin, JsonToken.class);
        assertEquals(QueueStatusEnum.S, jsonToken.getQueueStatus());

        submitReview(bizStore, jsonToken);

        /* Submit review fails when person is not served. */
        QueueEntity queue = queueManager.findOne(bizStore.getCodeQR(), jsonToken.getToken());
        assertEquals(QueueUserStateEnum.Q, queue.getQueueUserState());
        assertEquals(did, queue.getDid());

        /* Initial state of Queue. */
        TokenQueueEntity tokenQueue = queueMobileService.getTokenQueueByCodeQR(bizStore.getCodeQR());
        assertEquals(QueueStatusEnum.S, tokenQueue.getQueueStatus());

        String sid = UUID.randomUUID().toString();

        /* After starting queue state. */
        JsonToken nextInQueue = queueMobileService.getNextInQueue(bizStore.getCodeQR(), "Go To Counter", sid);
        assertEquals(jsonToken.getToken(), nextInQueue.getToken());
        assertNotEquals(jsonToken.getServingNumber(), nextInQueue.getServingNumber());
        assertEquals(QueueStatusEnum.N, nextInQueue.getQueueStatus());
        TokenQueueEntity tokenQueueAfterPressingNext = queueMobileService.getTokenQueueByCodeQR(bizStore.getCodeQR());
        assertEquals(QueueStatusEnum.N, tokenQueueAfterPressingNext.getQueueStatus());

        /* When no more to serve, service is done. Queue state is set to Done. */
        nextInQueue = queueMobileService.updateAndGetNextInQueue(
                bizStore.getCodeQR(),
                queue.getTokenNumber(),
                QueueUserStateEnum.S,
                "Go To Counter",
                sid);
        assertEquals(QueueStatusEnum.D, nextInQueue.getQueueStatus());
        TokenQueueEntity tokenQueueAfterReachingDoneWhenThereIsNoNext = queueMobileService.getTokenQueueByCodeQR(bizStore.getCodeQR());
        assertEquals(QueueStatusEnum.D, tokenQueueAfterReachingDoneWhenThereIsNoNext.getQueueStatus());

        /* Review after user has been serviced. */
        submitReview(bizStore, jsonToken);

        /* Check for submitted review. */
        QueueEntity queueAfterService = queueManager.findOne(bizStore.getCodeQR(), jsonToken.getToken());
        assertEquals(QueueUserStateEnum.S, queueAfterService.getQueueUserState());
        assertEquals(did, queueAfterService.getDid());
    }

    private void submitReview(BizStoreEntity bizStore, JsonToken jsonToken) throws IOException {
        ReviewRating reviewRating = new ReviewRating()
                .setCodeQR(bizStore.getCodeQR())
                .setToken(jsonToken.getToken())
                .setRatingCount("5")
                .setHoursSaved("1");

        /* Fails to update as its still under Queued state. */
        String response = reviewController.service(
                new ScrubbedInput(did),
                new ScrubbedInput(deviceType),
                reviewRating.asJson(),
                httpServletResponse
        );
        JsonResponse jsonResponse = new ObjectMapper().readValue(response, JsonResponse.class);
        assertEquals(1, jsonResponse.getResponse());
    }

    private BizStoreEntity registerBusiness(UserProfileEntity userProfile) {
        BizNameEntity bizName = BizNameEntity.newInstance()
                .setBusinessName("Champ")
                .setBusinessTypes(Arrays.asList(BusinessTypeEnum.AT, BusinessTypeEnum.BA))
                .setPhone("9118000000000")
                .setPhoneRaw("18000000000")
                .setAddress("Shop NO RB.1, Haware's centurion Mall, 1st Floor, Sector No 19, Nerul - East, Seawoods, Navi Mumbai, Mumbai, 400706, India")
                .setTimeZone("Asia/Calcutta")
                .setInviteeCode(userProfile.getInviteCode())
                .setAddressOrigin(AddressOriginEnum.G)
                .setCountryShortName("IN")
                .setCoordinate(new double[] {73.022498, 19.0244723})
                .setMultiStore(false);
        bizService.saveName(bizName);

        BizStoreEntity bizStore = BizStoreEntity.newInstance()
                .setBizName(bizName)
                .setDisplayName("Food")
                .setBusinessTypes(Arrays.asList(BusinessTypeEnum.AT, BusinessTypeEnum.BA))
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
        tokenQueueService.create(bizStore.getCodeQR(), bizStore.getTopic(), bizStore.getDisplayName());
        return bizStore;
    }

    private UserProfileEntity registerUser() throws IOException {
        Registration registration = new Registration()
                .setPhone("+9118000000000")
                .setFirstName("ROCKET mAniA")
                .setMail("rocket@r.com")
                .setBirthday("2000-12-12")
                .setGender("M")
                .setCountryShortName("IN")
                .setTimeZoneId("Asia/Calcutta")
                .setInviteCode("");

        accountClientController.register(
                new ScrubbedInput(did),
                new ScrubbedInput(deviceType),
                registration.asJson(),
                httpServletResponse);

        return accountService.checkUserExistsByPhone(registration.getPhone());
    }
}