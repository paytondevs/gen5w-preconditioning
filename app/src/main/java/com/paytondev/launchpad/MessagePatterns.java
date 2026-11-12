package com.paytondev.launchpad;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parse only known messages. Unknown messages remain untouched. */
final class MessagePatterns {
    private static final Pattern PLAN = Pattern.compile("^About ([0-9]+) min to warm the battery from (-?[0-9]{1,3}°[CF]) to (-?[0-9]{1,3}°[CF])(\\.)( It may not be fully warm in time\\.)?$");
    private static final Pattern DEFER = Pattern.compile("^(.*) Trying until (.+)\\.$");
    private static final Pattern STARTING = Pattern.compile("^Now starting at (.+)$");
    private static final Pattern HEAD_OFF = Pattern.compile("^The head unit was off at (.+)$");
    private static final Pattern TIMEOUT = Pattern.compile("^No activation confirmed within ([0-9]+) (minute|minutes)\\. (Cancellation confirmed; the car reports Off\\.|Cancelling; waiting for the car’s Off indicator\\.)$");
    private static final Pattern REQUEST = Pattern.compile("^Request sent; conditioning is not active\\. Vehicle conditions may prevent activation\\. Cancels after ([0-9]+) (minute|minutes) without confirmed activation\\.$");
    private MessagePatterns() { }
    interface Formatter { String format(String key, String fallback, Object... args); String display(String value); }
    static String display(String value, Formatter formatter) {
        if (value == null) return null;
        Matcher timeout = TIMEOUT.matcher(value);
        if (timeout.matches()) {
            String unit = timeout.group(2);
            String time = formatter.format("time." + unit, "%1$s " + unit, timeout.group(1));
            return timeout.group(3).startsWith("Cancellation confirmed")
                ? formatter.format("detail.no_activation", "No activation confirmed within %1$s. Cancellation confirmed; the car reports Off.", time)
                : formatter.format("detail.cancelling", "No activation confirmed within %1$s. Cancelling; waiting for the car’s Off indicator.", time);
        }
        Matcher request = REQUEST.matcher(value);
        if (request.matches()) {
            String unit = request.group(2);
            String time = formatter.format("time." + unit, "%1$s " + unit, request.group(1));
            return formatter.format("detail.request_wait", "Request sent; conditioning is not active. Vehicle conditions may prevent activation. Cancels after %1$s without confirmed activation.", time);
        }
        Matcher p = PLAN.matcher(value);
        if (p.matches()) {
            String result = formatter.format("plan.estimate", "About %1$s min to warm the battery from %2$s to %3$s.", p.group(1), p.group(2), p.group(3));
            return p.group(5) == null ? result : result + " " + formatter.display("It may not be fully warm in time.");
        }
        Matcher d = DEFER.matcher(value);
        if (d.matches()) return formatter.format("record.retry", "%1$s Trying until %2$s.", formatter.display(d.group(1)), d.group(2));
        Matcher start = STARTING.matcher(value);
        if (start.matches()) return formatter.format("record.now_start", "Now starting at %1$s", start.group(1));
        Matcher off = HEAD_OFF.matcher(value);
        if (off.matches()) return formatter.format("record.head_off", "The head unit was off at %1$s", off.group(1));
        return value;
    }
}
