package com.paytondev.launchpad;

/** Firmware-specific OEM command profile. Evidence: verification/firmware-profile.md. */
final class PreconditioningProfile {
    // Both NEEV profiles are Ioniq 5 head units. NEEV23 US: model year unconfirmed.
    static final String IONIQ5_NEEV23_US_DEVICE = "wp_daudioplus_neev23_us";
    static final String IONIQ5_NEEV23_US_FRAMEWORK_SHA256 = "dd72e04bce0a2b4bda1506308ac21f91002c1436553b8156c345cc4313661338";
    static final String IONIQ5_NEEV23_US_SERVICE_SHA256 = "3cf40fb00854c63afa49abea1dad18a0afcbc0dd68b17de9ed7ea3f5aeab4222";
    // NEEV US 260507 (US 2022 Ioniq 5): automotive.odex and automotive-service.odex
    // captured on 2026-10-27 are byte-identical to NEEV23 US, so it reuses those hashes.
    static final String IONIQ5_NEEV_US_DEVICE = "wp_daudioplus_neev_us";
    static final String IONIQ5_NEEV_US_SOFTWARE = "NEEV.USA.0000.V015.012.260507";
    static final String EV6_DEVICE = "wp_daudioplus_cvev_us";
    static final String EV6_SOFTWARE = "CVEV.USA.0000.V015.012.260507";
    static final String EV6_FRAMEWORK_SHA256 = "b9e7e02b81e1a8cfcf767c6b8bbb28bcf1d2c411a208fbf6a465e7f0c662774a";
    static final String EV6_SERVICE_SHA256 = "8af745b41aa28f490953d61531215cd94e059f104b31f6236a8f6c7af23b8062";

    static boolean supportsDevice(int sdk, String device) {
        return sdk == 19 && (IONIQ5_NEEV23_US_DEVICE.equals(device) || IONIQ5_NEEV_US_DEVICE.equals(device) || EV6_DEVICE.equals(device));
    }
    // Cancel an unconfirmed request after this long. EV6: five minutes, field-validated.
    // Ioniq 5: activation normally follows in about 60–70 seconds, so two minutes.
    static final long IONIQ5_ACTIVATION_TIMEOUT_MS = 2 * 60 * 1000L;
    static long activationTimeoutMs(String device) {
        return EV6_DEVICE.equals(device) ? PreconditioningController.ACTIVATION_TIMEOUT_MS : IONIQ5_ACTIVATION_TIMEOUT_MS;
    }
    // Lowest pack reading at which the car is expected not to heat. Ioniq 5 (NEEV23 US
    // and NEEV US): the owner reports a 70°F cutoff; readings are whole °C, so 21°C.
    // EV6: unconfirmed, so it keeps 20°C / 68°F.
    static final int IONIQ5_WARM_THRESHOLD_C = 21;
    static int warmThresholdC(String device) {
        return EV6_DEVICE.equals(device) ? BatteryTemperature.EXPECTED_HEATING_THRESHOLD_C : IONIQ5_WARM_THRESHOLD_C;
    }
    static boolean matchesFirmware(int sdk, String device, String software,
                                   String frameworkHash, String serviceHash) {
        if (!supportsDevice(sdk, device)) return false;
        if (IONIQ5_NEEV23_US_DEVICE.equals(device)) {
            return IONIQ5_NEEV23_US_FRAMEWORK_SHA256.equals(frameworkHash) && IONIQ5_NEEV23_US_SERVICE_SHA256.equals(serviceHash);
        }
        if (IONIQ5_NEEV_US_DEVICE.equals(device)) {
            return IONIQ5_NEEV_US_SOFTWARE.equals(software) && IONIQ5_NEEV23_US_FRAMEWORK_SHA256.equals(frameworkHash)
                && IONIQ5_NEEV23_US_SERVICE_SHA256.equals(serviceHash);
        }
        return EV6_SOFTWARE.equals(software) && EV6_FRAMEWORK_SHA256.equals(frameworkHash)
            && EV6_SERVICE_SHA256.equals(serviceHash);
    }
    static final long REFRESH_MS = 5000;
    interface Commands {
        void write(int transaction, int... values) throws Exception;
        void pause(long milliseconds);
    }
    static void start(Commands commands) throws Exception {
        metadata(commands, 9, 18);
        // Stock FrameEthControl sends route metadata before the station event.
        commands.pause(10);
        // HEVService schedules Invalid (7,7) at 120 ms. Serialize a fast Stop
        // behind that pulse so the old timer cannot shorten the Cancel pulse.
        try { commands.write(62, 2, 3); } finally { commands.pause(130); }
    }
    static void maintain(Commands commands) throws Exception {
        metadata(commands, 9, 18); // Refresh metadata only; never repeat Start.
    }
    static void cancel(Commands commands) throws Exception {
        Exception failure = null;
        // Attempt every cancellation component even when an earlier call fails.
        try { commands.write(65, 2047); } catch (Exception e) { failure = e; }
        try { commands.write(66, 1023); } catch (Exception e) { if (failure == null) failure = e; }
        try { commands.write(62, 0, 0); } catch (Exception e) { if (failure == null) failure = e; }
        commands.pause(130);
        if (failure != null) throw failure;
    }
    private static void metadata(Commands commands, int distanceKm, int remainingMinutes) throws Exception {
        commands.write(65, distanceKm); commands.write(66, remainingMinutes);
    }
    private PreconditioningProfile() { }
}
