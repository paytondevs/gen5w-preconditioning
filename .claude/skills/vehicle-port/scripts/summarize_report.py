#!/usr/bin/env python3
"""Summarize a vehicle porting kit report against the profiles already in this repo.

Usage: python3 .claude/skills/vehicle-port/scripts/summarize_report.py REPORT [--repo PATH]

Reads launchpad-port-report.txt and app/src/main/java/com/paytondev/launchpad/
PreconditioningProfile.java. Prints the car's identity, which existing profile
(if any) its firmware hashes match, whether the Binder transactions Launchpad
uses still have the same codes, and the TUNE capture. Read-only; never edits.
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


def repo_profiles(repo):
    source = (repo / "app/src/main/java/com/paytondev/launchpad/PreconditioningProfile.java").read_text()
    constants = dict(re.findall(r'static final String (\w+) = "([^"]*)"', source))
    profiles = {}
    for name, value in constants.items():
        if name.endswith("_DEVICE"):
            prefix = name[:-len("_DEVICE")]
            profiles[prefix] = {"device": value,
                                "software": constants.get(prefix + "_SOFTWARE"),
                                "framework": constants.get(prefix + "_FRAMEWORK_SHA256"),
                                "service": constants.get(prefix + "_SERVICE_SHA256")}
    # Profiles may reuse another profile's hashes (NEEV US reuses NEEV23 US).
    for prefix, p in profiles.items():
        for other, q in profiles.items():
            if p["framework"] is None and q["framework"] and re.search(
                    prefix + r"_DEVICE\.equals\(device\)\)\s*\{[^}]*" + other + "_FRAMEWORK_SHA256", source):
                p["framework"], p["service"] = q["framework"], q["service"]
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

    print("\n== TUNE capture")
    for line in s.get("key_capture", []):
        if line.startswith(("key ", "likely_knob")):
            print(line)


if __name__ == "__main__":
    main()
