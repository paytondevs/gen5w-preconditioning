#!/usr/bin/env python3
"""Summarize a vehicle porting kit report against the profiles already in this repo.

Usage: python3 .claude/skills/vehicle-port/scripts/summarize_report.py REPORT [--repo PATH]

Reads launchpad-port-report.txt and app/src/main/java/com/paytondev/launchpad/
PreconditioningProfile.java. Prints the car's identity, which existing profile
(if any) its firmware hashes match, whether the Binder transactions Launchpad
uses (including the cluster battery-temperature read) still have the same codes,
and the TUNE capture. Read-only; never edits.
"""
import argparse
import re
from pathlib import Path

# Transactions OemVehicleClient sends or reads, by AIDL method name.
EXPECTED = {
    "com.hkmc.hev.IHEVService": {
        "getHEVStatus": 3,           # getter with IDs 94 (enabled), 44 (ready), 115 (failure)
        "setBattCondEvStation": 62,  # start (2,3) / cancel (0,0)
        "setBattCondRouteDis": 65,   # metadata 9 km / cancel 2047
        "setBattCondRemainTime": 66, # metadata 18 min / cancel 1023
    },
    "com.hkmc.bcm.IBCMService": {
        "getBattCondIndSta": 109,    # conditioning indicator: 0 Off, 1 Active
    },
}
SERVICES = {"com.hkmc.micom.hev": "com.hkmc.hev.IHEVService",
            "com.hkmc.micom.bcm": "com.hkmc.bcm.IBCMService"}
# ClusterBatteryTemperatureReader runs on every profile; there is no per-car switch.
# An absent service or other descriptor just means no reading. Moved codes don't.
CLUSTER_SERVICE, CLUSTER = "com.hkmc.micom.cluster", "com.hkmc.cluster.IClusterService"
CLUSTER_EXPECTED = {
    "addNavigationClusterListener": 37,
    "requestBatteryInfoReq": 109,
    "removeNavigationClusterListener": 38,
}
FRAMEWORK, SERVICE = "/system/framework/automotive.odex", "/system/framework/automotive-service.odex"


def sections(text):
    """[name] -> list of lines. getprop lines ("[ro.x]: [y]") stay inside [identity]."""
    out, name = {}, None
    for line in text.splitlines():
        m = re.fullmatch(r"\[([a-z_]+)\]", line)
        if m:
            name = m.group(1)
            out.setdefault(name, [])
        elif name:
            out[name].append(line)
    return out


def keyvalues(lines):
    return dict(l.split("=", 1) for l in lines if "=" in l and not l.startswith(("[", " ")))


def interfaces(lines):
    tables, current = {}, None
    for line in lines:
        if line.startswith("interface="):
            current = line[len("interface="):]
            tables[current] = {"stub": None, "transactions": {}}
        elif current and line.startswith("stub="):
            tables[current]["stub"] = line[len("stub="):]
        elif current and line.startswith("transaction "):
            _, code, method = line.split(" ", 2)
            tables[current]["transactions"][method] = int(code)
    return tables


PROFILE_JAVA = "app/src/main/java/com/paytondev/launchpad/PreconditioningProfile.java"
SUFFIXES = {"_DEVICE": "device", "_SOFTWARE": "software",
            "_FRAMEWORK_SHA256": "framework", "_SERVICE_SHA256": "service"}
# One exact-match check in matchesFirmware(). The hash constants can belong to another
# profile: NEEV US checks its own software against the NEEV23 US hashes.
BRANCH = re.compile(r"(\w+)_SOFTWARE\.equals\(software\)"
                    r"\s*&&\s*(\w+)_FRAMEWORK_SHA256\.equals\(frameworkHash\)"
                    r"\s*&&\s*(\w+)_SERVICE_SHA256\.equals\(serviceHash\)")


def braces(code, start):
    """(open, end) of the {...} block whose "{" is the first one at or after start."""
    open_at, depth = code.index("{", start), 0
    for i in range(open_at, len(code)):
        depth += {"{": 1, "}": -1}.get(code[i], 0)
        if depth == 0:
            return open_at, i + 1
    raise ValueError("unbalanced braces in " + PROFILE_JAVA)


def method_body(code, name):
    match = re.search(r"static \w+ " + name + r"\([^)]*\)\s*\{", code)
    if not match:
        raise ValueError(f"{name}() not found in {PROFILE_JAVA}")
    open_at, end = braces(code, match.end() - 1)
    return code[open_at + 1:end - 1]


def device_regions(code):
    """(device prefix, code) for each "if (<X>_DEVICE.equals(device)) {...}" block in
    matchesFirmware(), then the code outside those blocks. That code only runs for the
    supportsDevice() device that has no block of its own (the EV6); None if that isn't
    exactly one device."""
    body = method_body(code, "matchesFirmware")
    regions, outside, last = [], [], 0
    for match in re.finditer(r"if\s*\(\s*(\w+)_DEVICE\.equals\(device\)\s*\)\s*\{", body):
        if match.start() < last:
            continue  # nested in the previous block
        open_at, end = braces(body, match.end() - 1)
        regions.append((match.group(1), body[open_at:end]))
        outside.append(body[last:match.start()])
        last = end
    outside.append(body[last:])
    blocked = {device for device, _ in regions}
    supported = re.findall(r"(\w+)_DEVICE\.equals\(device\)", method_body(code, "supportsDevice"))
    rest = [device for device in dict.fromkeys(supported) if device not in blocked]
    regions.append((rest[0] if len(rest) == 1 else None, "".join(outside)))
    return regions


def repo_profiles(repo):
    """Profile name -> device, software and the framework/service hashes it requires.

    Every <NAME> with a _DEVICE, _SOFTWARE, _FRAMEWORK_SHA256 or _SERVICE_SHA256
    constant is a profile. A sub-profile (another software version for an existing
    device, like IONIQ5_NEEV_US_240822) has no _DEVICE, and a profile may reuse another
    profile's hashes, so device and hashes come from the matchesFirmware() branch that
    checks <NAME>_SOFTWARE: the device of the block it sits in, and the hash constants
    it compares. Without such a branch, the profile's own constants are all there is.
    """
    source = (repo / PROFILE_JAVA).read_text(encoding="utf-8")
    constants = dict(re.findall(r'static final String (\w+) = "([^"]*)"', source))

    def own(prefix):
        return {key: constants.get(prefix + suffix) for suffix, key in SUFFIXES.items()}

    profiles = {}
    for name in constants:
        for suffix in SUFFIXES:
            if name.endswith(suffix):
                profiles.setdefault(name[:-len(suffix)], own(name[:-len(suffix)]))
    code = re.sub(r"//[^\n]*|/\*.*?\*/", "", source, flags=re.S)  # ignore commented-out code
    for device, region in device_regions(code):
        for software, framework, service in BRANCH.findall(region):
            profile = profiles.setdefault(software, own(software))
            if device:
                profile["device"] = constants.get(device + "_DEVICE")
            profile["framework"] = constants.get(framework + "_FRAMEWORK_SHA256")
            profile["service"] = constants.get(service + "_SERVICE_SHA256")
    return profiles


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("report", type=Path)
    parser.add_argument("--repo", type=Path, default=Path(__file__).resolve().parents[4])
    args = parser.parse_args()
    s = sections(args.report.read_text(encoding="utf-8", errors="replace"))
    ident, existing = keyvalues(s.get("identity", [])), keyvalues(s.get("existing_profile", []))
    hashes = {}
    for line in s.get("firmware", []):
        m = re.match(r"file=(\S+) size=(\d+) sha256=([0-9a-f]*)", line)
        if m:
            hashes[m.group(1)] = m.group(3)

    print("== report")
    print("kit_version:", keyvalues(s.get("kit", [])).get("kit_version", "?"))
    print("complete:", "yes" if "probe_complete=yes" in s.get("end", []) else
          "NO - the Java probe did not finish; check launchpad-collect.log")
    for key in ("sdk", "device", "software"):
        print(f"{key}:", ident.get(key, existing.get(key, "?")))
    print("framework sha256:", hashes.get(FRAMEWORK, "(missing)"))
    print("service sha256:  ", hashes.get(SERVICE, "(missing)"))
    print("existing profile match (on car):", existing.get("matches_existing_profile", "?"),
          "| supported device:", existing.get("supported_device", "?"))

    print("\n== against this repo's profiles")
    profiles = repo_profiles(args.repo)
    same_hashes = [n for n, p in profiles.items()
                   if p["framework"] == hashes.get(FRAMEWORK) and p["service"] == hashes.get(SERVICE)]
    same_device = [n for n, p in profiles.items() if p["device"] == ident.get("device")]
    print("profiles with identical firmware hashes:", ", ".join(same_hashes) or "none")
    print("profiles for the same device:", ", ".join(same_device) or "none")
    for name in same_device:
        expected = profiles[name]["software"]
        if expected:
            print(f"  {name} requires software {expected}:",
                  "same" if expected == ident.get("software") else "DIFFERENT")
        else:
            print(f"  {name} does not check the software version")

    print("\n== services and transactions")
    descriptors = {}
    for line in s.get("service_descriptors", []):
        m = re.match(r"service=(\S+) descriptor=(.*)", line)
        if m:
            descriptors[m.group(1)] = m.group(2)
    for service, descriptor in SERVICES.items():
        found = descriptors.get(service)
        print(f"{service}:", "ok" if found == descriptor else f"MISMATCH ({found or 'absent'})")
    tables = interfaces(s.get("interfaces", []))
    moved = 0
    for descriptor, methods in EXPECTED.items():
        table = tables.get(descriptor)
        if not table or not table["stub"] or table["stub"] == "not found":
            print(f"{descriptor}: stub not found - transaction codes unknown")
            moved += 1
            continue
        for method, code in methods.items():
            actual = table["transactions"].get(method)
            status = "same" if actual == code else f"CHANGED (now {actual})" if actual else "MISSING"
            moved += status != "same"
            print(f"  {code:>3} {method}: {status}")
    print("transactions Launchpad uses:", "all unchanged" if moved == 0 else f"{moved} need attention")

    print("\n== battery temperature")
    found = descriptors.get(CLUSTER_SERVICE)
    if found != CLUSTER:
        print(f"{CLUSTER_SERVICE}:", f"MISMATCH ({found})" if found else "absent")
        print("battery temperature: no reading on this car")
    else:
        print(f"{CLUSTER_SERVICE}: ok")
        table = tables.get(CLUSTER)
        moved = 0
        if not table or not table["stub"] or table["stub"] == "not found":
            print(f"{CLUSTER}: stub not found - transaction codes unknown")
            moved += 1
        else:
            for method, code in CLUSTER_EXPECTED.items():
                actual = table["transactions"].get(method)
                status = "same" if actual == code else f"CHANGED (now {actual})" if actual else "MISSING"
                moved += status != "same"
                print(f"  {code:>3} {method}: {status}")
        print("battery temperature:", "contract unchanged" if moved == 0 else f"{moved} need attention")

    print("\n== TUNE capture")
    for line in s.get("key_capture", []):
        if line.startswith(("key ", "likely_knob")):
            print(line)


if __name__ == "__main__":
    main()
