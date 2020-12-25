package com.noqapp.mobile.view.controller.api.client;

import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.mobile.view.controller.api.client.TokenQueueAPIController.authorizeRequest;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.json.JsonStoreProductList;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.repository.StoreHourManager;
import com.noqapp.search.elastic.domain.BizStoreElastic;
import com.noqapp.search.elastic.helper.DomainConversion;
import com.noqapp.search.elastic.json.ElasticFavorite;
import com.noqapp.service.BizService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import javax.servlet.http.HttpServletResponse;

/**
 * hitender
 * 12/23/20 11:57 AM
 */
@SuppressWarnings ({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/api/c/favourite")
public class FavouriteController {
    private static final Logger LOG = LoggerFactory.getLogger(FavouriteController.class);

    private BizService bizService;
    private StoreHourManager storeHourManager;
    private AuthenticateMobileService authenticateMobileService;
    private ApiHealthService apiHealthService;

    @Autowired
    public FavouriteController(
        BizService bizService,
        StoreHourManager storeHourManager,
        AuthenticateMobileService authenticateMobileService,
        ApiHealthService apiHealthService
    ) {
        this.bizService = bizService;
        this.storeHourManager = storeHourManager;
        this.authenticateMobileService = authenticateMobileService;
        this.apiHealthService = apiHealthService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public String favorite(
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
        LOG.info("Feedback API for mail={} auth={} did={} dt={}", mail, AUTH_KEY_HIDDEN, did, dt);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        ElasticFavorite elasticFavorite = new ElasticFavorite();
        try {
            for (BizStoreEntity bizStore : bizService.favoriteSuggested(qid)) {
                BizStoreElastic bizStoreElastic = DomainConversion.getAsBizStoreElastic(bizStore, storeHourManager.findAll(bizStore.getId()));
                elasticFavorite.addFavoriteSuggested(bizStoreElastic);
            }

            for (BizStoreEntity bizStore : bizService.favoriteTagged(qid)) {
                BizStoreElastic bizStoreElastic = DomainConversion.getAsBizStoreElastic(bizStore, storeHourManager.findAll(bizStore.getId()));
                elasticFavorite.addFavoriteTagged(bizStoreElastic);
            }

            return elasticFavorite.asJson();
        } catch (Exception e) {
            LOG.error("Failed getting favorite reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return elasticFavorite.asJson();
        } finally {
            apiHealthService.insert(
                "/",
                "favorite",
                FavouriteController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }
}
