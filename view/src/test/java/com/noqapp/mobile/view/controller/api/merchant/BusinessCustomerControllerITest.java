package com.noqapp.mobile.view.controller.api.merchant;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BusinessUserStoreEntity;
import com.noqapp.domain.UserAccountEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.json.JsonPurchaseOrder;
import com.noqapp.domain.json.JsonQueuedPerson;
import com.noqapp.domain.json.JsonResponse;
import com.noqapp.domain.json.JsonToken;
import com.noqapp.domain.types.ActionTypeEnum;
import com.noqapp.domain.types.AppFlavorEnum;
import com.noqapp.domain.types.BusinessCustomerAttributeEnum;
import com.noqapp.domain.types.CustomerPriorityLevelEnum;
import com.noqapp.domain.types.DeviceTypeEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.domain.body.client.JoinQueue;
import com.noqapp.mobile.domain.body.client.QueueAuthorize;
import com.noqapp.mobile.domain.body.merchant.CustomerPriority;
import com.noqapp.mobile.service.AccountMobileService;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.service.TokenQueueMobileService;
import com.noqapp.mobile.view.ITest;
import com.noqapp.mobile.view.controller.api.client.TokenQueueAPIController;
import com.noqapp.service.AccountService;
import com.noqapp.service.BusinessCustomerService;
import com.noqapp.service.BusinessUserStoreService;
import com.noqapp.service.QueueService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jr.ob.JSON;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * hitender
 * 5/27/20 9:55 AM
 */
@DisplayName("Business Customer Controller API")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("api")
class BusinessCustomerControllerITest extends ITest {

    private BusinessCustomerController businessCustomerController;
    private TokenQueueAPIController tokenQueueAPIController;

    @BeforeEach
    void setUp() throws IOException {
        businessCustomerController = new BusinessCustomerController(
            authenticateMobileService,
            accountService,
            businessCustomerService,
            businessUserStoreService,
            queueService,
            accountMobileService,
            tokenQueueMobileService,
            apiHealthService
        );

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

        UserProfileEntity queueManagerUserProfile = accountService.checkUserExistsByPhone("9118000001102");
        List<BusinessUserStoreEntity> businessUserStores = businessUserStoreService.findAllStoreQueueAssociated(queueManagerUserProfile.getQueueUserId());
        UserAccountEntity userAccount = accountService.findByQueueUserId(queueManagerUserProfile.getQueueUserId());
        QueueAuthorize queueAuthorize = new QueueAuthorize()
            .setFirstCustomerId(new ScrubbedInput("G-9118000001102"))
            .setAdditionalCustomerId(new ScrubbedInput("L-9118000001102"))
            .setCodeQR(new ScrubbedInput(businessUserStores.get(0).getCodeQR()));
        tokenQueueAPIController.businessApprove(
            new ScrubbedInput(did),
            new ScrubbedInput(deviceType),
            new ScrubbedInput(userAccount.getUserId()),
            new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
            queueAuthorize,
            httpServletResponse
        );
    }

    @Test
    void accessAction_Approve_FirstTime() throws Exception {
        UserProfileEntity queueManagerUserProfile = accountService.checkUserExistsByPhone("9118000001102");
        List<BusinessUserStoreEntity> businessUserStores = businessUserStoreService.findAllStoreQueueAssociated(queueManagerUserProfile.getQueueUserId());
        UserAccountEntity userAccount = accountService.findByQueueUserId(queueManagerUserProfile.getQueueUserId());

        JoinQueue joinQueue = new JoinQueue().setQueueUserId(userAccount.getQueueUserId()).setCodeQR(businessUserStores.get(0).getCodeQR());
        String jsonTokenResponse = tokenQueueAPIController.joinQueue(
            new ScrubbedInput(did),
            new ScrubbedInput(deviceType),
            new ScrubbedInput(userAccount.getUserId()),
            new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
            joinQueue,
            httpServletResponse
        );
        JsonToken jsonToken = new ObjectMapper().readValue(jsonTokenResponse, JsonToken.class);
        assertTrue(jsonToken.getToken() > 0);

        CustomerPriority customerPriority = new CustomerPriority()
            .setQueueUserId(new ScrubbedInput(queueManagerUserProfile.getQueueUserId()))
            .setActionType(ActionTypeEnum.APPROVE)
            .setCodeQR(new ScrubbedInput(businessUserStores.get(0).getCodeQR()))
            .setCustomerPriorityLevel(CustomerPriorityLevelEnum.I)
            .setBusinessCustomerAttributes(new ArrayList<>() {{ add(BusinessCustomerAttributeEnum.AP); }});
        String jsonQueuedPersonResponse = businessCustomerController.accessAction(
            new ScrubbedInput(did),
            new ScrubbedInput(deviceType),
            new ScrubbedInput(userAccount.getUserId()),
            new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
            customerPriority,
            httpServletResponse
        );

        JsonQueuedPerson jsonQueuedPerson = new ObjectMapper().readValue(jsonQueuedPersonResponse, JsonQueuedPerson.class);
        assertEquals(jsonToken.getToken(), jsonQueuedPerson.getToken());
        assertTrue(jsonQueuedPerson.getBusinessCustomerAttributes().contains(BusinessCustomerAttributeEnum.AP));

        /* Join second queue. */
        joinQueue = new JoinQueue().setQueueUserId(userAccount.getQueueUserId()).setCodeQR(businessUserStores.get(1).getCodeQR());
        jsonTokenResponse = tokenQueueAPIController.joinQueue(
            new ScrubbedInput(did),
            new ScrubbedInput(deviceType),
            new ScrubbedInput(userAccount.getUserId()),
            new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
            joinQueue,
            httpServletResponse
        );
        jsonToken = new ObjectMapper().readValue(jsonTokenResponse, JsonToken.class);
        assertTrue(jsonToken.getToken() > 0);

        customerPriority = new CustomerPriority()
            .setQueueUserId(new ScrubbedInput(queueManagerUserProfile.getQueueUserId()))
            .setActionType(ActionTypeEnum.APPROVE)
            .setCodeQR(new ScrubbedInput(businessUserStores.get(1).getCodeQR()))
            .setCustomerPriorityLevel(CustomerPriorityLevelEnum.I)
            .setBusinessCustomerAttributes(new ArrayList<>() {{ add(BusinessCustomerAttributeEnum.AP); }});
        jsonQueuedPersonResponse = businessCustomerController.accessAction(
            new ScrubbedInput(did),
            new ScrubbedInput(deviceType),
            new ScrubbedInput(userAccount.getUserId()),
            new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
            customerPriority,
            httpServletResponse
        );

        jsonQueuedPerson = new ObjectMapper().readValue(jsonQueuedPersonResponse, JsonQueuedPerson.class);
        assertEquals(jsonToken.getToken(), jsonQueuedPerson.getToken());
        assertTrue(jsonQueuedPerson.getBusinessCustomerAttributes().contains(BusinessCustomerAttributeEnum.AP));
    }

    @Test
    void accessAction_Reject_FirstTime() throws Exception {
        UserProfileEntity queueManagerUserProfile = accountService.checkUserExistsByPhone("9118000001102");
        List<BusinessUserStoreEntity> businessUserStores = businessUserStoreService.findAllStoreQueueAssociated(queueManagerUserProfile.getQueueUserId());
        UserAccountEntity userAccount = accountService.findByQueueUserId(queueManagerUserProfile.getQueueUserId());

        JoinQueue joinQueue = new JoinQueue().setQueueUserId(userAccount.getQueueUserId()).setCodeQR(businessUserStores.get(0).getCodeQR());
        String jsonTokenResponse = tokenQueueAPIController.joinQueue(
            new ScrubbedInput(did),
            new ScrubbedInput(deviceType),
            new ScrubbedInput(userAccount.getUserId()),
            new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
            joinQueue,
            httpServletResponse
        );
        JsonToken jsonToken = new ObjectMapper().readValue(jsonTokenResponse, JsonToken.class);
        assertTrue(jsonToken.getToken() > 0);

        CustomerPriority customerPriority = new CustomerPriority()
            .setQueueUserId(new ScrubbedInput(queueManagerUserProfile.getQueueUserId()))
            .setActionType(ActionTypeEnum.REJECT)
            .setCodeQR(new ScrubbedInput(businessUserStores.get(0).getCodeQR()))
            .setCustomerPriorityLevel(CustomerPriorityLevelEnum.I)
            .setBusinessCustomerAttributes(new ArrayList<>() {{ add(BusinessCustomerAttributeEnum.RJ); }});
        String jsonQueuedPersonResponse = businessCustomerController.accessAction(
            new ScrubbedInput(did),
            new ScrubbedInput(deviceType),
            new ScrubbedInput(userAccount.getUserId()),
            new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
            customerPriority,
            httpServletResponse
        );

        JsonQueuedPerson jsonQueuedPerson = new ObjectMapper().readValue(jsonQueuedPersonResponse, JsonQueuedPerson.class);
        assertEquals(jsonToken.getToken(), jsonQueuedPerson.getToken());
        assertTrue(jsonQueuedPerson.getBusinessCustomerAttributes().contains(BusinessCustomerAttributeEnum.RJ));

        /* Join second queue. */
        joinQueue = new JoinQueue().setQueueUserId(userAccount.getQueueUserId()).setCodeQR(businessUserStores.get(1).getCodeQR());
        jsonTokenResponse = tokenQueueAPIController.joinQueue(
            new ScrubbedInput(did),
            new ScrubbedInput(deviceType),
            new ScrubbedInput(userAccount.getUserId()),
            new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
            joinQueue,
            httpServletResponse
        );
        jsonToken = new ObjectMapper().readValue(jsonTokenResponse, JsonToken.class);
        assertTrue(jsonToken.getToken() > 0);

        customerPriority = new CustomerPriority()
            .setQueueUserId(new ScrubbedInput(queueManagerUserProfile.getQueueUserId()))
            .setActionType(ActionTypeEnum.REJECT)
            .setCodeQR(new ScrubbedInput(businessUserStores.get(1).getCodeQR()))
            .setCustomerPriorityLevel(CustomerPriorityLevelEnum.I)
            .setBusinessCustomerAttributes(new ArrayList<>() {{ add(BusinessCustomerAttributeEnum.RJ); }});
        jsonQueuedPersonResponse = businessCustomerController.accessAction(
            new ScrubbedInput(did),
            new ScrubbedInput(deviceType),
            new ScrubbedInput(userAccount.getUserId()),
            new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
            customerPriority,
            httpServletResponse
        );

        jsonQueuedPerson = new ObjectMapper().readValue(jsonQueuedPersonResponse, JsonQueuedPerson.class);
        assertEquals(jsonToken.getToken(), jsonQueuedPerson.getToken());
        assertTrue(jsonQueuedPerson.getBusinessCustomerAttributes().contains(BusinessCustomerAttributeEnum.RJ));

        accessAction_After_Reject();
    }

    void accessAction_After_Reject() throws Exception {
        UserProfileEntity queueManagerUserProfile = accountService.checkUserExistsByPhone("9118000001102");
        List<BusinessUserStoreEntity> businessUserStores = businessUserStoreService.findAllStoreQueueAssociated(queueManagerUserProfile.getQueueUserId());
        UserAccountEntity userAccount = accountService.findByQueueUserId(queueManagerUserProfile.getQueueUserId());

        JoinQueue joinQueue = new JoinQueue().setQueueUserId(userAccount.getQueueUserId()).setCodeQR(businessUserStores.get(0).getCodeQR());
        String jsonTokenResponse = tokenQueueAPIController.joinQueue(
            new ScrubbedInput(did),
            new ScrubbedInput(deviceType),
            new ScrubbedInput(userAccount.getUserId()),
            new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
            joinQueue,
            httpServletResponse
        );
        JsonToken jsonToken = new ObjectMapper().readValue(jsonTokenResponse, JsonToken.class);
        assertTrue(jsonToken.getToken() > 0);

        /* Join second queue. */
        joinQueue = new JoinQueue().setQueueUserId(userAccount.getQueueUserId()).setCodeQR(businessUserStores.get(1).getCodeQR());
        jsonTokenResponse = tokenQueueAPIController.joinQueue(
            new ScrubbedInput(did),
            new ScrubbedInput(deviceType),
            new ScrubbedInput(userAccount.getUserId()),
            new ScrubbedInput(userAccount.getUserAuthentication().getAuthenticationKey()),
            joinQueue,
            httpServletResponse
        );
        jsonToken = new ObjectMapper().readValue(jsonTokenResponse, JsonToken.class);
        assertTrue(jsonToken.getToken() > 0);
    }
}
