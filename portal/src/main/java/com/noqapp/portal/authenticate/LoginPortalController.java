package com.noqapp.portal.authenticate;

import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.ACCOUNT_INACTIVE;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.USER_INPUT;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.USER_NOT_FOUND;

import com.noqapp.common.errors.ErrorEncounteredJson;
import com.noqapp.common.errors.MobileSystemErrorCodeEnum;
import com.noqapp.common.utils.Formatter;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.UserAccountEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.types.DeviceTypeEnum;
import com.noqapp.mobile.service.AccountMobileService;
import com.noqapp.portal.body.Login;
import com.noqapp.mobile.service.DeviceRegistrationService;
import com.noqapp.service.AccountService;
import com.noqapp.service.FirebaseService;
import com.noqapp.social.exception.AccountNotActiveException;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

/**
 * hitender
 * 3/23/20 4:20 PM
 */
@SuppressWarnings({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/portal/login")
public class LoginPortalController {
    private static final Logger LOG = LoggerFactory.getLogger(LoginPortalController.class);

    private AccountService accountService;
    private DeviceRegistrationService deviceRegistrationService;
    private AccountMobileService accountMobileService;
    private FirebaseService firebaseService;

    @Autowired
    public LoginPortalController(
        AccountService accountService,
        DeviceRegistrationService deviceRegistrationService,
        AccountMobileService accountMobileService,
        FirebaseService firebaseService
    ) {
        this.accountService = accountService;
        this.deviceRegistrationService = deviceRegistrationService;
        this.accountMobileService = accountMobileService;
        this.firebaseService = firebaseService;
    }

    @PostMapping(
        headers = "Accept=" + MediaType.APPLICATION_JSON_VALUE,
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String login(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader("X-R-DT")
        ScrubbedInput deviceType,

        @RequestBody
        Login loginJson,

        HttpServletResponse response
    ) {
        /* Required. */
        String phone = StringUtils.deleteWhitespace(loginJson.getPhone().getText());

        /* Required. */
        String countryShortName = Formatter.getCountryShortNameFromInternationalPhone(phone);

        Map<String, String> errors;
        try {
            if (!firebaseService.isFirebaseUserExists(loginJson.getPhone().getText(), loginJson.getFirebaseUid().getText())) {
                LOG.error("Failed successful login");
                return askUserToRegister(phone);
            }

            UserProfileEntity userProfile = accountService.checkUserExistsByPhone(phone);
            if (null == userProfile) {
                LOG.info("Failed user login as no user found with phone={} cs={}", phone, countryShortName);
                return askUserToRegister(phone);
            }

            UserAccountEntity userAccount = accountService.findByQueueUserId(userProfile.getQueueUserId());
            if (!userAccount.isPhoneValidated()) {
                //TODO mark otp validated after verifying with FB server with token received
                userAccount.setPhoneValidated(true);
                accountService.save(userAccount);
            }

            /* Update authentication key after login. */
            String updatedAuthenticationKey = accountService.updateAuthenticationKey(userAccount.getUserAuthentication().getId());
            response.addHeader("X-R-MAIL", userAccount.getUserId());
            response.addHeader("X-R-AUTH", updatedAuthenticationKey);

            /* This is the only time we need to expose the headers. */
            response.addHeader("Access-Control-Expose-Headers", "X-R-MAIL, X-R-AUTH");
            LOG.info("Success login phone={} qid={}", phone, userAccount.getQueueUserId());

            DeviceTypeEnum deviceTypeEnum;
            try {
                deviceTypeEnum = DeviceTypeEnum.valueOf(deviceType.getText());
            } catch (Exception e) {
                LOG.error("Failed parsing deviceType, reason={}", e.getLocalizedMessage(), e);
                return getErrorReason("Incorrect device type.", USER_INPUT);
            }
            deviceRegistrationService.updateRegisteredDevice(userAccount.getQueueUserId(), did.getText(), deviceTypeEnum);
            return accountMobileService.getProfileAsJson(userAccount.getQueueUserId()).asJson();
        } catch (AccountNotActiveException e) {
            LOG.error("Failed getting profile phone={}, reason={}", phone, e.getLocalizedMessage());
            return getErrorReason("Please contact support related to your account", ACCOUNT_INACTIVE);
        } catch (Exception e) {
            LOG.error("Failed login for phone={} cs={} reason={}", phone, countryShortName, e.getLocalizedMessage(), e);

            errors = new HashMap<>();
            errors.put(ErrorEncounteredJson.REASON, "Something went wrong. Engineers are looking into this.");
            errors.put("PH", phone);
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR, SEVERE.name());
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, SEVERE.getCode());
            return ErrorEncounteredJson.toJson(errors);
        }
    }

    private String askUserToRegister(String phone) {
        Map<String, String> errors;
        errors = new HashMap<>();
        errors.put(ErrorEncounteredJson.REASON, "No user found. Would you like to register?");
        errors.put("PH", phone);
        errors.put(ErrorEncounteredJson.SYSTEM_ERROR, USER_NOT_FOUND.name());
        errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, USER_NOT_FOUND.getCode());
        return ErrorEncounteredJson.toJson(errors);
    }

    public static String getErrorReason(String reason, MobileSystemErrorCodeEnum mobileSystemErrorCode) {
        Map<String, String> errors = new HashMap<>();
        errors.put(ErrorEncounteredJson.REASON, reason);
        errors.put(ErrorEncounteredJson.SYSTEM_ERROR, mobileSystemErrorCode.name());
        errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, mobileSystemErrorCode.getCode());

        return ErrorEncounteredJson.toJson(errors);
    }
}
