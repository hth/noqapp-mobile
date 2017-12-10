package com.noqapp.mobile.view.controller.open;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.json.JsonLatestAppVersion;
import com.noqapp.domain.types.DeviceTypeEnum;
import com.noqapp.mobile.domain.DeviceRegistered;
import com.noqapp.mobile.domain.body.DeviceToken;
import com.noqapp.mobile.types.LowestSupportedAppEnum;
import com.noqapp.mobile.view.ITest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Callable;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * hitender
 * 12/8/17 12:54 AM
 */
@DisplayName("Device Registration API")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("api")
class DeviceControllerITest extends ITest {
    private String did;

    private DeviceController deviceController;

    @Mock private HttpServletResponse httpServletResponse;

    @BeforeAll
    void testSetUp() {
        did = UUID.randomUUID().toString();
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        deviceController = new DeviceController(
                deviceService,
                apiHealthService
        );
    }

    @Test
    @DisplayName("Register Device")
    void registerDevice() throws IOException {
        String deviceType = DeviceTypeEnum.A.getName();

        DeviceToken deviceToken = new DeviceToken(did);

        String jsonDeviceRegistered = deviceController.registerDevice(
                new ScrubbedInput(did),
                new ScrubbedInput(deviceType),
                deviceToken.asJson(),
                httpServletResponse
        );

        DeviceRegistered deviceRegistered = new ObjectMapper().readValue(jsonDeviceRegistered, DeviceRegistered.class);
        assertEquals(1, deviceRegistered.getRegistered());

        await().atMost(5, SECONDS).until(awaitUntilDeviceIsRegistered(did));
        assertTrue(deviceService.isDeviceRegistered(null, did));
    }

    @Test
    @DisplayName("Check mobile version is supported")
    void isSupportedAppVersion() throws IOException {
        String deviceType = DeviceTypeEnum.A.getName();

        String version = String.valueOf(LowestSupportedAppEnum.VA.getOldestAppVersionNumber());
        String response = deviceController.isSupportedAppVersion(
                new ScrubbedInput(did),
                new ScrubbedInput(deviceType),
                new ScrubbedInput(version)
        );

        JsonLatestAppVersion jsonLatestAppVersion = new ObjectMapper().readValue(response, JsonLatestAppVersion.class);
        assertEquals(LowestSupportedAppEnum.VA.getLatestAppVersion(), jsonLatestAppVersion.getLatestAppVersion());
    }

    private Callable<Boolean> awaitUntilDeviceIsRegistered(String did) {
        return () -> {
            return deviceService.isDeviceRegistered(null, did); // The condition that must be fulfilled
        };
    }
}