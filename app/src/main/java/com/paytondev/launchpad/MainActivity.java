package com.paytondev.launchpad;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ScrollView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** APK-contained touch and TUNE launcher; the external hold service is unchanged. */
public final class MainActivity extends Activity {
    static final String EXTRA_FROM_KNOB = "from_knob";
    private static final int TAB_FAVORITES = 0, TAB_APPS = 1, TAB_ALL = 2;
    private static final int KEY_TUNE_UP = keyCode("KEYCODE_TUNE_UP", 223);
    private static final int KEY_TUNE_DOWN = keyCode("KEYCODE_TUNE_DOWN", 224);
    private static final int KEY_TUNE_ENTER = keyCode("KEYCODE_TUNE_ENTER", 243);
    private static final String[] TAB_TITLES = {"Preconditioning", "Home", "Advanced"};
    private static final int STOP_RED = 0xffe5484d;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<Target> targets = new ArrayList<>();
    private final List<Target> headerTargets = new ArrayList<>();
    private FocusUi ui;
    private LinearLayout toolbar;
    private FrameLayout content;
    private Favorites favorites;
    private SharedPreferences prefs;
    private TileAdapter adapter;
    private GridView grid;
    private TextView empty, statusText, networkText, ipText, adbText;
    private View statusDot, copyButton;
    private TextView preconditionStatus, preconditionDetail, preconditionElapsed, preconditionButton;
    private TextView preconditionTemperature;
    private TextView lowestValue, lowestCelsius, highestValue, temperatureAge;
    private View highestColumn, failureRow;
    private TemperatureGauge gauge;
    private TemperatureChart chart;
    private final CheckMark[] checkMarks = new CheckMark[6];
    private final TextView[] checkValues = new TextView[6];
    private final TextView[] defaultChips = new TextView[ScreenFlow.TABS.length];
    private final TextView[] appearanceChips = new TextView[AppearanceMode.values().length];
    private ScrollView advancedScroll;
    private Palette palette = Palette.LIGHT;
    private AppearanceMode appearance = AppearanceMode.FOLLOW_CAR;
    private boolean carNight, signalsReading;
    private TextView signalsText;
    private SignalLog signals;
    private PreconditioningService.LocalBinder vehicleService;
    private PreconditioningSnapshot preconditioning = PreconditioningSnapshot.checking();
    private BatteryTemperatureReading batteryTemperature = BatteryTemperatureReading.checking();
    private TemperatureHistory.Trace history = TemperatureHistory.Trace.EMPTY;
    private boolean vehicleBound;
    private ConnectionSnapshot connection = ConnectionSnapshot.unknown();
    private ScreenFlow.Page page = ScreenFlow.Page.PRECONDITIONING, defaultPage = ScreenFlow.Page.PRECONDITIONING;
    private int tab = TAB_ALL, knobIndex = -1, connectionEpoch, toggleColor;
    private long resumedAt, enterAt;
    private boolean resumed, started, enterDown, enterLong, preview, receiverRegistered, connectionReading;
    /** The toggle keeps the knob while it is briefly disabled, until TUNE is turned away. */
    private boolean knobOnToggle;

    private static final class Target {
        final View view;
        final int gridPosition;
        final Runnable action;
        boolean available = true;
        Target(View view, int gridPosition, Runnable action) {
            this.view = view; this.gridPosition = gridPosition; this.action = action;
        }
        boolean enabled() { return available && (view == null || view.isEnabled()); }
    }

    private final Runnable connectionTick = new Runnable() {
        @Override public void run() {
            if (resumed && page == ScreenFlow.Page.ADVANCED) {
                refreshConnection();
                main.postDelayed(this, 5000);
            }
        }
    };
    private final Runnable signalsTick = new Runnable() {
        @Override public void run() {
            if (resumed && page == ScreenFlow.Page.ADVANCED) {
                refreshSignals();
                main.postDelayed(this, 2000);
            }
        }
    };
    private final BroadcastReceiver wifiChanges = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (resumed && page == ScreenFlow.Page.ADVANCED) refreshConnection();
        }
    };
    private final ServiceConnection vehicleConnection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder binder) {
            vehicleService = (PreconditioningService.LocalBinder) binder;
            updatePreconditioningViews();
        }
        @Override public void onServiceDisconnected(ComponentName name) {
            vehicleService = null; preconditioning = PreconditioningSnapshot.checking();
            batteryTemperature = BatteryTemperatureReading.checking().update(null, SystemClock.elapsedRealtime());
            history = TemperatureHistory.Trace.EMPTY;
            updatePreconditioningViews();
        }
    };
    private final Runnable preconditioningTick = new Runnable() {
        @Override public void run() {
            if (!resumed) return;
            if (vehicleService != null) {
                preconditioning = vehicleService.snapshot();
                batteryTemperature = vehicleService.temperature();
                history = vehicleService.history();
            }
            updatePreconditioningViews(); main.postDelayed(this, 1000);
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_focus);
        toolbar = (LinearLayout) findViewById(R.id.focus_toolbar);
        content = (FrameLayout) findViewById(R.id.focus_content);
        prefs = getSharedPreferences("launchpad", MODE_PRIVATE);
        favorites = new Favorites(this);
        adapter = new TileAdapter(getLayoutInflater(), favorites);
        appearance = AppearanceMode.fromPreference(prefs.getString("appearance", null));
        carNight = DisplaySignals.night(getResources().getConfiguration());
        palette = Palette.of(appearance.dark(carNight));
        ui = new FocusUi(this, palette); adapter.setUi(ui); applyWindowColors();
        preview = PreviewData.enabled(getIntent());
        defaultPage = ScreenFlow.fromPreference(prefs.getString("default_page", null));
        showPage(defaultPage);
        final Context app = getApplicationContext();
        new Thread(new Runnable() {
            @Override public void run() {
                AppCatalog.load(app);
                main.post(new Runnable() {
                    @Override public void run() {
                        if (!isFinishing() && page == ScreenFlow.Page.BROWSER) renderBrowser();
                    }
                });
            }
        }, "launchpad-catalog").start();
    }

    @Override protected void onStart() {
        super.onStart(); started = true;
        vehicleBound = bindService(new Intent(this, PreconditioningService.class), vehicleConnection, BIND_AUTO_CREATE);
    }
    @Override protected void onStop() {
        started = false;
        if (vehicleBound) { unbindService(vehicleConnection); vehicleBound = false; }
        vehicleService = null; super.onStop();
    }
    @Override protected void onResume() {
        super.onResume(); resumed = true; resumedAt = SystemClock.uptimeMillis();
        IntentFilter filter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        try { registerReceiver(wifiChanges, filter); receiverRegistered = true; }
        catch (SecurityException e) { receiverRegistered = false; }
        restartConnectionTick();
        main.removeCallbacks(preconditioningTick); preconditioningTick.run();
    }
    @Override protected void onPause() {
        resumed = false; connectionEpoch++; main.removeCallbacks(connectionTick); main.removeCallbacks(signalsTick);
        main.removeCallbacks(preconditioningTick);
        if (receiverRegistered) { unregisterReceiver(wifiChanges); receiverRegistered = false; }
        super.onPause();
    }
    @Override protected void onDestroy() {
        main.removeCallbacksAndMessages(null); super.onDestroy();
    }
    // uiMode is in configChanges, so a day/night switch arrives here instead of recreating.
    @Override public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        carNight = DisplaySignals.night(configuration); applyAppearance();
    }
    /** Rebuilds the current page in the resolved palette, keeping the TUNE selection. */
    private void applyAppearance() {
        Palette next = Palette.of(appearance.dark(carNight));
        if (next == palette) return;
        palette = next; ui = new FocusUi(this, palette); adapter.setUi(ui); applyWindowColors();
        int knob = knobIndex; showPage(page);
        if (knob >= 0 && knob < targets.size() && targets.get(knob).enabled()) { knobIndex = knob; applyKnob(); }
    }
    private void applyWindowColors() {
        findViewById(R.id.focus_root).setBackgroundColor(palette.background);
        getWindow().setBackgroundDrawable(new ColorDrawable(palette.background));
    }
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent); setIntent(intent);
        // Android pauses a resumed activity before delivering onNewIntent, so its
        // visible/started state is the correct guard for the hold-to-close intent.
        if (intent.getBooleanExtra(EXTRA_FROM_KNOB, false) && started
            && SystemClock.uptimeMillis() - resumedAt > 700) finish();
    }
    @Override public void onBackPressed() {
        ScreenFlow.Page previous = ScreenFlow.back(page, defaultPage);
        if (previous == null) finish(); else showPage(previous);
    }

    private void showPage(ScreenFlow.Page next) {
        // A day/night rebuild of Advanced keeps the log: that switch is what it records.
        if (next != page || next != ScreenFlow.Page.ADVANCED) signals = null;
        page = next; connectionEpoch++;
        targets.clear(); headerTargets.clear(); grid = null;
        advancedScroll = null;
        preconditionStatus = null; preconditionDetail = null; preconditionButton = null;
        preconditionElapsed = null; preconditionTemperature = null;
        lowestValue = null; lowestCelsius = null; highestValue = null; temperatureAge = null;
        highestColumn = null; failureRow = null; gauge = null; chart = null; toggleColor = 0;
        Arrays.fill(checkMarks, null); Arrays.fill(checkValues, null);
        Arrays.fill(defaultChips, null); Arrays.fill(appearanceChips, null);
        signalsText = null;
        content.removeAllViews(); toolbar.removeAllViews();
        LinearLayout.LayoutParams contentParams = (LinearLayout.LayoutParams) content.getLayoutParams();
        contentParams.topMargin = ui.dp(page == ScreenFlow.Page.ADVANCED ? 28 : 16);
        content.setLayoutParams(contentParams);
        buildToolbar();
        if (page == ScreenFlow.Page.PRECONDITIONING) buildPreconditioning();
        else if (page == ScreenFlow.Page.HOME) buildHome();
        else if (page == ScreenFlow.Page.ADVANCED) buildAdvanced();
        else buildBrowser();
        if (page != ScreenFlow.Page.BROWSER) targets.addAll(headerTargets);
        knobOnToggle = page == ScreenFlow.Page.PRECONDITIONING;
        knobIndex = KnobFocus.next(-1, 1, enabledTargets()); applyKnob();
        restartConnectionTick();
        updatePreconditioningViews();
    }

    private void buildToolbar() {
        TextView title = ui.text("Launchpad", 36, palette.ink, true);
        toolbar.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout segmented = ui.row(); segmented.setPadding(ui.dp(4), ui.dp(4), ui.dp(4), ui.dp(4));
        ui.background(segmented, ui.rounded(palette.surface, 28, 0));
        ScreenFlow.Page current = page == ScreenFlow.Page.BROWSER ? ScreenFlow.Page.ADVANCED : page;
        for (int i = 0; i < ScreenFlow.TABS.length; i++) {
            final ScreenFlow.Page destination = ScreenFlow.TABS[i];
            TextView chip = chip(TAB_TITLES[i], destination == current, palette.surface);
            segmented.addView(chip, new LinearLayout.LayoutParams(ui.dp(i == 0 ? 170 : 128), ui.dp(40)));
            Runnable action = new Runnable() { @Override public void run() { showPage(destination); } };
            clickable(chip, action); headerTargets.add(new Target(chip, -1, action));
        }
        toolbar.addView(segmented);
        LinearLayout carHome = ui.button("Car home", R.drawable.ic_car, false, 154);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ui.dp(154), ui.dp(48));
        params.leftMargin = ui.dp(28); toolbar.addView(carHome, params);
        Runnable homeAction = shortcutAction(ShortcutSpec.CAR_HOME);
        clickable(carHome, homeAction); ui.available(carHome, available(ShortcutSpec.CAR_HOME), "");
        headerTargets.add(new Target(carHome, -1, homeAction));
    }

    private void buildHome() {
        LinearLayout body = ui.row();
        content.addView(body, new FrameLayout.LayoutParams(-1, -1));
        FrameLayout navigation = new FrameLayout(this);
        ui.background(navigation, ui.control(palette.hero, 20));
        LinearLayout.LayoutParams heroParams = new LinearLayout.LayoutParams(0, -1, 0.57f);
        body.addView(navigation, heroParams);
        FrameLayout.LayoutParams glyph = new FrameLayout.LayoutParams(ui.dp(128), ui.dp(128), Gravity.TOP | Gravity.LEFT);
        glyph.leftMargin = ui.dp(40); glyph.topMargin = ui.dp(36);
        View navigationIcon = ui.icon(R.drawable.ic_nav, 128);
        navigationIcon.setRotation(90);
        navigation.addView(navigationIcon, glyph);
        LinearLayout labels = ui.column();
        labels.addView(ui.text("Navigation", 64, palette.ink, true));
        TextView subtitle = ui.text(ShortcutSpec.NAVIGATION.subtitle, 26, palette.muted, false);
        subtitle.setTag("subtitle");
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(-2, -2);
        subtitleParams.topMargin = ui.dp(9); labels.addView(subtitle, subtitleParams);
        FrameLayout.LayoutParams labelsParams = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM | Gravity.LEFT);
        labelsParams.leftMargin = ui.dp(36); labelsParams.bottomMargin = ui.dp(30);
        labelsParams.rightMargin = ui.dp(112);
        navigation.addView(labels, labelsParams);
        FrameLayout go = ui.appIcon(R.drawable.ic_arrow, palette.accent, 68, 36);
        ui.background(go, ui.rounded(palette.accent, 34, 0));
        FrameLayout.LayoutParams goParams = new FrameLayout.LayoutParams(ui.dp(68), ui.dp(68), Gravity.BOTTOM | Gravity.RIGHT);
        goParams.rightMargin = ui.dp(28); goParams.bottomMargin = ui.dp(28);
        navigation.addView(go, goParams);
        addShortcutTarget(navigation, ShortcutSpec.NAVIGATION);
        navigation.setContentDescription("Navigation. Choose a destination");

        LinearLayout actions = ui.column();
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(0, -1, 0.43f);
        actionsParams.leftMargin = ui.dp(28); body.addView(actions, actionsParams);
        ShortcutSpec[] specs = {ShortcutSpec.MUSIC, ShortcutSpec.PHONE, ShortcutSpec.PROJECTION};
        int[] icons = {R.drawable.ic_music, R.drawable.ic_phone, R.drawable.ic_projection};
        int[] colors = {0xffff6175, 0xff22c967, 0xff9861ff};
        for (int i = 0; i < specs.length; i++) {
            LinearLayout row = ui.homeAction(specs[i].title, specs[i].subtitle, icons[i], colors[i]);
            actions.addView(row, new LinearLayout.LayoutParams(-1, 0, 1));
            addShortcutTarget(row, specs[i]);
            actions.addView(ui.divider(false));
        }
        LinearLayout precondition = ui.homeAction("Preconditioning", "Checking vehicle services",
            R.drawable.ic_preconditioning, 0xffffa623);
        // Give the action its own focus target, independent of the read-only labels.
        precondition.removeViewAt(precondition.getChildCount() - 1);
        preconditionStatus = (TextView) precondition.findViewWithTag("subtitle");
        preconditionTemperature = ui.text("Battery temp: checking…", 14, palette.muted, false);
        preconditionTemperature.setSingleLine(true);
        preconditionTemperature.setEllipsize(TextUtils.TruncateAt.END);
        ((LinearLayout) preconditionStatus.getParent()).addView(preconditionTemperature);
        // The row opens the tab; add it before the button so the button's knob state wins.
        precondition.setContentDescription("Preconditioning. Open the preconditioning tab");
        addTarget(precondition, new Runnable() { @Override public void run() { showPage(ScreenFlow.Page.PRECONDITIONING); } });
        preconditionButton = preconditioningButton();
        precondition.addView(preconditionButton, new LinearLayout.LayoutParams(ui.dp(82), ui.dp(44)));
        actions.addView(precondition, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private void buildAdvanced() {
        advancedScroll = new ScrollView(this); advancedScroll.setFillViewport(true);
        content.addView(advancedScroll, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout body = ui.column();
        advancedScroll.addView(body, new ScrollView.LayoutParams(-1, -2));
        LinearLayout group = ui.row(); group.setGravity(Gravity.CENTER_VERTICAL);
        group.setPadding(ui.dp(24), ui.dp(16), ui.dp(18), ui.dp(16));
        ui.background(group, ui.rounded(palette.surface, 20, 0));
        body.addView(group, new LinearLayout.LayoutParams(-1, ui.dp(100)));
        LinearLayout network = ui.row(); network.setGravity(Gravity.CENTER_VERTICAL);
        statusDot = new View(this); ui.background(statusDot, ui.rounded(palette.idle, 6, 0));
        network.addView(statusDot, new LinearLayout.LayoutParams(ui.dp(16), ui.dp(16)));
        LinearLayout networkLabels = ui.column();
        statusText = ui.text("Checking Wi-Fi", 24, palette.ink, true);
        networkText = ui.text("Network / Unavailable", 16, palette.muted, false);
        networkText.setSingleLine(true); networkText.setEllipsize(TextUtils.TruncateAt.END);
        networkLabels.addView(statusText);
        LinearLayout.LayoutParams sub = new LinearLayout.LayoutParams(-1, -2); sub.topMargin = ui.dp(7);
        networkLabels.addView(networkText, sub);
        LinearLayout.LayoutParams networkLabelParams = new LinearLayout.LayoutParams(0, -2, 1);
        networkLabelParams.leftMargin = ui.dp(18); network.addView(networkLabels, networkLabelParams);
        group.addView(network, new LinearLayout.LayoutParams(0, -1, 0.31f));
        group.addView(ui.divider(true));
        LinearLayout ip = detailColumn("IP address", false);
        ipText = (TextView) ip.findViewWithTag("value");
        group.addView(ip, new LinearLayout.LayoutParams(0, -1, 0.22f)); group.addView(ui.divider(true));
        LinearLayout adbGroup = ui.row(); adbGroup.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout adbColumn = detailColumn("ADB address", true);
        adbText = (TextView) adbColumn.findViewWithTag("value");
        adbGroup.addView(adbColumn, new LinearLayout.LayoutParams(0, -1, 1));
        FrameLayout copy = ui.appIcon(R.drawable.ic_copy, palette.quiet, 40, 23);
        ui.background(copy, ui.control(palette.quiet, 12));
        copyButton = copy; copyButton.setContentDescription("Copy ADB address"); copyButton.setEnabled(false);
        adbGroup.addView(copy, new LinearLayout.LayoutParams(ui.dp(40), ui.dp(40)));
        LinearLayout.LayoutParams adbParams = new LinearLayout.LayoutParams(0, -1, 0.36f);
        adbParams.rightMargin = ui.dp(20); group.addView(adbGroup, adbParams);
        Runnable copyAction = new Runnable() { @Override public void run() { copyAddress(); } };
        clickable(copy, copyAction);
        group.addView(ui.divider(true));
        LinearLayout wifi = ui.button("Wi-Fi settings", R.drawable.ic_wifi, true, 198);
        LinearLayout.LayoutParams wifiParams = new LinearLayout.LayoutParams(ui.dp(198), ui.dp(48));
        wifiParams.leftMargin = ui.dp(22); group.addView(wifi, wifiParams);
        addShortcutTarget(wifi, ShortcutSpec.WIFI);
        targets.add(new Target(copyButton, -1, copyAction));

        LinearLayout card = ui.row(); card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(ui.dp(24), ui.dp(16), ui.dp(20), ui.dp(16));
        ui.background(card, ui.rounded(palette.surface, 20, 0));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.topMargin = ui.dp(12); body.addView(card, cardParams);
        LinearLayout details = ui.column();
        details.addView(ui.text("Default tab", 24, palette.ink, true));
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(-2, -2); hintParams.topMargin = ui.dp(7);
        details.addView(ui.text("Launchpad opens on this tab", 16, palette.muted, false), hintParams);
        card.addView(details, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout choices = ui.row(); choices.setPadding(ui.dp(4), ui.dp(4), ui.dp(4), ui.dp(4));
        ui.background(choices, ui.rounded(palette.card, 28, 0));
        card.addView(choices);
        for (int i = 0; i < ScreenFlow.TABS.length; i++) {
            final int index = i;
            defaultChips[i] = chip(TAB_TITLES[i], ScreenFlow.TABS[i] == defaultPage, palette.card);
            choices.addView(defaultChips[i], new LinearLayout.LayoutParams(ui.dp(i == 0 ? 170 : 128), ui.dp(40)));
            addTarget(defaultChips[i], new Runnable() { @Override public void run() {
                defaultPage = ScreenFlow.TABS[index];
                prefs.edit().putString("default_page", defaultPage.name()).apply();
                for (int j = 0; j < defaultChips.length; j++) styleChip(defaultChips[j], j == index, palette.card);
            } });
        }

        LinearLayout look = ui.row(); look.setGravity(Gravity.CENTER_VERTICAL);
        look.setPadding(ui.dp(24), ui.dp(16), ui.dp(20), ui.dp(16));
        ui.background(look, ui.rounded(palette.surface, 20, 0));
        LinearLayout.LayoutParams lookParams = new LinearLayout.LayoutParams(-1, -2);
        lookParams.topMargin = ui.dp(12); body.addView(look, lookParams);
        LinearLayout lookDetails = ui.column();
        lookDetails.addView(ui.text("Appearance", 24, palette.ink, true));
        LinearLayout.LayoutParams lookHint = new LinearLayout.LayoutParams(-2, -2); lookHint.topMargin = ui.dp(7);
        lookDetails.addView(ui.text(appearance == AppearanceMode.FOLLOW_CAR ? "Follows the car's day/night display"
            : "Always " + appearance.title.toLowerCase(java.util.Locale.US), 16, palette.muted, false), lookHint);
        look.addView(lookDetails, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout modes = ui.row(); modes.setPadding(ui.dp(4), ui.dp(4), ui.dp(4), ui.dp(4));
        ui.background(modes, ui.rounded(palette.card, 28, 0));
        look.addView(modes);
        final AppearanceMode[] modeValues = AppearanceMode.values();
        for (int i = 0; i < modeValues.length; i++) {
            final AppearanceMode mode = modeValues[i];
            appearanceChips[i] = chip(mode.title, mode == appearance, palette.card);
            modes.addView(appearanceChips[i], new LinearLayout.LayoutParams(ui.dp(i == 0 ? 150 : 110), ui.dp(40)));
            addTarget(appearanceChips[i], new Runnable() { @Override public void run() {
                if (mode == appearance) return;
                appearance = mode; prefs.edit().putString("appearance", mode.name()).apply();
                Palette before = palette; applyAppearance();
                // Same palette: the page wasn't rebuilt, so restyle the chips here.
                if (before == palette) {
                    int knob = knobIndex; showPage(page);
                    if (knob >= 0 && knob < targets.size()) { knobIndex = knob; applyKnob(); }
                }
            } });
        }

        TextView tools = ui.text("TOOLS", 12, palette.muted, false);
        LinearLayout.LayoutParams toolsParams = new LinearLayout.LayoutParams(-1, -2);
        toolsParams.topMargin = ui.dp(12); toolsParams.bottomMargin = ui.dp(8); toolsParams.leftMargin = ui.dp(4);
        body.addView(tools, toolsParams);
        LinearLayout list = ui.column();
        ui.background(list, ui.rounded(palette.card, 17, palette.line));
        list.setPadding(ui.dp(2), ui.dp(2), ui.dp(2), ui.dp(2));
        body.addView(list, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout screens = ui.actionRow("All screens", "Browse every available screen",
            R.drawable.ic_screens, palette.screensIcon, false);
        list.addView(screens, new LinearLayout.LayoutParams(-1, ui.dp(80)));
        addTarget(screens, new Runnable() { @Override public void run() {
            tab = TAB_ALL; showPage(ScreenFlow.Page.BROWSER);
        } });
        View separator = ui.divider(false);
        LinearLayout.LayoutParams separatorParams = new LinearLayout.LayoutParams(-1, ui.dp(1));
        separatorParams.leftMargin = ui.dp(24); separatorParams.rightMargin = ui.dp(24);
        list.addView(separator, separatorParams);
        LinearLayout nemesis = ui.actionRow(ShortcutSpec.NEMESIS.title, ShortcutSpec.NEMESIS.subtitle,
            R.drawable.ic_nemesis, 0xff9861ff, false);
        list.addView(nemesis, new LinearLayout.LayoutParams(-1, ui.dp(80)));
        addShortcutTarget(nemesis, ShortcutSpec.NEMESIS);

        LinearLayout probe = ui.column(); probe.setPadding(ui.dp(24), ui.dp(16), ui.dp(20), ui.dp(16));
        ui.background(probe, ui.rounded(palette.surface, 20, 0));
        LinearLayout.LayoutParams probeParams = new LinearLayout.LayoutParams(-1, -2);
        probeParams.topMargin = ui.dp(12); body.addView(probe, probeParams);
        probe.addView(ui.text("Appearance signals", 24, palette.ink, true));
        probe.addView(ui.text("Read-only. Switch the Hyundai display between Day and Night (and Auto with the "
            + "headlights on and off), then report which lines changed.", 16, palette.muted, false), below(7));
        signalsText = ui.text("Reading…", 14, palette.ink, false);
        signalsText.setTypeface(android.graphics.Typeface.MONOSPACE); signalsText.setLineSpacing(0, 1.15f);
        probe.addView(signalsText, below(12));
        updateConnectionViews();
    }

    private TextView chip(String label, boolean selected, int idle) {
        TextView chip = ui.text(label, 17, palette.ink, false); chip.setGravity(Gravity.CENTER);
        styleChip(chip, selected, idle); return chip;
    }
    private void styleChip(TextView chip, boolean selected, int idle) {
        chip.setTextColor(selected ? palette.onAccent : palette.ink);
        ui.background(chip, ui.control(selected ? palette.accent : idle, 24));
    }
    private TextView preconditioningButton() {
        TextView button = ui.text("Start", 18, palette.onAccent, true);
        button.setGravity(Gravity.CENTER);
        ui.background(button, ui.control(palette.accent, 22));
        addTarget(button, new Runnable() { @Override public void run() { preconditioningAction(); } });
        button.setEnabled(false); return button;
    }
    private void preconditioningAction() {
        if (!preconditioning.canStop && !preconditioning.canStart) return;
        String action = preconditioning.canStop ? PreconditioningService.STOP
            : preconditioning.state == PreconditioningSnapshot.State.WARM
                ? PreconditioningService.START_ANYWAY : PreconditioningService.START;
        startService(new Intent(this, PreconditioningService.class).setAction(action));
        // Serialize presses until the service publishes the result.
        if (preconditionButton != null) preconditionButton.setEnabled(false);
    }

    private void buildPreconditioning() {
        LinearLayout body = ui.row();
        content.addView(body, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout control = panel(body, 0.34f, 0);
        control.addView(caption("BATTERY PRECONDITIONING"));
        preconditionStatus = ui.text("Checking", 34, palette.ink, true); preconditionStatus.setMaxLines(2);
        control.addView(preconditionStatus, below(10));
        preconditionElapsed = ui.text("Elapsed: —", 18, palette.ink, false);
        control.addView(preconditionElapsed, below(10));
        preconditionDetail = ui.text("Checking vehicle services", 16, palette.muted, false);
        preconditionDetail.setLineSpacing(0, 1.15f); preconditionDetail.setEllipsize(TextUtils.TruncateAt.END);
        preconditionDetail.setMaxLines(6);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(-1, 0, 1); detailParams.topMargin = ui.dp(14);
        control.addView(preconditionDetail, detailParams);
        preconditionButton = ui.text("Start", 26, palette.onAccent, true); preconditionButton.setGravity(Gravity.CENTER);
        addTarget(preconditionButton, new Runnable() { @Override public void run() { preconditioningAction(); } });
        preconditionButton.setEnabled(false);
        control.addView(preconditionButton, new LinearLayout.LayoutParams(ui.dp(260), ui.dp(72)));

        LinearLayout temperature = panel(body, 0.40f, 16);
        LinearLayout readings = ui.row();
        LinearLayout lowest = ui.column(); lowest.addView(caption("LOWEST PACK"));
        LinearLayout values = ui.row(); values.setGravity(Gravity.BOTTOM);
        lowestValue = ui.text("—", 48, palette.ink, true); values.addView(lowestValue);
        lowestCelsius = ui.text("", 20, palette.muted, false);
        LinearLayout.LayoutParams celsiusParams = new LinearLayout.LayoutParams(-2, -2);
        celsiusParams.leftMargin = ui.dp(10); celsiusParams.bottomMargin = ui.dp(5); values.addView(lowestCelsius, celsiusParams);
        lowest.addView(values, below(6));
        readings.addView(lowest, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout highest = ui.column(); highest.setGravity(Gravity.RIGHT);
        highest.addView(caption("HIGHEST PACK"));
        highestValue = ui.text("—", 28, palette.ink, true); highest.addView(highestValue, below(6));
        highestColumn = highest; highest.setVisibility(View.GONE); readings.addView(highest);
        temperature.addView(readings);
        temperatureAge = ui.text("Checking…", 14, palette.muted, false); temperatureAge.setSingleLine(true);
        temperature.addView(temperatureAge, below(6));
        gauge = new TemperatureGauge(this); gauge.setPalette(palette);
        LinearLayout.LayoutParams gaugeParams = new LinearLayout.LayoutParams(-1, ui.dp(58)); gaugeParams.topMargin = ui.dp(8);
        temperature.addView(gauge, gaugeParams);
        chart = new TemperatureChart(this); chart.setPalette(palette);
        LinearLayout.LayoutParams chartParams = new LinearLayout.LayoutParams(-1, 0, 1); chartParams.topMargin = ui.dp(6);
        temperature.addView(chart, chartParams);

        LinearLayout checks = panel(body, 0.26f, 16);
        checks.addView(caption("VEHICLE CHECKS"));
        String[] labels = {"EV READY", "Battery conditioning", "Car indicator", "Firmware profile", "Temperature source", "Failure code"};
        for (int i = 0; i < labels.length; i++) {
            LinearLayout row = ui.row(); row.setGravity(Gravity.CENTER_VERTICAL);
            checkMarks[i] = new CheckMark(this);
            row.addView(checkMarks[i], new LinearLayout.LayoutParams(ui.dp(26), ui.dp(26)));
            LinearLayout text = ui.column();
            TextView label = ui.text(labels[i], 17, palette.ink, true); label.setSingleLine(true);
            text.addView(label);
            checkValues[i] = ui.text("Unknown", 14, palette.muted, false); checkValues[i].setSingleLine(true);
            checkValues[i].setEllipsize(TextUtils.TruncateAt.END); text.addView(checkValues[i], below(4));
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, -2, 1); textParams.leftMargin = ui.dp(12);
            row.addView(text, textParams);
            checks.addView(row, new LinearLayout.LayoutParams(-1, 0, 1));
            if (i == labels.length - 1) { failureRow = row; row.setVisibility(View.GONE); }
        }
    }
    private LinearLayout panel(LinearLayout parent, float weight, int gap) {
        LinearLayout panel = ui.column(); panel.setPadding(ui.dp(20), ui.dp(18), ui.dp(20), ui.dp(18));
        ui.background(panel, ui.rounded(palette.surface, 20, 0));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, weight); params.leftMargin = ui.dp(gap);
        parent.addView(panel, params); return panel;
    }
    private TextView caption(String value) { return ui.text(value, 12, palette.muted, true); }
    private LinearLayout.LayoutParams below(int margin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2); params.topMargin = ui.dp(margin); return params;
    }

    private void updatePreconditioningViews() {
        if (preconditionStatus == null) return;
        long now = SystemClock.elapsedRealtime();
        String status = preconditioning.title();
        if (page == ScreenFlow.Page.HOME && preconditioning.requested) status += " · " + preconditioning.elapsed();
        preconditionStatus.setText(status);
        if (preconditionTemperature != null) preconditionTemperature.setText(batteryTemperature.label(now, true));
        if (preconditionDetail != null) preconditionDetail.setText(preconditioning.detail);
        if (preconditionElapsed != null) preconditionElapsed.setText("Elapsed: " +
            (preconditioning.requested || preconditioning.state == PreconditioningSnapshot.State.ENDED
                || preconditioning.state == PreconditioningSnapshot.State.TIMED_OUT ? preconditioning.elapsed() : "—"));
        if (lowestValue != null) updateTemperatureViews(now);
        if (preconditionButton != null) {
            boolean warm = preconditioning.state == PreconditioningSnapshot.State.WARM;
            boolean toggle = page == ScreenFlow.Page.PRECONDITIONING;
            preconditionButton.setText(warm ? toggle ? "Start anyway" : "Start\nanyway" : preconditioning.requested ? "Stop" : "Start");
            preconditionButton.setTextSize(toggle ? warm ? 22 : 26 : warm ? 14 : 18);
            preconditionButton.setContentDescription(warm ? "Start preconditioning anyway" : preconditioning.requested ? "Stop preconditioning" : "Start preconditioning");
            int color = preconditioning.requested ? STOP_RED : warm ? TemperatureGauge.WARM : palette.accent;
            if (toggle && color != toggleColor) { toggleColor = color; ui.background(preconditionButton, ui.pill(color, 36)); }
            boolean enabled = preconditioning.canStart || preconditioning.canStop;
            preconditionButton.setEnabled(enabled); preconditionButton.setAlpha(enabled ? 1f : 0.4f);
            if (knobIndex >= 0 && knobIndex < targets.size() && !targets.get(knobIndex).enabled()) moveKnob(1);
            // The toggle is the first target on its tab.
            if (toggle && enabled && knobOnToggle && knobIndex != 0) { knobIndex = 0; applyKnob(); }
        }
    }
    private void updateTemperatureViews(long now) {
        Integer minimum = batteryTemperature.minimumC;
        boolean stale = batteryTemperature.stale(now);
        lowestValue.setText(minimum == null ? "—" : BatteryTemperatureReading.fahrenheit(minimum) + "°F");
        lowestValue.setTextColor(stale ? palette.muted : palette.ink);
        lowestCelsius.setText(minimum == null ? "" : minimum + "°C");
        temperatureAge.setText(batteryTemperature.age(now));
        TemperatureHistory.Sample latest = history.latest();
        Integer maximum = latest == null || stale || latest.at != batteryTemperature.receivedAt ? null : latest.maximumC;
        highestColumn.setVisibility(maximum == null ? View.GONE : View.VISIBLE);
        if (maximum != null) highestValue.setText(BatteryTemperatureReading.fahrenheit(maximum) + "°F · " + maximum + "°C");
        int threshold = vehicleService == null ? BatteryTemperature.EXPECTED_HEATING_THRESHOLD_C : vehicleService.warmThresholdC();
        gauge.setValue(minimum, threshold); chart.setTrace(history, now, threshold);
        setCheck(0, preconditioning.readyCheck(), preconditioning.readyValue());
        setCheck(1, preconditioning.enabledCheck(), preconditioning.enabledValue());
        setCheck(2, preconditioning.indicatorCheck(), preconditioning.indicatorValue());
        setCheck(3, preconditioning.supported, preconditioning.supportedValue());
        setCheck(4, minimum != null ? PreconditioningSnapshot.PASS : batteryTemperature.attempted
            ? PreconditioningSnapshot.FAIL : PreconditioningSnapshot.UNKNOWN,
            minimum != null ? "Native" : batteryTemperature.attempted ? "Not available on this firmware" : "Checking");
        failureRow.setVisibility(preconditioning.failureRaw >= 0 ? View.VISIBLE : View.GONE);
        setCheck(5, PreconditioningSnapshot.UNKNOWN, "Raw " + preconditioning.failureRaw + " · meaning unknown");
    }
    private void setCheck(int index, int state, String value) {
        checkMarks[index].setState(state); checkValues[index].setText(value);
    }

    private LinearLayout detailColumn(String title, boolean compact) {
        LinearLayout column = ui.column(); column.setGravity(Gravity.CENTER_VERTICAL);
        column.setPadding(ui.dp(compact ? 24 : 32), 0, ui.dp(12), 0);
        column.addView(ui.text(title, 14, palette.muted, false));
        TextView value = ui.text("—", 20, palette.ink, true); value.setTag("value"); value.setSingleLine(true);
        value.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.topMargin = ui.dp(8);
        column.addView(value, params); return column;
    }

    private void buildBrowser() {
        LinearLayout body = ui.column(); content.addView(body, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout browserToolbar = ui.row(); browserToolbar.setGravity(Gravity.CENTER_VERTICAL);
        body.addView(browserToolbar, new LinearLayout.LayoutParams(-1, ui.dp(52)));
        LinearLayout back = ui.button("All screens", R.drawable.ic_back, false, 180);
        browserToolbar.addView(back);
        addTarget(back, new Runnable() { @Override public void run() { showPage(ScreenFlow.Page.ADVANCED); } });
        String[] names = {"Favorites", "Apps", "All screens"};
        for (int i = 0; i < names.length; i++) {
            final int selection = i;
            TextView button = ui.text(names[i], 18, tab == i ? palette.onAccent : palette.ink, false);
            button.setGravity(Gravity.CENTER);
            ui.background(button, ui.control(tab == i ? palette.accent : palette.surface, 22));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ui.dp(156), ui.dp(44));
            params.leftMargin = ui.dp(12); browserToolbar.addView(button, params);
            addTarget(button, new Runnable() { @Override public void run() {
                tab = selection; prefs.edit().putInt("tab", tab).apply(); showPage(ScreenFlow.Page.BROWSER);
            } });
        }
        TextView hint = ui.text("Hold an app to favorite", 14, palette.muted, false);
        hint.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        browserToolbar.addView(hint, new LinearLayout.LayoutParams(0, -1, 1));
        FrameLayout list = new FrameLayout(this);
        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(-1, 0, 1);
        listParams.topMargin = ui.dp(12); body.addView(list, listParams);
        grid = new GridView(this); grid.setNumColumns(GridView.AUTO_FIT); grid.setColumnWidth(ui.dp(152));
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setHorizontalSpacing(ui.dp(12)); grid.setVerticalSpacing(ui.dp(12));
        grid.setSelector(android.R.color.transparent); grid.setClipToPadding(false); grid.setAdapter(adapter);
        list.addView(grid, new FrameLayout.LayoutParams(-1, -1));
        empty = ui.text("Loading screens…", 20, palette.muted, false);
        empty.setGravity(Gravity.CENTER); list.addView(empty, new FrameLayout.LayoutParams(-1, -1));
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                launch(adapter.getItem(position));
            }
        });
        grid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                AppCatalog.Entry entry = adapter.getItem(position);
                boolean added = favorites.toggle(entry.key);
                toast(getString(added ? R.string.fav_added : R.string.fav_removed, entry.label));
                if (tab == TAB_FAVORITES) renderBrowser(); else adapter.notifyDataSetChanged();
                return true;
            }
        });
        renderBrowser();
    }

    private void renderBrowser() {
        if (grid == null) return;
        AppCatalog catalog = AppCatalog.cached();
        List<AppCatalog.Entry> entries = new ArrayList<>();
        if (catalog != null) {
            if (tab == TAB_FAVORITES) {
                for (String key : favorites.keys()) { AppCatalog.Entry entry = catalog.find(key); if (entry != null) entries.add(entry); }
            } else entries.addAll(tab == TAB_APPS ? catalog.apps : catalog.all);
        }
        adapter.setItems(entries);
        empty.setText(catalog == null ? "Loading screens…" : tab == TAB_FAVORITES
            ? "No favorites yet\nHold an app to add it here" : "No available screens");
        empty.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);
        // Keep the four browser controls; rebuild item targets when the catalog or favorites change.
        while (targets.size() > 4) targets.remove(targets.size() - 1);
        for (int i = 0; i < entries.size(); i++) {
            final AppCatalog.Entry entry = entries.get(i);
            Target target = new Target(null, i, new Runnable() { @Override public void run() { launch(entry); } });
            target.available = entry.launchable;
            targets.add(target);
        }
        targets.addAll(headerTargets);
        knobIndex = KnobFocus.next(-1, 1, enabledTargets()); applyKnob();
    }

    private Intent intent(ShortcutSpec spec) {
        Intent intent = new Intent(spec.action);
        if (spec.component != null) intent.setComponent(ComponentName.unflattenFromString(spec.component));
        if (spec.launcher) intent.addCategory(Intent.CATEGORY_LAUNCHER);
        if (spec == ShortcutSpec.CAR_HOME) intent.addCategory(Intent.CATEGORY_HOME);
        return intent;
    }
    private boolean available(ShortcutSpec spec) {
        return preview || canStart(intent(spec));
    }
    private boolean canStart(Intent intent) {
        ResolveInfo result = getPackageManager().resolveActivity(intent, 0);
        ActivityInfo info = result == null ? null : result.activityInfo;
        boolean permitted = info != null && (info.permission == null || getPackageManager().checkPermission(
            info.permission, getPackageName()) == PackageManager.PERMISSION_GRANTED);
        return ShortcutSpec.mayLaunch(info != null, info != null && info.exported,
            info != null && info.enabled, info != null && info.applicationInfo.enabled, permitted);
    }
    private Runnable shortcutAction(final ShortcutSpec spec) {
        return new Runnable() { @Override public void run() { start(intent(spec), spec.title); } };
    }
    private void addShortcutTarget(View view, ShortcutSpec spec) {
        ui.available(view, available(spec), spec.subtitle); addTarget(view, shortcutAction(spec));
    }
    private void addTarget(View view, Runnable action) {
        clickable(view, action); targets.add(new Target(view, -1, action));
    }
    private void clickable(View view, final Runnable action) {
        // TUNE has one explicit selection model. Suppress the separate native
        // keyboard focus so it cannot draw a second highlight on another control.
        view.setFocusable(false);
        view.setClickable(true);
        view.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { action.run(); } });
    }
    private void launch(AppCatalog.Entry entry) {
        Intent intent = new Intent(Intent.ACTION_MAIN).setComponent(entry.component);
        AppCatalog catalog = AppCatalog.cached();
        if (catalog != null && catalog.apps.contains(entry)) intent.addCategory(Intent.CATEGORY_LAUNCHER);
        start(intent, entry.label);
    }
    private void start(Intent intent, String title) {
        if (!canStart(intent)) { toast(getString(R.string.cannot_open, title)); return; }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try { startActivity(intent); finish(); }
        catch (ActivityNotFoundException | SecurityException error) { toast(getString(R.string.cannot_open, title)); }
    }

    private void restartConnectionTick() {
        main.removeCallbacks(connectionTick); main.removeCallbacks(signalsTick);
        if (resumed && page == ScreenFlow.Page.ADVANCED) { main.post(connectionTick); main.post(signalsTick); }
    }
    private void refreshSignals() {
        if (signalsReading) return;
        signalsReading = true;
        final int epoch = connectionEpoch;
        final Context app = getApplicationContext();
        new Thread(new Runnable() {
            @Override public void run() {
                final Map<String, String> values = DisplaySignals.read(app);
                main.post(new Runnable() { @Override public void run() {
                    signalsReading = false;
                    if (isFinishing() || !resumed || page != ScreenFlow.Page.ADVANCED || epoch != connectionEpoch
                        || signalsText == null) return;
                    if (signals == null) signals = new SignalLog(values); else signals.update(values);
                    signalsText.setText(TextUtils.join("\n", signals.lines()));
                } });
            }
        }, "launchpad-display-signals").start();
    }
    private void refreshConnection() {
        if (connectionReading) return;
        if (preview) { connection = PreviewData.connection(); updateConnectionViews(); return; }
        connectionReading = true;
        final int epoch = connectionEpoch;
        new Thread(new Runnable() {
            @Override public void run() {
                ConnectionSnapshot value;
                try {
                    WifiManager manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    if (manager == null) value = ConnectionSnapshot.unknown();
                    else {
                        WifiInfo info = manager.getConnectionInfo();
                        value = ConnectionSnapshot.read(manager.isWifiEnabled(), info != null
                            && info.getSupplicantState() == SupplicantState.COMPLETED,
                            info == null ? null : info.getSSID(), info == null ? 0 : info.getIpAddress(),
                            property("service.adb.tcp.port"), property("persist.adb.tcp.port"));
                    }
                } catch (SecurityException error) { value = ConnectionSnapshot.unknown(); }
                final ConnectionSnapshot result = value;
                main.post(new Runnable() { @Override public void run() {
                    connectionReading = false;
                    if (!isFinishing() && resumed && page == ScreenFlow.Page.ADVANCED && epoch == connectionEpoch) {
                        connection = result; updateConnectionViews();
                    }
                } });
            }
        }, "launchpad-connection").start();
    }
    private String property(String name) {
        try {
            Process process = new ProcessBuilder("/system/bin/getprop", name).start();
            String value;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                value = reader.readLine();
            }
            process.waitFor(); return value == null ? "" : value;
        } catch (Exception error) { return ""; }
    }
    private void updateConnectionViews() {
        if (page != ScreenFlow.Page.ADVANCED || statusText == null) return;
        String status = connection.state == ConnectionSnapshot.State.CONNECTED ? "Connected"
            : connection.state == ConnectionSnapshot.State.OFF ? "Wi-Fi off"
            : connection.state == ConnectionSnapshot.State.DISCONNECTED ? "Disconnected" : "Unavailable";
        statusText.setText(status); networkText.setText("Network / " + connection.network);
        ipText.setText(connection.ip); adbText.setText(connection.adb);
        ui.background(statusDot, ui.rounded(connection.state == ConnectionSnapshot.State.CONNECTED ? 0xff1acb59 : palette.idle, 7, 0));
        copyButton.setEnabled(connection.canCopy); copyButton.setAlpha(connection.canCopy ? 1f : 0.4f);
        if (knobIndex >= 0 && knobIndex < targets.size() && !targets.get(knobIndex).enabled()) moveKnob(1);
    }
    private void copyAddress() {
        if (!connection.canCopy) return;
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) { clipboard.setPrimaryClip(ClipData.newPlainText("ADB address", connection.adb)); toast("ADB address copied"); }
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        int code = event.getKeyCode(); boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
        if (code == KEY_TUNE_UP || code == KEY_TUNE_DOWN || code == KeyEvent.KEYCODE_DPAD_RIGHT || code == KeyEvent.KEYCODE_DPAD_LEFT) {
            if (down) {
                moveKnob(code == KEY_TUNE_UP || code == KeyEvent.KEYCODE_DPAD_RIGHT ? 1 : -1);
                knobOnToggle = page == ScreenFlow.Page.PRECONDITIONING && knobIndex == 0;
            }
            return true;
        }
        if (code == KEY_TUNE_ENTER || code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER) {
            if (down) {
                if (event.getRepeatCount() == 0) { enterDown = true; enterLong = false; enterAt = SystemClock.uptimeMillis(); }
                else enterLong = true;
            } else {
                if (enterDown && !enterLong && SystemClock.uptimeMillis() - enterAt < 800) activateKnob();
                enterDown = false; enterLong = false;
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
    }
    private boolean[] enabledTargets() {
        boolean[] values = new boolean[targets.size()];
        for (int i = 0; i < values.length; i++) values[i] = targets.get(i).enabled();
        return values;
    }
    private void moveKnob(int delta) { knobIndex = KnobFocus.next(knobIndex, delta, enabledTargets()); applyKnob(); }
    private void applyKnob() {
        int selectedGridPosition = -1;
        for (int i = 0; i < targets.size(); i++) {
            Target target = targets.get(i);
            if (target.view != null) {
                target.view.setActivated(i == knobIndex);
                target.view.setSelected(i == knobIndex);
            }
            else if (i == knobIndex) selectedGridPosition = target.gridPosition;
        }
        adapter.setKnobPosition(selectedGridPosition);
        if (grid != null && selectedGridPosition >= 0) grid.smoothScrollToPosition(selectedGridPosition);
        if (advancedScroll != null && knobIndex >= 0 && knobIndex < targets.size()) {
            View selected = targets.get(knobIndex).view;
            if (selected != null) selected.requestRectangleOnScreen(
                new android.graphics.Rect(0, 0, selected.getWidth(), selected.getHeight()), false);
        }
    }
    private void activateKnob() {
        if (knobIndex < 0) moveKnob(1);
        else if (knobIndex < targets.size() && targets.get(knobIndex).enabled()) targets.get(knobIndex).action.run();
    }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show(); }
    private static int keyCode(String name, int fallback) {
        int code = KeyEvent.keyCodeFromString(name); return code > 0 ? code : fallback;
    }
}
