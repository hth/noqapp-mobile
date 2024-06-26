package com.noqapp.mobile.service;

import static java.util.concurrent.Executors.newCachedThreadPool;

import com.noqapp.common.utils.CommonUtil;
import com.noqapp.domain.RegisteredDeviceEntity;
import com.noqapp.domain.UserPreferenceEntity;
import com.noqapp.domain.types.AppFlavorEnum;
import com.noqapp.domain.types.DeviceTypeEnum;
import com.noqapp.mobile.service.exception.DeviceDetailMissingException;
import com.noqapp.repository.RegisteredDeviceManager;
import com.noqapp.search.elastic.helper.GeoIP;
import com.noqapp.search.elastic.service.GeoIPLocationService;
import com.noqapp.service.FirebaseService;
import com.noqapp.service.UserProfilePreferenceService;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

/**
 * User: hitender
 * Date: 3/1/17 12:40 PM
 */
@SuppressWarnings({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@Service
public class DeviceRegistrationService {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceRegistrationService.class);

    private String information;
    private RegisteredDeviceManager registeredDeviceManager;
    private FirebaseService firebaseService;
    private UserProfilePreferenceService userProfilePreferenceService;
    private GeoIPLocationService geoIPLocationService;

    private ExecutorService executorService;

    @Autowired
    public DeviceRegistrationService(
        @Value("${subscribe.information}")
        String information,

        RegisteredDeviceManager registeredDeviceManager,
        FirebaseService firebaseService,
        UserProfilePreferenceService userProfilePreferenceService,
        GeoIPLocationService geoIPLocationService
    ) {
        this.information = information;

        this.registeredDeviceManager = registeredDeviceManager;
        this.firebaseService = firebaseService;
        this.userProfilePreferenceService = userProfilePreferenceService;
        this.geoIPLocationService = geoIPLocationService;

        this.executorService = newCachedThreadPool();
    }

    public void registerDevice(
        String qid,
        String did,
        DeviceTypeEnum deviceType,
        AppFlavorEnum appFlavor,
        String token,
        String model,
        String osVersion,
        String appVersion,
        String deviceLanguage,
        double[] coordinate,
        String ipAddress
    ) {
        try {
            Assert.hasLength(did, "DID cannot be blank");
            Assert.hasLength(token, "FCM Token cannot be blank");
            executorService.submit(() -> registeringDevice(qid, did, deviceType, appFlavor, token, model, osVersion, appVersion, deviceLanguage, coordinate, ipAddress));
        } catch (Exception e) {
            LOG.error("Failed registration as cannot find qid={} did={} token={} reason={}", qid, did, token, e.getLocalizedMessage(), e);
            throw new DeviceDetailMissingException("Something went wrong. Please restart the app.");
        }
    }

    /**
     * Checks if the device is registered, if not registered then it registers the device.
     *
     * @param qid
     * @param did
     * @param deviceType iPhone or Android
     * @return
     */
    private void registeringDevice(
        String qid,
        String did,
        DeviceTypeEnum deviceType,
        AppFlavorEnum appFlavor,
        String token,
        String model,
        String osVersion,
        String appVersion,
        String deviceLanguage,
        double[] coordinate,
        String ipAddress
    ) {
        try {
            RegisteredDeviceEntity registeredDevice = registeredDeviceManager.find(qid, did);
            GeoIP geoIP = geoIPLocationService.getLocation(ipAddress);
            if (null == registeredDevice) {
                LOG.info("Registering new deviceType={} appFlavor={} did={} qid={} ip={}", deviceType.getName(), appFlavor.getName(), did, qid, ipAddress);
                registeredDevice = RegisteredDeviceEntity.newInstance(qid, did, deviceType, appFlavor, token, appVersion, deviceLanguage, geoIP.getCityName(), coordinate, ipAddress);
                try {
                    registeredDevice
                        .setModel(model)
                        .setOsVersion(osVersion);
                    registeredDeviceManager.save(registeredDevice);

                    /* Always subscribe to information. */
                    firebaseService.subscribeToTopic(
                        new ArrayList<>() {
                            private static final long serialVersionUID = -4512369320581819200L;

                            {
                                add(token);
                            }
                        },
                        CommonUtil.buildTopic(information, deviceType.name()));

                    LOG.info("Registered device for did={}", did);
                } catch (DuplicateKeyException duplicateKeyException) {
                    LOG.warn("Its registered device, update existing with new details deviceType={} did={} qid={}", deviceType, did, qid);

                    /* Reset update date with create date to fetch all the possible historical data. */
                    boolean updateStatus = registeredDeviceManager.resetRegisteredDeviceWithNewDetails(
                        registeredDevice.getDeviceId(),
                        qid,
                        deviceType,
                        appFlavor,
                        token,
                        model,
                        osVersion,
                        deviceLanguage,
                        geoIP.getCityName(),
                        coordinate,
                        ipAddress);
                    LOG.info("existing registered device updateStatus={} with qid={} token={}", updateStatus, qid, token);
                }
            } else if (StringUtils.isNotBlank(token)) {
                LOG.info("Updating registered device of deviceType={} appFlavor={} did={} qid={} ip={}", deviceType, appFlavor.getName(), did, qid, ipAddress);
                boolean updateSuccess = registeredDeviceManager.updateDevice(
                    registeredDevice.getId(),
                    registeredDevice.getDeviceId(),
                    qid,
                    deviceType,
                    appFlavor,
                    token,
                    model,
                    osVersion,
                    deviceLanguage,
                    geoIP.getCityName(),
                    coordinate,
                    ipAddress,
                    true);
                LOG.info("updated registered device for did={} token={} updateSuccess={}", did, token, updateSuccess);
            }
        } catch (Exception e) {
            LOG.error("Failed device registration deviceType={} did={} qid={} reason={}", deviceType, did, qid, e.getLocalizedMessage(), e);
        }
    }

    public boolean isDeviceRegistered(String qid, String did) {
        return registeredDeviceManager.find(qid, did) != null;
    }

    public RegisteredDeviceEntity lastAccessed(String qid, String did, String token, String model, String osVersion, String appVersion, String deviceLanguage, String ipAddress) {
        GeoIP geoIP = geoIPLocationService.getLocation(ipAddress);
        return registeredDeviceManager.lastAccessed(qid, did, token, model, osVersion, appVersion, ipAddress, deviceLanguage, geoIP.getCityName());
    }

    /** Update Registered Device after register or login when token is not available. */
    public void updateRegisteredDevice(String qid, String did, DeviceTypeEnum deviceType) {
        RegisteredDeviceEntity registeredDevice = registeredDeviceManager.find(null, did);

        if (null == registeredDevice) {
            LOG.warn("Failed finding Registered device to update with qid={} did={} deviceType={}", qid, did, deviceType);
            return;
        }

        if (null == registeredDevice.getQueueUserId()) {
            executorService.submit(() -> subscribeToAllAssociatedTopics(qid, deviceType, registeredDevice.getToken()));
        }

        registeredDeviceManager.updateRegisteredDevice(registeredDevice.getDeviceId(), qid, deviceType, true);
        LOG.info("Updated did={} with qid={} deviceType={}", did, qid, deviceType);
    }

    private void subscribeToAllAssociatedTopics(String qid, DeviceTypeEnum deviceType, String token) {
        UserPreferenceEntity userPreference = userProfilePreferenceService.findByQueueUserId(qid);
        for (String topicsToBeSubscribedTo : userPreference.getSubscriptionTopics()) {
            firebaseService.subscribeToTopic(
                new ArrayList<>() {
                    private static final long serialVersionUID = 356234509938382570L;

                    {
                        add(token);
                    }
                },
                CommonUtil.buildTopic(topicsToBeSubscribedTo, deviceType.name()));
        }
    }

    public void markFetchedSinceBeginningForDevice(String id) {
        registeredDeviceManager.markFetchedSinceBeginningForDevice(id);
    }

    public void unsetQidForDevice(String id) {
        registeredDeviceManager.unsetQidForDevice(id);
    }
}
