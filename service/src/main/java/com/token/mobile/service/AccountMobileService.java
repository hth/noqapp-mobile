package com.token.mobile.service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.token.domain.EmailValidateEntity;
import com.token.domain.UserAccountEntity;
import com.token.mobile.domain.AccountRecover;
import com.token.mobile.domain.SignupUserInfo;
import com.token.service.AccountService;
import com.token.service.EmailValidateService;

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
    private String accountRecoverEndPoint;
    private String registrationAcceptingEndPoint;
    private String inviteUserEndPoint;

    private WebConnectorService webConnectorService;
    private EmailValidateService emailValidateService;
    private AccountService accountService;

    @Autowired
    public AccountMobileService(
            @Value ("${accountSignupEndPoint:/webapi/mobile/mail/accountSignup.htm}")
            String accountSignupEndPoint,

            @Value ("${accountRecover:/webapi/mobile/mail/accountRecover.htm}")
            String accountRecoverEndPoint,

            @Value ("${registrationAccepting:/webapi/mobile/registration/accepting.htm}")
            String registrationAcceptingEndPoint,

            @Value ("${inviteUser:/webapi/mobile/mail/invite.htm}")
            String inviteUserEndPoint,

            WebConnectorService webConnectorService,
            EmailValidateService emailValidateService,
            AccountService accountService
    ) {
        this.accountValidationEndPoint = accountSignupEndPoint;
        this.accountRecoverEndPoint = accountRecoverEndPoint;
        this.registrationAcceptingEndPoint = registrationAcceptingEndPoint;
        this.inviteUserEndPoint = inviteUserEndPoint;

        this.webConnectorService = webConnectorService;
        this.emailValidateService = emailValidateService;
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
    public String signup(String mail, String firstName, String lastName, String password, String birthday) {
        UserAccountEntity userAccount;
        try {
            userAccount = accountService.createNewAccount(mail, firstName, lastName, password, birthday);
            Assert.notNull(userAccount);
            LOG.info("Registered new user Id={}", userAccount.getReceiptUserId());
        } catch (RuntimeException exce) {
            LOG.error("failed creating new account for user={} reason={}", mail, exce.getLocalizedMessage(), exce);
            throw new RuntimeException("failed creating new account for user " + mail, exce);
        }

        sendValidationEmail(userAccount);
        return userAccount.getUserAuthentication().getAuthenticationKey();
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

    public void sendValidationEmail(UserAccountEntity userAccount) {
        EmailValidateEntity accountValidate = emailValidateService.saveAccountValidate(
                userAccount.getReceiptUserId(),
                userAccount.getUserId());
        Assert.notNull(accountValidate);

        //TODO(hth) mail sending can be done on background. Just store this as a task.
        boolean mailStatus = sendMailDuringSignup(
                userAccount.getUserId(),
                userAccount.getName(),
                accountValidate.getAuthenticationKey(),
                HttpClientBuilder.create().build());

        LOG.info("mail sent={} to user={}", mailStatus, userAccount.getUserId());
    }

    /**
     * Call this on terminal as below.
     * http localhost:9090/receipt-mobile/authenticate.json < ~/Downloads/pid.json
     *
     * @param userId
     * @param name
     * @param auth
     * @return
     */
    protected boolean sendMailDuringSignup(String userId, String name, String auth, HttpClient httpClient) {
        LOG.debug("userId={} name={} webApiAccessToken={}", userId, name, "*******");
        HttpPost httpPost = webConnectorService.getHttpPost(accountValidationEndPoint, httpClient);
        if (null == httpPost) {
            LOG.warn("failed connecting, reason={}", webConnectorService.getNoResponseFromWebServer());
            return false;
        }

        setEntity(SignupUserInfo.newInstance(userId, name, auth), httpPost);
        return invokeHttpPost(httpClient, httpPost);
    }

    /**
     * @param userId
     * @return
     */
    public boolean recoverAccount(String userId) {
        LOG.debug("userId={} webApiAccessToken={}", userId, "*******");
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost httpPost = webConnectorService.getHttpPost(accountRecoverEndPoint, httpClient);
        if (null == httpPost) {
            LOG.warn("failed connecting, reason={}", webConnectorService.getNoResponseFromWebServer());
            return false;
        }

        setEntity(AccountRecover.newInstance(userId), httpPost);
        return invokeHttpPost(httpClient, httpPost);
    }

    /**
     * Query server to check if sign-ups are being accepted.
     *
     * @return
     */
    public boolean acceptingSignup() {
        LOG.debug("userId={} webApiAccessToken={}", "*******");
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpGet httpGet = webConnectorService.getHttpGet(registrationAcceptingEndPoint, httpClient);

        if (null == httpGet) {
            LOG.warn("failed connecting, reason={}", webConnectorService.getNoResponseFromWebServer());
            return false;
        }

        HttpResponse response = null;
        try {
            response = httpClient.execute(httpGet);
        } catch (IOException e) {
            LOG.error("error occurred while executing request path={} reason={}",
                    httpGet.getURI(), e.getLocalizedMessage(), e);
        }

        if (null == response) {
            LOG.warn("failed response, reason={}", webConnectorService.getNoResponseFromWebServer());
            return false;
        }

        int status = response.getStatusLine().getStatusCode();
        LOG.debug("status={}", status);
        if (WebConnectorService.HTTP_STATUS_200 <= status && WebConnectorService.HTTP_STATUS_300 > status) {
            try {
                String data = EntityUtils.toString(response.getEntity());
                LOG.debug("data={}", data);
                JsonElement element = new JsonParser().parse(data);
                JsonObject object = element.getAsJsonObject();
                return object.get(REGISTRATION_TURNED_ON.RTO.name()).getAsBoolean();
            } catch (IOException e) {
                LOG.error("failed parsing data={} reason={}", response.getEntity(), e.getLocalizedMessage(), e);
                return false;
            }
        }

        LOG.error("server responded with response code={}", status);
        return false;
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

    public enum REGISTRATION {
        FN, //FirstName
        EM, //Email
        BD, //Birthday
        PW, //Password
        CS  //CountryShortName
    }

    public enum REGISTRATION_TURNED_ON {
        RTO
    }
}
