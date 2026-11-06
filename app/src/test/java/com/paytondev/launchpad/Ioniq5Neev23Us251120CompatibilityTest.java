package com.paytondev.launchpad;

import org.junit.Test;
import static org.junit.Assert.*;

public class Ioniq5Neev23Us251120CompatibilityTest {
    private static boolean neev23(int sdk, String device, String software, String framework, String service) {
        return PreconditioningProfile.matchesFirmware(sdk, device, software, framework, service);
    }

    @Test public void exactCollectedIoniq5Neev23Us251120FirmwareMatches() {
        assertTrue(PreconditioningProfile.supportsDevice(19, "wp_daudioplus_neev23_us"));
        assertTrue(neev23(19, "wp_daudioplus_neev23_us", "NEEV23.USA.0000.V014.011.251120",
            "adf81f0967b6ea400bea54b70ea0bba65bce98b10da19f9765ee8fe80efe17c1",
            "f6dd0c443217ac2f15437d9843f4cff53b63d9aa74e88b28ca0f934dd68d22ab"));
        assertEquals(2 * 60 * 1000L,
            PreconditioningProfile.activationTimeoutMs("wp_daudioplus_neev23_us"));
        assertEquals(21, PreconditioningProfile.warmThresholdC("wp_daudioplus_neev23_us"));
    }

    @Test public void mismatchedApiDeviceSoftwareOrHashesCannotEnableIoniq5Neev23Us251120() {
        String device = "wp_daudioplus_neev23_us";
        String software = "NEEV23.USA.0000.V014.011.251120";
        String framework = "adf81f0967b6ea400bea54b70ea0bba65bce98b10da19f9765ee8fe80efe17c1";
        String service = "f6dd0c443217ac2f15437d9843f4cff53b63d9aa74e88b28ca0f934dd68d22ab";

        assertFalse(neev23(20, device, software, framework, service));
        assertFalse(neev23(19, "wp_daudioplus_neev23_us_can", software, framework, service));
        assertFalse(neev23(19, PreconditioningProfile.IONIQ5_NEEV_US_DEVICE, software, framework, service));
        assertFalse(neev23(19, PreconditioningProfile.EV6_DEVICE, software, framework, service));
        assertFalse(neev23(19, PreconditioningProfile.IONIQ6_CEEV_US_DEVICE, software, framework, service));
        assertFalse(neev23(19, device, "NEEV23.USA.0000.V014.011.999999", framework, service));
        assertFalse(neev23(19, device, "NEEV23.USA.S5W_M.V014.011.251120", framework, service));
        assertFalse(neev23(19, device, "", framework, service));
        assertFalse(neev23(19, device, null, framework, service));
        assertFalse(neev23(19, device, software, "unknown", service));
        assertFalse(neev23(19, device, software, framework, "unknown"));
        assertFalse(neev23(19, device, software, null, null));

        assertFalse(neev23(19, device, software, framework,
            PreconditioningProfile.IONIQ5_NEEV23_US_260903_SERVICE_SHA256));
        assertFalse(neev23(19, device, software,
            PreconditioningProfile.IONIQ5_NEEV23_US_260903_FRAMEWORK_SHA256, service));
        assertFalse(neev23(19, device, software, framework,
            PreconditioningProfile.IONIQ5_NEEV23_US_260206_SERVICE_SHA256));
        assertFalse(neev23(19, device, software,
            PreconditioningProfile.IONIQ5_NEEV23_US_260206_FRAMEWORK_SHA256, service));
        assertFalse(neev23(19, device, software,
            PreconditioningProfile.IONIQ5_NEEV23_US_FRAMEWORK_SHA256,
            PreconditioningProfile.IONIQ5_NEEV23_US_SERVICE_SHA256));
    }

    @Test public void ioniq5Neev23Us251120DoesNotChangeOtherProfiles() {
        assertTrue(neev23(19, PreconditioningProfile.IONIQ5_NEEV23_US_DEVICE,
            PreconditioningProfile.IONIQ5_NEEV23_US_260903_SOFTWARE,
            PreconditioningProfile.IONIQ5_NEEV23_US_260903_FRAMEWORK_SHA256,
            PreconditioningProfile.IONIQ5_NEEV23_US_260903_SERVICE_SHA256));
        assertTrue(neev23(19, PreconditioningProfile.IONIQ5_NEEV23_US_DEVICE,
            PreconditioningProfile.IONIQ5_NEEV23_US_260206_SOFTWARE,
            PreconditioningProfile.IONIQ5_NEEV23_US_260206_FRAMEWORK_SHA256,
            PreconditioningProfile.IONIQ5_NEEV23_US_260206_SERVICE_SHA256));
        assertTrue(neev23(19, PreconditioningProfile.IONIQ5_NEEV23_US_DEVICE,
            PreconditioningProfile.IONIQ5_NEEV23_US_SOFTWARE,
            PreconditioningProfile.IONIQ5_NEEV23_US_FRAMEWORK_SHA256,
            PreconditioningProfile.IONIQ5_NEEV23_US_SERVICE_SHA256));
        assertTrue(neev23(19, PreconditioningProfile.EV6_DEVICE, PreconditioningProfile.EV6_SOFTWARE,
            PreconditioningProfile.EV6_FRAMEWORK_SHA256, PreconditioningProfile.EV6_SERVICE_SHA256));
    }
}