package com.noqapp.mobile.view.controller.api.merchant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.noqapp.domain.TokenQueueEntity;
import com.noqapp.domain.json.JsonToken;
import com.noqapp.domain.json.JsonTopic;
import com.noqapp.domain.json.JsonTopicList;
import com.noqapp.domain.types.DeviceTypeEnum;
import com.noqapp.domain.types.QueueStatusEnum;
import com.noqapp.domain.types.QueueUserStateEnum;
import com.noqapp.mobile.common.util.ErrorJsonList;
import com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.service.QueueMobileService;
import com.noqapp.mobile.types.LowestSupportedAppEnum;
import com.noqapp.service.BusinessUserStoreService;
import com.noqapp.utils.ScrubbedInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
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
    @Mock private QueueMobileService queueMobileService;
    @Mock private BusinessUserStoreService businessUserStoreService;
    @Mock private HttpServletResponse response;
    private TokenQueueEntity tokenQueue;

    private ManageQueueController manageQueueController;
    private ObjectMapper mapper;
    private JsonTopicList jsonTopicList;
    private List<JsonTopic> topics;


    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        manageQueueController = new ManageQueueController(20, authenticateMobileService, queueMobileService, businessUserStoreService);
        mapper = new ObjectMapper();

        tokenQueue = new TokenQueueEntity("topic", "displayName");
        tokenQueue.setLastNumber(10);
        tokenQueue.setCurrentlyServing(5);
        tokenQueue.setId("codeQR");

        JsonTopic topic = new JsonTopic(tokenQueue);
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
        json.addProperty("c", "queuecode");
        json.addProperty("t", "1");
        json.addProperty("q", QueueUserStateEnum.S.getName());
        json.addProperty("s", QueueStatusEnum.N.getName());
        json.addProperty("g", "GoTo");
        String jsonRequest = new Gson().toJson(json);

        tokenQueue.setQueueStatus(QueueStatusEnum.N);

        when(authenticateMobileService.getQueueUserId(anyString(), anyString())).thenReturn("1234");
        when(businessUserStoreService.hasAccess(anyString(), anyString())).thenReturn(true);
        when(queueMobileService.updateAndGetNextInQueue(anyString(), anyInt(), Matchers.any(QueueUserStateEnum.class), anyString(), anyString())).thenReturn(new JsonToken("queuecode"));
        when(queueMobileService.getTokenQueueByCodeQR(anyString())).thenReturn(tokenQueue);

        String responseJson = manageQueueController.served(
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                jsonRequest,
                response);

        verify(authenticateMobileService, times(1)).getQueueUserId(any(String.class), any(String.class));
        verify(businessUserStoreService, times(1)).hasAccess(any(String.class), any(String.class));
        verify(queueMobileService, times(1)).updateAndGetNextInQueue(any(String.class), any(Integer.class), Matchers.any(QueueUserStateEnum.class), anyString(), anyString());

        JsonObject jo = (JsonObject) new JsonParser().parse(responseJson);
        assertEquals("queuecode", jo.get("c").getAsString());
    }

    @Test
    void served_code_queue_fail() throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("c", "");
        json.addProperty("t", "1");
        json.addProperty("q", QueueUserStateEnum.S.getName());
        json.addProperty("s", QueueStatusEnum.N.getName());
        json.addProperty("g", "some counter");
        String jsonRequest = new Gson().toJson(json);

        when(authenticateMobileService.getQueueUserId(anyString(), anyString())).thenReturn("1234");
        when(businessUserStoreService.hasAccess(anyString(), anyString())).thenReturn(true);
        when(queueMobileService.updateAndGetNextInQueue(anyString(), anyInt(), Matchers.any(QueueUserStateEnum.class), anyString(), anyString())).thenReturn(new JsonToken("queuecode"));

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
        json.addProperty("c", "queueCode");
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
        json.addProperty("c", "queuecode");
        json.addProperty("t", "1");
        json.addProperty("q", QueueUserStateEnum.S.getName());
        json.addProperty("s", QueueStatusEnum.N.getName());
        json.addProperty("g", "some counter");
        String jsonRequest = new Gson().toJson(json);

        tokenQueue.setQueueStatus(QueueStatusEnum.N);

        when(authenticateMobileService.getQueueUserId(anyString(), anyString())).thenReturn("1234");
        when(businessUserStoreService.hasAccess(anyString(), anyString())).thenReturn(true);
        when(queueMobileService.updateAndGetNextInQueue(anyString(), anyInt(), Matchers.any(QueueUserStateEnum.class), anyString(), anyString())).thenReturn(null);
        when(queueMobileService.getTokenQueueByCodeQR(anyString())).thenReturn(tokenQueue);

        String responseJson = manageQueueController.served(
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                jsonRequest,
                response);

        verify(authenticateMobileService, times(1)).getQueueUserId(any(String.class), any(String.class));
        verify(businessUserStoreService, times(1)).hasAccess(any(String.class), any(String.class));
        verify(queueMobileService, times(1)).updateAndGetNextInQueue(any(String.class), any(Integer.class), Matchers.any(QueueUserStateEnum.class), anyString(), anyString());

        ErrorJsonList errorJsonList = mapper.readValue(responseJson, ErrorJsonList.class);
        assertEquals(errorJsonList.getError().getSystemError(), MobileSystemErrorCodeEnum.SEVERE.name());
    }
}