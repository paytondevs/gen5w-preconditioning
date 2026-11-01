package com.paytondev.launchpad;

import org.junit.Test;
import static org.junit.Assert.*;

public class Ev6251120CompatibilityTest {
    private static boolean ev6(int sdk, String device, String software, String framework, String service) {
        return PreconditioningProfile.matchesFirmware(sdk, device, software, framework, service);
    }
    @Test public void exactCollectedEv6251120FirmwareMatches() {
        assertTrue(PreconditioningProfile.supportsDevice(19, "wp_daudioplus_cvev_us"));
        assertTrue(ev6(19, "wp_daudioplus_cvev_us", "CVEV.USA.0000.V014.011.251120",
            "6919d451f299c1bf8e3d49aead4fcdcd60e055a32cf107d224e56e5cdd5072c0",
            "8a3523882752935a2d607101794a0037e1bfda1d6df0082e61ed71706778e41f"));
        // Same device as EV6 260507, so the EV6 five minutes and 20°C apply.
        assertEquals(5 * 60 * 1000L, PreconditioningProfile.activationTimeoutMs("wp_daudioplus_cvev_us"));
        assertEquals(20, PreconditioningProfile.warmThresholdC("wp_daudioplus_cvev_us"));
    }
    @Test public void mismatchedApiDeviceSoftwareOrEitherHashCannotEnableEv6251120() {
        String device = PreconditioningProfile.EV6_DEVICE, software = PreconditioningProfile.EV6_251120_SOFTWARE;
        String framework = PreconditioningProfile.EV6_251120_FRAMEWORK_SHA256,
            service = PreconditioningProfile.EV6_251120_SERVICE_SHA256;
        assertFalse(ev6(20, device, software, framework, service));
        assertFalse(ev6(19, "wp_daudioplus_cvev_us_knob", software, framework, service));
        assertFalse(ev6(19, PreconditioningProfile.IONIQ5_NEEV23_US_DEVICE, software, framework, service));
        assertFalse(ev6(19, PreconditioningProfile.IONIQ5_NEEV_US_DEVICE, software, framework, service));
        assertFalse(ev6(19, PreconditioningProfile.IONIQ6_CEEV_US_DEVICE, software, framework, service));
        assertFalse(ev6(19, PreconditioningProfile.IONIQ5_NEEV_KR_DEVICE, software, framework, service));
        assertFalse(ev6(19, null, software, framework, service));
        assertFalse(ev6(19, device, "CVEV.USA.0000.V014.011.999999", framework, service));
        assertFalse(ev6(19, device, "CVEV.USA.S5W_M.V014.011.251120", framework, service));
        assertFalse(ev6(19, device, "CV_E.USA.S5W_M.V014.011.251120", framework, service));
        assertFalse(ev6(19, device, "", framework, service));
        assertFalse(ev6(19, device, null, framework, service));
        assertFalse(ev6(19, device, software, "unknown", service));
        assertFalse(ev6(19, device, software, framework, "unknown"));
        assertFalse(ev6(19, device, software, null, null));
        // Each hash pair only counts together: no mixing with the EV6 260507 files.
        assertFalse(ev6(19, device, software, framework, PreconditioningProfile.EV6_SERVICE_SHA256));
        assertFalse(ev6(19, device, software, PreconditioningProfile.EV6_FRAMEWORK_SHA256, service));
        // The 251120 software doesn't unlock the 260507 files either.
        assertFalse(ev6(19, device, software,
            PreconditioningProfile.EV6_FRAMEWORK_SHA256, PreconditioningProfile.EV6_SERVICE_SHA256));
    }
    @Test public void otherIdentitiesCannotUseTheEv6251120Files() {
        String framework = PreconditioningProfile.EV6_251120_FRAMEWORK_SHA256,
            service = PreconditioningProfile.EV6_251120_SERVICE_SHA256;
        // Same device on its 260507 release, or any other software.
        assertFalse(ev6(19, PreconditioningProfile.EV6_DEVICE, PreconditioningProfile.EV6_SOFTWARE, framework, service));
        assertFalse(ev6(19, PreconditioningProfile.EV6_DEVICE, PreconditioningProfile.IONIQ6_CEEV_US_SOFTWARE, framework, service));
        // Other head units on their own exact identities.
        assertFalse(ev6(19, PreconditioningProfile.IONIQ5_NEEV23_US_DEVICE, PreconditioningProfile.IONIQ5_NEEV23_US_SOFTWARE,
            framework, service));
        assertFalse(ev6(19, PreconditioningProfile.IONIQ5_NEEV_US_DEVICE, PreconditioningProfile.IONIQ5_NEEV_US_SOFTWARE,
            framework, service));
        assertFalse(ev6(19, PreconditioningProfile.IONIQ6_CEEV_US_DEVICE, PreconditioningProfile.IONIQ6_CEEV_US_SOFTWARE,
            framework, service));
        assertFalse(ev6(19, PreconditioningProfile.IONIQ5_NEEV_KR_DEVICE, PreconditioningProfile.IONIQ5_NEEV_KR_SOFTWARE,
            framework, service));
    }
    @Test public void ev6251120DoesNotChangeTheOtherProfiles() {
        // The EV6 260507 profile still matches its own software and hashes.
        assertTrue(ev6(19, PreconditioningProfile.EV6_DEVICE, PreconditioningProfile.EV6_SOFTWARE,
            PreconditioningProfile.EV6_FRAMEWORK_SHA256, PreconditioningProfile.EV6_SERVICE_SHA256));
        assertTrue(ev6(19, PreconditioningProfile.IONIQ5_NEEV23_US_DEVICE, PreconditioningProfile.IONIQ5_NEEV23_US_SOFTWARE,
            PreconditioningProfile.IONIQ5_NEEV23_US_FRAMEWORK_SHA256, PreconditioningProfile.IONIQ5_NEEV23_US_SERVICE_SHA256));
        assertTrue(ev6(19, PreconditioningProfile.IONIQ5_NEEV_US_DEVICE, PreconditioningProfile.IONIQ5_NEEV_US_SOFTWARE,
            PreconditioningProfile.IONIQ5_NEEV23_US_FRAMEWORK_SHA256, PreconditioningProfile.IONIQ5_NEEV23_US_SERVICE_SHA256));
        assertTrue(ev6(19, PreconditioningProfile.IONIQ6_CEEV_US_DEVICE, PreconditioningProfile.IONIQ6_CEEV_US_SOFTWARE,
            PreconditioningProfile.IONIQ6_CEEV_US_FRAMEWORK_SHA256, PreconditioningProfile.IONIQ6_CEEV_US_SERVICE_SHA256));
        assertTrue(ev6(19, PreconditioningProfile.IONIQ5_NEEV_KR_DEVICE, PreconditioningProfile.IONIQ5_NEEV_KR_SOFTWARE,
            PreconditioningProfile.IONIQ5_NEEV_KR_FRAMEWORK_SHA256, PreconditioningProfile.IONIQ5_NEEV_KR_SERVICE_SHA256));
        // The EV6 260507 software is not accepted with another profile's hashes.
        assertFalse(ev6(19, PreconditioningProfile.EV6_DEVICE, PreconditioningProfile.EV6_SOFTWARE,
            PreconditioningProfile.IONIQ5_NEEV23_US_FRAMEWORK_SHA256, PreconditioningProfile.IONIQ5_NEEV23_US_SERVICE_SHA256));
    }
}
