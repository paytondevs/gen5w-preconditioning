#!/usr/bin/env python3
"""Run the vehicle-port report summarizer against PreconditioningProfile.java.

The reports are synthetic: each copies only the identity and firmware hashes of a
profile already in the repo, in the porting kit's [identity] and [firmware] format.
No owner's report is used, and nothing connects to a car.
"""
import importlib.util
from pathlib import Path
import re
import subprocess
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / ".claude/skills/vehicle-port/scripts/summarize_report.py"
spec = importlib.util.spec_from_file_location("summarize_report", SCRIPT)
summarize_report = importlib.util.module_from_spec(spec)
spec.loader.exec_module(summarize_report)


def report(device, software, framework, service):
    # existing_profile=no: the report came from a build without this profile, as
    # issue #44's did.
    return f"""# Launchpad vehicle porting kit report
# Synthetic test fixture, not from a car.

[kit]
kit_version=test
date=2026-01-01 00:00:00

[identity]
sdk=19
device={device}
software={software}
[ro.build.product]: [{software}]
[ro.build.version.sdk]: [19]
[ro.product.device]: [{device}]

[existing_profile]
sdk=19
device={device}
software={software}
supported_device=yes
matches_existing_profile=no

[firmware]
file=/system/framework/automotive-service.jar size=314 sha256={"0" * 64}
file=/system/framework/automotive-service.odex size=2718281 sha256={service}
file=/system/framework/automotive.jar size=271 sha256={"1" * 64}
file=/system/framework/automotive.odex size=3141592 sha256={framework}

[end]
probe_complete=yes
"""


class SummarizeReportTest(unittest.TestCase):
    def summarize(self, *identity):
        with tempfile.TemporaryDirectory(prefix="launchpad-report-") as temp:
            path = Path(temp) / "launchpad-port-report.txt"
            path.write_text(report(*identity), encoding="utf-8")
            result = subprocess.run([sys.executable, "-I", str(SCRIPT), str(path), "--repo", str(ROOT)],
                                    capture_output=True, text=True, encoding="utf-8", timeout=30)
        self.assertEqual(result.returncode, 0, result.stderr)
        return result.stdout

    def listed(self, output, label):
        line = re.search(r"^" + label + r": (.*)$", output, re.M).group(1)
        return [] if line == "none" else line.split(", ")

    def test_base_profile(self):
        output = self.summarize("wp_daudioplus_cvev_us", "CVEV.USA.0000.V015.012.260507",
                                "b9e7e02b81e1a8cfcf767c6b8bbb28bcf1d2c411a208fbf6a465e7f0c662774a",
                                "8af745b41aa28f490953d61531215cd94e059f104b31f6236a8f6c7af23b8062")
        self.assertEqual(self.listed(output, "profiles with identical firmware hashes"), ["EV6"])
        self.assertEqual(self.listed(output, "profiles for the same device"), ["EV6", "EV6_251120"])
        self.assertIn("EV6 requires software CVEV.USA.0000.V015.012.260507: same", output)
        self.assertIn("EV6_251120 requires software CVEV.USA.0000.V014.011.251120: DIFFERENT", output)

    def test_same_device_sub_profile(self):
        # Issue #44: this car is already supported by a sub-profile with no _DEVICE.
        output = self.summarize("wp_daudioplus_neev_us", "NEEV.USA.0000.V012.010.240822",
                                "0f6786b9c8c167f09c08c559afea7da1e2137e2a1968b9ab372def65762674f9",
                                "2758a413c184855bf92080cfdc6decbfcd6f09775128a982b1eee211993772a1")
        self.assertEqual(self.listed(output, "profiles with identical firmware hashes"), ["IONIQ5_NEEV_US_240822"])
        self.assertEqual(self.listed(output, "profiles for the same device"),
                         ["IONIQ5_NEEV_US", "IONIQ5_NEEV_US_240822", "IONIQ5_NEEV_US_260903"])
        self.assertIn("IONIQ5_NEEV_US_240822 requires software NEEV.USA.0000.V012.010.240822: same", output)
        self.assertIn("IONIQ5_NEEV_US requires software NEEV.USA.0000.V015.012.260507: DIFFERENT", output)

    def test_hash_reusing_profile(self):
        # NEEV US profiles check their own software against NEEV23 US hash constants.
        cases = [
            ("NEEV.USA.0000.V015.012.260507",
             "dd72e04bce0a2b4bda1506308ac21f91002c1436553b8156c345cc4313661338",
             "3cf40fb00854c63afa49abea1dad18a0afcbc0dd68b17de9ed7ea3f5aeab4222",
             ["IONIQ5_NEEV23_US", "IONIQ5_NEEV_US"], "IONIQ5_NEEV_US"),
            ("NEEV.USA.0000.V016.010.260903",
             "c52e0825956880d627b32979022705a618200f059476c094d729133d1ff6a3be",
             "bf17e682d1beb349ae8ef2acaae6d6e908fe42e88853d7145ee16b29bb4d9ec1",
             ["IONIQ5_NEEV23_US_260903", "IONIQ5_NEEV_US_260903"], "IONIQ5_NEEV_US_260903"),
        ]
        for software, framework, service, identical, profile in cases:
            with self.subTest(profile=profile):
                output = self.summarize("wp_daudioplus_neev_us", software, framework, service)
                self.assertEqual(self.listed(output, "profiles with identical firmware hashes"), identical)
                self.assertIn(f"{profile} requires software {software}: same", output)

    def test_lists_every_profile(self):
        source = (ROOT / summarize_report.PROFILE_JAVA).read_text(encoding="utf-8")
        names = set(re.findall(
            r"static final String (\w+?)_(?:DEVICE|SOFTWARE|FRAMEWORK_SHA256|SERVICE_SHA256) =", source))
        devices = set(re.findall(r'static final String \w+_DEVICE = "([^"]+)"', source))
        profiles = summarize_report.repo_profiles(ROOT)
        self.assertEqual(set(profiles), names)
        for name, profile in profiles.items():
            with self.subTest(profile=name):
                self.assertIn(profile["device"], devices)
                self.assertRegex(profile["software"] or "", r"^\S+$")
                self.assertRegex(profile["framework"] or "", r"^[0-9a-f]{64}$")
                self.assertRegex(profile["service"] or "", r"^[0-9a-f]{64}$")
        # Sub-profiles get their device from the matchesFirmware() block they sit in,
        # or, for the EV6, from the code after the device blocks.
        self.assertEqual(profiles["IONIQ5_NEEV23_US_251120"]["device"], "wp_daudioplus_neev23_us")
        self.assertEqual(profiles["EV6_251120"]["device"], "wp_daudioplus_cvev_us")


if __name__ == "__main__":
    unittest.main()
