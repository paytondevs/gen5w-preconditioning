package com.paytondev.launchpad;

import org.junit.Test;
import static org.junit.Assert.*;

public class Ioniq5Neev23Us260903CompatibilityTest {
    private static boolean neev23(int sdk, String device, String software, String framework, String service) {
        return PreconditioningProfile.matchesFirmware(sdk, device, software, framework, service);
    }
    @Test public void exactCollectedIoniq5Neev23Us260903FirmwareMatches() {
        assertTrue(PreconditioningProfile.supportsDevice(19, "wp_daudioplus_neev23_us"));
        assertTrue(neev23(19, "wp_daudioplus_neev23_us", "NEEV23.USA.0000.V016.010.260903",
            "c52e0825956880d627b32979022705a618200f059476c094d729133d1ff6a3be",
            "bf17e682d1beb349ae8ef2acaae6d6e908fe42e88853d7145ee16b29bb4d9ec1"));
        // Same device as the other NEEV23 US releases, so the Ioniq 5 two minutes and 21°C apply.
        assertEquals(2 * 60 * 1000L, PreconditioningProfile.activationTimeoutMs("wp_daudioplus_neev23_us"));
        assertEquals(21, PreconditioningProfile.warmThresholdC("wp_daudioplus_neev23_us"));
    }
    @Test public void mismatchedApiDeviceSoftwareOrEitherHashCannotEnableIoniq5Neev23Us260903() {
        String device = PreconditioningProfile.IONIQ5_NEEV23_US_DEVICE, software = PreconditioningProfile.IONIQ5_NEEV23_US_260903_SOFTWARE;
        String framework = PreconditioningProfile.IONIQ5_NEEV23_US_260903_FRAMEWORK_SHA256,
            service = PreconditioningProfile.IONIQ5_NEEV23_US_260903_SERVICE_SHA256;
        assertFalse(neev23(20, device, software, framework, service));
        assertFalse(neev23(19, "wp_daudioplus_neev23_us_can", software, framework, service));
        assertFalse(neev23(19, PreconditioningProfile.IONIQ5_NEEV_US_DEVICE, software, framework, service));
        assertFalse(neev23(19, PreconditioningProfile.EV6_DEVICE, software, framework, service));
        assertFalse(neev23(19, PreconditioningProfile.IONIQ6_CEEV_US_DEVICE, software, framework, service));
        assertFalse(neev23(19, device, "NEEV23.USA.0000.V016.010.999999", framework, service));
        assertFalse(neev23(19, device, "NEEV23.USA.S5W_M.V016.010.260903", framework, service));
        assertFalse(neev23(19, device, "", framework, service));
        assertFalse(neev23(19, device, null, framework, service));
        assertFalse(neev23(19, device, software, "unknown", service));
        assertFalse(neev23(19, device, software, framework, "unknown"));
        assertFalse(neev23(19, device, software, null, null));
        // Each hash pair only counts together: no mixing with the 260206 or 260507 files.
        assertFalse(neev23(19, device, software, framework, PreconditioningProfile.IONIQ5_NEEV23_US_260206_SERVICE_SHA256));
        assertFalse(neev23(19, device, software, PreconditioningProfile.IONIQ5_NEEV23_US_260206_FRAMEWORK_SHA256, service));
        assertFalse(neev23(19, device, software, framework, PreconditioningProfile.IONIQ5_NEEV23_US_SERVICE_SHA256));
        assertFalse(neev23(19, device, software, PreconditioningProfile.IONIQ5_NEEV23_US_FRAMEWORK_SHA256, service));
        // The 260903 software doesn't unlock the 260206 files either.
        assertFalse(neev23(19, device, software,
            PreconditioningProfile.IONIQ5_NEEV23_US_260206_FRAMEWORK_SHA256, PreconditioningProfile.IONIQ5_NEEV23_US_260206_SERVICE_SHA256));
    }
    @Test public void otherIdentitiesCannotUseTheIoniq5Neev23Us260903Files() {
        String framework = PreconditioningProfile.IONIQ5_NEEV23_US_260903_FRAMEWORK_SHA256,
            service = PreconditioningProfile.IONIQ5_NEEV23_US_260903_SERVICE_SHA256;
        // Same device on its 260206 and 260507 releases, or any other software.
        assertFalse(neev23(19, PreconditioningProfile.IONIQ5_NEEV23_US_DEVICE, PreconditioningProfile.IONIQ5_NEEV23_US_260206_SOFTWARE,
            framework, service));
        assertFalse(neev23(19, PreconditioningProfile.IONIQ5_NEEV23_US_DEVICE, "NEEV23.USA.0000.V015.012.260507", framework, service));
        assertFalse(neev23(19, PreconditioningProfile.IONIQ5_NEEV23_US_DEVICE, "Ioniq 5 NEEV23 US software not constrained",
            framework, service));
        // Other head units on their own exact identities.
        assertFalse(neev23(19, PreconditioningProfile.IONIQ5_NEEV_US_DEVICE, PreconditioningProfile.IONIQ5_NEEV_US_SOFTWARE, framework, service));
        assertFalse(neev23(19, PreconditioningProfile.EV6_DEVICE, PreconditioningProfile.EV6_SOFTWARE, framework, service));
        assertFalse(neev23(19, PreconditioningProfile.IONIQ6_CEEV_US_DEVICE, PreconditioningProfile.IONIQ6_CEEV_US_SOFTWARE,
            framework, service));
    }
    @Test public void ioniq5Neev23Us260903DoesNotChangeTheOtherProfiles() {
        // The original NEEV23 US profile still matches its own hashes, with any software.
        assertTrue(neev23(19, PreconditioningProfile.IONIQ5_NEEV23_US_DEVICE, "Ioniq 5 NEEV23 US software not constrained",
            PreconditioningProfile.IONIQ5_NEEV23_US_FRAMEWORK_SHA256, PreconditioningProfile.IONIQ5_NEEV23_US_SERVICE_SHA256));
        assertTrue(neev23(19, PreconditioningProfile.IONIQ5_NEEV23_US_DEVICE, PreconditioningProfile.IONIQ5_NEEV23_US_260206_SOFTWARE,
            PreconditioningProfile.IONIQ5_NEEV23_US_260206_FRAMEWORK_SHA256, PreconditioningProfile.IONIQ5_NEEV23_US_260206_SERVICE_SHA256));
        assertTrue(neev23(19, PreconditioningProfile.IONIQ5_NEEV_US_DEVICE, PreconditioningProfile.IONIQ5_NEEV_US_SOFTWARE,
            PreconditioningProfile.IONIQ5_NEEV23_US_FRAMEWORK_SHA256, PreconditioningProfile.IONIQ5_NEEV23_US_SERVICE_SHA256));
        assertTrue(neev23(19, PreconditioningProfile.EV6_DEVICE, PreconditioningProfile.EV6_SOFTWARE,
            PreconditioningProfile.EV6_FRAMEWORK_SHA256, PreconditioningProfile.EV6_SERVICE_SHA256));
        assertTrue(neev23(19, PreconditioningProfile.IONIQ6_CEEV_US_DEVICE, PreconditioningProfile.IONIQ6_CEEV_US_SOFTWARE,
            PreconditioningProfile.IONIQ6_CEEV_US_FRAMEWORK_SHA256, PreconditioningProfile.IONIQ6_CEEV_US_SERVICE_SHA256));
    }
}
