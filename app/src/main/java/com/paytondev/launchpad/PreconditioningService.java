package com.paytondev.launchpad;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.SystemClock;
import android.text.format.DateFormat;

import java.util.TimeZone;

/**
 * APK-contained ownership. The only automatic Start is a schedule the user armed: it is
 * sent once, through the same path as the Start button, and the boot receiver only
 * restores such a schedule. Recovery never starts conditioning.
 */
public final class PreconditioningService extends Service {
    static final String START = "com.paytondev.launchpad.PRECONDITION_START";
    static final String START_ANYWAY = "com.paytondev.launchpad.PRECONDITION_START_ANYWAY";
    static final String STOP = "com.paytondev.launchpad.PRECONDITION_STOP";
    static final String SCHEDULE_SET = "com.paytondev.launchpad.PRECONDITION_SCHEDULE_SET";
    static final String SCHEDULE_CANCEL = "com.paytondev.launchpad.PRECONDITION_SCHEDULE_CANCEL";
    /** From the AlarmManager backstop when the process may have been killed. */
    static final String SCHEDULE_FIRE = "com.paytondev.launchpad.PRECONDITION_SCHEDULE_FIRE";
    /** From BootReceiver, only when a schedule is stored. */
    static final String SCHEDULE_RESTORE = "com.paytondev.launchpad.PRECONDITION_SCHEDULE_RESTORE";
    static final String EXTRA_MODE = "mode", EXTRA_TARGET = "target";
    static final String PREFS = "launchpad_preconditioning", SCHEDULE_KEY = "schedule", LOG_KEY = "schedule_log";
    private static final int NOTIFICATION = 1203, OUTCOME_NOTIFICATION = 1207;
    private static final int STOP_REQUEST = 1204, ALARM_REQUEST = 1205, CANCEL_SCHEDULE_REQUEST = 1206;
    /** Repeated "car not ready" lines are logged at most this often. */
    private static final long DEFER_LOG_MS = 30_000;
    private enum StartOutcome { STARTED, WARM, NOT_SENT, UNCERTAIN }
    private final LocalBinder binder = new LocalBinder();
    private HandlerThread thread;
    private Handler worker;
    private HandlerThread temperatureThread;
    private Handler temperatureWorker;
    private volatile boolean temperatureVisible, destroyed;
    private volatile int temperatureGeneration;
    private volatile BatteryTemperatureReading temperature = BatteryTemperatureReading.checking();
    private volatile Integer outsideC;
    private volatile int warmThresholdC = BatteryTemperature.EXPECTED_HEATING_THRESHOLD_C;
    private final TemperatureHistory history = new TemperatureHistory();
    private PreconditioningController controller;
    private VehiclePort vehicle;
    private SharedPreferences journal;
    private volatile PreconditioningSnapshot snapshot = PreconditioningSnapshot.checking();
    /** Armed schedule, or null. Written only on the worker. */
    private volatile PreconditioningSchedule schedule;
    private volatile ScheduleLog scheduleLog = ScheduleLog.EMPTY;
    /** Set when a stored schedule was loaded at create; names what brought us back. */
    private boolean restoredSchedule;
    private Integer lastStartMinimumC;
    private boolean bound, foreground;
    private String lastNotification;
    final class LocalBinder extends Binder {
        PreconditioningSnapshot snapshot() { return snapshot; }
        BatteryTemperatureReading temperature() { return temperature; }
        Integer outsideC() { return outsideC; }
        int warmThresholdC() { return warmThresholdC; }
        TemperatureHistory.Trace history() { return history.copy(SystemClock.elapsedRealtime()); }
        PreconditioningSchedule schedule() { return schedule; }
        ScheduleLog scheduleLog() { return scheduleLog; }
    }
    private boolean temperatureWanted() { return temperatureVisible || schedule != null; }
    private final Runnable temperaturePoll = new Runnable() {
        @Override public void run() {
            if (!temperatureWanted() || destroyed) return;
            final int generation = temperatureGeneration;
            final BatteryTemperature.Sample sample = vehicle.readTemperatures();
            final long receivedAt = SystemClock.elapsedRealtime();
            worker.post(new Runnable() { @Override public void run() {
                if ((!bound && schedule == null) || destroyed || generation != temperatureGeneration) return;
                Integer value = sample.minimumC, maximum = sample.maximumC;
                temperature = temperature.update(value, receivedAt);
                if (!temperature.failed) history.add(receivedAt, temperature.minimumC,
                    maximum == null || maximum < -40 || maximum > 60 ? null : maximum);
                if (sample.outsideC != null) outsideC = sample.outsideC;
                replan();
                temperatureWorker.postDelayed(temperaturePoll, BatteryTemperatureReading.REFRESH_MS);
            } });
        }
    };
    private final Runnable attach = new Runnable() {
        @Override public void run() {
            bound = true; worker.removeCallbacks(tick);
            noteRestored("after Precondition restarted");
            tick.run();
            temperatureVisible = true; startTemperaturePoll();
        }
    };
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            controller.refresh(SystemClock.elapsedRealtime());
            evaluateSchedule(); publish();
            if (keepAlive()) worker.postDelayed(this, 1000);
            else stopSelf();
        }
    };
    private boolean keepAlive() { return bound || snapshot.requested || schedule != null; }
    /** Worker thread only. */
    private void startTemperaturePoll() {
        temperatureGeneration++;
        temperatureWorker.removeCallbacks(temperaturePoll);
        temperatureWorker.post(temperaturePoll);
    }
    @Override public void onCreate() {
        super.onCreate();
        journal = getSharedPreferences(PREFS, MODE_PRIVATE);
        thread = new HandlerThread("launchpad-vehicle"); thread.start();
        worker = new Handler(thread.getLooper());
        temperatureThread = new HandlerThread("launchpad-temperature-display");
        temperatureThread.start();
        temperatureWorker = new Handler(temperatureThread.getLooper());
        worker.post(new Runnable() { @Override public void run() {
            vehicle = VehicleClientFactory.create();
            warmThresholdC = vehicle.warmThresholdC();
            controller = new PreconditioningController(vehicle,
                new PreconditioningController.Journal() {
                    @Override public void requested(boolean value) {
                        if (!journal.edit().putBoolean("requested", value).commit())
                            throw new IllegalStateException("Could not record session ownership");
                    }
                });
            boolean pending = journal.getBoolean("requested", false);
            if (pending) promote("Recovering · cancel pending request", false);
            controller.recover(pending, SystemClock.elapsedRealtime());
            scheduleLog = ScheduleLog.decode(journal.getString(LOG_KEY, null));
            schedule = PreconditioningSchedule.decode(journal.getString(SCHEDULE_KEY, null));
            if (schedule != null) {
                restoredSchedule = true;
                if (pending) log(ScheduleLog.Kind.RECOVERED,
                    "Precondition restarted mid-request, so it cancelled that first");
                armAlarm();            // Alarms do not survive a reboot.
                startTemperaturePoll();
            }
            publish();
        } });
    }
    @Override public IBinder onBind(Intent intent) {
        worker.post(attach);
        return binder;
    }
    // A service kept alive by a session skips onBind on the next bind; resume polling here.
    @Override public void onRebind(Intent intent) { worker.post(attach); }
    @Override public boolean onUnbind(Intent intent) {
        worker.post(new Runnable() { @Override public void run() {
            bound = false;
            temperatureVisible = false;
            // An armed schedule keeps polling so arrive-by can re-plan while Launchpad is closed.
            if (schedule == null) { temperatureGeneration++; temperatureWorker.removeCallbacks(temperaturePoll); }
            if (!keepAlive()) { worker.removeCallbacks(tick); stopSelf(); }
        } });
        return true; // Request onRebind when a client returns to the live service.
    }
    @Override public int onStartCommand(final Intent intent, int flags, int startId) {
        worker.post(new Runnable() { @Override public void run() {
            if (intent == null) return;
            String action = intent.getAction();
            if (START.equals(action)) {
                if (schedule != null) cancelSchedule("Start was pressed");
                startNow();
            }
            else if (START_ANYWAY.equals(action)) {
                if (schedule != null) cancelSchedule("Start anyway was pressed");
                controller.refresh(SystemClock.elapsedRealtime());
                if (controller.snapshot(SystemClock.elapsedRealtime()).state == PreconditioningSnapshot.State.WARM) {
                    promote("Sending manual request", false);
                    long now = SystemClock.elapsedRealtime();
                    if (controller.startAnyway(now)) history.markSessionStart(now);
                }
            }
            else if (STOP.equals(action)) controller.stop(SystemClock.elapsedRealtime());
            else if (SCHEDULE_SET.equals(action)) arm(intent.getStringExtra(EXTRA_MODE), intent.getLongExtra(EXTRA_TARGET, -1));
            else if (SCHEDULE_CANCEL.equals(action)) { if (schedule != null) cancelSchedule("By you"); }
            else if (SCHEDULE_FIRE.equals(action)) noteRestored("after Precondition restarted");
            else if (SCHEDULE_RESTORE.equals(action)) noteRestored("after the head unit restarted");
            evaluateSchedule();
            publish(); worker.removeCallbacks(tick);
            if (keepAlive()) worker.postDelayed(tick, 1000); else stopSelf();
        } });
        return START_NOT_STICKY;
    }

    /** The Start button's path, shared with the scheduler. Worker thread only. */
    private StartOutcome startNow() {
        long now = SystemClock.elapsedRealtime();
        controller.refresh(now);
        PreconditioningSnapshot before = controller.snapshot(now);
        lastStartMinimumC = null;
        // A duplicate Start during the warm notice must never become consent to override it.
        if (before.state == PreconditioningSnapshot.State.WARM) return StartOutcome.WARM;
        if (!before.canStart) return StartOutcome.NOT_SENT;
        promote("Checking battery temperature", false);
        Integer minimum = vehicle.readMinimumBatteryTemperatureC();
        lastStartMinimumC = minimum;
        // Recheck READY/Off after the optional bounded telemetry read.
        controller.refresh(SystemClock.elapsedRealtime());
        now = SystemClock.elapsedRealtime();
        if (controller.start(now, minimum)) { history.markSessionStart(now); return StartOutcome.STARTED; }
        PreconditioningSnapshot after = controller.snapshot(now);
        if (after.requested) return StartOutcome.UNCERTAIN;   // Journalled and attempted; Stop stays available.
        return after.state == PreconditioningSnapshot.State.WARM ? StartOutcome.WARM : StartOutcome.NOT_SENT;
    }

    private void arm(String modeName, long target) {
        long now = WallClock.now();
        PreconditioningSchedule.Mode mode;
        try { mode = PreconditioningSchedule.Mode.valueOf(modeName); }
        catch (RuntimeException e) { log(ScheduleLog.Kind.REJECTED, "Unknown schedule type"); return; }
        if (!PreconditioningSchedule.acceptable(target, now)) {
            log(ScheduleLog.Kind.REJECTED, "Pick a time 2 minutes to 24 hours away");
            return;
        }
        PreconditioningSchedule next = PreconditioningSchedule.create(mode, target, now, freshMinimumC(), outsideC, warmThresholdC);
        if (!persist(next)) { log(ScheduleLog.Kind.REJECTED, "Couldn't save the schedule"); return; }
        if (schedule != null) log(ScheduleLog.Kind.CANCELLED, "Replaced by a new schedule");
        schedule = next; restoredSchedule = false;
        log(ScheduleLog.Kind.ARMED, describe(next));
        armAlarm();
        if (!temperatureVisible) startTemperaturePoll();
    }
    private void replan() {
        PreconditioningSchedule current = schedule;
        if (current == null || current.mode != PreconditioningSchedule.Mode.ARRIVE_BY || temperature.failed) return;
        PreconditioningSchedule next = current.replanned(WallClock.now(), freshMinimumC(), outsideC, warmThresholdC);
        if (next == current || !next.materiallyDifferent(current) || !persist(next)) return;
        schedule = next;
        log(ScheduleLog.Kind.REPLANNED, next.plannedSendAt == PreconditioningSchedule.NONE ? "Battery is already warm"
            : "Now starting at " + clock(next.plannedSendAt));
        armAlarm();
    }
    /** Fire, defer or expire the armed schedule. Worker thread, every tick and command. */
    private void evaluateSchedule() {
        PreconditioningSchedule current = schedule;
        if (current == null) return;
        long now = WallClock.now();
        PreconditioningSchedule.Due due = current.due(now);
        if (due == PreconditioningSchedule.Due.WAIT) return;
        if (due == PreconditioningSchedule.Due.EXPIRED) {
            if (current.plannedSendAt == PreconditioningSchedule.NONE)
                finish(ScheduleLog.Kind.SKIPPED, "Battery stayed warm, so it didn't need to start");
            else {
                ScheduleLog.Event last = scheduleLog.latest();
                finish(ScheduleLog.Kind.MISSED, last != null && last.kind == ScheduleLog.Kind.DEFERRED
                    ? "The car wasn't ready in time"
                    : "The head unit was off at " + clock(current.plannedSendAt));
            }
            return;
        }
        long elapsed = SystemClock.elapsedRealtime();
        controller.refresh(elapsed);
        PreconditioningSnapshot s = controller.snapshot(elapsed);
        if (!s.requested && s.indicator == 1) {
            finish(ScheduleLog.Kind.SKIPPED, "The car was already warming the battery");
            return;
        }
        if (s.state != PreconditioningSnapshot.State.WARM && !s.canStart) { defer(s.detail, current); return; }
        // Remove the stored schedule before transmitting, like the ownership journal:
        // a restart must never find a schedule that was already sent.
        if (!persist(null)) { defer("The schedule could not be updated before sending.", current); return; }
        switch (startNow()) {
            case STARTED:
                finish(ScheduleLog.Kind.FIRED, "Asked the car to start warming the battery");
                return;
            case UNCERTAIN:
                finish(ScheduleLog.Kind.FIRED, "Tried to start, but the car didn't confirm. Stop is still available.");
                return;
            case WARM:
                finish(ScheduleLog.Kind.SKIPPED, "Battery was already warm"
                    + (lastStartMinimumC == null ? "" : " (" + BatteryTemperatureReading.fahrenheit(lastStartMinimumC) + "°F)"));
                return;
            default:
                persist(current);   // Nothing was sent; keep trying until the grace deadline.
                defer(controller.snapshot(SystemClock.elapsedRealtime()).detail, current);
        }
    }
    private void defer(String reason, PreconditioningSchedule current) {
        String text = reason.trim() + " Trying until " + clock(current.deadline()) + ".";
        ScheduleLog.Event last = scheduleLog.latest();
        if (last != null && last.kind == ScheduleLog.Kind.DEFERRED
            && (last.text.equals(text) || WallClock.now() - last.at < DEFER_LOG_MS)) return;
        log(ScheduleLog.Kind.DEFERRED, text);
    }
    /** A final outcome: log it, clear the schedule, and tell the user. */
    private void finish(ScheduleLog.Kind kind, String text) {
        log(kind, text);
        clearSchedule();
        notifyOutcome(kind, text);
    }
    private void cancelSchedule(String reason) {
        log(ScheduleLog.Kind.CANCELLED, reason);
        clearSchedule();
    }
    private void clearSchedule() {
        persist(null); schedule = null; restoredSchedule = false;
        alarms().cancel(firePendingIntent());
    }
    private void noteRestored(String how) {
        if (!restoredSchedule || schedule == null) return;
        restoredSchedule = false;
        log(ScheduleLog.Kind.RESTORED, describe(schedule) + ", " + how);
    }
    private boolean persist(PreconditioningSchedule value) {
        SharedPreferences.Editor edit = journal.edit();
        if (value == null) edit.remove(SCHEDULE_KEY); else edit.putString(SCHEDULE_KEY, value.encode());
        return edit.commit();
    }
    private void log(ScheduleLog.Kind kind, String text) {
        scheduleLog = scheduleLog.add(WallClock.now(), kind, text);
        journal.edit().putString(LOG_KEY, scheduleLog.encode()).commit();   // Best effort.
    }
    private Integer freshMinimumC() {
        BatteryTemperatureReading t = temperature;
        return t.minimumC == null || t.stale(SystemClock.elapsedRealtime()) ? null : t.minimumC;
    }
    private String describe(PreconditioningSchedule s) {
        String head = s.title() + " " + clock(s.targetAt);
        if (s.mode == PreconditioningSchedule.Mode.START_BY) return head;
        if (s.plannedSendAt == PreconditioningSchedule.NONE) return head + ", battery already warm";
        return head + ", starting at " + clock(s.plannedSendAt);
    }
    private String clock(long epochMs) {
        return ScheduleFormat.time(epochMs, DateFormat.is24HourFormat(this), TimeZone.getDefault());
    }

    private AlarmManager alarms() { return (AlarmManager) getSystemService(ALARM_SERVICE); }
    private PendingIntent firePendingIntent() {
        return PendingIntent.getService(this, ALARM_REQUEST,
            new Intent(this, PreconditioningService.class).setAction(SCHEDULE_FIRE), PendingIntent.FLAG_UPDATE_CURRENT);
    }
    /** Backstop for a killed process: wake the service at the send time. */
    private void armAlarm() {
        PreconditioningSchedule current = schedule;
        AlarmManager alarms = alarms(); PendingIntent fire = firePendingIntent();
        alarms.cancel(fire);
        if (current == null) return;
        // A warm arrive-by plan still wakes before arrival to check whether the pack cooled.
        long at = current.plannedSendAt != PreconditioningSchedule.NONE ? current.plannedSendAt
            : current.targetAt - PreconditioningSchedule.SEND_LEAD_MS;
        alarms.setExact(AlarmManager.RTC_WAKEUP, at, fire);
    }

    private void publish() {
        snapshot = controller.snapshot(SystemClock.elapsedRealtime());
        PreconditioningSchedule current = schedule;
        if (snapshot.requested) {
            promote(snapshot.title() + " · " + snapshot.elapsed(), false);
        } else if (current != null) {
            promote("Scheduled: " + current.title().toLowerCase(java.util.Locale.US) + " " + clock(current.targetAt), true);
        } else if (foreground) { stopForeground(true); foreground = false; lastNotification = null; }
    }
    private void promote(String message, boolean armed) {
        if (foreground && message.equals(lastNotification)) return;
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent content = PendingIntent.getActivity(this, NOTIFICATION, open, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent action = armed
            ? PendingIntent.getService(this, CANCEL_SCHEDULE_REQUEST,
                new Intent(this, PreconditioningService.class).setAction(SCHEDULE_CANCEL), PendingIntent.FLAG_UPDATE_CURRENT)
            : PendingIntent.getService(this, STOP_REQUEST,
                new Intent(this, PreconditioningService.class).setAction(STOP), PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new Notification.Builder(this)
            .setSmallIcon(R.drawable.ic_preconditioning).setContentTitle("Battery preconditioning")
            .setContentText(message).setOngoing(true).setContentIntent(content)
            .addAction(R.drawable.ic_preconditioning, armed ? "Cancel schedule" : "Stop", action).build();
        startForeground(NOTIFICATION, notification); foreground = true; lastNotification = message;
    }
    /** Best effort: the head unit may not show notifications; the in-app log is the record. */
    private void notifyOutcome(ScheduleLog.Kind kind, String text) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager == null) return;
        // Short: the head unit wraps notification titles past about 20 characters.
        String title = kind == ScheduleLog.Kind.FIRED ? "Warming started"
            : kind == ScheduleLog.Kind.SKIPPED ? "Warming skipped" : "Warming missed";
        PendingIntent content = PendingIntent.getActivity(this, OUTCOME_NOTIFICATION,
            new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        manager.notify(OUTCOME_NOTIFICATION, new Notification.Builder(this)
            .setSmallIcon(R.drawable.ic_preconditioning).setContentTitle(title).setContentText(text)
            .setContentIntent(content).setAutoCancel(true).build());
    }
    @Override public void onDestroy() {
        destroyed = true; temperatureVisible = false;
        if (temperatureWorker != null) temperatureWorker.removeCallbacksAndMessages(null);
        if (temperatureThread != null) temperatureThread.quitSafely();
        if (worker != null) worker.removeCallbacksAndMessages(null);
        // Retain the ownership journal after unexpected destruction. Recovery attempts
        // cancellation, never Start. Do not claim cancellation from onDestroy alone.
        // An armed schedule stays stored and its alarm stays set.
        if (thread != null) thread.quitSafely();
        super.onDestroy();
    }
}
