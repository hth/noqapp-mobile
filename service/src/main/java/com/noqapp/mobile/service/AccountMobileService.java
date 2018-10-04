package com.noqapp.mobile.service;

import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;

import com.noqapp.common.utils.AbstractDomain;
import com.noqapp.common.utils.RandomString;
import com.noqapp.domain.ProfessionalProfileEntity;
import com.noqapp.domain.UserAccountEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.flow.RegisterUser;
import com.noqapp.domain.helper.NameDatePair;
import com.noqapp.domain.json.JsonNameDatePair;
import com.noqapp.domain.json.JsonProfessionalProfile;
import com.noqapp.domain.types.GenderEnum;
import com.noqapp.medical.service.UserMedicalProfileService;
import com.noqapp.mobile.domain.ChangeMailOTP;
import com.noqapp.mobile.domain.JsonProfile;
import com.noqapp.mobile.domain.SignupUserInfo;
import com.noqapp.service.AccountService;
import com.noqapp.service.ProfessionalProfileService;
import com.noqapp.service.exceptions.DuplicateAccountException;

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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * User: hitender
 * Date: 1/14/17 10:57 AM
 */
@SuppressWarnings({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@Component
public class AccountMobileService {
    private static final Logger LOG = LoggerFactory.getLogger(AccountMobileService.class);

    private String accountSignup;
    private String mailChange;

    private WebConnectorService webConnectorService;
    private AccountService accountService;
    private UserMedicalProfileService userMedicalProfileService;
    private ProfessionalProfileService professionalProfileService;

    @Autowired
    public AccountMobileService(
        @Value("${accountSignup:/webapi/mobile/mail/accountSignup.htm}")
        String accountSignup,

        @Value("${mailChange:/webapi/mobile/mail/mailChange.htm}")
        String mailChange,

        WebConnectorService webConnectorService,
        AccountService accountService,
        UserMedicalProfileService userMedicalProfileService,
        ProfessionalProfileService professionalProfileService
    ) {
        this.accountSignup = accountSignup;
        this.mailChange = mailChange;

        this.webConnectorService = webConnectorService;
        this.accountService = accountService;
        this.userMedicalProfileService = userMedicalProfileService;
        this.professionalProfileService = professionalProfileService;
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
    @Deprecated //has never been used. Can be deleted at some time later
    public UserAccountEntity changeUID(String existingUserId, String newUserId) {
        /* No QID hence using method without QID. */
        UserAccountEntity userAccount = accountService.updateUID(existingUserId, newUserId);
        sendValidationEmail(userAccount);
        return userAccount;
    }

    public UserAccountEntity changeUIDWithMailOTP(String existingUserId, String newUserId) {
        /* No QID hence using method without QID. */
        UserAccountEntity userAccount = accountService.updateUID(existingUserId, newUserId);
        userAccount.setAccountValidated(true);
        accountService.save(userAccount);
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
        HttpPost httpPost = webConnectorService.getHttpPost(accountSignup, httpClient);
        if (null == httpPost) {
            LOG.warn("failed connecting, reason={}", webConnectorService.getNoResponseFromWebServer());
            return false;
        }

        webConnectorService.setEntityWithGson(SignupUserInfo.newInstance(userId, qid, name), httpPost);
        return webConnectorService.invokeHttpPost(httpClient, httpPost);
    }

    public UserAccountEntity findByQueueUserId(String qid) {
        return accountService.findByQueueUserId(qid);
    }

    public String getProfileAsJson(String qid) {
        UserProfileEntity userProfile = findProfileByQueueUserId(qid);
        UserAccountEntity userAccount = findByQueueUserId(qid);
        JsonProfile jsonProfile = JsonProfile.newInstance(userProfile, userAccount);
        jsonProfile.setJsonUserMedicalProfile(userMedicalProfileService.findOneAsJson(qid));

        if (null != userProfile.getQidOfDependents()) {
            for (String qidOfDependent : userProfile.getQidOfDependents()) {
                jsonProfile.addDependents(
                    JsonProfile.newInstance(
                        findProfileByQueueUserId(qidOfDependent),
                        findByQueueUserId(qidOfDependent)));
            }
        }

        return jsonProfile.asJson();
    }

    public UserProfileEntity findProfileByQueueUserId(String qid) {
        return accountService.findProfileByQueueUserId(qid);
    }

    public String updatePhoneNumber(String qid, String phone, String countryShortName, String timeZone) {
        return accountService.updatePhoneNumber(qid, phone, countryShortName, timeZone);
    }

    public UserProfileEntity checkUserExistsByPhone(String phone) {
        return accountService.checkUserExistsByPhone(phone);
    }

    public UserProfileEntity doesUserExists(String mail) {
        return accountService.doesUserExists(mail);
    }

    @Async
    public void initiateChangeMailOTP(String qid, String migrateToMail) {
        UserProfileEntity userProfile = findProfileByQueueUserId(qid);
        String mailOTP = RandomString.newInstance(6).nextString().toUpperCase();
        userProfile.setMailOTP(mailOTP);
        accountService.save(userProfile);

        populateChangeMailWithData(userProfile, migrateToMail);
    }

    private void populateChangeMailWithData(UserProfileEntity userProfile, String migrateToMail) {
        boolean mailStatus = sendMailWhenChangingMail(
            migrateToMail,
            userProfile.getName(),
            userProfile.getMailOTP(),
            HttpClientBuilder.create().build());

        LOG.info("Change mail confirmation sent={} to qid={} at {}", mailStatus, userProfile.getQueueUserId(), migrateToMail);
    }

    private boolean sendMailWhenChangingMail(String migrateToMail, String name, String mailOTP, HttpClient httpClient) {
        LOG.debug("migrateToMail={} name={} webApiAccessToken={}", migrateToMail, name, AUTH_KEY_HIDDEN);
        HttpPost httpPost = webConnectorService.getHttpPost(mailChange, httpClient);
        if (null == httpPost) {
            LOG.warn("failed connecting, reason={}", webConnectorService.getNoResponseFromWebServer());
            return false;
        }

        webConnectorService.setEntityWithGson(ChangeMailOTP.newInstance(migrateToMail, name, mailOTP), httpPost);
        return webConnectorService.invokeHttpPost(httpClient, httpPost);
    }

    public void updateUserProfile(RegisterUser registerUser, String email) {
        accountService.updateUserProfile(registerUser, email);
    }

    public String updateProfessionalProfile(String qidOfSubmitter, JsonProfessionalProfile jsonProfessionalProfile) {
        ProfessionalProfileEntity professionalProfile = professionalProfileService.findByQid(qidOfSubmitter);

        List<NameDatePair> nameDatePairsAwards = new ArrayList<>();
        for (JsonNameDatePair jsonNameDatePair : jsonProfessionalProfile.getAwards()) {
            NameDatePair nameDatePair = new NameDatePair()
                .setName(jsonNameDatePair.getName())
                .setMonthYear(jsonNameDatePair.getMonthYear());
            nameDatePairsAwards.add(nameDatePair);
        }

        List<NameDatePair> nameDatePairsEducations = new ArrayList<>();
        for (JsonNameDatePair jsonNameDatePair : jsonProfessionalProfile.getEducation()) {
            NameDatePair nameDatePair = new NameDatePair()
                .setName(jsonNameDatePair.getName())
                .setMonthYear(jsonNameDatePair.getMonthYear());
            nameDatePairsEducations.add(nameDatePair);
        }

        List<NameDatePair> nameDatePairsLicenses = new ArrayList<>();
        for (JsonNameDatePair jsonNameDatePair : jsonProfessionalProfile.getLicenses()) {
            NameDatePair nameDatePair = new NameDatePair()
                .setName(jsonNameDatePair.getName())
                .setMonthYear(jsonNameDatePair.getMonthYear());
            nameDatePairsLicenses.add(nameDatePair);
        }

        professionalProfile
            .setPracticeStart(jsonProfessionalProfile.getPracticeStart())
            .setAboutMe(jsonProfessionalProfile.getAboutMe())
            .setDataDictionary(jsonProfessionalProfile.getDataDictionary())
            .setAwards(nameDatePairsAwards)
            .setEducation(nameDatePairsEducations)
            .setLicenses(nameDatePairsLicenses);
        professionalProfileService.save(professionalProfile);
        return jsonProfessionalProfile.asJson();
    }

    @Async
    public void unsetMailOTP(String id) {
        accountService.unsetMailOTP(id);
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

    public enum ACCOUNT_MAIL_MIGRATE {
        EM, //Email
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
