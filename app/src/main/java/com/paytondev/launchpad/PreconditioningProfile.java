package com.paytondev.launchpad;

/** Firmware-specific OEM command profile. Evidence: verification/firmware-profile.md. */
final class PreconditioningProfile {
    // Both NEEV profiles are Ioniq 5 head units. NEEV23 US: model year unconfirmed.
    static final String IONIQ5_NEEV23_US_DEVICE = "wp_daudioplus_neev23_us";
    // NEEV23 US 260507: the NEEV US 260507 string with NEEV23 in place of NEEV.
    static final String IONIQ5_NEEV23_US_SOFTWARE = "NEEV23.USA.0000.V015.012.260507";
    static final String IONIQ5_NEEV23_US_FRAMEWORK_SHA256 = "dd72e04bce0a2b4bda1506308ac21f91002c1436553b8156c345cc4313661338";
    static final String IONIQ5_NEEV23_US_SERVICE_SHA256 = "3cf40fb00854c63afa49abea1dad18a0afcbc0dd68b17de9ed7ea3f5aeab4222";
    // NEEV23 US 260206 (US 2024 Ioniq 5): same device, older software with its own
    // framework files; the porting kit report of 2026-10-29 showed the same transactions.
    static final String IONIQ5_NEEV23_US_260206_SOFTWARE = "NEEV23.USA.0000.V015.011.260206";
    static final String IONIQ5_NEEV23_US_260206_FRAMEWORK_SHA256 = "301b134d231b1ca1d0aa75ae090d219ac963fc935c3f3bc0b3b1393c6614703e";
    static final String IONIQ5_NEEV23_US_260206_SERVICE_SHA256 = "efb949ec92fff388cbbb78e5bf4e16df3cc8d417dbf5cb5e87e62fb46d542549";
    // NEEV23 US 260903 (US 2024 Ioniq 5): same device, newer software with its own
    // framework files; porting kit reports of 2026-10-29 and 2026-10-30 from two cars
    // showed identical files and the same transactions.
    static final String IONIQ5_NEEV23_US_260903_SOFTWARE = "NEEV23.USA.0000.V016.010.260903";
    static final String IONIQ5_NEEV23_US_260903_FRAMEWORK_SHA256 = "c52e0825956880d627b32979022705a618200f059476c094d729133d1ff6a3be";
    static final String IONIQ5_NEEV23_US_260903_SERVICE_SHA256 = "bf17e682d1beb349ae8ef2acaae6d6e908fe42e88853d7145ee16b29bb4d9ec1";
    // NEEV US 260507 (US 2022 Ioniq 5): automotive.odex and automotive-service.odex
    // captured on 2026-10-27 are byte-identical to NEEV23 US, so it reuses those hashes.
    static final String IONIQ5_NEEV_US_DEVICE = "wp_daudioplus_neev_us";
    static final String IONIQ5_NEEV_US_SOFTWARE = "NEEV.USA.0000.V015.012.260507";
    // NEEV US 251120 (US 2024 Ioniq 5 SEL AWD): automotive.odex and automotive-service.odex
    // captured on 2026-11-01. 
    static final String IONIQ5_NEEV23_US_251120_SOFTWARE = "NEEV23.USA.0000.V014.011.251120";
    static final String IONIQ5_NEEV23_US_251120_FRAMEWORK_SHA256 = "adf81f0967b6ea400bea54b70ea0bba65bce98b10da19f9765ee8fe80efe17c1";
    static final String IONIQ5_NEEV23_US_251120_SERVICE_SHA256 = "f6dd0c443217ac2f15437d9843f4cff53b63d9aa74e88b28ca0f934dd68d22ab";
    // NEEV US 240822 (US 2021 Ioniq 5 AWD): same device, older software with its own
    // framework files; the porting kit report of 2026-10-31 showed the same transactions.
    static final String IONIQ5_NEEV_US_240822_SOFTWARE = "NEEV.USA.0000.V012.010.240822";
    static final String IONIQ5_NEEV_US_240822_FRAMEWORK_SHA256 = "0f6786b9c8c167f09c08c559afea7da1e2137e2a1968b9ab372def65762674f9";
    static final String IONIQ5_NEEV_US_240822_SERVICE_SHA256 = "2758a413c184855bf92080cfdc6decbfcd6f09775128a982b1eee211993772a1";
    // NEEV US 260903 (US 2022 Ioniq 5 SEL AWD): the porting kit report of 2026-11-01 found
    // both framework files byte-identical to NEEV23 US 260903, so it reuses those hashes.
    static final String IONIQ5_NEEV_US_260903_SOFTWARE = "NEEV.USA.0000.V016.010.260903";
    static final String EV6_DEVICE = "wp_daudioplus_cvev_us";
    static final String EV6_SOFTWARE = "CVEV.USA.0000.V015.012.260507";
    static final String EV6_FRAMEWORK_SHA256 = "b9e7e02b81e1a8cfcf767c6b8bbb28bcf1d2c411a208fbf6a465e7f0c662774a";
    static final String EV6_SERVICE_SHA256 = "8af745b41aa28f490953d61531215cd94e059f104b31f6236a8f6c7af23b8062";
    // EV6 US 251120 (US 2023 EV6 Wind): same device, older software with its own
    // framework files; the porting kit report of 2026-10-30 showed the same transactions.
    static final String EV6_251120_SOFTWARE = "CVEV.USA.0000.V014.011.251120";
    static final String EV6_251120_FRAMEWORK_SHA256 = "6919d451f299c1bf8e3d49aead4fcdcd60e055a32cf107d224e56e5cdd5072c0";
    static final String EV6_251120_SERVICE_SHA256 = "8a3523882752935a2d607101794a0037e1bfda1d6df0082e61ed71706778e41f";
    // CEEV US 260507 (US 2023 Ioniq 6 SEL): own firmware files, captured on 2026-10-29;
    // the five HEV/BCM transactions and the cluster contract match the other profiles.
    static final String IONIQ6_CEEV_US_DEVICE = "wp_daudioplus_ceev_us";
    static final String IONIQ6_CEEV_US_SOFTWARE = "CEEV.USA.0000.V015.012.260507";
    static final String IONIQ6_CEEV_US_FRAMEWORK_SHA256 = "408b426e14b3f8dfdf1926e608e4f02689e89fc386b7f93a97f261ac5b6e4287";
    static final String IONIQ6_CEEV_US_SERVICE_SHA256 = "afbc60509dd8a8fbac842523bf618424c088685d832a125a84d1d11dc1770bd4";
    // NEEV KR 260902 (KR 2022 Ioniq 5 Prestige): own firmware files, captured on 2026-10-30;
    // the five HEV/BCM transactions and the cluster contract match the other profiles.
    static final String IONIQ5_NEEV_KR_DEVICE = "wp_daudioplus_neev_kr";
    static final String IONIQ5_NEEV_KR_SOFTWARE = "NEEV.KOR.0000.V039.001.260902";
    static final String IONIQ5_NEEV_KR_FRAMEWORK_SHA256 = "8186fec9601a216e8e3015b77c13b4d18c2fd2dfc2d3a6b7e42dac55b7a22c06";
    static final String IONIQ5_NEEV_KR_SERVICE_SHA256 = "176e2375ef9e2c8b1df16b600f0434df7e2ce7bb7378779e7180e5c23651f80d";
    // CVEV KR 260902 (KR 2022 EV6 Long Range AWD Earth): own device and firmware files,
    // captured on 2026-11-01; the five HEV/BCM transactions and the cluster contract match
    // the other profiles.
    static final String EV6_CVEV_KR_DEVICE = "wp_daudioplus_cvev_kr";
    static final String EV6_CVEV_KR_SOFTWARE = "CVEV.KOR.0000.V039.001.260902";
    static final String EV6_CVEV_KR_FRAMEWORK_SHA256 = "e9ff63f7b456327b9a1c683dc2bacf4b30679bb8ff08279799918233ad1de25d";
    static final String EV6_CVEV_KR_SERVICE_SHA256 = "f830984e0401d24156a7101bb6ec03896ce1a70ff7ec732ad9e65072d8b717eb";

    static boolean supportsDevice(int sdk, String device) {
        return sdk == 19 && (IONIQ5_NEEV23_US_DEVICE.equals(device) || IONIQ5_NEEV_US_DEVICE.equals(device) || EV6_DEVICE.equals(device)
            || IONIQ6_CEEV_US_DEVICE.equals(device) || IONIQ5_NEEV_KR_DEVICE.equals(device)
            || EV6_CVEV_KR_DEVICE.equals(device));
    }
    // Cancel an unconfirmed request after this long. EV6: five minutes, field-validated.
    // Ioniq 5: activation normally follows in about 60–70 seconds, so two minutes.
    // Ioniq 6 and every newer port use the Ioniq 5 two minutes.
    // The timeout and warm threshold are keyed on device, so EV6 251120 (same device)
    // gets the EV6's five minutes and 20°C, and NEEV US 240822 gets the Ioniq 5 two
    // minutes and 21°C. The EV6 CVEV KR is its own device, so as a new port it gets
    // the two minutes and 21°C.
    static final long IONIQ5_ACTIVATION_TIMEOUT_MS = 2 * 60 * 1000L;
    static long activationTimeoutMs(String device) {
        return EV6_DEVICE.equals(device) ? PreconditioningController.ACTIVATION_TIMEOUT_MS : IONIQ5_ACTIVATION_TIMEOUT_MS;
    }
    // Lowest pack reading at which the car is expected not to heat. Ioniq 5 (NEEV23 US
    // and NEEV US): the owner reports a 70°F cutoff; readings are whole °C, so 21°C.
    // The NEEV KR Ioniq 5 uses the same. EV6: unconfirmed, so it keeps 20°C / 68°F.
    // Ioniq 6 uses the same.
    static final int IONIQ5_WARM_THRESHOLD_C = 21;
    static int warmThresholdC(String device) {
        return EV6_DEVICE.equals(device) || IONIQ6_CEEV_US_DEVICE.equals(device)
            ? BatteryTemperature.EXPECTED_HEATING_THRESHOLD_C : IONIQ5_WARM_THRESHOLD_C;
    }
    static boolean matchesFirmware(int sdk, String device, String software,
                                   String frameworkHash, String serviceHash) {
        if (!supportsDevice(sdk, device)) return false;
        if (IONIQ5_NEEV23_US_DEVICE.equals(device)) {
            if (IONIQ5_NEEV23_US_260206_SOFTWARE.equals(software)
                && IONIQ5_NEEV23_US_260206_FRAMEWORK_SHA256.equals(frameworkHash)
                && IONIQ5_NEEV23_US_260206_SERVICE_SHA256.equals(serviceHash)) return true;
            if (IONIQ5_NEEV23_US_260903_SOFTWARE.equals(software)
                && IONIQ5_NEEV23_US_260903_FRAMEWORK_SHA256.equals(frameworkHash)
                && IONIQ5_NEEV23_US_260903_SERVICE_SHA256.equals(serviceHash)) return true;
            if (IONIQ5_NEEV23_US_251120_SOFTWARE.equals(software)
                && IONIQ5_NEEV23_US_251120_FRAMEWORK_SHA256.equals(frameworkHash)
                && IONIQ5_NEEV23_US_251120_SERVICE_SHA256.equals(serviceHash)) return true;
            return IONIQ5_NEEV23_US_SOFTWARE.equals(software) && IONIQ5_NEEV23_US_FRAMEWORK_SHA256.equals(frameworkHash)
                && IONIQ5_NEEV23_US_SERVICE_SHA256.equals(serviceHash);
        }
        if (IONIQ5_NEEV_US_DEVICE.equals(device)) {
            if (IONIQ5_NEEV_US_240822_SOFTWARE.equals(software)
                && IONIQ5_NEEV_US_240822_FRAMEWORK_SHA256.equals(frameworkHash)
                && IONIQ5_NEEV_US_240822_SERVICE_SHA256.equals(serviceHash)) return true;
            if (IONIQ5_NEEV_US_260903_SOFTWARE.equals(software)
                && IONIQ5_NEEV23_US_260903_FRAMEWORK_SHA256.equals(frameworkHash)
                && IONIQ5_NEEV23_US_260903_SERVICE_SHA256.equals(serviceHash)) return true;
            return IONIQ5_NEEV_US_SOFTWARE.equals(software) && IONIQ5_NEEV23_US_FRAMEWORK_SHA256.equals(frameworkHash)
                && IONIQ5_NEEV23_US_SERVICE_SHA256.equals(serviceHash);
        }
        if (IONIQ6_CEEV_US_DEVICE.equals(device)) {
            return IONIQ6_CEEV_US_SOFTWARE.equals(software) && IONIQ6_CEEV_US_FRAMEWORK_SHA256.equals(frameworkHash)
                && IONIQ6_CEEV_US_SERVICE_SHA256.equals(serviceHash);
        }
        if (IONIQ5_NEEV_KR_DEVICE.equals(device)) {
            return IONIQ5_NEEV_KR_SOFTWARE.equals(software) && IONIQ5_NEEV_KR_FRAMEWORK_SHA256.equals(frameworkHash)
                && IONIQ5_NEEV_KR_SERVICE_SHA256.equals(serviceHash);
        }
        if (EV6_CVEV_KR_DEVICE.equals(device)) {
            return EV6_CVEV_KR_SOFTWARE.equals(software) && EV6_CVEV_KR_FRAMEWORK_SHA256.equals(frameworkHash)
                && EV6_CVEV_KR_SERVICE_SHA256.equals(serviceHash);
        }
        if (EV6_251120_SOFTWARE.equals(software)
            && EV6_251120_FRAMEWORK_SHA256.equals(frameworkHash)
            && EV6_251120_SERVICE_SHA256.equals(serviceHash)) return true;
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
