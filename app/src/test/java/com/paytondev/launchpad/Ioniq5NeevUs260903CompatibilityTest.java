package com.paytondev.launchpad;

import org.junit.Test;
import static org.junit.Assert.*;

public class Ioniq5NeevUs260903CompatibilityTest {
    private static boolean neev(int sdk, String device, String software, String framework, String service) {
        return PreconditioningProfile.matchesFirmware(sdk, device, software, framework, service);
    }
    @Test public void exactCollectedIoniq5NeevUs260903FirmwareMatches() {
        assertTrue(PreconditioningProfile.supportsDevice(19, "wp_daudioplus_neev_us"));
        assertTrue(neev(19, "wp_daudioplus_neev_us", "NEEV.USA.0000.V016.010.260903",
            "c52e0825956880d627b32979022705a618200f059476c094d729133d1ff6a3be",
            "bf17e682d1beb349ae8ef2acaae6d6e908fe42e88853d7145ee16b29bb4d9ec1"));
        // Same device as NEEV US 260507, so the Ioniq 5 two minutes and 21°C apply.
        assertEquals(2 * 60 * 1000L, PreconditioningProfile.activationTimeoutMs("wp_daudioplus_neev_us"));
        assertEquals(21, PreconditioningProfile.warmThresholdC("wp_daudioplus_neev_us"));
    }
    @Test public void mismatchedApiDeviceSoftwareOrEitherHashCannotEnableIoniq5NeevUs260903() {
        String device = PreconditioningProfile.IONIQ5_NEEV_US_DEVICE, software = PreconditioningProfile.IONIQ5_NEEV_US_260903_SOFTWARE;
        String framework = PreconditioningProfile.IONIQ5_NEEV23_US_260903_FRAMEWORK_SHA256,
            service = PreconditioningProfile.IONIQ5_NEEV23_US_260903_SERVICE_SHA256;
        assertFalse(neev(20, device, software, framework, service));
        assertFalse(neev(19, "wp_daudioplus_neev_us_can", software, framework, service));
        assertFalse(neev(19, PreconditioningProfile.IONIQ5_NEEV23_US_DEVICE, software, framework, service));
        assertFalse(neev(19, PreconditioningProfile.IONIQ5_NEEV_KR_DEVICE, software, framework, service));
        assertFalse(neev(19, PreconditioningProfile.EV6_DEVICE, software, framework, service));
        assertFalse(neev(19, device, "NEEV.USA.0000.V016.010.999999", framework, service));
        assertFalse(neev(19, device, "NEEV.USA.S5W_M.V016.010.260903", framework, service));
        assertFalse(neev(19, device, "", framework, service));
        assertFalse(neev(19, device, null, framework, service));
        assertFalse(neev(19, device, software, "unknown", service));
        assertFalse(neev(19, device, software, framework, "unknown"));
        assertFalse(neev(19, device, software, null, null));
        // The hash pair only counts together: no mixing with the 260507 files.
        assertFalse(neev(19, device, software, framework, PreconditioningProfile.IONIQ5_NEEV23_US_SERVICE_SHA256));
        assertFalse(neev(19, device, software, PreconditioningProfile.IONIQ5_NEEV23_US_FRAMEWORK_SHA256, service));
        // The 260903 software doesn't unlock the 260507 files either.
        assertFalse(neev(19, device, software,
            PreconditioningProfile.IONIQ5_NEEV23_US_FRAMEWORK_SHA256, PreconditioningProfile.IONIQ5_NEEV23_US_SERVICE_SHA256));
        // Nor the 240822 files on the same device.
        assertFalse(neev(19, device, software,
            PreconditioningProfile.IONIQ5_NEEV_US_240822_FRAMEWORK_SHA256, PreconditioningProfile.IONIQ5_NEEV_US_240822_SERVICE_SHA256));
    }
    @Test public void otherIdentitiesCannotUseTheIoniq5NeevUs260903Software() {
        String framework = PreconditioningProfile.IONIQ5_NEEV23_US_260903_FRAMEWORK_SHA256,
            service = PreconditioningProfile.IONIQ5_NEEV23_US_260903_SERVICE_SHA256;
        // The NEEV US 260507 software still needs its own 260507 files.
        assertFalse(neev(19, PreconditioningProfile.IONIQ5_NEEV_US_DEVICE, PreconditioningProfile.IONIQ5_NEEV_US_SOFTWARE,
            framework, service));
        // Nor does the NEEV US 240822 software.
        assertFalse(neev(19, PreconditioningProfile.IONIQ5_NEEV_US_DEVICE, PreconditioningProfile.IONIQ5_NEEV_US_240822_SOFTWARE,
            framework, service));
        // NEEV23 US doesn't accept the NEEV US string, even with the same files.
        assertFalse(neev(19, PreconditioningProfile.IONIQ5_NEEV23_US_DEVICE, PreconditioningProfile.IONIQ5_NEEV_US_260903_SOFTWARE,
            framework, service));
    }
    @Test public void ioniq5NeevUs260903DoesNotChangeTheOtherProfiles() {
        assertTrue(neev(19, PreconditioningProfile.IONIQ5_NEEV_US_DEVICE, PreconditioningProfile.IONIQ5_NEEV_US_SOFTWARE,
            PreconditioningProfile.IONIQ5_NEEV23_US_FRAMEWORK_SHA256, PreconditioningProfile.IONIQ5_NEEV23_US_SERVICE_SHA256));
        assertTrue(neev(19, PreconditioningProfile.IONIQ5_NEEV_US_DEVICE, PreconditioningProfile.IONIQ5_NEEV_US_240822_SOFTWARE,
            PreconditioningProfile.IONIQ5_NEEV_US_240822_FRAMEWORK_SHA256, PreconditioningProfile.IONIQ5_NEEV_US_240822_SERVICE_SHA256));
        assertTrue(neev(19, PreconditioningProfile.IONIQ5_NEEV23_US_DEVICE, PreconditioningProfile.IONIQ5_NEEV23_US_260903_SOFTWARE,
            PreconditioningProfile.IONIQ5_NEEV23_US_260903_FRAMEWORK_SHA256, PreconditioningProfile.IONIQ5_NEEV23_US_260903_SERVICE_SHA256));
        assertTrue(neev(19, PreconditioningProfile.IONIQ5_NEEV23_US_DEVICE, PreconditioningProfile.IONIQ5_NEEV23_US_SOFTWARE,
            PreconditioningProfile.IONIQ5_NEEV23_US_FRAMEWORK_SHA256, PreconditioningProfile.IONIQ5_NEEV23_US_SERVICE_SHA256));
        assertTrue(neev(19, PreconditioningProfile.EV6_DEVICE, PreconditioningProfile.EV6_SOFTWARE,
            PreconditioningProfile.EV6_FRAMEWORK_SHA256, PreconditioningProfile.EV6_SERVICE_SHA256));
        assertTrue(neev(19, PreconditioningProfile.IONIQ6_CEEV_US_DEVICE, PreconditioningProfile.IONIQ6_CEEV_US_SOFTWARE,
            PreconditioningProfile.IONIQ6_CEEV_US_FRAMEWORK_SHA256, PreconditioningProfile.IONIQ6_CEEV_US_SERVICE_SHA256));
        assertTrue(neev(19, PreconditioningProfile.IONIQ5_NEEV_KR_DEVICE, PreconditioningProfile.IONIQ5_NEEV_KR_SOFTWARE,
            PreconditioningProfile.IONIQ5_NEEV_KR_FRAMEWORK_SHA256, PreconditioningProfile.IONIQ5_NEEV_KR_SERVICE_SHA256));
    }
}
