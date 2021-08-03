package com.noqapp.mobile.view.controller.open;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.json.JsonResponse;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.domain.body.Notification;
import com.noqapp.service.MessageCustomerService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;

/**
 * hitender
 * 12/30/20 10:53 PM
 */
@SuppressWarnings ({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/open/notification")
public class NotificationAcknowledgeController {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationAcknowledgeController.class);

    private MessageCustomerService messageCustomerService;
    private ApiHealthService apiHealthService;

    @Autowired
    public NotificationAcknowledgeController(MessageCustomerService messageCustomerService, ApiHealthService apiHealthService) {
        this.messageCustomerService = messageCustomerService;
        this.apiHealthService = apiHealthService;
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public String notificationViewed(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput dt,

        @RequestBody
        Notification notification
    ) {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Notification read for {} did={} dt={}", notification.getId(), did, dt);
        try {
            messageCustomerService.increaseViewUnregisteredCount(notification.getId().getText());
            return new JsonResponse(true).asJson();
        } catch (Exception e) {
            LOG.error("Failed processing notification view reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return new JsonResponse(false).asJson();
        } finally {
            apiHealthService.insert(
                "/",
                "notificationViewed",
                NotificationAcknowledgeController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }
}
