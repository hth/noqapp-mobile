package com.noqapp.mobile.view.controller.api.client;

import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.QUEUE_JOIN_FAILED_PAYMENT_CALL_REQUEST;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.STORE_DAY_CLOSED;
import static com.noqapp.mobile.view.controller.api.client.TokenQueueAPIController.authorizeRequest;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

import com.noqapp.common.utils.CommonUtil;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.types.TokenServiceEnum;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.common.util.ErrorEncounteredJson;
import com.noqapp.mobile.domain.body.client.JoinQueue;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.service.PurchaseOrderMobileService;
import com.noqapp.mobile.service.QueueMobileService;
import com.noqapp.mobile.service.TokenQueueMobileService;
import com.noqapp.service.DeviceService;
import com.noqapp.service.JoinAbortService;
import com.noqapp.service.PurchaseOrderService;
import com.noqapp.service.ScheduleAppointmentService;
import com.noqapp.service.exceptions.StoreDayClosedException;

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
    private ApiHealthService apiHealthService;

    @Autowired
    public KioskController(
        TokenQueueMobileService tokenQueueMobileService,
        JoinAbortService joinAbortService,
        DeviceService deviceService,
        AuthenticateMobileService authenticateMobileService,
        ApiHealthService apiHealthService
    ) {
        this.tokenQueueMobileService = tokenQueueMobileService;
        this.joinAbortService = joinAbortService;
        this.deviceService = deviceService;
        this.authenticateMobileService = authenticateMobileService;
        this.apiHealthService = apiHealthService;
    }

    /** Join the queue. */
    @PostMapping(
        value = "/queue",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
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
            return joinAbortService.joinQueue(
                joinQueue.getCodeQR(),
                deviceService.getExistingDeviceId(joinQueue.getQueueUserId(), did.getText()),
                joinQueue.getQueueUserId(),
                joinQueue.getGuardianQid(),
                bizStore.getAverageServiceTime(),
                TokenServiceEnum.C).asJson();
        } catch (StoreDayClosedException e) {
            LOG.warn("Failed joining queue qid={}, reason={}", qid, e.getLocalizedMessage());
            methodStatusSuccess = false;
            return ErrorEncounteredJson.toJson("Store is closed today", STORE_DAY_CLOSED);
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
