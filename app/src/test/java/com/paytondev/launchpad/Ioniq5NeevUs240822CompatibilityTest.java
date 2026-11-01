package com.paytondev.launchpad;

import org.junit.Test;
import static org.junit.Assert.*;

public class Ioniq5NeevUs240822CompatibilityTest {
    private static boolean ioniq5Neev(int sdk, String device, String software, String framework, String service) {
        return PreconditioningProfile.matchesFirmware(sdk, device, software, framework, service);
    }
    @Test public void exactCollectedIoniq5NeevUs240822FirmwareMatches() {
        assertTrue(PreconditioningProfile.supportsDevice(19, "wp_daudioplus_neev_us"));
        assertTrue(ioniq5Neev(19, "wp_daudioplus_neev_us", "NEEV.USA.0000.V012.010.240822",
            "0f6786b9c8c167f09c08c559afea7da1e2137e2a1968b9ab372def65762674f9",
            "2758a413c184855bf92080cfdc6decbfcd6f09775128a982b1eee211993772a1"));
        // Same device as NEEV US 260507, so the Ioniq 5 two minutes and 21°C apply.
        assertEquals(2 * 60 * 1000L, PreconditioningProfile.activationTimeoutMs("wp_daudioplus_neev_us"));
        assertEquals(21, PreconditioningProfile.warmThresholdC("wp_daudioplus_neev_us"));
    }
    @Test public void mismatchedApiDeviceSoftwareOrEitherHashCannotEnableIoniq5NeevUs240822() {
        String device = PreconditioningProfile.IONIQ5_NEEV_US_DEVICE,
            software = PreconditioningProfile.IONIQ5_NEEV_US_240822_SOFTWARE;
        String framework = PreconditioningProfile.IONIQ5_NEEV_US_240822_FRAMEWORK_SHA256,
            service = PreconditioningProfile.IONIQ5_NEEV_US_240822_SERVICE_SHA256;
        assertFalse(ioniq5Neev(20, device, software, framework, service));
        assertFalse(ioniq5Neev(19, "wp_daudioplus_neev_us_can", software, framework, service));
        assertFalse(ioniq5Neev(19, "wp_daudioplus_neev_us_knob", software, framework, service));
        assertFalse(ioniq5Neev(19, PreconditioningProfile.IONIQ5_NEEV23_US_DEVICE, software, framework, service));
        assertFalse(ioniq5Neev(19, PreconditioningProfile.EV6_DEVICE, software, framework, service));
        assertFalse(ioniq5Neev(19, PreconditioningProfile.IONIQ6_CEEV_US_DEVICE, software, framework, service));
        assertFalse(ioniq5Neev(19, PreconditioningProfile.IONIQ5_NEEV_KR_DEVICE, software, framework, service));
        assertFalse(ioniq5Neev(19, null, software, framework, service));
        assertFalse(ioniq5Neev(19, device, "NEEV.USA.0000.V012.010.999999", framework, service));
        assertFalse(ioniq5Neev(19, device, "NEEV.USA.S5W_M.V012.010.240822", framework, service));
        assertFalse(ioniq5Neev(19, device, "NEEV23.USA.0000.V012.010.240822", framework, service));
        assertFalse(ioniq5Neev(19, device, "", framework, service));
        assertFalse(ioniq5Neev(19, device, null, framework, service));
        assertFalse(ioniq5Neev(19, device, software, "unknown", service));
        assertFalse(ioniq5Neev(19, device, software, framework, "unknown"));
        assertFalse(ioniq5Neev(19, device, software, null, null));
        // Each hash pair only counts together: no mixing with the NEEV US 260507 files.
        assertFalse(ioniq5Neev(19, device, software, framework, PreconditioningProfile.IONIQ5_NEEV23_US_SERVICE_SHA256));
        assertFalse(ioniq5Neev(19, device, software, PreconditioningProfile.IONIQ5_NEEV23_US_FRAMEWORK_SHA256, service));
        // The 240822 software doesn't unlock the 260507 files either.
        assertFalse(ioniq5Neev(19, device, software,
            PreconditioningProfile.IONIQ5_NEEV23_US_FRAMEWORK_SHA256, PreconditioningProfile.IONIQ5_NEEV23_US_SERVICE_SHA256));
    }
    @Test public void otherIdentitiesCannotUseTheIoniq5NeevUs240822Files() {
        String framework = PreconditioningProfile.IONIQ5_NEEV_US_240822_FRAMEWORK_SHA256,
            service = PreconditioningProfile.IONIQ5_NEEV_US_240822_SERVICE_SHA256;
        // Same device on its 260507 release, or any other software.
        assertFalse(ioniq5Neev(19, PreconditioningProfile.IONIQ5_NEEV_US_DEVICE, PreconditioningProfile.IONIQ5_NEEV_US_SOFTWARE,
            framework, service));
        assertFalse(ioniq5Neev(19, PreconditioningProfile.IONIQ5_NEEV_US_DEVICE, PreconditioningProfile.IONIQ5_NEEV23_US_SOFTWARE,
            framework, service));
        // Other head units on their own exact identities.
        assertFalse(ioniq5Neev(19, PreconditioningProfile.IONIQ5_NEEV23_US_DEVICE, PreconditioningProfile.IONIQ5_NEEV23_US_SOFTWARE,
            framework, service));
        assertFalse(ioniq5Neev(19, PreconditioningProfile.EV6_DEVICE, PreconditioningProfile.EV6_SOFTWARE, framework, service));
        assertFalse(ioniq5Neev(19, PreconditioningProfile.IONIQ6_CEEV_US_DEVICE, PreconditioningProfile.IONIQ6_CEEV_US_SOFTWARE,
            framework, service));
        assertFalse(ioniq5Neev(19, PreconditioningProfile.IONIQ5_NEEV_KR_DEVICE, PreconditioningProfile.IONIQ5_NEEV_KR_SOFTWARE,
            framework, service));
    }
    @Test public void ioniq5NeevUs240822DoesNotChangeTheOtherProfiles() {
        // The NEEV US 260507 profile still matches its own software and the NEEV23 US hashes.
        assertTrue(ioniq5Neev(19, PreconditioningProfile.IONIQ5_NEEV_US_DEVICE, PreconditioningProfile.IONIQ5_NEEV_US_SOFTWARE,
            PreconditioningProfile.IONIQ5_NEEV23_US_FRAMEWORK_SHA256, PreconditioningProfile.IONIQ5_NEEV23_US_SERVICE_SHA256));
        assertTrue(ioniq5Neev(19, PreconditioningProfile.IONIQ5_NEEV23_US_DEVICE, PreconditioningProfile.IONIQ5_NEEV23_US_SOFTWARE,
            PreconditioningProfile.IONIQ5_NEEV23_US_FRAMEWORK_SHA256, PreconditioningProfile.IONIQ5_NEEV23_US_SERVICE_SHA256));
        assertTrue(ioniq5Neev(19, PreconditioningProfile.EV6_DEVICE, PreconditioningProfile.EV6_SOFTWARE,
            PreconditioningProfile.EV6_FRAMEWORK_SHA256, PreconditioningProfile.EV6_SERVICE_SHA256));
        assertTrue(ioniq5Neev(19, PreconditioningProfile.EV6_DEVICE, PreconditioningProfile.EV6_251120_SOFTWARE,
            PreconditioningProfile.EV6_251120_FRAMEWORK_SHA256, PreconditioningProfile.EV6_251120_SERVICE_SHA256));
        assertTrue(ioniq5Neev(19, PreconditioningProfile.IONIQ6_CEEV_US_DEVICE, PreconditioningProfile.IONIQ6_CEEV_US_SOFTWARE,
            PreconditioningProfile.IONIQ6_CEEV_US_FRAMEWORK_SHA256, PreconditioningProfile.IONIQ6_CEEV_US_SERVICE_SHA256));
        assertTrue(ioniq5Neev(19, PreconditioningProfile.IONIQ5_NEEV_KR_DEVICE, PreconditioningProfile.IONIQ5_NEEV_KR_SOFTWARE,
            PreconditioningProfile.IONIQ5_NEEV_KR_FRAMEWORK_SHA256, PreconditioningProfile.IONIQ5_NEEV_KR_SERVICE_SHA256));
        // The NEEV US 260507 software is not accepted with the 240822 hashes.
        assertFalse(ioniq5Neev(19, PreconditioningProfile.IONIQ5_NEEV_US_DEVICE, PreconditioningProfile.IONIQ5_NEEV_US_SOFTWARE,
            PreconditioningProfile.IONIQ5_NEEV_US_240822_FRAMEWORK_SHA256, PreconditioningProfile.IONIQ5_NEEV_US_240822_SERVICE_SHA256));
    }
}
