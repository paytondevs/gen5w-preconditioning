package com.paytondev.launchpad;

import android.os.Build;
import android.os.IBinder;
import com.paytondev.launchpad.knob.Evdev;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

/**
 * Read-only vehicle porting kit probe. The port-kit USB (usb/port-kit/collect.sh)
 * runs it through app_process with the Launchpad APK as its dex container, and it
 * appends its sections to the report on the drive.
 *
 * It never sends a vehicle command: the only Binder call it makes is
 * getInterfaceDescriptor(). Transaction codes come from the OEM framework's own
 * AIDL classes by reflection, so no firmware file leaves the car. Key events are
 * read passively, without grabbing devices or injecting events, like knobd.
 *
 * Usage: PortProbe REPORT_PATH CAPTURE_SECONDS
 */
public final class PortProbe {
    static final String FRAMEWORK = "/system/framework";
    static final String HKMC = "com.hkmc.";
    /** A press at least this long counts as a deliberate hold of the knob. */
    static final long HOLD_MS = 700;
    private static final int MAX_EVENTS = 400;
    private static final long BINDER_TIMEOUT_MS = 2000;

    private interface Section { void write(PrintWriter out) throws Exception; }

    private final PrintWriter out;
    private final List<KeyEvent> events = Collections.synchronizedList(new ArrayList<KeyEvent>());
    private final Map<String, String> inputNames = new LinkedHashMap<>();
    private final TreeSet<String> descriptors = new TreeSet<>();
    private long captureStart;

    private PortProbe(PrintWriter out) { this.out = out; }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("usage: PortProbe REPORT_PATH CAPTURE_SECONDS");
            System.exit(2);
        }
        long seconds = Long.parseLong(args[1]);
        PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(args[0], true), "UTF-8"));
        try {
            new PortProbe(out).run(seconds);
        } finally {
            out.close();
        }
        // Key readers may still be blocked in read(); they are daemon threads.
        System.exit(0);
    }

    private void run(final long seconds) {
        // Start listening first, so the owner's knob holds land while the rest runs.
        section("key_capture_start", new Section() {
            @Override public void write(PrintWriter out) throws Exception { startCapture(out, seconds); }
        });
        section("existing_profile", new Section() {
            @Override public void write(PrintWriter out) throws Exception { existingProfile(out); }
        });
        section("firmware", new Section() {
            @Override public void write(PrintWriter out) throws Exception { firmware(out); }
        });
        section("service_descriptors", new Section() {
            @Override public void write(PrintWriter out) throws Exception { serviceDescriptors(out); }
        });
        section("interfaces", new Section() {
            @Override public void write(PrintWriter out) throws Exception { interfaces(out); }
        });
        section("key_capture", new Section() {
            @Override public void write(PrintWriter out) throws Exception { finishCapture(out, seconds); }
        });
        section("end", new Section() {
            @Override public void write(PrintWriter out) { out.println("probe_complete=yes"); }
        });
    }

    private void section(String name, Section body) {
        out.println();
        out.println("[" + name + "]");
        try {
            body.write(out);
        } catch (Throwable t) {
            out.println("error=" + t);
        }
        out.flush();
        System.out.println("port probe: wrote [" + name + "]");
    }

    private void existingProfile(PrintWriter out) throws Exception {
        String software = systemProperty("ro.build.product");
        String framework = hashOrEmpty(FRAMEWORK + "/automotive.odex");
        String service = hashOrEmpty(FRAMEWORK + "/automotive-service.odex");
        out.println("sdk=" + Build.VERSION.SDK_INT);
        out.println("device=" + Build.DEVICE);
        out.println("software=" + software);
        out.println("supported_device=" + yes(PreconditioningProfile.supportsDevice(Build.VERSION.SDK_INT, Build.DEVICE)));
        out.println("matches_existing_profile=" + yes(PreconditioningProfile.matchesFirmware(
            Build.VERSION.SDK_INT, Build.DEVICE, software, framework, service)));
    }

    private void firmware(PrintWriter out) throws Exception {
        String[] names = new File(FRAMEWORK).list();
        if (names == null) throw new IOException(FRAMEWORK + " is not readable");
        Arrays.sort(names);
        for (String name : names) {
            if (!name.startsWith("automotive")) continue;
            File file = new File(FRAMEWORK, name);
            out.println("file=" + file + " size=" + file.length() + " sha256=" + hashOrEmpty(file.getPath()));
        }
    }

    private void serviceDescriptors(PrintWriter out) throws Exception {
        Class<?> manager = Class.forName("android.os.ServiceManager");
        String[] names = (String[]) manager.getMethod("listServices").invoke(null);
        Arrays.sort(names);
        for (String name : names) {
            String descriptor;
            try {
                final IBinder binder = (IBinder) manager.getMethod("getService", String.class).invoke(null, name);
                if (binder == null) {
                    descriptor = "(none)";
                } else {
                    // INTERFACE_TRANSACTION is answered by Binder itself, not by OEM code.
                    descriptor = within(new Callable<String>() {
                        @Override public String call() throws Exception { return binder.getInterfaceDescriptor(); }
                    });
                }
            } catch (Exception e) {
                descriptor = "(error " + e + ")";
            }
            out.println("service=" + name + " descriptor=" + descriptor);
            if (descriptor != null && descriptor.startsWith(HKMC)) descriptors.add(descriptor);
        }
    }

    private void interfaces(PrintWriter out) throws Exception {
        List<ClassLoader> loaders = frameworkLoaders(out);
        for (String descriptor : descriptors) {
            out.println();
            out.println("interface=" + descriptor);
            Class<?> stub = find(loaders, descriptor + "$Stub");
            if (stub == null) {
                out.println("stub=not found");
            } else {
                out.println("stub=" + stub.getName());
                for (String line : transactionTable(stub)) out.println("transaction " + line);
            }
            Class<?> iface = find(loaders, descriptor);
            if (iface != null) for (String line : constants(iface)) out.println("constant " + line);
        }
    }

    /** The boot classpath first, then each OEM framework jar in case it is not on it. */
    private static List<ClassLoader> frameworkLoaders(PrintWriter out) {
        List<ClassLoader> loaders = new ArrayList<>();
        loaders.add(ClassLoader.getSystemClassLoader());
        String[] names = new File(FRAMEWORK).list();
        if (names == null) return loaders;
        Arrays.sort(names);
        for (String name : names) {
            if (!name.startsWith("automotive") || !name.endsWith(".jar")) continue;
            try {
                loaders.add((ClassLoader) Class.forName("dalvik.system.PathClassLoader")
                    .getConstructor(String.class, ClassLoader.class)
                    .newInstance(FRAMEWORK + "/" + name, ClassLoader.getSystemClassLoader()));
                out.println("loader=" + FRAMEWORK + "/" + name);
            } catch (Exception e) {
                out.println("loader=" + FRAMEWORK + "/" + name + " error=" + e);
            }
        }
        return loaders;
    }

    private static Class<?> find(List<ClassLoader> loaders, String name) {
        for (ClassLoader loader : loaders) {
            try {
                return Class.forName(name, false, loader);
            } catch (Throwable ignored) { }
        }
        return null;
    }

    /** "code name" for each static int TRANSACTION_* field, ordered by code then name. */
    static List<String> transactionTable(Class<?> stub) throws IllegalAccessException {
        List<Object[]> rows = new ArrayList<>();
        for (Field field : stub.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || field.getType() != int.class
                || !field.getName().startsWith("TRANSACTION_")) continue;
            field.setAccessible(true);
            rows.add(new Object[] {field.getInt(null), field.getName().substring("TRANSACTION_".length())});
        }
        Collections.sort(rows, (a, b) -> {
            int byCode = Integer.compare((Integer) a[0], (Integer) b[0]);
            return byCode != 0 ? byCode : ((String) a[1]).compareTo((String) b[1]);
        });
        List<String> lines = new ArrayList<>();
        for (Object[] row : rows) lines.add(row[0] + " " + row[1]);
        return lines;
    }

    /** "NAME=value" for each static final int, long or String field, ordered by name. */
    static List<String> constants(Class<?> type) throws IllegalAccessException {
        List<String> lines = new ArrayList<>();
        for (Field field : type.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            Class<?> kind = field.getType();
            if (!Modifier.isStatic(modifiers) || !Modifier.isFinal(modifiers)
                || (kind != int.class && kind != long.class && kind != String.class)) continue;
            field.setAccessible(true);
            lines.add(field.getName() + "=" + field.get(null));
        }
        Collections.sort(lines);
        return lines;
    }

    private void startCapture(PrintWriter out, long seconds) throws Exception {
        FileReader devices = new FileReader("/proc/bus/input/devices");
        try {
            inputNames.putAll(inputDevices(devices));
        } finally {
            devices.close();
        }
        captureStart = now();
        out.println("window_seconds=" + seconds);
        for (Map.Entry<String, String> device : inputNames.entrySet()) {
            final String path = device.getKey(), name = device.getValue();
            Thread reader = new Thread(new Runnable() {
                @Override public void run() { capture(path, name); }
            }, "port-probe-" + path);
            reader.setDaemon(true);
            reader.start();
            out.println("listening=" + path + " name=\"" + name + "\"");
        }
    }

    private void capture(String path, String name) {
        try {
            DataInputStream in = new DataInputStream(new FileInputStream(path));
            byte[] ev = new byte[Evdev.EVENT_SIZE];
            while (true) {
                in.readFully(ev);
                if (Evdev.type(ev, 0) == Evdev.EV_KEY) {
                    events.add(new KeyEvent(name, Evdev.code(ev, 0), Evdev.value(ev, 0), now() - captureStart));
                }
            }
        } catch (IOException e) {
            // Unreadable or vanished device: nothing to record.
        }
    }

    private void finishCapture(PrintWriter out, long seconds) throws Exception {
        long remaining = seconds * 1000 - (now() - captureStart);
        if (remaining > 0) Thread.sleep(remaining);
        List<KeyEvent> captured;
        synchronized (events) { captured = new ArrayList<>(events); }
        out.println("events=" + captured.size());
        for (int i = 0; i < captured.size() && i < MAX_EVENTS; i++) {
            KeyEvent e = captured.get(i);
            out.println("event t_ms=" + e.ms + " device=\"" + e.device + "\" code=" + e.code + " value=" + e.value);
        }
        for (String line : summarizeKeys(captured)) out.println(line);
    }

    /** One captured EV_KEY event. value: 1 down, 0 up, 2 autorepeat. */
    static final class KeyEvent {
        final String device;
        final int code, value;
        final long ms;
        KeyEvent(String device, int code, int value, long ms) {
            this.device = device; this.code = code; this.value = value; this.ms = ms;
        }
    }

    /**
     * Per device and key: presses, holds of at least HOLD_MS and the longest hold,
     * then the key held most often (ties go to the longest hold) as the likely knob.
     */
    static List<String> summarizeKeys(List<KeyEvent> events) {
        Map<String, long[]> keys = new LinkedHashMap<>();  // presses, holds, longest, down-at (-1 if up)
        for (KeyEvent e : events) {
            String key = "device=\"" + e.device + "\" code=" + e.code;
            long[] k = keys.get(key);
            if (k == null) keys.put(key, k = new long[] {0, 0, 0, -1});
            if (e.value == 1 && k[3] < 0) {
                k[0]++;
                k[3] = e.ms;
            } else if (e.value == 0 && k[3] >= 0) {
                long held = e.ms - k[3];
                if (held >= HOLD_MS) k[1]++;
                k[2] = Math.max(k[2], held);
                k[3] = -1;
            }
        }
        List<String> lines = new ArrayList<>();
        String likely = null;
        long[] best = null;
        for (Map.Entry<String, long[]> entry : keys.entrySet()) {
            long[] k = entry.getValue();
            lines.add("key " + entry.getKey() + " presses=" + k[0] + " holds=" + k[1] + " longest_hold_ms=" + k[2]);
            if (k[1] > 0 && (best == null || k[1] > best[1] || (k[1] == best[1] && k[2] > best[2]))) {
                likely = entry.getKey();
                best = k;
            }
        }
        lines.add(likely == null ? "likely_knob=none (no held key captured)" : "likely_knob " + likely);
        return lines;
    }

    /** "/dev/input/eventN" to device name, from /proc/bus/input/devices. */
    static Map<String, String> inputDevices(Reader procDevices) throws IOException {
        Map<String, String> devices = new LinkedHashMap<>();
        BufferedReader in = new BufferedReader(procDevices);
        String name = null;
        for (String line; (line = in.readLine()) != null; ) {
            if (line.startsWith("N: Name=")) {
                name = line.substring(8);
                if (name.length() >= 2 && name.startsWith("\"") && name.endsWith("\"")) {
                    name = name.substring(1, name.length() - 1);
                }
            } else if (name != null && line.startsWith("H: Handlers=")) {
                for (String h : line.substring(12).trim().split("\\s+")) {
                    if (h.startsWith("event")) devices.put("/dev/input/" + h, name);
                }
            } else if (line.isEmpty()) {
                name = null;
            }
        }
        return devices;
    }

    private static <T> T within(Callable<T> call) throws Exception {
        FutureTask<T> task = new FutureTask<>(call);
        Thread helper = new Thread(task, "port-probe-binder");
        helper.setDaemon(true);
        helper.start();
        return task.get(BINDER_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }

    private static String systemProperty(String name) throws Exception {
        return (String) Class.forName("android.os.SystemProperties").getMethod("get", String.class).invoke(null, name);
    }

    private static String hashOrEmpty(String path) {
        try {
            return OemVehicleClient.hash(path);
        } catch (Exception e) {
            return "";
        }
    }

    private static String yes(boolean value) { return value ? "yes" : "no"; }

    private static long now() { return System.nanoTime() / 1000000L; }
}
