# compatibility

## compatibility chart

Launchpad only works on the exact cars and head-unit software versions below. Your head unit's **software version** is what decides it, not the model year.

| vehicle | model years | market | head unit | software version | preconditioning | battery temperature | status |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Hyundai IONIQ 5 | 2022–2023 | US | NEEV (`wp_daudioplus_neev_us`) | `NEEV.USA.0000.V015.012.260507` | ✅ | ✅ | ✅ supported |
| Hyundai IONIQ 5 | 2022–2023 | US | NEEV23 (`wp_daudioplus_neev23_us`) | 260507 (see below) | ✅ | ✅ | ✅ supported |
| Hyundai IONIQ 5 | 2024 | US | NEEV23 (`wp_daudioplus_neev23_us`) | `NEEV23.USA.0000.V015.011.260206` | ✅ | ✅ | ✅ supported |
| Kia EV6 | 2023 | US | CVEV (`wp_daudioplus_cvev_us`) | `CVEV.USA.0000.V015.012.260507` | ✅ | ✅ | ✅ supported |
| Hyundai IONIQ 6 | 2023 | US | CEEV (`wp_daudioplus_ceev_us`) | `CEEV.USA.0000.V015.012.260507` | ✅ | ✅ | ✅ supported |

- **NEEV23:** the supported NEEV23 firmware is the 260507 release. Launchpad doesn't check the software version for that release; it checks the head unit's firmware files instead. The older 260206 release (shown in the car's settings as `NEEV23.USA.S5W_M.V015.011.260206`) has its own entry, which checks both.
- **Other model years with the same head unit and software** will probably work, but haven't been tested.
- **Other software versions aren't supported yet.** Support for the newest software is being worked on.
- **On an unsupported car, nothing bad happens.** Launchpad checks the car before it enables **Start**. If it doesn't match a row above, preconditioning shows **Unavailable** and Launchpad never sends anything to the car.
- **EV6 owners** can follow the [EV6 guide](EV6-QUICK-START.md).
- **IONIQ 6:** the car's settings show this software as `CEEV.USA.S5W_M.V015.012.260507`.

## car not listed? run the vehicle porting kit

The vehicle porting kit is a USB drive that reads the details needed to add your car and saves them to a text file on the drive. **It's read-only:** it doesn't install anything, doesn't change any files on the car, and doesn't start preconditioning. It also works on cars that are already supported.

You need [NEMESIS](README.md#1-install-nemesis) installed first, same as for Launchpad.

1. Download **`Launchpad-port-kit-usb.zip`** from the [releases page](../../releases).
2. Extract it so its **contents** sit at the **root** of an empty FAT32 or exFAT USB drive. It should look like this:
   ```text
   USB drive/
   ├── README.txt
   ├── NEMESIS.data
   └── launchpad-port/
   ```
   Move any other `NEMESIS.data` or Launchpad installer files off the drive first.
3. Park, turn the car on, and wait for the screen to finish starting. Plug the drive into the USB **data** port and tap **Yes** when NEMESIS asks.
4. **Optional, but it helps:** right after tapping **Yes**, hold the right-hand knob that changes radio stations (TUNE) for about 2 seconds, let go, and repeat until the result appears. This shows which button can open Launchpad on your car. Skip it if your car has no such knob.
5. Wait for the result, about 30 seconds. No reboot is needed.

The drive now has **`launchpad-port-report.txt`**. It's plain text, so you can open it and see everything in it. It contains no VIN, serial number or location.

### send it in

Open a **[Vehicle port request](../../issues/new?template=vehicle-port.yml)** issue on GitHub and include:

- **your vehicle:** model, model year, trim and market, e.g. *US 2024 IONIQ 5 Limited AWD*
- **your head unit's software version**, as shown in the car's settings
- **the report:** drag `launchpad-port-report.txt` into the issue. If the kit said it failed, attach `launchpad-collect.log` from the drive too.

A report doesn't guarantee your car can be supported, but it's everything needed to find out. Developers adding a car from a report: see [CONTRIBUTING.md](CONTRIBUTING.md#vehicle-ports).

## technical details

This part is for developers. It covers how each supported head unit is identified, the checks Launchpad makes before it enables **Start**, and what was tested in which car.

Two Ioniq 5 head units are supported, identified by their device codes:
`wp_daudioplus_neev23_us` (NEEV23 US) and `wp_daudioplus_neev_us` (NEEV US,
below). The NEEV23 US model year is unconfirmed; the NEEV US profile was
verified on a US 2022 Ioniq 5 SE AWD.

The NEEV23 US profile, the first one in this project, targets
`wp_daudioplus_neev23_us` on Android 4.4/API 19. It validates these firmware files before enabling Start:

| File | SHA-256 |
| --- | --- |
| `/system/framework/automotive.odex` | `dd72e04bce0a2b4bda1506308ac21f91002c1436553b8156c345cc4313661338` |
| `/system/framework/automotive-service.odex` | `3cf40fb00854c63afa49abea1dad18a0afcbc0dd68b17de9ed7ea3f5aeab4222` |

It checks HEV service `com.hkmc.micom.hev` / descriptor
`com.hkmc.hev.IHEVService` and BCM service `com.hkmc.micom.bcm` / descriptor
`com.hkmc.bcm.IBCMService`. An incompatible or unreadable interface leaves the
feature unavailable. Do not remove these checks to support another firmware;
establish its command and feedback profile first.

### NEEV US 260507 profile

Verified vehicle: **US 2022 Hyundai Ioniq 5 SE AWD**.

The additional profile requires Android API 19, device `wp_daudioplus_neev_us`,
and `ro.build.product=NEEV.USA.0000.V015.012.260507` (build `KVT49L`). A
read-only USB capture on 2026-10-27 found both framework files byte-identical to
the NEEV23 US profile, so it requires those same two fingerprints above.
`com.hkmc.micom.hev` / `com.hkmc.hev.IHEVService` and `com.hkmc.micom.bcm` /
`com.hkmc.bcm.IBCMService` were both present. No command or enum remapping was
made.

Holding TUNE was verified on that car on 2026-10-27: key 17 arrives on the main
`daudio_wp_daudioplus_neev_us` input device (not its `_can` or `_knob`
devices), so `knob.conf` selects that name on the exact NEEV US identity with
the original 800 ms hold.

Field testing on that car repeated the EV6 physical tests below, parked with no
active navigation route, using a local build of this profile signed with a
personal key:

- The installed app observed Off -> Active -> Off using the existing command
  arguments and feedback mapping, first on 2026-10-27.
- The owner confirmed app Start/Stop and Ready after an infotainment restart
  following Stop. A cool-battery morning test also activated.
- Holding TUNE opened Launchpad after reboot. Recorded input was key 17 on
  `daudio_wp_daudioplus_neev_us`; holding TUNE opens the screen and does not
  issue Start.

Matching framework files show the Binder interface is the same code; they do not
prove the vehicle MICOM/BMS behind it responds identically on other cars. The
same items left unverified on the EV6 (driving activation, navigation
interaction, active-session power loss, and a public release cycle) remain
unverified here. No firmware, raw logs, or disassembly are included.

**Battery temperature:** read-only USB captures on
2026-10-28 found `com.hkmc.micom.cluster` / `com.hkmc.cluster.IClusterService`
present, and this profile's `automotive.odex` declares the same cluster contract
the EV6 reader uses: `addNavigationClusterListener` 37,
`requestBatteryInfoReq()` 109, `removeNavigationClusterListener` 38, and
listener `onHandleBatteryInfoResp(int[])` 16. The temperature display and warm
advisory are therefore enabled for this exact profile. The interface does not
define the array layout, so the EV6 decoding (eight values, index 1 as minimum
pack temperature) was checked in the car: on 2026-10-28 the owner confirmed
the Launchpad reading appeared and agreed with an OBD reader's temperature.
Exact values were not recorded. On the same day, with the battery already
warm, tapping Start showed the Battery warm notice. NEEV23 US shares these
framework files and reads the same contract; its in-car temperature check is
still pending.

**Other model years:** other US-market Ioniq 5 vehicles reporting this exact
device, software version, and both firmware fingerprints may also be
compatible, but have not been tested beyond the verified **US 2022 Ioniq 5 SE AWD**.
Model year alone does not establish compatibility.

### NEEV23 US 260206 profile

Vehicle: **US 2024 Hyundai Ioniq 5 Limited AWD**
([port request #16](../../issues/16)).

The profile requires Android API 19, the NEEV23 US device
`wp_daudioplus_neev23_us`, and `ro.build.product=NEEV23.USA.0000.V015.011.260206`
(build `KVT49L`; the settings screen shows `ro.build.product2`,
`NEEV23.USA.S5W_M.V015.011.260206`), plus both fingerprints from a read-only
porting kit report (kit 1.4.0) collected on 2026-10-29:

| File | SHA-256 |
| --- | --- |
| `/system/framework/automotive.odex` | `301b134d231b1ca1d0aa75ae090d219ac963fc935c3f3bc0b3b1393c6614703e` |
| `/system/framework/automotive-service.odex` | `efb949ec92fff388cbbb78e5bf4e16df3cc8d417dbf5cb5e87e62fb46d542549` |

These framework files differ from the 260507 NEEV23 US ones. The original
NEEV23 US check (its own hashes, any software) is unchanged; this profile is a
separate exact match on the same device, and neither hash pair can be mixed
with the other. `OemVehicleClient` reads `ro.build.product` on NEEV23 US for
it. The report shows `com.hkmc.micom.hev` / `com.hkmc.hev.IHEVService` and
`com.hkmc.micom.bcm` / `com.hkmc.bcm.IBCMService` present, and every
transaction Launchpad uses keeps its code: HEV 3 `getHEVStatus`, 62
`setBattCondEvStation`, 65 `setBattCondRouteDis`, 66 `setBattCondRemainTime`,
and BCM 109 `getBattCondIndSta`. No command or enum remapping was made. Status
reads use the same `getHEVStatus` ID arguments as the other profiles (94
enabled, 44 ready, 115 failure).

Holding TUNE during the kit run recorded key 17 on the main
`daudio_wp_daudioplus_neev23_us` input device, which is already the `knob.conf`
default, so TUNE opens Launchpad with the original 800 ms hold and `knob.conf`
is unchanged.

**Battery temperature:** the report shows `com.hkmc.micom.cluster` /
`com.hkmc.cluster.IClusterService` present with an unchanged contract:
`addNavigationClusterListener` 37, `requestBatteryInfoReq()` 109 and
`removeNavigationClusterListener` 38. Launchpad reads it with the same listener
callback and array layout as the other profiles. As on the NEEV23 US device's
260507 release, the warm notice uses 21°C / 70°F and the pending timeout is two
minutes.

No report, firmware, raw logs, or disassembly are included.

### CEEV US 260507 (Ioniq 6) profile

Vehicle: **US 2023 Hyundai Ioniq 6 SEL**
([port request #18](../../issues/18)).

The profile requires Android API 19, device `wp_daudioplus_ceev_us`, and
`ro.build.product=CEEV.USA.0000.V015.012.260507` (build `KVT49L`; the settings
screen shows `ro.build.product2`, `CEEV.USA.S5W_M.V015.012.260507`), plus both
fingerprints from a read-only porting kit report (kit 1.4.0) collected on
2026-10-29:

| File | SHA-256 |
| --- | --- |
| `/system/framework/automotive.odex` | `408b426e14b3f8dfdf1926e608e4f02689e89fc386b7f93a97f261ac5b6e4287` |
| `/system/framework/automotive-service.odex` | `afbc60509dd8a8fbac842523bf618424c088685d832a125a84d1d11dc1770bd4` |

These framework files differ from both the Ioniq 5 and EV6 profiles, and no
profile accepts another's hashes. The report shows `com.hkmc.micom.hev` /
`com.hkmc.hev.IHEVService` and `com.hkmc.micom.bcm` / `com.hkmc.bcm.IBCMService`
present, and every transaction Launchpad uses keeps its code: HEV 3
`getHEVStatus`, 62 `setBattCondEvStation`, 65 `setBattCondRouteDis`, 66
`setBattCondRemainTime`, and BCM 109 `getBattCondIndSta`. No command or enum
remapping was made. Status reads use the same `getHEVStatus` ID arguments as the
other profiles (94 enabled, 44 ready, 115 failure).

Holding TUNE during the kit run recorded key 17 on the main
`daudio_wp_daudioplus_ceev_us` input device (not its `_can` or `_knob`
devices), so `knob.conf` selects that name on the exact CEEV US identity with
the original 800 ms hold.

**Battery temperature:** the report shows `com.hkmc.micom.cluster` /
`com.hkmc.cluster.IClusterService` present with an unchanged contract:
`addNavigationClusterListener` 37, `requestBatteryInfoReq()` 109 and
`removeNavigationClusterListener` 38. Launchpad reads it with the same listener
callback and array layout as the other profiles. The warm notice uses the 20°C /
68°F default, and the pending timeout is two minutes, the same as the Ioniq 5.

No report, firmware, raw logs, or disassembly are included.

### EV6 US 260507 profile

**Other model years:** US-market 2022–2024 EV6 vehicles with the same Gen5W
hardware and exact supported firmware may also be compatible, but have not
been tested beyond the verified **US 2023 EV6 Wind RWD**. The app checks device
identity, software version, and both firmware fingerprints before allowing
preconditioning. Reports from owners of matching 2022 and 2024 vehicles are
welcome. Model year alone does not establish compatibility.

The additional profile requires Android API 19, device `wp_daudioplus_cvev_us`,
and `ro.build.product=CVEV.USA.0000.V015.012.260507`, plus both fingerprints:

| File | SHA-256 |
| --- | --- |
| `/system/framework/automotive.odex` | `b9e7e02b81e1a8cfcf767c6b8bbb28bcf1d2c411a208fbf6a465e7f0c662774a` |
| `/system/framework/automotive-service.odex` | `8af745b41aa28f490953d61531215cd94e059f104b31f6236a8f6c7af23b8062` |

The property is read directly: Android `Build.PRODUCT` exposes a different
property. The NEEV23 US checks remain unchanged. Neither profile accepts
the other profile's hashes. The HEV/BCM descriptors are checked for both.

Field testing on October 24-25, 2026 used a US 2023 Kia EV6 Wind RWD, Gen5W
software `CV_E.USA.S5W_M.V015.012.260507`, with NEMESIS 1.15:

- Supervised Start/Stop scripts and the installed app observed Off -> Active ->
  Off using the existing command arguments and feedback mapping below.
- The owner confirmed app Start/Stop and Ready after an infotainment restart
  following Stop. A subsequent cool-battery morning test also activated.
- Holding TUNE opened Launchpad after reboot. Recorded input was key 17 on
  `daudio_wp_daudioplus_cvev_us`; the original 800 ms hold and passive event
  handling are retained. Holding TUNE opens the screen; it does not issue Start.
- The first installer attempt failed because this head unit's toolbox `cmp`
  rejects `-s`. Quiet byte comparisons now redirect output instead, including
  staging, installation, rollback, and USB completion checks.

These observations came from local builds carrying these changes, not from a
published upstream release. This follow-up now includes the temperature display,
warm advisory and timeout from later local builds; their separate evidence and
limits are in [TEMPERATURE-AND-TIMEOUT.md](TEMPERATURE-AND-TIMEOUT.md). The
extracted source combination has host build/test coverage but has not itself
been installed as a new vehicle build.

No EV6 command/enum remapping was needed. Start/Stop command arguments, package
IDs, release version, and original Ioniq TUNE default remain unchanged. The
controller adds a pending timeout: five minutes on the EV6 and two minutes on
the Ioniq 5 profiles and the Ioniq 6. Native temperature reads and the warm advisory run on
every supported profile once its firmware check passes.
No raw vehicle logs, firmware, disassembly, APKs, or signing material
are included. This is evidence for the listed profile, not blanket EV6 support.

Tests used no active Kia navigation route. Driving activation, interaction with
native navigation, other trims/regions/software, active-session process/power
loss, and a public fresh-install/update/uninstall cycle remain unverified.
Closing the app is not Stop. Do not update or uninstall during an active or
pending request; stop it and confirm Off first. Public APKs should be built and
signed through the maintainer's release process. Local test keys differ from the
upstream key and do not establish cross-signed update compatibility.

### Command profile

The Binder argument mapping was derived from the inspected OEM framework and
native navigation implementation, using the public
[gen5w-utils decryption implementation](https://github.com/Helloyunho/gen5w-utils/blob/master/decrypt_navi.py).
Proprietary firmware binaries and disassembly are not included in this repository.
The upstream [CAN reference logs](https://github.com/tylerharvey/Ioniq5_CAN/tree/main/preconditioning_messages)
are corroborating evidence; their bytes are not sent as raw Binder arguments.

- Start: HEV transactions 65(9), 66(18), wait 10 ms, then 62(2,3).
- Maintain: refresh only 65(9), 66(18) every 5 seconds.
- Cancel: stop refreshing, then attempt 65(2047), 66(1023), 62(0,0).
- Station events have an OEM 120 ms reset; command sequences are serialized
  through a 130 ms window.
- The 9 km / 18 minute values are synthetic request metadata, not a heating
  countdown or a changed navigation destination.
- BCM transaction 109 reports the conditioning indicator: 0 Off, 1 Active,
  other values Unknown. The failure field is kept raw; enum meanings are not
  invented.

The app does not transmit raw CAN/UART, write the global conditioning
preference, or coordinate with native navigation in V1. Start requires the
existing enable setting, EV READY, and an Off indicator. The BMS retains control
of temperature, charge, and other operating constraints.

### Session behavior and verification limits

A foreground service owns a requested session after the activity closes. Stop
is available while waiting. Recovery cancels uncertain ownership and never
automatically starts conditioning. A successful Binder call or elapsed 60–120
seconds does not establish activation; only vehicle feedback can show Active.

The source includes unit tests for the request/feedback distinction, cancellation,
communication failures, recovery, navigation between app pages, favorites,
connection formatting, and TUNE event parsing/hold detection. Emulator tests
and static profile derivation do not establish real battery heating. At source
publication, the NEEV23 US profile had not been verified in a parked car
in this workspace. The later EV6 field observations are scoped separately above.
