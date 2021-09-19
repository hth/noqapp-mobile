package com.noqapp.mobile.view.controller.api.client;

import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.mobile.view.controller.api.client.TokenQueueAPIController.authorizeRequest;

import com.noqapp.common.errors.ErrorEncounteredJson;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.json.JsonResponse;
import com.noqapp.domain.json.marketplace.JsonPropertyRental;
import com.noqapp.domain.market.MarketplaceEntity;
import com.noqapp.domain.market.PropertyRentalEntity;
import com.noqapp.domain.shared.DecodedAddress;
import com.noqapp.domain.shared.Geocode;
import com.noqapp.domain.types.BusinessTypeEnum;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.view.controller.api.ImageCommonHelper;
import com.noqapp.mobile.view.validator.ImageValidator;
import com.noqapp.search.elastic.domain.MarketplaceElastic;
import com.noqapp.search.elastic.domain.MarketplaceElasticList;
import com.noqapp.search.elastic.helper.DomainConversion;
import com.noqapp.search.elastic.service.MarketplaceElasticService;
import com.noqapp.service.ExternalService;
import com.noqapp.service.FileService;
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
@RequestMapping(value = "/api/c/marketplace/propertyRental")
public class MarketplacePropertyRentalController {
    private static final Logger LOG = LoggerFactory.getLogger(MarketplacePropertyRentalController.class);

    private PropertyRentalService propertyRentalService;
    private MarketplaceElasticService marketplaceElasticService;
    private ExternalService externalService;
    private AuthenticateMobileService authenticateMobileService;
    private FileService fileService;
    private ImageCommonHelper imageCommonHelper;
    private ImageValidator imageValidator;
    private ApiHealthService apiHealthService;

    @Autowired
    public MarketplacePropertyRentalController(
        PropertyRentalService propertyRentalService,
        MarketplaceElasticService marketplaceElasticService,
        ExternalService externalService,
        AuthenticateMobileService authenticateMobileService,
        FileService fileService,
        ImageCommonHelper imageCommonHelper,
        ImageValidator imageValidator,
        ApiHealthService apiHealthService
    ) {
        this.propertyRentalService = propertyRentalService;
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
            return marketplaceElastics.asJson();
        } catch (Exception e) {
            LOG.error("Failed finding all posting on marketplace reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return new MarketplaceElasticList().asJson();
        } finally {
            apiHealthService.insert(
                "/",
                "showMyPostOnMarketplace",
                MarketplacePropertyRentalController.class.getName(),
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
        JsonPropertyRental jsonPropertyRental,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Post of marketplace API for mail={} auth={} did={} dt={}", mail, AUTH_KEY_HIDDEN, did, dt);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        try {
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
            populateFrom(propertyRental, jsonPropertyRental, qid);
            propertyRentalService.save(propertyRental);
            MarketplaceElastic marketplaceElastic = DomainConversion.getAsMarketplaceElastic(propertyRental);
            marketplaceElasticService.save(marketplaceElastic);

            return marketplaceElastic.asJson();
        } catch (Exception e) {
            LOG.error("Failed posting on marketplace={} reason={}", jsonPropertyRental.getBusinessType(), e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return new JsonResponse(false).asJson();
        } finally {
            apiHealthService.insert(
                "/",
                "postOnMarketplace",
                MarketplacePropertyRentalController.class.getName(),
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
        String postId,

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
            postId,
            BusinessTypeEnum.PR,
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
        JsonPropertyRental jsonPropertyRental,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Remove image from marketplace for mail={} auth={} did={} dt={}", mail, AUTH_KEY_HIDDEN, did, dt);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        try {
            PropertyRentalEntity propertyRental = propertyRentalService.findOneById(qid, jsonPropertyRental.getId());
            for (String imageId : jsonPropertyRental.getPostImages()) {
                if (propertyRental.getPostImages().contains(imageId)) {
                    fileService.deleteMarketImage(propertyRental.getQueueUserId(), imageId, jsonPropertyRental.getId(), propertyRental.getBusinessType());

                    propertyRental.getPostImages().remove(imageId);
                }
            }
            propertyRentalService.save(propertyRental);

            MarketplaceElastic marketplaceElastic = DomainConversion.getAsMarketplaceElastic(propertyRental);
            marketplaceElasticService.save(marketplaceElastic);
            return marketplaceElastic.asJson();
        } catch (Exception e) {
            LOG.error("Failed removing image from marketplace {} {} reason={}", jsonPropertyRental.getBusinessType(), jsonPropertyRental.getId(), e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return new JsonResponse(false).asJson();
        } finally {
            apiHealthService.insert(
                "/removeImage",
                "removeImage",
                MarketplacePropertyRentalController.class.getName(),
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
        JsonPropertyRental jsonPropertyRental,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("View marketplace API for mail={} auth={} did={} dt={}", mail, AUTH_KEY_HIDDEN, did, dt);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        try {
            MarketplaceElastic marketplaceElastic = DomainConversion.getAsMarketplaceElastic(propertyRentalService.findOneByIdAndViewCount(jsonPropertyRental.getId()));
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
                MarketplacePropertyRentalController.class.getName(),
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
        JsonPropertyRental jsonPropertyRental,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Initiate contact API for mail={} auth={} did={} dt={}", mail, AUTH_KEY_HIDDEN, did, dt);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        try {
            MarketplaceElastic marketplaceElastic = DomainConversion.getAsMarketplaceElastic(propertyRentalService.initiateContactWithMarketplacePostOwner(qid, jsonPropertyRental));
            marketplaceElasticService.save(marketplaceElastic);
            return new JsonResponse(true).asJson();
        } catch (Exception e) {
            LOG.error("Failed initiate contact on marketplace={} reason={}", jsonPropertyRental.getBusinessType(), e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return new JsonResponse(false).asJson();
        } finally {
            apiHealthService.insert(
                "/",
                "initiateContact",
                MarketplacePropertyRentalController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    private void populateFrom(MarketplaceEntity marketplace, JsonPropertyRental jsonPropertyRental, String qid) {
        double[] coordinate;
        String countryShortName;

        boolean isAddressChanged = false;
        if (StringUtils.isNotBlank(marketplace.getId())) {
            isAddressChanged = !marketplace.getTown().equalsIgnoreCase(jsonPropertyRental.getTown()) || !marketplace.getCity().equalsIgnoreCase(jsonPropertyRental.getCity());
        }

        if (isAddressChanged) {
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
            coordinate = jsonPropertyRental.getCoordinate();
            countryShortName = StringUtils.isBlank(jsonPropertyRental.getCountryShortName()) ? "IN" : jsonPropertyRental.getCountryShortName();
        }

        marketplace
            .setQueueUserId(qid)
            .setBusinessType(jsonPropertyRental.getBusinessType())
            .setCoordinate(coordinate)
            .setProductPrice(jsonPropertyRental.getProductPrice())
            .setTitle(jsonPropertyRental.getTitle())
            .setDescription(jsonPropertyRental.getDescription())
            .setPostImages(jsonPropertyRental.getPostImages())
            .setTags(jsonPropertyRental.getTags())
            //viewCount skipped
            //expressedInterestCount skipped
            .setAddress(jsonPropertyRental.getAddress())
            .setCity(jsonPropertyRental.getCity())
            .setTown(jsonPropertyRental.getTown())
            .setCountryShortName(countryShortName)
            .setLandmark(jsonPropertyRental.getLandmark());
    }
}
