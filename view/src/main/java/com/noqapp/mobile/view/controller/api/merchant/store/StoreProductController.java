package com.noqapp.mobile.view.controller.api.merchant.store;

import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.MOBILE;
import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.common.utils.CommonUtil.UNAUTHORIZED;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.MOBILE_JSON;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

import com.noqapp.common.errors.ErrorEncounteredJson;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BusinessUserStoreEntity;
import com.noqapp.domain.StoreProductEntity;
import com.noqapp.domain.json.JsonResponse;
import com.noqapp.domain.json.JsonStoreProduct;
import com.noqapp.domain.types.ActionTypeEnum;
import com.noqapp.domain.types.UserLevelEnum;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.service.StoreDetailService;
import com.noqapp.service.BusinessUserStoreService;
import com.noqapp.service.StoreProductService;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import javax.servlet.http.HttpServletResponse;

/**
 * hitender
 * 10/19/18 12:54 PM
 */
@SuppressWarnings ({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/api/m/s/product")
public class StoreProductController {
    private static final Logger LOG = LoggerFactory.getLogger(StoreProductController.class);

    private AuthenticateMobileService authenticateMobileService;
    private BusinessUserStoreService businessUserStoreService;
    private StoreDetailService storeDetailService;
    private StoreProductService storeProductService;
    private ApiHealthService apiHealthService;

    @Autowired
    public StoreProductController(
        AuthenticateMobileService authenticateMobileService,
        BusinessUserStoreService businessUserStoreService,
        StoreDetailService storeDetailService,
        StoreProductService storeProductService,
        ApiHealthService apiHealthService
    ) {
        this.authenticateMobileService = authenticateMobileService;
        this.businessUserStoreService = businessUserStoreService;
        this.storeDetailService = storeDetailService;
        this.storeProductService = storeProductService;
        this.apiHealthService = apiHealthService;
    }

    /** Note: Manager and Supervisor has Read Access. */
    @GetMapping(
        value = "/store/{codeQR}",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String storeProduct(
        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @PathVariable("codeQR")
        ScrubbedInput codeQR,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Clients shown for codeQR={} request from mail={} auth={}",
            codeQR,
            mail,
            AUTH_KEY_HIDDEN);

        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/s/product/store/{codeQR} by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        if (StringUtils.isBlank(codeQR.getText())) {
            LOG.warn("Not a valid codeQR={} qid={}", codeQR.getText(), qid);
            return getErrorReason("Not a valid queue code.", MOBILE_JSON);
        } else if (!businessUserStoreService.hasAccess(qid, codeQR.getText())) {
            LOG.info("Un-authorized store access to /api/m/s/product/store/{codeQR} by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            return storeDetailService.populateStoreDetail(codeQR.getText());
        } catch (Exception e) {
            LOG.error("Failed parsing json qid={} message={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/store/{codeQR}",
                "storeProduct",
                StoreProductController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Note: Manager has Write Access. */
    @PostMapping(
        value = "/store/{codeQR}/{action}",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String actionOnProduct(
        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @PathVariable("codeQR")
        ScrubbedInput codeQR,

        @PathVariable("action")
        ScrubbedInput action,

        @RequestBody
        JsonStoreProduct jsonStoreProduct,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Clients shown for codeQR={} request from mail={} auth={}",
            codeQR,
            mail,
            AUTH_KEY_HIDDEN);

        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/s/product/store/{codeQR}/{action} by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        if (StringUtils.isBlank(codeQR.getText())) {
            LOG.warn("Not a valid codeQR={} qid={}", codeQR.getText(), qid);
            return getErrorReason("Not a valid queue code.", MOBILE_JSON);
        } else if (!businessUserStoreService.hasAccessWithUserLevel(qid, codeQR.getText(), UserLevelEnum.S_MANAGER)) {
            LOG.info("Un-authorized store access to /api/m/s/product/store/{codeQR}/{action} by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            ActionTypeEnum actionType = ActionTypeEnum.valueOf(action.getText());
            StoreProductEntity storeProductFromJson = StoreProductEntity.parseJsonStoreProduct(jsonStoreProduct);
            switch (actionType) {
                case ADD:
                    BusinessUserStoreEntity businessUserStore = businessUserStoreService.findOneByQidAndCodeQR(qid, codeQR.getText());
                    storeProductFromJson.setBizStoreId(businessUserStore.getBizStoreId());
                    LOG.info("Add new product {}", storeProductFromJson);
                    storeProductService.save(storeProductFromJson);
                    break;
                case EDIT:
                    if (StringUtils.isBlank(storeProductFromJson.getId())) {
                        LOG.error("Missing product id name={}", storeProductFromJson.getProductName());
                        return ErrorEncounteredJson.toJson("Cannot edit as missing product id", MOBILE);
                    }

                    storeProductFromJson.populateWithExistingStoreProduct(storeProductService.findOne(storeProductFromJson.getId()));
                    LOG.info("Edit product {}", storeProductFromJson);
                    storeProductService.save(storeProductFromJson);
                    break;
                case REMOVE:
                    if (StringUtils.isBlank(storeProductFromJson.getId())) {
                        LOG.error("Missing product id name={}", storeProductFromJson.getProductName());
                        return ErrorEncounteredJson.toJson("Cannot remove as missing product id", MOBILE);
                    }

                    LOG.info("Removed product {}", storeProductFromJson);
                    storeProductService.removeById(storeProductFromJson.getId());
                    break;
                case INACTIVE:
                    StoreProductEntity storeProductFromDB = storeProductService.findOne(storeProductFromJson.getId());
                    storeProductFromDB.inActive();
                    LOG.info("Inactive product {}", storeProductFromDB);
                    storeProductService.save(storeProductFromDB);
                    break;
                case ACTIVE:
                    storeProductFromDB = storeProductService.findOne(storeProductFromJson.getId());
                    storeProductFromDB.active();
                    LOG.info("Active product {}", storeProductFromDB);
                    storeProductService.save(storeProductFromDB);
                    break;
                default:
                    LOG.error("Reached unsupported condition actionType={}", actionType);
                    throw new UnsupportedOperationException("Reached unsupported condition for ActionType " + actionType);
            }
            return new JsonResponse(true).asJson();
        } catch (Exception e) {
            LOG.error("Failed parsing json qid={} message={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/store/{codeQR}/{action}",
                "actionOnProduct",
                StoreProductController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }
}
