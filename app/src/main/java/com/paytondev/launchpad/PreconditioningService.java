package com.paytondev.launchpad;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.SystemClock;

/** APK-contained ownership. There is no boot receiver and no automatic Start. */
public final class PreconditioningService extends Service {
    static final String START = "com.paytondev.launchpad.PRECONDITION_START";
    static final String START_ANYWAY = "com.paytondev.launchpad.PRECONDITION_START_ANYWAY";
    static final String STOP = "com.paytondev.launchpad.PRECONDITION_STOP";
    private static final int NOTIFICATION = 1203;
    private final LocalBinder binder = new LocalBinder();
    private HandlerThread thread;
    private Handler worker;
    private HandlerThread temperatureThread;
    private Handler temperatureWorker;
    private volatile boolean temperatureVisible, destroyed;
    private volatile int temperatureGeneration;
    private volatile BatteryTemperatureReading temperature = BatteryTemperatureReading.checking();
    private final TemperatureHistory history = new TemperatureHistory();
    private PreconditioningController controller;
    private VehiclePort vehicle;
    private SharedPreferences journal;
    private volatile PreconditioningSnapshot snapshot = PreconditioningSnapshot.checking();
    private boolean bound, foreground;
    final class LocalBinder extends Binder {
        PreconditioningSnapshot snapshot() { return snapshot; }
        BatteryTemperatureReading temperature() { return temperature; }
        TemperatureHistory.Trace history() { return history.copy(SystemClock.elapsedRealtime()); }
    }
    private final Runnable temperaturePoll = new Runnable() {
        @Override public void run() {
            if (!temperatureVisible || destroyed) return;
            final int generation = temperatureGeneration;
            final Integer value = vehicle.readMinimumBatteryTemperatureC();
            final Integer maximum = value == null ? null : vehicle.readMaximumBatteryTemperatureC();
            final long receivedAt = SystemClock.elapsedRealtime();
            worker.post(new Runnable() { @Override public void run() {
                if (!bound || destroyed || generation != temperatureGeneration) return;
                temperature = temperature.update(value, receivedAt);
                if (!temperature.failed) history.add(receivedAt, temperature.minimumC,
                    maximum == null || maximum < -40 || maximum > 60 ? null : maximum);
                temperatureWorker.postDelayed(temperaturePoll, BatteryTemperatureReading.REFRESH_MS);
            } });
        }
    };
    private final Runnable attach = new Runnable() {
        @Override public void run() {
            bound = true; worker.removeCallbacks(tick); tick.run();
            temperatureVisible = true; temperatureGeneration++;
            temperatureWorker.removeCallbacks(temperaturePoll);
            temperatureWorker.post(temperaturePoll);
        }
    };
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            controller.refresh(SystemClock.elapsedRealtime()); publish();
            if (bound || snapshot.requested) worker.postDelayed(this, 1000);
            else stopSelf();
        }
    };
    @Override public void onCreate() {
        super.onCreate();
        journal = getSharedPreferences("launchpad_preconditioning", MODE_PRIVATE);
        thread = new HandlerThread("launchpad-vehicle"); thread.start();
        worker = new Handler(thread.getLooper());
        temperatureThread = new HandlerThread("launchpad-temperature-display");
        temperatureThread.start();
        temperatureWorker = new Handler(temperatureThread.getLooper());
        worker.post(new Runnable() { @Override public void run() {
            vehicle = VehicleClientFactory.create();
            controller = new PreconditioningController(vehicle,
                new PreconditioningController.Journal() {
                    @Override public void requested(boolean value) {
                        if (!journal.edit().putBoolean("requested", value).commit())
                            throw new IllegalStateException("Could not record session ownership");
                    }
                });
            boolean pending = journal.getBoolean("requested", false);
            if (pending) promote("Recovering · cancel pending request");
            controller.recover(pending, SystemClock.elapsedRealtime());
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
            temperatureVisible = false; temperatureGeneration++;
            temperatureWorker.removeCallbacks(temperaturePoll);
            if (!snapshot.requested) { worker.removeCallbacks(tick); stopSelf(); }
        } });
        return true;
    }
    @Override public int onStartCommand(final Intent intent, int flags, int startId) {
        worker.post(new Runnable() { @Override public void run() {
            if (intent == null) return;
            if (START.equals(intent.getAction())) {
                controller.refresh(SystemClock.elapsedRealtime());
                if (controller.snapshot(SystemClock.elapsedRealtime()).canStart
                        && controller.snapshot(SystemClock.elapsedRealtime()).state != PreconditioningSnapshot.State.WARM) {
                    promote("Checking battery temperature");
                    Integer minimum = vehicle.readMinimumBatteryTemperatureC();
                    // Recheck READY/Off after the optional bounded telemetry read.
                    controller.refresh(SystemClock.elapsedRealtime());
                    long now = SystemClock.elapsedRealtime();
                    if (controller.start(now, minimum)) history.markSessionStart(now);
                }
            }
            else if (START_ANYWAY.equals(intent.getAction())) {
                controller.refresh(SystemClock.elapsedRealtime());
                if (controller.snapshot(SystemClock.elapsedRealtime()).state == PreconditioningSnapshot.State.WARM) {
                    promote("Sending manual request");
                    long now = SystemClock.elapsedRealtime();
                    if (controller.startAnyway(now)) history.markSessionStart(now);
                }
            }
            else if (STOP.equals(intent.getAction())) controller.stop(SystemClock.elapsedRealtime());
            publish(); worker.removeCallbacks(tick);
            if (bound || snapshot.requested) worker.postDelayed(tick, 1000); else stopSelf();
        } });
        return START_NOT_STICKY;
    }
    private void publish() {
        snapshot = controller.snapshot(SystemClock.elapsedRealtime());
        if (snapshot.requested) {
            promote(snapshot.title() + " · " + snapshot.elapsed());
        } else if (foreground) { stopForeground(true); foreground = false; }
    }
    private void promote(String message) {
            Intent open = new Intent(this, MainActivity.class);
            PendingIntent content = PendingIntent.getActivity(this, 1203, open, PendingIntent.FLAG_UPDATE_CURRENT);
            Intent stop = new Intent(this, PreconditioningService.class).setAction(STOP);
            PendingIntent cancel = PendingIntent.getService(this, 1204, stop, PendingIntent.FLAG_UPDATE_CURRENT);
            Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_preconditioning).setContentTitle("Battery preconditioning")
                .setContentText(message).setOngoing(true)
                .setContentIntent(content).addAction(R.drawable.ic_preconditioning, "Stop", cancel).build();
            startForeground(NOTIFICATION, notification); foreground = true;
    }
    @Override public void onDestroy() {
        destroyed = true; temperatureVisible = false;
        if (temperatureWorker != null) temperatureWorker.removeCallbacksAndMessages(null);
        if (temperatureThread != null) temperatureThread.quitSafely();
        if (worker != null) worker.removeCallbacksAndMessages(null);
        // Retain the ownership journal after unexpected destruction. Recovery attempts
        // cancellation, never Start. Do not claim cancellation from onDestroy alone.
        if (thread != null) thread.quitSafely();
        super.onDestroy();
    }
}
