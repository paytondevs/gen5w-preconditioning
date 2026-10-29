#!/usr/bin/env python3
"""Host-only EV6 profile and toolbox regressions; never connects to a car.

Requires Python 3 and Bash (Git for Windows is supported). All hook destinations
are replaced with temporary fixture paths; vehicle operations are mocked.
"""
from pathlib import Path
import os
import shutil
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
        cases = [
            ("wp_daudioplus_cvev_us", "CVEV.USA.0000.V015.012.260507", "19", ev6),
            ("wp_daudioplus_neev23_us", "original", "19", ioniq5_neev23_us),
            ("wp_daudioplus_neev23_us", "NEEV23.USA.0000.V015.011.260206", "19", ioniq5_neev23_us),
            ("wp_daudioplus_cvev_us", "other", "19", ioniq5_neev23_us),
            ("wp_daudioplus_cvev_us", "CVEV.USA.0000.V015.012.260507", "20", ioniq5_neev23_us),
            ("other", "CVEV.USA.0000.V015.012.260507", "19", ioniq5_neev23_us),
            ("wp_daudioplus_neev_us", "NEEV.USA.0000.V015.012.260507", "19", ioniq5_neev_us),
            ("wp_daudioplus_neev_us", "other", "19", ioniq5_neev23_us),
            ("wp_daudioplus_neev_us", "NEEV.USA.0000.V015.012.260507", "20", ioniq5_neev23_us),
            ("wp_daudioplus_cvev_us", "NEEV.USA.0000.V015.012.260507", "19", ioniq5_neev23_us),
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
                    self.assertEqual(result.stdout, expected + "|17|800|com.paytondev.launchpad/.MainActivity")

    def test_postinstall_staging_install_and_rollback(self):
        cases = ("fresh", "existing-equal", "replace-existing", "bad-stage",
                 "rollback-existing", "rollback-fresh")
        for case in cases:
            with self.subTest(case=case), tempfile.TemporaryDirectory(prefix="launchpad-hook-") as tmp:
                folder = Path(tmp)
                app = folder / "package/app"
                app.mkdir(parents=True)
                (folder / "system").mkdir()
                expected, previous = b"new APK bytes\x00\x01", b"previous APK bytes"
                (app / "Launchpad.apk").write_bytes(expected)
                system = folder / "system/Launchpad.apk"
                if case in ("replace-existing", "rollback-existing"):
                    system.write_bytes(previous)
                if case == "existing-equal":
                    system.write_bytes(expected)
                common = (ROOT / "tune-service/scripts/common.sh").read_text()
                common = common.replace("HERE=/data/nemesis/apps/paytondev.launchpad", "HERE=" + quote(folder / "package"))
                common = common.replace("SYSAPK=/system/app/Launchpad.apk", "SYSAPK=" + quote(system))
                common = common.replace("WORK=/data/local/tmp/launchpad", "WORK=" + quote(folder / "work"))
                common += '''
export PATH=/usr/bin:/bin
file_size() { wc -c < "$1"; }
system_free() { echo 104857600; }
system_rw() { echo rw >> "$TEST_FOLDER/remounts"; }
system_restore() { echo ro >> "$TEST_FOLDER/remounts"; }
stop_app() { :; }
chown() { :; }
chmod() { :; }
sync() { :; }
cmp() {
 case "$1" in -*) echo 'toolbox cmp rejects options' >&2; return 2 ;; esac
 if [ "$TEST_CASE" = bad-stage ] && [ "$2" = "$WORK/Launchpad.apk" ]; then return 1; fi
 case "$TEST_CASE" in rollback-*)
   if [ "$1" = "$WORK/Launchpad.apk" ] && [ "$2" = "$SYSAPK" ]; then return 1; fi ;;
 esac
 command cmp "$@"
}
'''
                (folder / "common.sh").write_text(common, newline="\n")
                hook = (ROOT / "tune-service/scripts/postinstall.sh").read_text()
                hook = hook.replace(". /data/nemesis/apps/paytondev.launchpad/scripts/common.sh",
                                    ". " + quote(folder / "common.sh"))
                script = folder / "postinstall.sh"
                script.write_text(hook, newline="\n")
                result = run(script, TEST_CASE=case, TEST_FOLDER=shell_path(folder))
                success = case in ("fresh", "existing-equal", "replace-existing")
                self.assertEqual(result.returncode == 0, success, result.stdout + result.stderr)
                if success:
                    self.assertEqual(system.read_bytes(), expected)
                elif case == "rollback-existing":
                    self.assertEqual(system.read_bytes(), previous)
                else:
                    self.assertFalse(system.exists())
                remounts = folder / "remounts"
                if case in ("existing-equal", "bad-stage"):
                    self.assertFalse(remounts.exists())
                else:
                    self.assertEqual(remounts.read_text().splitlines(), ["rw", "ro"])

    def test_usb_verification_rejects_different_or_missing_apk(self):
        with tempfile.TemporaryDirectory(prefix="launchpad-verify-") as tmp:
            folder = Path(tmp)
            expected, system = folder / "expected.apk", folder / "system.apk"
            expected.write_bytes(b"expected")
            # Source the real helper and real USB verification function. Stop
            # before the USB library's top-level initialization/log redirection.
            common = (ROOT / "tune-service/scripts/common.sh").read_text()
            library = (ROOT / "usb/launchpad/lib.sh").read_text()
            start = library.index("verify_installed() {")
            end = library.index("\n}", start) + 2
            script = folder / "verify.sh"
            script.write_text(common + '\nexport PATH=/usr/bin:/bin\n' + library[start:end] + '''
say() { :; }
pkg_installed() { return 0; }
knobd_state() { echo running; }
sleep() { :; }
cmp() { case "$1" in -*) return 2 ;; esac; command cmp "$@"; }
''' + "\nDEX=" + quote(expected) + "\nSYSAPK=" + quote(system) + "\nverify_installed\n", newline="\n")
            for value in (b"expected", b"different", None):
                with self.subTest(value=value):
                    if value is None:
                        system.unlink()
                    else:
                        system.write_bytes(value)
                    result = run(script)
                    self.assertEqual(result.returncode == 0, value == b"expected", result.stderr)

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


if __name__ == "__main__":
    unittest.main(verbosity=2)
