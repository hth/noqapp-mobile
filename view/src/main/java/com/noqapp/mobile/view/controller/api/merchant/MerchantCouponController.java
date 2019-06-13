package com.noqapp.mobile.view.controller.api.merchant;

import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.common.utils.CommonUtil.UNAUTHORIZED;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.COUPON_NOT_APPLICABLE;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.COUPON_REMOVAL_FAILED;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MOBILE_JSON;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.PROMOTION_ACCESS_DENIED;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.PurchaseOrderEntity;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.domain.body.merchant.CouponOnOrder;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.service.BusinessUserStoreService;
import com.noqapp.service.CouponService;
import com.noqapp.service.PurchaseOrderProductService;
import com.noqapp.service.PurchaseOrderService;
import com.noqapp.service.exceptions.CouponCannotApplyException;
import com.noqapp.service.exceptions.CouponRemovalException;

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
 * User: hitender
 * Date: 2019-06-10 13:33
 */
@SuppressWarnings ({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/api/m/coupon")
public class MerchantCouponController {
    private static final Logger LOG = LoggerFactory.getLogger(MerchantCouponController.class);

    private CouponService couponService;
    private BusinessUserStoreService businessUserStoreService;
    private PurchaseOrderService purchaseOrderService;
    private PurchaseOrderProductService purchaseOrderProductService;
    private AuthenticateMobileService authenticateMobileService;
    private ApiHealthService apiHealthService;

    @Autowired
    public MerchantCouponController(
        CouponService couponService,
        BusinessUserStoreService businessUserStoreService,
        PurchaseOrderService purchaseOrderService,
        PurchaseOrderProductService purchaseOrderProductService,
        AuthenticateMobileService authenticateMobileService,
        ApiHealthService apiHealthService
    ) {
        this.couponService = couponService;
        this.businessUserStoreService = businessUserStoreService;
        this.purchaseOrderService = purchaseOrderService;
        this.purchaseOrderProductService = purchaseOrderProductService;
        this.authenticateMobileService = authenticateMobileService;
        this.apiHealthService = apiHealthService;
    }

    @GetMapping(
        value = "/available/{codeQR}",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String availableCoupon(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @PathVariable("codeQR")
        ScrubbedInput codeQR,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Available coupon with mail={} did={} deviceType={} auth={}", mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/coupon/available by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            if (StringUtils.isBlank(codeQR.getText())) {
                LOG.warn("Not a valid codeQR={} qid={}", codeQR.getText(), qid);
                return getErrorReason("Not a valid queue code.", MOBILE_JSON);
            } else if (!businessUserStoreService.hasAccess(qid, codeQR.getText())) {
                LOG.warn("Your are not authorized to access coupon mail={}", mail);
                return getErrorReason("Your are not coupon to access discounts", PROMOTION_ACCESS_DENIED);
            }

            return couponService.findActiveBusinessCouponAsJson(codeQR.getText()).asJson();
        } catch (Exception e) {
            LOG.error("Failed getting coupons for {} reason={}", codeQR.getText(), e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/available",
                "availableCoupon",
                MerchantCouponController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    @PostMapping(
        value = "/apply",
        headers = "Accept=" + MediaType.APPLICATION_JSON_VALUE,
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String apply(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        CouponOnOrder couponOnOrder,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Apply coupon with mail={} did={} deviceType={} auth={}", mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/coupon/apply by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            if (StringUtils.isBlank(couponOnOrder.getCodeQR().getText())) {
                LOG.warn("Not a valid codeQR={} qid={}", couponOnOrder.getCodeQR().getText(), qid);
                return getErrorReason("Not a valid queue code.", MOBILE_JSON);
            } else if (!businessUserStoreService.hasAccess(qid, couponOnOrder.getCodeQR().getText())) {
                LOG.warn("Your are not authorized to access coupon mail={}", mail);
                return getErrorReason("Failed to find coupon", PROMOTION_ACCESS_DENIED);
            }

            PurchaseOrderEntity purchaseOrder = purchaseOrderService.applyCoupon(
                couponOnOrder.getQueueUserId().getText(),
                couponOnOrder.getTransactionId().getText(),
                couponOnOrder.getCouponId().getText(),
                qid);

            return purchaseOrderProductService.populateJsonPurchaseOrder(purchaseOrder).asJson();
        } catch (CouponRemovalException e) {
            LOG.error("Failed removing coupons for {} {} reason={}",
                couponOnOrder.getCodeQR().getText(), couponOnOrder.getCouponId(), e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason(e.getLocalizedMessage(), COUPON_REMOVAL_FAILED);
        } catch (CouponCannotApplyException e) {
            LOG.error("Failed applying coupons for {} {} reason={}",
                couponOnOrder.getCodeQR().getText(), couponOnOrder.getCouponId(), e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason(e.getLocalizedMessage(), COUPON_NOT_APPLICABLE);
        } catch (Exception e) {
            LOG.error("Failed applying coupons for {} {} reason={}",
                couponOnOrder.getCodeQR().getText(), couponOnOrder.getCouponId(), e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/apply",
                "apply",
                MerchantCouponController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    @PostMapping(
        value = "/remove",
        headers = "Accept=" + MediaType.APPLICATION_JSON_VALUE,
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String remove(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        CouponOnOrder couponOnOrder,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Remove coupon with mail={} did={} deviceType={} auth={}", mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/coupon/remove by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            if (StringUtils.isBlank(couponOnOrder.getCodeQR().getText())) {
                LOG.warn("Not a valid codeQR={} qid={}", couponOnOrder.getCodeQR().getText(), qid);
                return getErrorReason("Not a valid queue code.", MOBILE_JSON);
            } else if (!businessUserStoreService.hasAccess(qid, couponOnOrder.getCodeQR().getText())) {
                LOG.warn("Your are not authorized to access coupon mail={}", mail);
                return getErrorReason("Failed to find coupon", PROMOTION_ACCESS_DENIED);
            }

            PurchaseOrderEntity purchaseOrder = purchaseOrderService.removeCoupon(
                couponOnOrder.getQueueUserId().getText(),
                couponOnOrder.getTransactionId().getText());
            
            return purchaseOrderProductService.populateJsonPurchaseOrder(purchaseOrder).asJson();
        } catch (CouponRemovalException e) {
            LOG.error("Failed removing coupons for {} {} reason={}",
                couponOnOrder.getCodeQR().getText(), couponOnOrder.getCouponId(), e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason(e.getLocalizedMessage(), COUPON_REMOVAL_FAILED);
        } catch (Exception e) {
            LOG.error("Failed removing coupons for {} {} reason={}",
                couponOnOrder.getCodeQR().getText(), couponOnOrder.getCouponId(), e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/remove",
                "remove",
                MerchantCouponController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }
}
