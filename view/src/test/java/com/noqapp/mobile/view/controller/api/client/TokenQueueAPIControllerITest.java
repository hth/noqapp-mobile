package com.noqapp.mobile.view.controller.api.client;

import static com.noqapp.common.utils.DateUtil.MINUTES_IN_MILLISECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.noqapp.common.utils.RandomString;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BizNameEntity;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.StoreHourEntity;
import com.noqapp.domain.UserAccountEntity;
import com.noqapp.domain.json.JsonToken;
import com.noqapp.domain.types.DeviceTypeEnum;
import com.noqapp.mobile.domain.body.client.JoinQueue;
import com.noqapp.mobile.domain.body.client.QueueAuthorize;
import com.noqapp.mobile.domain.body.client.Registration;
import com.noqapp.mobile.view.ITest;
import com.noqapp.mobile.view.controller.open.AccountClientController;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import org.junit.Ignore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * hitender
 * 7/24/20 9:56 AM
 */
@DisplayName("Queue API")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("api")
class TokenQueueAPIControllerITest extends ITest {
    private static final Logger LOG = LoggerFactory.getLogger(TokenQueueAPIControllerITest.class);

    private TokenQueueAPIController tokenQueueAPIController;
    private AccountClientController accountClientController;

    @BeforeEach
    void setUp() {
        tokenQueueAPIController = new TokenQueueAPIController(
            tokenQueueMobileService,
            joinAbortService,
            queueMobileService,
            authenticateMobileService,
            purchaseOrderService,
            purchaseOrderMobileService,
            scheduleAppointmentService,
            geoIPLocationService,
            businessCustomerService,
            apiHealthService
        );

        accountClientController = new AccountClientController(
            accountService,
            accountMobileService,
            accountClientValidator,
            deviceRegistrationService,
            hospitalVisitScheduleService,
            authenticateMobileService
        );
    }

    //@Test
    @Ignore("Tests token issued when limited token available")
    void joinQueue() throws IOException {
        Authentication authentication = Mockito.mock(Authentication.class);
        // Mockito.whens() for your authorization object
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        BizNameEntity bizName = bizService.findByPhone("9118000000041");
        BizStoreEntity bizStore = bizService.findOneBizStore(bizName.getId());
        bizStore.setAverageServiceTime(114000).setAvailableTokenCount(200);
        bizStore.setTimeZone("Pacific/Honolulu");
        bizService.saveStore(bizStore, "Changed AST");

        StoreHourEntity storeHour = bizService.getStoreHours(bizStore.getCodeQR(), bizStore);
        storeHour.setStartHour(930)
            .setEndHour(1600)
            .setLunchTimeStart(1300)
            .setLunchTimeEnd(1400)
            .setTokenAvailableFrom(100)
            .setTokenNotAvailableFrom(1530)
            .setDayClosed(false)
            .setTempDayClosed(false)
            .setPreventJoining(false);
        storeHourManager.save(storeHour);
        long averageServiceTime = bizService.computeAverageServiceTime(DayOfWeek.of(storeHour.getDayOfWeek()), bizStore.getAvailableTokenCount(), bizStore.getId());
        bizService.updateStoreTokenAndServiceTime(bizStore.getCodeQR(), averageServiceTime, bizStore.getAvailableTokenCount());

        List<String> mails = new LinkedList<>();
        for (int i = 0; i < 210; i++) {
            String phone = "+91" + StringUtils.leftPad(String.valueOf(i), 10, '0');
            String name = RandomString.newInstance(6).nextString().toLowerCase();
            Registration user = new Registration()
                .setPhone(phone)
                .setFirstName(name)
                .setMail(name + "@r.com")
                .setPassword("password")
                .setBirthday("2000-12-12")
                .setGender("M")
                .setCountryShortName("IN")
                .setTimeZoneId("Asia/Calcutta")
                .setInviteCode("");

            accountClientController.register(
                new ScrubbedInput(UUID.randomUUID().toString()),
                new ScrubbedInput(DeviceTypeEnum.A.getName()),
                user.asJson(),
                httpServletResponse
            );

            mails.add(name + "@r.com");
        }

        Map<String, String> display = new LinkedHashMap<>();
        for (String mail : mails) {
            UserAccountEntity userAccount = userAccountManager.findByUserId(mail);
            QueueAuthorize queueAuthorize = new QueueAuthorize()
                .setCodeQR(new ScrubbedInput(bizStore.getCodeQR()))
                .setFirstCustomerId(new ScrubbedInput("G" + StringUtils.leftPad(String.valueOf(userAccount.getQueueUserId()), 18, '0')))
                .setAdditionalCustomerId(new ScrubbedInput("L" + StringUtils.leftPad(String.valueOf(userAccount.getQueueUserId()), 18, '0')));

            tokenQueueAPIController.businessApprove(
                new ScrubbedInput(UUID.randomUUID().toString()),
                new ScrubbedInput(DeviceTypeEnum.A.getName()),
                new ScrubbedInput(userAccount.getUserId()),
                new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
                queueAuthorize,
                httpServletResponse
            );

            String jsonToken_String = tokenQueueAPIController.joinQueue(
                new ScrubbedInput(UUID.randomUUID().toString()),
                new ScrubbedInput(DeviceTypeEnum.A.getName()),
                new ScrubbedInput(userAccount.getUserId()),
                new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
                new JoinQueue().setCodeQR(userAccount.getQueueUserId()).setCodeQR(bizStore.getCodeQR()),
                httpServletResponse
            );

            //{"error":{"reason":"CSD Liquor for Ex-Servicemen has not started. Please correct time on your device.","systemErrorCode":"4071","systemError":"DEVICE_TIMEZONE_OFF"}}
            JsonToken jsonToken = new ObjectMapper().readValue(jsonToken_String, JsonToken.class);
            display.put(String.valueOf(jsonToken.getToken()), jsonToken.getTimeSlotMessage());
            LOG.info("Joined queue {} : {}", jsonToken.getToken(), jsonToken);
        }

        Map<String, Integer> count = new HashMap<>();
        for (String key : display.keySet()) {
            LOG.info("{} {}", key, display.get(key));

            if (count.containsKey(display.get(key))) {
                count.put(display.get(key), count.get(display.get(key)) + 1);
            } else {
                count.put(display.get(key), 1);
            }
        }

        for (String key : count.keySet()) {
            System.out.println(key + " " + count.get(key));
        }
        System.out.println("averageServiceTime =" + new BigDecimal(averageServiceTime).divide(new BigDecimal(MINUTES_IN_MILLISECONDS), MathContext.DECIMAL64));
        assertEquals(200, display.size(), "Number of token issued must be equal");
    }
}
