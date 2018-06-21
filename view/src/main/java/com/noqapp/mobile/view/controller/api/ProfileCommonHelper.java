package com.noqapp.mobile.view.controller.api;

import com.noqapp.common.utils.FileUtil;
import com.noqapp.common.utils.ParseJsonStringToMap;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.flow.RegisterUser;
import com.noqapp.domain.json.JsonResponse;
import com.noqapp.domain.types.AddressOriginEnum;
import com.noqapp.domain.types.GenderEnum;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.common.util.ErrorEncounteredJson;
import com.noqapp.mobile.common.util.ExtractFirstLastName;
import com.noqapp.mobile.service.AccountMobileService;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.view.controller.api.client.ClientProfileAPIController;
import com.noqapp.mobile.view.validator.AccountClientValidator;
import com.noqapp.service.AccountService;
import com.noqapp.service.FileService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.common.utils.CommonUtil.UNAUTHORIZED;
import static com.noqapp.common.utils.FileUtil.getFileExtensionWithDot;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MOBILE_JSON;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MOBILE_UPLOAD;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

/**
 * Common code shared between Client and Merchant to update profile.
 * hitender
 * 6/12/18 1:59 AM
 */
@Controller
public class ProfileCommonHelper {
    private static final Logger LOG = LoggerFactory.getLogger(ProfileCommonHelper.class);

    private AuthenticateMobileService authenticateMobileService;
    private AccountClientValidator accountClientValidator;
    private AccountService accountService;
    private AccountMobileService accountMobileService;
    private FileService fileService;
    private ApiHealthService apiHealthService;

    @Autowired
    public ProfileCommonHelper(
            AuthenticateMobileService authenticateMobileService,
            AccountClientValidator accountClientValidator,
            AccountService accountService,
            AccountMobileService accountMobileService,
            FileService fileService,
            ApiHealthService apiHealthService
    ) {
        this.authenticateMobileService = authenticateMobileService;
        this.accountClientValidator = accountClientValidator;
        this.accountService = accountService;
        this.accountMobileService = accountMobileService;
        this.fileService = fileService;
        this.apiHealthService = apiHealthService;
    }

    /**
     * Update profile does not change phone number or email address.
     *
     * @param qidOfSubmitter
     * @param updateProfileJson
     * @param response
     * @return
     */
    private String updateProfile(String qidOfSubmitter, String updateProfileJson, HttpServletResponse response) {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();

        Map<String, ScrubbedInput> map;
        try {
            map = ParseJsonStringToMap.jsonStringToMap(updateProfileJson);
        } catch (IOException e) {
            LOG.error("Could not parse json={} reason={}", updateProfileJson, e.getLocalizedMessage(), e);
            return ErrorEncounteredJson.toJson("Could not parse JSON", MOBILE_JSON);
        }

        Map<String, String> errors;
        String qid = null;
        try {
            if (map.isEmpty()) {
                /* Validation failure as there is no data in the map. */
                return ErrorEncounteredJson.toJson(accountClientValidator.validate(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null));
            } else {
                Set<String> unknownKeys = invalidElementsInMapDuringUpdate(map);
                if (!unknownKeys.isEmpty()) {
                    /* Validation failure as there are unknown keys. */
                    return ErrorEncounteredJson.toJson("Could not parse " + unknownKeys, MOBILE_JSON);
                }

                /* Required. But not changing the phone number. For that we have phone migrate API. */
                qid = map.get(AccountMobileService.ACCOUNT_UPDATE.QID.name()).getText();
                UserProfileEntity userProfile = checkSelfOrDependent(qidOfSubmitter, qid);
                if (null == userProfile) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
                    return null;
                }

                String phone = userProfile.getPhone();
                /* Required. */
                String firstName = WordUtils.capitalize(map.get(AccountMobileService.ACCOUNT_UPDATE.FN.name()).getText());
                String lastName = null;
                if (StringUtils.isNotBlank(firstName)) {
                    ExtractFirstLastName extractFirstLastName = new ExtractFirstLastName(firstName);
                    firstName = extractFirstLastName.getFirstName();
                    lastName = extractFirstLastName.getLastName();
                }

                ScrubbedInput address = map.get(AccountMobileService.ACCOUNT_UPDATE.AD.name());
                ScrubbedInput birthday = map.get(AccountMobileService.ACCOUNT_UPDATE.BD.name());
                /* Required. */
                String gender = map.get(AccountMobileService.ACCOUNT_UPDATE.GE.name()).getText();
                /* Required. */
                String countryShortName = userProfile.getCountryShortName();
                /* Required. */
                ScrubbedInput timeZone = map.get(AccountMobileService.ACCOUNT_UPDATE.TZ.name());

                errors = accountClientValidator.validate(
                        phone,
                        map.get(AccountMobileService.ACCOUNT_UPDATE.FN.name()).getText(),
                        userProfile.getEmail(),
                        birthday.getText(),
                        gender,
                        countryShortName,
                        timeZone.getText()
                );

                if (!errors.isEmpty()) {
                    return ErrorEncounteredJson.toJson(errors);
                }

                RegisterUser registerUser = new RegisterUser()
                        .setEmail(new ScrubbedInput(userProfile.getEmail()))
                        .setQueueUserId(qid)
                        .setFirstName(new ScrubbedInput(firstName))
                        .setLastName(new ScrubbedInput(lastName))
                        .setAddress(address)
                        .setAddressOrigin(AddressOriginEnum.S)
                        .setBirthday(birthday)
                        .setGender(GenderEnum.valueOf(gender))
                        .setCountryShortName(new ScrubbedInput(countryShortName))
                        .setTimeZone(timeZone)
                        .setPhone(new ScrubbedInput(phone));
                accountService.updateUserProfile(registerUser, userProfile.getEmail());
            }

            return accountMobileService.getProfileAsJson(qidOfSubmitter);
        } catch (Exception e) {
            LOG.error("Failed updating profile qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                    "/updateProfile",
                    "updateProfile",
                    ProfileCommonHelper.class.getName(),
                    Duration.between(start, Instant.now()),
                    methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /**
     * Check if the person is self or a guardian of the dependent.
     *
     * @param qidOfSubmitter    Guardian
     * @param qid               Qid of the person who's record is being modified
     * @return
     */
    private UserProfileEntity checkSelfOrDependent(String qidOfSubmitter, String qid) {
        UserProfileEntity userProfile = accountService.findProfileByQueueUserId(qid);
        if (!qidOfSubmitter.equalsIgnoreCase(userProfile.getQueueUserId())) {
            UserProfileEntity userProfileGuardian = accountService.checkUserExistsByPhone(userProfile.getGuardianPhone());

            if (!qidOfSubmitter.equalsIgnoreCase(userProfileGuardian.getQueueUserId())) {
                LOG.info("Profile user does not match with QID of submitter nor is a guardian {} {}",
                        qidOfSubmitter,
                        userProfileGuardian.getQueueUserId());
                return null;
            }
        }
        return userProfile;
    }

    public String updateProfile(
            ScrubbedInput mail,
            ScrubbedInput auth,
            String updateProfileJson,
            HttpServletResponse response
    ) throws IOException {
        LOG.debug("mail={}, auth={}", mail, AUTH_KEY_HIDDEN);
        String qidOfSubmitter = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qidOfSubmitter) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        LOG.info("Profile update being performed by qidOfSubmitter={}", qidOfSubmitter);
        return updateProfile(qidOfSubmitter, updateProfileJson, response);
    }

    public String uploadProfileImage(
            ScrubbedInput did,
            ScrubbedInput dt,
            ScrubbedInput mail,
            ScrubbedInput auth,
            MultipartFile multipartFile,
            HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = false;
        Instant start = Instant.now();
        LOG.info("Profile Image upload dt={} did={} mail={}, auth={}", dt, did, mail, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        if (multipartFile.isEmpty()) {
            LOG.error("File name missing in request or no file uploaded");
            return ErrorEncounteredJson.toJson("File missing in request or no file uploaded.", MOBILE_UPLOAD);
        }

        try {
            processProfileImage(qid, multipartFile);
            methodStatusSuccess = true;
            return new JsonResponse(true).asJson();
        } catch (Exception e) {
            LOG.error("Failed adding address reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return new JsonResponse(false).asJson();
        } finally {
            apiHealthService.insert(
                    "/upload",
                    "upload",
                    ClientProfileAPIController.class.getName(),
                    Duration.between(start, Instant.now()),
                    methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    private Set<String> invalidElementsInMapDuringUpdate(Map<String, ScrubbedInput> map) {
        Set<String> keys = new HashSet<>(map.keySet());
        List<AccountMobileService.ACCOUNT_UPDATE> enums = new ArrayList<>(Arrays.asList(AccountMobileService.ACCOUNT_UPDATE.values()));
        for (AccountMobileService.ACCOUNT_UPDATE registration : enums) {
            keys.remove(registration.name());
        }

        return keys;
    }

    private void processProfileImage(String qid, MultipartFile multipartFile) throws IOException {
        BufferedImage bufferedImage = fileService.bufferedImage(multipartFile.getInputStream());
        String mimeType = FileUtil.detectMimeType(multipartFile.getInputStream());
        if (mimeType.equalsIgnoreCase(multipartFile.getContentType())) {
            String profileFilename = FileUtil.createRandomFilenameOf24Chars() + getFileExtensionWithDot(multipartFile.getOriginalFilename());
            fileService.addProfileImage(qid, profileFilename, bufferedImage);
        } else {
            LOG.error("Failed mime mismatch found={} sentMime={}", mimeType, multipartFile.getContentType());
            throw new RuntimeException("Mime type mismatch");
        }
    }
}
