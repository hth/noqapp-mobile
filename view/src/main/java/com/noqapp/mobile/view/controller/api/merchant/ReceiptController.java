package com.noqapp.mobile.view.controller.api.merchant;

import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.PURCHASE_ORDER_NOT_FOUND;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.USER_NOT_FOUND;
import static com.noqapp.mobile.view.controller.api.client.TokenQueueAPIController.authorizeRequest;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;
import static com.noqapp.service.ProfessionalProfileService.POPULATE_PROFILE.PUBLIC;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.BusinessUserStoreEntity;
import com.noqapp.domain.PurchaseOrderEntity;
import com.noqapp.domain.UserAccountEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.json.JsonProfessionalProfile;
import com.noqapp.domain.json.JsonProfile;
import com.noqapp.domain.types.UserLevelEnum;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.domain.body.merchant.Receipt;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.repository.BizStoreManager;
import com.noqapp.service.AccountService;
import com.noqapp.service.BusinessUserStoreService;
import com.noqapp.service.ProfessionalProfileService;
import com.noqapp.service.PurchaseOrderProductService;
import com.noqapp.service.PurchaseOrderService;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

/**
 * User: hitender
 * Date: 2019-04-30 11:01
 */
@SuppressWarnings({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/api/m/receipt")
public class ReceiptController {
    private static final Logger LOG = LoggerFactory.getLogger(ReceiptController.class);

    private AuthenticateMobileService authenticateMobileService;
    private AccountService accountService;
    private PurchaseOrderService purchaseOrderService;
    private BusinessUserStoreService businessUserStoreService;
    private ProfessionalProfileService professionalProfileService;
    private PurchaseOrderProductService purchaseOrderProductService;
    private ApiHealthService apiHealthService;

    private BizStoreManager bizStoreManager;

    @Autowired
    public ReceiptController(
        AuthenticateMobileService authenticateMobileService,
        AccountService accountService,
        PurchaseOrderService purchaseOrderService,
        BusinessUserStoreService businessUserStoreService,
        ProfessionalProfileService professionalProfileService,
        PurchaseOrderProductService purchaseOrderProductService,
        ApiHealthService apiHealthService,
        BizStoreManager bizStoreManager
    ) {
        this.authenticateMobileService = authenticateMobileService;
        this.accountService = accountService;
        this.purchaseOrderService = purchaseOrderService;
        this.businessUserStoreService = businessUserStoreService;
        this.professionalProfileService = professionalProfileService;
        this.purchaseOrderProductService = purchaseOrderProductService;
        this.apiHealthService = apiHealthService;
        this.bizStoreManager = bizStoreManager;
    }

    @PostMapping(
        value = "/detail",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String detail(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        Receipt receipt,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Review for did={} transactionId={} codeQR={}", did, receipt.getTransactionId(), receipt.getCodeQR());
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid, mail.getText(), did.getText(), "/api/m/receipt/detail")) return null;

        try {
            if (StringUtils.isBlank(receipt.getTransactionId())) {
                return getErrorReason("Could not find purchase order", PURCHASE_ORDER_NOT_FOUND);
            }

            if (StringUtils.isBlank(receipt.getQueueUserId())) {
                return getErrorReason("Could not find user", USER_NOT_FOUND);
            }

            /* Required. */
            BizStoreEntity bizStore = bizStoreManager.findByCodeQR(receipt.getCodeQR());
            if (null == bizStore) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid token");
                return null;
            }

            receipt.setBusinessName(bizStore.getBizName().getBusinessName())
                .setStoreAddress(bizStore.getAreaAndTown())
                .setBusinessType(bizStore.getBusinessType())
                .setStorePhone(bizStore.getPhone());

            PurchaseOrderEntity purchaseOrder = purchaseOrderService.findByTransactionId(receipt.getTransactionId());
            if (null == purchaseOrder) {
                purchaseOrder = purchaseOrderService.findHistoricalPurchaseOrder(receipt.getQueueUserId(), receipt.getTransactionId());
                receipt.setJsonPurchaseOrder(purchaseOrderProductService.populateHistoricalJsonPurchaseOrder(purchaseOrder));
            } else {
                if (purchaseOrder.getQueueUserId().equalsIgnoreCase(receipt.getQueueUserId())) {
                    receipt.setJsonPurchaseOrder(purchaseOrderProductService.populateJsonPurchaseOrder(purchaseOrder));
                } else {
                    return getErrorReason("Could not find user", USER_NOT_FOUND);
                }
            }

            UserAccountEntity userAccount = accountService.findByQueueUserId(purchaseOrder.getQueueUserId());
            UserProfileEntity userProfile = accountService.findProfileByQueueUserId(purchaseOrder.getQueueUserId());
            JsonProfile jsonProfile = JsonProfile.newInstance(userProfile, userAccount);
            receipt.setJsonProfile(jsonProfile);

            switch (bizStore.getBusinessType()) {
                case DO:
                    List<BusinessUserStoreEntity> businessUserStores = businessUserStoreService.findAllManagingStoreWithUserLevel(
                        bizStore.getId(),
                        UserLevelEnum.S_MANAGER);

                    if (businessUserStores.isEmpty()) {
                        LOG.warn("No manager assigned as professional profile for {} {}", receipt.getCodeQR(), receipt.getTransactionId());
                        return getErrorReason("No professional profile exists. Cannot print receipt.", SEVERE);
                    }

                    BusinessUserStoreEntity businessUserStore = businessUserStores.get(0);
                    JsonProfessionalProfile jsonProfessionalProfile = professionalProfileService.getJsonProfessionalProfile(businessUserStore.getQueueUserId(), PUBLIC);
                    receipt.setName(jsonProfessionalProfile.getName())
                        .setEducation(jsonProfessionalProfile.getEducation())
                        .setLicenses(jsonProfessionalProfile.getLicenses());

                    break;
                default:
                    //Do Nothing
            }

            return receipt.asJson();
        } catch (Exception e) {
            LOG.error("Failed populating with receipt detail reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/detail",
                "detail",
                ReceiptController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }
}
