#!/usr/bin/env python3
"""Check generated USB contents in a temporary tree. No car or signing key used.

The synthetic APK has signature-shaped entries solely to exercise the packager;
it is not installable and does not verify cryptographic signing or Android use.
"""
import io
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest
import zipfile

ROOT = Path(__file__).resolve().parents[1]


class UsbBundleTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="launchpad-bundles-")
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        for folder in ("tools", "usb", "tune-service"):
            if folder == "tools":
                (self.root / folder).mkdir()
                shutil.copy2(ROOT / folder / "build_usb_bundles.py", self.root / folder)
                shutil.copytree(ROOT / folder / "usb", self.root / folder / "usb")
            else:
                shutil.copytree(ROOT / folder, self.root / folder)
        (self.root / "VERSION").write_text("4.5.6\n", encoding="utf-8")
        self.apk = self.root / "fixture.apk"

    def build(self, entries):
        with zipfile.ZipFile(self.apk, "w") as apk:
            for name, value in entries.items():
                apk.writestr(name, value)
        return subprocess.run(
            [sys.executable, str(self.root / "tools/build_usb_bundles.py"),
             "--apk", str(self.apk)], capture_output=True, text=True, timeout=30)

    def test_bundle_matches_descriptor_and_payload(self):
        # Exercise LF conversion as well as normal Windows source checkouts.
        source = self.root / "usb/launchpad/install.sh"
        source.write_bytes(source.read_bytes().replace(b"\r\n", b"\n").replace(b"\n", b"\r\n"))
        result = self.build({"classes.dex": b"fixture-not-dex", "META-INF/TEST.RSA": b"not-a-signature"})
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        package_bytes = (self.root / "dist/paytondev.launchpad.zip").read_bytes()
        with zipfile.ZipFile(io.BytesIO(package_bytes)) as package:
            self.assertEqual(package.read("app/Launchpad.apk"), self.apk.read_bytes())
            expected = {p.relative_to(self.root / "tune-service").as_posix()
                        for p in (self.root / "tune-service").rglob("*") if p.is_file()}
            self.assertEqual(set(package.namelist()), expected | {"app/Launchpad.apk"})
            for name in expected:
                self.assertNotIn(b"\r", package.read(name), name)
            descriptor = package.read("NEMESIS.data").decode()
            self.assertIn("\nversion=4.5.6\n", descriptor)
            self.assertNotIn("@VERSION@", descriptor)
        # One bundle installs or updates; uninstall is NEMESIS Manager's button.
        # The other is the read-only porting kit.
        self.assertEqual(sorted(p.name for p in (self.root / "dist").glob("Launchpad-*-usb.zip")),
                         ["Launchpad-install-usb.zip", "Launchpad-port-kit-usb.zip"])
        with zipfile.ZipFile(self.root / "dist/Launchpad-install-usb.zip") as bundle:
            expected = {"README.txt", "NEMESIS.data", "launchpad/common.sh",
                        "launchpad/lib.sh", "launchpad/install.sh",
                        "packages/paytondev.launchpad.zip"}
            self.assertEqual(set(bundle.namelist()), expected)
            self.assertEqual(bundle.read("packages/paytondev.launchpad.zip"), package_bytes)
            self.assertEqual(bundle.read("NEMESIS.data").decode(),
                             "[nemesis]\nversion=1.0\ntype=execute\npath=launchpad/install.sh\n")
            readme = bundle.read("README.txt").decode("utf-8")
            self.assertIn("Launchpad - install/update USB", readme)
            self.assertIn("launchpad-install.log", readme)
            for name in expected - {"packages/paytondev.launchpad.zip"}:
                self.assertNotIn(b"\r", bundle.read(name), name)

    def test_port_kit_reads_only_and_carries_the_same_apk(self):
        source = self.root / "usb/port-kit/collect.sh"
        source.write_bytes(source.read_bytes().replace(b"\r\n", b"\n").replace(b"\n", b"\r\n"))
        result = self.build({"classes.dex": b"fixture-not-dex", "META-INF/TEST.RSA": b"not-a-signature"})
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        with zipfile.ZipFile(self.root / "dist/Launchpad-port-kit-usb.zip") as kit:
            expected = {"README.txt", "NEMESIS.data", "launchpad-port/collect.sh",
                        "launchpad-port/common.sh", "launchpad-port/lib.sh",
                        "launchpad-port/Launchpad.apk"}
            # No packages/ folder: the kit has nothing NEMESIS could install.
            self.assertEqual(set(kit.namelist()), expected)
            self.assertEqual(kit.read("launchpad-port/Launchpad.apk"), self.apk.read_bytes())
            self.assertEqual(kit.read("NEMESIS.data").decode(),
                             "[nemesis]\nversion=1.0\ntype=execute\npath=launchpad-port/collect.sh\n")
            collect = kit.read("launchpad-port/collect.sh").decode()
            self.assertIn("\nKIT_VERSION=4.5.6\n", collect)
            self.assertNotIn("@VERSION@", collect)
            for word in ('"$NEMESIS" install', "remount rw", "system_rw", "/system/app"):
                self.assertNotIn(word, collect)
            self.assertEqual(kit.read("launchpad-port/common.sh"),
                             (self.root / "tune-service/scripts/common.sh").read_bytes().replace(b"\r\n", b"\n"))
            readme = kit.read("README.txt").decode("utf-8")
            self.assertIn("vehicle porting kit", readme)
            self.assertIn("launchpad-port-report.txt", readme)
            self.assertIn("template=vehicle-port.yml", readme)
            for name in expected - {"launchpad-port/Launchpad.apk"}:
                self.assertNotIn(b"\r", kit.read(name), name)

    def test_reject_port_kit_without_version_placeholder(self):
        source = self.root / "usb/port-kit/collect.sh"
        source.write_text(source.read_text().replace("KIT_VERSION=@VERSION@", "KIT_VERSION=1"), newline="\n")
        result = self.build({"classes.dex": b"fixture-not-dex", "META-INF/TEST.RSA": b"not-a-signature"})
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("KIT_VERSION=@VERSION@", result.stderr)

    def test_reject_unsigned_input_before_creating_bundles(self):
        result = self.build({"classes.dex": b"fixture-not-dex"})
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("is not signed", result.stderr)
        self.assertFalse((self.root / "dist").exists())

    def test_reject_malformed_version(self):
        (self.root / "VERSION").write_text("1.3\n", encoding="utf-8")
        result = self.build({"classes.dex": b"fixture-not-dex", "META-INF/TEST.RSA": b"not-a-signature"})
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("VERSION must be major.minor.patch", result.stderr)
        self.assertFalse((self.root / "dist").exists())

    def test_reject_input_without_dex(self):
        result = self.build({"META-INF/TEST.RSA": b"not-a-signature"})
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("has no classes.dex", result.stderr)
        self.assertFalse((self.root / "dist").exists())

    def check_writer(self, command):
        result = self.build({"classes.dex": b"fixture-not-dex", "META-INF/TEST.RSA": b"not-a-signature"})
        self.assertEqual(result.returncode, 0, result.stderr)
        drive = self.root / "fixture-drive"
        drive.mkdir()
        for name in ("README.txt", "ota_package.tar", "NEMESIS.data"):
            (drive / name).write_text("old " + name, encoding="utf-8")
        (drive / "personal.txt").write_text("keep", encoding="utf-8")
        result = subprocess.run(command(drive), capture_output=True, text=True, timeout=30)
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        backups = list(drive.glob("NEMESIS-backup-*"))
        self.assertEqual(len(backups), 1)
        for name in ("README.txt", "ota_package.tar", "NEMESIS.data"):
            self.assertEqual((backups[0] / name).read_text(encoding="utf-8"), "old " + name)
        self.assertFalse((drive / "ota_package.tar").exists())
        self.assertEqual((drive / "personal.txt").read_text(encoding="utf-8"), "keep")
        with zipfile.ZipFile(self.root / "dist/Launchpad-install-usb.zip") as bundle:
            for name in bundle.namelist():
                self.assertEqual((drive / name).read_bytes(), bundle.read(name), name)

    def test_powershell_writer_preserves_old_readme_and_ota(self):
        powershell = shutil.which("powershell") or shutil.which("pwsh")
        if not powershell:
            self.skipTest("PowerShell is not installed")
        self.check_writer(lambda drive: [powershell, "-NoProfile", "-ExecutionPolicy", "Bypass",
            "-File", str(self.root / "tools/usb/write-usb.ps1"),
            "-Drive", str(drive), "-BundleDir", str(self.root / "dist")])

    def test_shell_writer_preserves_old_readme_and_ota(self):
        bash = shutil.which("bash")
        if os.name == "nt":
            git_bash = Path(os.environ.get("ProgramFiles", "C:/Program Files")) / "Git/bin/bash.exe"
            bash = str(git_bash) if git_bash.is_file() else None
        if not bash:
            self.skipTest("Bash is not installed")
        def shell_path(path):
            value = path.resolve().as_posix()
            return "/" + value[0].lower() + value[2:] if os.name == "nt" else value
        self.check_writer(lambda drive: [bash, shell_path(self.root / "tools/usb/write-usb.sh"),
            shell_path(drive), shell_path(self.root / "dist")])


class CollectScriptTest(unittest.TestCase):
    """Run the read-only collect script against fixture framework files and fake tools."""
    def setUp(self):
        self.bash = shutil.which("bash")
        if os.name == "nt" or not self.bash:
            self.skipTest("Bash with POSIX paths is required")
        self.temp = tempfile.TemporaryDirectory(prefix="launchpad-collect-")
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.drive = self.root / "drive"
        shutil.copytree(ROOT / "usb/collect", self.drive)
        self.framework = self.root / "framework"
        self.framework.mkdir()
        (self.framework / "automotive.odex").write_bytes(b"dey\n036\0automotive")
        (self.framework / "automotive-service.odex").write_bytes(b"dey\n036\0service")
        (self.framework / "framework.jar").write_bytes(b"jar")
        (self.framework / "notes.txt").write_text("skip")
        (self.framework / "subdir").mkdir()
        self.bin = self.root / "bin"
        self.bin.mkdir()
        for name, body in {"getprop": "echo '[ro.product.device]: [fixture]'",
                           "service": "echo '47\tcom.hkmc.micom.cluster: [com.hkmc.cluster.IClusterService]'"}.items():
            tool = self.bin / name
            tool.write_text("#!/bin/sh\n" + body + "\n")
            tool.chmod(0o755)

    def run_collect(self):
        return subprocess.run([self.bash, str(self.drive / "launchpad-collect/collect.sh")],
            capture_output=True, text=True, timeout=30, cwd=self.drive,
            env=dict(os.environ, COLLECT_PATH=f"{self.bin}:/usr/bin:/bin",
                     COLLECT_FRAMEWORK=str(self.framework), BOOTCLASSPATH="/system/framework/core.jar"))

    def test_descriptor_runs_the_collect_script(self):
        descriptor = (self.drive / "NEMESIS.data").read_text()
        self.assertIn("type=execute", descriptor)
        self.assertIn("path=launchpad-collect/collect.sh", descriptor)

    def test_copies_framework_files_and_identifiers_only_to_the_drive(self):
        before = {p: p.read_bytes() for p in self.framework.iterdir() if p.is_file()}
        result = self.run_collect()
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        outputs = list(self.drive.glob("launchpad-collect-*"))
        self.assertEqual(len(outputs), 1)
        out = outputs[0]
        self.assertEqual(sorted(p.name for p in (out / "framework").iterdir()),
                         ["automotive-service.odex", "automotive.odex", "framework.jar"])
        for name in ("automotive.odex", "automotive-service.odex", "framework.jar"):
            self.assertEqual((out / "framework" / name).read_bytes(), before[self.framework / name])
        self.assertIn("ro.product.device", (out / "props.txt").read_text())
        self.assertIn("IClusterService", (out / "services.txt").read_text())
        self.assertIn("core.jar", (out / "bootclasspath.txt").read_text())
        self.assertIn("SUCCESS", (self.drive / "launchpad-collect.log").read_text())
        # The source tree is untouched.
        self.assertEqual({p: p.read_bytes() for p in self.framework.iterdir() if p.is_file()}, before)

    def test_missing_automotive_odex_fails(self):
        (self.framework / "automotive-service.odex").unlink()
        result = self.run_collect()
        self.assertEqual(result.returncode, 1, result.stdout + result.stderr)
        self.assertIn("automotive-service.odex not collected", (self.drive / "launchpad-collect.log").read_text())


if __name__ == "__main__":
    unittest.main(verbosity=2)
