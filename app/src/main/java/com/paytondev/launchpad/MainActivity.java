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
import android.database.ContentObserver;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ScrollView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/** Battery preconditioning screen, driven by touch and the TUNE knob; the external hold service is unchanged. */
public final class MainActivity extends Activity {
    static final String EXTRA_FROM_KNOB = "from_knob";
    private static final String APP_NAME = "Precondition";
    private static final int TAB_FAVORITES = 0, TAB_APPS = 1, TAB_ALL = 2;
    private static final int KEY_TUNE_UP = keyCode("KEYCODE_TUNE_UP", 223);
    private static final int KEY_TUNE_DOWN = keyCode("KEYCODE_TUNE_DOWN", 224);
    private static final int KEY_TUNE_ENTER = keyCode("KEYCODE_TUNE_ENTER", 243);
    private static final String[] MENU_TITLES = {"Battery", "Schedule", "Settings"};
    private static final String[] CHECK_LABELS = {"EV READY", "Battery conditioning", "Car indicator", "Firmware profile",
        "Temperature source", "Failure code"};
    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<Target> targets = new ArrayList<>();
    private final List<Target> headerTargets = new ArrayList<>();
    private FocusUi ui;
    private LinearLayout menu;
    private FrameLayout content;
    private Favorites favorites;
    private SharedPreferences prefs;
    private TileAdapter adapter;
    private GridView grid;
    private ScrollView scroller;
    private TextView empty, wifiValue, statusText, networkText, ipText, adbText;
    private View copyRow;
    private TextView batteryMeta, scheduleMeta;
    /** Battery page. */
    private TextView lowestValue, lowestUnit, lowestSub, highestText, statusPill, preconditionDetail, preconditionButton;
    private TemperatureGauge gauge;
    private CountdownBar countdown;
    private TemperatureChart chart;
    private CheckMark summaryMark;
    private TextView summaryTitle, summaryText;
    private int buttonStyle;
    /** Schedule page. */
    private PreconditioningSchedule.Mode scheduleMode = PreconditioningSchedule.Mode.START_BY;
    private int scheduleMinute;
    private final TextView[] modeChips = new TextView[2];
    private TextView schedulePill, scheduleDay, scheduleHint, schedulePreview, scheduleInputs, scheduleConfirm, scheduleCancel;
    /** Time picker: hour and minute wheels (previous, current, next) and the AM/PM pair. */
    private static final int EDIT_NONE = 0, EDIT_HOUR = 1, EDIT_MINUTE = 2;
    private final TextView[] hourDigits = new TextView[3], minuteDigits = new TextView[3];
    private LinearLayout hourWheel, minuteWheel;
    private TextView morningChip, afternoonChip;
    /** The wheel TUNE is turning, if any; and the target indexes a press moves the knob to. */
    private int editField = EDIT_NONE, minuteTarget, confirmTarget;
    /** Cancel was pressed; it stays disabled until the service drops the schedule. */
    private boolean cancelSent;
    /** Settings → Vehicle. */
    private final CheckMark[] checkMarks = new CheckMark[CHECK_LABELS.length];
    private final TextView[] checkValues = new TextView[CHECK_LABELS.length];
    private View failureRow, failureDivider;
    private PreconditioningSchedule schedule;
    private ScheduleLog scheduleLog = ScheduleLog.EMPTY;
    private Integer outsideC;
    private Palette palette = Palette.DARK;
    private AppearanceMode appearance = AppearanceMode.FOLLOW_CAR;
    private boolean carNight, signalsReading, compact, splitObserved;
    private int rightPadding;
    private TextView signalsText;
    private SignalLog signals;
    private PreconditioningService.LocalBinder vehicleService;
    private PreconditioningSnapshot preconditioning = PreconditioningSnapshot.checking();
    private BatteryTemperatureReading batteryTemperature = BatteryTemperatureReading.checking();
    private TemperatureHistory.Trace history = TemperatureHistory.Trace.EMPTY;
    private boolean vehicleBound;
    /** Earliest estimate of when the pending request started; snapshots only ever lag. */
    private long requestStartedAt = -1;
    private ConnectionSnapshot connection = ConnectionSnapshot.unknown();
    private ScreenFlow.Page page;
    private int tab = TAB_ALL, knobIndex = -1, connectionEpoch;
    private long resumedAt, enterAt;
    private boolean resumed, started, enterDown, enterLong, preview, receiverRegistered, connectionReading;
    /** Start keeps the knob while it is briefly disabled, until TUNE is turned away. */
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
            if (resumed && connectionPage()) {
                refreshConnection();
                main.postDelayed(this, 5000);
            }
        }
    };
    private final Runnable signalsTick = new Runnable() {
        @Override public void run() {
            if (resumed && page == ScreenFlow.Page.SIGNALS) {
                refreshSignals();
                main.postDelayed(this, 2000);
            }
        }
    };
    private final ContentObserver splitChanges = new ContentObserver(main) {
        @Override public void onChange(boolean selfChange) { applySplitScreen(); }
    };
    private final BroadcastReceiver wifiChanges = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (resumed && connectionPage()) refreshConnection();
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
            schedule = null; scheduleLog = ScheduleLog.EMPTY; outsideC = null;
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
                schedule = vehicleService.schedule();
                scheduleLog = vehicleService.scheduleLog();
                outsideC = vehicleService.outsideC();
            }
            updatePreconditioningViews(); main.postDelayed(this, 1000);
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_focus);
        menu = (LinearLayout) findViewById(R.id.focus_menu);
        content = (FrameLayout) findViewById(R.id.focus_content);
        prefs = getSharedPreferences("launchpad", MODE_PRIVATE);
        favorites = new Favorites(this);
        adapter = new TileAdapter(getLayoutInflater(), favorites);
        appearance = AppearanceMode.fromPreference(prefs.getString("appearance", null));
        carNight = DisplaySignals.night(this, getResources().getConfiguration());
        palette = Palette.of(appearance.dark(carNight));
        ui = new FocusUi(this, palette); adapter.setUi(ui); applyWindowColors();
        rightPadding = findViewById(R.id.focus_root).getPaddingRight();
        compact = splitScreenOn(); applySplitPadding();
        preview = PreviewData.enabled(getIntent());
        showPage(ScreenFlow.Page.BATTERY);
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
        // A day/night switch while the app was hidden may not have reached onConfigurationChanged.
        carNight = DisplaySignals.night(this, getResources().getConfiguration()); applyAppearance();
        try {
            getContentResolver().registerContentObserver(Settings.System.getUriFor(SplitScreen.SETTING), false, splitChanges);
            splitObserved = true;
        } catch (SecurityException e) { splitObserved = false; }
        applySplitScreen();
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
        if (splitObserved) { getContentResolver().unregisterContentObserver(splitChanges); splitObserved = false; }
        super.onPause();
    }
    @Override protected void onDestroy() {
        main.removeCallbacksAndMessages(null); super.onDestroy();
    }
    // uiMode is in configChanges, so a day/night switch arrives here instead of recreating.
    @Override public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        carNight = DisplaySignals.night(this, configuration); applyAppearance();
    }
    /** Rebuilds the current page in the resolved palette, keeping the TUNE selection. */
    private void applyAppearance() {
        Palette next = Palette.of(appearance.dark(carNight));
        if (next == palette) return;
        palette = next; ui = new FocusUi(this, palette); adapter.setUi(ui); applyWindowColors();
        rebuildPage();
    }
    private void rebuildPage() {
        int knob = knobIndex; showPage(page);
        if (knob >= 0 && knob < targets.size() && targets.get(knob).enabled()) { knobIndex = knob; applyKnob(); }
    }
    private boolean splitScreenOn() {
        try { return SplitScreen.on(Settings.System.getInt(getContentResolver(), SplitScreen.SETTING, 0)); }
        catch (SecurityException e) { return false; }
    }
    /** The map panel covers the right of this window; pad past it and use the compact layout. */
    private void applySplitScreen() {
        boolean next = splitScreenOn();
        if (next == compact) return;
        compact = next; applySplitPadding(); rebuildPage();
    }
    private void applySplitPadding() {
        View root = findViewById(R.id.focus_root);
        int right = compact ? SplitScreen.insetPx(getResources().getDisplayMetrics().widthPixels) : rightPadding;
        root.setPadding(root.getPaddingLeft(), root.getPaddingTop(), right, root.getPaddingBottom());
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) menu.getLayoutParams();
        params.width = ui.dp(compact ? 84 : 248); menu.setLayoutParams(params);
    }
    private void applyWindowColors() {
        findViewById(R.id.focus_root).setBackgroundColor(palette.background);
        findViewById(R.id.focus_rule).setBackgroundColor(palette.line);
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
        ScreenFlow.Page previous = ScreenFlow.back(page);
        if (previous == null) finish(); else showPage(previous);
    }

    private void showPage(ScreenFlow.Page next) {
        // A day/night rebuild of the signals page keeps the log: that switch is what it records.
        if (next != page || next != ScreenFlow.Page.SIGNALS) signals = null;
        if (next == ScreenFlow.Page.SCHEDULE && page != ScreenFlow.Page.SCHEDULE) resetScheduleDraft();
        page = next; connectionEpoch++;
        targets.clear(); headerTargets.clear(); grid = null; scroller = null;
        wifiValue = null; statusText = null; networkText = null; ipText = null; adbText = null; copyRow = null;
        lowestValue = null; lowestUnit = null; lowestSub = null; highestText = null; statusPill = null;
        preconditionDetail = null; preconditionButton = null; gauge = null; countdown = null; chart = null;
        summaryMark = null; summaryTitle = null; summaryText = null; buttonStyle = 0;
        schedulePill = null; scheduleDay = null; scheduleHint = null; schedulePreview = null; scheduleInputs = null;
        scheduleConfirm = null; scheduleCancel = null; Arrays.fill(modeChips, null);
        Arrays.fill(hourDigits, null); Arrays.fill(minuteDigits, null);
        hourWheel = null; minuteWheel = null; morningChip = null; afternoonChip = null; editField = EDIT_NONE;
        Arrays.fill(checkMarks, null); Arrays.fill(checkValues, null); failureRow = null; failureDivider = null;
        signalsText = null;
        content.removeAllViews();
        buildMenu();
        if (page == ScreenFlow.Page.BATTERY) buildBattery();
        else if (page == ScreenFlow.Page.SCHEDULE) buildSchedule();
        else if (page == ScreenFlow.Page.SETTINGS) buildSettings();
        else if (page == ScreenFlow.Page.WIFI) buildWifi();
        else if (page == ScreenFlow.Page.SIGNALS) buildSignals();
        else buildBrowser();
        if (page != ScreenFlow.Page.BROWSER) targets.addAll(headerTargets);
        knobOnToggle = page == ScreenFlow.Page.BATTERY;
        knobIndex = page == ScreenFlow.Page.SCHEDULE ? confirmTarget : KnobFocus.next(-1, 1, enabledTargets());
        updatePreconditioningViews();
        if (knobIndex >= 0 && knobIndex < targets.size() && !targets.get(knobIndex).enabled())
            knobIndex = KnobFocus.next(knobIndex, 1, enabledTargets());
        applyKnob();
        restartConnectionTick();
    }

    /** Left menu like the car's Settings screens; icons only while split screen leaves less room. */
    private void buildMenu() {
        menu.removeAllViews(); batteryMeta = null; scheduleMeta = null;
        LinearLayout title = ui.row(); title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(ui.dp(compact ? 0 : 12), 0, 0, 0);
        if (compact) title.setGravity(Gravity.CENTER);
        menu.addView(title, new LinearLayout.LayoutParams(-1, ui.dp(72)));
        FrameLayout back = new FrameLayout(this); ui.background(back, ui.quiet(0, 24));
        MenuGlyph backGlyph = new MenuGlyph(this, MenuGlyph.Kind.BACK); backGlyph.setColor(palette.muted);
        back.addView(backGlyph, new FrameLayout.LayoutParams(ui.dp(22), ui.dp(22), Gravity.CENTER));
        back.setContentDescription(page == ScreenFlow.Page.BATTERY ? "Close" : "Back");
        title.addView(back, new LinearLayout.LayoutParams(ui.dp(48), ui.dp(48)));
        Runnable backAction = new Runnable() { @Override public void run() { onBackPressed(); } };
        clickable(back, backAction); headerTargets.add(new Target(back, -1, backAction));
        if (!compact) {
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(-2, -2); nameParams.leftMargin = ui.dp(4);
            title.addView(ui.text(APP_NAME, 24, palette.ink, Fonts.Weight.LIGHT), nameParams);
        }
        ScreenFlow.Page current = ScreenFlow.menu(page);
        for (int i = 0; i < ScreenFlow.MENU.length; i++) {
            final ScreenFlow.Page destination = ScreenFlow.MENU[i];
            boolean selected = destination == current;
            LinearLayout row = ui.row(); row.setGravity(Gravity.CENTER_VERTICAL);
            ui.background(row, ui.quiet(selected ? palette.accentSoft : 0, 0));
            View bar = new View(this); bar.setBackgroundColor(selected ? palette.accent : 0);
            row.addView(bar, new LinearLayout.LayoutParams(ui.dp(4), ui.dp(40)));
            View glyph = menuIcon(destination, selected ? palette.ink : palette.muted);
            LinearLayout.LayoutParams glyphParams = new LinearLayout.LayoutParams(ui.dp(26), ui.dp(26));
            glyphParams.leftMargin = ui.dp(compact ? 23 : 18); row.addView(glyph, glyphParams);
            if (!compact) {
                TextView label = ui.text(MENU_TITLES[i], 19, selected ? palette.ink : palette.muted, Fonts.Weight.REGULAR);
                LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, -2, 1); labelParams.leftMargin = ui.dp(14);
                row.addView(label, labelParams);
                TextView meta = ui.text("", 14, palette.accent, Fonts.Weight.MEDIUM); meta.setSingleLine(true);
                LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(-2, -2); metaParams.rightMargin = ui.dp(18);
                row.addView(meta, metaParams);
                if (destination == ScreenFlow.Page.BATTERY) batteryMeta = meta;
                else if (destination == ScreenFlow.Page.SCHEDULE) scheduleMeta = meta;
            }
            row.setContentDescription(MENU_TITLES[i]);
            menu.addView(row, new LinearLayout.LayoutParams(-1, ui.dp(64)));
            Runnable action = new Runnable() { @Override public void run() { if (page != destination) showPage(destination); } };
            clickable(row, action); headerTargets.add(new Target(row, -1, action));
        }
    }

    /** Battery and Schedule are drawn; Settings is the Material Symbols gear, tinted like them. */
    private View menuIcon(ScreenFlow.Page destination, int color) {
        if (destination == ScreenFlow.Page.SETTINGS) {
            ImageView icon = new ImageView(this); icon.setImageResource(R.drawable.ic_settings);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER); icon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
            icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            return icon;
        }
        MenuGlyph glyph = new MenuGlyph(this, destination == ScreenFlow.Page.BATTERY ? MenuGlyph.Kind.BATTERY : MenuGlyph.Kind.SCHEDULE);
        glyph.setColor(color); return glyph;
    }

    private void buildBattery() {
        LinearLayout body = ui.row();
        content.addView(body, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout left = ui.column();
        body.addView(left, compact ? new LinearLayout.LayoutParams(0, -1, 1) : new LinearLayout.LayoutParams(ui.dp(420), -1));
        left.addView(ui.caption("Lowest pack temperature"));
        LinearLayout reading = ui.row(); reading.setBaselineAligned(false);
        lowestValue = ui.text("—", 84, palette.ink, Fonts.Weight.BOLD); lowestValue.setSingleLine(true);
        reading.addView(lowestValue);
        lowestUnit = ui.text("°F", 34, palette.ink, Fonts.Weight.MEDIUM);
        LinearLayout.LayoutParams unitParams = new LinearLayout.LayoutParams(-2, -2);
        unitParams.leftMargin = ui.dp(4); unitParams.topMargin = ui.dp(12); reading.addView(lowestUnit, unitParams);
        left.addView(reading, below(8));
        lowestSub = ui.text("Checking…", 16, palette.muted, false); lowestSub.setSingleLine(true);
        lowestSub.setEllipsize(TextUtils.TruncateAt.END);
        left.addView(lowestSub, below(4));
        statusPill = ui.pill();
        left.addView(statusPill, margins(new LinearLayout.LayoutParams(-2, ui.dp(32)), 0, 16));
        countdown = new CountdownBar(this); countdown.setPalette(palette); countdown.setVisibility(View.GONE);
        left.addView(countdown, margins(new LinearLayout.LayoutParams(-1, ui.dp(12)), 0, 8));
        preconditionDetail = ui.text("", 15, palette.muted, false);
        preconditionDetail.setLineSpacing(0, 1.1f); preconditionDetail.setMaxLines(2);
        preconditionDetail.setEllipsize(TextUtils.TruncateAt.END);
        left.addView(preconditionDetail, margins(new LinearLayout.LayoutParams(-1, -2), 0, 8));
        left.addView(new View(this), new LinearLayout.LayoutParams(-1, 0, 1));
        gauge = new TemperatureGauge(this); gauge.setPalette(palette);
        left.addView(gauge, new LinearLayout.LayoutParams(-1, ui.dp(40)));
        LinearLayout actions = ui.row(); actions.setGravity(Gravity.CENTER_VERTICAL);
        left.addView(actions, margins(new LinearLayout.LayoutParams(-1, ui.dp(64)), 0, 12));
        preconditionButton = ui.primaryButton("Start");
        addTarget(preconditionButton, new Runnable() { @Override public void run() { preconditioningAction(); } });
        preconditionButton.setEnabled(false); preconditionButton.setAlpha(0.4f);
        actions.addView(preconditionButton, compact ? new LinearLayout.LayoutParams(0, -1, 1)
            : new LinearLayout.LayoutParams(ui.dp(220), -1));
        FrameLayout scheduleButton = new FrameLayout(this); ui.background(scheduleButton, ui.secondary(12));
        MenuGlyph clock = new MenuGlyph(this, MenuGlyph.Kind.SCHEDULE); clock.setColor(palette.ink);
        scheduleButton.addView(clock, new FrameLayout.LayoutParams(ui.dp(30), ui.dp(30), Gravity.CENTER));
        scheduleButton.setContentDescription("Schedule preconditioning");
        actions.addView(scheduleButton, margins(new LinearLayout.LayoutParams(ui.dp(64), -1), 14, 0));
        addTarget(scheduleButton, new Runnable() { @Override public void run() { showPage(ScreenFlow.Page.SCHEDULE); } });
        if (compact) return;

        LinearLayout right = ui.column();
        body.addView(right, margins(new LinearLayout.LayoutParams(0, -1, 1), 24, 0));
        LinearLayout chartCard = ui.card();
        right.addView(chartCard, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout chartHeader = ui.row(); chartHeader.setGravity(Gravity.CENTER_VERTICAL);
        chartHeader.addView(ui.caption("Last 30 minutes"), new LinearLayout.LayoutParams(0, -2, 1));
        highestText = ui.text("", 14, palette.muted, false); chartHeader.addView(highestText);
        chartCard.addView(chartHeader);
        chart = new TemperatureChart(this); chart.setPalette(palette);
        chartCard.addView(chart, margins(new LinearLayout.LayoutParams(-1, 0, 1), 0, 10));

        LinearLayout summary = ui.row(); summary.setGravity(Gravity.CENTER_VERTICAL);
        summary.setPadding(ui.dp(18), 0, ui.dp(14), 0);
        ui.background(summary, ui.secondary(16));
        right.addView(summary, margins(new LinearLayout.LayoutParams(-1, ui.dp(60)), 0, 14));
        summaryMark = new CheckMark(this); summaryMark.setPalette(palette);
        summary.addView(summaryMark, new LinearLayout.LayoutParams(ui.dp(20), ui.dp(20)));
        summaryTitle = ui.text("Checking car", 17, palette.ink, false); summaryTitle.setSingleLine(true);
        summary.addView(summaryTitle, margins(new LinearLayout.LayoutParams(-2, -2), 12, 0));
        summaryText = ui.text("", 15, palette.muted, false); summaryText.setSingleLine(true);
        summaryText.setEllipsize(TextUtils.TruncateAt.END);
        summary.addView(summaryText, margins(new LinearLayout.LayoutParams(0, -2, 1), 12, 0));
        MenuGlyph more = new MenuGlyph(this, MenuGlyph.Kind.MORE); more.setColor(palette.faint);
        summary.addView(more, new LinearLayout.LayoutParams(ui.dp(18), ui.dp(18)));
        summary.setContentDescription("Vehicle checks");
        addTarget(summary, new Runnable() { @Override public void run() { showPage(ScreenFlow.Page.SETTINGS); } });
    }
    private LinearLayout.LayoutParams below(int margin) {
        return margins(new LinearLayout.LayoutParams(-2, -2), 0, margin);
    }
    private LinearLayout.LayoutParams margins(LinearLayout.LayoutParams params, int left, int top) {
        params.leftMargin = ui.dp(left); params.topMargin = ui.dp(top); return params;
    }

    private void preconditioningAction() {
        // Start while a schedule is armed: the service cancels the schedule first.
        if (!preconditioning.canStop && !preconditioning.canStart) return;
        String action = preconditioning.canStop ? PreconditioningService.STOP
            : preconditioning.state == PreconditioningSnapshot.State.WARM
                ? PreconditioningService.START_ANYWAY : PreconditioningService.START;
        startService(new Intent(this, PreconditioningService.class).setAction(action));
        // Serialize presses until the service publishes the result.
        if (preconditionButton != null) preconditionButton.setEnabled(false);
    }
    /** The car is heating the pack: our request is active, or the car started it itself. */
    private boolean heating() {
        return preconditioning.state == PreconditioningSnapshot.State.ACTIVE
            || (preconditioning.state == PreconditioningSnapshot.State.IDLE && preconditioning.indicator == 1);
    }
    private boolean scheduleArmed() { return schedule != null && !preconditioning.requested; }

    private void updatePreconditioningViews() {
        long now = SystemClock.elapsedRealtime();
        long wall = WallClock.now();
        boolean twentyFourHour = DateFormat.is24HourFormat(this);
        TimeZone zone = TimeZone.getDefault();
        boolean armed = scheduleArmed();
        if (!preconditioning.requested) requestStartedAt = -1;
        else if (requestStartedAt < 0 || now - preconditioning.elapsedMs < requestStartedAt)
            requestStartedAt = now - preconditioning.elapsedMs;
        boolean waiting = preconditioning.state == PreconditioningSnapshot.State.WAITING;
        if (batteryMeta != null) {
            batteryMeta.setText(waiting ? "Starting" : heating() && preconditioning.requested ? preconditioning.elapsed() : "");
            batteryMeta.setTextColor(waiting ? palette.accent : palette.warm);
        }
        if (scheduleMeta != null) scheduleMeta.setText(armed ? ScheduleFormat.time(schedule.targetAt, twentyFourHour, zone) : "");
        if (statusPill != null) updateBatteryViews(now, wall, twentyFourHour, zone, armed, waiting);
        if (hourWheel != null) renderSchedule();
        if (checkMarks[0] != null) updateChecks();
    }
    private void updateBatteryViews(long now, long wall, boolean twentyFourHour, TimeZone zone, boolean armed, boolean waiting) {
        PreconditioningSnapshot.State state = preconditioning.state;
        String detail = preconditioning.detail;
        if (waiting) {
            int left = (int) Math.ceil(CountdownBar.remaining(requestStartedAt, now) * CountdownBar.DURATION_MS / 1000f);
            ui.setPill(statusPill, String.format(Locale.US, "Starting · %d:%02d left", left / 60, left % 60),
                palette.accent, palette.accentSoft);
            detail = "Sent to the car. It usually starts within a minute.";
        } else if (heating()) {
            ui.setPill(statusPill, preconditioning.requested ? "Conditioning · " + preconditioning.elapsed()
                : "Conditioning · started by the car", palette.warm, palette.warmSoft);
        } else if (armed) {
            ui.setPill(statusPill, "Scheduled · " + schedule.title() + " " + ScheduleFormat.time(schedule.targetAt, twentyFourHour, zone),
                palette.accent, palette.accentSoft);
            ScheduleLog.Event last = scheduleLog.latest();
            // Past the send time the newest "car not ready" line explains the wait better than the plan.
            boolean deferred = schedule.plannedSendAt != PreconditioningSchedule.NONE && wall >= schedule.plannedSendAt
                && last != null && last.kind == ScheduleLog.Kind.DEFERRED;
            detail = deferred ? last.text : ScheduleFormat.sendLine(schedule, wall) + ". " + schedule.reason;
        } else if (state == PreconditioningSnapshot.State.WARM) {
            ui.setPill(statusPill, preconditioning.title(), palette.warm, palette.warmSoft);
        } else if (state == PreconditioningSnapshot.State.ERROR || state == PreconditioningSnapshot.State.UNAVAILABLE
            || state == PreconditioningSnapshot.State.UNKNOWN || state == PreconditioningSnapshot.State.TIMED_OUT) {
            ui.setPill(statusPill, preconditioning.title(), palette.danger, palette.raised);
        } else {
            String title = preconditioning.title();
            if (state == PreconditioningSnapshot.State.ENDED) title += " · " + preconditioning.elapsed();
            ui.setPill(statusPill, title, palette.ink, palette.raised);
        }
        preconditionDetail.setText(detail);
        preconditionDetail.setVisibility(TextUtils.isEmpty(detail) ? View.GONE : View.VISIBLE);
        countdown.setVisibility(waiting ? View.VISIBLE : View.GONE);
        countdown.setStartedAt(waiting ? requestStartedAt : -1);

        Integer minimum = batteryTemperature.minimumC;
        boolean stale = batteryTemperature.stale(now);
        lowestValue.setText(minimum == null ? "—" : String.valueOf(BatteryTemperatureReading.fahrenheit(minimum)));
        lowestValue.setTextColor(stale ? palette.muted : palette.ink);
        lowestUnit.setVisibility(minimum == null ? View.GONE : View.VISIBLE);
        lowestSub.setText(minimum == null ? batteryTemperature.age(now) : minimum + "°C · " + batteryTemperature.age(now));
        int threshold = warmThreshold();
        gauge.setValue(minimum, threshold, heating());
        if (chart != null) {
            chart.setTrace(history, now, threshold, heating());
            TemperatureHistory.Sample latest = history.latest();
            Integer maximum = latest == null || stale || latest.at != batteryTemperature.receivedAt ? null : latest.maximumC;
            highestText.setText(maximum == null ? "" : "Highest " + BatteryTemperatureReading.fahrenheit(maximum) + "°F");
        }
        if (summaryMark != null) {
            boolean unsupported = preconditioning.supported == PreconditioningSnapshot.FAIL;
            int ready = unsupported ? PreconditioningSnapshot.FAIL : preconditioning.readyCheck();
            summaryMark.setState(ready);
            summaryTitle.setText(unsupported ? "Firmware not supported" : ready == PreconditioningSnapshot.PASS ? "Car ready"
                : ready == PreconditioningSnapshot.FAIL ? "Car not ready" : "Checking car");
            String enabled = preconditioning.enabledValue();
            summaryText.setText(unsupported ? preconditioning.supportedValue() : preconditioning.readyValue()
                + " · Conditioning " + Character.toLowerCase(enabled.charAt(0)) + enabled.substring(1));
        }

        boolean warm = preconditioning.state == PreconditioningSnapshot.State.WARM;
        preconditionButton.setText(warm ? "Start anyway" : preconditioning.requested ? "Stop" : "Start");
        preconditionButton.setContentDescription(warm ? "Start preconditioning anyway"
            : preconditioning.requested ? "Stop preconditioning" : "Start preconditioning");
        int style = preconditioning.requested ? 2 : 1;
        if (style != buttonStyle) {
            buttonStyle = style;
            ui.background(preconditionButton, style == 2 ? ui.secondary(12) : ui.primary(12));
            preconditionButton.setTextColor(style == 2 ? palette.warm : palette.onAccent);
        }
        boolean enabled = preconditioning.canStart || preconditioning.canStop;
        preconditionButton.setEnabled(enabled); preconditionButton.setAlpha(enabled ? 1f : 0.4f);
        if (knobIndex >= 0 && knobIndex < targets.size() && !targets.get(knobIndex).enabled()) moveKnob(1);
        // Start is the first target on the Battery page.
        if (enabled && knobOnToggle && knobIndex != 0) { knobIndex = 0; applyKnob(); }
    }
    private int warmThreshold() {
        return vehicleService == null ? BatteryTemperature.EXPECTED_HEATING_THRESHOLD_C : vehicleService.warmThresholdC();
    }
    private Integer freshMinimumC() {
        return batteryTemperature.stale(SystemClock.elapsedRealtime()) ? null : batteryTemperature.minimumC;
    }
    private static String fahrenheit(Integer celsius) {
        return celsius == null ? "—" : BatteryTemperatureReading.fahrenheit(celsius) + "°F";
    }

    /** The draft starts from the armed schedule, or half an hour from now. */
    private void resetScheduleDraft() {
        TimeZone zone = TimeZone.getDefault();
        if (schedule != null) {
            scheduleMode = schedule.mode; scheduleMinute = ScheduleFormat.minuteOfDay(schedule.targetAt, zone);
        } else scheduleMinute = ScheduleFormat.roundUpToFive(ScheduleFormat.minuteOfDay(WallClock.now() + 30 * 60_000L, zone));
    }
    private void buildSchedule() {
        LinearLayout body = ui.column();
        content.addView(body, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout header = ui.row(); header.setGravity(Gravity.CENTER_VERTICAL);
        body.addView(header, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout modes = ui.row(); modes.setPadding(ui.dp(4), ui.dp(4), ui.dp(4), ui.dp(4));
        ui.background(modes, ui.rounded(palette.raised, 12, palette.line));
        header.addView(modes);
        String[] labels = {"Start by", "Arrive by"};
        for (int i = 0; i < modeChips.length; i++) {
            final PreconditioningSchedule.Mode mode = PreconditioningSchedule.Mode.values()[i];
            modeChips[i] = ui.text(labels[i], 17, palette.muted, Fonts.Weight.MEDIUM); modeChips[i].setGravity(Gravity.CENTER);
            modes.addView(modeChips[i], new LinearLayout.LayoutParams(ui.dp(132), ui.dp(44)));
            addTarget(modeChips[i], new Runnable() { @Override public void run() { scheduleMode = mode; renderSchedule(); } });
        }
        header.addView(new View(this), new LinearLayout.LayoutParams(0, 0, 1));
        schedulePill = ui.pill(); header.addView(schedulePill, new LinearLayout.LayoutParams(-2, ui.dp(32)));

        LinearLayout middle = ui.row(); middle.setGravity(Gravity.CENTER_VERTICAL);
        body.addView(middle, margins(new LinearLayout.LayoutParams(-1, -2), 0, 10));
        boolean twelveHour = !DateFormat.is24HourFormat(this);
        int wheelWidth = compact ? 112 : 150;
        hourWheel = wheel(hourDigits, EDIT_HOUR, "Hour");
        middle.addView(hourWheel, new LinearLayout.LayoutParams(ui.dp(wheelWidth), -2));
        TextView colon = ui.text(":", compact ? 60 : 72, palette.muted, Fonts.Weight.LIGHT);
        middle.addView(colon, margins(new LinearLayout.LayoutParams(-2, -2), 2, 0));
        minuteTarget = targets.size();
        minuteWheel = wheel(minuteDigits, EDIT_MINUTE, "Minutes");
        middle.addView(minuteWheel, margins(new LinearLayout.LayoutParams(ui.dp(wheelWidth), -2), 2, 0));
        if (twelveHour) {
            LinearLayout halves = ui.column();
            middle.addView(halves, margins(new LinearLayout.LayoutParams(-2, -2), 12, 0));
            morningChip = halfChip(halves, "AM", false); afternoonChip = halfChip(halves, "PM", true);
        }

        LinearLayout plan = ui.card();
        middle.addView(plan, margins(new LinearLayout.LayoutParams(0, -2, 1), compact ? 16 : 28, 0));
        plan.addView(ui.caption("Plan"));
        schedulePreview = ui.text("", 18, palette.ink, false);
        schedulePreview.setLineSpacing(0, 1.1f); schedulePreview.setMaxLines(3); schedulePreview.setEllipsize(TextUtils.TruncateAt.END);
        plan.addView(schedulePreview, margins(new LinearLayout.LayoutParams(-1, -2), 0, 8));
        scheduleInputs = ui.text("", 15, palette.muted, false); scheduleInputs.setSingleLine(true);
        scheduleInputs.setEllipsize(TextUtils.TruncateAt.END);
        plan.addView(scheduleInputs, margins(new LinearLayout.LayoutParams(-1, -2), 0, 6));

        LinearLayout status = ui.row(); status.setGravity(Gravity.CENTER_VERTICAL);
        body.addView(status, margins(new LinearLayout.LayoutParams(-1, -2), 0, 6));
        scheduleDay = ui.text("", 16, palette.muted, false);
        status.addView(scheduleDay, new LinearLayout.LayoutParams(ui.dp(wheelWidth * 2 + 30), -2));
        scheduleDay.setGravity(Gravity.CENTER_HORIZONTAL);
        scheduleHint = ui.text("", 15, palette.accent, Fonts.Weight.MEDIUM); scheduleHint.setSingleLine(true);
        status.addView(scheduleHint, margins(new LinearLayout.LayoutParams(0, -2, 1), 16, 0));

        body.addView(new View(this), new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout actions = ui.row();
        body.addView(actions, new LinearLayout.LayoutParams(-1, ui.dp(64)));
        scheduleConfirm = ui.primaryButton("Schedule"); scheduleConfirm.setPadding(ui.dp(28), 0, ui.dp(28), 0);
        scheduleConfirm.setMinWidth(ui.dp(220));
        actions.addView(scheduleConfirm, new LinearLayout.LayoutParams(-2, -1));
        confirmTarget = targets.size();
        addTarget(scheduleConfirm, new Runnable() { @Override public void run() { confirmSchedule(); } });
        scheduleCancel = ui.secondaryButton("Cancel schedule", palette.warm);
        actions.addView(scheduleCancel, margins(new LinearLayout.LayoutParams(-2, -1), 14, 0));
        addTarget(scheduleCancel, new Runnable() { @Override public void run() {
            startService(new Intent(MainActivity.this, PreconditioningService.class).setAction(PreconditioningService.SCHEDULE_CANCEL));
            cancelSent = true; renderSchedule();
        } });
    }
    /**
     * One wheel: the current value large, its neighbours small above and below. Tapping a neighbour steps
     * to it. TUNE selects the wheel, a press grabs it so turning changes it, and the next press moves on.
     */
    private LinearLayout wheel(TextView[] digits, final int field, String label) {
        LinearLayout wheel = ui.column(); wheel.setGravity(Gravity.CENTER_HORIZONTAL);
        wheel.setPadding(0, ui.dp(4), 0, ui.dp(8)); wheel.setContentDescription(label);
        for (int i = 0; i < digits.length; i++) {
            boolean current = i == 1;
            digits[i] = ui.text("", current ? compact ? 76 : 88 : 30, current ? palette.ink : palette.faint,
                current ? Fonts.Weight.SEMI_BOLD : Fonts.Weight.LIGHT);
            digits[i].setGravity(Gravity.CENTER); digits[i].setSingleLine(true);
            wheel.addView(digits[i], new LinearLayout.LayoutParams(-1, -2));
        }
        clickable(digits[0], new Runnable() { @Override public void run() { stepWheel(field, -1); } });
        clickable(digits[2], new Runnable() { @Override public void run() { stepWheel(field, 1); } });
        digits[0].setContentDescription("Earlier"); digits[2].setContentDescription("Later");
        // A knob-only target: touch uses the neighbours, so the wheel itself isn't clickable.
        wheel.setFocusable(false);
        targets.add(new Target(wheel, -1, new Runnable() { @Override public void run() { pressWheel(field); } }));
        return wheel;
    }
    private TextView halfChip(LinearLayout column, String label, final boolean afternoon) {
        TextView chip = ui.text(label, 18, palette.muted, Fonts.Weight.MEDIUM); chip.setGravity(Gravity.CENTER);
        column.addView(chip, margins(new LinearLayout.LayoutParams(ui.dp(76), ui.dp(52)), 0, afternoon ? 10 : 0));
        addTarget(chip, new Runnable() { @Override public void run() {
            scheduleMinute = ScheduleFormat.withAfternoon(scheduleMinute, afternoon); renderSchedule();
        } });
        return chip;
    }
    private void stepWheel(int field, int steps) {
        scheduleMinute = field == EDIT_HOUR ? ScheduleFormat.stepHour(scheduleMinute, steps)
            : ScheduleFormat.stepMinute(scheduleMinute, steps);
        renderSchedule();
    }
    /** Press on a wheel: grab it; press again on the hour to move to minutes, on the minutes to finish. */
    private void pressWheel(int field) {
        if (editField != field) editField = field;
        else if (field == EDIT_HOUR) { editField = EDIT_MINUTE; knobIndex = minuteTarget; }
        else {
            editField = EDIT_NONE;
            if (targets.get(confirmTarget).enabled()) knobIndex = confirmTarget;
        }
        applyKnob(); renderSchedule();
    }
    private void renderSchedule() {
        long wall = WallClock.now(); TimeZone zone = TimeZone.getDefault();
        boolean twentyFourHour = DateFormat.is24HourFormat(this);
        for (int i = 0; i < modeChips.length; i++) {
            boolean selected = PreconditioningSchedule.Mode.values()[i] == scheduleMode;
            modeChips[i].setTextColor(selected ? palette.accent : palette.muted);
            ui.background(modeChips[i], ui.quiet(selected ? palette.accentSoft : 0, 9));
        }
        boolean armed = scheduleArmed();
        schedulePill.setVisibility(armed ? View.VISIBLE : View.GONE);
        if (armed) ui.setPill(schedulePill, "Scheduled · " + schedule.title() + " "
            + ScheduleFormat.time(schedule.targetAt, twentyFourHour, zone), palette.accent, palette.accentSoft);
        long target = ScheduleFormat.nextOccurrence(scheduleMinute, wall, zone);
        for (int i = 0; i < 3; i++) {
            hourDigits[i].setText(ScheduleFormat.hourLabel(ScheduleFormat.stepHour(scheduleMinute, i - 1), twentyFourHour));
            minuteDigits[i].setText(ScheduleFormat.minuteLabel(ScheduleFormat.stepMinute(scheduleMinute, i - 1)));
        }
        styleWheel(hourWheel, hourDigits[1], editField == EDIT_HOUR);
        styleWheel(minuteWheel, minuteDigits[1], editField == EDIT_MINUTE);
        if (morningChip != null) {
            boolean afternoon = scheduleMinute >= 12 * 60;
            styleHalf(morningChip, !afternoon); styleHalf(afternoonChip, afternoon);
        }
        boolean onWheel = knobIndex >= 0 && knobIndex < targets.size()
            && (targets.get(knobIndex).view == hourWheel || targets.get(knobIndex).view == minuteWheel);
        scheduleHint.setText(editField == EDIT_HOUR ? "Turn to change the hour · press for minutes"
            : editField == EDIT_MINUTE ? "Turn to change minutes · press to finish" : onWheel ? "Press to change" : "");
        String day = ScheduleFormat.dayHint(target, wall, zone).trim();
        scheduleDay.setText(day.isEmpty() ? "Today" : Character.toUpperCase(day.charAt(0)) + day.substring(1));
        boolean acceptable = PreconditioningSchedule.acceptable(target, wall);
        // Scheduling for later is fine while the car isn't READY; not during a request.
        boolean allowed = acceptable && vehicleService != null && !preconditioning.requested;
        scheduleConfirm.setText(schedule != null ? "Update schedule" : "Schedule");
        scheduleConfirm.setEnabled(allowed); scheduleConfirm.setAlpha(allowed ? 1f : 0.4f);
        schedulePreview.setText(preconditioning.requested ? "Stop the current session to schedule another."
            : planText(target, wall, twentyFourHour, zone, acceptable));
        // Temperatures only matter to an arrive-by plan.
        boolean inputs = scheduleMode == PreconditioningSchedule.Mode.ARRIVE_BY;
        scheduleInputs.setText("Battery " + fahrenheit(freshMinimumC()) + " · Outside " + fahrenheit(outsideC));
        scheduleInputs.setVisibility(inputs ? View.VISIBLE : View.GONE);
        if (!armed) cancelSent = false;
        scheduleCancel.setVisibility(armed ? View.VISIBLE : View.GONE);
        scheduleCancel.setEnabled(armed && !cancelSent);
        if (knobIndex >= 0 && knobIndex < targets.size() && !targets.get(knobIndex).enabled()) moveKnob(-1);
    }
    /** A grabbed wheel keeps a solid tint and an accent value; otherwise it tints only while selected. */
    private void styleWheel(LinearLayout wheel, TextView value, boolean editing) {
        ui.background(wheel, editing ? ui.rounded(palette.focus, 16, 0) : ui.quiet(0, 16));
        value.setTextColor(editing ? palette.accent : palette.ink);
    }
    private void styleHalf(TextView chip, boolean selected) {
        chip.setTextColor(selected ? palette.accent : palette.muted);
        ui.background(chip, selected ? ui.quiet(palette.accentSoft, 10) : ui.secondary(10));
    }
    private String planText(long target, long wall, boolean twentyFourHour, TimeZone zone, boolean acceptable) {
        if (!acceptable) return "Pick a time at least 2 minutes away.";
        String text;
        if (scheduleMode == PreconditioningSchedule.Mode.START_BY) {
            text = "Preconditioning will be running by " + ScheduleFormat.time(target, twentyFourHour, zone) + ".";
        } else {
            ArriveByPlanner.Plan plan = ArriveByPlanner.plan(target, freshMinimumC(), outsideC, warmThreshold(), wall);
            text = plan.alreadyWarm || plan.lateStart ? plan.reason
                : "Starts at " + ScheduleFormat.time(plan.sendAt, twentyFourHour, zone) + ". " + plan.reason;
        }
        if (!preconditioning.canStart) text += " The car needs to be on then.";
        return text;
    }
    private void confirmSchedule() {
        long wall = WallClock.now();
        long target = ScheduleFormat.nextOccurrence(scheduleMinute, wall, TimeZone.getDefault());
        if (!PreconditioningSchedule.acceptable(target, wall)) return;
        startService(new Intent(this, PreconditioningService.class).setAction(PreconditioningService.SCHEDULE_SET)
            .putExtra(PreconditioningService.EXTRA_MODE, scheduleMode.name())
            .putExtra(PreconditioningService.EXTRA_TARGET, target));
        showPage(ScreenFlow.Page.BATTERY);
    }

    private void buildSettings() {
        LinearLayout body = ui.row();
        content.addView(body, new FrameLayout.LayoutParams(-1, -1));
        scroller = new ScrollView(this); scroller.setFillViewport(true); scroller.setVerticalScrollBarEnabled(false);
        body.addView(scroller, new LinearLayout.LayoutParams(0, -1, 1));
        LinearLayout list = ui.column(); scroller.addView(list, new ScrollView.LayoutParams(-1, -2));
        list.addView(sectionCaption("Display", 0));
        LinearLayout look = settingsRow(list, "Appearance", appearance.title, false);
        addTarget(look, new Runnable() { @Override public void run() {
            AppearanceMode[] modes = AppearanceMode.values();
            appearance = modes[(appearance.ordinal() + 1) % modes.length];
            prefs.edit().putString("appearance", appearance.name()).apply();
            Palette before = palette; applyAppearance();
            // Same palette: the page wasn't rebuilt, so show the new value here.
            if (before == palette) rebuildPage();
        } });
        list.addView(sectionCaption("Tools", 18));
        LinearLayout wifi = settingsRow(list, "Wi-Fi & ADB", "Checking", true);
        wifiValue = (TextView) wifi.findViewWithTag("value");
        addTarget(wifi, new Runnable() { @Override public void run() { showPage(ScreenFlow.Page.WIFI); } });
        list.addView(ui.divider(false));
        LinearLayout screens = settingsRow(list, "All screens", "", true);
        addTarget(screens, new Runnable() { @Override public void run() { tab = TAB_ALL; showPage(ScreenFlow.Page.BROWSER); } });
        list.addView(ui.divider(false));
        LinearLayout nemesis = settingsRow(list, "NEMESIS Manager", "", true);
        ui.available(nemesis, available(ShortcutSpec.NEMESIS)); addTarget(nemesis, shortcutAction(ShortcutSpec.NEMESIS));
        list.addView(ui.divider(false));
        LinearLayout probe = settingsRow(list, "Appearance signals", "", true);
        addTarget(probe, new Runnable() { @Override public void run() { showPage(ScreenFlow.Page.SIGNALS); } });

        LinearLayout vehicle = ui.column();
        body.addView(vehicle, margins(new LinearLayout.LayoutParams(0, -1, 1), 28, 0));
        vehicle.addView(sectionCaption("Vehicle", 0));
        for (int i = 0; i < CHECK_LABELS.length; i++) {
            LinearLayout row = ui.row(); row.setGravity(Gravity.CENTER_VERTICAL);
            View divider = ui.divider(false);
            if (i > 0) vehicle.addView(divider);
            checkMarks[i] = new CheckMark(this); checkMarks[i].setPalette(palette);
            row.addView(checkMarks[i], new LinearLayout.LayoutParams(ui.dp(20), ui.dp(20)));
            TextView label = ui.text(CHECK_LABELS[i], 17, palette.ink, false); label.setSingleLine(true);
            row.addView(label, margins(new LinearLayout.LayoutParams(-2, -2), 12, 0));
            checkValues[i] = ui.text("Unknown", 15, palette.muted, false); checkValues[i].setSingleLine(true);
            checkValues[i].setEllipsize(TextUtils.TruncateAt.END); checkValues[i].setGravity(Gravity.RIGHT);
            row.addView(checkValues[i], margins(new LinearLayout.LayoutParams(0, -2, 1), 12, 0));
            vehicle.addView(row, new LinearLayout.LayoutParams(-1, ui.dp(50)));
            if (i == CHECK_LABELS.length - 1) { failureRow = row; failureDivider = divider; }
        }
        vehicle.addView(new View(this), new LinearLayout.LayoutParams(-1, 0, 1));
        vehicle.addView(ui.text(APP_NAME + " " + versionName(), 13, palette.faint, false));
        updateConnectionViews();
    }
    private TextView sectionCaption(String label, int top) {
        TextView caption = ui.caption(label);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = ui.dp(top); params.bottomMargin = ui.dp(6); caption.setLayoutParams(params);
        return caption;
    }
    private LinearLayout settingsRow(LinearLayout parent, String label, String value, boolean chevron) {
        LinearLayout row = ui.listRow(label, value, chevron);
        parent.addView(row, new LinearLayout.LayoutParams(-1, ui.dp(56)));
        return row;
    }
    private String versionName() {
        try { return getPackageManager().getPackageInfo(getPackageName(), 0).versionName; }
        catch (PackageManager.NameNotFoundException e) { return ""; }
    }
    private void updateChecks() {
        Integer minimum = batteryTemperature.minimumC;
        setCheck(0, preconditioning.readyCheck(), preconditioning.readyValue());
        setCheck(1, preconditioning.enabledCheck(), preconditioning.enabledValue());
        setCheck(2, preconditioning.indicatorCheck(), preconditioning.indicatorValue());
        setCheck(3, preconditioning.supported, preconditioning.supportedValue());
        setCheck(4, minimum != null ? PreconditioningSnapshot.PASS : batteryTemperature.attempted
            ? PreconditioningSnapshot.FAIL : PreconditioningSnapshot.UNKNOWN,
            minimum != null ? "Native" : batteryTemperature.attempted ? "Not available on this firmware" : "Checking");
        int failure = preconditioning.failureRaw >= 0 ? View.VISIBLE : View.GONE;
        failureRow.setVisibility(failure); failureDivider.setVisibility(failure);
        setCheck(5, PreconditioningSnapshot.UNKNOWN, "Raw " + preconditioning.failureRaw + " · meaning unknown");
    }
    private void setCheck(int index, int state, String value) {
        checkMarks[index].setState(state); checkValues[index].setText(value);
    }

    private void buildWifi() {
        LinearLayout list = ui.column();
        content.addView(list, new FrameLayout.LayoutParams(compact ? -1 : ui.dp(620), -2));
        list.addView(sectionCaption("Wi-Fi & ADB", 0));
        statusText = (TextView) settingsRow(list, "Status", "Checking", false).findViewWithTag("value");
        list.addView(ui.divider(false));
        networkText = (TextView) settingsRow(list, "Network", "—", false).findViewWithTag("value");
        list.addView(ui.divider(false));
        ipText = (TextView) settingsRow(list, "IP address", "—", false).findViewWithTag("value");
        list.addView(ui.divider(false));
        LinearLayout adb = settingsRow(list, "ADB address", "—", false);
        adbText = (TextView) adb.findViewWithTag("value"); copyRow = adb;
        adb.setContentDescription("Copy ADB address");
        addTarget(adb, new Runnable() { @Override public void run() { copyAddress(); } });
        list.addView(ui.divider(false));
        LinearLayout settings = settingsRow(list, ShortcutSpec.WIFI.title, "", true);
        ui.available(settings, available(ShortcutSpec.WIFI)); addTarget(settings, shortcutAction(ShortcutSpec.WIFI));
        updateConnectionViews();
    }

    private void buildSignals() {
        LinearLayout body = ui.column();
        content.addView(body, new FrameLayout.LayoutParams(-1, -1));
        body.addView(sectionCaption("Appearance signals", 0));
        TextView hint = ui.text("Read-only. Switch the Hyundai display between Day and Night (and Auto with the "
            + "headlights on and off), then report which lines changed.", 16, palette.muted, false);
        hint.setLineSpacing(0, 1.1f); body.addView(hint);
        scroller = new ScrollView(this);
        body.addView(scroller, margins(new LinearLayout.LayoutParams(-1, 0, 1), 0, 14));
        signalsText = ui.text("Reading…", 14, palette.ink, false);
        signalsText.setTypeface(android.graphics.Typeface.MONOSPACE); signalsText.setLineSpacing(0, 1.15f);
        scroller.addView(signalsText);
    }

    private void buildBrowser() {
        LinearLayout body = ui.column(); content.addView(body, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout browserToolbar = ui.row(); browserToolbar.setGravity(Gravity.CENTER_VERTICAL);
        body.addView(browserToolbar, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout tabs = ui.row(); tabs.setPadding(ui.dp(4), ui.dp(4), ui.dp(4), ui.dp(4));
        ui.background(tabs, ui.rounded(palette.raised, 12, palette.line));
        browserToolbar.addView(tabs);
        String[] names = {"Favorites", "Apps", "All screens"};
        for (int i = 0; i < names.length; i++) {
            final int selection = i;
            TextView button = ui.text(names[i], 17, tab == i ? palette.accent : palette.muted, Fonts.Weight.MEDIUM);
            button.setGravity(Gravity.CENTER);
            ui.background(button, ui.quiet(tab == i ? palette.accentSoft : 0, 9));
            tabs.addView(button, new LinearLayout.LayoutParams(ui.dp(compact ? 112 : 132), ui.dp(44)));
            addTarget(button, new Runnable() { @Override public void run() {
                tab = selection; prefs.edit().putInt("tab", tab).apply(); showPage(ScreenFlow.Page.BROWSER);
            } });
        }
        TextView hint = ui.text("Hold an app to favorite", 14, palette.muted, false);
        hint.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        browserToolbar.addView(hint, new LinearLayout.LayoutParams(0, -2, 1));
        FrameLayout list = new FrameLayout(this);
        body.addView(list, margins(new LinearLayout.LayoutParams(-1, 0, 1), 0, 14));
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
        // Keep the three tab controls; rebuild item targets when the catalog or favorites change.
        while (targets.size() > 3) targets.remove(targets.size() - 1);
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
    private void addTarget(View view, Runnable action) {
        clickable(view, action); targets.add(new Target(view, -1, action));
    }
    private void clickable(View view, final Runnable action) {
        // TUNE has one explicit selection model. Suppress the separate native
        // keyboard focus so it cannot draw a second highlight on another control.
        view.setFocusable(false);
        view.setClickable(true);
        // A touch lets go of a time wheel the knob had grabbed, so a later turn moves the knob again.
        view.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
            editField = EDIT_NONE; action.run();
        } });
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

    /** Settings shows the ADB address; Wi-Fi & ADB shows the whole connection. */
    private boolean connectionPage() { return page == ScreenFlow.Page.SETTINGS || page == ScreenFlow.Page.WIFI; }
    private void restartConnectionTick() {
        main.removeCallbacks(connectionTick); main.removeCallbacks(signalsTick);
        if (!resumed) return;
        if (connectionPage()) main.post(connectionTick);
        if (page == ScreenFlow.Page.SIGNALS) main.post(signalsTick);
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
                    if (isFinishing() || !resumed || page != ScreenFlow.Page.SIGNALS || epoch != connectionEpoch
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
                    if (!isFinishing() && resumed && connectionPage() && epoch == connectionEpoch) {
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
    private String connectionStatus() {
        return connection.state == ConnectionSnapshot.State.CONNECTED ? "Connected"
            : connection.state == ConnectionSnapshot.State.OFF ? "Wi-Fi off"
            : connection.state == ConnectionSnapshot.State.DISCONNECTED ? "Disconnected" : "Unavailable";
    }
    private void updateConnectionViews() {
        if (wifiValue != null) wifiValue.setText(connection.canCopy ? connection.adb : connectionStatus());
        if (statusText == null) return;
        statusText.setText(connectionStatus());
        statusText.setTextColor(connection.state == ConnectionSnapshot.State.CONNECTED ? palette.ok : palette.muted);
        networkText.setText(connection.network); ipText.setText(connection.ip);
        adbText.setText(connection.canCopy ? connection.adb + " · Copy" : connection.adb);
        copyRow.setEnabled(connection.canCopy);
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
                int steps = code == KEY_TUNE_UP || code == KeyEvent.KEYCODE_DPAD_RIGHT ? 1 : -1;
                if (editField != EDIT_NONE) { stepWheel(editField, steps); return true; }
                moveKnob(steps);
                if (hourWheel != null) renderSchedule();   // "Press to change" follows the knob onto a wheel.
                knobOnToggle = page == ScreenFlow.Page.BATTERY && knobIndex == 0;
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
        if (scroller != null && knobIndex >= 0 && knobIndex < targets.size()) {
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
