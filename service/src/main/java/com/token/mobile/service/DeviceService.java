package com.token.mobile.service;

import org.apache.commons.lang3.StringUtils;

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

    /**
     * Checks if the device is registered, if not registered then it registers the device.
     *
     * @param rid
     * @param did
     * @param deviceType iPhone or Android
     * @return
     */
    public boolean registerDevice(String rid, String did, DeviceTypeEnum deviceType, String token) {
        RegisteredDeviceEntity registeredDevice = registeredDeviceManager.find(did, token);
        if (registeredDevice == null) {
            registeredDevice = RegisteredDeviceEntity.newInstance(rid, did, deviceType, token);
            registeredDeviceManager.save(registeredDevice);
            LOG.info("registered device for did={}", did);
        } else if (StringUtils.isNotBlank(token)) {
            registeredDevice.setReceiptUserId(rid);
            registeredDevice.setDeviceType(deviceType);
            registeredDevice.setToken(token);
            registeredDeviceManager.save(registeredDevice);
            LOG.info("updated registered device for did={} token={}", did, token);
        }
        return true;
    }
}
