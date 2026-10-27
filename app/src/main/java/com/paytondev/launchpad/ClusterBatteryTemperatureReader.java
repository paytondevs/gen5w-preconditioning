package com.paytondev.launchpad;

import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

/** One-shot read of the cluster battery-info callback, identical on the EV6 and IONIQ 5 profiles. */
final class ClusterBatteryTemperatureReader {
    private static final String SERVICE = "com.hkmc.cluster.IClusterService";
    private static final String LISTENER = "com.hkmc.cluster.INavigationClusterListener";
    private static FutureTask<Integer> pending;
    private static long deadlineNanos;

    // Keep even a hung Binder transaction off the controller worker. At most one
    // helper is in flight across service recreation. Timeouts are never reused.
    static Integer read() {
        final FutureTask<Integer> task;
        final long deadline;
        synchronized (ClusterBatteryTemperatureReader.class) {
            if (pending == null || pending.isDone()) {
                pending = new FutureTask<Integer>(() -> collect());
                deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(1800);
                Thread helper = new Thread(pending, "launchpad-battery-precheck");
                helper.setDaemon(true);
                helper.start();
            }
            task = pending;
            deadline = deadlineNanos;
        }
        // Display polling and a Start precheck may share an in-flight read.
        // Neither waits for the other's monitor lock or extends its deadline.
        long remaining = deadline - System.nanoTime();
        if (remaining <= 0) return null;
        try {
            Integer value = task.get(remaining, TimeUnit.NANOSECONDS);
            return System.nanoTime() <= deadline ? value : null;
        }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); return null; }
        catch (Exception e) { return null; }
    }
    private static Integer collect() throws Exception {
        IBinder remote = (IBinder) Class.forName("android.os.ServiceManager")
            .getMethod("getService", String.class).invoke(null, "com.hkmc.micom.cluster");
        if (remote == null || !remote.isBinderAlive() || !SERVICE.equals(remote.getInterfaceDescriptor())) return null;
        Listener listener = new Listener();
        try {
            call(remote, 37, listener);
            listener.armed = true;
            call(remote, 109, null);
            return listener.response.await(1000, TimeUnit.MILLISECONDS) ? listener.temperature : null;
        } finally {
            listener.armed = false;
            // Also remove after uncertain registration; never retain a subscription.
            call(remote, 38, listener);
        }
    }
    private static final class Listener extends Binder {
        final CountDownLatch response = new CountDownLatch(1);
        volatile boolean armed;
        volatile Integer temperature;
        Listener() { attachInterface(null, LISTENER); }
        @Override protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code == INTERFACE_TRANSACTION) {
                if (reply != null) reply.writeString(LISTENER);
                return true;
            }
            if (code >= 1 && code <= 16) {
                data.enforceInterface(LISTENER);
                if (code == 16 && armed && response.getCount() != 0) {
                    int[] values = data.createIntArray();
                    temperature = data.dataAvail() == 0 ? BatteryTemperature.decode(values) : null;
                    response.countDown();
                }
                if (reply != null) reply.writeNoException();
                return true;
            }
            return super.onTransact(code, data, reply, flags);
        }
    }
    private static void call(IBinder remote, int code, IBinder callback) throws Exception {
        if (code != 37 && code != 38 && code != 109) throw new IllegalArgumentException("Read-only battery transaction required");
        Parcel data = Parcel.obtain(), reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(SERVICE);
            if (code == 37 || code == 38) data.writeStrongBinder(callback);
            if (!remote.transact(code, data, reply, 0)) throw new RemoteException("Battery read rejected");
            reply.readException();
            if (reply.dataAvail() != 0) throw new RemoteException("Unexpected battery reply");
        } finally { data.recycle(); reply.recycle(); }
    }
}
