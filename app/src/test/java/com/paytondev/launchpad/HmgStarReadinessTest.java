package com.paytondev.launchpad;

import hmgapi.engine.Json;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.Assert.*;

/** File-backed fixtures; HMG's parser and point checker are explicitly mocked. */
public class HmgStarReadinessTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();
    private Path root, dir, checkFile, data;
    private Map<String,Object> car, check, files, expected, framework, other;
    private int calls;
    private HmgStarCheck.FileDigester digester;
    private HmgStarCheck.HookChecker hooks;
    private static final String BUTTON_MD5 = "ea2e733ea011d05246c76e1b1a9116ea";
    private static Map<String,Object> map(Object... pairs) {
        Map<String,Object> out = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) out.put((String)pairs[i], pairs[i + 1]);
        return out;
    }
    private void write(Path p, String value) throws Exception {
        Files.createDirectories(p.getParent()); Files.write(p, value.getBytes(StandardCharsets.UTF_8));
    }
    @Before public void prepare() throws Exception {
        Json.objects.clear(); calls = 0;
        root = tmp.newFolder("headunit").toPath();
        dir = root.resolve("data/hmgapi/maps/hyundai/neev/kor/260902");
        checkFile = root.resolve("data/hmgapi/state/map-check.json");
        File base = new File("tune-service/star/data");
        if (!base.isDirectory()) base = new File("../tune-service/star/data");
        data = base.getAbsoluteFile().toPath();
        write(root.resolve("system/app/ButtonSettings.odex"), "fixture-buttonsettings");
        // Greater than the JSON 1 MiB cap: system file hashing must stream.
        Path f = root.resolve("system/framework/framework.odex"); Files.createDirectories(f.getParent());
        Files.write(f, new byte[1024 * 1024 + 100]);
        files = map("/system/app/ButtonSettings.odex", BUTTON_MD5,
                    "/system/framework/framework.odex", "ffe86fa1540c8dea3b5499b1191bb727");
        String fixtureFrameworkMd5 = HmgStarCheck.md5(f.toFile());
        Map<String,Object> appFiles = map("buttonsettings", map("package", "com.android.buttonsettings",
            "files", Collections.singletonList("/system/app/ButtonSettings.odex")),
            "framework", map("package", null, "files", Collections.singletonList("/system/framework/framework.odex")));
        car = map("firmware", "260902", "products", Collections.singletonList("NEEV.KOR."),
            "apps", Arrays.asList("framework", "buttonsettings", "other"), "files", files,
            "app_files", appFiles, "status", "probed", "missing", new LinkedHashMap<String,Object>());
        check = map("ok", true, "folder", "hyundai/neev/kor/260902", "product", "NEEV.KOR.0000.V039.001.260902",
                    "files", new LinkedHashMap<>(files), "points", 406L, "failed", new ArrayList<>());
        expected = new LinkedHashMap<>();
        for (int i = 0; i < 31; i++) expected.put("buttonsettings.p" + i, map("class", "type" + i));
        framework = map("framework.resources", map("class", "android.content.res.Resources"),
            "framework.resources.get_text", map("method", "getText"),
            "framework.resources.get_drawable", map("method", "getDrawable"));
        other = new LinkedHashMap<>();
        write(dir.resolve("car.json"), "CAR"); write(checkFile, "CHECK");
        write(dir.resolve("buttonsettings.json"), "BUTTONS"); write(dir.resolve("framework.json"), "FRAMEWORK");
        write(dir.resolve("other.json"), "OTHER");
        Json.objects.put("CAR", car); Json.objects.put("CHECK", check);
        Json.objects.put("BUTTONS", expected); Json.objects.put("FRAMEWORK", framework); Json.objects.put("OTHER", other);
        Json.objects.put(new String(Files.readAllBytes(data.resolve("buttonsettings.json")), StandardCharsets.UTF_8), new LinkedHashMap<>(expected));
        Json.objects.put(new String(Files.readAllBytes(data.resolve("framework-resources.json")), StandardCharsets.UTF_8), new LinkedHashMap<>(framework));
        digester = file -> {
            if (file.getPath().endsWith("/system/app/ButtonSettings.odex") && file.isFile()
                    && "fixture-buttonsettings".equals(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8))) return BUTTON_MD5;
            String actual = HmgStarCheck.md5(file);
            if (file.getPath().endsWith("/system/framework/framework.odex") && fixtureFrameworkMd5.equals(actual))
                return "ffe86fa1540c8dea3b5499b1191bb727";
            return actual;
        };
        hooks = (a,b) -> { assertEquals(31, a.size()); assertEquals(3, b.size()); calls++; };
    }
    private boolean ready() throws Exception {
        return HmgStarCheck.readyAt(root.toString(), data.resolve("buttonsettings.json").toString(), digester, hooks);
    }
    @Test public void matchingFilesAndReportAreReadOnly() throws Exception {
        byte[] before = Files.readAllBytes(checkFile); long mtime = Files.getLastModifiedTime(checkFile).toMillis();
        assertTrue(ready()); assertEquals(1, calls); assertArrayEquals(before, Files.readAllBytes(checkFile));
        assertEquals(mtime, Files.getLastModifiedTime(checkFile).toMillis());
    }
    @Test public void staleOrDifferentReportFailsClosed() throws Exception {
        check.put("points", 375L); assertFalse(ready()); check.put("points", 406L);
        check.put("files", Collections.emptyMap()); assertFalse(ready());
        check.put("files", new LinkedHashMap<>(files)); check.put("ok", false); assertFalse(ready());
        assertEquals(0, calls);
    }
    @Test public void alteredFrameworkOrDuplicateAppFailsClosed() throws Exception {
        framework.put("framework.resources", map("class", "wrong")); assertFalse(ready());
        framework.put("framework.resources", map("class", "android.content.res.Resources"));
        other.put("framework.resources.get_text", map("method", "malicious")); assertFalse(ready());
        other.clear(); other.put("buttonsettings.p1", map("class", "wrong")); assertFalse(ready());
        assertEquals(0, calls);
    }
    @Test public void fileDigestOrAppOwnershipMismatchFailsClosed() throws Exception {
        write(root.resolve("system/app/ButtonSettings.odex"), "altered"); assertFalse(ready());
        write(root.resolve("system/app/ButtonSettings.odex"), "fixture-buttonsettings");
        Files.write(root.resolve("system/framework/framework.odex"), new byte[]{1}); assertFalse(ready());
        files.remove("/system/framework/framework.odex"); check.put("files", new LinkedHashMap<>(files));
        assertFalse(ready());
        assertEquals(0, calls);
    }
    @Test public void reflectionFailureDoesNotBecomeSuccess() throws Exception {
        hooks = (a,b) -> { throw new ReflectiveOperationException("missing method"); };
        try { ready(); fail("must reject failed reflection"); }
        catch (ReflectiveOperationException expected) { assertEquals("missing method", expected.getMessage()); }
    }
}
