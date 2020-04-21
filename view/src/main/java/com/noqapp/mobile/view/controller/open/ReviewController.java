package com.noqapp.mobile.view.controller.open;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.json.JsonResponse;
import com.noqapp.domain.json.JsonReviewList;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.domain.body.client.QueueReview;
import com.noqapp.mobile.service.QueueMobileService;
import com.noqapp.mobile.service.TokenQueueMobileService;
import com.noqapp.service.ReviewService;

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

import java.time.Duration;
import java.time.Instant;

import javax.servlet.http.HttpServletResponse;

/**
 * User: hitender
 * Date: 4/28/17 10:00 PM
 */
@SuppressWarnings({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/open/review")
public class ReviewController {
    private static final Logger LOG = LoggerFactory.getLogger(ReviewController.class);

    private TokenQueueMobileService tokenQueueMobileService;
    private QueueMobileService queueMobileService;
    private ReviewService reviewService;
    private ApiHealthService apiHealthService;

    @Autowired
    public ReviewController(
        TokenQueueMobileService tokenQueueMobileService,
        QueueMobileService queueMobileService,
        ReviewService reviewService,
        ApiHealthService apiHealthService
    ) {
        this.tokenQueueMobileService = tokenQueueMobileService;
        this.queueMobileService = queueMobileService;
        this.reviewService = reviewService;
        this.apiHealthService = apiHealthService;
    }

    /** Add review to queue service. This includes today's service or historical queue service. */
    @PostMapping(
        value = "/queue",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String queue(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader("X-R-DT")
        ScrubbedInput dt,

        @RequestBody
        QueueReview queueReview,

        HttpServletResponse response
    ) {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Review for did={} dt={}", did, dt);

        boolean reviewSuccess = false;
        try {
            /* Required. */
            if (!tokenQueueMobileService.getBizService().isValidCodeQR(queueReview.getCodeQR())) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid token");
                return null;
            }

            reviewSuccess = queueMobileService.reviewService(
                queueReview.getCodeQR(),
                queueReview.getToken(),
                did.getText(),
                null,
                queueReview.getRatingCount(),
                queueReview.getHoursSaved(),
                queueReview.getReview());
            return new JsonResponse(reviewSuccess).asJson();
        } catch (Exception e) {
            LOG.error("Failed processing review reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return new JsonResponse(reviewSuccess).asJson();
        } finally {
            apiHealthService.insert(
                "/queue",
                "queue",
                ReviewController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Get all reviews associated with business. This includes today's review and historical review. */
    @GetMapping(
        value = "/reviews/{codeQR}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String reviews(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader("X-R-DT")
        ScrubbedInput dt,

        @PathVariable("codeQR")
        ScrubbedInput codeQR,

        HttpServletResponse response
    ) {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Review for did={} dt={} codeQR={}", did, dt, codeQR);

        try {
            /* Required. */
            if (!tokenQueueMobileService.getBizService().isValidCodeQR(codeQR.getText())) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid token");
                return null;
            }

            BizStoreEntity bizStore = tokenQueueMobileService.getBizService().findByCodeQR(codeQR.getText());
            switch (bizStore.getBusinessType().getMessageOrigin()) {
                case O:
                    return reviewService.findOrderReviews(codeQR.getText()).asJson();
                case Q:
                    return reviewService.findQueueReviews(codeQR.getText()).asJson();
            }

            return new JsonReviewList().asJson();
        } catch (Exception e) {
            LOG.error("Failed processing review reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return new JsonReviewList().asJson();
        } finally {
            apiHealthService.insert(
                "/reviews/{codeQR}",
                "reviews",
                ReviewController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Get all reviews associated with business. This includes today's review and historical review. */
    @GetMapping(
        value = "/reviews/levelUp/{codeQR}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String reviewsLevelUp(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader("X-R-DT")
        ScrubbedInput dt,

        @PathVariable("codeQR")
        ScrubbedInput codeQR,

        HttpServletResponse response
    ) {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Review for did={} dt={} codeQR={}", did, dt, codeQR);

        try {
            /* Required. */
            if (!tokenQueueMobileService.getBizService().isValidCodeQR(codeQR.getText())) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid token");
                return null;
            }

            BizStoreEntity bizStore = tokenQueueMobileService.getBizService().findByCodeQR(codeQR.getText());
            switch (bizStore.getBusinessType().getMessageOrigin()) {
                case O:
                    LOG.error("Should not reach here bizStoreId={} name={}", bizStore.getId(), bizStore.getDisplayName());
                    break;
                case Q:
                    return reviewService.findQueueLevelUpReviews(bizStore.getBizName().getId()).asJson();
            }

            return new JsonReviewList().asJson();
        } catch (Exception e) {
            LOG.error("Failed processing review reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return new JsonReviewList().asJson();
        } finally {
            apiHealthService.insert(
                "/reviews/levelUp/{codeQR}",
                "reviewsLevelUp",
                ReviewController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }
}
