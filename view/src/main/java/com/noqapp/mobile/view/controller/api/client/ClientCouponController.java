package com.noqapp.mobile.view.controller.api.client;

import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.COUPON_NOT_APPLICABLE;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.COUPON_REMOVAL_FAILED;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.MOBILE_JSON;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.common.utils.CommonUtil.UNAUTHORIZED;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.PurchaseOrderEntity;
import com.noqapp.domain.json.JsonCouponList;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.domain.body.client.Location;
import com.noqapp.mobile.domain.body.merchant.CouponOnOrder;
import com.noqapp.mobile.service.AuthenticateMobileService;
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
 * Date: 2019-06-13 12:51
 */
@SuppressWarnings ({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/api/c/coupon")
public class ClientCouponController {
    private static final Logger LOG = LoggerFactory.getLogger(ClientCouponController.class);

    private CouponService couponService;
    private PurchaseOrderService purchaseOrderService;
    private PurchaseOrderProductService purchaseOrderProductService;
    private AuthenticateMobileService authenticateMobileService;
    private ApiHealthService apiHealthService;

    @Autowired
    public ClientCouponController(
        CouponService couponService,
        PurchaseOrderService purchaseOrderService,
        PurchaseOrderProductService purchaseOrderProductService,
        AuthenticateMobileService authenticateMobileService,
        ApiHealthService apiHealthService
    ) {
        this.couponService = couponService;
        this.purchaseOrderService = purchaseOrderService;
        this.purchaseOrderProductService = purchaseOrderProductService;
        this.authenticateMobileService = authenticateMobileService;
        this.apiHealthService = apiHealthService;
    }

    @GetMapping(
        value = "/available",
        produces = MediaType.APPLICATION_JSON_VALUE
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

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Available personal coupon with mail={} did={} deviceType={} auth={}", mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/c/coupon/available by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            return couponService.findActiveClientCouponByQidAsJson(qid).asJson();
        } catch (Exception e) {
            LOG.error("Failed getting personal coupons for reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/available",
                "availableCoupon",
                ClientCouponController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    @GetMapping(
        value = "/filter/{codeQR}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String filterCoupon(
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
        LOG.info("Available personal coupon business specific with mail={} did={} deviceType={} auth={}", mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/c/coupon/filter by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            JsonCouponList myCoupons = couponService.findActiveClientCouponByQidAsJson(qid, codeQR.getText());
            JsonCouponList globalCoupon = couponService.findAllGlobalCouponForClient(codeQR.getText());
            myCoupons.getCoupons().addAll(globalCoupon.getCoupons());
            return myCoupons.asJson();
        } catch (Exception e) {
            LOG.error("Failed getting personal coupons business specific for reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/filter",
                "filterCoupon",
                ClientCouponController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    @PostMapping(
        value = "/global",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String globalCoupon(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        Location location,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Available global coupon with mail={} did={} deviceType={} auth={}", mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/c/coupon/global by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            return couponService.findNearByCouponAsJson(
                Double.parseDouble(location.getLongitude().getText()),
                Double.parseDouble(location.getLatitude().getText())).asJson();
        } catch (Exception e) {
            LOG.error("Failed getting global coupons for reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/global",
                "globalCoupon",
                ClientCouponController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    @PostMapping(
        value = "/apply",
        headers = "Accept=" + MediaType.APPLICATION_JSON_VALUE,
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
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
            LOG.warn("Un-authorized access to /api/c/coupon/apply by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            if (StringUtils.isBlank(couponOnOrder.getQueueUserId().getText())
                || StringUtils.isBlank(couponOnOrder.getTransactionId().getText())
                || StringUtils.isBlank(couponOnOrder.getCouponId().getText())
            ) {
                LOG.error("Failed validation {} {} {}",
                    couponOnOrder.getQueueUserId().getText(),
                    couponOnOrder.getTransactionId().getText(),
                    couponOnOrder.getCouponId().getText());
                return getErrorReason("Could not parse JSON", MOBILE_JSON);
            }

            PurchaseOrderEntity purchaseOrder = purchaseOrderService.applyCoupon(
                couponOnOrder.getQueueUserId().getText(),
                couponOnOrder.getTransactionId().getText(),
                couponOnOrder.getCouponId().getText());

            return purchaseOrderProductService.populateJsonPurchaseOrder(purchaseOrder).asJson();
        } catch (CouponRemovalException e) {
            LOG.error("Failed removing coupons for {} {} reason={}", couponOnOrder, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason(e.getLocalizedMessage(), COUPON_REMOVAL_FAILED);
        } catch (CouponCannotApplyException e) {
            LOG.error("Failed applying coupons for {} reason={}", couponOnOrder, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason(e.getLocalizedMessage(), COUPON_NOT_APPLICABLE);
        } catch (Exception e) {
            LOG.error("Failed applying coupons for {} reason={}", couponOnOrder, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/apply",
                "apply",
                ClientCouponController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    @PostMapping(
        value = "/remove",
        headers = "Accept=" + MediaType.APPLICATION_JSON_VALUE,
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
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
            LOG.warn("Un-authorized access to /api/c/coupon/remove by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            if (StringUtils.isBlank(couponOnOrder.getQueueUserId().getText())
                || StringUtils.isBlank(couponOnOrder.getTransactionId().getText())
            ) {
                LOG.error("Failed client validation {} {}",
                    couponOnOrder.getQueueUserId().getText(),
                    couponOnOrder.getTransactionId().getText());
                return getErrorReason("Could not parse JSON", MOBILE_JSON);
            }

            PurchaseOrderEntity purchaseOrder = purchaseOrderService.removeCoupon(
                couponOnOrder.getQueueUserId().getText(),
                couponOnOrder.getTransactionId().getText());

            return purchaseOrderProductService.populateJsonPurchaseOrder(purchaseOrder).asJson();
        } catch (CouponRemovalException e) {
            LOG.error("Failed removing coupons for {} reason={}", couponOnOrder, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason(e.getLocalizedMessage(), COUPON_REMOVAL_FAILED);
        } catch (Exception e) {
            LOG.error("Failed removing coupons for {} reason={}", couponOnOrder, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/remove",
                "remove",
                ClientCouponController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }
}
