#!/usr/bin/env python3
"""Tests for contributor language-pack validation."""
import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("validator", ROOT / "tools/check_language_pack.py")
validator = importlib.util.module_from_spec(spec)
spec.loader.exec_module(validator)

class PackValidationTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.file = Path(self.tmp.name) / "pack.json"
        self.base = json.loads((ROOT / "languages/en.json").read_text(encoding="utf-8"))
    def write(self, **changes):
        data = dict(self.base, locale="fr", name="Français", **changes)
        self.file.write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")
    def test_valid_partial_pack(self):
        self.write(strings={"menu.battery": "Batterie"})
        validator.main([str(self.file)])
    def test_bad_placeholder_rejected(self):
        self.write(strings={"warning.warm": "Warning %1$s"})
        with self.assertRaises(ValueError): validator.main([str(self.file)])
    def test_unknown_key_rejected(self):
        self.write(strings={"bad.key": "test"})
        with self.assertRaises(ValueError): validator.main([str(self.file)])
    def test_invalid_locale_rejected(self):
        self.write()
        data = json.loads(self.file.read_text()); data["locale"] = "../bad"
        self.file.write_text(json.dumps(data))
        with self.assertRaises(ValueError): validator.main([str(self.file)])
if __name__ == "__main__": unittest.main()
