package com.paytondev.launchpad;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Finds how the head unit signals day/night: every setting or property that changes
 * while the owner switches the Hyundai display mode, plus the ones whose names look related.
 */
final class SignalLog {
    static final String[] WORDS = {"night", "day", "dark", "illum", "theme", "lamp", "dimm", "bright", "light"};
    static final int MAX_WATCHED = 16;
    private final Map<String, String> baseline;
    private final Map<String, String[]> changes = new LinkedHashMap<>();
    private Map<String, String> latest;

    SignalLog(Map<String, String> first) { baseline = new TreeMap<>(first); latest = baseline; }

    static boolean interesting(String key) {
        String lower = key.toLowerCase(Locale.US);
        for (String word : WORDS) if (lower.contains(word)) return true;
        return false;
    }
    /** One getprop line, "[name]: [value]", into {@code into} as "prop/name". */
    static void parseProp(String line, Map<String, String> into) {
        int split = line.indexOf("]: [");
        if (!line.startsWith("[") || split < 0 || !line.endsWith("]")) return;
        into.put("prop/" + line.substring(1, split), line.substring(split + 4, line.length() - 1));
    }
    /** Records every key whose value differs from when the panel opened. */
    void update(Map<String, String> now) {
        latest = new TreeMap<>(now);
        TreeSet<String> keys = new TreeSet<>(baseline.keySet()); keys.addAll(now.keySet()); keys.addAll(changes.keySet());
        for (String key : keys) {
            String before = baseline.get(key), after = now.get(key);
            if (before == null ? after == null : before.equals(after)) changes.remove(key);
            else changes.put(key, new String[]{before, after});
        }
    }
    Map<String, String[]> changes() { return changes; }
    List<String> lines() {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, String> e : latest.entrySet())
            if (e.getKey().startsWith("uimode/")) lines.add(e.getKey() + " = " + e.getValue());
        lines.add("");
        if (changes.isEmpty()) lines.add("No changes yet. Switch the Hyundai display between Day and Night.");
        else {
            lines.add("Changed since this tab opened:");
            for (Map.Entry<String, String[]> e : changes.entrySet())
                lines.add(e.getKey() + ": " + shown(e.getValue()[0]) + " → " + shown(e.getValue()[1]));
        }
        lines.add("");
        lines.add("Watching:");
        int watched = 0;
        for (Map.Entry<String, String> e : latest.entrySet()) {
            if (e.getKey().startsWith("uimode/") || !interesting(e.getKey())) continue;
            if (watched++ == MAX_WATCHED) { lines.add("…"); break; }
            lines.add(e.getKey() + " = " + shown(e.getValue()));
        }
        if (watched == 0) lines.add("(no setting or property names mention day, night or illumination)");
        return lines;
    }
    private static String shown(String value) { return value == null ? "(none)" : value.isEmpty() ? "\"\"" : value; }
}
