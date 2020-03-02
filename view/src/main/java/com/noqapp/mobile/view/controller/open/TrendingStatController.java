package com.noqapp.mobile.view.controller.open;

import com.noqapp.mobile.domain.JsonCoronaStat;
import com.noqapp.mobile.domain.JsonCoronaStatElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * hitender
 * 3/1/20 3:29 AM
 */
@SuppressWarnings ({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/open/trending/stat")
public class TrendingStatController {
    private static final Logger LOG = LoggerFactory.getLogger(TrendingStatController.class);

    /** Get state of queue at the store. */
    @GetMapping(
        value = "/corona",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String coronaStats() {
        JsonCoronaStat jsonCoronaStat = new JsonCoronaStat();
        jsonCoronaStat.addCoronaStat(new JsonCoronaStatElement()
            .setCountry("US")
            .setOfficialFS("100")
            .setOfficialCC("101")
            .setOfficialSS("102")
            .setTrackedCC("103")
            .setTrackedFS("104")
            .setTrackedSS("105")
        );
        jsonCoronaStat.addCoronaStat(
            new JsonCoronaStatElement()
                .setCountry("IN")
                .setOfficialFS("1001")
                .setOfficialCC("1011")
                .setOfficialSS("1021")
                .setTrackedCC("1031")
                .setTrackedFS("1041")
                .setTrackedSS("1051")
        );

        LOG.info("List of data {}", jsonCoronaStat.asJson());
        return jsonCoronaStat.asJson();
    }
}
