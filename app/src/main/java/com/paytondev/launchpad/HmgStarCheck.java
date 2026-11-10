package com.paytondev.launchpad;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Constructor;

/** Read HMG-API's checked map and current OEM button actions; never edits either. */
public final class HmgStarCheck {
    static final String MAP_SHA256 = "c63d59112c0abdd196968aba74321d69edd5f740eff821d6d49ad6a980627ed1";
    static final String FRAMEWORK_SHA256 = "30fe1f660a53922223c44397fc7a0503b040d0a4b167f3a033682ba9897c5fca";
    static final String ACTION = "hmgapi.star.precondition-star.open";
    private HmgStarCheck() {}

    static boolean supports(boolean runnable, boolean checked, String folder,
                            Map<?, ?> points, Map<?, ?> expected) {
        if (!runnable || !checked || !"hyundai/neev/kor/260902".equals(folder)
                || expected.size() != 31 || !points.containsKey("framework.resources.get_text")
                || !points.containsKey("framework.resources.get_drawable")) return false;
        for (Map.Entry<?, ?> e : expected.entrySet()) {
            if (!(e.getKey() instanceof String) || !((String)e.getKey()).startsWith("buttonsettings.")
                    || !e.getValue().equals(points.get(e.getKey()))) return false;
        }
        return true;
    }

    static byte[] read(String path) throws Exception {
        InputStream in = new FileInputStream(path);
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            byte[] bytes = new byte[4096]; int n;
            while ((n = in.read(bytes)) != -1) {
                if (b.size() + n > 1024 * 1024) throw new IllegalArgumentException("file too large");
                b.write(bytes, 0, n);
            }
            return b.toByteArray();
        } finally { in.close(); }
    }
    static String sha256(byte[] bytes) throws Exception {
        StringBuilder s = new StringBuilder();
        for (byte b : MessageDigest.getInstance("SHA-256").digest(bytes)) s.append(String.format("%02x", b & 255));
        return s.toString();
    }
    @SuppressWarnings("unchecked")
    static Map<String,Object> object(String path) throws Exception {
        Class<?> json = Class.forName("hmgapi.engine.Json");
        return (Map<String,Object>)json.getMethod("object", String.class).invoke(null, new String(read(path), "UTF-8"));
    }
    static String md5(File f) throws Exception {
        if (!f.isFile()) return "absent";
        MessageDigest d = MessageDigest.getInstance("MD5");
        InputStream in = new FileInputStream(f);
        try { byte[] bytes = new byte[65536]; int n; while ((n = in.read(bytes)) != -1) d.update(bytes, 0, n); }
        finally { in.close(); }
        StringBuilder out = new StringBuilder();
        for (byte b : d.digest()) out.append(String.format("%02x", b & 255));
        return out.toString();
    }
    interface FileDigester { String digest(File f) throws Exception; }
    interface HookChecker { void check(Map<String,Object> expected, Map<String,Object> framework) throws Exception; }
    @SuppressWarnings("unchecked")
    static boolean readyAt(String root, String expectedFile, FileDigester digester, HookChecker checker) throws Exception {
        byte[] bytes = read(expectedFile);
        if (!MAP_SHA256.equals(sha256(bytes))) return false;
        Class<?> json = Class.forName("hmgapi.engine.Json");
        Map<String,Object> expected = (Map<String,Object>)json.getMethod("object", String.class)
            .invoke(null, new String(bytes, "UTF-8"));
        String folder = "hyundai/neev/kor/260902";
        String maps = root + "/data/hmgapi/maps/";
        String dir = maps + folder + "/";
        Map<String,Object> car = object(dir + "car.json");
        Map<String,Object> check = object(root + "/data/hmgapi/state/map-check.json");
        if (!"260902".equals(car.get("firmware")) || !(car.get("products") instanceof List)
                || !((List<?>)car.get("products")).contains("NEEV.KOR.")
                || !(car.get("apps") instanceof List) || !((List<?>)car.get("apps")).contains("buttonsettings")
                || !((List<?>)car.get("apps")).contains("framework") || !(car.get("files") instanceof Map)
                || !Boolean.TRUE.equals(check.get("ok")) || !folder.equals(check.get("folder"))
                || !"NEEV.KOR.0000.V039.001.260902".equals(check.get("product"))
                || !car.get("files").equals(check.get("files")) || !(check.get("points") instanceof Number)
                || ((Number)check.get("points")).longValue() < 406 || !(check.get("failed") instanceof List)
                || !((List<?>)check.get("failed")).isEmpty()) return false;
        String status = String.valueOf(car.get("status"));
        if (!"verified".equals(status) && !"probed".equals(status) && !"mapped".equals(status)) return false;
        Object af = car.get("app_files");
        if (!(af instanceof Map)) return false;
        Object bs = ((Map<?,?>)af).get("buttonsettings"), fr = ((Map<?,?>)af).get("framework");
        if (!(bs instanceof Map) || !(fr instanceof Map)) return false;
        Map<?,?> buttonApp = (Map<?,?>)bs, frameworkApp = (Map<?,?>)fr;
        if (!"com.android.buttonsettings".equals(buttonApp.get("package"))
                || !(buttonApp.get("files") instanceof List) || ((List<?>)buttonApp.get("files")).size() != 1
                || !((List<?>)buttonApp.get("files")).contains("/system/app/ButtonSettings.odex")
                || !(frameworkApp.get("files") instanceof List)
                || !((List<?>)frameworkApp.get("files")).contains("/system/framework/framework.odex")) return false;
        Map<String,Object> files = (Map<String,Object>)car.get("files");
        if (!"ea2e733ea011d05246c76e1b1a9116ea".equals(files.get("/system/app/ButtonSettings.odex"))) return false;
        if (!"ffe86fa1540c8dea3b5499b1191bb727".equals(files.get("/system/framework/framework.odex"))) return false;
        for (Map.Entry<String,Object> e : files.entrySet()) {
            String path = e.getKey();
            if (!path.startsWith("/system/") || path.contains("..") || !(e.getValue() instanceof String)) return false;
            String want = (String)e.getValue();
            if (!want.equals(digester.digest(new File(root + path))) && !want.equals(digester.digest(new File(root + "/data/hmgapi/stock" + path)))) return false;
        }
        String frameworkExpectedPath = new File(expectedFile).getParent() + "/framework-resources.json";
        byte[] frameworkBytes = read(frameworkExpectedPath);
        if (!FRAMEWORK_SHA256.equals(sha256(frameworkBytes))) return false;
        Map<String,Object> expectedFramework = (Map<String,Object>)json.getMethod("object", String.class)
            .invoke(null, new String(frameworkBytes, "UTF-8"));
        if (expectedFramework.size() != 3 || !expectedFramework.containsKey("framework.resources")
                || !expectedFramework.containsKey("framework.resources.get_text")
                || !expectedFramework.containsKey("framework.resources.get_drawable")) return false;
        Map<String,Object> points = object(dir + "buttonsettings.json");
        Map<String,Object> framework = object(dir + "framework.json");
        if (!expected.equals(points)) return false;
        for (Map.Entry<String,Object> e : expectedFramework.entrySet())
            if (!e.getValue().equals(framework.get(e.getKey()))) return false;
        // HMG-API merges apps in order; reject any other app that would shadow
        // a relevant point, and reject duplicate app names altogether.
        java.util.Set<String> seen = new java.util.HashSet<String>();
        for (Object entry : (List<?>)car.get("apps")) {
            if (!(entry instanceof String)) return false;
            String app = (String)entry;
            if (!app.matches("[a-z0-9_]+") || !seen.add(app)) return false;
            Map<String,Object> other = object(dir + app + ".json");
            if (!"buttonsettings".equals(app)) for (String id : expected.keySet())
                if (other.containsKey(id)) return false;
            if (!"framework".equals(app)) for (String id : expectedFramework.keySet())
                if (other.containsKey(id)) return false;
        }
        points.putAll(expectedFramework);
        Object missing = car.get("missing");
        if (missing instanceof Map) for (Object key : ((Map<?,?>)missing).keySet())
            if (String.valueOf(key).startsWith("buttonsettings.")) return false;
        File catalogue = new File(maps + "points.txt");
        if (catalogue.isFile()) for (String line : new String(read(catalogue.getPath()), "UTF-8").split("\\n")) {
            String id = line.trim(); if (id.startsWith("buttonsettings.") && !points.containsKey(id)) return false;
        }
        if (!supports(true, true, folder, points, expected)) return false;
        checker.check(expected, expectedFramework);
        return true;
    }
    static void checkHooks(Map<String,Object> expected, Map<String,Object> framework) throws Exception {
        // Use only HMG-API's point checker: verify-map rewrites its shared report.
        Class<?> mapClass = Class.forName("hmgapi.engine.CarMap");
        Object map = mapClass.getConstructor().newInstance();
        @SuppressWarnings("unchecked") Map<String,Object> points = (Map<String,Object>)mapClass.getField("points").get(map);
        points.putAll(expected); points.putAll(framework);
        Class<?> pcl = Class.forName("dalvik.system.PathClassLoader");
        Constructor<?> ctor = pcl.getConstructor(String.class, ClassLoader.class);
        ClassLoader boot = ClassLoader.getSystemClassLoader().getParent();
        ClassLoader cl = (ClassLoader)ctor.newInstance("/system/app/ButtonSettings.apk", boot);
        Method checkPoint = Class.forName("hmgapi.engine.MapCheck").getDeclaredMethod(
                "check", ClassLoader.class, mapClass, String.class, Map.class);
        checkPoint.setAccessible(true);
        for (Map.Entry<String,Object> e : expected.entrySet())
            checkPoint.invoke(null, cl, map, e.getKey(), e.getValue());
        for (Map.Entry<String,Object> e : framework.entrySet())
            checkPoint.invoke(null, boot, map, e.getKey(), e.getValue());
    }
    static boolean ready(String expectedFile) throws Exception {
        return readyAt("", expectedFile, new FileDigester() {
            @Override public String digest(File f) throws Exception { return md5(f); }
        }, new HookChecker() {
            @Override public void check(Map<String,Object> points, Map<String,Object> framework) throws Exception {
                checkHooks(points, framework);
            }
        });
    }
    static boolean noButtonUsesMod() throws Exception {
        Class<?> binder = Class.forName("android.os.IBinder");
        Object service = Class.forName("android.os.ServiceManager").getMethod("getService", String.class)
            .invoke(null, "com.hkmc.micom.mode");
        if (service == null) throw new IllegalStateException("mode service missing");
        Object manager = Class.forName("com.hkmc.mode.IModeManager$Stub").getMethod("asInterface", binder).invoke(null, service);
        Object head = Class.forName("com.hkmc.mode.IModeManager").getMethod("getCustomKeyAction").invoke(manager);
        Method get = Class.forName("android.os.SystemProperties").getMethod("get", String.class, String.class);
        Object wheel = get.invoke(null, "persist.sys.key.swrccustom", "");
        return head instanceof String && wheel instanceof String && !ACTION.equals(head) && !ACTION.equals(wheel);
    }
    public static void main(String[] args) {
        try {
            if (args.length == 2 && "ready".equals(args[0]) && ready(args[1])) { System.out.println("STAR_MAP_READY"); return; }
            if (args.length == 1 && "unused".equals(args[0]) && noButtonUsesMod()) { System.out.println("STAR_BUTTONS_UNUSED"); return; }
        } catch (Throwable error) { System.err.println("STAR_CHECK_ERROR " + error); }
        System.err.println("STAR_CHECK_UNAVAILABLE"); System.exit(1);
    }
}
