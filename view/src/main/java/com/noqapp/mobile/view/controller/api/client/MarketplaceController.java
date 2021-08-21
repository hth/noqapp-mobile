package com.noqapp.mobile.view.controller.api.client;

import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.MOBILE;
import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.mobile.view.controller.api.client.TokenQueueAPIController.authorizeRequest;

import com.noqapp.common.errors.ErrorEncounteredJson;
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
import com.noqapp.domain.types.BusinessTypeEnum;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.domain.body.client.UpdateProfile;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.view.controller.api.ImageCommonHelper;
import com.noqapp.mobile.view.validator.ImageValidator;
import com.noqapp.search.elastic.domain.MarketplaceElastic;
import com.noqapp.search.elastic.domain.MarketplaceElasticList;
import com.noqapp.search.elastic.helper.DomainConversion;
import com.noqapp.search.elastic.service.MarketplaceElasticService;
import com.noqapp.service.ExternalService;
import com.noqapp.service.FileService;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

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
    private FileService fileService;
    private ImageCommonHelper imageCommonHelper;
    private ImageValidator imageValidator;
    private ApiHealthService apiHealthService;

    @Autowired
    public MarketplaceController(
        PropertyRentalService propertyRentalService,
        HouseholdItemService householdItemService,
        MarketplaceElasticService marketplaceElasticService,
        ExternalService externalService,
        AuthenticateMobileService authenticateMobileService,
        FileService fileService,
        ImageCommonHelper imageCommonHelper,
        ImageValidator imageValidator,
        ApiHealthService apiHealthService
    ) {
        this.propertyRentalService = propertyRentalService;
        this.householdItemService = householdItemService;
        this.marketplaceElasticService = marketplaceElasticService;
        this.externalService = externalService;
        this.authenticateMobileService = authenticateMobileService;
        this.fileService = fileService;
        this.imageCommonHelper = imageCommonHelper;
        this.imageValidator = imageValidator;
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
        LOG.info("Show all my marketplace API for mail={} auth={} did={} dt={}", mail, AUTH_KEY_HIDDEN, did, dt);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        MarketplaceElasticList marketplaceElastics = new MarketplaceElasticList();
        try {
            List<PropertyRentalEntity> propertyRentals = propertyRentalService.findPostedByMeOnMarketplace(qid);
            for (PropertyRentalEntity propertyRental : propertyRentals) {
                marketplaceElastics.addMarketplaceElastic(DomainConversion.getAsMarketplaceElastic(propertyRental));
            }

            List<HouseholdItemEntity> householdItems = householdItemService.findPostedByMeOnMarketplace(qid);
            for (HouseholdItemEntity householdItem : householdItems) {
                marketplaceElastics.addMarketplaceElastic(DomainConversion.getAsMarketplaceElastic(householdItem));
            }
            return marketplaceElastics.asJson();
        } catch (Exception e) {
            LOG.error("Failed finding all posting on marketplace reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return new MarketplaceElasticList().asJson();
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
        LOG.info("Post of marketplace API for mail={} auth={} did={} dt={}", mail, AUTH_KEY_HIDDEN, did, dt);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        try {
            MarketplaceElastic marketplaceElastic;
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
                    householdItemService.save(householdItem);
                    marketplaceElastic = DomainConversion.getAsMarketplaceElastic(householdItem);
                    marketplaceElasticService.save(marketplaceElastic);
                    break;
                case PR:
                    JsonPropertyRental jsonPropertyRental = (JsonPropertyRental) jsonMarketplace;
                    PropertyRentalEntity propertyRental;
                    if (StringUtils.isNotBlank(jsonPropertyRental.getId())) {
                        propertyRental = propertyRentalService.findOneById(jsonPropertyRental.getId());
                        if (StringUtils.isBlank(propertyRental.getHousingAgentQID())) {
                            propertyRental.setHousingAgentQID(jsonPropertyRental.getHousingAgentQID());
                        }

                        if (StringUtils.isBlank(propertyRental.getHousingAgentReview())) {
                            propertyRental.setHousingAgentReview(jsonPropertyRental.getHousingAgentReview());
                        }
                    } else {
                        propertyRental = new PropertyRentalEntity()
                            .setRentalType(jsonPropertyRental.getRentalType())
                            .setBathroom(jsonPropertyRental.getBathroom())
                            .setBedroom(jsonPropertyRental.getBedroom())
                            .setCarpetArea(jsonPropertyRental.getCarpetArea())
                            .setRentalAvailableDay(jsonPropertyRental.getRentalAvailableDay());
                    }
                    populateFrom(propertyRental, jsonMarketplace, qid);
                    propertyRentalService.save(propertyRental);
                    marketplaceElastic = DomainConversion.getAsMarketplaceElastic(propertyRental);
                    marketplaceElasticService.save(marketplaceElastic);
                    break;
                default:
                    LOG.warn("Reached unsupported condition {} on /api/c/marketplace by mail={}", jsonMarketplace.getBusinessType(), mail);
                    return ErrorEncounteredJson.toJson("Not supported condition", MOBILE);
            }

            return marketplaceElastic.asJson();
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

    @PostMapping (
        value = "/uploadImage",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String uploadImage(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput dt,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @RequestPart("file")
        MultipartFile multipartFile,

        @RequestPart("postId")
        ScrubbedInput postId,

        @RequestPart("businessTypeAsString")
        ScrubbedInput businessTypeAsString,

        HttpServletResponse response
    ) throws IOException {
        Map<String, String> errors = imageValidator.validate(multipartFile, ImageValidator.SUPPORTED_FILE.IMAGE);
        if (!errors.isEmpty()) {
            return ErrorEncounteredJson.toJson(errors);
        }

        return imageCommonHelper.uploadImageForMarketplace(
            did.getText(),
            dt.getText(),
            mail.getText(),
            auth.getText(),
            postId.getText(),
            BusinessTypeEnum.valueOf(businessTypeAsString.getText()),
            multipartFile,
            response);
    }

    /** Needs PostId, ImageId and Business Type to remove image. */
    @PostMapping (
        value = "/removeImage",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String removeImage(
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
        LOG.info("Remove image from marketplace for mail={} auth={} did={} dt={}", mail, AUTH_KEY_HIDDEN, did, dt);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        try {
            MarketplaceElastic marketplaceElastic;
            switch (jsonMarketplace.getBusinessType()) {
                case PR:
                    PropertyRentalEntity propertyRental = propertyRentalService.findOneById(qid, jsonMarketplace.getId());
                    for (String imageId : jsonMarketplace.getPostImages()) {
                        if (propertyRental.getPostImages().contains(imageId)) {
                            fileService.deleteMarketImage(propertyRental.getQueueUserId(), imageId, jsonMarketplace.getId(), propertyRental.getBusinessType());

                            propertyRental.getPostImages().remove(imageId);
                        }
                    }
                    propertyRentalService.save(propertyRental);
                    marketplaceElastic = DomainConversion.getAsMarketplaceElastic(propertyRental);
                    marketplaceElasticService.save(marketplaceElastic);
                    break;
                case HI:
                    HouseholdItemEntity householdItem = householdItemService.findOneById(qid, jsonMarketplace.getId());
                    for (String imageId : jsonMarketplace.getPostImages()) {
                        if (householdItem.getPostImages().contains(imageId)) {
                            fileService.deleteMarketImage(householdItem.getQueueUserId(), imageId, jsonMarketplace.getId(), householdItem.getBusinessType());

                            householdItem.getPostImages().remove(imageId);
                        }
                    }
                    householdItemService.save(householdItem);
                    marketplaceElastic = DomainConversion.getAsMarketplaceElastic(householdItem);
                    marketplaceElasticService.save(marketplaceElastic);
                    break;
                default:
                    LOG.error("Reached unsupported condition={}", jsonMarketplace.getBusinessType());
                    throw new UnsupportedOperationException("Reached unsupported condition " + jsonMarketplace.getBusinessType());
            }
            return marketplaceElastic.asJson();
        } catch (Exception e) {
            LOG.error("Failed removing image from marketplace {} {} reason={}", jsonMarketplace.getBusinessType(), jsonMarketplace.getId(), e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return new JsonResponse(false).asJson();
        } finally {
            apiHealthService.insert(
                "/removeImage",
                "removeImage",
                MarketplaceController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** View details of the post. */
    @PostMapping(
        value = "/view",
        produces = MediaType.APPLICATION_JSON_VALUE)
    public String viewMarketplace(
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
        LOG.info("View marketplace API for mail={} auth={} did={} dt={}", mail, AUTH_KEY_HIDDEN, did, dt);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        MarketplaceElastic marketplaceElastic;
        try {
            switch (jsonMarketplace.getBusinessType()) {
                case HI:
                    marketplaceElastic = DomainConversion.getAsMarketplaceElastic(propertyRentalService.findOneByIdAndViewCount(jsonMarketplace.getId()));
                    break;
                case PR:
                    marketplaceElastic = DomainConversion.getAsMarketplaceElastic(householdItemService.findOneByIdAndViewCount(jsonMarketplace.getId()));
                    break;
                default:
                    LOG.warn("Reached unsupported condition {} on /api/c/marketplace/view by mail={}", jsonMarketplace.getBusinessType(), mail);
                    return ErrorEncounteredJson.toJson("Not supported condition", MOBILE);
            }
            marketplaceElasticService.save(marketplaceElastic);
            return new JsonResponse(true).asJson();
        } catch (Exception e) {
            LOG.error("Failed finding all posting on marketplace reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return new JsonResponse(false).asJson();
        } finally {
            apiHealthService.insert(
                "/view",
                "viewMarketplace",
                MarketplaceController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Post on marketplace. */
    @PostMapping(
        value = "/initiateContact",
        produces = MediaType.APPLICATION_JSON_VALUE)
    public String initiateContact(
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
        LOG.info("Initiate contact API for mail={} auth={} did={} dt={}", mail, AUTH_KEY_HIDDEN, did, dt);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        try {
            MarketplaceElastic marketplaceElastic;
            switch (jsonMarketplace.getBusinessType()) {
                case HI:
                    marketplaceElastic = DomainConversion.getAsMarketplaceElastic(householdItemService.initiateContactWithMarketplacePostOwner(qid, jsonMarketplace));
                    break;
                case PR:
                    marketplaceElastic = DomainConversion.getAsMarketplaceElastic(propertyRentalService.initiateContactWithMarketplacePostOwner(qid, jsonMarketplace));
                    break;
                default:
                    LOG.warn("Reached unsupported condition {} on /api/c/marketplace/initiateContact by mail={}", jsonMarketplace.getBusinessType(), mail);
                    return ErrorEncounteredJson.toJson("Not supported condition", MOBILE);
            }
            marketplaceElasticService.save(marketplaceElastic);
            return new JsonResponse(true).asJson();
        } catch (Exception e) {
            LOG.error("Failed initiate contact on marketplace={} reason={}", jsonMarketplace.getBusinessType(), e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return new JsonResponse(false).asJson();
        } finally {
            apiHealthService.insert(
                "/",
                "initiateContact",
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
            //viewCount skipped
            //expressedInterestCount skipped
            .setAddress(jsonMarketplace.getAddress())
            .setCity(jsonMarketplace.getCity())
            .setTown(jsonMarketplace.getTown())
            .setCountryShortName(countryShortName)
            .setLandmark(jsonMarketplace.getLandmark());
    }
}
