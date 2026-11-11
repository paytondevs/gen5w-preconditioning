package com.paytondev.launchpad;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

/** Owns the picker while the no-history main screen is not visible. */
public final class LanguageImportActivity extends Activity {
    private static final int PICK = 1;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (state != null) return;
        try {
            Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json");
            startActivityForResult(pick, PICK);
        } catch (ActivityNotFoundException | SecurityException e) { finishWithMessage("language.no_picker", "No file picker available"); }
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != PICK) return;
        if (result != RESULT_OK || data == null || data.getData() == null) { returnToApp(); return; }
        try {
            String code = LanguagePack.importPack(this, data.getData());
            getSharedPreferences("launchpad", MODE_PRIVATE).edit().putString("app_language", code).apply();
            finishWithMessage("language.imported", "Language pack added");
        } catch (Exception e) { finishWithMessage("language.invalid", "Invalid or unsupported language pack"); }
    }
    private void finishWithMessage(String key, String fallback) {
        String code = getSharedPreferences("launchpad", MODE_PRIVATE).getString("app_language", "en");
        Toast.makeText(this, LanguagePack.load(this, code).text(key, fallback), Toast.LENGTH_LONG).show();
        returnToApp();
    }
    private void returnToApp() {
        startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();
    }
}
