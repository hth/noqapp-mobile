package com.noqapp.mobile.view.controller.api.merchant;

import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.ACCOUNT_INACTIVE;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.BUSINESS_CUSTOMER_ID_DOES_NOT_EXISTS;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.BUSINESS_CUSTOMER_ID_EXISTS;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.MOBILE_JSON;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.USER_INPUT;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.USER_NOT_FOUND;
import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.common.utils.CommonUtil.UNAUTHORIZED;
import static com.noqapp.mobile.view.controller.api.client.TokenQueueAPIController.authorizeRequest;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

import com.noqapp.common.errors.ErrorEncounteredJson;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.common.utils.Validate;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.BusinessCustomerEntity;
import com.noqapp.domain.BusinessUserStoreEntity;
import com.noqapp.domain.QueueEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.helper.CommonHelper;
import com.noqapp.domain.json.JsonBusinessCustomer;
import com.noqapp.domain.json.JsonBusinessCustomerLookup;
import com.noqapp.domain.json.JsonQueuedPerson;
import com.noqapp.domain.types.BusinessCustomerAttributeEnum;
import com.noqapp.domain.types.CustomerPriorityLevelEnum;
import com.noqapp.domain.types.MessageOriginEnum;
import com.noqapp.domain.types.OnOffEnum;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.domain.body.merchant.CustomerPriority;
import com.noqapp.mobile.service.AccountMobileService;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.service.TokenQueueMobileService;
import com.noqapp.mobile.view.controller.open.DeviceController;
import com.noqapp.service.AccountService;
import com.noqapp.service.BusinessCustomerService;
import com.noqapp.service.BusinessUserStoreService;
import com.noqapp.service.QueueService;
import com.noqapp.social.exception.AccountNotActiveException;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

/**
 * Helps find client details when adding client to queue. Add client to business and find dependents.
 * hitender
 * 6/17/18 1:54 PM
 */
@SuppressWarnings ({
        "PMD.BeanMembersShouldSerialize",
        "PMD.LocalVariableCouldBeFinal",
        "PMD.MethodArgumentCouldBeFinal",
        "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/api/m/bc")
public class BusinessCustomerController {
    private static final Logger LOG = LoggerFactory.getLogger(BusinessCustomerController.class);

    private AuthenticateMobileService authenticateMobileService;
    private AccountService accountService;
    private BusinessCustomerService businessCustomerService;
    private BusinessUserStoreService businessUserStoreService;
    private QueueService queueService;
    private AccountMobileService accountMobileService;
    private TokenQueueMobileService tokenQueueMobileService;
    private ApiHealthService apiHealthService;

    @Autowired
    public BusinessCustomerController(
        AuthenticateMobileService authenticateMobileService,
        AccountService accountService,
        BusinessCustomerService businessCustomerService,
        BusinessUserStoreService businessUserStoreService,
        QueueService queueService,
        AccountMobileService accountMobileService,
        TokenQueueMobileService tokenQueueMobileService,
        ApiHealthService apiHealthService
    ) {
        this.authenticateMobileService = authenticateMobileService;
        this.accountService = accountService;
        this.businessCustomerService = businessCustomerService;
        this.businessUserStoreService = businessUserStoreService;
        this.queueService = queueService;
        this.accountMobileService = accountMobileService;
        this.tokenQueueMobileService = tokenQueueMobileService;
        this.apiHealthService = apiHealthService;
    }

    /** Add Business Customer Id to existing QID. */
    @PostMapping(
            value = "/addId",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String addBusinessCustomerId(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        String requestBodyJson,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("All queues associated with mail={} did={} deviceType={} auth={}", mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/bc/addId by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            JsonBusinessCustomer json = new ObjectMapper().readValue(requestBodyJson, JsonBusinessCustomer.class);
            if (StringUtils.isBlank(json.getCodeQR().getText())) {
                LOG.warn("Not a valid codeQR={} qid={}", json.getCodeQR(), qid);
                return getErrorReason("Not a valid queue code.", MOBILE_JSON);
            } else if (!businessUserStoreService.hasAccess(qid, json.getCodeQR().getText())) {
                LOG.info("Un-authorized store access to /api/m/bc/addId by mail={}", mail);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
                return null;
            }

            try {
                Assert.isTrue(Validate.isValidQid(json.getQueueUserId()), "Queue user id not valid");
            } catch (Exception e) {
                LOG.error("Failed as qid is null reason={}", e.getLocalizedMessage(), e);
                return getErrorReason("No user found. Please register user.", USER_NOT_FOUND);
            }

            BusinessUserStoreEntity businessUserStore = businessUserStoreService.findOneByQidAndCodeQR(qid, json.getCodeQR().getText());
            BusinessCustomerEntity businessCustomer = businessCustomerService.findOneByQid(json.getQueueUserId(), businessUserStore.getBizNameId());
            if (null != businessCustomer) {
                LOG.info("Found existing business customer qid={} codeQR={} businessQid={} bizNameId={} businessCustomerId={}",
                    qid, json.getCodeQR(), json.getQueueUserId(), businessUserStore.getBizNameId(), businessCustomer.getBusinessCustomerId());
                return getErrorReason("Business customer id already exists", BUSINESS_CUSTOMER_ID_EXISTS);
            }

            businessCustomer = businessCustomerService.findOneByCustomerId(json.getBusinessCustomerId().getText(), businessUserStore.getBizNameId());
            if (null != businessCustomer) {
                UserProfileEntity userProfile = accountService.findProfileByQueueUserId(businessCustomer.getQueueUserId());
                LOG.warn("Found existing business customer qid={} codeQR={} businessQid={} bizNameId={} businessCustomerId={}",
                    qid, json.getCodeQR(), json.getQueueUserId(), businessUserStore.getBizNameId(), businessCustomer.getBusinessCustomerId());
                return getErrorReason("Business customer id already exists for user with phone: " + userProfile.getPhoneFormatted(), BUSINESS_CUSTOMER_ID_EXISTS);
            }

            UserProfileEntity userProfile = accountService.findProfileByQueueUserId(json.getQueueUserId());
            if (null == userProfile) {
                /* Likely hood of reach here is zero, but if you do reach, then do investigate. */

                Map<String, String> errors = new HashMap<>();
                errors.put(ErrorEncounteredJson.REASON, "No user found. Would you like to register?");
                errors.put(AccountMobileService.ACCOUNT_REGISTRATION.PH.name(), json.getCustomerPhone().getText());
                errors.put(ErrorEncounteredJson.SYSTEM_ERROR, USER_NOT_FOUND.name());
                errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, USER_NOT_FOUND.getCode());
                return ErrorEncounteredJson.toJson(errors);
            }

            businessCustomerService.addBusinessCustomer(
                userProfile.getQueueUserId(),
                json.getCodeQR().getText(),
                businessUserStore.getBizNameId(),
                json.getBusinessCustomerId().getText());
            LOG.info("Added business customer number to qid={} businessCustomerId={}", userProfile.getQueueUserId(), json.getBusinessCustomerId());
            return queueService.findThisPersonInQueue(userProfile.getQueueUserId(), json.getCodeQR().getText());
        } catch (JsonMappingException e) {
            LOG.error("Failed parsing json={} qid={} reason={}", requestBodyJson, qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } catch (Exception e) {
            LOG.error("Failed adding customer id qid={} reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/addId",
                "addId",
                BusinessCustomerController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Edit Business Customer Id to existing QID. */
    @PostMapping(
        value = "/editId",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String editBusinessCustomerId(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        String requestBodyJson,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("All queues associated with mail={} did={} deviceType={} auth={}", mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/bc/editId by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            JsonBusinessCustomer json = new ObjectMapper().readValue(requestBodyJson, JsonBusinessCustomer.class);

            if (StringUtils.isBlank(json.getCodeQR().getText())) {
                LOG.warn("Not a valid codeQR={} qid={}", json.getCodeQR(), qid);
                return getErrorReason("Not a valid queue code.", MOBILE_JSON);
            } else if (!businessUserStoreService.hasAccess(qid, json.getCodeQR().getText())) {
                LOG.info("Un-authorized store access to /api/m/bc/editId by mail={}", mail);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
                return null;
            }

            try {
                Assert.isTrue(Validate.isValidQid(json.getQueueUserId()), "Queue user id not valid");
            } catch (Exception e) {
                LOG.error("Failed as qid is null reason={}", e.getLocalizedMessage(), e);
                return getErrorReason("No user found. Please register user.", USER_NOT_FOUND);
            }

            BusinessUserStoreEntity businessUserStore = businessUserStoreService.findOneByQidAndCodeQR(qid, json.getCodeQR().getText());
            BusinessCustomerEntity businessCustomer = businessCustomerService.findOneByQid(
                json.getQueueUserId(),
                businessUserStore.getBizNameId());
            if (null == businessCustomer) {
                LOG.warn("Could not find customer with qid={} bizNameId={}",json.getQueueUserId(), businessUserStore.getBizNameId());
                return getErrorReason("Business customer id does not exists", BUSINESS_CUSTOMER_ID_DOES_NOT_EXISTS);
            }

            businessCustomer = businessCustomerService.findOneByCustomerId(json.getBusinessCustomerId().getText(), businessUserStore.getBizNameId());
            if (null != businessCustomer) {
                UserProfileEntity userProfile = accountService.findProfileByQueueUserId(businessCustomer.getQueueUserId());
                LOG.warn("Found existing business customer qid={} codeQR={} businessQid={} bizNameId={} businessCustomerId={}",
                    qid, json.getCodeQR(), json.getQueueUserId(), businessUserStore.getBizNameId(), businessCustomer.getBusinessCustomerId());
                return getErrorReason("Business customer id already exists for user with phone: " + userProfile.getPhoneFormatted(), BUSINESS_CUSTOMER_ID_EXISTS);
            }

            businessCustomerService.editBusinessCustomer(
                json.getQueueUserId(),
                json.getCodeQR().getText(),
                businessUserStore.getBizNameId(),
                json.getBusinessCustomerId().getText());
            LOG.info("Edit business customer number to qid={} businessCustomerId={}",
                json.getQueueUserId(), json.getBusinessCustomerId());

            return queueService.findThisPersonInQueue(json.getQueueUserId(), json.getCodeQR().getText());
        } catch (JsonMappingException e) {
            LOG.error("Failed parsing json={} qid={} reason={}", requestBodyJson, qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/editId",
                "editId",
                BusinessCustomerController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    @PostMapping(
        value = "/findCustomer",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String findCustomer(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput dt,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        JsonBusinessCustomerLookup businessCustomerLookup,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Find customer for did={} dt={} mail={}", did, dt, mail);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid, mail.getText(), did.getText(), "/api/m/bc/findCustomer")) return null;

        try {
            if (StringUtils.isBlank(businessCustomerLookup.getCodeQR().getText())) {
                LOG.warn("Not a valid codeQR={} qid={}", businessCustomerLookup.getCodeQR(), qid);
                return getErrorReason("Not a valid queue code.", MOBILE_JSON);
            } else if (!businessUserStoreService.hasAccess(qid, businessCustomerLookup.getCodeQR().getText())) {
                LOG.info("Un-authorized store access to /api/m/bc/findCustomer by mail={}", mail);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
                return null;
            }

            BizStoreEntity bizStore = tokenQueueMobileService.getBizService().findByCodeQR(businessCustomerLookup.getCodeQR().getText());
            if (null == bizStore) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid QR Code");
                return null;
            }

            UserProfileEntity userProfile = null;
            if (StringUtils.isNotBlank(businessCustomerLookup.getCustomerPhone().getText())) {
                userProfile = accountService.checkUserExistsByPhone(businessCustomerLookup.getCustomerPhone().getText());
            } else if (StringUtils.isNotBlank(businessCustomerLookup.getBusinessCustomerId().getText())) {
                userProfile = businessCustomerService.findByBusinessCustomerIdAndBizNameId(
                    businessCustomerLookup.getBusinessCustomerId().getText(),
                    bizStore.getBizName().getId());
            }

            if (null == userProfile) {
                LOG.info("Failed as no user found with phone={} businessCustomerId={}",
                    businessCustomerLookup.getCustomerPhone(),
                    businessCustomerLookup.getBusinessCustomerId());

                Map<String, String> errors = new HashMap<>();
                errors.put(ErrorEncounteredJson.REASON, "No user found. Would you like to register?");
                errors.put(AccountMobileService.ACCOUNT_REGISTRATION.PH.name(), businessCustomerLookup.getCustomerPhone().getText());
                errors.put(ErrorEncounteredJson.SYSTEM_ERROR, USER_NOT_FOUND.name());
                errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, USER_NOT_FOUND.getCode());
                return ErrorEncounteredJson.toJson(errors);
            }

            return accountMobileService.getProfileAsJson(userProfile.getQueueUserId()).asJson();
        } catch(AccountNotActiveException e) {
            LOG.error("Failed getting profile qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return DeviceController.getErrorReason("Please contact support related to your account", ACCOUNT_INACTIVE);
        } catch (Exception e) {
            LOG.error("Failed getting profile qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/findCustomer",
                "findCustomer",
                BusinessCustomerController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    @PostMapping(
        value = "/access/action",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String accessAction(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput dt,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        CustomerPriority customerPriority,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Find customer for did={} dt={} mail={}", did, dt, mail);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid, mail.getText(), did.getText(), "/api/m/bc/access/action")) return null;

        try {
            if (StringUtils.isBlank(customerPriority.getCodeQR().getText())) {
                LOG.warn("Not a valid codeQR={} qid={}", customerPriority.getCodeQR(), qid);
                return getErrorReason("Not a valid queue code.", MOBILE_JSON);
            } else if (!businessUserStoreService.hasAccess(qid, customerPriority.getCodeQR().getText())) {
                LOG.info("Un-authorized store access to /api/m/bc/access/action by mail={}", mail);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
                return null;
            }

            BizStoreEntity bizStore = tokenQueueMobileService.getBizService().findByCodeQR(customerPriority.getCodeQR().getText());
            if (null == bizStore) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid QR Code");
                return null;
            }

            if (OnOffEnum.F == bizStore.getBizName().getPriorityAccess()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid QR Code");
                return null;
            }

            if (!customerPriority.isValid()) {
                return getErrorReason("Please select all options on the form", USER_INPUT);
            }

            QueueEntity queue = queueService.findOneWithoutState(customerPriority.getQueueUserId().getText(), customerPriority.getCodeQR().getText());
            if (null == queue) {
                LOG.info("Customer {} is not in queue {}", customerPriority.getQueueUserId(), customerPriority.getCodeQR());
                return getErrorReason("Person may not be active in queue", USER_INPUT);
            }

            /* To be sent back as response. */
            JsonQueuedPerson jsonQueuedPerson = queueService.getJsonQueuedPerson(queue);

            BusinessCustomerEntity businessCustomer = businessCustomerService.findOneByQidAndAttribute(
                customerPriority.getQueueUserId().getText(),
                bizStore.getBizName().getId(),
                CommonHelper.findBusinessCustomerAttribute(bizStore));
            if (businessCustomer == null) {
                LOG.info("Customer {} is not in queue {}", customerPriority.getQueueUserId(), customerPriority.getCodeQR());
                return getErrorReason("No such customer found. Reset may have already been performed.", USER_INPUT);
            }

            switch (customerPriority.getActionType()) {
                case APPROVE:
                    businessCustomerService.updateBusinessCustomer(
                        businessCustomer.getId(),
                        new LinkedHashSet<>() {{
                            add(CommonHelper.findBusinessCustomerAttribute(bizStore));
                            add(BusinessCustomerAttributeEnum.AP);
                        }},
                        customerPriority.getCustomerPriorityLevel(),
                        bizStore.getBizCategoryId()
                    );

                    jsonQueuedPerson.getBusinessCustomerAttributes().add(BusinessCustomerAttributeEnum.AP);
                    jsonQueuedPerson.setCustomerPriorityLevel(customerPriority.getCustomerPriorityLevel());

                    queueService.updateCustomerPriorityAndCustomerAttributes(
                        customerPriority.getQueueUserId().getText(),
                        customerPriority.getCodeQR().getText(),
                        jsonQueuedPerson.getToken(),
                        customerPriority.getCustomerPriorityLevel(),
                        BusinessCustomerAttributeEnum.AP);

                    tokenQueueMobileService.sendMessageToSpecificUser(
                        bizStore.getDisplayName() + " approved you",
                        "Your credential has been approved. " +
                            "Going forward please join " + bizStore.getDisplayName(),
                        queue.getQueueUserId(),
                        MessageOriginEnum.A
                    );
                    break;
                case REJECT:
                    businessCustomerService.updateBusinessCustomer(
                        businessCustomer.getId(),
                        new LinkedHashSet<>() {{
                            add(CommonHelper.findBusinessCustomerAttribute(bizStore));
                            add(BusinessCustomerAttributeEnum.RJ);
                        }},
                        CustomerPriorityLevelEnum.I,
                        null
                    );

                    jsonQueuedPerson.setCustomerPriorityLevel(CustomerPriorityLevelEnum.I);
                    jsonQueuedPerson.getBusinessCustomerAttributes().add(BusinessCustomerAttributeEnum.RJ);

                    queueService.updateCustomerPriorityAndCustomerAttributes(
                        customerPriority.getQueueUserId().getText(),
                        customerPriority.getCodeQR().getText(),
                        jsonQueuedPerson.getToken(),
                        CustomerPriorityLevelEnum.I,
                        BusinessCustomerAttributeEnum.RJ);

                    tokenQueueMobileService.sendMessageToSpecificUser(
                        bizStore.getDisplayName() + " rejected you",
                        "Your credential has been rejected. " +
                            "Business has permanently blocked you from joining this queue. Please contact business for any queries.",
                        queue.getQueueUserId(),
                        MessageOriginEnum.A
                    );
                    break;
                case CLEAR:
                    businessCustomerService.clearBusinessCustomer(businessCustomer.getQueueUserId(), businessCustomer.getBizNameId());
                    
                    jsonQueuedPerson.setBusinessCustomerAttributes(
                        new LinkedHashSet<>() {{
                            add(BusinessCustomerAttributeEnum.RJ);
                        }});
                    jsonQueuedPerson.setCustomerPriorityLevel(CustomerPriorityLevelEnum.I);

                    queueService.updateCustomerPriorityAndCustomerAttributes(
                        customerPriority.getQueueUserId().getText(),
                        customerPriority.getCodeQR().getText(),
                        jsonQueuedPerson.getToken(),
                        CustomerPriorityLevelEnum.I,
                        BusinessCustomerAttributeEnum.RJ);

                    tokenQueueMobileService.sendMessageToSpecificUser(
                        bizStore.getDisplayName() + " has cleared all your credentials",
                        "Your credential has been reset. " +
                            "Please register yourself with correct credentials.",
                        queue.getQueueUserId(),
                        MessageOriginEnum.A
                    );
                    break;
                default:
                    throw new UnsupportedOperationException("Reached not supported condition");
            }

            return jsonQueuedPerson.asJson();
        } catch (Exception e) {
            LOG.error("Failed updating business customer qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/access/action",
                "accessAction",
                BusinessCustomerController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }
}
