package com.paytondev.launchpad;

import android.app.UiModeManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Settings;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/** Read-only snapshot of Android display state, settings and properties. No vehicle Binder calls. */
final class DisplaySignals {
    static boolean night(Configuration configuration) {
        return (configuration.uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    /** Runs off the main thread: it starts getprop and queries three settings tables. */
    static Map<String, String> read(Context context) {
        Map<String, String> values = new HashMap<>();
        Configuration configuration = context.getResources().getConfiguration();
        int night = configuration.uiMode & Configuration.UI_MODE_NIGHT_MASK;
        values.put("uimode/night", night == Configuration.UI_MODE_NIGHT_YES ? "yes"
            : night == Configuration.UI_MODE_NIGHT_NO ? "no" : "undefined");
        values.put("uimode/type", String.valueOf(configuration.uiMode & Configuration.UI_MODE_TYPE_MASK));
        UiModeManager manager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
        if (manager != null) {
            values.put("uimode/manager_night", String.valueOf(manager.getNightMode()));
            values.put("uimode/manager_type", String.valueOf(manager.getCurrentModeType()));
        }
        ContentResolver resolver = context.getContentResolver();
        table(resolver, Settings.System.CONTENT_URI, "system/", values);
        table(resolver, Settings.Secure.CONTENT_URI, "secure/", values);
        table(resolver, Settings.Global.CONTENT_URI, "global/", values);
        try {
            Process process = new ProcessBuilder("/system/bin/getprop").redirectErrorStream(true).start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                for (String line; (line = reader.readLine()) != null; ) SignalLog.parseProp(line, values);
            }
            process.waitFor();
        } catch (Exception ignored) { }
        return values;
    }
    private static void table(ContentResolver resolver, Uri uri, String prefix, Map<String, String> into) {
        try (Cursor cursor = resolver.query(uri, new String[]{"name", "value"}, null, null, null)) {
            while (cursor != null && cursor.moveToNext()) {
                String name = cursor.getString(0);
                if (name != null) into.put(prefix + name, cursor.isNull(1) ? null : cursor.getString(1));
            }
        } catch (Exception ignored) { }
    }
    private DisplaySignals() { }
}
