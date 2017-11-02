package com.noqapp.mobile.view.controller.api.merchant;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.noqapp.domain.json.JsonResponse;
import com.noqapp.utils.ScrubbedInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static com.noqapp.mobile.view.controller.open.DeviceController.validatedIfDeviceVersionSupported;

/**
 * Remote scan of QR code is only available to registered user.
 *
 * User: hitender
 * Date: 3/31/17 7:23 PM
 */
@SuppressWarnings ({
        "PMD.BeanMembersShouldSerialize",
        "PMD.LocalVariableCouldBeFinal",
        "PMD.MethodArgumentCouldBeFinal",
        "PMD.LongVariable"
})
@RestController
@RequestMapping (value = "/api/m/device")
public class DeviceController {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceController.class);

    /**
     * Checks is device version is supported.
     *
     * @param did
     * @param deviceType
     * @param versionRelease
     * @return
     */
    @Timed
    @ExceptionMetered
    @RequestMapping (
            method = RequestMethod.POST,
            value = "/version",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String joinQueue(
            @RequestHeader("X-R-DID")
            ScrubbedInput did,

            @RequestHeader ("X-R-DT")
            ScrubbedInput deviceType,

            @RequestHeader (value = "X-R-VR")
            ScrubbedInput versionRelease
    ) {
        LOG.info("Supported device did={} deviceType={} versionRelease={}", did, deviceType, versionRelease);

        String message = validatedIfDeviceVersionSupported(deviceType.getText(), versionRelease.getText());
        if (message != null) return message;
        return new JsonResponse(true).asJson();
    }
}
