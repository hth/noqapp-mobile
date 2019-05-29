package com.noqapp.mobile.view.controller.api.merchant;

import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.common.utils.CommonUtil.UNAUTHORIZED;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.APPOINTMENT_ACTION_NOT_PERMITTED;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.FAILED_TO_FIND_APPOINTMENT;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MOBILE_JSON;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.ScheduleAppointmentEntity;
import com.noqapp.domain.json.JsonSchedule;
import com.noqapp.domain.json.JsonScheduleList;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.view.controller.api.client.AppointmentController;
import com.noqapp.service.BusinessUserStoreService;
import com.noqapp.service.ScheduleAppointmentService;
import com.noqapp.service.exceptions.AppointmentBookingException;

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
 * Date: 2019-05-22 11:23
 */
@SuppressWarnings({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/api/m/schedule")
public class ScheduleController {
    private static final Logger LOG = LoggerFactory.getLogger(AppointmentController.class);

    private AuthenticateMobileService authenticateMobileService;
    private ScheduleAppointmentService scheduleAppointmentService;
    private BusinessUserStoreService businessUserStoreService;
    private ApiHealthService apiHealthService;

    @Autowired
    public ScheduleController(
        AuthenticateMobileService authenticateMobileService,
        ScheduleAppointmentService scheduleAppointmentService,
        BusinessUserStoreService businessUserStoreService,
        ApiHealthService apiHealthService
    ) {
        this.authenticateMobileService = authenticateMobileService;
        this.scheduleAppointmentService = scheduleAppointmentService;
        this.businessUserStoreService = businessUserStoreService;
        this.apiHealthService = apiHealthService;
    }

    @GetMapping(
        value = "/scheduleForMonth/{month}/{codeQR}",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String scheduleForMonth(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput dt,

        @RequestHeader("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @PathVariable("month")
        ScrubbedInput month,

        @PathVariable("codeQR")
        ScrubbedInput codeQR,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.debug("scheduleForMonth {} {} mail={}, auth={}", month.getText(), codeQR.getText(), mail, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        if (!businessUserStoreService.hasAccess(qid, codeQR.getText())) {
            LOG.info("Your are not authorized to access schedule for codeQR={} mail={}", codeQR.getText(), mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            return scheduleAppointmentService.numberOfAppointmentsForMonth(codeQR.getText(), month.getText()).asJson();
        } catch (Exception e) {
            LOG.error("Failed getting schedule qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/scheduleForMonth",
                "scheduleForMonth",
                ScheduleController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    @GetMapping(
        value = "/scheduleForDay/{day}/{codeQR}",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String scheduleForDay(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput dt,

        @RequestHeader("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @PathVariable("day")
        ScrubbedInput day,

        @PathVariable("codeQR")
        ScrubbedInput codeQR,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.debug("ScheduleForDay {} {} mail={}, auth={}", day.getText(), codeQR.getText(), mail, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        if (!businessUserStoreService.hasAccess(qid, codeQR.getText())) {
            LOG.info("Your are not authorized to access schedule for codeQR={} mail={}", codeQR.getText(), mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            JsonScheduleList jsonSchedules = scheduleAppointmentService.findScheduleForDayAsJson(codeQR.getText(), day.getText());
            return jsonSchedules.asJson();
        } catch (Exception e) {
            LOG.error("Failed getting schedule qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/scheduleForDay",
                "scheduleForDay",
                ScheduleController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Action on schedule. */
    @PostMapping(
        value = "/action",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String scheduleAction(
        @RequestHeader ("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        JsonSchedule jsonSchedule,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Perform action on schedule id={} appointmentStatus={} qid={} codeQR={} mail={} did={} deviceType={} auth={}",
            jsonSchedule.getScheduleAppointmentId(),
            jsonSchedule.getAppointmentStatus(),
            jsonSchedule.getQueueUserId(),
            jsonSchedule.getCodeQR(),
            mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/schedule/action by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        if (StringUtils.isBlank(jsonSchedule.getCodeQR())) {
            LOG.warn("Not a valid codeQR={} qid={}", jsonSchedule.getCodeQR(), qid);
            return getErrorReason("Not a valid queue code.", MOBILE_JSON);
        } else if (!businessUserStoreService.hasAccess(qid, jsonSchedule.getCodeQR())) {
            LOG.info("Un-authorized store access to /api/m/schedule/action by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            ScheduleAppointmentEntity scheduleAppointment = scheduleAppointmentService.findAppointment(
                jsonSchedule.getScheduleAppointmentId(),
                jsonSchedule.getQueueUserId(),
                jsonSchedule.getCodeQR());

            switch (scheduleAppointment.getAppointmentStatus()) {
                case U:
                case A:
                    return scheduleAppointmentService.scheduleAction(
                        jsonSchedule.getScheduleAppointmentId(),
                        jsonSchedule.getAppointmentStatus(),
                        jsonSchedule.getQueueUserId(),
                        jsonSchedule.getCodeQR()).asJson();
                case C:
                case R:
                case S:
                    return getErrorReason(
                        "Cannot perform " + scheduleAppointment.getAppointmentStatus().getDescription() + " action on appointment",
                        APPOINTMENT_ACTION_NOT_PERMITTED);
            }

            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } catch (AppointmentBookingException e) {
            LOG.error("Failed action on schedule reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason(e.getLocalizedMessage(), FAILED_TO_FIND_APPOINTMENT);
        } catch (Exception e) {
            LOG.error("Failed action on schedule reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/action",
                "scheduleAction",
                ScheduleController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }
}
