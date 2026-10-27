# V1 compatibility and verification

The original Ioniq 5 preconditioning profile targets `wp_daudioplus_neev23_us` on
Android 4.4/API 19. It validates these firmware files before enabling Start:

| File | SHA-256 |
| --- | --- |
| `/system/framework/automotive.odex` | `dd72e04bce0a2b4bda1506308ac21f91002c1436553b8156c345cc4313661338` |
| `/system/framework/automotive-service.odex` | `3cf40fb00854c63afa49abea1dad18a0afcbc0dd68b17de9ed7ea3f5aeab4222` |

It checks HEV service `com.hkmc.micom.hev` / descriptor
`com.hkmc.hev.IHEVService` and BCM service `com.hkmc.micom.bcm` / descriptor
`com.hkmc.bcm.IBCMService`. An incompatible or unreadable interface leaves the
feature unavailable. Do not remove these checks to support another firmware;
establish its command and feedback profile first.

## EV6 US 260507 profile

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
property. The original Ioniq 5 checks remain unchanged. Neither profile accepts
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
controller adds a pending timeout: five minutes for the EV6 profile and two minutes
for the Ioniq 5, whose activation normally follows in about 60–70 seconds. Native
temperature reads and the warm advisory run only on the exact EV6 and Ioniq 5
profiles, which share one cluster contract (see TEMPERATURE-AND-TIMEOUT.md).
No raw vehicle logs, firmware, disassembly, APKs, or signing material
are included. This is evidence for the listed profile, not blanket EV6 support.

Tests used no active Kia navigation route. Driving activation, interaction with
native navigation, other trims/regions/software, active-session process/power
loss, and a public fresh-install/update/uninstall cycle remain unverified.
Closing the app is not Stop. Do not update or uninstall during an active or
pending request; stop it and confirm Off first. Public APKs should be built and
signed through the maintainer's release process. Local test keys differ from the
upstream key and do not establish cross-signed update compatibility.

## Command profile

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

## Session behavior and verification limits

A foreground service owns a requested session after the activity closes. Stop
is available while waiting. Recovery cancels uncertain ownership and never
automatically starts conditioning. A successful Binder call or elapsed 60–120
seconds does not establish activation; only vehicle feedback can show Active.

The source includes unit tests for the request/feedback distinction, cancellation,
communication failures, recovery, navigation between app pages, favorites,
connection formatting, and TUNE event parsing/hold detection. Emulator tests
and static profile derivation do not establish real battery heating. At source
publication, the original Ioniq 5 profile had not been verified in a parked car
in this workspace. The later EV6 field observations are scoped separately above.
