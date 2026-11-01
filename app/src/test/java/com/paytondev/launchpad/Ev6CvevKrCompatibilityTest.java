package com.paytondev.launchpad;

import org.junit.Test;
import static org.junit.Assert.*;

public class Ev6CvevKrCompatibilityTest {
    private static boolean ev6CvevKr(int sdk, String device, String software, String framework, String service) {
        return PreconditioningProfile.matchesFirmware(sdk, device, software, framework, service);
    }
    @Test public void exactCollectedEv6CvevKrFirmwareMatches() {
        assertTrue(PreconditioningProfile.supportsDevice(19, "wp_daudioplus_cvev_kr"));
        assertTrue(ev6CvevKr(19, "wp_daudioplus_cvev_kr", "CVEV.KOR.0000.V039.001.260902",
            "e9ff63f7b456327b9a1c683dc2bacf4b30679bb8ff08279799918233ad1de25d",
            "f830984e0401d24156a7101bb6ec03896ce1a70ff7ec732ad9e65072d8b717eb"));
        // Its own device, outside the EV6 US branches: the two minutes every new port uses, and 21°C.
        assertEquals(2 * 60 * 1000L, PreconditioningProfile.activationTimeoutMs("wp_daudioplus_cvev_kr"));
        assertEquals(21, PreconditioningProfile.warmThresholdC("wp_daudioplus_cvev_kr"));
    }
    @Test public void mismatchedApiDeviceSoftwareOrEitherHashCannotEnableEv6CvevKr() {
        String device = PreconditioningProfile.EV6_CVEV_KR_DEVICE, software = PreconditioningProfile.EV6_CVEV_KR_SOFTWARE;
        String framework = PreconditioningProfile.EV6_CVEV_KR_FRAMEWORK_SHA256, service = PreconditioningProfile.EV6_CVEV_KR_SERVICE_SHA256;
        assertFalse(PreconditioningProfile.supportsDevice(20, device));
        assertFalse(ev6CvevKr(20, device, software, framework, service));
        assertFalse(ev6CvevKr(19, "wp_daudioplus_cvev_kr_knob", software, framework, service));
        assertFalse(ev6CvevKr(19, "wp_daudioplus_ecokmc_kr", software, framework, service));
        assertFalse(ev6CvevKr(19, PreconditioningProfile.EV6_DEVICE, software, framework, service));
        assertFalse(ev6CvevKr(19, PreconditioningProfile.IONIQ5_NEEV_KR_DEVICE, software, framework, service));
        assertFalse(ev6CvevKr(19, null, software, framework, service));
        assertFalse(ev6CvevKr(19, device, "CVEV.KOR.0000.V039.001.999999", framework, service));
        // The displayed versions (ro.build.product2 and ro.build.product3) are not the checked property.
        assertFalse(ev6CvevKr(19, device, "CVEV.KOR.S5W_M.V039.001.260902", framework, service));
        assertFalse(ev6CvevKr(19, device, "CV_E.KOR.S5W_M.V039.001.260902", framework, service));
        assertFalse(ev6CvevKr(19, device, PreconditioningProfile.EV6_SOFTWARE, framework, service));
        assertFalse(ev6CvevKr(19, device, PreconditioningProfile.IONIQ5_NEEV_KR_SOFTWARE, framework, service));
        assertFalse(ev6CvevKr(19, device, "", framework, service));
        assertFalse(ev6CvevKr(19, device, null, framework, service));
        assertFalse(ev6CvevKr(19, device, software,
            PreconditioningProfile.EV6_FRAMEWORK_SHA256, PreconditioningProfile.EV6_SERVICE_SHA256));
        assertFalse(ev6CvevKr(19, device, software,
            PreconditioningProfile.EV6_251120_FRAMEWORK_SHA256, PreconditioningProfile.EV6_251120_SERVICE_SHA256));
        assertFalse(ev6CvevKr(19, device, software,
            PreconditioningProfile.IONIQ5_NEEV_KR_FRAMEWORK_SHA256, PreconditioningProfile.IONIQ5_NEEV_KR_SERVICE_SHA256));
        assertFalse(ev6CvevKr(19, device, software, "unknown", service));
        assertFalse(ev6CvevKr(19, device, software, framework, "unknown"));
        assertFalse(ev6CvevKr(19, device, software, null, null));
        // Each hash pair only counts together: no mixing with another profile's files.
        assertFalse(ev6CvevKr(19, device, software, framework, PreconditioningProfile.EV6_SERVICE_SHA256));
        assertFalse(ev6CvevKr(19, device, software, PreconditioningProfile.EV6_FRAMEWORK_SHA256, service));
        assertFalse(ev6CvevKr(19, device, software, framework, PreconditioningProfile.IONIQ5_NEEV_KR_SERVICE_SHA256));
        assertFalse(ev6CvevKr(19, device, software, PreconditioningProfile.IONIQ5_NEEV_KR_FRAMEWORK_SHA256, service));
    }
    @Test public void ev6CvevKrFirmwareDoesNotChangeTheOtherProfiles() {
        String software = PreconditioningProfile.EV6_CVEV_KR_SOFTWARE;
        String framework = PreconditioningProfile.EV6_CVEV_KR_FRAMEWORK_SHA256, service = PreconditioningProfile.EV6_CVEV_KR_SERVICE_SHA256;
        // Other head units can't use the EV6 CVEV KR files, with its software or their own.
        assertFalse(ev6CvevKr(19, PreconditioningProfile.EV6_DEVICE, PreconditioningProfile.EV6_SOFTWARE, framework, service));
        assertFalse(ev6CvevKr(19, PreconditioningProfile.EV6_DEVICE, PreconditioningProfile.EV6_251120_SOFTWARE, framework, service));
        assertFalse(ev6CvevKr(19, PreconditioningProfile.IONIQ5_NEEV_KR_DEVICE, software, framework, service));
        assertFalse(ev6CvevKr(19, PreconditioningProfile.IONIQ5_NEEV_KR_DEVICE, PreconditioningProfile.IONIQ5_NEEV_KR_SOFTWARE,
            framework, service));
        assertFalse(ev6CvevKr(19, PreconditioningProfile.IONIQ5_NEEV23_US_DEVICE, software, framework, service));
        assertFalse(ev6CvevKr(19, PreconditioningProfile.IONIQ5_NEEV_US_DEVICE, software, framework, service));
        assertFalse(ev6CvevKr(19, PreconditioningProfile.IONIQ6_CEEV_US_DEVICE, software, framework, service));
        // The existing profiles still match their own identities.
        assertTrue(ev6CvevKr(19, PreconditioningProfile.EV6_DEVICE, PreconditioningProfile.EV6_SOFTWARE,
            PreconditioningProfile.EV6_FRAMEWORK_SHA256, PreconditioningProfile.EV6_SERVICE_SHA256));
        assertTrue(ev6CvevKr(19, PreconditioningProfile.EV6_DEVICE, PreconditioningProfile.EV6_251120_SOFTWARE,
            PreconditioningProfile.EV6_251120_FRAMEWORK_SHA256, PreconditioningProfile.EV6_251120_SERVICE_SHA256));
        assertTrue(ev6CvevKr(19, PreconditioningProfile.IONIQ5_NEEV_KR_DEVICE, PreconditioningProfile.IONIQ5_NEEV_KR_SOFTWARE,
            PreconditioningProfile.IONIQ5_NEEV_KR_FRAMEWORK_SHA256, PreconditioningProfile.IONIQ5_NEEV_KR_SERVICE_SHA256));
        assertTrue(ev6CvevKr(19, PreconditioningProfile.IONIQ5_NEEV_US_DEVICE, PreconditioningProfile.IONIQ5_NEEV_US_SOFTWARE,
            PreconditioningProfile.IONIQ5_NEEV23_US_FRAMEWORK_SHA256, PreconditioningProfile.IONIQ5_NEEV23_US_SERVICE_SHA256));
        assertTrue(ev6CvevKr(19, PreconditioningProfile.IONIQ5_NEEV23_US_DEVICE, PreconditioningProfile.IONIQ5_NEEV23_US_SOFTWARE,
            PreconditioningProfile.IONIQ5_NEEV23_US_FRAMEWORK_SHA256, PreconditioningProfile.IONIQ5_NEEV23_US_SERVICE_SHA256));
        assertTrue(ev6CvevKr(19, PreconditioningProfile.IONIQ6_CEEV_US_DEVICE, PreconditioningProfile.IONIQ6_CEEV_US_SOFTWARE,
            PreconditioningProfile.IONIQ6_CEEV_US_FRAMEWORK_SHA256, PreconditioningProfile.IONIQ6_CEEV_US_SERVICE_SHA256));
        // The EV6 US device keeps its five minutes and 20°C.
        assertEquals(5 * 60 * 1000L, PreconditioningProfile.activationTimeoutMs(PreconditioningProfile.EV6_DEVICE));
        assertEquals(20, PreconditioningProfile.warmThresholdC(PreconditioningProfile.EV6_DEVICE));
    }
}
