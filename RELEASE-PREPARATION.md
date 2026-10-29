# EV6 release handoff

Status: source contribution for maintainer review; not a published binary release.
The compatibility contribution is PR #2 and temperature/timeout follow-up is PR #3.
The follow-up depends on the compatibility changes. `VERSION` is now 1.3.0,
covering the later NEEV US vehicle port (PR #7); the maintainer selects the
release version there before publishing (the version code follows from it). Personal ev6.* build numbers are
not the proposed public release sequence.

## What owners can expect

- Hold TUNE to open Launchpad, then use Start/Stop without a Kia navigation route.
- Exact firmware checks for the tested US 2023 EV6 Wind RWD / Gen5W 260507 profile.
- Native lowest battery-pack temperature, Fahrenheit on Home and both units in
  Advanced. No OBD adapter or phone bridge is required.
- A ten-second warm-battery advisory with an explicit Start anyway option.
- Cancellation of a request never observed Active after five minutes, retaining
  the unresolved session until cancellation succeeds and fresh Off is received.

The owner has confirmed manual Start/Active/Stop, Ready after restart following
Stop, the TUNE shortcut, temperature refresh and a rising temperature, the warm
notice reset, and explicit Start anyway. An observed stop near a displayed
70°F / 21°C suggests a possible temperature-based stop; it is not a proven fixed
BMS cutoff. See [the detailed evidence](TEMPERATURE-AND-TIMEOUT.md).

## Limits to include in the release notes

- The five-minute pending timeout is covered by automated tests, not an in-car
  timeout observation. This is disclosed rather than requiring a manufactured
  failure before release preparation can proceed.
- Builds before 1.3.1 can leave temperature stale after leaving and returning
  during an active session; 1.3.1 fixes this (confirmed on the Ioniq 5 NEEV US
  profile, not rechecked on the EV6). Do not use a stale value to infer current
  temperature or heating status.
- Only the stated US 2023 vehicle/profile has been tested. US 2022-2024 vehicles
  with matching hardware and exact firmware are possible candidates, untested.
- Driving activation, native-navigation coexistence, active-session process or
  power loss, and failed native-read recovery are not claimed verified.
- The native temperature reader/advisory runs on the exact EV6 and Ioniq 5
  profiles; Ioniq 5 reads are not yet field-tested. The pending timeout is
  five minutes on the EV6 and two minutes on the Ioniq 5.

## Maintainer decisions before publishing

1. Review the two contributions, including the shared Ioniq controller behavior.
2. Clarify project licensing and the permitted distribution route. This branch
   does not add a license on behalf of the original author.
3. Choose the release version and put it in `VERSION`; the app, the NEMESIS
   package and the draft release all take it from there. There is no signing
   certificate to choose: CI signs every build with a new throwaway key. The
   package writes `/system/app/Launchpad.apk` directly rather than through
   `pm install`, so releases don't need to share a certificate.
4. Merge to `main`. CI builds and signs the APK, verifies it for API 19, builds
   the USB bundle from that exact APK, and attaches everything to the draft
   release `v<VERSION>` (see [BUILDING.md](BUILDING.md)).
5. Check a first installation, an update over a build signed with a different
   key that keeps app data, and removal from NEMESIS Manager, using the draft's
   artifacts. Stop and confirm Off before each change. Verify TUNE after reboot
   and that removal leaves NEMESIS intact. Host packaging and hook tests support
   these checks but do not replace them.
6. Publish the draft. It carries the APK, NEMESIS package,
   `Launchpad-install-usb.zip` and `SHA256SUMS.txt`, and targets the source
   commit. The APK's certificate fingerprint is printed by `apksigner verify`
   in that commit's CI log if the release record needs it.

The USB ZIP contains README.txt instructions. Link owners to
[EV6-QUICK-START.md](EV6-QUICK-START.md) from the release page. Keep the status at
the top of that guide accurate when the first supporting release is available.
Only then propose the released feature for the NEMESIS community catalog, linking
the public release and guide rather than private development USB packages.

EV6 development, documentation and vehicle testing: **Abro86**. Development,
documentation and host-test assistance: **OpenAI Codex**. Original application:
**paytondevs** and contributors.
