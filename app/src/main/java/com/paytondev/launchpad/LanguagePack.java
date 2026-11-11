package com.paytondev.launchpad;

import android.content.Context;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/** JSON data only. English stays built in and cannot be overridden by downloaded files. */
final class LanguagePack implements MessagePatterns.Formatter {
    private static final int MAX_BYTES = 128 * 1024;
    private static final Pattern CODE = Pattern.compile("[a-z]{2,3}(?:-[A-Za-z0-9]{2,8})*");
    final String locale, name;
    private final JSONObject english, translated;
    private LanguagePack(String locale, String name, JSONObject english, JSONObject translated) {
        this.locale = locale; this.name = name; this.english = english; this.translated = translated;
    }
    private static JSONObject parse(InputStream stream, String code) throws Exception {
        try (InputStream in = stream) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] b = new byte[4096]; int n;
            while ((n = in.read(b)) != -1) { if (out.size() + n > MAX_BYTES) throw new IllegalArgumentException("too large"); out.write(b, 0, n); }
            JSONObject root = new JSONObject(new String(out.toByteArray(), Charset.forName("UTF-8")));
            if (root.optInt("format") != 1 || !code.equals(root.optString("locale")) || root.optString("name").trim().isEmpty() || root.optJSONObject("strings") == null) throw new IllegalArgumentException("invalid pack");
            return root;
        }
    }
    private static JSONObject read(Context c, String code) {
        if (!CODE.matcher(code).matches()) return null;
        try { return parse(c.getAssets().open("languages/" + code + ".json"), code); }
        catch (Exception ignored) {
            if ("en".equals(code) || "ko".equals(code)) return null;
            try {
                File dir = new File(c.getFilesDir(), "languages").getCanonicalFile();
                File f = new File(dir, code + ".json").getCanonicalFile();
                if (!dir.equals(f.getParentFile()) || !f.isFile()) return null;
                return parse(new FileInputStream(f), code);
            } catch (Exception unavailable) { return null; }
        }
    }
    static LanguagePack load(Context c, String code) {
        JSONObject base = read(c, "en");
        JSONObject selected = read(c, code == null ? "en" : code);
        if (base == null) base = new JSONObject();
        if (selected == null) { code = "en"; selected = base; }
        JSONObject english = base.optJSONObject("strings"), translated = selected.optJSONObject("strings");
        return new LanguagePack(code, selected.optString("name", "English"), english == null ? new JSONObject() : english, translated == null ? new JSONObject() : translated);
    }
    static List<String> available(Context c) {
        List<String> codes = new ArrayList<String>();
        try { String[] names = c.getAssets().list("languages"); if (names != null) for (String n : names) add(c, codes, n); } catch (Exception ignored) { }
        File[] files = new File(c.getFilesDir(), "languages").listFiles();
        if (files != null) for (File f : files) add(c, codes, f.getName());
        Collections.sort(codes); codes.remove("en"); codes.add(0, "en"); return codes;
    }
    private static void add(Context c, List<String> codes, String file) {
        if (!file.endsWith(".json")) return;
        String code = file.substring(0, file.length() - 5);
        if (CODE.matcher(code).matches() && !codes.contains(code) && read(c, code) != null) codes.add(code);
    }
    public String display(String value) {
        if (value == null) return null;
        if ("Checking vehicle services".equals(value)) return text("dynamic.text000", "Checking vehicle services");
        if ("The car has not supplied a recognized conditioning indicator.".equals(value)) return text("dynamic.text001", "The car has not supplied a recognized conditioning indicator.");
        if ("Cancel sent; the car reports conditioning Off.".equals(value)) return text("dynamic.text002", "Cancel sent; the car reports conditioning Off.");
        if ("Cancel sent; waiting for the car’s Off indicator.".equals(value)) return text("dynamic.text003", "Cancel sent; waiting for the car’s Off indicator.");
        if ("Vehicle operating status is unknown. Stop remains available.".equals(value)) return text("dynamic.text004", "Vehicle operating status is unknown. Stop remains available.");
        if ("The car reports battery conditioning active.".equals(value)) return text("dynamic.text005", "The car reports battery conditioning active.");
        if ("Enable battery conditioning in the vehicle’s EV settings first.".equals(value)) return text("dynamic.text006", "Enable battery conditioning in the vehicle’s EV settings first.");
        if ("The car has not reported READY.".equals(value)) return text("dynamic.text007", "The car has not reported READY.");
        if ("Conditioning is controlled by the car.".equals(value)) return text("dynamic.text008", "Conditioning is controlled by the car.");
        if ("Start a manual battery-conditioning request.".equals(value)) return text("dynamic.text009", "Start a manual battery-conditioning request.");
        if ("Releasing the manual request; waiting for the car’s Off indicator.".equals(value)) return text("dynamic.text010", "Releasing the manual request; waiting for the car’s Off indicator.");
        if ("Reported READY".equals(value)) return text("dynamic.text011", "Reported READY");
        if ("Not READY".equals(value)) return text("dynamic.text012", "Not READY");
        if ("On in EV settings".equals(value)) return text("dynamic.text013", "On in EV settings");
        if ("Active".equals(value)) return text("dynamic.text014", "Active");
        if ("Off".equals(value)) return text("dynamic.text015", "Off");
        if ("Supported".equals(value)) return text("dynamic.text016", "Supported");
        if ("Not supported".equals(value)) return text("dynamic.text017", "Not supported");
        if ("Unknown".equals(value)) return text("dynamic.text018", "Unknown");
        if ("Checking".equals(value)) return text("dynamic.text019", "Checking");
        if ("Battery is already warm. It only starts if it cools down.".equals(value)) return text("dynamic.text020", "Battery is already warm. It only starts if it cools down.");
        if ("Not enough time to fully warm the battery, so it starts right away.".equals(value)) return text("dynamic.text021", "Not enough time to fully warm the battery, so it starts right away.");
        if ("Battery temperature unknown, so it allows the full 30 min.".equals(value)) return text("dynamic.text022", "Battery temperature unknown, so it allows the full 30 min.");
        if ("Battery already warm".equals(value)) return text("dynamic.text023", "Battery already warm");
        if ("Waiting for the car".equals(value)) return text("dynamic.text024", "Waiting for the car");
        if (value.startsWith("Vehicle status unavailable. ")) return text("detail.vehicle_error", "Vehicle status unavailable. ") + display(value.substring(28));
        if (value.startsWith("Cancel sent, but session completion could not be recorded. ")) return text("detail.cancel_record", "Cancel sent, but session completion could not be recorded. ") + display(value.substring(59));
        if (value.startsWith("Manual request maintenance failed. ")) return text("detail.maintenance", "Manual request maintenance failed. ") + display(value.substring(35));
        if (value.startsWith("Start was not sent because session ownership could not be recorded. ")) return text("detail.start_record", "Start was not sent because session ownership could not be recorded. ") + display(value.substring(68));
        if (value.startsWith("Start could not be confirmed. Stop remains available. ")) return text("detail.start_error", "Start could not be confirmed. Stop remains available. ") + display(value.substring(54));
        if (value.startsWith("Cancellation is unconfirmed. Stop can be tried again. ")) return text("detail.stop_error", "Cancellation is unconfirmed. Stop can be tried again. ") + display(value.substring(54));
        if ("Unknown schedule type".equals(value)) return text("record.unknown", "Unknown schedule type");
        if ("Pick a time 2 minutes to 24 hours away".equals(value)) return text("record.time", "Pick a time 2 minutes to 24 hours away");
        if ("Couldn't save the schedule".equals(value)) return text("record.save_error", "Couldn't save the schedule");
        if ("Replaced by a new schedule".equals(value)) return text("record.replaced", "Replaced by a new schedule");
        if ("Battery is already warm".equals(value)) return text("record.warm", "Battery is already warm");
        if ("Battery stayed warm, so it didn't need to start".equals(value)) return text("record.stayed_warm", "Battery stayed warm, so it didn't need to start");
        if ("The car was already warming the battery".equals(value)) return text("record.already_active", "The car was already warming the battery");
        if ("Asked the car to start warming the battery".equals(value)) return text("record.sent", "Asked the car to start warming the battery");
        if ("Tried to start, but the car didn't confirm. Stop is still available.".equals(value)) return text("record.unconfirmed", "Tried to start, but the car didn't confirm. Stop is still available.");
        if ("Scheduled".equals(value)) return text("record.armed", "Scheduled");
        if ("Updated".equals(value)) return text("record.updated", "Updated");
        if ("Waiting".equals(value)) return text("record.waiting", "Waiting");
        if ("Started".equals(value)) return text("record.started", "Started");
        if ("Skipped".equals(value)) return text("record.skipped", "Skipped");
        if ("Missed".equals(value)) return text("record.missed", "Missed");
        if ("Cancelled".equals(value)) return text("record.cancelled", "Cancelled");
        if ("Resumed".equals(value)) return text("record.resumed", "Resumed");
        if ("Recovered".equals(value)) return text("record.recovered", "Recovered");
        if ("Not scheduled".equals(value)) return text("record.none", "Not scheduled");
        if ("Unsupported head-unit firmware".equals(value)) return text("error.unsupported", "Unsupported head-unit firmware");
        if ("Vehicle interface version differs from the inspected firmware".equals(value)) return text("error.firmware", "Vehicle interface version differs from the inspected firmware");
        if ("Vehicle services unavailable or incompatible".equals(value)) return text("error.service", "Vehicle services unavailable or incompatible");
        if ("Getter rejected".equals(value)) return text("error.getter", "Getter rejected");
        if ("Unexpected vehicle reply".equals(value)) return text("error.reply", "Unexpected vehicle reply");
        if ("Vehicle command rejected".equals(value)) return text("error.command", "Vehicle command rejected");
        if ("Unexpected vehicle command reply".equals(value)) return text("error.command_reply", "Unexpected vehicle command reply");
        if ("The car wasn't ready in time".equals(value)) return text("record.not_ready", "The car wasn't ready in time");
        if ("The schedule could not be updated before sending.".equals(value)) return text("record.schedule_update", "The schedule could not be updated before sending.");
        if ("Start was pressed".equals(value)) return text("record.start_pressed", "Start was pressed");
        if ("Start anyway was pressed".equals(value)) return text("record.force_pressed", "Start anyway was pressed");
        if ("By you".equals(value)) return text("record.user", "By you");
        if ("Battery was already warm".equals(value)) return text("record.warm_prev", "Battery was already warm");
        if ("Recovering · cancel pending request".equals(value)) return text("notify.recover", "Recovering · cancel pending request");
        if ("Sending manual request".equals(value)) return text("notify.sending", "Sending manual request");
        if ("Checking battery temperature".equals(value)) return text("notify.temp", "Checking battery temperature");
        if ("It may not be fully warm in time.".equals(value)) return text("plan.may_not_warm", "It may not be fully warm in time.");
        return MessagePatterns.display(value, this);
    }
    public String format(String key, String fallback, Object... args) {
        try { return String.format(java.util.Locale.getDefault(), text(key, fallback), args); }
        catch (java.util.IllegalFormatException e) { return String.format(java.util.Locale.US, fallback, args); }
    }
    /** Import only validated data from a user-selected document into app-private storage. */
    static String importPack(Context context, android.net.Uri uri) throws Exception {
        byte[] bytes;
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IllegalArgumentException("unreadable");
            ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] buf = new byte[4096]; int n;
            while ((n = in.read(buf)) != -1) {
                if (out.size() + n > MAX_BYTES) throw new IllegalArgumentException("too large");
                out.write(buf, 0, n);
            }
            bytes = out.toByteArray();
        }
        JSONObject root = new JSONObject(new String(bytes, Charset.forName("UTF-8")));
        String code = root.optString("locale");
        if (!CODE.matcher(code).matches() || "en".equals(code) || "ko".equals(code)
                || root.optInt("format") != 1 || root.optString("name").trim().isEmpty())
            throw new IllegalArgumentException("invalid language");
        JSONObject candidate = root.optJSONObject("strings"), baseRoot = read(context, "en");
        if (candidate == null || baseRoot == null) throw new IllegalArgumentException("missing strings");
        JSONObject base = baseRoot.getJSONObject("strings");
        java.util.Iterator<String> keys = candidate.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (!base.has(key) || !(candidate.get(key) instanceof String)
                    || !placeholders(base.getString(key)).equals(placeholders(candidate.getString(key))))
                throw new IllegalArgumentException("invalid translation");
        }
        File dir = new File(context.getFilesDir(), "languages");
        if (!dir.isDirectory() && !dir.mkdirs()) throw new java.io.IOException("cannot create directory");
        dir = dir.getCanonicalFile();
        File target = new File(dir, code + ".json").getCanonicalFile();
        File tmp = new File(dir, code + ".json.new").getCanonicalFile();
        if (!dir.equals(target.getParentFile()) || !dir.equals(tmp.getParentFile()) || target.exists() || tmp.exists())
            throw new java.io.IOException("file exists or path unsafe");
        try {
            try (java.io.FileOutputStream out = new java.io.FileOutputStream(tmp)) { out.write(bytes); out.getFD().sync(); }
            if (!tmp.renameTo(target)) throw new java.io.IOException("rename failed");
        } finally { if (tmp.exists()) tmp.delete(); }
        return code;
    }
    private static java.util.Map<String,Integer> placeholders(String value) {
        java.util.Map<String,Integer> result = new java.util.HashMap<String,Integer>();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("%(?:\\d+\\$)?[-+# 0,(<]*\\d*(?:\\.\\d+)?[a-zA-Z%]").matcher(value);
        while (m.find()) { String v = m.group(); Integer n = result.get(v); result.put(v, n == null ? 1 : n + 1); }
        return result;
    }
    String text(String key, String fallback) {
        String base = english.optString(key, fallback), value = translated.optString(key, "");
        return value.isEmpty() ? base : value;
    }
}
