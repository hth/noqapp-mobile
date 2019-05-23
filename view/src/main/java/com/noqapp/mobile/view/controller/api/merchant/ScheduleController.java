package com.noqapp.mobile.view.controller.api.merchant;

import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.common.utils.CommonUtil.UNAUTHORIZED;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.json.JsonSchedule;
import com.noqapp.domain.json.JsonScheduleList;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.view.controller.api.client.AppointmentController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private ApiHealthService apiHealthService;

    @Autowired
    public ScheduleController(AuthenticateMobileService authenticateMobileService, ApiHealthService apiHealthService) {
        this.authenticateMobileService = authenticateMobileService;
        this.apiHealthService = apiHealthService;
    }

    @GetMapping(
        value = "/scheduleForMonth/{month}/{codeQR}",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String scheduleForMonth(
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

        try {
            JsonSchedule jsonSchedule1 = new JsonSchedule()
                .setDay("2019-05-25")
                .setTotalAppointments(20);

            JsonSchedule jsonSchedule2 = new JsonSchedule()
                .setDay("2019-05-28")
                .setTotalAppointments(19);

            JsonSchedule jsonSchedule3 = new JsonSchedule()
                .setDay("2019-05-30")
                .setTotalAppointments(3);

            JsonSchedule jsonSchedule4 = new JsonSchedule()
                .setDay("2019-06-05")
                .setTotalAppointments(8);

            JsonSchedule jsonSchedule5 = new JsonSchedule()
                .setDay("2019-06-15")
                .setTotalAppointments(6);

            JsonScheduleList jsonSchedules = new JsonScheduleList();
            jsonSchedules.addJsonSchedule(jsonSchedule1);
            jsonSchedules.addJsonSchedule(jsonSchedule2);
            jsonSchedules.addJsonSchedule(jsonSchedule3);
            jsonSchedules.addJsonSchedule(jsonSchedule4);
            jsonSchedules.addJsonSchedule(jsonSchedule5);
            return jsonSchedules.asJson();
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

        try {
            JsonSchedule jsonSchedule1 = new JsonSchedule()
                .setDay("2019-05-25")
                .setName("Milan")
                .setStartTime("1000")
                .setEndTime("1030");

            JsonSchedule jsonSchedule2 = new JsonSchedule()
                .setDay("2019-05-25")
                .setName("Amit")
                .setStartTime("1030")
                .setEndTime("1045");

            JsonSchedule jsonSchedule3 = new JsonSchedule()
                .setDay("2019-05-25")
                .setName("Jackie")
                .setStartTime("1045")
                .setEndTime("1100");

            JsonSchedule jsonSchedule4 = new JsonSchedule()
                .setDay("2019-05-25")
                .setName("Sunny")
                .setStartTime("1100")
                .setEndTime("1120");

            JsonSchedule jsonSchedule5 = new JsonSchedule()
                .setDay("2019-05-25")
                .setName("Madhu")
                .setStartTime("1120")
                .setEndTime("1145");

            JsonScheduleList jsonSchedules = new JsonScheduleList();
            jsonSchedules.addJsonSchedule(jsonSchedule1);
            jsonSchedules.addJsonSchedule(jsonSchedule2);
            jsonSchedules.addJsonSchedule(jsonSchedule3);
            jsonSchedules.addJsonSchedule(jsonSchedule4);
            jsonSchedules.addJsonSchedule(jsonSchedule5);
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
}
