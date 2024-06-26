package com.noqapp.mobile.view.controller.api.merchant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.noqapp.common.errors.ErrorJsonList;
import com.noqapp.common.errors.MobileSystemErrorCodeEnum;
import com.noqapp.common.utils.CommonUtil;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.BusinessUserStoreEntity;
import com.noqapp.domain.TokenQueueEntity;
import com.noqapp.domain.json.JsonBusinessCustomer;
import com.noqapp.domain.json.JsonHour;
import com.noqapp.domain.json.JsonToken;
import com.noqapp.domain.json.JsonTopic;
import com.noqapp.domain.json.JsonTopicList;
import com.noqapp.domain.types.BusinessTypeEnum;
import com.noqapp.domain.types.DeviceTypeEnum;
import com.noqapp.domain.types.QueueStatusEnum;
import com.noqapp.domain.types.QueueUserStateEnum;
import com.noqapp.domain.types.TokenServiceEnum;
import com.noqapp.domain.types.UserLevelEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.medical.service.MedicalRecordService;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.service.MerchantExtendingJoinService;
import com.noqapp.mobile.service.QueueMobileService;
import com.noqapp.mobile.service.TokenQueueMobileService;
import com.noqapp.mobile.view.controller.api.merchant.queue.QueueController;
import com.noqapp.service.BizService;
import com.noqapp.service.BusinessUserStoreService;
import com.noqapp.service.DeviceService;
import com.noqapp.service.JoinAbortService;
import com.noqapp.service.NotifyMobileService;
import com.noqapp.service.PurchaseOrderService;
import com.noqapp.service.QueueService;
import com.noqapp.service.TokenQueueService;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.BeforeEach;
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
@SuppressWarnings({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@DisplayName("Queue")
class QueueControllerTest {

    @Mock private AuthenticateMobileService authenticateMobileService;
    @Mock private QueueService queueService;
    @Mock private QueueMobileService queueMobileService;
    @Mock private TokenQueueService tokenQueueService;
    @Mock private BusinessUserStoreService businessUserStoreService;
    @Mock private ApiHealthService apiHealthService;
    @Mock private BizService bizService;
    @Mock private MerchantExtendingJoinService merchantExtendingJoinService;
    @Mock private TokenQueueMobileService tokenQueueMobileService;
    @Mock private JoinAbortService joinAbortService;
    @Mock private MedicalRecordService medicalRecordService;
    @Mock private PurchaseOrderService purchaseOrderService;
    @Mock private DeviceService deviceService;
    @Mock private NotifyMobileService notifyMobileService;

    @Mock private HttpServletResponse response;

    private QueueController queueController;
    private TokenQueueEntity tokenQueue;
    private ObjectMapper mapper;
    private JsonTopicList jsonTopicList;
    private List<JsonTopic> topics;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        queueController = new QueueController(
            20,
            authenticateMobileService,
            queueService,
            queueMobileService,
            businessUserStoreService,
            tokenQueueService,
            tokenQueueMobileService,
            joinAbortService,
            purchaseOrderService,
            medicalRecordService,
            deviceService,
            merchantExtendingJoinService,
            notifyMobileService,
            apiHealthService);

        mapper = new ObjectMapper();

        tokenQueue = new TokenQueueEntity("topic", "displayName");
        tokenQueue.setLastNumber(10);
        tokenQueue.setCurrentlyServing(5);
        tokenQueue.setId("codeQR");
        tokenQueue.setBusinessType(BusinessTypeEnum.DO);

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
        String responseJson = queueController.getQueues(
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
        doThrow(new RuntimeException()).when(businessUserStoreService).getAssignedTokenAndQueues(anyString());

        String responseJson = queueController.getQueues(
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
        when(businessUserStoreService.getAssignedTokenAndQueues(anyString())).thenReturn(topics);
        String responseJson = queueController.getQueues(
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
        String responseJson = queueController.served(
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
        String responseJson = queueController.served(
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

        String responseJson = queueController.served(
            new ScrubbedInput(""),
            new ScrubbedInput(""),
            new ScrubbedInput(""),
            new ScrubbedInput(""),
            jsonRequest,
            response);

        verify(authenticateMobileService, times(1)).getQueueUserId(any(String.class), any(String.class));
        verify(businessUserStoreService, times(1)).hasAccess(any(String.class), any(String.class));
        verify(queueService, times(1)).updateAndGetNextInQueue(any(String.class), any(Integer.class), ArgumentMatchers.any(QueueUserStateEnum.class), anyString(), anyString(), ArgumentMatchers.any(TokenServiceEnum.class));

        JsonObject jo = (JsonObject) JsonParser.parseString(responseJson);
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

        String responseJson = queueController.served(
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

        String responseJson = queueController.served(
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

        String responseJson = queueController.served(
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

        String responseJson = queueController.served(
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

        String responseJson = queueController.served(
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

        when(authenticateMobileService.getQueueUserId(anyString(), anyString())).thenReturn("qid");
        when(businessUserStoreService.hasAccess(anyString(), anyString())).thenReturn(true);
        when(queueService.updateAndGetNextInQueue(anyString(), anyInt(), ArgumentMatchers.any(QueueUserStateEnum.class), anyString(), anyString(), ArgumentMatchers.any(TokenServiceEnum.class))).thenReturn(null);
        when(tokenQueueService.findByCodeQR(anyString())).thenReturn(tokenQueue);

        String responseJson = queueController.served(
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

    @Test
    @DisplayName("Dispense Token fails for CD or CDQ when user level is store manager")
    void dispenseToken_CSD_pass() throws Exception {
        tokenQueue.setQueueStatus(QueueStatusEnum.N);
        JsonToken jsonToken = new JsonToken(tokenQueue);
        jsonToken.setToken(11);

        BizStoreEntity bizStore = new BizStoreEntity()
            .setDisplayName("Store")
            .setAverageServiceTime(100)
            .setCodeQR("codeQR")
            .setBusinessType(BusinessTypeEnum.CDQ);

        BusinessUserStoreEntity businessUserStore = new BusinessUserStoreEntity(
            "100000000003",
            CommonUtil.generateHexFromObjectId(),
            CommonUtil.generateHexFromObjectId(),
            CommonUtil.generateHexFromObjectId(),
            UserLevelEnum.S_MANAGER);

        when(authenticateMobileService.getQueueUserId(anyString(), anyString())).thenReturn("qid");
        when(tokenQueueMobileService.getBizService()).thenReturn(bizService);
        when(businessUserStoreService.hasAccess(anyString(), anyString())).thenReturn(true);
        when(businessUserStoreService.findOneByQidAndCodeQR(anyString(), anyString())).thenReturn(businessUserStore);
        when(tokenQueueMobileService.getBizService().findByCodeQR(anyString())).thenReturn(bizStore);
        when(merchantExtendingJoinService.dispenseTokenWithClientInfo(anyString(), any(JsonBusinessCustomer.class), any(BizStoreEntity.class))).thenReturn(jsonToken.asJson());

        JsonBusinessCustomer jsonBusinessCustomer = new JsonBusinessCustomer();
        jsonBusinessCustomer
            .setCustomerPhone(new ScrubbedInput("customerPhone"))
            .setCustomerName(new ScrubbedInput("customerName"))
            .setCodeQR(new ScrubbedInput("codeQR"))
            .setRegisteredUser(false);

        String responseJson = queueController.dispenseTokenWithClientInfo(
            new ScrubbedInput(""),
            new ScrubbedInput(""),
            new ScrubbedInput(""),
            new ScrubbedInput(""),
            jsonBusinessCustomer,
            response);

        verify(authenticateMobileService, times(1)).getQueueUserId(any(String.class), any(String.class));
        verify(tokenQueueMobileService.getBizService(), times(1)).findByCodeQR(any(String.class));
        verify(merchantExtendingJoinService, times(1)).dispenseTokenWithClientInfo(anyString(), any(JsonBusinessCustomer.class), any(BizStoreEntity.class));

        JsonObject jo = (JsonObject) new JsonParser().parse(responseJson);
        assertEquals(11, jo.get("t").getAsInt());
    }

    @Test
    @DisplayName("Dispense Token fails for CD or CDQ when user level is store supervisor")
    void dispenseToken_CSD_fail() throws Exception {
        tokenQueue.setQueueStatus(QueueStatusEnum.N);
        JsonToken jsonToken = new JsonToken(tokenQueue);
        jsonToken.setToken(11);

        BizStoreEntity bizStore = new BizStoreEntity()
            .setDisplayName("Store")
            .setAverageServiceTime(100)
            .setCodeQR("codeQR")
            .setBusinessType(BusinessTypeEnum.CDQ);

        BusinessUserStoreEntity businessUserStore = new BusinessUserStoreEntity(
            "100000000003",
            CommonUtil.generateHexFromObjectId(),
            CommonUtil.generateHexFromObjectId(),
            CommonUtil.generateHexFromObjectId(),
            UserLevelEnum.Q_SUPERVISOR);

        when(authenticateMobileService.getQueueUserId(anyString(), anyString())).thenReturn("qid");
        when(tokenQueueMobileService.getBizService()).thenReturn(bizService);
        when(businessUserStoreService.hasAccess(anyString(), anyString())).thenReturn(true);
        when(businessUserStoreService.findOneByQidAndCodeQR(anyString(), anyString())).thenReturn(businessUserStore);
        when(tokenQueueMobileService.getBizService().findByCodeQR(anyString())).thenReturn(bizStore);
        when(merchantExtendingJoinService.dispenseTokenWithClientInfo(anyString(), any(JsonBusinessCustomer.class), any(BizStoreEntity.class))).thenReturn(jsonToken.asJson());

        JsonBusinessCustomer jsonBusinessCustomer = new JsonBusinessCustomer();
        jsonBusinessCustomer
            .setCustomerPhone(new ScrubbedInput("customerPhone"))
            .setCustomerName(new ScrubbedInput("customerName"))
            .setCodeQR(new ScrubbedInput("codeQR"))
            .setRegisteredUser(false);

        String responseJson = queueController.dispenseTokenWithClientInfo(
            new ScrubbedInput(""),
            new ScrubbedInput(""),
            new ScrubbedInput(""),
            new ScrubbedInput(""),
            jsonBusinessCustomer,
            response);

        verify(authenticateMobileService, times(1)).getQueueUserId(any(String.class), any(String.class));
        verify(tokenQueueMobileService.getBizService(), times(1)).findByCodeQR(any(String.class));
        verify(merchantExtendingJoinService, times(0)).dispenseTokenWithClientInfo(anyString(), any(JsonBusinessCustomer.class), any(BizStoreEntity.class));

        ErrorJsonList errorJsonList = new ObjectMapper().readValue(responseJson, ErrorJsonList.class);
        assertEquals(errorJsonList.getError().getSystemError(), MobileSystemErrorCodeEnum.QUEUE_TOKEN_LIMIT.name());
    }

    @Test
    @DisplayName("Dispense Token fails for RS when user level is store manager")
    void dispenseToken_RS_pass() throws Exception {
        tokenQueue.setQueueStatus(QueueStatusEnum.N);
        JsonToken jsonToken = new JsonToken(tokenQueue);
        jsonToken.setToken(11);

        BizStoreEntity bizStore = new BizStoreEntity()
            .setDisplayName("Store")
            .setAverageServiceTime(100)
            .setCodeQR("codeQR")
            .setBusinessType(BusinessTypeEnum.RS);

        BusinessUserStoreEntity businessUserStore = new BusinessUserStoreEntity(
            "100000000003",
            CommonUtil.generateHexFromObjectId(),
            CommonUtil.generateHexFromObjectId(),
            CommonUtil.generateHexFromObjectId(),
            UserLevelEnum.S_MANAGER);

        when(authenticateMobileService.getQueueUserId(anyString(), anyString())).thenReturn("qid");
        when(tokenQueueMobileService.getBizService()).thenReturn(bizService);
        when(businessUserStoreService.hasAccess(anyString(), anyString())).thenReturn(true);
        when(businessUserStoreService.findOneByQidAndCodeQR(anyString(), anyString())).thenReturn(businessUserStore);
        when(tokenQueueMobileService.getBizService().findByCodeQR(anyString())).thenReturn(bizStore);
        when(merchantExtendingJoinService.dispenseTokenWithClientInfo(anyString(), any(JsonBusinessCustomer.class), any(BizStoreEntity.class))).thenReturn(jsonToken.asJson());

        JsonBusinessCustomer jsonBusinessCustomer = new JsonBusinessCustomer();
        jsonBusinessCustomer
            .setCustomerPhone(new ScrubbedInput("customerPhone"))
            .setCustomerName(new ScrubbedInput("customerName"))
            .setCodeQR(new ScrubbedInput("codeQR"))
            .setRegisteredUser(false);

        String responseJson = queueController.dispenseTokenWithClientInfo(
            new ScrubbedInput(""),
            new ScrubbedInput(""),
            new ScrubbedInput(""),
            new ScrubbedInput(""),
            jsonBusinessCustomer,
            response);

        verify(authenticateMobileService, times(1)).getQueueUserId(any(String.class), any(String.class));
        verify(tokenQueueMobileService.getBizService(), times(1)).findByCodeQR(any(String.class));
        verify(merchantExtendingJoinService, times(1)).dispenseTokenWithClientInfo(anyString(), any(JsonBusinessCustomer.class), any(BizStoreEntity.class));

        JsonObject jo = (JsonObject) new JsonParser().parse(responseJson);
        assertEquals(11, jo.get("t").getAsInt());
    }
}
