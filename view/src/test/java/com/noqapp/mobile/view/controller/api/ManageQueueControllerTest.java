package com.noqapp.mobile.view.controller.api;

import static com.noqapp.mobile.common.util.ErrorEncounteredJson.ERROR;
import static com.noqapp.mobile.common.util.ErrorEncounteredJson.REASON;
import static com.noqapp.mobile.common.util.ErrorEncounteredJson.SYSTEM_ERROR;
import static com.noqapp.mobile.common.util.ErrorEncounteredJson.SYSTEM_ERROR_CODE;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.USER_INPUT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.noqapp.mobile.view.controller.api.merchant.ManageQueueController;
import com.noqapp.domain.json.JsonToken;
import com.noqapp.domain.types.QueueStatusEnum;
import com.noqapp.domain.types.QueueUserStateEnum;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.service.QueueMobileService;
import com.noqapp.service.BusinessUserStoreService;
import com.noqapp.utils.ScrubbedInput;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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
public class ManageQueueControllerTest {

    @Mock private AuthenticateMobileService authenticateMobileService;
    @Mock private QueueMobileService queueMobileService;
    @Mock private BusinessUserStoreService businessUserStoreService;
    @Mock private HttpServletResponse response;

    private ManageQueueController manageQueueController;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        manageQueueController = new ManageQueueController(authenticateMobileService, queueMobileService, businessUserStoreService);
    }

    @Test
    public void getState_fail_authentication() throws Exception {
        String responseJson = manageQueueController.served(
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                "",
                response);

        verify(authenticateMobileService, times(1)).getReceiptUserId(any(String.class), any(String.class));
        assertSame(null, responseJson);
    }

    @Test
    public void served_json_parsing_error() throws Exception {
        when(authenticateMobileService.getReceiptUserId(anyString(), anyString())).thenReturn("1234");
        String responseJson = manageQueueController.served(
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                "",
                response);

        verify(authenticateMobileService, times(1)).getReceiptUserId(any(String.class), any(String.class));

        JsonObject jo = (JsonObject) new JsonParser().parse(responseJson);
        assertEquals(SEVERE.getCode(), jo.get(ERROR).getAsJsonObject().get(SYSTEM_ERROR_CODE).getAsString());
        assertEquals(SEVERE.name(), jo.get(ERROR).getAsJsonObject().get(SYSTEM_ERROR).getAsString());
        assertEquals("Something went wrong. Engineers are looking into this.", jo.get(ERROR).getAsJsonObject().get(REASON).getAsString());
    }

    @Test
    public void served() throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("c", "queuecode");
        json.addProperty("t", "1");
        json.addProperty("q", QueueUserStateEnum.S.getName());
        json.addProperty("s", QueueStatusEnum.N.getName());
        String jsonRequest = new Gson().toJson(json);

        when(authenticateMobileService.getReceiptUserId(anyString(), anyString())).thenReturn("1234");
        when(businessUserStoreService.hasAccess(anyString(), anyString())).thenReturn(true);
        when(queueMobileService.updateAndGetNextInQueue(anyString(), anyInt(), Matchers.any(QueueUserStateEnum.class))).thenReturn(new JsonToken("queuecode"));

        String responseJson = manageQueueController.served(
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                jsonRequest,
                response);

        verify(authenticateMobileService, times(1)).getReceiptUserId(any(String.class), any(String.class));
        verify(businessUserStoreService, times(1)).hasAccess(any(String.class), any(String.class));
        verify(queueMobileService, times(1)).updateAndGetNextInQueue(any(String.class), any(Integer.class), Matchers.any(QueueUserStateEnum.class));

        JsonObject jo = (JsonObject) new JsonParser().parse(responseJson);
        assertEquals("queuecode", jo.get("c").getAsString());
    }

    @Test
    public void served_code_queue_fail() throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("c", "");
        json.addProperty("t", "1");
        json.addProperty("q", QueueUserStateEnum.S.getName());
        json.addProperty("s", QueueStatusEnum.N.getName());
        String jsonRequest = new Gson().toJson(json);

        when(authenticateMobileService.getReceiptUserId(anyString(), anyString())).thenReturn("1234");
        when(businessUserStoreService.hasAccess(anyString(), anyString())).thenReturn(true);
        when(queueMobileService.updateAndGetNextInQueue(anyString(), anyInt(), Matchers.any(QueueUserStateEnum.class))).thenReturn(new JsonToken("queuecode"));

        String responseJson = manageQueueController.served(
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                jsonRequest,
                response);

        verify(authenticateMobileService, times(1)).getReceiptUserId(any(String.class), any(String.class));

        JsonObject jo = (JsonObject) new JsonParser().parse(responseJson);
        assertEquals(USER_INPUT.getCode(), jo.get(ERROR).getAsJsonObject().get(SYSTEM_ERROR_CODE).getAsString());
        assertEquals(USER_INPUT.name(), jo.get(ERROR).getAsJsonObject().get(SYSTEM_ERROR).getAsString());
        assertEquals("Not a valid queue code.", jo.get(ERROR).getAsJsonObject().get(REASON).getAsString());
    }

    @Test
    public void served_code_queue_authentication_fail() throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("c", "queueCode");
        json.addProperty("t", "1");
        json.addProperty("q", QueueUserStateEnum.S.getName());
        json.addProperty("s", QueueStatusEnum.N.getName());
        String jsonRequest = new Gson().toJson(json);

        when(authenticateMobileService.getReceiptUserId(anyString(), anyString())).thenReturn("1234");
        when(businessUserStoreService.hasAccess(anyString(), anyString())).thenReturn(false);

        String responseJson = manageQueueController.served(
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                jsonRequest,
                response);

        verify(authenticateMobileService, times(1)).getReceiptUserId(any(String.class), any(String.class));
        verify(businessUserStoreService, times(1)).hasAccess(any(String.class), any(String.class));

        assertSame(null, responseJson);
    }

    @Test
    public void served_serve_number_fail() throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("c", "111");
        json.addProperty("t", "a");
        json.addProperty("q", QueueUserStateEnum.S.getName());
        json.addProperty("s", QueueStatusEnum.N.getName());
        String jsonRequest = new Gson().toJson(json);

        when(authenticateMobileService.getReceiptUserId(anyString(), anyString())).thenReturn("1234");
        when(businessUserStoreService.hasAccess(anyString(), anyString())).thenReturn(true);

        String responseJson = manageQueueController.served(
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                jsonRequest,
                response);

        verify(authenticateMobileService, times(1)).getReceiptUserId(any(String.class), any(String.class));

        JsonObject jo = (JsonObject) new JsonParser().parse(responseJson);
        assertEquals(USER_INPUT.getCode(), jo.get(ERROR).getAsJsonObject().get(SYSTEM_ERROR_CODE).getAsString());
        assertEquals(USER_INPUT.name(), jo.get(ERROR).getAsJsonObject().get(SYSTEM_ERROR).getAsString());
        assertEquals("Not a valid number.", jo.get(ERROR).getAsJsonObject().get(REASON).getAsString());
    }

    @Test
    public void served_queue_state_fail() throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("c", "111");
        json.addProperty("t", "1");
        json.addProperty("q", "ZZ");
        json.addProperty("s", QueueStatusEnum.N.getName());
        String jsonRequest = new Gson().toJson(json);

        when(authenticateMobileService.getReceiptUserId(anyString(), anyString())).thenReturn("1234");
        when(businessUserStoreService.hasAccess(anyString(), anyString())).thenReturn(true);

        String responseJson = manageQueueController.served(
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                jsonRequest,
                response);

        verify(authenticateMobileService, times(1)).getReceiptUserId(any(String.class), any(String.class));

        JsonObject jo = (JsonObject) new JsonParser().parse(responseJson);
        assertEquals(USER_INPUT.getCode(), jo.get(ERROR).getAsJsonObject().get(SYSTEM_ERROR_CODE).getAsString());
        assertEquals(USER_INPUT.name(), jo.get(ERROR).getAsJsonObject().get(SYSTEM_ERROR).getAsString());
        assertEquals("Not a valid queue user state.", jo.get(ERROR).getAsJsonObject().get(REASON).getAsString());
    }

    @Test
    public void served_fail() throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("c", "queuecode");
        json.addProperty("t", "1");
        json.addProperty("q", QueueUserStateEnum.S.getName());
        json.addProperty("s", QueueStatusEnum.N.getName());
        String jsonRequest = new Gson().toJson(json);

        when(authenticateMobileService.getReceiptUserId(anyString(), anyString())).thenReturn("1234");
        when(businessUserStoreService.hasAccess(anyString(), anyString())).thenReturn(true);
        when(queueMobileService.updateAndGetNextInQueue(anyString(), anyInt(), Matchers.any(QueueUserStateEnum.class))).thenReturn(null);

        String responseJson = manageQueueController.served(
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                new ScrubbedInput(""),
                jsonRequest,
                response);

        verify(authenticateMobileService, times(1)).getReceiptUserId(any(String.class), any(String.class));
        verify(businessUserStoreService, times(1)).hasAccess(any(String.class), any(String.class));
        verify(queueMobileService, times(1)).updateAndGetNextInQueue(any(String.class), any(Integer.class), Matchers.any(QueueUserStateEnum.class));

        JsonObject jo = (JsonObject) new JsonParser().parse(responseJson);
        assertEquals(SEVERE.getCode(), jo.get(ERROR).getAsJsonObject().get(SYSTEM_ERROR_CODE).getAsString());
        assertEquals(SEVERE.name(), jo.get(ERROR).getAsJsonObject().get(SYSTEM_ERROR).getAsString());
        assertEquals("Something went wrong. Engineers are looking into this.", jo.get(ERROR).getAsJsonObject().get(REASON).getAsString());
    }
}