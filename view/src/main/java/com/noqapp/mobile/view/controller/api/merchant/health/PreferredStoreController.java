package com.noqapp.mobile.view.controller.api.merchant.health;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.types.BusinessTypeEnum;
import com.noqapp.service.BizService;
import com.noqapp.service.PreferredBusinessService;

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

import javax.servlet.http.HttpServletResponse;

/**
 * User: hitender
 * Date: 8/12/18 2:49 PM
 */
@SuppressWarnings({
        "PMD.BeanMembersShouldSerialize",
        "PMD.LocalVariableCouldBeFinal",
        "PMD.MethodArgumentCouldBeFinal",
        "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/api/m/h/preferredStore")
public class PreferredStoreController {
    private static final Logger LOG = LoggerFactory.getLogger(PreferredStoreController.class);

    private BizService bizService;
    private PreferredBusinessService preferredBusinessService;

    @Autowired
    public PreferredStoreController(BizService bizService, PreferredBusinessService preferredBusinessService) {
        this.bizService = bizService;
        this.preferredBusinessService = preferredBusinessService;
    }

    /** Gets preferred business stores if any for business type. */
    @GetMapping(
            value = "/{businessType}/{codeQR}",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String getPreferredStoresByBusinessType(
            @RequestHeader("X-R-DID")
            ScrubbedInput did,

            @RequestHeader ("X-R-DT")
            ScrubbedInput deviceType,

            @RequestHeader ("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            @PathVariable("businessType")
            ScrubbedInput businessType,

            @PathVariable("codeQR")
            ScrubbedInput codeQR,

            HttpServletResponse response
    ) throws IOException {
        BizStoreEntity bizStore = bizService.findByCodeQR(codeQR.getText());
        return preferredBusinessService.findAllAsJson(bizStore, BusinessTypeEnum.valueOf(businessType.getText())).asJson();
    }

    /** Gets all preferred business stores. */
    @GetMapping(
            value = "/{codeQR}",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String getAllPreferredStores(
            @RequestHeader("X-R-DID")
            ScrubbedInput did,

            @RequestHeader ("X-R-DT")
            ScrubbedInput deviceType,

            @RequestHeader ("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            @PathVariable("businessType")
            ScrubbedInput businessType,

            @PathVariable("codeQR")
            ScrubbedInput codeQR,

            HttpServletResponse response
    ) throws IOException {
        BizStoreEntity bizStore = bizService.findByCodeQR(codeQR.getText());
        return preferredBusinessService.findAllAsJson(bizStore, BusinessTypeEnum.valueOf(businessType.getText())).asJson();
    }
}
