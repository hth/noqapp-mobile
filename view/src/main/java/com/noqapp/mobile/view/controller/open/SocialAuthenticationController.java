package com.noqapp.mobile.view.controller.open;

import com.noqapp.common.utils.ParseJsonStringToMap;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.mobile.common.util.ErrorEncounteredJson;
import com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum;
import com.noqapp.mobile.service.SocialAuthenticationService;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * User: hitender
 * Date: 1/14/17 12:16 PM
 */
@SuppressWarnings ({
        "PMD.BeanMembersShouldSerialize",
        "PMD.LocalVariableCouldBeFinal",
        "PMD.MethodArgumentCouldBeFinal",
        "PMD.LongVariable"
})
@RestController
public class SocialAuthenticationController {
    private static final Logger LOG = LoggerFactory.getLogger(SocialAuthenticationController.class);

    public static final String AUTH = "X-R-AUTH";
    public static final String MAIL = "X-R-MAIL";

    private SocialAuthenticationService socialAuthenticationService;

    @Autowired
    public SocialAuthenticationController(SocialAuthenticationService socialAuthenticationService) {
        this.socialAuthenticationService = socialAuthenticationService;
    }

    /**
     * Social provider signup or login supports.
     * For Facebook, AccessToken token works as is to access user information.
     * For Google, there is access code is fetched and it needs to be executed against the API to get
     * access token and refresh token.
     *
     * http http://localhost:9090/receipt-mobile/authenticate.json < ~/Downloads/pid.json
     * pid.json
     *  {
     *      "pid": "SOCIAL",
     *      "at": "XXXXXXXXX"
     *  }
     *
     *  On Success
     *
     *
     *
     *
     *  On failure
     *
     *  Cache-Control: no-cache, no-store, max-age=0, must-revalidate
     *  Content-Length: 97
     *  Content-Type: application/json;charset=UTF-8
     *  Date: Fri, 11 Jul 2014 04:44:43 GMT
     *  Expires: 0
     *  Pragma: no-cache
     *  Server: Apache-Coyote/1.1
     *  X-Content-Type-Options: nosniff
     *  X-Frame-Options: DENY
     *  X-XSS-Protection: 1; mode=block
     *
     *  {
     *      "error": {
     *          "httpStatus": "UNAUTHORIZED",
     *          "httpStatusCode": 401,
     *          "reason": "denied by provider GOOGLE"
     *      }
     *  }
     *
     * @return
     */
    @PostMapping(
            value = "/authenticate.json",
            headers = "Accept=" + MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String authenticateUser(
            @RequestBody
            String authenticationJson,

            HttpServletResponse response
    ) throws IOException {
        try {
            Map<String, ScrubbedInput> map = ParseJsonStringToMap.jsonStringToMap(authenticationJson);
            LOG.info("pid={} at={}", map.get("pid"), map.get("at"));
            String credential = socialAuthenticationService.authenticateWeb(
                    map.get("pid").getText(),
                    map.get("at").getText(),
                    HttpClientBuilder.create().build());

            if (StringUtils.isBlank(credential) ||
                    credential.contains("systemError") ||
                    credential.contains("401")) {
                return credential;
            }

            Map<String, ScrubbedInput> credentialMap = ParseJsonStringToMap.jsonStringToMap(credential);
            response.addHeader(AUTH, credentialMap.get(AUTH).getText());
            response.addHeader(MAIL, credentialMap.get(MAIL).getText());

            LOG.info("credential={}", credential);
            return credential;
        } catch (IOException e) {
            LOG.error("could not parse json={} reason={}", authenticationJson, e.getLocalizedMessage(), e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            Map<String, String> errors = new HashMap<>();
            errors.put(ErrorEncounteredJson.REASON, "Something went wrong. Engineers are looking into this.");
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR, MobileSystemErrorCodeEnum.USER_INPUT.name());
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, MobileSystemErrorCodeEnum.USER_INPUT.getCode());

            return ErrorEncounteredJson.toJson(errors);
        }
    }
}
