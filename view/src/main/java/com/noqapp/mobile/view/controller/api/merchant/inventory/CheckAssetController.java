package com.noqapp.mobile.view.controller.api.merchant.inventory;

import com.noqapp.inventory.service.CheckAssetService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * User: hitender
 * Date: 2019-07-30 01:32
 */
@SuppressWarnings({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/api/m/i")
public class CheckAssetController {
    private static final Logger LOG = LoggerFactory.getLogger(CheckAssetController.class);

    private CheckAssetService checkAssetService;

    @Autowired
    public CheckAssetController(CheckAssetService checkAssetService) {
        this.checkAssetService = checkAssetService;
    }
}
