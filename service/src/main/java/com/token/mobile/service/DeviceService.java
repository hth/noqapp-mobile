package com.token.mobile.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.token.domain.RegisteredDeviceEntity;
import com.token.domain.types.DeviceTypeEnum;
import com.token.repository.RegisteredDeviceManager;

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

    public RegisteredDeviceEntity lastAccessed(String did) {
        return registeredDeviceManager.lastAccessed(null, did);
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
        boolean registrationSuccess = false;
        RegisteredDeviceEntity registeredDevice = registeredDeviceManager.registerDevice(rid, did, deviceType, token);
        if (null == registeredDevice) {
            LOG.error("Failure device registration rid={} did={}", rid, did);
        } else {
            LOG.info("Success device registration rid={} did={}", rid, registeredDevice.getDeviceId());
            registrationSuccess = true;
        }
        return registrationSuccess;
    }

}
