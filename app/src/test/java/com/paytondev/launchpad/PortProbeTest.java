package com.paytondev.launchpad;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class PortProbeTest {
    /** Shaped like an AIDL Stub: static int TRANSACTION_* codes alongside other fields. */
    @SuppressWarnings("unused")
    static class FakeStub {
        static final int TRANSACTION_setValue = 62;
        static final int TRANSACTION_getValue = 3;
        static final int TRANSACTION_alsoThree = 3;
        static final int OTHER = 5;
        static final String DESCRIPTOR = "com.hkmc.fake.IFakeService";
        static final long TRANSACTION_wrongType = 9L;
        int TRANSACTION_instance = 7;
    }

    private static PortProbe.KeyEvent key(String device, int code, int value, long ms) {
        return new PortProbe.KeyEvent(device, code, value, ms);
    }

    @Test public void transactionTableListsStaticIntCodesInOrder() throws Exception {
        assertEquals(Arrays.asList("3 alsoThree", "3 getValue", "62 setValue"),
            PortProbe.transactionTable(FakeStub.class));
    }

    @Test public void constantsListStaticFinalValuesByName() throws Exception {
        List<String> constants = PortProbe.constants(FakeStub.class);
        assertTrue(constants.contains("DESCRIPTOR=com.hkmc.fake.IFakeService"));
        assertTrue(constants.contains("OTHER=5"));
        assertTrue(constants.contains("TRANSACTION_wrongType=9"));
        assertFalse(constants.toString().contains("TRANSACTION_instance"));
        assertEquals(constants.get(0), "DESCRIPTOR=com.hkmc.fake.IFakeService");
    }

    @Test public void heldKnobIsPickedOverTapsAndOtherKeys() {
        String main = "daudio_wp_daudioplus_neev_us", knob = "daudio_wp_daudioplus_neev_us_knob";
        List<String> summary = PortProbe.summarizeKeys(Arrays.asList(
            key(knob, 17, 1, 0), key(knob, 17, 0, 100),            // tap
            key(main, 17, 1, 1000), key(main, 17, 2, 1500), key(main, 17, 0, 3000),
            key(main, 115, 1, 3500), key(main, 115, 0, 4300),      // one hold of another key
            key(main, 17, 1, 5000), key(main, 17, 0, 6900),
            key(main, 17, 1, 8000)));                               // still down at the end
        assertTrue(summary.contains("key device=\"" + knob + "\" code=17 presses=1 holds=0 longest_hold_ms=100"));
        assertTrue(summary.contains("key device=\"" + main + "\" code=17 presses=3 holds=2 longest_hold_ms=2000"));
        assertTrue(summary.contains("key device=\"" + main + "\" code=115 presses=1 holds=1 longest_hold_ms=800"));
        assertEquals("likely_knob device=\"" + main + "\" code=17", summary.get(summary.size() - 1));
    }

    @Test public void equalHoldCountsPreferTheLongerHold() {
        List<String> summary = PortProbe.summarizeKeys(Arrays.asList(
            key("a", 1, 1, 0), key("a", 1, 0, 900),
            key("b", 2, 1, 1000), key("b", 2, 0, 3000)));
        assertEquals("likely_knob device=\"b\" code=2", summary.get(summary.size() - 1));
    }

    @Test public void noHeldKeyIsReportedAsNone() {
        List<String> none = PortProbe.summarizeKeys(Arrays.asList(key("a", 1, 1, 0), key("a", 1, 0, 200)));
        assertEquals("likely_knob=none (no held key captured)", none.get(none.size() - 1));
        assertEquals(Arrays.asList("likely_knob=none (no held key captured)"),
            PortProbe.summarizeKeys(Arrays.<PortProbe.KeyEvent>asList()));
    }

    @Test public void inputDevicesMapsEveryEventNodeToItsName() throws Exception {
        Map<String, String> devices = PortProbe.inputDevices(new StringReader(
            "I: Bus=0019 Vendor=0000 Product=0000 Version=0000\n"
            + "N: Name=\"daudio_wp_daudioplus_cvev_us\"\n"
            + "H: Handlers=kbd event1\n"
            + "\n"
            + "N: Name=\"daudio_wp_daudioplus_cvev_us_knob\"\n"
            + "H: Handlers=event2 mouse0\n"
            + "\n"
            + "H: Handlers=event9\n"));
        assertEquals(2, devices.size());
        assertEquals("daudio_wp_daudioplus_cvev_us", devices.get("/dev/input/event1"));
        assertEquals("daudio_wp_daudioplus_cvev_us_knob", devices.get("/dev/input/event2"));
    }
}
