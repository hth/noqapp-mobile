package com.noqapp.mobile.service;

import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;

import com.google.gson.Gson;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.noqapp.domain.UserAccountEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.flow.RegisterUser;
import com.noqapp.domain.types.GenderEnum;
import com.noqapp.medical.service.UserMedicalProfileService;
import com.noqapp.mobile.domain.JsonProfile;
import com.noqapp.mobile.domain.SignupUserInfo;
import com.noqapp.service.AccountService;
import com.noqapp.service.exceptions.DuplicateAccountException;

import java.io.IOException;

/**
 * User: hitender
 * Date: 1/14/17 10:57 AM
 */
@SuppressWarnings ({
        "PMD.BeanMembersShouldSerialize",
        "PMD.LocalVariableCouldBeFinal",
        "PMD.MethodArgumentCouldBeFinal",
        "PMD.LongVariable"
})
@Component
public class AccountMobileService {
    private static final Logger LOG = LoggerFactory.getLogger(AccountMobileService.class);

    private String accountValidationEndPoint;

    private WebConnectorService webConnectorService;
    private AccountService accountService;
    private UserMedicalProfileService userMedicalProfileService;

    @Autowired
    public AccountMobileService(
            @Value ("${accountSignupEndPoint:/webapi/mobile/mail/accountSignup.htm}")
            String accountSignupEndPoint,

            WebConnectorService webConnectorService,
            AccountService accountService,
            UserMedicalProfileService userMedicalProfileService
    ) {
        this.accountValidationEndPoint = accountSignupEndPoint;

        this.webConnectorService = webConnectorService;
        this.accountService = accountService;
        this.userMedicalProfileService = userMedicalProfileService;
    }

    /**
     * Signup user and return authenticated key.
     *
     * @param mail
     * @param firstName
     * @param lastName
     * @param password
     * @param birthday
     * @return
     */
    @SuppressWarnings ("all")
    public String createNewMerchantAccount(
            String phone,
            String firstName,
            String lastName,
            String mail,
            String birthday,
            GenderEnum gender,
            String countryShortName,
            String timeZone,
            String password
    ) {
        UserAccountEntity userAccount;
        try {
            userAccount = accountService.createNewAccount(
                    phone,
                    firstName,
                    lastName,
                    mail,
                    birthday,
                    gender,
                    countryShortName,
                    timeZone,
                    password,
                    null,
                    false,
                    true);
            Assert.notNull(userAccount, "Account creation cannot be null");
            LOG.info("Registered new user Id={}", userAccount.getQueueUserId());
        } catch (DuplicateAccountException e) {
            LOG.error("Duplicate Account found reason={}", e.getLocalizedMessage(), e);
            throw e;
        } catch (RuntimeException exce) {
            LOG.error("Failed creating new account for user={} reason={}", mail, exce.getLocalizedMessage(), exce);
            throw new RuntimeException("Failed creating new account for user " + mail, exce);
        }

        sendValidationEmail(userAccount);
        return userAccount.getUserAuthentication().getAuthenticationKey();
    }

    public UserAccountEntity createNewClientAccount(
            String phone,
            String firstName,
            String lastName,
            String mail,
            String birthday,
            GenderEnum gender,
            String countryShortName,
            String timeZone,
            String password,
            String inviteCode,
            boolean dependent
    ) {
        UserAccountEntity userAccount;
        try {
            //TODO before marking client account as validated, make sure you compare FB token with server
            userAccount = accountService.createNewAccount(
                    phone,
                    firstName,
                    lastName,
                    mail,
                    birthday,
                    gender,
                    countryShortName,
                    timeZone,
                    password,
                    inviteCode,
                    true,
                    dependent);
            Assert.notNull(userAccount, "Account creation cannot be null");
            LOG.info("Registered new user Id={}", userAccount.getQueueUserId());
        } catch (DuplicateAccountException e) {
            LOG.error("Duplicate Account found reason={}", e.getLocalizedMessage(), e);
            throw e;
        } catch (RuntimeException exce) {
            LOG.error("Failed creating new account for user={} reason={}", mail, exce.getLocalizedMessage(), exce);
            throw new RuntimeException("Failed creating new account for user " + mail, exce);
        }

        sendValidationEmail(userAccount);
        return userAccount;
    }

    /**
     * Updates existing userId with new userId and also sends out email to validate new userId.
     *
     * @param existingUserId
     * @param newUserId
     * @return
     */
    public UserAccountEntity changeUID(String existingUserId, String newUserId) {
        /* No QID hence using method without QID. */
        UserAccountEntity userAccount = accountService.updateUID(existingUserId, newUserId);
        sendValidationEmail(userAccount);
        return userAccount;
    }

    private void sendValidationEmail(UserAccountEntity userAccount) {
        boolean mailStatus = sendMailDuringSignup(
                userAccount.getUserId(),
                userAccount.getQueueUserId(),
                userAccount.getName(),
                HttpClientBuilder.create().build());

        LOG.info("mail sent={} to user={}", mailStatus, userAccount.getUserId());
    }

    /**
     * Call this on terminal as below.
     * http localhost:9090/receipt-mobile/authenticate.json < ~/Downloads/pid.json
     *
     * @param userId
     * @param qid
     * @param name
     * @param httpClient
     * @return
     */
    private boolean sendMailDuringSignup(String userId, String qid, String name, HttpClient httpClient) {
        LOG.debug("userId={} name={} webApiAccessToken={}", userId, name, AUTH_KEY_HIDDEN);
        HttpPost httpPost = webConnectorService.getHttpPost(accountValidationEndPoint, httpClient);
        if (null == httpPost) {
            LOG.warn("failed connecting, reason={}", webConnectorService.getNoResponseFromWebServer());
            return false;
        }

        setEntity(SignupUserInfo.newInstance(userId, qid, name), httpPost);
        return invokeHttpPost(httpClient, httpPost);
    }

    private boolean invokeHttpPost(HttpClient httpClient, HttpPost httpPost) {
        HttpResponse response = null;
        try {
            response = httpClient.execute(httpPost);
        } catch (IOException e) {
            LOG.error("error occurred while executing request path={} reason={}",
                    httpPost.getURI(), e.getLocalizedMessage(), e);
        }

        if (null == response) {
            LOG.warn("failed response, reason={}", webConnectorService.getNoResponseFromWebServer());
            return false;
        }

        int status = response.getStatusLine().getStatusCode();
        LOG.debug("status={}", status);
        if (WebConnectorService.HTTP_STATUS_200 <= status && WebConnectorService.HTTP_STATUS_300 > status) {
            return true;
        }

        LOG.error("server responded with response code={}", status);
        return false;
    }

    public UserAccountEntity findByQueueUserId(String qid) {
        return accountService.findByQueueUserId(qid);
    }

    public String getProfileAsJson(String qid) {
        UserProfileEntity userProfile = findProfileByQueueUserId(qid);
        JsonProfile jsonProfile = JsonProfile.newInstance(userProfile, 0);
        jsonProfile.setJsonUserMedicalProfile(userMedicalProfileService.findOneAsJson(qid));

        if (null != userProfile.getQidOfDependents()) {
            for (String qidOfDependent : userProfile.getQidOfDependents()) {
                jsonProfile.addDependents(JsonProfile.newInstance(findProfileByQueueUserId(qidOfDependent), 0));
            }
        }

        return jsonProfile.asJson();
    }

    public UserProfileEntity findProfileByQueueUserId(String qid) {
        return accountService.findProfileByQueueUserId(qid);
    }

    /**
     * Create Request Body.
     *
     * @param object
     * @param httpPost
     */
    private void setEntity(Object object, HttpPost httpPost) {
        httpPost.setEntity(
                new StringEntity(
                        new Gson().toJson(object),
                        ContentType.create(MediaType.APPLICATION_JSON_VALUE, "UTF-8")
                )
        );
    }

    public String updatePhoneNumber(String qid, String phone, String countryShortName, String timeZone) {
        return accountService.updatePhoneNumber(qid, phone, countryShortName, timeZone);
    }

    public UserProfileEntity checkUserExistsByPhone(String phone) {
        return accountService.checkUserExistsByPhone(phone);
    }

    public void updateUserProfile(RegisterUser registerUser, String email) {
        accountService.updateUserProfile(registerUser, email);
    }

    public enum ACCOUNT_REGISTRATION_MERCHANT {
        PW  //Password
    }

    public enum ACCOUNT_REGISTRATION_CLIENT {
        IC  //Invite Code
    }

    public enum ACCOUNT_REGISTRATION {
        PH, //Phone             //TODO add this to token merchant registration
        FN, //FirstName
        EM, //Email
        BD, //Birthday
        GE, //Gender            //TODO add this to token merchant registration
        CS, //CountryShortName  //TODO add this to token merchant registration
        TZ, //TimeZone          //TODO add this to token merchant registration
    }

    public enum ACCOUNT_MIGRATE {
        PH, //Phone
        CS, //CountryShortName
        TZ, //TimeZone
    }

    public enum ACCOUNT_UPDATE {
        QID,//QueueUserId
        AD, //Address
        FN, //FirstName
        BD, //Birthday
        GE, //Gender            //TODO add this to token merchant registration
        TZ, //TimeZone          //TODO add this to token merchant registration
    }

    //TODO(hth) include height
    public enum MEDICAL_PROFILE {
        BT  //BloodType
    }
}
