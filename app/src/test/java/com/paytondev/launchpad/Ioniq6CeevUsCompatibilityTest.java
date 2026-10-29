package com.paytondev.launchpad;

import org.junit.Test;
import static org.junit.Assert.*;

public class Ioniq6CeevUsCompatibilityTest {
    private static boolean ioniq6Ceev(int sdk, String device, String software, String framework, String service) {
        return PreconditioningProfile.matchesFirmware(sdk, device, software, framework, service);
    }
    @Test public void exactCollectedIoniq6CeevUsFirmwareMatches() {
        assertTrue(PreconditioningProfile.supportsDevice(19, "wp_daudioplus_ceev_us"));
        assertTrue(ioniq6Ceev(19, "wp_daudioplus_ceev_us", "CEEV.USA.0000.V015.012.260507",
            "408b426e14b3f8dfdf1926e608e4f02689e89fc386b7f93a97f261ac5b6e4287",
            "afbc60509dd8a8fbac842523bf618424c088685d832a125a84d1d11dc1770bd4"));
    }
    @Test public void mismatchedApiDeviceSoftwareOrEitherHashCannotEnableIoniq6CeevUs() {
        String device = PreconditioningProfile.IONIQ6_CEEV_US_DEVICE, software = PreconditioningProfile.IONIQ6_CEEV_US_SOFTWARE;
        String framework = PreconditioningProfile.IONIQ6_CEEV_US_FRAMEWORK_SHA256, service = PreconditioningProfile.IONIQ6_CEEV_US_SERVICE_SHA256;
        assertFalse(PreconditioningProfile.supportsDevice(20, device));
        assertFalse(ioniq6Ceev(20, device, software, framework, service));
        assertFalse(ioniq6Ceev(19, "wp_daudioplus_ceev_us_can", software, framework, service));
        assertFalse(ioniq6Ceev(19, device, "CEEV.USA.0000.V015.012.999999", framework, service));
        // The displayed version (ro.build.product2) is not the checked property.
        assertFalse(ioniq6Ceev(19, device, "CEEV.USA.S5W_M.V015.012.260507", framework, service));
        assertFalse(ioniq6Ceev(19, device, PreconditioningProfile.EV6_SOFTWARE, framework, service));
        assertFalse(ioniq6Ceev(19, device, software,
            PreconditioningProfile.EV6_FRAMEWORK_SHA256, PreconditioningProfile.EV6_SERVICE_SHA256));
        assertFalse(ioniq6Ceev(19, device, software,
            PreconditioningProfile.IONIQ5_NEEV23_US_FRAMEWORK_SHA256, PreconditioningProfile.IONIQ5_NEEV23_US_SERVICE_SHA256));
        assertFalse(ioniq6Ceev(19, device, software, "unknown", service));
        assertFalse(ioniq6Ceev(19, device, software, framework, "unknown"));
        assertFalse(ioniq6Ceev(19, device, null, framework, service));
        assertFalse(ioniq6Ceev(19, device, software, null, null));
    }
    @Test public void ioniq6CeevUsFirmwareDoesNotChangeTheOtherProfiles() {
        String software = PreconditioningProfile.IONIQ6_CEEV_US_SOFTWARE;
        String framework = PreconditioningProfile.IONIQ6_CEEV_US_FRAMEWORK_SHA256, service = PreconditioningProfile.IONIQ6_CEEV_US_SERVICE_SHA256;
        assertFalse(ioniq6Ceev(19, PreconditioningProfile.EV6_DEVICE, software, framework, service));
        assertFalse(ioniq6Ceev(19, PreconditioningProfile.EV6_DEVICE, PreconditioningProfile.EV6_SOFTWARE, framework, service));
        assertFalse(ioniq6Ceev(19, PreconditioningProfile.IONIQ5_NEEV_US_DEVICE, software, framework, service));
        assertFalse(ioniq6Ceev(19, PreconditioningProfile.IONIQ5_NEEV23_US_DEVICE, software, framework, service));
        assertTrue(ioniq6Ceev(19, PreconditioningProfile.EV6_DEVICE, PreconditioningProfile.EV6_SOFTWARE,
            PreconditioningProfile.EV6_FRAMEWORK_SHA256, PreconditioningProfile.EV6_SERVICE_SHA256));
        assertTrue(ioniq6Ceev(19, PreconditioningProfile.IONIQ5_NEEV_US_DEVICE, PreconditioningProfile.IONIQ5_NEEV_US_SOFTWARE,
            PreconditioningProfile.IONIQ5_NEEV23_US_FRAMEWORK_SHA256, PreconditioningProfile.IONIQ5_NEEV23_US_SERVICE_SHA256));
        assertTrue(ioniq6Ceev(19, PreconditioningProfile.IONIQ5_NEEV23_US_DEVICE, "Ioniq 5 NEEV23 US software not constrained",
            PreconditioningProfile.IONIQ5_NEEV23_US_FRAMEWORK_SHA256, PreconditioningProfile.IONIQ5_NEEV23_US_SERVICE_SHA256));
    }
}
