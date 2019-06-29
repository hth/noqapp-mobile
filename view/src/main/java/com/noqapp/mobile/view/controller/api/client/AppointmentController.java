package com.noqapp.mobile.view.controller.api.client;

import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.common.utils.CommonUtil.UNAUTHORIZED;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.APPOINTMENT_ALREADY_EXISTS;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.CANNOT_BOOK_APPOINTMENT;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.FAILED_TO_CANCEL_APPOINTMENT;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MOBILE;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

import com.noqapp.common.utils.DateUtil;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.json.JsonResponse;
import com.noqapp.domain.json.JsonSchedule;
import com.noqapp.domain.json.JsonScheduleList;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.service.TokenQueueMobileService;
import com.noqapp.repository.UserProfileManager;
import com.noqapp.service.ScheduleAppointmentService;
import com.noqapp.service.exceptions.AppointmentBookingException;
import com.noqapp.service.exceptions.AppointmentCancellationException;

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
 * Date: 2019-05-22 10:36
 */
@SuppressWarnings ({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/api/c/appointment")
public class AppointmentController {
    private static final Logger LOG = LoggerFactory.getLogger(AppointmentController.class);

    private UserProfileManager userProfileManager;
    private TokenQueueMobileService tokenQueueMobileService;

    private AuthenticateMobileService authenticateMobileService;
    private ScheduleAppointmentService scheduleAppointmentService;
    private ApiHealthService apiHealthService;

    @Autowired
    public AppointmentController(
        UserProfileManager userProfileManager,

        TokenQueueMobileService tokenQueueMobileService,
        AuthenticateMobileService authenticateMobileService,
        ScheduleAppointmentService scheduleAppointmentService,
        ApiHealthService apiHealthService
    ) {
        this.userProfileManager = userProfileManager;

        this.tokenQueueMobileService = tokenQueueMobileService;
        this.authenticateMobileService = authenticateMobileService;
        this.scheduleAppointmentService = scheduleAppointmentService;
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

        if (!tokenQueueMobileService.isValidCodeQR(codeQR.getText())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid token");
            return null;
        }

        try {
            DateUtil.DTF_YYYY_MM_DD.parse(month.getText());
        } catch (Exception e) {
            LOG.error("Cannot parse date {}", month.getText());
            return getErrorReason("Date provided is not valid", MOBILE);
        }

        try {
            JsonScheduleList jsonSchedules = scheduleAppointmentService.numberOfAppointmentsForMonth(codeQR.getText(), month.getText());
            return jsonSchedules.asJson();
        } catch (Exception e) {
            LOG.error("Failed getting schedule qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/scheduleForMonth",
                "scheduleForMonth",
                AppointmentController.class.getName(),
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

        if (!tokenQueueMobileService.isValidCodeQR(codeQR.getText())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid token");
            return null;
        }

        try {
            DateUtil.DTF_YYYY_MM_DD.parse(day.getText());
        } catch (Exception e) {
            LOG.error("Cannot parse date {}", day.getText());
            return getErrorReason("Date provided is not valid", MOBILE);
        }

        try {
            JsonScheduleList jsonSchedules = scheduleAppointmentService.findBookedAppointmentsForDayAsJson(codeQR.getText(), day.getText());
            return jsonSchedules.asJson();
        } catch (Exception e) {
            LOG.error("Failed getting schedule qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/scheduleForDay",
                "scheduleForDay",
                AppointmentController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    @PostMapping(
        value = "/bookAppointment",
        headers = "Accept=" + MediaType.APPLICATION_JSON_VALUE,
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String bookAppointment(
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
        LOG.debug("ScheduleForDay mail={}, auth={}", mail, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            if (scheduleAppointmentService.doesAppointmentExists(jsonSchedule.getQueueUserId(), jsonSchedule.getCodeQR(), jsonSchedule.getScheduleDate())) {
                LOG.warn("Cannot book when appointment already exists {} {} {}", jsonSchedule.getQueueUserId(), jsonSchedule.getCodeQR(), jsonSchedule.getScheduleDate());
                return getErrorReason("Appointment already exists for this day. Please cancel to re-book for the day.", APPOINTMENT_ALREADY_EXISTS);
            }

            JsonSchedule bookedAppointment;
            if (jsonSchedule.getQueueUserId().equalsIgnoreCase(qid)) {
                bookedAppointment = scheduleAppointmentService.bookAppointment(null, jsonSchedule);
            } else {
                UserProfileEntity userProfile = userProfileManager.findByQueueUserId(qid);
                if (!userProfile.getQidOfDependents().contains(jsonSchedule.getQueueUserId())) {
                    LOG.warn("Attempt to book appointment for non existent dependent {} {} {}", qid, jsonSchedule.getQueueUserId(), jsonSchedule.getCodeQR());
                    return getErrorReason("Something went wrong. Engineers are looking into this.", CANNOT_BOOK_APPOINTMENT);
                }
                bookedAppointment = scheduleAppointmentService.bookAppointment(qid, jsonSchedule);
            }
            return bookedAppointment.asJson();
        } catch (AppointmentBookingException e) {
            LOG.warn("Failed booking appointment qid={}, reason={}", qid, e.getLocalizedMessage());
            methodStatusSuccess = false;
            return getErrorReason(e.getLocalizedMessage(), CANNOT_BOOK_APPOINTMENT);
        } catch (Exception e) {
            LOG.error("Failed booking appointment qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/bookAppointment",
                "bookAppointment",
                AppointmentController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    @PostMapping(
        value = "/cancelAppointment",
        headers = "Accept=" + MediaType.APPLICATION_JSON_VALUE,
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String cancelAppointment(
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
        LOG.debug("Cancel appointment mail={}, auth={}", mail, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            boolean status = scheduleAppointmentService.cancelAppointment(jsonSchedule.getScheduleAppointmentId(), jsonSchedule.getQueueUserId(), jsonSchedule.getCodeQR());
            return new JsonResponse(status).asJson();
        } catch (AppointmentCancellationException e) {
            LOG.warn("Failed cancelling appointments qid={}, reason={}", qid, e.getLocalizedMessage());
            methodStatusSuccess = false;
            return getErrorReason(e.getLocalizedMessage() + " Please call to cancel appointment.", FAILED_TO_CANCEL_APPOINTMENT);
        } catch (Exception e) {
            LOG.error("Failed cancelling appointment qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/cancelAppointment",
                "cancelAppointment",
                AppointmentController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    @GetMapping(
        value = "/all",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String allAppointments(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput dt,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.debug("Future appointments mail={}, auth={}", mail, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            return scheduleAppointmentService.findAllUpComingAppointments(qid).asJson();
        } catch (Exception e) {
            LOG.error("Failed finding all upcoming appointments qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/all",
                "allAppointments",
                AppointmentController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    @GetMapping(
        value = "/allPast",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String allPastAppointments(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput dt,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.debug("Past appointments mail={}, auth={}", mail, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            return scheduleAppointmentService.findAllPastAppointments(qid).asJson();
        } catch (Exception e) {
            LOG.error("Failed finding all past appointments qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/allPast",
                "allPastAppointments",
                AppointmentController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }
}
