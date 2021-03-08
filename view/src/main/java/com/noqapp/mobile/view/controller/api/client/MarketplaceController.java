package com.noqapp.mobile.view.controller.api.client;

import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.mobile.view.controller.api.client.TokenQueueAPIController.authorizeRequest;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.json.JsonResponse;
import com.noqapp.domain.json.marketplace.JsonHouseholdItem;
import com.noqapp.domain.json.marketplace.JsonMarketplace;
import com.noqapp.domain.json.marketplace.JsonPropertyRental;
import com.noqapp.domain.market.HouseholdItemEntity;
import com.noqapp.domain.market.MarketplaceEntity;
import com.noqapp.domain.market.PropertyRentalEntity;
import com.noqapp.domain.shared.DecodedAddress;
import com.noqapp.domain.shared.Geocode;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.search.elastic.domain.MarketplaceElasticList;
import com.noqapp.search.elastic.helper.DomainConversion;
import com.noqapp.search.elastic.service.MarketplaceElasticService;
import com.noqapp.service.ExternalService;
import com.noqapp.service.market.HouseholdItemService;
import com.noqapp.service.market.PropertyRentalService;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

/**
 * hitender
 * 3/6/21 11:55 PM
 */
@SuppressWarnings ({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/api/c/marketplace")
public class MarketplaceController {
    private static final Logger LOG = LoggerFactory.getLogger(MarketplaceController.class);

    private PropertyRentalService propertyRentalService;
    private HouseholdItemService householdItemService;
    private MarketplaceElasticService marketplaceElasticService;
    private ExternalService externalService;
    private AuthenticateMobileService authenticateMobileService;
    private ApiHealthService apiHealthService;

    @Autowired
    public MarketplaceController(
        PropertyRentalService propertyRentalService,
        HouseholdItemService householdItemService,
        MarketplaceElasticService marketplaceElasticService,
        ExternalService externalService,
        AuthenticateMobileService authenticateMobileService,
        ApiHealthService apiHealthService
    ) {
        this.propertyRentalService = propertyRentalService;
        this.householdItemService = householdItemService;
        this.marketplaceElasticService = marketplaceElasticService;
        this.externalService = externalService;
        this.authenticateMobileService = authenticateMobileService;
        this.apiHealthService = apiHealthService;
    }

    /** Finds all my post on marketplace. */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public String showMyPostOnMarketplace(
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
        LOG.info("Load favorite API for mail={} auth={} did={} dt={}", mail, AUTH_KEY_HIDDEN, did, dt);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        MarketplaceElasticList marketplaceElastics = new MarketplaceElasticList();
        try {
            List<PropertyRentalEntity> propertyRentals = propertyRentalService.findPostedProperties(qid);
            for (PropertyRentalEntity propertyRental : propertyRentals) {
                marketplaceElastics.addMarketplaceElastic(DomainConversion.getAsMarketplaceElastic(propertyRental));
            }

            List<HouseholdItemEntity> householdItems = householdItemService.findPostedProperties(qid);
            for (HouseholdItemEntity householdItem : householdItems) {
                marketplaceElastics.addMarketplaceElastic(DomainConversion.getAsMarketplaceElastic(householdItem));
            }
            return marketplaceElastics.asJson();
        } catch (Exception e) {
            LOG.error("Failed finding all posting on marketplace reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return new JsonResponse(false).asJson();
        } finally {
            apiHealthService.insert(
                "/",
                "showMyPostOnMarketplace",
                MarketplaceController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Post on marketplace. */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public String postOnMarketplace(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput dt,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        JsonMarketplace jsonMarketplace,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Load favorite API for mail={} auth={} did={} dt={}", mail, AUTH_KEY_HIDDEN, did, dt);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        try {
            switch (jsonMarketplace.getBusinessType()) {
                case HI:
                    JsonHouseholdItem jsonHouseholdItem = (JsonHouseholdItem) jsonMarketplace;
                    HouseholdItemEntity householdItem;
                    if (StringUtils.isNotBlank(jsonHouseholdItem.getId())) {
                        householdItem = householdItemService.findOneById(jsonHouseholdItem.getId());
                    } else {
                        householdItem = new HouseholdItemEntity()
                            .setItemCondition(jsonHouseholdItem.getItemCondition());
                    }
                    populateFrom(householdItem, jsonMarketplace, qid);
                    marketplaceElasticService.save(DomainConversion.getAsMarketplaceElastic(householdItem));
                    break;
                case PR:
                    JsonPropertyRental jsonPropertyRental = (JsonPropertyRental) jsonMarketplace;
                    PropertyRentalEntity propertyRental;
                    if (StringUtils.isNotBlank(jsonPropertyRental.getId())) {
                        propertyRental = propertyRentalService.findOneById(jsonPropertyRental.getId());
                    } else {
                        propertyRental = new PropertyRentalEntity()
                            .setRentalType(jsonPropertyRental.getRentalType())
                            .setBathroom(jsonPropertyRental.getBathroom())
                            .setBedroom(jsonPropertyRental.getBedroom())
                            .setCarpetArea(jsonPropertyRental.getCarpetArea());

                    }
                    populateFrom(propertyRental, jsonMarketplace, qid);
                    marketplaceElasticService.save(DomainConversion.getAsMarketplaceElastic(propertyRental));
                    break;
                default:
                    //
            }

            return new JsonResponse(true).asJson();
        } catch (Exception e) {
            LOG.error("Failed posting on marketplace={} reason={}", jsonMarketplace.getBusinessType(), e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return new JsonResponse(false).asJson();
        } finally {
            apiHealthService.insert(
                "/",
                "postOnMarketplace",
                MarketplaceController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    private void populateFrom(MarketplaceEntity marketplace, JsonMarketplace jsonMarketplace, String qid) {
        double[] coordinate;
        String countryShortName;

        boolean isAddressChanged = !marketplace.getTown().equalsIgnoreCase(jsonMarketplace.getTown()) || !marketplace.getCity().equalsIgnoreCase(jsonMarketplace.getCity());
        if (StringUtils.isBlank(marketplace.getId()) || isAddressChanged) {
            Geocode geocode;
            if (StringUtils.isNotBlank(marketplace.getAddress())) {
                geocode = Geocode.newInstance(externalService.getGeocodingResults(marketplace.getAddress()), marketplace.getAddress());
            } else {
                geocode = Geocode.newInstance(externalService.getGeocodingResults(marketplace.getTown() + " " + marketplace.getCity()), marketplace.getTown() + " " + marketplace.getCity());
            }
            DecodedAddress decodedAddress = DecodedAddress.newInstance(geocode.getResults(), 0);
            coordinate = decodedAddress.getCoordinate();
            countryShortName = decodedAddress.getCountryShortName();
        } else {
            coordinate = marketplace.getCoordinate();
            countryShortName = marketplace.getCountryShortName();
        }

        marketplace
            .setQueueUserId(qid)
            .setBusinessType(jsonMarketplace.getBusinessType())
            .setCoordinate(coordinate)
            .setProductPrice(jsonMarketplace.getProductPrice())
            .setTitle(jsonMarketplace.getTitle())
            .setDescription(jsonMarketplace.getDescription())
            .setPostImages(jsonMarketplace.getPostImages())
            .setTags(jsonMarketplace.getTags())
            //likeCount skipped
            //expressedInterestCount skipped
            .setAddress(jsonMarketplace.getAddress())
            .setCity(jsonMarketplace.getCity())
            .setTown(jsonMarketplace.getTown())
            .setCountryShortName(countryShortName)
            .setLandmark(jsonMarketplace.getLandmark());
    }
}
