package com.noqapp.mobile.view.controller.api.merchant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.TokenQueueEntity;
import com.noqapp.domain.json.JsonHour;
import com.noqapp.domain.json.JsonToken;
import com.noqapp.domain.json.JsonTopic;
import com.noqapp.domain.json.JsonTopicList;
import com.noqapp.domain.types.BusinessTypeEnum;
import com.noqapp.domain.types.DeviceTypeEnum;
import com.noqapp.domain.types.QueueStatusEnum;
import com.noqapp.domain.types.QueueUserStateEnum;
import com.noqapp.domain.types.TokenServiceEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.common.util.ErrorJsonList;
import com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.service.QueueMobileService;
import com.noqapp.mobile.service.TokenQueueMobileService;
import com.noqapp.repository.BusinessUserStoreManager;
import com.noqapp.repository.QueueManager;
import com.noqapp.repository.RegisteredDeviceManager;
import com.noqapp.repository.TokenQueueManager;
import com.noqapp.repository.UserProfileManager;
import com.noqapp.service.AccountService;
import com.noqapp.service.BizService;
import com.noqapp.service.BusinessCustomerService;
import com.noqapp.service.BusinessUserStoreService;
import com.noqapp.service.ProfessionalProfileService;
import com.noqapp.service.QueueService;
import com.noqapp.service.TokenQueueService;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

/**
 * User: hitender
 * Date: 1/11/17 12:32 AM
 */
@SuppressWarnings ({
        "PMD.BeanMembersShouldSerialize",
        "PMD.LocalVariableCouldBeFinal",
        "PMD.MethodArgumentCouldBeFinal",
        "PMD.LongVariable"
})
class ManageQueueControllerTest {

    @Mock private AuthenticateMobileService authenticateMobileService;
    @Mock private QueueService queueService;
    @Mock private QueueMobileService queueMobileService;
    @Mock private TokenQueueService tokenQueueService;
    @Mock private TokenQueueManager tokenQueueManager;
    @Mock private BusinessUserStoreService businessUserStoreService;
    @Mock private ApiHealthService apiHealthService;
    @Mock private BizService bizService;
    @Mock private QueueManager queueManager;
    @Mock private ProfessionalProfileService professionalProfileService;
    @Mock private UserProfileManager userProfileManager;
    @Mock private BusinessUserStoreManager businessUserStoreManager;
    @Mock private AccountService accountService;
    @Mock private BusinessCustomerService businessCustomerService;
    @Mock private RegisteredDeviceManager registeredDeviceManager;

    @Mock private HttpServletResponse response;
    private TokenQueueEntity tokenQueue;

    private ManageQueueController manageQueueController;
    private TokenQueueMobileService tokenQueueMobileService;
    private ObjectMapper mapper;
    private JsonTopicList jsonTopicList;
    private List<JsonTopic> topics;


    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        tokenQueueMobileService = new TokenQueueMobileService(
                tokenQueueService,
                bizService,
                tokenQueueManager,
                queueManager,
                professionalProfileService,
                userProfileManager,
                businessUserStoreManager);

        manageQueueController = new ManageQueueController(
                20,
                authenticateMobileService,
                queueService,
                queueMobileService,
                businessUserStoreService,
                tokenQueueService,
                tokenQueueMobileService,
                accountService,
                businessCustomerService,
                registeredDeviceManager,
                apiHealthService);

        mapper = new ObjectMapper();

        tokenQueue = new TokenQueueEntity("topic", "displayName");
        tokenQueue.setLastNumber(10);
        tokenQueue.setCurrentlyServing(5);
        tokenQueue.setId("codeQR");

        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);

        JsonHour jsonHour = new JsonHour()
                .setEndHour(2359)
                .setStartHour(1)
                .setDayOfWeek(dayOfWeek)
                .setTokenAvailableFrom(1)
                .setTokenNotAvailableFrom(2359)
                .setDayClosed(false)
                .setPreventJoining(false)
                .setDelayedInMinutes(1);

        JsonTopic topic = new JsonTopic(tokenQueue).setHour(jsonHour);
        topics = new ArrayList<>();
        topics.add(topic);

        jsonTopicList = new JsonTopicList();
        jsonTopicList.addTopic(topic);
    }

    @Test
    void queues_fail_authentication() throws Exception {
        String responseJson = manageQueueController.getQueues(
                new ScrubbedInput(""),
                new ScrubbedInput(DeviceTypeEnum.A.getName()),
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                response);

        verify(authenticateMobileService, times(1)).getQueueUserId(any(String.class), any(String.class));
        assertSame(null, responseJson);
    }

    @Test
    void queues_exception() throws Exception {
        when(authenticateMobileService.getQueueUserId(anyString(), anyString())).thenReturn("rid");
        doThrow(new RuntimeException()).when(businessUserStoreService).getQueues(anyString());

        String responseJson = manageQueueController.getQueues(
                new ScrubbedInput(""),
                new ScrubbedInput(DeviceTypeEnum.A.getName()),
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                response);

        verify(authenticateMobileService, times(1)).getQueueUserId(any(String.class), any(String.class));
        ErrorJsonList errorJsonList = mapper.readValue(responseJson, ErrorJsonList.class);
        assertEquals(errorJsonList.getError().getSystemError(), MobileSystemErrorCodeEnum.SEVERE.name());
    }

    @Test
    void queues_pass() throws Exception {
        when(authenticateMobileService.getQueueUserId(anyString(), anyString())).thenReturn("rid");
        when(businessUserStoreService.getQueues(anyString())).thenReturn(topics);
        String responseJson = manageQueueController.getQueues(
                new ScrubbedInput(""),
                new ScrubbedInput(DeviceTypeEnum.A.getName()),
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                response);

        verify(authenticateMobileService, times(1)).getQueueUserId(any(String.class), any(String.class));
        JsonTopicList jsonTopicList = mapper.readValue(responseJson, JsonTopicList.class);
        assertEquals(jsonTopicList.getTopics().size(), 1);
    }

    @Test
    void served_fail_authentication() throws Exception {
        String responseJson = manageQueueController.served(
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                "",
                response);

        verify(authenticateMobileService, times(1)).getQueueUserId(any(String.class), any(String.class));
        assertSame(null, responseJson);
    }

    @Test
    void served_json_parsing_error() throws Exception {
        when(authenticateMobileService.getQueueUserId(anyString(), anyString())).thenReturn("1234");
        String responseJson = manageQueueController.served(
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                "",
                response);

        verify(authenticateMobileService, times(1)).getQueueUserId(any(String.class), any(String.class));
                
        ErrorJsonList errorJsonList = mapper.readValue(responseJson, ErrorJsonList.class);
        assertEquals(errorJsonList.getError().getSystemError(), MobileSystemErrorCodeEnum.SEVERE.name());
    }

    @Test
    void served() throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("qr", "queuecode");
        json.addProperty("t", "1");
        json.addProperty("q", QueueUserStateEnum.S.getName());
        json.addProperty("s", QueueStatusEnum.N.getName());
        json.addProperty("g", "GoTo");
        String jsonRequest = new Gson().toJson(json);

        tokenQueue.setQueueStatus(QueueStatusEnum.N);

        when(authenticateMobileService.getQueueUserId(anyString(), anyString())).thenReturn("1234");
        when(businessUserStoreService.hasAccess(anyString(), anyString())).thenReturn(true);
        when(queueService.updateAndGetNextInQueue(
                anyString(),
                anyInt(),
                ArgumentMatchers.any(QueueUserStateEnum.class),
                anyString(),
                anyString(),
                ArgumentMatchers.any(TokenServiceEnum.class))
        ).thenReturn(new JsonToken("queuecode", BusinessTypeEnum.DO)
                .setQueueStatus(QueueStatusEnum.D));
        when(tokenQueueService.findByCodeQR(anyString())).thenReturn(tokenQueue);

        String responseJson = manageQueueController.served(
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                jsonRequest,
                response);

        verify(authenticateMobileService, times(1)).getQueueUserId(any(String.class), any(String.class));
        verify(businessUserStoreService, times(1)).hasAccess(any(String.class), any(String.class));
        verify(queueService, times(1)).updateAndGetNextInQueue(any(String.class), any(Integer.class), ArgumentMatchers.any(QueueUserStateEnum.class), anyString(), anyString(), ArgumentMatchers.any(TokenServiceEnum.class));

        JsonObject jo = (JsonObject) new JsonParser().parse(responseJson);
        assertEquals("queuecode", jo.get("qr").getAsString());
        assertEquals(QueueStatusEnum.D, QueueStatusEnum.valueOf(jo.get("q").getAsString()));
    }

    @Test
    void served_code_queue_fail() throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("qr", "");
        json.addProperty("t", "1");
        json.addProperty("q", QueueUserStateEnum.S.getName());
        json.addProperty("s", QueueStatusEnum.N.getName());
        json.addProperty("g", "some counter");
        String jsonRequest = new Gson().toJson(json);

        when(authenticateMobileService.getQueueUserId(anyString(), anyString())).thenReturn("1234");
        when(businessUserStoreService.hasAccess(anyString(), anyString())).thenReturn(true);
        when(queueService.updateAndGetNextInQueue(anyString(), anyInt(), ArgumentMatchers.any(QueueUserStateEnum.class), anyString(), anyString(), ArgumentMatchers.any(TokenServiceEnum.class))).thenReturn(new JsonToken("queuecode", BusinessTypeEnum.DO));

        String responseJson = manageQueueController.served(
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                jsonRequest,
                response);

        verify(authenticateMobileService, times(1)).getQueueUserId(any(String.class), any(String.class));

        ErrorJsonList errorJsonList = mapper.readValue(responseJson, ErrorJsonList.class);
        assertEquals(errorJsonList.getError().getSystemError(), MobileSystemErrorCodeEnum.MOBILE_JSON.name());
    }

    @Test
    void served_code_queue_authentication_fail() throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("qr", "queueCode");
        json.addProperty("t", "1");
        json.addProperty("q", QueueUserStateEnum.S.getName());
        json.addProperty("s", QueueStatusEnum.N.getName());
        json.addProperty("g", "some counter");
        String jsonRequest = new Gson().toJson(json);

        when(authenticateMobileService.getQueueUserId(anyString(), anyString())).thenReturn("1234");
        when(businessUserStoreService.hasAccess(anyString(), anyString())).thenReturn(false);

        String responseJson = manageQueueController.served(
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                jsonRequest,
                response);

        verify(authenticateMobileService, times(1)).getQueueUserId(any(String.class), any(String.class));
        verify(businessUserStoreService, times(1)).hasAccess(any(String.class), any(String.class));

        assertSame(null, responseJson);
    }

    @Test
    void served_serve_number_fail() throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("c", "111");
        json.addProperty("t", "a");
        json.addProperty("q", QueueUserStateEnum.S.getName());
        json.addProperty("s", QueueStatusEnum.N.getName());
        json.addProperty("g", "some counter");
        String jsonRequest = new Gson().toJson(json);

        when(authenticateMobileService.getQueueUserId(anyString(), anyString())).thenReturn("1234");
        when(businessUserStoreService.hasAccess(anyString(), anyString())).thenReturn(true);

        String responseJson = manageQueueController.served(
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                jsonRequest,
                response);

        verify(authenticateMobileService, times(1)).getQueueUserId(any(String.class), any(String.class));

        ErrorJsonList errorJsonList = mapper.readValue(responseJson, ErrorJsonList.class);
        assertEquals(errorJsonList.getError().getSystemError(), MobileSystemErrorCodeEnum.MOBILE_JSON.name());
    }

    @Test
    void served_queue_state_fail() throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("c", "111");
        json.addProperty("t", "1");
        json.addProperty("q", "ZZ");
        json.addProperty("s", QueueStatusEnum.N.getName());
        json.addProperty("g", "some counter");
        String jsonRequest = new Gson().toJson(json);

        when(authenticateMobileService.getQueueUserId(anyString(), anyString())).thenReturn("1234");
        when(businessUserStoreService.hasAccess(anyString(), anyString())).thenReturn(true);

        String responseJson = manageQueueController.served(
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                jsonRequest,
                response);

        verify(authenticateMobileService, times(1)).getQueueUserId(any(String.class), any(String.class));

        ErrorJsonList errorJsonList = mapper.readValue(responseJson, ErrorJsonList.class);
        assertEquals(errorJsonList.getError().getSystemError(), MobileSystemErrorCodeEnum.MOBILE_JSON.name());
    }

    @Test
    void served_queue_status_fail() throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("c", "111");
        json.addProperty("t", "1");
        json.addProperty("q", QueueUserStateEnum.S.getName());
        json.addProperty("s", "bbbb");
        json.addProperty("g", "some counter");
        String jsonRequest = new Gson().toJson(json);

        when(authenticateMobileService.getQueueUserId(anyString(), anyString())).thenReturn("1234");
        when(businessUserStoreService.hasAccess(anyString(), anyString())).thenReturn(true);

        String responseJson = manageQueueController.served(
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                jsonRequest,
                response);

        verify(authenticateMobileService, times(1)).getQueueUserId(any(String.class), any(String.class));

        ErrorJsonList errorJsonList = mapper.readValue(responseJson, ErrorJsonList.class);
        assertEquals(errorJsonList.getError().getSystemError(), MobileSystemErrorCodeEnum.MOBILE_JSON.name());
    }

    @Test
    void served_fail() throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("qr", "queuecode");
        json.addProperty("t", "1");
        json.addProperty("q", QueueUserStateEnum.S.getName());
        json.addProperty("s", QueueStatusEnum.N.getName());
        json.addProperty("g", "some counter");
        String jsonRequest = new Gson().toJson(json);

        tokenQueue.setQueueStatus(QueueStatusEnum.N);

        when(authenticateMobileService.getQueueUserId(anyString(), anyString())).thenReturn("1234");
        when(businessUserStoreService.hasAccess(anyString(), anyString())).thenReturn(true);
        when(queueService.updateAndGetNextInQueue(anyString(), anyInt(), ArgumentMatchers.any(QueueUserStateEnum.class), anyString(), anyString(), ArgumentMatchers.any(TokenServiceEnum.class))).thenReturn(null);
        when(tokenQueueService.findByCodeQR(anyString())).thenReturn(tokenQueue);

        String responseJson = manageQueueController.served(
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                jsonRequest,
                response);

        verify(authenticateMobileService, times(1)).getQueueUserId(any(String.class), any(String.class));
        verify(businessUserStoreService, times(1)).hasAccess(any(String.class), any(String.class));
        verify(queueService, times(1)).updateAndGetNextInQueue(any(String.class), any(Integer.class), ArgumentMatchers.any(QueueUserStateEnum.class), anyString(), anyString(), ArgumentMatchers.any(TokenServiceEnum.class));

        ErrorJsonList errorJsonList = mapper.readValue(responseJson, ErrorJsonList.class);
        assertEquals(errorJsonList.getError().getSystemError(), MobileSystemErrorCodeEnum.SEVERE.name());
    }

    @Disabled
    @DisplayName("Dispense Token does not work. Some mockito issue.")
    void dispenseToken() throws Exception {
        tokenQueue.setQueueStatus(QueueStatusEnum.N);
        JsonToken jsonToken = new JsonToken(tokenQueue);
        jsonToken.setToken(11);

        when(authenticateMobileService.getQueueUserId(anyString(), anyString())).thenReturn("1234");
        when(tokenQueueMobileService.getBizService().findByCodeQR(anyString())).thenReturn(new BizStoreEntity().setAverageServiceTime(100));
        when(tokenQueueMobileService.joinQueue(anyString(), anyString(), anyString(), anyString(), anyLong(), ArgumentMatchers.any(TokenServiceEnum.class))).thenReturn(jsonToken);

        String responseJson = manageQueueController.dispenseTokenWithoutClientInfo(
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                response);

//        verify(authenticateMobileService, times(1)).getQueueUserId(any(String.class), any(String.class));
//        verify(tokenQueueMobileService.getBizService(), times(1)).findByCodeQR(any(String.class));
//        verify(tokenQueueMobileService, times(1)).joinQueue(anyString(), anyString(), anyString(), anyLong());

        JsonObject jo = (JsonObject) new JsonParser().parse(responseJson);
        assertEquals(11, jo.get("t").getAsInt());
    }
}
