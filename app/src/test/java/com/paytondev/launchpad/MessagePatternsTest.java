package com.paytondev.launchpad;
import org.junit.Test;
import java.util.Locale;
import static org.junit.Assert.assertEquals;
public class MessagePatternsTest {
    private final MessagePatterns.Formatter f = new MessagePatterns.Formatter() {
        public String format(String key, String fallback, Object... args) { return String.format(Locale.US, fallback, args); }
        public String display(String value) { return value; }
    };
    @Test public void planPreservesTemperaturesAndSuffix() {
        String s = "About 12 min to warm the battery from -4°C to 21°C. It may not be fully warm in time.";
        assertEquals(s, MessagePatterns.display(s, f));
    }
    @Test public void retryPreservesTimeAndReason() {
        String s = "Car not ready. Trying until 3:05 PM.";
        assertEquals(s, MessagePatterns.display(s, f));
    }
    @Test public void timedMessagesPreserveDuration() {
        String a = "No activation confirmed within 5 minutes. Cancellation confirmed; the car reports Off.";
        String b = "Request sent; conditioning is not active. Vehicle conditions may prevent activation. Cancels after 2 minutes without confirmed activation.";
        assertEquals(a, MessagePatterns.display(a, f));
        assertEquals(b, MessagePatterns.display(b, f));
    }
    @Test public void unknownMessageIsNotChanged() {
        assertEquals("unknown: 123", MessagePatterns.display("unknown: 123", f));
    }
}
