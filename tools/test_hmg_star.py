#!/usr/bin/env python3
"""Exercise star integration lifecycle with isolated, fully mocked host fixtures."""
from pathlib import Path
import os
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]

class HmgStarScriptTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="hmg-star-")
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.bin = self.root / "bin"
        self.bin.mkdir()
        self.here = self.root / "package"
        (self.here / "star/mod/META-HMGAPI").mkdir(parents=True)
        (self.here / "star/mod/META-HMGAPI/manifest.json").write_text("ours")
        (self.here / "star/mod/mod.json").write_text("correct-target")
        (self.here / "star/data").mkdir(parents=True)
        self.mod = self.root / "hmg/mods/precondition-star"
        self.engine = self.root / "hmgapi-hook.jar"
        self.dex = self.root / "Launchpad.apk"
        self.engine.touch(); self.dex.touch()
        self.hook = self.root / "hook"
        self.hook.mkdir(); (self.hook / "enable").touch()
        self.common = self.root / "common.sh"
        self.common.write_text(f'''HERE="{self.here}"
DEX="{self.dex}"
android_up() {{ [ "${{TEST_BOOT:-1}}" = 1 ]; }}
load_boot_env() {{ :; }}
files_equal() {{ cmp "$1" "$2" >/dev/null 2>&1; }}
log() {{ echo "$*"; }}
''')
        stubs = {
            "getprop": '''#!/bin/sh
case "$1" in ro.build.product) echo "${TEST_PRODUCT:-NEEV.KOR.0000.V039.001.260902}";;
ro.board.platform) echo tcc803x;; ro.build.version.sdk) echo 19;; esac
''',
            "hmgapid": '''#!/bin/sh
if [ "$1" = version ]; then echo "${TEST_HMG:-hmgapid 1.0.2}"; exit 0; fi
printf '%s\\n' "$*" >> "$TEST_LOG"
exit "${TEST_ENGINE_FAIL:-0}"
''',
            "app_process": '''#!/bin/sh
case "$*" in
 *HmgStarCheck*ready*) exit "${TEST_READY_FAIL:-0}";;
 *HmgStarCheck*unused*) exit "${TEST_SELECTED:-0}";;
 *Signing*verify*) exit "${TEST_SIG_FAIL:-0}";;
 *) exit 99;;
esac
'''}
        for name, data in stubs.items():
            f = self.bin / name; f.write_text(data); f.chmod(0o755)
        text = (ROOT / "tune-service/scripts/hmg-star.sh").read_text()
        replacements = {
            ". /data/nemesis/apps/paytondev.launchpad/scripts/common.sh": f'. "{self.common}"',
            "export PATH=/sbin:/vendor/bin:/system/sbin:/system/bin:/system/xbin:/data/nemesis/bin": f'export PATH="{self.bin}:$PATH"',
            "MOD=/data/hmgapi/mods/$ID": f'MOD="{self.mod}"',
            "ENGINE=/system/framework/hmgapi-hook.jar": f'ENGINE="{self.engine}"',
            "PROCESS=/system/bin/app_process.hmgapi-stock": f'PROCESS="{self.bin / "app_process"}"',
            "/data/hmgapi/hook/enable": str(self.hook / "enable"),
            "/data/hmgapi/hook/disable": str(self.hook / "disable"),
            "/data/hmgapi/state/journal.json": str(self.root / "journal.json"),
        }
        for old, new in replacements.items():
            self.assertIn(old, text); text = text.replace(old, new)
        self.script = self.root / "star.sh"; self.script.write_text(text)
        self.log = self.root / "log"

    def call(self, action, **env):
        return subprocess.run(["sh", str(self.script), action], text=True, capture_output=True,
                              env={**os.environ, "TEST_LOG": str(self.log), **env})
    def events(self):
        return self.log.read_text().splitlines() if self.log.exists() else []
    def install_existing(self, data="ours", disabled=False):
        (self.mod / "META-HMGAPI").mkdir(parents=True)
        (self.mod / "META-HMGAPI/manifest.json").write_text(data)
        (self.mod / "mod.json").write_text("correct-target" if data == "ours" else "other-target")
        if disabled: (self.mod / ".disabled").touch()
    def test_first_install_calls_only_managed_hmg_installer(self):
        r = self.call("install"); self.assertEqual(0, r.returncode, r.stdout + r.stderr)
        self.assertEqual(len(self.events()), 1)
        self.assertTrue(self.events()[0].startswith("engine install "))
        self.assertNotIn("--force", self.events()[0])
        self.assertNotIn("verify-map", self.events()[0])
    def test_unsupported_hmg_or_unchecked_map_never_calls_engine(self):
        for env in ({"TEST_PRODUCT": "OTHER"}, {"TEST_BOOT": "0"},
                    {"TEST_HMG": "hmgapid 1.0.3"}, {"TEST_READY_FAIL": "1"}, {"TEST_SIG_FAIL": "1"}):
            with self.subTest(env=env):
                self.assertNotEqual(0, self.call("install", **env).returncode)
                self.assertEqual([], self.events())
    def test_existing_mod_is_neither_overwritten_nor_reenabled(self):
        self.install_existing(); self.assertEqual(0, self.call("install").returncode); self.assertEqual([], self.events())
        (self.mod / ".disabled").touch()
        self.assertNotEqual(0, self.call("install").returncode); self.assertEqual([], self.events())
    def test_different_existing_manifest_fails_closed(self):
        self.install_existing("different")
        self.assertNotEqual(0, self.call("install").returncode); self.assertEqual([], self.events())
        self.assertEqual(0, self.call("remove").returncode); self.assertEqual([], self.events())
    def test_removal_requires_unselected_button_and_verified_owner(self):
        self.install_existing()
        self.assertNotEqual(0, self.call("remove", TEST_SELECTED="1").returncode)
        self.assertNotEqual(0, self.call("remove", TEST_BOOT="0").returncode)
        self.assertEqual([], self.events())
        self.assertEqual(0, self.call("remove").returncode)
        self.assertEqual([], self.events())
    def test_no_mod_or_booted_down_during_removal(self):
        self.assertEqual(0, self.call("remove").returncode)
        self.install_existing()
        self.assertNotEqual(0, self.call("remove", TEST_BOOT="0").returncode)
        self.assertEqual([], self.events())

if __name__ == "__main__":
    unittest.main()
