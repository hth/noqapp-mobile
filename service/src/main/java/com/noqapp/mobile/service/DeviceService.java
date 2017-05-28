package com.noqapp.mobile.service;

import com.mongodb.DuplicateKeyException;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.noqapp.domain.RegisteredDeviceEntity;
import com.noqapp.domain.types.DeviceTypeEnum;
import com.noqapp.repository.RegisteredDeviceManager;

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

    @Autowired
    public DeviceService(RegisteredDeviceManager registeredDeviceManager) {
        this.registeredDeviceManager = registeredDeviceManager;
    }

    /**
     * Checks if the device is registered, if not registered then it registers the device.
     *
     * @param rid
     * @param did
     * @param deviceType iPhone or Android
     * @return
     */
    public boolean registerDevice(String rid, String did, DeviceTypeEnum deviceType, String token) {
        try {
            RegisteredDeviceEntity registeredDevice = registeredDeviceManager.find(rid, did);
            if (registeredDevice == null) {
                LOG.info("Registering new deviceType={} did={} rid={}", deviceType, did, rid);
                registeredDevice = RegisteredDeviceEntity.newInstance(rid, did, deviceType, token);
                try {
                    registeredDeviceManager.save(registeredDevice);
                    LOG.info("registered device for did={}", did);
                } catch (DuplicateKeyException duplicateKeyException) {
                    LOG.warn("Already registered device exists, update existing with new details deviceType={} did={} rid={}",
                            deviceType, did, rid);
                    
                    /* Reset update date with create date to fetch all the possible historical data. */
                    boolean updateStatus = registeredDeviceManager.resetRegisteredDeviceWithNewDetails(
                            registeredDevice.getDeviceId(),
                            rid,
                            deviceType,
                            token
                    );
                    LOG.info("existing registered device updateStatus={} with rid={} token={}", updateStatus, rid, token);
                }
            } else if (StringUtils.isNotBlank(token)) {
                LOG.info("Updating registered device of deviceType={} did={} rid={}", deviceType, did, rid);
                registeredDevice.setReceiptUserId(rid);
                registeredDevice.setDeviceType(deviceType);
                registeredDevice.setToken(token);
                registeredDeviceManager.save(registeredDevice);
                LOG.info("updated registered device for did={} token={}", did, token);
            }
            return true;
        } catch (Exception e) {
            LOG.error("Failed device registration deviceType={} did={} rid={} reason={}", deviceType, did, rid, e.getLocalizedMessage(), e);
            return false;
        }
    }

    public boolean isDeviceRegistered(String rid, String did) {
        return registeredDeviceManager.find(rid, did) != null;
    }

    public RegisteredDeviceEntity lastAccessed(String rid, String did, String token) {
        return registeredDeviceManager.lastAccessed(rid, did, token);
    }
}
