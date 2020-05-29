package com.noqapp.mobile.view.controller.api.client;

import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.DEVICE_TIMEZONE_OFF;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.QUEUE_AUTHORIZED_ONLY;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.QUEUE_JOINING_IN_AUTHORIZED_QUEUE;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.QUEUE_SERVICE_LIMIT;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.QUEUE_TOKEN_LIMIT;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.STORE_DAY_CLOSED;
import static com.noqapp.mobile.view.controller.api.client.TokenQueueAPIController.authorizeRequest;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.BusinessCustomerEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.helper.CommonHelper;
import com.noqapp.domain.types.OnOffEnum;
import com.noqapp.domain.types.TokenServiceEnum;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.common.errors.ErrorEncounteredJson;
import com.noqapp.mobile.domain.body.client.JoinQueue;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.service.TokenQueueMobileService;
import com.noqapp.service.BusinessCustomerService;
import com.noqapp.service.DeviceService;
import com.noqapp.service.JoinAbortService;
import com.noqapp.service.exceptions.AuthorizedUserCanJoinQueueException;
import com.noqapp.service.exceptions.BeforeStartOfStoreException;
import com.noqapp.service.exceptions.JoiningNonAuthorizedQueueException;
import com.noqapp.service.exceptions.LimitedPeriodException;
import com.noqapp.service.exceptions.StoreDayClosedException;
import com.noqapp.service.exceptions.TokenAvailableLimitReachedException;

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

import javax.servlet.http.HttpServletResponse;

/**
 * User: hitender
 * Date: 11/16/19 1:17 AM
 */
@SuppressWarnings ({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/api/c/kiosk")
public class KioskController {
    private static final Logger LOG = LoggerFactory.getLogger(TokenQueueAPIController.class);

    private TokenQueueMobileService tokenQueueMobileService;
    private JoinAbortService joinAbortService;
    private DeviceService deviceService;
    private AuthenticateMobileService authenticateMobileService;
    private BusinessCustomerService businessCustomerService;
    private ApiHealthService apiHealthService;

    @Autowired
    public KioskController(
        TokenQueueMobileService tokenQueueMobileService,
        JoinAbortService joinAbortService,
        DeviceService deviceService,
        AuthenticateMobileService authenticateMobileService,
        BusinessCustomerService businessCustomerService,
        ApiHealthService apiHealthService
    ) {
        this.tokenQueueMobileService = tokenQueueMobileService;
        this.joinAbortService = joinAbortService;
        this.deviceService = deviceService;
        this.authenticateMobileService = authenticateMobileService;
        this.businessCustomerService = businessCustomerService;
        this.apiHealthService = apiHealthService;
    }

    /** Join the queue. */
    @PostMapping(
        value = "/queue",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String joinQueue(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        JoinQueue joinQueue,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Join queue via Kiosk did={} dt={}", did, deviceType);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        BizStoreEntity bizStore = tokenQueueMobileService.getBizService().findByCodeQR(joinQueue.getCodeQR());
        if (null == bizStore) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid QR Code");
            return null;
        }

        try {
            LOG.info("codeQR={} qid={} guardianQid={}", joinQueue.getCodeQR(), joinQueue.getQueueUserId(), joinQueue.getGuardianQid());
            joinAbortService.checkCustomerApprovedForTheQueue(qid, bizStore);
            return joinAbortService.joinQueue(
                joinQueue.getCodeQR(),
                deviceService.getExistingDeviceId(joinQueue.getQueueUserId(), did.getText()),
                joinQueue.getQueueUserId(),
                joinQueue.getGuardianQid(),
                bizStore.getAverageServiceTime(),
                TokenServiceEnum.C).asJson();
        } catch (StoreDayClosedException e) {
            LOG.warn("Failed joining queue store closed qid={}, reason={}", qid, e.getLocalizedMessage());
            methodStatusSuccess = false;
            return ErrorEncounteredJson.toJson("Store is closed today", STORE_DAY_CLOSED);
        } catch (BeforeStartOfStoreException e) {
            LOG.warn("Failed joining queue as trying to join before store opens qid={}, reason={}", qid, e.getLocalizedMessage());
            methodStatusSuccess = true;
            return ErrorEncounteredJson.toJson(bizStore.getDisplayName() + " has not started. Please correct time on your device.", DEVICE_TIMEZONE_OFF);
        } catch (LimitedPeriodException e) {
            LOG.warn("Failed joining queue as limited join allowed qid={}, reason={}", qid, e.getLocalizedMessage());
            methodStatusSuccess = true;
            String message = bizStore.getDisplayName() + " allows a customer one token in " + bizStore.getBizName().getLimitServiceByDays()
                + " days. You have been serviced with-in past " + bizStore.getBizName().getLimitServiceByDays()
                + " days. Please try again later.";
            return ErrorEncounteredJson.toJson(message, QUEUE_SERVICE_LIMIT);
        } catch (TokenAvailableLimitReachedException e) {
            LOG.warn("Failed joining queue as token limit reached qid={}, reason={}", qid, e.getLocalizedMessage());
            methodStatusSuccess = true;
            return ErrorEncounteredJson.toJson(bizStore.getDisplayName() + " token limit for the day has reached.", QUEUE_TOKEN_LIMIT);
        } catch (JoiningQueuePreApprovedRequiredException e) {
            LOG.warn("Store has to pre-approve qid={}, reason={}", qid, e.getLocalizedMessage());
            methodStatusSuccess = true;
            return ErrorEncounteredJson.toJson("Store has to pre-approve. Please complete pre-approval before joining the queue.", JOINING_NOT_PRE_APPROVED_QUEUE);
        } catch (JoiningNonApprovedQueueException e) {
            LOG.warn("This queue is not approved qid={}, reason={}", qid, e.getLocalizedMessage());
            methodStatusSuccess = true;
            return ErrorEncounteredJson.toJson("This queue is not approved. Select correct pre-approved queue.", JOIN_PRE_APPROVED_QUEUE_ONLY);
        } catch(JoiningQueuePermissionDeniedException e) {
            LOG.warn("Store prevented user from joining queue qid={}, reason={}", qid, e.getLocalizedMessage());
            methodStatusSuccess = true;
            return ErrorEncounteredJson.toJson("Store has denied you from joining the queue. Please contact store for resolving this issue.", JOINING_QUEUE_PERMISSION_DENIED);
        } catch (Exception e) {
            LOG.error("Failed joining queue qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/queue",
                "joinQueue",
                KioskController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }
}
