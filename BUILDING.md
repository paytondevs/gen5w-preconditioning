# Building Launchpad and the TUNE-knob service

The Android project builds Launchpad, package
`com.paytondev.launchpad`, for Android 4.4/API 19. The TUNE daemon source is in
`app/src/main/java/com/paytondev/launchpad/knob/`; its NEMESIS scripts and input
profile are in `tune-service/`.

The release version is the single line in [`VERSION`](VERSION), e.g. `1.3.0`.
Gradle uses it as `versionName` and derives `versionCode` as
`major * 10000 + minor * 100 + patch` (so minor and patch stay below 100).
`tools/build_usb_bundles.py` writes it into the packaged `NEMESIS.data`, whose
source keeps a `version=@VERSION@` placeholder, and CI tags the draft release
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

Signing keys and passwords are deliberately not included; any key works. The
NEMESIS package writes `/system/app/Launchpad.apk` directly instead of going
through `pm install`, so builds don't have to share a certificate, and CI signs
each build with a new throwaway key. Don't change verifier settings or use
`adb install` to work around a signing error.

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
`tools/build_apk.sh`), because the package installs it at
`/system/app/Launchpad.apk`. The same APK is also the **dex container** for the
root `app_process` TUNE listener (`KnobDaemon`, `Evdev`, `HoldDetector`), so the
visible app and the daemon always come from one build.

Outputs in `dist/`:

- `paytondev.launchpad.zip`: the NEMESIS package (`tune-service/` plus
  `app/Launchpad.apk`).
- `Launchpad-install-usb.zip`: the USB drive root. Its `type=execute`
  `NEMESIS.data` runs `launchpad/install.sh` (from `usb/launchpad/`, with a copy
  of `tune-service/scripts/common.sh`) against
  `packages/paytondev.launchpad.zip`.

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

How it works on the car:

- **install** (package not registered): `nemesis install`. The package's
  `postinstall` stages and verifies the APK in `/data/local/tmp/launchpad`, then
  writes `/system/app/Launchpad.apk`. It checks free space first, compares the
  result byte for byte, and restores the previous APK if that check fails.
- **update** (same drive, package already registered): `nemesis install
  --update`. NEMESIS runs the old package's `preuninstall`, which sees the new
  package in NEMESIS's stage directory and leaves the APK in place. The new
  `postinstall` then rewrites it in place, so app data survives. A plain
  `type=install` USB descriptor isn't used because, on the firmware this was
  tested on, NEMESIS answered "uninstall first" with Android up.
- **uninstall**: NEMESIS Manager → Packages → Uninstall, or `nemesis uninstall
  paytondev.launchpad`. `preuninstall` force-stops the app and removes
  `/system/app/Launchpad.apk`. There is no USB uninstall bundle.

Rule for these scripts: the only file under `/system` they may write or remove
is `/system/app/Launchpad.apk`. `/system` is remounted rw (`nemesis remount`)
only around that write, and only when the file actually changes. Car-side
scripts are toolbox-only `#!/system/bin/sh` (KitKat mksh: no `grep`, `awk`,
`cp` or busybox).

The service uses package id `paytondev.launchpad` and registry name
`paytondev.launchpad/knobd`. It opens `com.paytondev.launchpad/.MainActivity`
and passes the existing `from_knob` intent extra. It listens passively, without
grabbing the input device or injecting events. Its default profile expects the NEEV23 US (Ioniq 5) device name
`daudio_wp_daudioplus_neev23_us`, key code 17, and an 800 ms hold; the
NEEV23 US 260206 porting kit report showed the same device and key. On the exact
EV6 US API 19 / 260507 identity, `knob.conf` selects
`daudio_wp_daudioplus_cvev_us` with the same key and hold duration, and on the
exact NEEV US (Ioniq 5) API 19 / 260507 identity it selects
`daudio_wp_daudioplus_neev_us`. On the exact CEEV US (Ioniq 6) API 19 / 260507
identity it selects `daudio_wp_daudioplus_ceev_us`. The NEEV KR (Ioniq 5) 260902
identity has no entry, because its porting kit report recorded no TUNE press.

Publish `Launchpad-install-usb.zip`, `Launchpad-port-kit-usb.zip`,
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
