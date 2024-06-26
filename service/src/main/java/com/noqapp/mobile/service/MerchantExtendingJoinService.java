package com.noqapp.mobile.service;

import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.USER_NOT_FOUND;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

import com.noqapp.common.errors.ErrorEncounteredJson;
import com.noqapp.common.utils.CommonUtil;
import com.noqapp.common.utils.DateUtil;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.RegisteredDeviceEntity;
import com.noqapp.domain.StoreHourEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.json.JsonBusinessCustomer;
import com.noqapp.domain.json.JsonToken;
import com.noqapp.domain.types.MessageCodeEnum;
import com.noqapp.domain.types.TokenServiceEnum;
import com.noqapp.service.AccountService;
import com.noqapp.service.BusinessCustomerService;
import com.noqapp.service.DeviceService;
import com.noqapp.service.JoinAbortService;
import com.noqapp.service.NotifyMobileService;
import com.noqapp.service.SmsService;
import com.noqapp.service.StoreHourService;
import com.noqapp.service.utils.ServiceUtils;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * hitender
 * 8/18/20 5:47 PM
 */
@Service
public class MerchantExtendingJoinService {
    private static final Logger LOG = LoggerFactory.getLogger(MerchantExtendingJoinService.class);

    private JoinAbortService joinAbortService;
    private QueueMobileService queueMobileService;
    private SmsService smsService;
    private AccountService accountService;
    private BusinessCustomerService businessCustomerService;
    private DeviceService deviceService;
    private NotifyMobileService notifyMobileService;
    private StoreHourService storeHourService;

    private ExecutorService executorService;

    @Autowired
    public MerchantExtendingJoinService(
        JoinAbortService joinAbortService,
        QueueMobileService queueMobileService,
        SmsService smsService,
        AccountService accountService,
        BusinessCustomerService businessCustomerService,
        DeviceService deviceService,
        NotifyMobileService notifyMobileService,
        StoreHourService storeHourService
    ) {
        this.joinAbortService = joinAbortService;
        this.queueMobileService = queueMobileService;
        this.smsService = smsService;
        this.accountService = accountService;
        this.businessCustomerService = businessCustomerService;
        this.deviceService = deviceService;
        this.notifyMobileService = notifyMobileService;
        this.storeHourService = storeHourService;

        /* For executing in order of sequence. */
        this.executorService = newSingleThreadExecutor();
    }

    public String dispenseTokenWithClientInfo(String did, JsonBusinessCustomer businessCustomer, BizStoreEntity bizStore) {
        if (businessCustomer.isRegisteredUser()) {
            return createTokenForRegisteredUser(did, businessCustomer, bizStore);
        } else {
            return createTokenForUnregisteredUser(did, businessCustomer, bizStore);
        }
    }

    private String createTokenForUnregisteredUser(String did, JsonBusinessCustomer businessCustomer, BizStoreEntity bizStore) {
        JsonToken jsonToken = joinAbortService.joinQueueObsolete(
            CommonUtil.appendRandomToDeviceId(did),
            bizStore,
            TokenServiceEnum.M);

        queueMobileService.updateUnregisteredUserWithNameAndPhone(
            jsonToken.getCodeQR(),
            jsonToken.getToken(),
            businessCustomer.getCustomerName().getText(),
            businessCustomer.getCustomerPhone().getText());

        StoreHourEntity storeHour = storeHourService.getStoreHours(bizStore.getCodeQR(), bizStore);
        MessageCodeEnum messageCode;
        String estimateWaitTime;
        switch (bizStore.getBusinessType()) {
            case CDQ:
            case CD:
                messageCode = MessageCodeEnum.SMTS;
                estimateWaitTime = ServiceUtils.timeSlot(jsonToken.getExpectedServiceBeginDate(), ZoneId.of(bizStore.getTimeZone()), storeHour);
                break;
            default:
                messageCode = MessageCodeEnum.SMEW;
                estimateWaitTime = ServiceUtils.calculateEstimatedWaitTime(
                    bizStore.getAverageServiceTime(),
                    jsonToken.getToken() - jsonToken.getServingNumber(),
                    jsonToken.getQueueStatus(),
                    storeHour.getStartHour(),
                    bizStore.getTimeZone()
                );
        }

        executorService.submit(() -> {
            String smsMessage = smsService.smsMessage(
                messageCode,
                bizStore.getBizName().getSmsLocale(),
                bizStore.getDisplayName(),
                jsonToken.getExpectedServiceBeginDate().withZoneSameInstant(ZoneId.of(bizStore.getTimeZone())).format(DateUtil.DTF_DD_MMM_YYYY_HH_MM),
                jsonToken.getDisplayToken(),
                (jsonToken.getToken() - jsonToken.getServingNumber()),
                estimateWaitTime);

            LOG.info("SMS=\"{}\" length={}", smsMessage, smsMessage.length());
            smsService.sendTransactionalSMS(businessCustomer.getCustomerPhone().getText(), smsMessage, bizStore.getBizName().getSmsLocale());
        });
        return jsonToken.asJson();
    }

    private String createTokenForRegisteredUser(String did, JsonBusinessCustomer businessCustomer, BizStoreEntity bizStore) {
        joinAbortService.checkCustomerApprovedForTheQueue(businessCustomer.getQueueUserId(), bizStore);

        UserProfileEntity userProfile = null;
        if (StringUtils.isNotBlank(businessCustomer.getCustomerPhone().getText())) {
            LOG.info("Look up customer by phone {}", businessCustomer.getCustomerPhone());
            userProfile = accountService.checkUserExistsByPhone(businessCustomer.getCustomerPhone().getText());
            if (!userProfile.getQueueUserId().equalsIgnoreCase(businessCustomer.getQueueUserId())) {
                if (userProfile.getQidOfDependents().contains(businessCustomer.getQueueUserId())) {
                    userProfile = accountService.findProfileByQueueUserId(businessCustomer.getQueueUserId());
                } else {
                    userProfile = null;
                }
            }
        } else if (StringUtils.isNotBlank(businessCustomer.getBusinessCustomerId().getText())) {
            userProfile = businessCustomerService.findByBusinessCustomerIdAndBizNameId(businessCustomer.getBusinessCustomerId().getText(), bizStore.getBizName().getId());
        }

        if (null == userProfile) {
            LOG.info("Failed joining queue as no user found with phone={} businessCustomerId={}",
                businessCustomer.getCustomerPhone(),
                businessCustomer.getBusinessCustomerId());

            Map<String, String> errors = new HashMap<>();
            errors.put(ErrorEncounteredJson.REASON, "No user found. Would you like to register?");
            errors.put(AccountMobileService.ACCOUNT_REGISTRATION.PH.name(), businessCustomer.getCustomerPhone().getText());
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR, USER_NOT_FOUND.name());
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, USER_NOT_FOUND.getCode());
            return ErrorEncounteredJson.toJson(errors);
        }

        String guardianQid = null;
        RegisteredDeviceEntity registeredDevice;
        if (StringUtils.isNotBlank(userProfile.getGuardianPhone())) {
            guardianQid = accountService.checkUserExistsByPhone(userProfile.getGuardianPhone()).getQueueUserId();
            registeredDevice = deviceService.findRecentDevice(guardianQid);
        } else {
            registeredDevice = deviceService.findRecentDevice(userProfile.getQueueUserId());
        }

        JsonToken jsonToken;
        if (bizStore.isEnabledPayment()) {
            jsonToken = joinAbortService.skipPayBeforeJoinQueue(
                businessCustomer.getCodeQR().getText(),
                DeviceService.getExistingDeviceId(registeredDevice, did),
                userProfile.getQueueUserId(),
                guardianQid,
                bizStore,
                TokenServiceEnum.M);
        } else {
            jsonToken = joinAbortService.joinQueue(
                DeviceService.getExistingDeviceId(registeredDevice, did),
                userProfile.getQueueUserId(),
                guardianQid,
                bizStore,
                TokenServiceEnum.M);
        }

        if (null != registeredDevice) {
            notifyMobileService.autoSubscribeClientToTopic(
                businessCustomer.getCodeQR().getText(),
                registeredDevice.getToken(),
                registeredDevice.getDeviceType());

            notifyMobileService.notifyClient(
                registeredDevice,
                "Joined " + bizStore.getDisplayName() + " Queue",
                "Your token number is " + jsonToken.getDisplayToken(),
                bizStore.getCodeQR());
        }

        return jsonToken.asJson();
    }
}
