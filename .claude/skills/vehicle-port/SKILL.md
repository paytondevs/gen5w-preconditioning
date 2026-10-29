---
name: vehicle-port
description: Turn a vehicle porting kit report (launchpad-port-report.txt) into a Launchpad vehicle port for this repo. Triage the report against the existing firmware profiles, then add the profile, TUNE knob entry, tests, compatibility chart row, VERSION bump and PR. Use this whenever someone shares a porting kit report or launchpad-collect.log, links a "Vehicle port request" issue, or asks to add support for a new IONIQ 5, EV6, IONIQ 6, GV60 or other Gen5W head unit or software version. Use it even when they only ask "is this enough to port?" or "does my car work?" with a report attached.
---

# Vehicle port from a porting kit report

A porting kit report is what an owner gets from `Launchpad-port-kit-usb.zip` (see
`usb/port-kit/` and `PortProbe.java`). It is collected read-only on the car and has
everything a port needs **except proof that Start and Stop work in that car**. Your
job is to work out what kind of port it is, make exactly the changes that kind
needs, and say clearly what is still unverified.

This project changes the infotainment system of real cars, and a wrong command
profile sends Binder transactions to code nobody has inspected. That is why
profiles match exact firmware, and why the rules at the end of this file exist.

## 1. Read the report

Run the summarizer first. It parses the report and compares it with the profiles
in `PreconditioningProfile.java`, so you don't have to diff hashes and transaction
tables by eye:

```sh
python3 .claude/skills/vehicle-port/scripts/summarize_report.py path/to/launchpad-port-report.txt
```

Then check these in the raw report yourself:

- **`complete: yes`.** If the Java probe didn't finish, the report has only the shell
  sections (identity, framework file listing, `service list`, input devices). Ask the
  owner for `launchpad-collect.log` and a re-run before going further.
- **The issue matches the report.** The vehicle and software version the owner typed
  into the issue should agree with `[identity]`. A mismatch usually means a report
  from another car or an old drive. Ask; don't guess.
- **Nothing personal is in it.** The kit filters serial, IMEI, MAC and VIN
  properties. Method names like `requestVin` in the transaction tables are fine.
  Anything else that looks personal should not be quoted in the PR.

Report sections, for reference:

| section | what it tells you |
| --- | --- |
| `[identity]` | `sdk`, `device` (`ro.product.device`), `software` (`ro.build.product`) and allowlisted build properties |
| `[existing_profile]` | whether the build that ran the kit already accepts this car |
| `[firmware]` | SHA-256 and size of each `/system/framework/automotive*` file |
| `[framework_files]` | `ls -l /system/framework` and `BOOTCLASSPATH` |
| `[services]`, `[service_descriptors]` | `service list`, and each service's interface descriptor |
| `[interfaces]` | per `com.hkmc.*` interface: its `$Stub`, every `transaction CODE method`, and interface constants |
| `[input]`, `[key_capture]` | input devices, key events in a 30 s window, `likely_knob` |
| `[nemesis]` | NEMESIS packages, including whether Launchpad is installed |

## 2. Decide what kind of port it is

Work down this list and stop at the first case that fits.

**A. Already supported.** `matches_existing_profile=yes`, or the summarizer finds an
existing profile for the same device with identical hashes and (where that profile
checks one) the same software. No code change is needed. At most, the
[compatibility chart](../../../COMPATIBILITY.md#compatibility-chart) gains a model
year or trim that this report newly confirms. Docs-only changes don't bump `VERSION`.
Tell the user the car should already work, and which profile covers it.

**B. Same firmware, new identity.** The hashes match an existing profile, but the
device or software string is new. This is how NEEV US 260507 was added: it reuses the
NEEV23 US hashes and command profile, adding only its own device and software
checks. The Binder interface is byte-identical code, so the existing command
profile and battery-temperature read apply. In-car Start/Stop is still required,
because identical framework files don't prove the MICOM/BMS behind them behaves the
same.

**C. Different firmware, same transactions.** The hashes are new, but the summarizer
reports `all unchanged` for the five transactions Launchpad uses, both HEV and BCM
descriptors are `ok`, and its battery temperature section says `contract unchanged`
or `no reading on this car`. This is how the EV6 was added: a new profile with its own
hashes, reusing the command sequence. The report can't show the `getHEVStatus` ID
arguments (94 enabled, 44 ready, 115 failure), because they are not interface
constants. They are unverified until the car reads sensible values. Say so.

**D. Transactions moved, missing, or a stub not found.** This includes a battery
temperature section that says `need attention`: every profile runs the temperature
read, so a new profile would send 37/109/38 to whatever those codes now mean. Stop and
report back. Don't remap numbers yourself. The literal codes live in
`PreconditioningProfile` (start, maintain, cancel), `OemVehicleClient` (getters and the
`write()` allowlist) and `ClusterBatteryTemperatureReader` (37/109/38). Moving
them for one profile means making the command profile per-profile, which is a design
change for the maintainer. Give the maintainer what they need to decide: the old and
new code of each method, from the report's `[interfaces]` table.

**E. Not a Gen5W API 19 head unit, or HEV/BCM services absent.** Out of scope. Explain
which check failed.

## 3. Make the changes (cases B and C)

Follow the existing profiles exactly. Read `PreconditioningProfile.java`,
`OemVehicleClient.java`, `tune-service/knob.conf` and the newest compatibility test
before editing, and mirror their naming and comment style.

1. **`PreconditioningProfile.java`.**
   - Add `<NAME>_DEVICE` and `<NAME>_SOFTWARE`. For case C, also add
     `<NAME>_FRAMEWORK_SHA256` and `<NAME>_SERVICE_SHA256`.
   - Add the device to `supportsDevice()`.
   - Add an exact-match branch to `matchesFirmware()` requiring the software and both
     hashes. In case B, reference the existing profile's hash constants and add a
     comment saying they are byte-identical, with the capture date.
   - Name constants as `<MODEL>_<HEADUNIT>_<MARKET>`, like `IONIQ5_NEEV_US` and
     `EV6`.
   - **Every new profile uses the Ioniq 5 two-minute pending timeout**
     (`IONIQ5_ACTIVATION_TIMEOUT_MS`), even when the car's activation time is
     unmeasured. Don't add the device to the EV6 branch of `activationTimeoutMs()`;
     the five minutes there is the EV6's own field-validated value, not a default for
     new cars. Add the new device to `eachProfileHasItsOwnActivationTimeout` in
     `PreconditioningControllerTest`, and say "two minutes" in the docs.
2. **`OemVehicleClient.connect()`.** It reads `ro.build.product` only for devices named
   in its `if`. Add the new device there, or `software` stays `""` and the new profile
   can never match. This is the easiest step to miss, and no unit test catches it.
3. **`tune-service/knob.conf`.** The default is `DEVICE_NAME=daudio_wp_daudioplus_neev23_us`,
   `KEY_CODE=17`, `HOLD_MS=800`.
   - If `likely_knob` names a different device, add an exact-identity `if` block like
     the EV6 and NEEV US ones (`ro.product.device`, `ro.build.product`,
     `ro.build.version.sdk` = 19).
   - If the key code also differs, set `KEY_CODE` inside that block only.
   - If the report shows `likely_knob=none`, leave TUNE out of the port and say so.
     TUNE only opens the screen; it never starts conditioning.
4. **Battery temperature needs no code.** Every profile reads the cluster minimum
   (`ClusterBatteryTemperatureReader`) once it passes the firmware check. Don't add a
   per-car switch, and don't skip the car's temperature in the docs or the in-car test.
   The battery warm notice (with **Start anyway**) comes with it: allow it on any car
   whose report shows `contract unchanged`. Don't hold it back waiting for an OBD
   comparison.
5. **Tests.**
   - Add `app/src/test/java/com/paytondev/launchpad/<Name>CompatibilityTest.java`,
     modelled on `Ioniq5NeevUsCompatibilityTest.java`: the exact identity matches; a
     wrong API, device, software or either hash does not; other profiles are
     unaffected.
   - If you changed `knob.conf`, add the new identity and its near-misses to
     `test_tune_selection_keeps_key_duration_and_target` in `tools/test_ev6_scripts.py`.
6. **`COMPATIBILITY.md`.**
   - Add a chart row (vehicle, model years, market, head unit with its device code,
     software version, preconditioning, battery temperature, status). Battery
     temperature is ✅ unless the summarizer said `no reading on this car`.
   - Under "technical details", add a `### <name> profile` section like the NEEV US
     one: device, software, both hashes (or that they match another profile), the
     service check, the TUNE input device, battery temperature, and what was and
     wasn't verified.
   - For battery temperature, give the summarizer's verdict and whether the owner has
     compared the reading with an OBD reader. Without a cluster service, say the car
     shows no temperature.
   - The chart is plain language for owners. Keep hashes and transaction detail in
     the technical section.
7. **`VERSION`.** Bump the minor version and reset patch (`1.4.0` → `1.5.0`), per
   CONTRIBUTING. Put the new number in the PR description.

## 4. Check it

Run the repo's checks from BUILDING.md. Gradle needs `ANDROID_HOME`, with platform 37
and build-tools 36.0.0.

```sh
./gradlew :app:testDebugUnitTest :app:lintRelease :app:assembleDebug :app:assembleRelease
python3 tools/test_ev6_scripts.py
python3 tools/test_temperature_binder.py
python3 tools/test_usb_bundles.py
```

Then re-read your diff for these problems:
- a check loosened for an existing profile
- a missed `OemVehicleClient` device
- a `knob.conf` block that isn't exact-identity
- the report file or raw logs staged for commit

## 5. The PR

Title it `Add <vehicle> <software> profile` or similar. The description needs what
CONTRIBUTING asks for:
- model, model year, trim and market
- head-unit device name and full software version
- which case this was (B or C) and why
- link to the port request issue
- what the report verified, and what it can't

**In-car verification is the open item.** CONTRIBUTING asks for a video of Start
reaching **Active**, Stop returning to **Off**, and TUNE opening Launchpad, if a TUNE
profile was added. A report is not that evidence. Unless the owner has already
provided it:
- open the PR as a draft
- say plainly that it is waiting on an in-car test
- list the steps for the owner in the PR description: install a build from this
  branch, press Start, wait for Active, press Stop, confirm Off
- ask the owner there to check that the Preconditioning tab shows a battery
  temperature and, if they have an OBD reader, that the two agree. The report can't show the listener's
  callback code or the reading's array layout, so only the car can confirm them.

Don't describe the car as supported until that test is done.

## Rules that don't bend

- **Don't reply to the port request issue.** No comments or reactions on it. Anything
  the owner needs goes in the PR or back to the user, and the maintainer talks to the
  owner.
- **Never loosen an existing profile's check** to admit a new car. Add a profile.
  Exact matching is what keeps Launchpad from sending commands to firmware nobody has
  inspected.
- **Don't commit the report, `launchpad-collect.log`, firmware, disassembly or raw
  vehicle logs.** Summarize what they showed in COMPATIBILITY.md instead.
- **Don't invent meanings for enum values or failure codes.** The indicator is 0 Off,
  1 Active, anything else Unknown. The failure field stays raw.
- **Battery temperature comes with every profile.** Once a new profile passes the
  firmware check, it reads the cluster minimum (`ClusterBatteryTemperatureReader`);
  there's no per-car switch. That's why moved cluster codes make a port case D. The
  battery warm notice is allowed on every car whose report shows `contract unchanged`.
  Don't claim the reading is accurate in COMPATIBILITY.md until the owner has compared
  it with an OBD reader.
- **Car-side scripts** (`tune-service/`, `usb/`) stay toolbox-only KitKat `sh` with LF
  endings, and nothing may write under `/system` except `/system/app/Launchpad.apk`.
