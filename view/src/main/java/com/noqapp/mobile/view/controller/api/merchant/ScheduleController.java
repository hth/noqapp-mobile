package com.noqapp.mobile.view.controller.api.merchant;

import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.JOINING_NOT_PRE_APPROVED_QUEUE;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.JOINING_QUEUE_PERMISSION_DENIED;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.JOIN_PRE_APPROVED_QUEUE_ONLY;
import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.common.utils.CommonUtil.UNAUTHORIZED;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.APPOINTMENT_ACTION_NOT_PERMITTED;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.APPOINTMENT_ALREADY_EXISTS;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.CANNOT_BOOK_APPOINTMENT;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.FAILED_TO_FIND_APPOINTMENT;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.FAILED_TO_RESCHEDULE_APPOINTMENT;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.MOBILE;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.MOBILE_JSON;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

import com.noqapp.common.errors.ErrorEncounteredJson;
import com.noqapp.common.utils.DateUtil;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.ScheduleAppointmentEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.json.JsonSchedule;
import com.noqapp.domain.json.JsonScheduleList;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.domain.body.merchant.BookSchedule;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.view.controller.api.client.AppointmentController;
import com.noqapp.repository.UserProfileManager;
import com.noqapp.service.BizService;
import com.noqapp.service.BusinessUserStoreService;
import com.noqapp.service.JoinAbortService;
import com.noqapp.service.ScheduleAppointmentService;
import com.noqapp.service.exceptions.AppointmentBookingException;
import com.noqapp.service.exceptions.JoiningNonApprovedQueueException;
import com.noqapp.service.exceptions.JoiningQueuePermissionDeniedException;
import com.noqapp.service.exceptions.JoiningQueuePreApprovedRequiredException;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    private UserProfileManager userProfileManager;

    private AuthenticateMobileService authenticateMobileService;
    private ScheduleAppointmentService scheduleAppointmentService;
    private BusinessUserStoreService businessUserStoreService;
    private BizService bizService;
    private JoinAbortService joinAbortService;
    private ApiHealthService apiHealthService;

    private int rescheduleLimit;

    @Autowired
    public ScheduleController(
        @Value("${rescheduleLimit:3}")
        int rescheduleLimit,

        UserProfileManager userProfileManager,
        AuthenticateMobileService authenticateMobileService,
        ScheduleAppointmentService scheduleAppointmentService,
        BusinessUserStoreService businessUserStoreService,
        BizService bizService,
        JoinAbortService joinAbortService,
        ApiHealthService apiHealthService
    ) {
        this.rescheduleLimit = rescheduleLimit;
        this.userProfileManager = userProfileManager;

        this.authenticateMobileService = authenticateMobileService;
        this.scheduleAppointmentService = scheduleAppointmentService;
        this.businessUserStoreService = businessUserStoreService;
        this.bizService = bizService;
        this.joinAbortService = joinAbortService;
        this.apiHealthService = apiHealthService;
    }

    @GetMapping(
        value = "/scheduleForMonth/{month}/{codeQR}",
        produces = MediaType.APPLICATION_JSON_VALUE
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
        LOG.debug("ScheduleForMonth {} {} mail={}, auth={}", month.getText(), codeQR.getText(), mail, AUTH_KEY_HIDDEN);
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
            DateUtil.DTF_YYYY_MM_DD.parse(month.getText());
        } catch (Exception e) {
            LOG.error("Cannot parse date {}", month.getText());
            return getErrorReason("Date provided is not valid", MOBILE);
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
        produces = MediaType.APPLICATION_JSON_VALUE
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
            DateUtil.DTF_YYYY_MM_DD.parse(day.getText());
        } catch (Exception e) {
            LOG.error("Cannot parse date {}", day.getText());
            return getErrorReason("Date provided is not valid", MOBILE);
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
        produces = MediaType.APPLICATION_JSON_VALUE
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

    @PostMapping(
        value = "/bookSchedule",
        headers = "Accept=" + MediaType.APPLICATION_JSON_VALUE,
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String bookSchedule(
        @RequestHeader ("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        BookSchedule bookSchedule,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.debug("Book schedule mail={}, auth={} action={}", mail, AUTH_KEY_HIDDEN, bookSchedule.getBookActionType().name());
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            switch (bookSchedule.getBookActionType()) {
                case ADD:
                    try {
                        return addBooking(bookSchedule);
                    } catch (AppointmentBookingException e) {
                        LOG.warn("Failed booking appointment qid={} {}, reason={}", qid, bookSchedule.getBookActionType(), e.getLocalizedMessage());
                        methodStatusSuccess = false;
                        return getErrorReason(e.getLocalizedMessage(), CANNOT_BOOK_APPOINTMENT);
                    }
                case EDIT:
                    return editBooking(bookSchedule);
            }

            return getErrorReason("Cannot decipher action", MOBILE);
        } catch (JoiningQueuePreApprovedRequiredException e) {
            LOG.warn("Store has to pre-approve qid={}, reason={}", qid, e.getLocalizedMessage());
            methodStatusSuccess = true;
            return ErrorEncounteredJson.toJson("Store has to pre-approve. Please complete pre-approval before joining the queue.", JOIN_PRE_APPROVED_QUEUE_ONLY);
        } catch (JoiningNonApprovedQueueException e) {
            LOG.warn("This queue is not approved qid={}, reason={}", qid, e.getLocalizedMessage());
            methodStatusSuccess = true;
            return ErrorEncounteredJson.toJson("This queue is not approved. Select correct pre-approved queue.", JOINING_NOT_PRE_APPROVED_QUEUE);
        } catch (JoiningQueuePermissionDeniedException e) {
            LOG.warn("Store prevented user from joining queue qid={}, reason={}", qid, e.getLocalizedMessage());
            methodStatusSuccess = true;
            return ErrorEncounteredJson.toJson("Store has denied you from joining the queue. Please contact store for resolving this issue.", JOINING_QUEUE_PERMISSION_DENIED);
        } catch (Exception e) {
            LOG.error("Failed performing action on appointment qid={} {}, reason={}", qid, bookSchedule.getBookActionType(), e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/bookSchedule",
                "bookSchedule",
                ScheduleController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    private String addBooking(BookSchedule bookSchedule) {
        JsonSchedule jsonSchedule = bookSchedule.getJsonSchedule();

        /* Check if business only allow approved customer. */
        BizStoreEntity bizStore = bizService.findByCodeQR(jsonSchedule.getCodeQR());
        joinAbortService.checkCustomerApprovedForTheQueue(
            StringUtils.isBlank(jsonSchedule.getGuardianQid()) ? jsonSchedule.getQueueUserId() : jsonSchedule.getGuardianQid(),
            bizStore);

        if (scheduleAppointmentService.doesAppointmentExists(jsonSchedule.getQueueUserId(), jsonSchedule.getCodeQR(), jsonSchedule.getScheduleDate())) {
            LOG.warn("Cannot book when appointment already exists {} {} {}", jsonSchedule.getQueueUserId(), jsonSchedule.getCodeQR(), jsonSchedule.getScheduleDate());
            return getErrorReason("Appointment already exists for this day. Please cancel to re-book for the day.", APPOINTMENT_ALREADY_EXISTS);
        }

        JsonSchedule bookedAppointment;
        if (StringUtils.isBlank(bookSchedule.getJsonSchedule().getGuardianQid())) {
            bookedAppointment = scheduleAppointmentService.bookAppointment(null, bookSchedule.getJsonSchedule());
        } else {
            UserProfileEntity userProfile = userProfileManager.findByQueueUserId(bookSchedule.getJsonSchedule().getGuardianQid());
            if (!userProfile.getQidOfDependents().contains(bookSchedule.getJsonSchedule().getQueueUserId())) {
                LOG.warn("Attempt to book appointment for non existent dependent {} {} {}",
                    bookSchedule.getJsonSchedule().getQueueUserId(), bookSchedule.getJsonSchedule().getQueueUserId(), bookSchedule.getJsonSchedule().getCodeQR());
                return getErrorReason("Something went wrong. Engineers are looking into this.", CANNOT_BOOK_APPOINTMENT);
            }
            bookedAppointment = scheduleAppointmentService.bookAppointment(bookSchedule.getJsonSchedule().getQueueUserId(), bookSchedule.getJsonSchedule());
        }

        return bookedAppointment.asJson();
    }

    private String editBooking(BookSchedule bookSchedule) {
        JsonSchedule jsonSchedule = bookSchedule.getJsonSchedule();
        ScheduleAppointmentEntity scheduleAppointment = scheduleAppointmentService.findAppointment(jsonSchedule.getScheduleAppointmentId(), jsonSchedule.getQueueUserId(), jsonSchedule.getCodeQR());
        if (null == scheduleAppointment) {
            LOG.warn("Could not find appointment {} {} {}", jsonSchedule.getQueueUserId(), jsonSchedule.getCodeQR(), jsonSchedule.getScheduleDate());
            return getErrorReason("Appointment does not exists for this day. Failed to update.", FAILED_TO_FIND_APPOINTMENT);
        }

        if (scheduleAppointment.getRescheduleCount() >= rescheduleLimit) {
            LOG.warn("Reached re-schedule limit {} {} {}", jsonSchedule.getQueueUserId(), jsonSchedule.getCodeQR(), jsonSchedule.getScheduleDate());
            return getErrorReason("Cannot re-schedule appointment as it has been re-schedule " + rescheduleLimit + " times.", FAILED_TO_RESCHEDULE_APPOINTMENT);
        }

        return scheduleAppointmentService.rescheduleAppointment(jsonSchedule).asJson();
    }
}
