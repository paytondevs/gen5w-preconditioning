package com.paytondev.launchpad;

import org.junit.Test;
import static org.junit.Assert.*;

public class Ioniq5Neev23Us260507CompatibilityTest {
    private static boolean neev23(int sdk, String device, String software, String framework, String service) {
        return PreconditioningProfile.matchesFirmware(sdk, device, software, framework, service);
    }
    @Test public void exactIoniq5Neev23Us260507FirmwareMatches() {
        assertTrue(neev23(19, "wp_daudioplus_neev23_us", "NEEV23.USA.0000.V015.012.260507",
            "dd72e04bce0a2b4bda1506308ac21f91002c1436553b8156c345cc4313661338",
            "3cf40fb00854c63afa49abea1dad18a0afcbc0dd68b17de9ed7ea3f5aeab4222"));
    }
    @Test public void otherSoftwareCannotEnableIoniq5Neev23Us260507() {
        String device = PreconditioningProfile.IONIQ5_NEEV23_US_DEVICE;
        String framework = PreconditioningProfile.IONIQ5_NEEV23_US_FRAMEWORK_SHA256, service = PreconditioningProfile.IONIQ5_NEEV23_US_SERVICE_SHA256;
        assertFalse(neev23(19, device, "NEEV23.USA.0000.V015.012.999999", framework, service));
        // The displayed version (ro.build.product2) is not the checked property.
        assertFalse(neev23(19, device, "NEEV23.USA.S5W_M.V015.012.260507", framework, service));
        // Same files as NEEV US 260507, but that software string belongs to the other device.
        assertFalse(neev23(19, device, PreconditioningProfile.IONIQ5_NEEV_US_SOFTWARE, framework, service));
        assertFalse(neev23(19, device, PreconditioningProfile.IONIQ5_NEEV23_US_260206_SOFTWARE, framework, service));
        assertFalse(neev23(19, device, PreconditioningProfile.IONIQ5_NEEV23_US_260903_SOFTWARE, framework, service));
        assertFalse(neev23(19, device, "", framework, service));
        assertFalse(neev23(19, device, null, framework, service));
        // The NEEV23 US 260507 string doesn't move to the NEEV US device either.
        assertFalse(neev23(19, PreconditioningProfile.IONIQ5_NEEV_US_DEVICE, PreconditioningProfile.IONIQ5_NEEV23_US_SOFTWARE,
            framework, service));
    }
}
