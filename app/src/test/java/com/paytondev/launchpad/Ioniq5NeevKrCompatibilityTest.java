package com.paytondev.launchpad;

import org.junit.Test;
import static org.junit.Assert.*;

public class Ioniq5NeevKrCompatibilityTest {
    private static boolean ioniq5NeevKr(int sdk, String device, String software, String framework, String service) {
        return PreconditioningProfile.matchesFirmware(sdk, device, software, framework, service);
    }
    @Test public void exactCollectedIoniq5NeevKrFirmwareMatches() {
        assertTrue(PreconditioningProfile.supportsDevice(19, "wp_daudioplus_neev_kr"));
        assertTrue(ioniq5NeevKr(19, "wp_daudioplus_neev_kr", "NEEV.KOR.0000.V039.001.260902",
            "8186fec9601a216e8e3015b77c13b4d18c2fd2dfc2d3a6b7e42dac55b7a22c06",
            "176e2375ef9e2c8b1df16b600f0434df7e2ce7bb7378779e7180e5c23651f80d"));
    }
    @Test public void mismatchedApiDeviceSoftwareOrEitherHashCannotEnableIoniq5NeevKr() {
        String device = PreconditioningProfile.IONIQ5_NEEV_KR_DEVICE, software = PreconditioningProfile.IONIQ5_NEEV_KR_SOFTWARE;
        String framework = PreconditioningProfile.IONIQ5_NEEV_KR_FRAMEWORK_SHA256, service = PreconditioningProfile.IONIQ5_NEEV_KR_SERVICE_SHA256;
        assertFalse(PreconditioningProfile.supportsDevice(20, device));
        assertFalse(ioniq5NeevKr(20, device, software, framework, service));
        assertFalse(ioniq5NeevKr(19, "wp_daudioplus_neev_kr_can", software, framework, service));
        assertFalse(ioniq5NeevKr(19, "wp_daudioplus_ecohmc_kr", software, framework, service));
        assertFalse(ioniq5NeevKr(19, device, "NEEV.KOR.0000.V039.001.999999", framework, service));
        // The displayed version (ro.build.product2) is not the checked property.
        assertFalse(ioniq5NeevKr(19, device, "NEEV.KOR.S5W_M.V039.001.260902", framework, service));
        assertFalse(ioniq5NeevKr(19, device, PreconditioningProfile.IONIQ5_NEEV_US_SOFTWARE, framework, service));
        assertFalse(ioniq5NeevKr(19, device, "", framework, service));
        assertFalse(ioniq5NeevKr(19, device, null, framework, service));
        assertFalse(ioniq5NeevKr(19, device, software,
            PreconditioningProfile.IONIQ5_NEEV23_US_FRAMEWORK_SHA256, PreconditioningProfile.IONIQ5_NEEV23_US_SERVICE_SHA256));
        assertFalse(ioniq5NeevKr(19, device, software,
            PreconditioningProfile.EV6_FRAMEWORK_SHA256, PreconditioningProfile.EV6_SERVICE_SHA256));
        assertFalse(ioniq5NeevKr(19, device, software, "unknown", service));
        assertFalse(ioniq5NeevKr(19, device, software, framework, "unknown"));
        assertFalse(ioniq5NeevKr(19, device, software, null, null));
        // Each hash pair only counts together: no mixing with another profile's files.
        assertFalse(ioniq5NeevKr(19, device, software, framework, PreconditioningProfile.IONIQ5_NEEV23_US_SERVICE_SHA256));
        assertFalse(ioniq5NeevKr(19, device, software, PreconditioningProfile.IONIQ5_NEEV23_US_FRAMEWORK_SHA256, service));
    }
    @Test public void ioniq5NeevKrFirmwareDoesNotChangeTheOtherProfiles() {
        String software = PreconditioningProfile.IONIQ5_NEEV_KR_SOFTWARE;
        String framework = PreconditioningProfile.IONIQ5_NEEV_KR_FRAMEWORK_SHA256, service = PreconditioningProfile.IONIQ5_NEEV_KR_SERVICE_SHA256;
        assertFalse(ioniq5NeevKr(19, PreconditioningProfile.IONIQ5_NEEV_US_DEVICE, software, framework, service));
        assertFalse(ioniq5NeevKr(19, PreconditioningProfile.IONIQ5_NEEV_US_DEVICE, PreconditioningProfile.IONIQ5_NEEV_US_SOFTWARE,
            framework, service));
        assertFalse(ioniq5NeevKr(19, PreconditioningProfile.IONIQ5_NEEV23_US_DEVICE, software, framework, service));
        assertFalse(ioniq5NeevKr(19, PreconditioningProfile.EV6_DEVICE, software, framework, service));
        assertFalse(ioniq5NeevKr(19, PreconditioningProfile.IONIQ6_CEEV_US_DEVICE, software, framework, service));
        assertTrue(ioniq5NeevKr(19, PreconditioningProfile.IONIQ5_NEEV_US_DEVICE, PreconditioningProfile.IONIQ5_NEEV_US_SOFTWARE,
            PreconditioningProfile.IONIQ5_NEEV23_US_FRAMEWORK_SHA256, PreconditioningProfile.IONIQ5_NEEV23_US_SERVICE_SHA256));
        assertTrue(ioniq5NeevKr(19, PreconditioningProfile.IONIQ5_NEEV23_US_DEVICE, "Ioniq 5 NEEV23 US software not constrained",
            PreconditioningProfile.IONIQ5_NEEV23_US_FRAMEWORK_SHA256, PreconditioningProfile.IONIQ5_NEEV23_US_SERVICE_SHA256));
        assertTrue(ioniq5NeevKr(19, PreconditioningProfile.IONIQ5_NEEV23_US_DEVICE, PreconditioningProfile.IONIQ5_NEEV23_US_260206_SOFTWARE,
            PreconditioningProfile.IONIQ5_NEEV23_US_260206_FRAMEWORK_SHA256, PreconditioningProfile.IONIQ5_NEEV23_US_260206_SERVICE_SHA256));
        assertTrue(ioniq5NeevKr(19, PreconditioningProfile.EV6_DEVICE, PreconditioningProfile.EV6_SOFTWARE,
            PreconditioningProfile.EV6_FRAMEWORK_SHA256, PreconditioningProfile.EV6_SERVICE_SHA256));
        assertTrue(ioniq5NeevKr(19, PreconditioningProfile.IONIQ6_CEEV_US_DEVICE, PreconditioningProfile.IONIQ6_CEEV_US_SOFTWARE,
            PreconditioningProfile.IONIQ6_CEEV_US_FRAMEWORK_SHA256, PreconditioningProfile.IONIQ6_CEEV_US_SERVICE_SHA256));
    }
}
