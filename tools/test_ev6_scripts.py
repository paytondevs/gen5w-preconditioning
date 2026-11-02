#!/usr/bin/env python3
"""Host-only EV6 profile and toolbox regressions; never connects to a car.

Requires Python 3 and Bash (Git for Windows is supported). All hook destinations
are replaced with temporary fixture paths; vehicle operations are mocked.
"""
from pathlib import Path
import os
import shutil
import stat
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
BASH = shutil.which("bash")
if os.name == "nt":
    git_bash = Path(os.environ.get("ProgramFiles", "C:/Program Files")) / "Git/bin/bash.exe"
    if git_bash.is_file():
        BASH = str(git_bash)
if not BASH:
    raise SystemExit("Bash is required (install Git for Windows on Windows).")


def shell_path(path):
    value = Path(path).resolve().as_posix()
    return "/" + value[0].lower() + value[2:] if os.name == "nt" else value


def quote(path):
    return "'" + shell_path(path).replace("'", "'\"'\"'") + "'"


def run(script, **env):
    return subprocess.run([BASH, shell_path(script)], capture_output=True,
                          text=True, timeout=15, env=dict(os.environ, **env))


class Ev6ScriptsTest(unittest.TestCase):
    def test_shell_syntax(self):
        sources = list((ROOT / "tune-service").rglob("*.sh"))
        sources += list((ROOT / "usb").rglob("*.sh"))
        sources.append(ROOT / "tune-service/knob.conf")
        for source in sources:
            with self.subTest(source=source.relative_to(ROOT)):
                result = subprocess.run([BASH, "-n", shell_path(source)],
                                        capture_output=True, text=True, timeout=10)
                self.assertEqual(result.returncode, 0, result.stderr)

    def test_tune_selection_keeps_key_duration_and_target(self):
        ev6, ioniq5_neev_us, ioniq5_neev23_us = ("daudio_wp_daudioplus_cvev_us", "daudio_wp_daudioplus_neev_us",
                               "daudio_wp_daudioplus_neev23_us")
        ioniq6_ceev_us = "daudio_wp_daudioplus_ceev_us"
        ev6_cvev_kr = "daudio_wp_daudioplus_cvev_kr"
        cases = [
            ("wp_daudioplus_cvev_us", "CVEV.USA.0000.V015.012.260507", "19", ev6),
            ("wp_daudioplus_cvev_us", "CVEV.USA.0000.V014.011.251120", "19", ev6),
            ("wp_daudioplus_cvev_us", "CVEV.USA.S5W_M.V014.011.251120", "19", ioniq5_neev23_us),
            ("wp_daudioplus_cvev_us", "CV_E.USA.S5W_M.V014.011.251120", "19", ioniq5_neev23_us),
            ("wp_daudioplus_cvev_us", "CVEV.USA.0000.V014.011.251120", "20", ioniq5_neev23_us),
            ("wp_daudioplus_neev23_us", "CVEV.USA.0000.V014.011.251120", "19", ioniq5_neev23_us),
            ("wp_daudioplus_ceev_us", "CVEV.USA.0000.V014.011.251120", "19", ioniq5_neev23_us),
            ("other", "CVEV.USA.0000.V014.011.251120", "19", ioniq5_neev23_us),
            ("wp_daudioplus_neev23_us", "original", "19", ioniq5_neev23_us),
            ("wp_daudioplus_neev23_us", "NEEV23.USA.0000.V015.011.260206", "19", ioniq5_neev23_us),
            ("wp_daudioplus_neev23_us", "NEEV23.USA.0000.V016.010.260903", "19", ioniq5_neev23_us),
            ("wp_daudioplus_cvev_us", "other", "19", ioniq5_neev23_us),
            ("wp_daudioplus_cvev_us", "CVEV.USA.0000.V015.012.260507", "20", ioniq5_neev23_us),
            ("other", "CVEV.USA.0000.V015.012.260507", "19", ioniq5_neev23_us),
            ("wp_daudioplus_neev_us", "NEEV.USA.0000.V015.012.260507", "19", ioniq5_neev_us),
            # NEEV US 240822 has no TUNE entry: its capture didn't confirm a TUNE hold.
            ("wp_daudioplus_neev_us", "NEEV.USA.0000.V012.010.240822", "19", ioniq5_neev23_us),
            ("wp_daudioplus_neev_us", "other", "19", ioniq5_neev23_us),
            ("wp_daudioplus_neev_us", "NEEV.USA.0000.V015.012.260507", "20", ioniq5_neev23_us),
            ("wp_daudioplus_cvev_us", "NEEV.USA.0000.V015.012.260507", "19", ioniq5_neev23_us),
            ("wp_daudioplus_ceev_us", "CEEV.USA.0000.V015.012.260507", "19", ioniq6_ceev_us),
            ("wp_daudioplus_ceev_us", "CEEV.USA.S5W_M.V015.012.260507", "19", ioniq5_neev23_us),
            ("wp_daudioplus_ceev_us", "other", "19", ioniq5_neev23_us),
            ("wp_daudioplus_ceev_us", "CEEV.USA.0000.V015.012.260507", "20", ioniq5_neev23_us),
            ("wp_daudioplus_cvev_us", "CEEV.USA.0000.V015.012.260507", "19", ioniq5_neev23_us),
            ("wp_daudioplus_ceev_us", "CVEV.USA.0000.V015.012.260507", "19", ioniq5_neev23_us),
            ("wp_daudioplus_cvev_kr", "CVEV.KOR.0000.V039.001.260902", "19", ev6_cvev_kr),
            ("wp_daudioplus_cvev_kr", "CVEV.KOR.S5W_M.V039.001.260902", "19", ioniq5_neev23_us),
            ("wp_daudioplus_cvev_kr", "CV_E.KOR.S5W_M.V039.001.260902", "19", ioniq5_neev23_us),
            ("wp_daudioplus_cvev_kr", "CVEV.KOR.0000.V039.001.260902", "20", ioniq5_neev23_us),
            ("wp_daudioplus_cvev_kr", "CVEV.USA.0000.V015.012.260507", "19", ioniq5_neev23_us),
            ("wp_daudioplus_cvev_kr", "other", "19", ioniq5_neev23_us),
            ("wp_daudioplus_cvev_us", "CVEV.KOR.0000.V039.001.260902", "19", ioniq5_neev23_us),
            ("wp_daudioplus_neev_kr", "CVEV.KOR.0000.V039.001.260902", "19", ioniq5_neev23_us),
            ("wp_daudioplus_cvev_kr_knob", "CVEV.KOR.0000.V039.001.260902", "19", ioniq5_neev23_us),
            ("other", "CVEV.KOR.0000.V039.001.260902", "19", ioniq5_neev23_us),
            ("", "", "", ioniq5_neev23_us),
        ]
        with tempfile.TemporaryDirectory(prefix="launchpad-tune-") as tmp:
            script = Path(tmp) / "profile.sh"
            script.write_text('''getprop() {
 case "$1" in
 ro.product.device) printf '%s' "$TEST_DEVICE" ;;
 ro.build.product) printf '%s' "$TEST_SOFTWARE" ;;
 ro.build.version.sdk) printf '%s' "$TEST_SDK" ;;
 esac
}
''' + (ROOT / "tune-service/knob.conf").read_text() + '''
printf '%s|%s|%s|%s' "$DEVICE_NAME" "$KEY_CODE" "$HOLD_MS" "$TARGET_COMPONENT"
''', newline="\n")
            for device, software, sdk, expected in cases:
                with self.subTest(device=device, software=software, sdk=sdk):
                    result = run(script, TEST_DEVICE=device, TEST_SOFTWARE=software, TEST_SDK=sdk)
                    self.assertEqual(result.returncode, 0, result.stderr)
                    self.assertEqual(result.stdout, expected + "|17|800|com.paytondev.precondition/com.paytondev.launchpad.MainActivity")

    def run_port_kit(self, folder, probe_rc="0"):
        """Run the real collect.sh from a fixture USB with every car command mocked."""
        usb = folder / "usb"
        kit = usb / "launchpad-port"
        if not kit.exists():
            kit.mkdir(parents=True)
            (kit / "Launchpad.apk").write_bytes(b"probe dex container")
            (folder / "framework").mkdir()
            (folder / "framework/automotive.odex").write_bytes(b"odex")
            (folder / "devices").write_text(
                'I: Bus=0019\nN: Name="daudio_wp_daudioplus_xyz_us"\nP: Phys=\nH: Handlers=kbd event1\n\n',
                newline="\n")
            (folder / "environ.rc").write_text(
                "on init\n    export ANDROID_ROOT /system\n    export BOOTCLASSPATH /system/framework/core.jar\n",
                newline="\n")
            nemesis = folder / "nemesis"
            nemesis.write_text('#!/bin/sh\necho "$*" >> "$TEST_FOLDER/nemesis-calls"\n'
                               'echo "paytondev.launchpad|1.3.0"\n', newline="\n")
            nemesis.chmod(0o755)
            collect = (ROOT / "usb/port-kit/collect.sh").read_text().replace("@VERSION@", "9.8.7")
            for name, value in (("FRAMEWORK_DIR=/system/framework", folder / "framework"),
                                ("INPUT_DEVICES=/proc/bus/input/devices", folder / "devices"),
                                ("PROBE_WORK=/data/local/tmp/launchpad-port", folder / "work"),
                                ("PROBE_CACHE=/data/dalvik-cache/data@local@tmp@launchpad-port@Launchpad.apk@classes.dex",
                                 folder / "cache.dex")):
                self.assertIn(name, collect)
                collect = collect.replace(name, name.split("=")[0] + "=" + quote(value))
            (kit / "collect.sh").write_text(collect, newline="\n")
            (kit / "lib.sh").write_text((ROOT / "usb/launchpad/lib.sh").read_text(), newline="\n")
            common = (ROOT / "tune-service/scripts/common.sh").read_text()
            common = common.replace("NEMESIS=/system/xbin/nemesis", "NEMESIS=" + quote(nemesis))
            common = common.replace("BOOT_ENV=/init.environ.rc", "BOOT_ENV=" + quote(folder / "environ.rc"))
            common += '''
export PATH=/usr/bin:/bin
getprop() {
 if [ $# = 0 ]; then
  printf '%s\\n' '[ro.build.product]: [XYZ.USA.0000.V015.012.999999]' \\
   '[ro.product.device]: [wp_daudioplus_xyz_us]' '[ro.build.version.sdk]: [19]' \\
   '[ro.serialno]: [SECRET-serial]' '[ro.boot.serialno]: [SECRET-boot]' \\
   '[ro.product.wifimac]: [SECRET-mac]' '[ro.product.vin]: [SECRET-vin]' \\
   '[persist.sys.owner]: [SECRET-other]'
  return
 fi
 case "$1" in
 ro.build.version.sdk) echo 19 ;;
 ro.product.device) echo wp_daudioplus_xyz_us ;;
 ro.build.product) echo XYZ.USA.0000.V015.012.999999 ;;
 sys.boot_completed) echo 1 ;;
 esac
}
service() { echo "0 com.hkmc.micom.hev: [com.hkmc.hev.IHEVService]"; }
sleep() { :; }
cmp() { case "$1" in -*) echo 'toolbox cmp rejects options' >&2; return 2 ;; esac; command cmp "$@"; }
app_process() {
 echo "$CLASSPATH|$BOOTCLASSPATH|$*" >> "$TEST_FOLDER/probe-calls"
 cmp "$CLASSPATH" "$TEST_FOLDER/usb/launchpad-port/Launchpad.apk" || return 3
 printf '\\n[end]\\nprobe_complete=yes\\n' >> "$4"
 return "$TEST_PROBE_RC"
}
'''
            (kit / "common.sh").write_text(common, newline="\n")
        (folder / "cache.dex").write_bytes(b"stale cache")
        return run(kit / "collect.sh", TEST_FOLDER=shell_path(folder), TEST_PROBE_RC=probe_rc)

    def test_port_kit_collects_a_filtered_report_and_cleans_up(self):
        with tempfile.TemporaryDirectory(prefix="launchpad-port-") as tmp:
            folder = Path(tmp)
            result = self.run_port_kit(folder)
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
            report = (folder / "usb/launchpad-port-report.txt").read_text()
            for section in ("[kit]", "[identity]", "[framework_files]", "[services]",
                            "[input]", "[nemesis]", "[end]"):
                self.assertIn(section, report)
            self.assertIn("kit_version=9.8.7", report)
            self.assertIn("software=XYZ.USA.0000.V015.012.999999", report)
            self.assertIn("[ro.product.device]: [wp_daudioplus_xyz_us]", report)
            self.assertIn("bootclasspath=/system/framework/core.jar", report)
            self.assertIn("automotive.odex", report)
            self.assertIn("com.hkmc.hev.IHEVService", report)
            self.assertIn('N: Name="daudio_wp_daudioplus_xyz_us"', report)
            self.assertIn("H: Handlers=kbd event1", report)
            self.assertNotIn("P: Phys", report)
            self.assertNotIn("SECRET", report)
            # Read-only: NEMESIS is only asked for its package list.
            self.assertEqual((folder / "nemesis-calls").read_text().splitlines(), ["pkg-list"])
            calls = (folder / "probe-calls").read_text().splitlines()
            self.assertEqual(len(calls), 1)
            self.assertTrue(calls[0].endswith("|/system/bin --nice-name=launchpad-port-probe "
                                              "com.paytondev.launchpad.PortProbe "
                                              + shell_path(folder / "usb/launchpad-port-report.txt") + " 30"),
                            calls[0])
            self.assertIn("|/system/framework/core.jar|", calls[0])
            self.assertFalse((folder / "work").exists())
            self.assertFalse((folder / "cache.dex").exists())
            log = (folder / "usb/launchpad-collect.log").read_text()
            self.assertIn("SUCCESS. Report saved to launchpad-port-report.txt", log)
            self.assertNotIn("Reboot", log)
            # A second run keeps the first report.
            result = self.run_port_kit(folder)
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
            self.assertEqual((folder / "usb/launchpad-port-report.txt.old").read_text(), report)

    def test_port_kit_failed_probe_still_cleans_up(self):
        with tempfile.TemporaryDirectory(prefix="launchpad-port-") as tmp:
            folder = Path(tmp)
            result = self.run_port_kit(folder, probe_rc="1")
            self.assertNotEqual(result.returncode, 0)
            self.assertIn("[identity]", (folder / "usb/launchpad-port-report.txt").read_text())
            self.assertFalse((folder / "work").exists())
            self.assertFalse((folder / "cache.dex").exists())
            self.assertIn("FAILED.", (folder / "usb/launchpad-collect.log").read_text())



APP = "com.paytondev.precondition"
LEGACY = "com.paytondev.launchpad"
SERVICE = APP + "/com.paytondev.launchpad.PreconditioningService"
USER_APK = "/data/app/com.paytondev.precondition-1.apk"
PREFS = ("launchpad.xml", "launchpad.xml.bak", "launchpad_preconditioning.xml")
# Car commands the hooks use, stubbed and recorded in $TEST_FOLDER.
HOOK_STUBS = r"""
export PATH=/usr/bin:/bin
getprop() { [ "$1" = sys.boot_completed ] && echo "${TEST_BOOT:-1}"; }
system_rw() { [ "$TEST_RW" = fail ] && return 1; echo rw >> "$TEST_FOLDER/remounts"; }
system_restore() { echo ro >> "$TEST_FOLDER/remounts"; }
pm() { echo "pm $*" >> "$TEST_FOLDER/calls"; [ -n "$TEST_PM_PATH" ] && echo "package:$TEST_PM_PATH"; return 0; }
am() { echo "am $*" >> "$TEST_FOLDER/calls"; [ -n "$TEST_AM_ERROR" ] && echo "Error: $TEST_AM_ERROR" >&2; return 0; }
ls() {
 if [ "$1" = -nd ]; then
  [ -d "$2" ] && echo "drwxr-x--x 10123    10123    2026-11-02 12:00 ${2##*/}"
 else command ls "$@"; fi
}
chown() { echo "chown $*" >> "$TEST_FOLDER/calls"; }
sync() { :; }
sleep() { :; }
cmp() { case "$1" in -*) echo 'toolbox cmp rejects options' >&2; return 2 ;; esac; command cmp "$@"; }
"""
FAKE_NEMESIS = """#!/bin/sh
echo "$*" >> "$TEST_FOLDER/nemesis-calls"
case "$1" in
install) : > "$TEST_FOLDER/registered" ;;
pkg-list) [ -n "$TEST_PKG_INSTALLED" ] || [ -f "$TEST_FOLDER/registered" ] && echo "paytondev.launchpad|1.7.0" ;;
service) echo "paytondev.launchpad/knobd|restart|running|42" ;;
esac
exit 0
"""


class PackageHooksTest(unittest.TestCase):
    """Run the real NEMESIS hooks and the manual USB installer against a temporary
    tree. Every /system, /data and Android path is redirected there."""

    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="launchpad-hooks-")
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.scripts = self.root / "package/scripts"
        self.scripts.mkdir(parents=True)
        self.system_apk = self.root / "system/app/Launchpad.apk"
        self.system_apk.parent.mkdir(parents=True)
        self.app_data = self.root / "data/data" / APP
        self.legacy_data = self.root / "data/data" / LEGACY
        self.backup = self.root / "data/nemesis/backup/paytondev.launchpad"
        self.prefs_backup = self.backup / "prefs"
        self.work = self.root / "work"
        self.stage = self.root / "stage"
        self.dexcache = self.root / "classes.dex"
        nemesis = self.root / "nemesis"
        nemesis.write_text(FAKE_NEMESIS, newline="\n")
        nemesis.chmod(0o755)
        common = (ROOT / "tune-service/scripts/common.sh").read_text()
        for line, value in (
                ("NEMESIS=/system/xbin/nemesis", nemesis),
                ("HERE=/data/nemesis/apps/paytondev.launchpad", self.root / "package"),
                ("DEXCACHE=/data/dalvik-cache/data@nemesis@apps@paytondev.launchpad@app@Launchpad.apk@classes.dex",
                 self.dexcache),
                ("APP_DATA=/data/data/$APP_PACKAGE", self.app_data),
                ("LEGACY_DATA=/data/data/$LEGACY_PACKAGE", self.legacy_data),
                ("SYSAPK=/system/app/Launchpad.apk", self.system_apk),
                ("BACKUP_DIR=/data/nemesis/backup/paytondev.launchpad", self.backup),
                ("WORK=/data/local/tmp/launchpad", self.work),
                ("STAGE=/data/local/tmp/nemesis/stage/paytondev.launchpad", self.stage)):
            self.assertEqual(common.count("\n" + line + "\n"), 1, line)
            common = common.replace("\n" + line + "\n", "\n" + line.split("=")[0] + "=" + quote(value) + "\n")
        self.common = common + HOOK_STUBS
        (self.scripts / "common.sh").write_text(self.common, newline="\n")
        source = ". /data/nemesis/apps/paytondev.launchpad/scripts/common.sh"
        for hook in ("preinstall", "postinstall", "preuninstall"):
            text = (ROOT / f"tune-service/scripts/{hook}.sh").read_text()
            if hook == "preinstall":
                # Runs from NEMESIS's staging copy, so it finds common.sh next to itself.
                self.assertIn('. "${0%/*}/common.sh"', text)
            else:
                self.assertIn(source, text)
                text = text.replace(source, ". " + quote(self.scripts / "common.sh"))
            (self.scripts / f"{hook}.sh").write_text(text, newline="\n")

    def hook(self, name, **env):
        return run(self.scripts / f"{name}.sh", TEST_FOLDER=shell_path(self.root), **env)

    def lines(self, name):
        path = self.root / name
        return path.read_text().splitlines() if path.exists() else []

    def write_prefs(self, folder, names=PREFS):
        folder.mkdir(parents=True, exist_ok=True)
        for name in names:
            (folder / name).write_text(f"<map><string name=\"{name}\">kept</string></map>\n")

    def assertPrefs(self, folder, names=PREFS):
        self.assertEqual(sorted(p.name for p in folder.iterdir()), sorted(names))
        for name in names:
            self.assertIn(f"\"{name}\">kept", (folder / name).read_text())

    # preinstall

    def test_preinstall_without_1x_changes_nothing(self):
        result = self.hook("preinstall")
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertEqual(self.lines("remounts"), [])
        self.assertEqual(self.lines("calls"), [])
        self.assertFalse(self.backup.exists())

    def test_preinstall_moves_installed_1x_over_and_continues(self):
        self.system_apk.write_bytes(b"1.x APK")
        self.write_prefs(self.legacy_data / "shared_prefs")
        result = self.hook("preinstall")
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertFalse(self.system_apk.exists())
        self.assertEqual(self.lines("remounts"), ["rw", "ro"])
        self.assertPrefs(self.prefs_backup)
        calls = self.lines("calls")
        self.assertIn("am force-stop " + LEGACY, calls)
        self.assertNotIn("am force-stop " + APP, calls)

    def test_preinstall_saves_settings_of_1x_uninstalled_without_a_reboot(self):
        # NEMESIS Manager's Uninstall removed the /system copy, but Android keeps
        # 1.x's data until the head unit restarts.
        self.write_prefs(self.legacy_data / "shared_prefs")
        result = self.hook("preinstall")
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertPrefs(self.prefs_backup)
        self.assertEqual(self.lines("remounts"), [])

    def test_preinstall_keeps_a_newer_backup(self):
        # An update: the old 2.x preuninstall already saved this app's settings.
        self.write_prefs(self.prefs_backup, ("launchpad.xml",))
        (self.prefs_backup / "launchpad.xml").write_text("<map>newer</map>\n")
        self.write_prefs(self.legacy_data / "shared_prefs")
        result = self.hook("preinstall")
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertEqual(sorted(p.name for p in self.prefs_backup.iterdir()), ["launchpad.xml"])
        self.assertEqual((self.prefs_backup / "launchpad.xml").read_text(), "<map>newer</map>\n")

    def test_preinstall_stops_when_1x_cannot_be_removed(self):
        self.system_apk.write_bytes(b"1.x APK")
        result = self.hook("preinstall", TEST_RW="fail")
        self.assertEqual(result.returncode, 1, result.stdout + result.stderr)
        self.assertTrue(self.system_apk.exists())
        self.assertEqual(self.lines("remounts"), [])

    def test_installing_over_1x_keeps_its_settings_in_one_run(self):
        self.system_apk.write_bytes(b"1.x APK")
        self.write_prefs(self.legacy_data / "shared_prefs")
        result = self.hook("preinstall")
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        # NEMESIS runs pm install, which creates the new app's data directory.
        self.app_data.mkdir(parents=True)
        result = self.hook("postinstall", TEST_PM_PATH=USER_APK)
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertPrefs(self.app_data / "shared_prefs")
        self.assertFalse(self.backup.exists())
        self.assertFalse(self.system_apk.exists())

    # preuninstall

    def test_preuninstall_update_saves_settings(self):
        self.stage.mkdir()
        (self.stage / "NEMESIS.data").write_text("[nemesis]\n")
        self.write_prefs(self.app_data / "shared_prefs")
        self.dexcache.write_bytes(b"dex")
        result = self.hook("preuninstall")
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertPrefs(self.prefs_backup)
        self.assertPrefs(self.app_data / "shared_prefs")
        self.assertFalse(self.dexcache.exists())
        self.assertEqual(self.lines("remounts"), [])
        self.assertIn("am force-stop " + APP, self.lines("calls"))

    def test_preuninstall_update_keeps_an_unrestored_backup(self):
        self.stage.mkdir()
        (self.stage / "NEMESIS.data").write_text("[nemesis]\n")
        self.write_prefs(self.prefs_backup)
        result = self.hook("preuninstall")
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertPrefs(self.prefs_backup)

    def test_preuninstall_removal_cleans_up(self):
        for legacy in (False, True):
            with self.subTest(legacy=legacy):
                (self.root / "remounts").unlink(missing_ok=True)
                self.write_prefs(self.prefs_backup)
                self.work.mkdir(exist_ok=True)
                if legacy:
                    self.system_apk.write_bytes(b"1.x APK")
                result = self.hook("preuninstall")
                self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
                self.assertFalse(self.backup.exists())
                self.assertFalse(self.work.exists())
                self.assertFalse(self.system_apk.exists())
                self.assertEqual(self.lines("remounts"), ["rw", "ro"] if legacy else [])

    # postinstall

    def test_postinstall_restores_settings_then_starts_the_service_last(self):
        self.write_prefs(self.prefs_backup)
        self.app_data.mkdir(parents=True)
        self.work.mkdir()
        result = self.hook("postinstall", TEST_PM_PATH=USER_APK)
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        restored = self.app_data / "shared_prefs"
        self.assertPrefs(restored)
        if os.name != "nt":
            self.assertEqual(stat.S_IMODE(restored.stat().st_mode), 0o771)
            for name in PREFS:
                self.assertEqual(stat.S_IMODE((restored / name).stat().st_mode), 0o660)
        calls = self.lines("calls")
        chowns = [c for c in calls if c.startswith("chown ")]
        self.assertEqual(chowns, ["chown 10123:10123 " + shell_path(restored)] +
                         ["chown 10123:10123 " + shell_path(restored / n) for n in
                          ("launchpad.xml", "launchpad.xml.bak", "launchpad_preconditioning.xml")])
        # A force-stop would put the package back into Android's stopped state.
        self.assertEqual([c for c in calls if c.startswith("am ")], ["am startservice -n " + SERVICE])
        self.assertEqual(calls[-1], "am startservice -n " + SERVICE)
        self.assertFalse(self.backup.exists())
        self.assertFalse(self.work.exists())

    def test_postinstall_without_backup_only_starts_the_service(self):
        self.app_data.mkdir(parents=True)
        result = self.hook("postinstall", TEST_PM_PATH=USER_APK)
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertEqual(self.lines("calls"), ["pm path " + APP, "am startservice -n " + SERVICE])
        self.assertFalse((self.app_data / "shared_prefs").exists())

    def test_postinstall_never_fails_the_install(self):
        self.write_prefs(self.prefs_backup)
        # No app data directory and no pm path: settings stay in the backup.
        result = self.hook("postinstall", TEST_AM_ERROR="Not found; no service started.")
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertPrefs(self.prefs_backup)
        self.assertIn("WARN: pm path shows 'nothing'", result.stdout)
        self.assertIn("WARN: could not start", result.stdout)

    def test_postinstall_with_android_down_leaves_the_backup(self):
        self.write_prefs(self.prefs_backup)
        result = self.hook("postinstall", TEST_BOOT="0")
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertEqual(self.lines("calls"), [])
        self.assertPrefs(self.prefs_backup)

    # USB verification and the manual installer

    def test_usb_verification_requires_a_user_app_and_no_1x_copy(self):
        library = (ROOT / "usb/launchpad/lib.sh").read_text()
        start = library.index("verify_installed() {")
        end = library.index("\n}", start) + 2
        script = self.root / "verify.sh"
        script.write_text(self.common + library[start:end] + """
say() { :; }
pkg_installed() { return 0; }
knobd_state() { echo running; }
verify_installed
""", newline="\n")
        for pm_path, legacy, ok in ((USER_APK, False, True),
                                    (USER_APK, True, False),
                                    ("/system/app/Launchpad.apk", False, False),
                                    ("", False, False)):
            with self.subTest(pm_path=pm_path, legacy=legacy):
                if legacy:
                    self.system_apk.write_bytes(b"1.x APK")
                else:
                    self.system_apk.unlink(missing_ok=True)
                result = run(script, TEST_FOLDER=shell_path(self.root), TEST_PM_PATH=pm_path)
                self.assertEqual(result.returncode == 0, ok, result.stdout + result.stderr)

    def run_install(self, **env):
        drive = self.root / "drive"
        if not drive.exists():
            (drive / "launchpad").mkdir(parents=True)
            (drive / "packages").mkdir()
            (drive / "packages/paytondev.launchpad.zip").write_bytes(b"package")
            for name in ("install.sh", "lib.sh"):
                (drive / "launchpad" / name).write_text(
                    (ROOT / "usb/launchpad" / name).read_text(), newline="\n")
            (drive / "launchpad/common.sh").write_text(self.common, newline="\n")
        result = run(drive / "launchpad/install.sh", TEST_FOLDER=shell_path(self.root), **env)
        log = drive / "launchpad-install.log"
        return result, log.read_text() if log.exists() else ""

    def test_usb_install_and_update(self):
        package = shell_path(self.root / "drive/packages/paytondev.launchpad.zip")
        for installed, command in (("", "install " + package), ("1", "install --update " + package)):
            with self.subTest(installed=bool(installed)):
                (self.root / "nemesis-calls").unlink(missing_ok=True)
                (self.root / "registered").unlink(missing_ok=True)
                result, log = self.run_install(TEST_PKG_INSTALLED=installed,
                                               TEST_PM_PATH=USER_APK)
                self.assertEqual(result.returncode, 0, log)
                self.assertIn(command, self.lines("nemesis-calls"))
                self.assertIn("ok: com.paytondev.precondition installed at /data/app/", log)


if __name__ == "__main__":
    unittest.main(verbosity=2)
