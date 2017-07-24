package com.noqapp.mobile.service;

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
import com.noqapp.mobile.domain.SignupUserInfo;
import com.noqapp.service.AccountService;

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

    @Autowired
    public AccountMobileService(
            @Value ("${accountSignupEndPoint:/webapi/mobile/mail/accountSignup.htm}")
            String accountSignupEndPoint,

            WebConnectorService webConnectorService,
            AccountService accountService
    ) {
        this.accountValidationEndPoint = accountSignupEndPoint;

        this.webConnectorService = webConnectorService;
        this.accountService = accountService;
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
    @SuppressWarnings("all")
    public String createNewMerchantAccount(
            String phone,
            String firstName,
            String lastName,
            String mail,
            String birthday,
            String gender,
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
                    false);
            Assert.notNull(userAccount, "Account creation cannot be null");
            LOG.info("Registered new user Id={}", userAccount.getReceiptUserId());
        } catch (RuntimeException exce) {
            LOG.error("failed creating new account for user={} reason={}", mail, exce.getLocalizedMessage(), exce);
            throw new RuntimeException("failed creating new account for user " + mail, exce);
        }

        sendValidationEmail(userAccount);
        return userAccount.getUserAuthentication().getAuthenticationKey();
    }

    /**
     *
     * @param phone
     * @param firstName
     * @param lastName
     * @param mail
     * @param birthday
     * @param gender
     * @param countryShortName
     * @param timeZone
     * @return
     */
    @SuppressWarnings("all")
    public UserAccountEntity createNewClientAccount(
            String phone,
            String firstName,
            String lastName,
            String mail,
            String birthday,
            String gender,
            String countryShortName,
            String timeZone,
            String inviteCode
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
                    null,
                    inviteCode,
                    true);
            Assert.notNull(userAccount, "Account creation cannot be null");
            LOG.info("Registered new user Id={}", userAccount.getReceiptUserId());
        } catch (RuntimeException exce) {
            LOG.error("failed creating new account for user={} reason={}", mail, exce.getLocalizedMessage(), exce);
            throw new RuntimeException("failed creating new account for user " + mail, exce);
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
        /** No RID hence using method without RID. */
        UserAccountEntity userAccount = accountService.updateUID(existingUserId, newUserId);
        sendValidationEmail(userAccount);
        return userAccount;
    }

    private void sendValidationEmail(UserAccountEntity userAccount) {
        boolean mailStatus = sendMailDuringSignup(
                userAccount.getUserId(),
                userAccount.getReceiptUserId(),
                userAccount.getName(),
                HttpClientBuilder.create().build());

        LOG.info("mail sent={} to user={}", mailStatus, userAccount.getUserId());
    }

    /**
     * Call this on terminal as below.
     * http localhost:9090/receipt-mobile/authenticate.json < ~/Downloads/pid.json
     *
     * @param userId
     * @param rid
     * @param name
     * @param httpClient
     * @return
     */
    private boolean sendMailDuringSignup(String userId, String rid, String name, HttpClient httpClient) {
        LOG.debug("userId={} name={} webApiAccessToken={}", userId, name, "*******");
        HttpPost httpPost = webConnectorService.getHttpPost(accountValidationEndPoint, httpClient);
        if (null == httpPost) {
            LOG.warn("failed connecting, reason={}", webConnectorService.getNoResponseFromWebServer());
            return false;
        }

        setEntity(SignupUserInfo.newInstance(userId, rid, name), httpPost);
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

    public UserAccountEntity findByRid(String rid) {
        return accountService.findByReceiptUserId(rid);
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
}
