package com.paytondev.launchpad;

import org.junit.Test;
import static org.junit.Assert.*;

public class Ioniq5Neev23Us260206CompatibilityTest {
    private static boolean neev23(int sdk, String device, String software, String framework, String service) {
        return PreconditioningProfile.matchesFirmware(sdk, device, software, framework, service);
    }
    @Test public void exactCollectedIoniq5Neev23Us260206FirmwareMatches() {
        assertTrue(PreconditioningProfile.supportsDevice(19, "wp_daudioplus_neev23_us"));
        assertTrue(neev23(19, "wp_daudioplus_neev23_us", "NEEV23.USA.0000.V015.011.260206",
            "301b134d231b1ca1d0aa75ae090d219ac963fc935c3f3bc0b3b1393c6614703e",
            "efb949ec92fff388cbbb78e5bf4e16df3cc8d417dbf5cb5e87e62fb46d542549"));
    }
    @Test public void mismatchedApiDeviceSoftwareOrEitherHashCannotEnableIoniq5Neev23Us260206() {
        String device = PreconditioningProfile.IONIQ5_NEEV23_US_DEVICE, software = PreconditioningProfile.IONIQ5_NEEV23_US_260206_SOFTWARE;
        String framework = PreconditioningProfile.IONIQ5_NEEV23_US_260206_FRAMEWORK_SHA256,
            service = PreconditioningProfile.IONIQ5_NEEV23_US_260206_SERVICE_SHA256;
        assertFalse(neev23(20, device, software, framework, service));
        assertFalse(neev23(19, "wp_daudioplus_neev23_us_can", software, framework, service));
        assertFalse(neev23(19, PreconditioningProfile.IONIQ5_NEEV_US_DEVICE, software, framework, service));
        assertFalse(neev23(19, PreconditioningProfile.EV6_DEVICE, software, framework, service));
        assertFalse(neev23(19, device, "NEEV23.USA.0000.V015.011.999999", framework, service));
        assertFalse(neev23(19, device, "NEEV23.USA.S5W_M.V015.011.260206", framework, service));
        assertFalse(neev23(19, device, "", framework, service));
        assertFalse(neev23(19, device, null, framework, service));
        assertFalse(neev23(19, device, software, "unknown", service));
        assertFalse(neev23(19, device, software, framework, "unknown"));
        assertFalse(neev23(19, device, software, null, null));
        // Each hash pair only counts together: no mixing with the 260507 files.
        assertFalse(neev23(19, device, software, framework, PreconditioningProfile.IONIQ5_NEEV23_US_SERVICE_SHA256));
        assertFalse(neev23(19, device, software, PreconditioningProfile.IONIQ5_NEEV23_US_FRAMEWORK_SHA256, service));
    }
    @Test public void ioniq5Neev23Us260206DoesNotChangeTheOtherProfiles() {
        // The original NEEV23 US profile still matches its own hashes, with any software.
        assertTrue(neev23(19, PreconditioningProfile.IONIQ5_NEEV23_US_DEVICE, "Ioniq 5 NEEV23 US software not constrained",
            PreconditioningProfile.IONIQ5_NEEV23_US_FRAMEWORK_SHA256, PreconditioningProfile.IONIQ5_NEEV23_US_SERVICE_SHA256));
        assertTrue(neev23(19, PreconditioningProfile.IONIQ5_NEEV23_US_DEVICE, PreconditioningProfile.IONIQ5_NEEV23_US_260206_SOFTWARE,
            PreconditioningProfile.IONIQ5_NEEV23_US_FRAMEWORK_SHA256, PreconditioningProfile.IONIQ5_NEEV23_US_SERVICE_SHA256));
        assertFalse(neev23(19, PreconditioningProfile.IONIQ5_NEEV_US_DEVICE, PreconditioningProfile.IONIQ5_NEEV_US_SOFTWARE,
            PreconditioningProfile.IONIQ5_NEEV23_US_260206_FRAMEWORK_SHA256, PreconditioningProfile.IONIQ5_NEEV23_US_260206_SERVICE_SHA256));
        assertFalse(neev23(19, PreconditioningProfile.EV6_DEVICE, PreconditioningProfile.EV6_SOFTWARE,
            PreconditioningProfile.IONIQ5_NEEV23_US_260206_FRAMEWORK_SHA256, PreconditioningProfile.IONIQ5_NEEV23_US_260206_SERVICE_SHA256));
        assertTrue(neev23(19, PreconditioningProfile.EV6_DEVICE, PreconditioningProfile.EV6_SOFTWARE,
            PreconditioningProfile.EV6_FRAMEWORK_SHA256, PreconditioningProfile.EV6_SERVICE_SHA256));
    }
}
