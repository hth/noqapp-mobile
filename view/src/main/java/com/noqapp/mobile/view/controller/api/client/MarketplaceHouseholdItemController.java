package com.noqapp.mobile.view.controller.api.client;

import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.mobile.view.controller.api.client.TokenQueueAPIController.authorizeRequest;

import com.noqapp.common.errors.ErrorEncounteredJson;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.json.JsonResponse;
import com.noqapp.domain.json.marketplace.JsonHouseholdItem;
import com.noqapp.domain.json.marketplace.JsonMarketplaceList;
import com.noqapp.domain.market.HouseholdItemEntity;
import com.noqapp.domain.market.MarketplaceEntity;
import com.noqapp.domain.shared.DecodedAddress;
import com.noqapp.domain.shared.Geocode;
import com.noqapp.domain.types.BusinessTypeEnum;
import com.noqapp.domain.types.ValidateStatusEnum;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.view.controller.api.ImageCommonHelper;
import com.noqapp.mobile.view.util.HttpRequestResponseParser;
import com.noqapp.mobile.view.validator.ImageValidator;
import com.noqapp.search.elastic.domain.MarketplaceElastic;
import com.noqapp.search.elastic.helper.DomainConversion;
import com.noqapp.search.elastic.service.MarketplaceElasticService;
import com.noqapp.service.ExternalService;
import com.noqapp.service.FileService;
import com.noqapp.service.market.HouseholdItemService;

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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * hitender
 * 3/6/21 11:55 PM
 */
@SuppressWarnings({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/api/c/marketplace/householdItem")
public class MarketplaceHouseholdItemController {
    private static final Logger LOG = LoggerFactory.getLogger(com.noqapp.mobile.view.controller.api.client.MarketplacePropertyRentalController.class);

    private HouseholdItemService householdItemService;
    private MarketplaceElasticService marketplaceElasticService;
    private ExternalService externalService;
    private AuthenticateMobileService authenticateMobileService;
    private FileService fileService;
    private ImageCommonHelper imageCommonHelper;
    private ImageValidator imageValidator;
    private ApiHealthService apiHealthService;

    @Autowired
    public MarketplaceHouseholdItemController(
        HouseholdItemService householdItemService,
        MarketplaceElasticService marketplaceElasticService,
        ExternalService externalService,
        AuthenticateMobileService authenticateMobileService,
        FileService fileService,
        ImageCommonHelper imageCommonHelper,
        ImageValidator imageValidator,
        ApiHealthService apiHealthService
    ) {
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

        @RequestHeader("X-R-DT")
        ScrubbedInput dt,

        @RequestHeader("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader("X-R-AUTH")
        ScrubbedInput auth,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Show all my marketplace API for mail={} auth={} did={} dt={}", mail, AUTH_KEY_HIDDEN, did, dt);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        JsonMarketplaceList jsonMarketplaceList = new JsonMarketplaceList();
        try {
            List<HouseholdItemEntity> householdItems = householdItemService.findPostedByMeOnMarketplace(qid);
            for (HouseholdItemEntity householdItem : householdItems) {
                jsonMarketplaceList.addJsonHouseholdItems(householdItem.populateJson());
            }
            return jsonMarketplaceList.asJson();
        } catch (Exception e) {
            LOG.error("Failed finding all posting on marketplace reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return jsonMarketplaceList.asJson();
        } finally {
            apiHealthService.insert(
                "/",
                "showMyPostOnMarketplace",
                com.noqapp.mobile.view.controller.api.client.MarketplacePropertyRentalController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Post on marketplace. */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public String postOnMarketplace(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader("X-R-DT")
        ScrubbedInput dt,

        @RequestHeader("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        JsonHouseholdItem jsonHouseholdItem,

        HttpServletRequest request,
        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Post of marketplace API for mail={} auth={} did={} dt={}", mail, AUTH_KEY_HIDDEN, did, dt);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        try {
            HouseholdItemEntity householdItem;
            if (StringUtils.isNotBlank(jsonHouseholdItem.getId())) {
                householdItem = householdItemService.findOneById(jsonHouseholdItem.getId());
            } else {
                householdItem = new HouseholdItemEntity()
                    .setItemCondition(jsonHouseholdItem.getItemCondition());
            }
            populateFrom(householdItem, jsonHouseholdItem, qid, HttpRequestResponseParser.getClientIpAddress(request));
            householdItemService.save(householdItem);
            return householdItem.populateJson().asJson();
        } catch (Exception e) {
            LOG.error("Failed posting on marketplace={} reason={}", jsonHouseholdItem.getBusinessType(), e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return new JsonResponse(false).asJson();
        } finally {
            apiHealthService.insert(
                "/",
                "postOnMarketplace",
                com.noqapp.mobile.view.controller.api.client.MarketplacePropertyRentalController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    @PostMapping(
        value = "/uploadImage",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String uploadImage(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader("X-R-DT")
        ScrubbedInput dt,

        @RequestHeader("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader("X-R-AUTH")
        ScrubbedInput auth,

        @RequestPart("file")
        MultipartFile multipartFile,

        @RequestPart("postId")
        ScrubbedInput postId,

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
            BusinessTypeEnum.HI,
            multipartFile,
            response);
    }

    /** Needs PostId, ImageId and Business Type to remove image. */
    @PostMapping(
        value = "/removeImage",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String removeImage(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader("X-R-DT")
        ScrubbedInput dt,

        @RequestHeader("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        JsonHouseholdItem jsonHouseholdItem,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Remove image from marketplace for mail={} auth={} did={} dt={}", mail, AUTH_KEY_HIDDEN, did, dt);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        try {
            HouseholdItemEntity householdItem = householdItemService.findOneById(qid, jsonHouseholdItem.getId());
            for (String imageId : jsonHouseholdItem.getPostImages()) {
                if (householdItem.getPostImages().contains(imageId)) {
                    fileService.deleteMarketImage(householdItem.getQueueUserId(), imageId, jsonHouseholdItem.getId(), householdItem.getBusinessType());

                    householdItem.getPostImages().remove(imageId);
                }
            }
            householdItemService.save(householdItem);

            MarketplaceElastic marketplaceElastic = DomainConversion.getAsMarketplaceElastic(householdItem);
            marketplaceElasticService.save(marketplaceElastic);
            return marketplaceElastic.asJson();
        } catch (Exception e) {
            LOG.error("Failed removing image from marketplace {} {} reason={}", jsonHouseholdItem.getBusinessType(), jsonHouseholdItem.getId(), e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return new JsonResponse(false).asJson();
        } finally {
            apiHealthService.insert(
                "/removeImage",
                "removeImage",
                com.noqapp.mobile.view.controller.api.client.MarketplacePropertyRentalController.class.getName(),
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

        @RequestHeader("X-R-DT")
        ScrubbedInput dt,

        @RequestHeader("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        JsonHouseholdItem jsonHouseholdItem,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("View marketplace API for mail={} auth={} did={} dt={}", mail, AUTH_KEY_HIDDEN, did, dt);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        try {
            MarketplaceElastic marketplaceElastic = DomainConversion.getAsMarketplaceElastic(householdItemService.findOneByIdAndViewCount(jsonHouseholdItem.getId()));
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
                com.noqapp.mobile.view.controller.api.client.MarketplacePropertyRentalController.class.getName(),
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

        @RequestHeader("X-R-DT")
        ScrubbedInput dt,

        @RequestHeader("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        JsonHouseholdItem jsonHouseholdItem,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Initiate contact API for mail={} auth={} did={} dt={}", mail, AUTH_KEY_HIDDEN, did, dt);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        try {
            MarketplaceElastic marketplaceElastic = DomainConversion.getAsMarketplaceElastic(householdItemService.initiateContactWithMarketplacePostOwner(qid, jsonHouseholdItem));
            marketplaceElasticService.save(marketplaceElastic);
            return new JsonResponse(true).asJson();
        } catch (Exception e) {
            LOG.error("Failed initiate contact on marketplace={} reason={}", jsonHouseholdItem.getBusinessType().name(), e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return new JsonResponse(false).asJson();
        } finally {
            apiHealthService.insert(
                "/",
                "initiateContact",
                com.noqapp.mobile.view.controller.api.client.MarketplacePropertyRentalController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    private void populateFrom(MarketplaceEntity marketplace, JsonHouseholdItem jsonHouseholdItem, String qid, String ip) {
        double[] coordinate;
        String countryShortName;

        boolean isAddressChanged = false;
        if (StringUtils.isNotBlank(marketplace.getId())) {
            isAddressChanged = !marketplace.getTown().equalsIgnoreCase(jsonHouseholdItem.getTown()) || !marketplace.getCity().equalsIgnoreCase(jsonHouseholdItem.getCity());
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
            coordinate = jsonHouseholdItem.getCoordinate();
            countryShortName = StringUtils.isBlank(jsonHouseholdItem.getCountryShortName()) ? "IN" : jsonHouseholdItem.getCountryShortName();
        }

        marketplace
            .setQueueUserId(qid)
            .setBusinessType(jsonHouseholdItem.getBusinessType())
            .setCoordinate(coordinate)
            .setProductPrice(jsonHouseholdItem.getProductPrice())
            .setTitle(jsonHouseholdItem.getTitle())
            .setDescription(jsonHouseholdItem.getDescription())
            .setPostImages(jsonHouseholdItem.getPostImages())
            .setTags(jsonHouseholdItem.getTags())
            //viewCount skipped
            //expressedInterestCount skipped
            .setAddress(jsonHouseholdItem.getAddress())
            .setCity(jsonHouseholdItem.getCity())
            .setTown(jsonHouseholdItem.getTown())
            .setCountryShortName(countryShortName)
            .setLandmark(jsonHouseholdItem.getLandmark())
            //publishUntil skipped
            //validateByQid skipped
            .setValidateStatus(ValidateStatusEnum.I)
            .setIpAddress(ip);
    }
}
