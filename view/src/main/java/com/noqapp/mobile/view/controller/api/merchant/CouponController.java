package com.noqapp.mobile.view.controller.api.merchant;

import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.common.utils.CommonUtil.UNAUTHORIZED;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.COUPON_NOT_APPLICABLE;
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
import com.noqapp.service.PurchaseOrderService;
import com.noqapp.service.exceptions.CouponCannotApplyException;

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
public class CouponController {
    private static final Logger LOG = LoggerFactory.getLogger(CouponController.class);

    private CouponService couponService;
    private BusinessUserStoreService businessUserStoreService;
    private PurchaseOrderService purchaseOrderService;
    private AuthenticateMobileService authenticateMobileService;
    private ApiHealthService apiHealthService;

    @Autowired
    public CouponController(
        CouponService couponService,
        BusinessUserStoreService businessUserStoreService,
        PurchaseOrderService purchaseOrderService,
        AuthenticateMobileService authenticateMobileService,
        ApiHealthService apiHealthService
    ) {
        this.couponService = couponService;
        this.businessUserStoreService = businessUserStoreService;
        this.purchaseOrderService = purchaseOrderService;
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
                LOG.info("Your are not authorized to access coupon mail={}", mail);
                return getErrorReason("Your are not coupon to access discounts", PROMOTION_ACCESS_DENIED);
            }

            return couponService.findAllCouponAsJson(codeQR.getText()).asJson();
        } catch (Exception e) {
            LOG.error("Failed getting coupons for {} reason={}", codeQR.getText(), e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/available",
                "availableCoupon",
                CouponController.class.getName(),
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
            LOG.warn("Un-authorized access to /api/m/coupon/available by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            if (StringUtils.isBlank(couponOnOrder.getCodeQR().getText())) {
                LOG.warn("Not a valid codeQR={} qid={}", couponOnOrder.getCodeQR().getText(), qid);
                return getErrorReason("Not a valid queue code.", MOBILE_JSON);
            } else if (!businessUserStoreService.hasAccess(qid, couponOnOrder.getCodeQR().getText())) {
                LOG.info("Your are not authorized to access coupon mail={}", mail);
                return getErrorReason("Your are not coupon to access discounts", PROMOTION_ACCESS_DENIED);
            }

            return purchaseOrderService.applyCoupon(couponOnOrder.getQueueUserId(), couponOnOrder.getTransactionId(), couponOnOrder.getCouponId()).asJson();
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
                CouponController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

}
