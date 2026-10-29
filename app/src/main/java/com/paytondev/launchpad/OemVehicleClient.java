package com.paytondev.launchpad;

import android.os.Build;
import android.os.IBinder;
import android.os.Parcel;
import android.os.SystemClock;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.Locale;

/** Uses the existing HEV service. No raw CAN, global preference writes, or nav hooks. */
final class OemVehicleClient implements VehiclePort, PreconditioningProfile.Commands {
    private static final String HEV = "com.hkmc.hev.IHEVService", BCM = "com.hkmc.bcm.IBCMService";
    private IBinder hev, bcm;
    private volatile boolean firmwareChecked;
    private void connect(boolean requireIndicator) throws Exception {
        if (!PreconditioningProfile.supportsDevice(Build.VERSION.SDK_INT, Build.DEVICE))
            throw new IllegalStateException("Unsupported head-unit firmware");
        if (!firmwareChecked) {
            // Build.PRODUCT is ro.product.name, not the collected ro.build.product.
            String software = "";
            if (PreconditioningProfile.EV6_DEVICE.equals(Build.DEVICE)
                || PreconditioningProfile.IONIQ5_NEEV_US_DEVICE.equals(Build.DEVICE)) {
                Method property = Class.forName("android.os.SystemProperties").getMethod("get", String.class);
                software = (String) property.invoke(null, "ro.build.product");
            }
            if (!PreconditioningProfile.matchesFirmware(Build.VERSION.SDK_INT, Build.DEVICE, software,
                hash("/system/framework/automotive.odex"), hash("/system/framework/automotive-service.odex")))
                throw new IllegalStateException("Vehicle interface version differs from the inspected firmware");
            firmwareChecked = true;
        }
        Method get = Class.forName("android.os.ServiceManager").getMethod("getService", String.class);
        hev = (IBinder) get.invoke(null, "com.hkmc.micom.hev");
        bcm = (IBinder) get.invoke(null, "com.hkmc.micom.bcm");
        if (hev == null || !hev.isBinderAlive() || !HEV.equals(hev.getInterfaceDescriptor())
            || (requireIndicator && (bcm == null || !bcm.isBinderAlive() || !BCM.equals(bcm.getInterfaceDescriptor()))))
            throw new IllegalStateException("Vehicle services unavailable or incompatible");
    }
    @Override public String unavailableReason() { return ""; }
    @Override public Integer readMinimumBatteryTemperatureC() {
        // Every supported profile reads the cluster minimum, but only after read()
        // has passed the exact firmware hashes.
        if (!firmwareChecked) return null;
        return ClusterBatteryTemperatureReader.read();
    }
    @Override public Reading read() throws Exception {
        connect(true);
        int enabled = getter(hev, HEV, 3, 94);
        int indicator = getter(bcm, BCM, 109);
        // Failure is kept raw until its enum meanings are established.
        return new Reading(enabled, indicator, getter(hev, HEV, 3, 44), getter(hev, HEV, 3, 115));
    }
    private static int getter(IBinder binder, String descriptor, int transaction, int... values) throws Exception {
        Parcel data = Parcel.obtain(), reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(descriptor);
            for (int value : values) data.writeInt(value);
            if (!binder.transact(transaction, data, reply, 0)) throw new IllegalStateException("Getter rejected");
            reply.readException();
            if (reply.dataAvail() != 4) throw new IllegalStateException("Unexpected vehicle reply");
            return reply.readInt();
        } finally { data.recycle(); reply.recycle(); }
    }
    @Override public void beginManual() throws Exception { connect(true); PreconditioningProfile.start(this); }
    @Override public void maintainManual() throws Exception { connect(true); PreconditioningProfile.maintain(this); }
    @Override public void stopAndRelease() throws Exception { connect(false); PreconditioningProfile.cancel(this); }
    @Override public long maintenanceIntervalMs() { return PreconditioningProfile.REFRESH_MS; }
    @Override public long activationTimeoutMs() { return PreconditioningProfile.activationTimeoutMs(Build.DEVICE); }
    @Override public int warmThresholdC() { return PreconditioningProfile.warmThresholdC(Build.DEVICE); }
    @Override public void pause(long milliseconds) { SystemClock.sleep(milliseconds); }
    @Override public void write(int transaction, int... values) throws Exception {
        if (!((transaction == 62 && values.length == 2) ||
            ((transaction == 65 || transaction == 66) && values.length == 1)))
            throw new IllegalArgumentException("Command outside the verified preconditioning profile");
        Parcel data = Parcel.obtain(), reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(HEV);
            for (int value : values) data.writeInt(value);
            if (!hev.transact(transaction, data, reply, 0)) throw new IllegalStateException("Vehicle command rejected");
            reply.readException();
            if (reply.dataAvail() != 0) throw new IllegalStateException("Unexpected vehicle command reply");
        } finally { data.recycle(); reply.recycle(); }
    }
    static String hash(String path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (FileInputStream in = new FileInputStream(path)) {
            byte[] bytes = new byte[16384]; int count;
            while ((count = in.read(bytes)) != -1) digest.update(bytes, 0, count);
        }
        StringBuilder value = new StringBuilder();
        for (byte b : digest.digest()) value.append(String.format(Locale.US, "%02x", b & 255));
        return value.toString();
    }
}
