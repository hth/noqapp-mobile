package com.noqapp.mobile.view.controller.api.client;

import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.common.utils.CommonUtil.UNAUTHORIZED;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MOBILE_JSON;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.USER_EXISTING;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

import com.noqapp.common.utils.CommonUtil;
import com.noqapp.common.utils.Formatter;
import com.noqapp.common.utils.ParseJsonStringToMap;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.UserAccountEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.json.JsonUserAddress;
import com.noqapp.domain.json.JsonUserAddressList;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.medical.service.UserMedicalProfileService;
import com.noqapp.mobile.common.util.ErrorEncounteredJson;
import com.noqapp.mobile.domain.JsonProfile;
import com.noqapp.mobile.service.AccountMobileService;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.view.controller.api.ProfileCommonHelper;
import com.noqapp.mobile.view.validator.AccountClientValidator;
import com.noqapp.service.UserAddressService;
import com.noqapp.service.UserProfilePreferenceService;

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
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

/**
 * User: hitender
 * Date: 3/25/17 12:46 AM
 */
@SuppressWarnings ({
        "PMD.BeanMembersShouldSerialize",
        "PMD.LocalVariableCouldBeFinal",
        "PMD.MethodArgumentCouldBeFinal",
        "PMD.LongVariable"
})
@RestController
@RequestMapping (value = "/api/c/profile")
public class ClientProfileAPIController {
    private static final Logger LOG = LoggerFactory.getLogger(ClientProfileAPIController.class);

    private AuthenticateMobileService authenticateMobileService;
    private UserProfilePreferenceService userProfilePreferenceService;
    private ApiHealthService apiHealthService;
    private AccountClientValidator accountClientValidator;
    private AccountMobileService accountMobileService;
    private UserAddressService userAddressService;
    private UserMedicalProfileService userMedicalProfileService;
    private ProfileCommonHelper profileCommonHelper;

    @Autowired
    public ClientProfileAPIController(
            AuthenticateMobileService authenticateMobileService,
            UserProfilePreferenceService userProfilePreferenceService,
            ApiHealthService apiHealthService,
            AccountClientValidator accountClientValidator,
            AccountMobileService accountMobileService,
            UserAddressService userAddressService,
            UserMedicalProfileService userMedicalProfileService,
            ProfileCommonHelper profileCommonHelper
    ) {
        this.authenticateMobileService = authenticateMobileService;
        this.userProfilePreferenceService = userProfilePreferenceService;
        this.apiHealthService = apiHealthService;
        this.accountClientValidator = accountClientValidator;
        this.accountMobileService = accountMobileService;
        this.userAddressService = userAddressService;
        this.userMedicalProfileService = userMedicalProfileService;
        this.profileCommonHelper = profileCommonHelper;
    }

    @GetMapping(
            value = "/fetch",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String fetch(
            @RequestHeader ("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.debug("mail={}, auth={}", mail, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            return accountMobileService.getProfileAsJson(qid);
        } catch(Exception e) {
            LOG.error("Failed getting profile qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                    "/fetch",
                    "fetch",
                    ClientProfileAPIController.class.getName(),
                    Duration.between(start, Instant.now()),
                    methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    @PostMapping(
            value = "/update",
            headers = "Accept=" + MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String update(
            @RequestHeader ("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            @RequestBody
            String updateProfileJson,

            HttpServletResponse response
    ) throws IOException {
        return profileCommonHelper.updateProfile(mail, auth, updateProfileJson, response);
    }

    @PostMapping(
            value="/migrate",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String migrate(
            @RequestHeader ("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            @RequestBody
            String registrationJson,

            HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.debug("mail={}, auth={}", mail, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        Map<String, ScrubbedInput> map;
        try {
            map = ParseJsonStringToMap.jsonStringToMap(registrationJson);
        } catch (IOException e) {
            LOG.error("Could not parse json={} reason={}", registrationJson, e.getLocalizedMessage(), e);
            return ErrorEncounteredJson.toJson("Could not parse JSON", MOBILE_JSON);
        }

        Map<String, String> errors;
        try {
            if (map.isEmpty()) {
                /* Validation failure as there is no data in the map. */
                return ErrorEncounteredJson.toJson(accountClientValidator.validateForPhoneMigration(
                        null,
                        null));
            } else {
                Set<String> unknownKeys = invalidElementsInMapDuringMigration(map);
                if (!unknownKeys.isEmpty()) {
                    /* Validation failure as there are unknown keys. */
                    return ErrorEncounteredJson.toJson("Could not parse " + unknownKeys, MOBILE_JSON);
                }

                /* Required. */
                String phone = StringUtils.deleteWhitespace(map.get(AccountMobileService.ACCOUNT_MIGRATE.PH.name()).getText());
                /* Required. */
                String countryShortName = Formatter.getCountryShortNameFromInternationalPhone(phone);
                /* Required. */
                String timeZone = map.get(AccountMobileService.ACCOUNT_MIGRATE.TZ.name()).getText();

                errors = accountClientValidator.validateForPhoneMigration(
                        phone,
                        countryShortName
                );

                if (!errors.isEmpty()) {
                    return ErrorEncounteredJson.toJson(errors);
                }

                LOG.debug("Check if existing user phone={}", phone);
                UserProfileEntity userProfile = accountMobileService.checkUserExistsByPhone(phone);
                if (null != userProfile) {
                    LOG.info("Failed user login as user found with phone={} cs={}", phone, countryShortName);
                    errors = new HashMap<>();
                    errors.put(ErrorEncounteredJson.REASON, "User already exists. Would you like to recover your account?");
                    errors.put(AccountMobileService.ACCOUNT_MIGRATE.PH.name(), phone);
                    errors.put(ErrorEncounteredJson.SYSTEM_ERROR, USER_EXISTING.name());
                    errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, USER_EXISTING.getCode());
                    return ErrorEncounteredJson.toJson(errors);
                }

                UserAccountEntity userAccount;
                try {
                    String updateAuthenticationKey = accountMobileService.updatePhoneNumber(qid, phone, countryShortName, timeZone);
                    userAccount = accountMobileService.findByQueueUserId(qid);
                    response.addHeader("X-R-MAIL", userAccount.getUserId());
                    response.addHeader("X-R-AUTH", updateAuthenticationKey);

                } catch (Exception e) {
                    LOG.error("Failed migration for user={} reason={}", mail, e.getLocalizedMessage(), e);

                    errors = new HashMap<>();
                    errors.put(ErrorEncounteredJson.REASON, "Something went wrong. Engineers are looking into this.");
                    errors.put(AccountMobileService.ACCOUNT_MIGRATE.PH.name(), phone);
                    errors.put(ErrorEncounteredJson.SYSTEM_ERROR, SEVERE.name());
                    errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, SEVERE.getCode());
                    return ErrorEncounteredJson.toJson(errors);
                }

                userProfile = userProfilePreferenceService.findByQueueUserId(userAccount.getQueueUserId());
                JsonProfile jsonProfile = JsonProfile.newInstance(userProfile);
                jsonProfile.setJsonUserMedicalProfile(userMedicalProfileService.findOneAsJson(qid));

                if (null != userProfile.getQidOfDependents()) {
                    for (String qidOfDependent : userProfile.getQidOfDependents()) {
                        jsonProfile.addDependents(JsonProfile.newInstance(userProfilePreferenceService.findByQueueUserId(qidOfDependent)));
                    }
                }

                return jsonProfile.asJson();
            }
        } catch(Exception e) {
            LOG.error("Failed migrating qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                    "/migrate",
                    "migrate",
                    ClientProfileAPIController.class.getName(),
                    Duration.between(start, Instant.now()),
                    methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Get all user addresses. */
    @GetMapping(
            value = "/address",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String address(
            @RequestHeader ("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.debug("mail={}, auth={}", mail, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            return userAddressService.getAllAsJson(qid).asJson();
        } catch (Exception e) {
            LOG.error("Failed fetching address reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return new JsonUserAddressList().asJson();
        } finally {
            apiHealthService.insert(
                    "/address",
                    "address",
                    ClientProfileAPIController.class.getName(),
                    Duration.between(start, Instant.now()),
                    methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Add user address. */
    @PostMapping(
            value = "/address/add",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String addressAdd(
            @RequestHeader ("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            @RequestBody
            String jsonUserAddressBody,

            HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.debug("mail={}, auth={}", mail, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            Map<String, ScrubbedInput> map;
            try {
                map = ParseJsonStringToMap.jsonStringToMap(jsonUserAddressBody);
            } catch (IOException e) {
                LOG.error("Could not parse json={} reason={}", jsonUserAddressBody, e.getLocalizedMessage(), e);
                return ErrorEncounteredJson.toJson("Could not parse JSON", MOBILE_JSON);
            }

            JsonUserAddressList jsonUserAddressList = userAddressService.getAllAsJson(qid);

            String id = map.get("id").getText();
            String address = StringUtils.capitalize(map.get("ad").getText());

            if (StringUtils.isBlank(id)) {
                id = CommonUtil.generateHexFromObjectId();
                userAddressService.saveAddress(id, qid, address);
            }

            return jsonUserAddressList.addJsonUserAddresses(new JsonUserAddress().setId(id).setAddress(address)).asJson();
        } catch (Exception e) {
            LOG.error("Failed adding address reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return new JsonUserAddressList().asJson();
        } finally {
            apiHealthService.insert(
                    "/address/add",
                    "addressAdd",
                    ClientProfileAPIController.class.getName(),
                    Duration.between(start, Instant.now()),
                    methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Delete user address. */
    @PostMapping(
            value = "/address/delete",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String addressDelete(
            @RequestHeader ("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            @RequestBody
            String jsonUserAddressBody,

            HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.debug("mail={}, auth={}", mail, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            Map<String, ScrubbedInput> map;
            try {
                map = ParseJsonStringToMap.jsonStringToMap(jsonUserAddressBody);
            } catch (IOException e) {
                LOG.error("Could not parse json={} reason={}", jsonUserAddressBody, e.getLocalizedMessage(), e);
                return ErrorEncounteredJson.toJson("Could not parse JSON", MOBILE_JSON);
            }

            JsonUserAddressList jsonUserAddressList = userAddressService.getAllAsJson(qid);

            String id = map.get("id").getText();
            if (StringUtils.isNotBlank(id)) {
                userAddressService.deleteAddress(id, qid);
                jsonUserAddressList.removeJsonUserAddresses(id);
            }

            return jsonUserAddressList.asJson();
        } catch (Exception e) {
            LOG.error("Failed adding address reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return new JsonUserAddressList().asJson();
        } finally {
            apiHealthService.insert(
                    "/address/delete",
                    "addressDelete",
                    ClientProfileAPIController.class.getName(),
                    Duration.between(start, Instant.now()),
                    methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    @RequestMapping (
            method = RequestMethod.POST,
            value = "/upload",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String upload(
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

            @RequestPart("profileImageOfQid")
            ScrubbedInput profileImageOfQid,

            HttpServletResponse response
    ) throws IOException {
        return profileCommonHelper.uploadProfileImage(
                did.getText(),
                dt.getText(),
                mail.getText(),
                auth.getText(),
                profileImageOfQid.getText(),
                multipartFile,
                response);
    }

    private Set<String> invalidElementsInMapDuringMigration(Map<String, ScrubbedInput> map) {
        Set<String> keys = new HashSet<>(map.keySet());
        List<AccountMobileService.ACCOUNT_MIGRATE> enums = new ArrayList<>(Arrays.asList(AccountMobileService.ACCOUNT_MIGRATE.values()));
        for (AccountMobileService.ACCOUNT_MIGRATE registration : enums) {
            keys.remove(registration.name());
        }

        return keys;
    }
}
