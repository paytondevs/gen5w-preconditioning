# Building Launchpad and the TUNE-knob service

The Android project builds Launchpad, Android package
`com.paytondev.precondition` (Java package and namespace
`com.paytondev.launchpad`), for Android 4.4/API 19. The TUNE daemon source is in
`app/src/main/java/com/paytondev/launchpad/knob/`; its NEMESIS scripts and input
profile are in `tune-service/`.

The release version is the single line in [`VERSION`](VERSION), e.g. `1.3.0`.
Gradle uses it as `versionName` and derives `versionCode` as
`major * 10000 + minor * 100 + patch` (so minor and patch stay below 100).
`tools/build_usb_bundles.py` writes it into the packaged `NEMESIS.data`, whose
source keeps a `package-version=@VERSION@` placeholder, and CI tags the draft release
`v<VERSION>`. Don't edit the version anywhere else.

## Requirements

- JDK 17 or later supported by the bundled Gradle/Android plugin (JDK 25 is used
  for local verification).
- Android SDK platform 37 and build-tools 36.0.0.
- Python 3 for packaging.

Set `JAVA_HOME` and `ANDROID_HOME` for your machine, or configure the SDK path
in an untracked `local.properties` file. The Gradle wrapper downloads Gradle;
the first build also downloads Android build dependencies.

## Tests and development builds

```sh
./gradlew :app:testDebugUnitTest :app:lintRelease :app:assembleDebug :app:assembleRelease
python3 tools/test_ev6_scripts.py
python3 tools/test_temperature_binder.py
python3 tools/test_service_rebind.py
python3 tools/test_usb_bundles.py
```

Debug uses Android's local debug signing key. On a ranchu/goldfish emulator, the
debug vehicle client simulates a pending request; it never reports heating from
a timer. Release uses the OEM client and rejects incompatible firmware.

The temperature contract test uses `JAVA_HOME` or `java`/`javac` on PATH, compiles
the real reader against small host Binder fixtures, and writes results under
`build/temperature-contract/`. It never contacts a car. It does not substitute
for Android service lifecycle or in-car verification.

The service rebind test compiles the real `PreconditioningService` against small
Android fixtures under `build/service-rebind/`. It models Android's rule that a
client returning to a live service gets `onRebind` only when `onUnbind` returned
true, and checks that temperature polling resumes during a session. It never
contacts a car and does not replace an in-car check.

The unsigned release is at
`app/build/outputs/apk/release/app-release-unsigned.apk`. Do not install it as a
finished release. Building does not connect to the car.

## Signed, API 19-compatible release

Signing keys and passwords are deliberately not included; any key works.
NEMESIS installs the APK with `pm install` and, on every update, uninstalls the
old APK before installing the new one, so builds don't have to share a
certificate, and CI signs each build with a new throwaway key. Don't change
verifier settings or use `adb install` to work around a signing error.

Set these environment variables privately, without committing them:

- `LAUNCHPAD_KEYSTORE`: absolute path to the private keystore.
- `LAUNCHPAD_STORE_PASSWORD`: keystore password.
- `LAUNCHPAD_KEY_PASSWORD`: key password (defaults to the store password).
- `LAUNCHPAD_KEY_ALIAS`: key alias (defaults to `launchpad`).
- `ANDROID_HOME`: Android SDK location.
- `JAVA_HOME`: JDK location, if needed.

Then run:

```sh
sh tools/build_apk.sh
```

The script runs tests/lint, builds, removes prior ZIP signature metadata, signs
with the existing v1 SHA1/RSA procedure, aligns the APK, and verifies the
signature for API 19. The final artifact is `dist/Launchpad.apk`.

## NEMESIS package and USB bundles

```sh
python3 tools/build_usb_bundles.py            # uses dist/Launchpad.apk
python3 tools/build_usb_bundles.py --apk path/to/signed.apk
```

The builder requires a **signed** APK (by default the `dist/Launchpad.apk` from
`tools/build_apk.sh`), because NEMESIS installs it with `pm install` (the
descriptor's `[app.0] kind=apk`). The same APK is also the **dex container** for the
root `app_process` TUNE listener (`KnobDaemon`, `Evdev`, `HoldDetector`), so the
visible app and the daemon always come from one build.

Outputs in `dist/`:

- `paytondev.launchpad.zip`: the NEMESIS package (`tune-service/` plus
  `app/Launchpad.apk`). NEMESIS Manager lists it as Precondition; its package
  ID stays `paytondev.launchpad`, so installs and updates match 1.x's package.
- `Launchpad-install-usb.zip`: the USB drive root. Its `type=install`
  `NEMESIS.data` points at `packages/paytondev.launchpad.zip`, so NEMESIS prompts,
  installs it, and offers to update it when it's already installed. There are no
  scripts of our own on this drive. Needs NEMESIS 1.16 or newer.
- `Launchpad-manual-install-usb.zip`: the same drive for any NEMESIS. Its
  `type=execute` `NEMESIS.data` runs `launchpad/install.sh` (from `usb/launchpad/`,
  with a copy of `tune-service/scripts/common.sh`), which installs or updates
  `packages/paytondev.launchpad.zip`, writes `launchpad-install.log` and checks
  that Android has the app under `/data/app` and that `knobd` runs afterwards.

- `Launchpad-port-kit-usb.zip`: the read-only vehicle porting kit. Its
  `type=execute` `NEMESIS.data` runs `launchpad-port/collect.sh` (from
  `usb/port-kit/`, with copies of `lib.sh` and `common.sh`). There is no
  `packages/` folder, so it has nothing to install. It writes
  `launchpad-port-report.txt` to the drive. The same signed APK is copied to
  `/data/local/tmp/launchpad-port` only as the dex container for `PortProbe`,
  run through `app_process`, and is removed afterwards with its dalvik-cache entry.
  `PortProbe` makes no Binder call except `getInterfaceDescriptor()`, reads
  transaction codes from the OEM framework classes by reflection, and listens to
  key events passively. See
  [COMPATIBILITY.md](COMPATIBILITY.md#car-not-listed-run-the-vehicle-porting-kit).

The install USB ZIP also includes a plain-text `README.txt` (from
`usb/README-template.txt`) with its log filename, installation steps and the
known EV6 display limitation. The packaging test builds the bundle in a
temporary directory using a synthetic APK; it checks the descriptor target,
exact contents, unchanged APK bytes and LF script endings, and rejects
unsigned/missing-dex inputs. It also exercises the Windows and shell USB writers
against temporary fixture drives, checking preservation of old instructions/OTA
files and unrelated personal files. Each writer test requires its corresponding
shell; unavailable shells are reported as skipped. It does not validate a real
APK signature or installation on Android. The packager's signature-file check
is only a structural check; use the signing verification in `build_apk.sh` for
the actual release APK.

`tools/usb/write-usb.sh` / `write-usb.ps1` write the bundle onto a drive. They
move any previous NEMESIS files on it into a backup folder rather than deleting
them.

How it works on the car. The descriptor's `[app.0]` is `kind=apk`, so NEMESIS
itself runs `pm install` on `app/Launchpad.apk` and `pm uninstall` on removal.
That makes the app a normal `/data/app` app, which is what NEMESIS Manager's
home-screen grid lists (it skips system apps):

- **install** (package not registered): `nemesis install`. `preinstall` checks
  for Launchpad 1.x (below), NEMESIS runs `pm install`, then `postinstall`
  restores any saved settings and starts `PreconditioningService` once. That
  start takes the new package out of Android's "stopped" state, so
  `BootReceiver` gets `BOOT_COMPLETED`; nothing may force-stop the app after it.
- **update** (same drive, package already registered): `nemesis install
  --update`. NEMESIS runs the old package's `preuninstall`, which sees the new
  package in NEMESIS's stage directory and copies the app's `shared_prefs`
  files to `/data/nemesis/backup/paytondev.launchpad/prefs`. NEMESIS then
  uninstalls the old APK (which deletes its data) and installs the new one, and
  the new `postinstall` puts the settings back, owned by the app's new uid.
- **uninstall**: NEMESIS Manager → Packages → Uninstall, or `nemesis uninstall
  paytondev.launchpad`. `preuninstall` deletes the backup folder, then NEMESIS
  runs `pm uninstall`. There is no USB uninstall bundle.
- **from 1.x**: 1.x was Android package `com.paytondev.launchpad`, copied to
  `/system/app/Launchpad.apk`. Android keeps that system package registered,
  with its data, until it restarts, even once the file is gone, so 2.x uses its
  own package, `com.paytondev.precondition`, and installs right away. When
  `preinstall` finds the 1.x file or 1.x's data folder, it copies 1.x's
  `shared_prefs` to the backup folder (unless an update's own backup is already
  waiting), removes the file, and lets the install continue. `postinstall` then
  restores them into the new app. A car without 1.x skips all of this.

Rule for these scripts: nothing writes under `/system`. The only `/system`
operation is removing the 1.x file `/system/app/Launchpad.apk`, with `/system`
remounted rw (`nemesis remount`) only around that. Persistent state goes in
`/data/nemesis/backup/paytondev.launchpad`, temporary files in
`/data/local/tmp`. Car-side scripts are toolbox-only `#!/system/bin/sh`
(KitKat mksh: no `grep`, `awk`, `cp` or busybox).

The service uses package id `paytondev.launchpad` and registry name
`paytondev.launchpad/knobd`. It opens `com.paytondev.precondition/com.paytondev.launchpad.MainActivity`
and passes the existing `from_knob` intent extra. It listens passively, without
grabbing the input device or injecting events. Its default profile expects the NEEV23 US (Ioniq 5) device name
`daudio_wp_daudioplus_neev23_us`, key code 17, and an 800 ms hold; the
NEEV23 US 260206 and 260903 porting kit reports showed the same device and key. On the exact
EV6 US API 19 / 260507 identity, `knob.conf` selects
`daudio_wp_daudioplus_cvev_us` with the same key and hold duration, as it does on
the exact EV6 US API 19 / 251120 identity, and on the
exact NEEV US (Ioniq 5) API 19 / 260507 identity it selects
`daudio_wp_daudioplus_neev_us`. On the exact CEEV US (Ioniq 6) API 19 / 260507
identity it selects `daudio_wp_daudioplus_ceev_us`. On the exact EV6 KR API 19 /
260902 identity it selects `daudio_wp_daudioplus_cvev_kr`, with the same key and
hold duration. The NEEV KR (Ioniq 5) 260902
identity has no entry, because its porting kit report recorded no TUNE press,
and neither does the NEEV US (Ioniq 5) 240822 identity, because its porting kit
report didn't confirm a TUNE hold.

Publish `Launchpad-install-usb.zip`, `Launchpad-manual-install-usb.zip`, `Launchpad-port-kit-usb.zip`,
`paytondev.launchpad.zip` (for manual ADB installs) and `dist/Launchpad.apk`
as GitHub release assets. These
scripts generate local artifacts only and never connect to a car.

## Continuous integration

`.github/workflows/build.yml` runs the same commands as above:

- **Pull requests**: tests, lint and an unsigned release build. No secrets.
- **Merges to `main`** (or a manual run on `main`): generates a throwaway
  signing key, runs `tools/build_apk.sh` with it, then
  `tools/build_usb_bundles.py`, then `SHA256SUMS.txt`. The files are kept as a
  workflow artifact and uploaded to the draft release `v<VERSION>`. There is
  one draft per version: each merge retargets it to the new commit and replaces
  its assets, leaving the notes alone. Publishing creates the tag.

Once `v<VERSION>` is published, later merges only produce the workflow
artifact. To start the next draft, change `VERSION`. The workflow needs no
secrets.

See [COMPATIBILITY.md](COMPATIBILITY.md) for firmware and verification limits.
See [RELEASE-PREPARATION.md](RELEASE-PREPARATION.md) for release decisions,
field-test limits and the release steps.
Icons retain the bundled [Phosphor license](app/src/main/assets/phosphor-LICENSE.txt).
