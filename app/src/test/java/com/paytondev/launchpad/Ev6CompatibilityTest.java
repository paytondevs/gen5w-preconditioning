package com.paytondev.launchpad;

import org.junit.Test;
import static org.junit.Assert.*;

public class Ev6CompatibilityTest {
    private static boolean ev6(int sdk, String device, String software, String framework, String service) {
        return PreconditioningProfile.matchesFirmware(sdk, device, software, framework, service);
    }
    @Test public void exactCollectedEv6FirmwareMatches() {
        assertTrue(ev6(19, "wp_daudioplus_cvev_us", "CVEV.USA.0000.V015.012.260507",
            "b9e7e02b81e1a8cfcf767c6b8bbb28bcf1d2c411a208fbf6a465e7f0c662774a",
            "8af745b41aa28f490953d61531215cd94e059f104b31f6236a8f6c7af23b8062"));
    }
    @Test public void ioniq5Neev23UsProfileRemainsExact() {
        assertTrue(ev6(19, PreconditioningProfile.IONIQ5_NEEV23_US_DEVICE, "Ioniq 5 NEEV23 US software not constrained",
            PreconditioningProfile.IONIQ5_NEEV23_US_FRAMEWORK_SHA256, PreconditioningProfile.IONIQ5_NEEV23_US_SERVICE_SHA256));
        assertFalse(ev6(19, PreconditioningProfile.IONIQ5_NEEV23_US_DEVICE, PreconditioningProfile.EV6_SOFTWARE,
            PreconditioningProfile.EV6_FRAMEWORK_SHA256, PreconditioningProfile.EV6_SERVICE_SHA256));
    }
    @Test public void mismatchedDeviceApiProductOrEitherHashCannotEnableEv6() {
        String device = PreconditioningProfile.EV6_DEVICE, software = PreconditioningProfile.EV6_SOFTWARE;
        String framework = PreconditioningProfile.EV6_FRAMEWORK_SHA256, service = PreconditioningProfile.EV6_SERVICE_SHA256;
        assertFalse(ev6(20, device, software, framework, service));
        assertFalse(ev6(19, "another_ev6", software, framework, service));
        assertFalse(ev6(19, device, "CVEV.USA.0000.V015.012.999999", framework, service));
        assertFalse(ev6(19, device, software, "unknown", service));
        assertFalse(ev6(19, device, software, framework, "unknown"));
        assertFalse(ev6(19, device, software, PreconditioningProfile.IONIQ5_NEEV23_US_FRAMEWORK_SHA256, PreconditioningProfile.IONIQ5_NEEV23_US_SERVICE_SHA256));
        assertFalse(ev6(19, device, null, framework, service));
        assertFalse(ev6(19, null, software, framework, service));
        assertFalse(ev6(19, device, software, null, null));
    }
}
