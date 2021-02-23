package com.noqapp.mobile.view.controller.open;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * User: hitender
 * Date: 11/19/16 1:47 PM
 */
@SuppressWarnings({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@Controller
@RequestMapping(value = "/open")
public class IsWorkingController {
    private static final Logger LOG = LoggerFactory.getLogger(IsWorkingController.class);

    /**
     * Supports HTML call.
     * <p>
     * During application start up a call is made to show index page. Hence this method and only this controller
     * contains support for request type HEAD.
     * <p>
     * We have added support for HEAD request in filter to prevent failing on HEAD request. As of now there is no valid
     * reason why filter contains this HEAD request as everything is secure after login and there are no bots or
     * crawlers when a valid user has logged in.
     * <p>
     *
     * @return
     * @see <a href="http://axelfontaine.com/blog/http-head.html">http://axelfontaine.com/blog/http-head.html</a>
     */
    @RequestMapping(
        value = "/isWorking",
        method = {RequestMethod.GET, RequestMethod.HEAD},
        produces = {MediaType.TEXT_HTML_VALUE + ";charset=UTF-8"}
    )
    public String isWorking() {
        LOG.info("isWorking");
        return "isWorking";
    }
}
