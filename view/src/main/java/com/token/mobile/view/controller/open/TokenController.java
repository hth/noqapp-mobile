package com.token.mobile.view.controller.open;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.token.domain.json.JsonTokenQueue;
import com.token.domain.json.JsonTokenState;
import com.token.mobile.service.TokenMobileService;
import com.token.utils.ScrubbedInput;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

/**
 * User: hitender
 * Date: 11/17/16 3:12 PM
 */
@SuppressWarnings ({
        "PMD.BeanMembersShouldSerialize",
        "PMD.LocalVariableCouldBeFinal",
        "PMD.MethodArgumentCouldBeFinal",
        "PMD.LongVariable"
})
@RestController
@RequestMapping (value = "/open/token")
public class TokenController {
    private static final Logger LOG = LoggerFactory.getLogger(TokenController.class);

    private TokenMobileService tokenMobileService;

    @Autowired
    public TokenController(TokenMobileService tokenMobileService) {
        this.tokenMobileService = tokenMobileService;
    }

    @Timed
    @ExceptionMetered
    @RequestMapping (
            method = RequestMethod.GET,
            value = "/{codeQR}",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public JsonTokenState getState(
            @PathVariable ("codeQR")
            ScrubbedInput codeQR,

            HttpServletResponse response
    ) throws IOException {
        LOG.info("codeQR={}", codeQR);
        if (!tokenMobileService.isValidCodeQR(codeQR.getText())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid token");
            return null;
        }

        return tokenMobileService.findTokenState(codeQR.getText());
    }

    @Timed
    @ExceptionMetered
    @RequestMapping (
            method = RequestMethod.POST,
            value = "/queue/{codeQR}",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public JsonTokenQueue joinQueue(
            @PathVariable ("codeQR")
            ScrubbedInput codeQR,

            HttpServletResponse response
    ) throws IOException {
        LOG.info("codeQR={}", codeQR);
        if (!tokenMobileService.isValidCodeQR(codeQR.getText())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid token");
            return null;
        }

        JsonTokenQueue  jsonTokenQueue =  new JsonTokenQueue(codeQR.getText())
                .setToken("25").setServingNumber("12");
        return jsonTokenQueue;
    }

}
