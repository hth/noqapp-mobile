package com.noqapp.mobile.service;

import static java.util.concurrent.Executors.newCachedThreadPool;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.noqapp.domain.RegisteredDeviceEntity;
import com.noqapp.domain.types.DeviceTypeEnum;
import com.noqapp.repository.RegisteredDeviceManager;

import java.util.concurrent.ExecutorService;

/**
 * User: hitender
 * Date: 3/1/17 12:40 PM
 */
@SuppressWarnings ({
        "PMD.BeanMembersShouldSerialize",
        "PMD.LocalVariableCouldBeFinal",
        "PMD.MethodArgumentCouldBeFinal",
        "PMD.LongVariable"
})
@Service
public class DeviceService {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceService.class);

    private RegisteredDeviceManager registeredDeviceManager;

    private ExecutorService executorService;

    @Autowired
    public DeviceService(RegisteredDeviceManager registeredDeviceManager) {
        this.registeredDeviceManager = registeredDeviceManager;

        this.executorService = newCachedThreadPool();
    }

    /**
     * Since registration can be done in background. Moved logic to thread.
     *
     * @param qid
     * @param did
     * @param deviceType
     * @param token
     */
    public void registerDevice(String qid, String did, DeviceTypeEnum deviceType, String token) {
        executorService.submit(() -> registeringDevice(qid, did, deviceType, token));
    }

    /**
     * Checks if the device is registered, if not registered then it registers the device.
     *
     * @param qid
     * @param did
     * @param deviceType iPhone or Android
     * @return
     */
    private void registeringDevice(String qid, String did, DeviceTypeEnum deviceType, String token) {
        try {
            RegisteredDeviceEntity registeredDevice = registeredDeviceManager.find(qid, did);
            if (registeredDevice == null) {
                LOG.info("Registering new deviceType={} did={} rid={}", deviceType, did, qid);
                registeredDevice = RegisteredDeviceEntity.newInstance(qid, did, deviceType, token);
                try {
                    registeredDeviceManager.save(registeredDevice);
                    LOG.info("registered device for did={}", did);
                } catch (DuplicateKeyException duplicateKeyException) {
                    LOG.warn("Already registered device exists, update existing with new details deviceType={} did={} qid={}",
                            deviceType, did, qid);
                    
                    /* Reset update date with create date to fetch all the possible historical data. */
                    boolean updateStatus = registeredDeviceManager.resetRegisteredDeviceWithNewDetails(
                            registeredDevice.getDeviceId(),
                            qid,
                            deviceType,
                            token
                    );
                    LOG.info("existing registered device updateStatus={} with qid={} token={}", updateStatus, qid, token);
                }
            } else if (StringUtils.isNotBlank(token)) {
                LOG.info("Updating registered device of deviceType={} did={} qid={}", deviceType, did, qid);
                registeredDevice.setQueueUserId(qid);
                registeredDevice.setDeviceType(deviceType);
                registeredDevice.setToken(token);
                registeredDevice.setSinceBeginning(true);
                registeredDeviceManager.save(registeredDevice);
                LOG.info("updated registered device for did={} token={}", did, token);
            }
        } catch (Exception e) {
            LOG.error("Failed device registration deviceType={} did={} rid={} reason={}", deviceType, did, qid, e.getLocalizedMessage(), e);
        }
    }

    public boolean isDeviceRegistered(String qid, String did) {
        return registeredDeviceManager.find(qid, did) != null;
    }

    RegisteredDeviceEntity lastAccessed(String qid, String did, String token) {
        return registeredDeviceManager.lastAccessed(qid, did, token);
    }

    /**
     * Update Registered Device after register or login when token is not available.
     *
     * @param qid
     * @param did
     */
    public void updateRegisteredDevice(String qid, String did, DeviceTypeEnum deviceType) {
        RegisteredDeviceEntity registeredDevice = registeredDeviceManager.find(null, did);

        if (null == registeredDevice) {
            LOG.warn("Failed finding Registered device to update with qid={} did={} deviceType={}", qid, did, deviceType);
            return;
        }

        registeredDevice.setQueueUserId(qid);
        registeredDevice.setDeviceType(deviceType);
        registeredDevice.setSinceBeginning(true);
        registeredDeviceManager.save(registeredDevice);
    }

    void markFetchedSinceBeginningForDevice(String id) {
        registeredDeviceManager.markFetchedSinceBeginningForDevice(id);
    }

    void unsetQidForDevice(String id) {
        registeredDeviceManager.unsetQidForDevice(id);
    }
}
