package com.noqapp.mobile.view.controller.open;

import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.MOBILE_UPGRADE;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.noqapp.common.errors.ErrorJsonList;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.json.JsonLatestAppVersion;
import com.noqapp.domain.types.AppFlavorEnum;
import com.noqapp.mobile.domain.DeviceRegistered;
import com.noqapp.mobile.domain.body.client.DeviceToken;
import com.noqapp.mobile.types.LowestSupportedAppEnum;
import com.noqapp.mobile.view.ITest;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * hitender
 * 12/8/17 12:54 AM
 */
@DisplayName("Device Registration API")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("api")
class DeviceControllerITest extends ITest {
    private DeviceController deviceController;

    @BeforeEach
    void setUp() {
        deviceController = new DeviceController(
            deviceRegistrationService,
            geoIPLocationService,
            apiHealthService
        );
    }

    @Test
    @DisplayName("Register Device")
    void registerDevice() throws IOException {
        DeviceToken deviceToken = new DeviceToken(fcmToken, model, osVersion, appVersion);

        String jsonDeviceRegistered = deviceController.registerDevice(
            new ScrubbedInput(deviceType),
            new ScrubbedInput(AppFlavorEnum.NQCL.getName()),
            deviceToken.asJson(),
            httpServletRequest
        );

        DeviceRegistered deviceRegistered = new ObjectMapper().readValue(jsonDeviceRegistered, DeviceRegistered.class);
        assertEquals(1, deviceRegistered.getRegistered());

        await().atMost(5, SECONDS).until(awaitUntilDeviceIsRegistered(deviceRegistered.getDeviceId()));
        assertTrue(deviceRegistrationService.isDeviceRegistered(null, deviceRegistered.getDeviceId()));
    }

    @Test
    @DisplayName("Check mobile version with flavor. Upgrade Request.")
    void isSupportedWithFlavor_UpgradeRequested() throws IOException {
        String version = "1.2.600";
        String response = deviceController.isSupportedAppVersion(
            new ScrubbedInput(deviceType),
            new ScrubbedInput(AppFlavorEnum.NQCL.getName()),
            new ScrubbedInput(version)
        );

        ErrorJsonList errorJsonList = new ObjectMapper().readValue(response, ErrorJsonList.class);
        assertEquals(MOBILE_UPGRADE.getCode(), errorJsonList.getError().getSystemErrorCode());
    }

    @Test
    @DisplayName("Check mobile version is supported with flavor. Response Client VACL")
    void isSupportedWithFlavor_Success_NQCL() throws IOException {
        String version = "1.3.150";
        String response = deviceController.isSupportedAppVersion(
            new ScrubbedInput(deviceType),
            new ScrubbedInput(AppFlavorEnum.NQCL.getName()),
            new ScrubbedInput(version)
        );

        JsonLatestAppVersion jsonLatestAppVersion = new ObjectMapper().readValue(response, JsonLatestAppVersion.class);
        assertEquals(LowestSupportedAppEnum.VACL.getLatestAppVersion(), jsonLatestAppVersion.getLatestAppVersion());
    }

    @Test
    @DisplayName("Check mobile version is supported with flavor. Response Merchant Medical VAMH")
    void isSupportedWithFlavor_Success_NQMH() throws IOException {
        String version = "1.3.150";
        String response = deviceController.isSupportedAppVersion(
            new ScrubbedInput(deviceType),
            new ScrubbedInput(AppFlavorEnum.NQMH.getName()),
            new ScrubbedInput(version)
        );

        JsonLatestAppVersion jsonLatestAppVersion = new ObjectMapper().readValue(response, JsonLatestAppVersion.class);
        assertEquals(LowestSupportedAppEnum.VAMH.getLatestAppVersion(), jsonLatestAppVersion.getLatestAppVersion());
    }

    private Callable<Boolean> awaitUntilDeviceIsRegistered(String did) {
        return () -> {
            return deviceRegistrationService.isDeviceRegistered(null, did); // The condition that must be fulfilled
        };
    }
}
