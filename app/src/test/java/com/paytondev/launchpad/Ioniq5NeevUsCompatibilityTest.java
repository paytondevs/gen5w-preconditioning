package com.paytondev.launchpad;

import org.junit.Test;
import static org.junit.Assert.*;

public class Ioniq5NeevUsCompatibilityTest {
    private static boolean ioniq5Neev(int sdk, String device, String software, String framework, String service) {
        return PreconditioningProfile.matchesFirmware(sdk, device, software, framework, service);
    }
    @Test public void exactCollectedIoniq5NeevUsFirmwareMatches() {
        assertTrue(PreconditioningProfile.supportsDevice(19, "wp_daudioplus_neev_us"));
        assertTrue(ioniq5Neev(19, "wp_daudioplus_neev_us", "NEEV.USA.0000.V015.012.260507",
            "dd72e04bce0a2b4bda1506308ac21f91002c1436553b8156c345cc4313661338",
            "3cf40fb00854c63afa49abea1dad18a0afcbc0dd68b17de9ed7ea3f5aeab4222"));
    }
    @Test public void mismatchedApiSoftwareOrEitherHashCannotEnableIoniq5NeevUs() {
        String device = PreconditioningProfile.IONIQ5_NEEV_US_DEVICE, software = PreconditioningProfile.IONIQ5_NEEV_US_SOFTWARE;
        String framework = PreconditioningProfile.IONIQ5_NEEV23_US_FRAMEWORK_SHA256, service = PreconditioningProfile.IONIQ5_NEEV23_US_SERVICE_SHA256;
        assertFalse(ioniq5Neev(19, device, software,
            PreconditioningProfile.EV6_FRAMEWORK_SHA256, PreconditioningProfile.EV6_SERVICE_SHA256));
        assertFalse(ioniq5Neev(20, device, software, framework, service));
        assertFalse(ioniq5Neev(19, "wp_daudioplus_neev_us_can", software, framework, service));
        assertFalse(ioniq5Neev(19, device, "NEEV.USA.0000.V015.012.999999", framework, service));
        assertFalse(ioniq5Neev(19, device, PreconditioningProfile.EV6_SOFTWARE, framework, service));
        assertFalse(ioniq5Neev(19, device, software, "unknown", service));
        assertFalse(ioniq5Neev(19, device, software, framework, "unknown"));
        assertFalse(ioniq5Neev(19, device, null, framework, service));
        assertFalse(ioniq5Neev(19, device, software, null, null));
    }
    @Test public void ioniq5NeevUsSoftwareDoesNotChangeTheOtherProfiles() {
        assertFalse(ioniq5Neev(19, PreconditioningProfile.EV6_DEVICE, PreconditioningProfile.IONIQ5_NEEV_US_SOFTWARE,
            PreconditioningProfile.IONIQ5_NEEV23_US_FRAMEWORK_SHA256, PreconditioningProfile.IONIQ5_NEEV23_US_SERVICE_SHA256));
        assertTrue(ioniq5Neev(19, PreconditioningProfile.IONIQ5_NEEV23_US_DEVICE, "Ioniq 5 NEEV23 US software not constrained",
            PreconditioningProfile.IONIQ5_NEEV23_US_FRAMEWORK_SHA256, PreconditioningProfile.IONIQ5_NEEV23_US_SERVICE_SHA256));
    }
}
