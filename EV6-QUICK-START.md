# EV6: install and use manual battery preconditioning

**Release status:** these instructions accompany an EV6 contribution under
review. Wait for a maintainer release whose notes explicitly include EV6 US
260507 support and the temperature/timeout improvements described here. The
existing release downloads may not contain them. This branch does not provide
a new signed installer.

## Check your car first

Tested vehicle: **US 2023 Kia EV6 Wind RWD**, with **Standard-class Gen5W
Navigation** and software **`CV_E.USA.S5W_M.V015.012.260507`**.

**Other model years:** US-market 2022–2024 EV6 vehicles with the same Gen5W
hardware and exact supported firmware may also be compatible, but have not
been tested beyond the vehicle above. The app checks device identity, software
version, and both firmware fingerprints before allowing preconditioning.
Reports from owners of matching 2022 and 2024 vehicles are welcome. Model year
alone does not establish compatibility.

Open **Setup -> General -> Software information/Update** to check your software
version. Match the **Software Version** line, not the separate Firmware Version
or map version. A different model year, market, or software version has not been
verified by this contribution. The app also checks internal firmware fingerprints
before allowing Start. See [compatibility details](COMPATIBILITY.md).

You need NEMESIS installed, a computer, and a FAT32 or exFAT USB drive. No OBD
adapter, phone connection, Wi-Fi, or ADB is needed to use this feature.

## 1. Install NEMESIS once

If NEMESIS already works on your car, skip this step.

Follow the [official NEMESIS installation guide](https://github.com/xwtk/hmg_nemesis#quick-installation-guidance).
It covers the separate Engineering Mode installation. Engineering Mode entry
depends on the head unit; this app guide does not supply a universal PIN or
button sequence. Once NEMESIS is installed, Launchpad installation and use do
not require Engineering Mode.

To open NEMESIS Manager, press **NAV three times quickly** on a default NEMESIS
installation. If you previously chose **Setup** in NEMESIS **Options**, use
**SETUP** three times instead. NAV/SETUP opens NEMESIS; holding TUNE opens
Launchpad after installation. These are separate shortcuts.

## 2. Prepare the USB

1. From the maintainer's [release page](https://github.com/paytondevs/ioniq5-preconditioning/releases),
   choose a release explicitly listing EV6 US 260507 support.
2. Download **`Launchpad-install-usb.zip`**. The same drive installs Launchpad
   or updates an existing installation; read **Updating** below first if
   Launchpad is already installed.
3. Use an empty drive, or back up its contents to your computer. Move any old
   `NEMESIS.data`, `NEMESIS.sh`, `ota_package.tar`, `README.txt`, `launchpad`, and
   `packages` items off the drive before copying a new bundle.
4. Extract the ZIP. Copy the **contents** of its folder onto the drive, rather
   than copying the folder itself. Do not extract the inner package ZIP.
5. Check that you can see this layout immediately when you open the drive:

```text
USB drive/
  README.txt
  NEMESIS.data
  launchpad/
    common.sh
    install.sh
    lib.sh
  packages/
    paytondev.launchpad.zip
```

The public installer uses a USB confirmation prompt. It is different from the
personal test USB that opened Manager and required **Run USB NEMESIS.sh**.

## 3. Install while parked

1. Keep the car parked and on **READY**. Wait for infotainment to finish starting.
2. If a manual conditioning session is already running or waiting, press
   **Stop** and wait for the car to report **Off** before updating anything.
3. Insert the drive into the USB **data port** used for wired Android Auto or
   CarPlay, not a charge-only port.
4. Accept NEMESIS's prompt to run the USB item. If it expires, remove and reinsert
   the drive once. This installer does not need the Manager's manual Run button.
5. Wait for the success result. If it fails, follow **If something goes wrong**
   below instead of repeatedly rerunning it.
6. After success, remove the USB and restart infotainment using its recessed
   reset button. This is an infotainment restart, not a factory reset.
7. Once infotainment is back, **press and hold the TUNE knob for about one second**.
   Launchpad should open. Hold the right-hand TUNE knob, not the volume knob.

You do not need to leave the USB plugged in.

## 4. Start and stop conditioning

1. In the Kia EV settings, enable **Battery conditioning mode**. Recheck it after
   an infotainment update; a previous setting is not proof it is still enabled.
2. For the first check, keep the car parked in READY with **no active Kia
   navigation route**. You do not need to select a charger in Kia navigation.
3. Hold **TUNE** to open Launchpad. Tap **Start** beside Preconditioning.
4. **Request sent** means the request was sent; it does not mean heating has
   started. **Active** means the car reports conditioning active. Activation is
   not guaranteed: the car decides whether temperature, charge, and other
   conditions allow it.
5. Tap **Stop** to cancel, even while waiting. Wait for **Session ended** and the
   Advanced screen's **Car indicator: Off** before considering cancellation
   confirmed. If it remains unknown or Stop reports an error, retain the logs.

Holding TUNE opens the app; it does not start heating. Closing the app or
returning to Android Auto does **not** stop an active manual request. Use Stop.
You can stop a waiting request yourself at any time. The app also requests
cancellation after five minutes without confirmed activation, as explained below.

## Reading the temperature

- **Home:** `Battery temp: 61°F` is the lowest reported battery-pack sensor
  temperature, not the outside temperature or the average of all sensors.
- **Advanced:** `Lowest Battery Pack Temperature: 61°F (16°C)` shows both units
  and how many seconds ago the app received the response.
- While Launchpad is open, it asks for a reading roughly every ten seconds. The
  **received ...s ago** counter should reset after successful responses. The
  temperature does not need to change each time. In the field test, the owner
  saw a rise after a little over five minutes of conditioning.
- **Checking** means the first read is pending. **Unavailable** means no usable
  reading was obtained. **Stale** means the last reading is old or a refresh
  failed; do not treat it as current. Advanced retains the old value and its age.

Temperature polling is intended to stop when you leave Launchpad and resume
when you reopen it. Builds before 1.3.1 could leave the reading stale after
returning during an active session; 1.3.1 fixes this (confirmed on an Ioniq 5,
not yet rechecked on the EV6). If it still happens, leave and reopen Launchpad
again. A stale reading does not confirm the current temperature or stop an
active request. Use **Stop**
when you want to cancel conditioning.

Reading the temperature does not turn heating on. The incoming value uses
whole degrees Celsius, so Fahrenheit readings can move in steps. The response
age tells you when the app received data, not exactly when the car sampled it.

## If the battery is already warm

When you tap **Start**, the app requests a temperature reading. At **68°F / 20°C
or above**, it shows **Battery warm** with a ten-second countdown instead of
sending a conditioning request. If the car remains READY, enabled, and Off,
the button returns to **Start** when the countdown ends. You can simply leave
the notice alone; no heating request has been sent.

If you still want to request conditioning, tap the separate **Start anyway**
button during the notice. The car still decides whether to activate. This is
an advisory based on an expected heating threshold, not a confirmed BMS refusal
reason or a guarantee of charging speed. An unavailable temperature reading
does not block a normal manual request.

## If the car never confirms activation

After five minutes without an **Active** response, the app sends cancellation
and waits for a fresh **Off** response before restoring Start. It then shows
**No activation**. An already-active session is not stopped by this five-minute
timer. A timeout does not prove the battery was warm.

This timeout has automated test coverage but has not yet been observed in the
car. The reported stop near 70°F occurred after Active and may have been a
temperature-based stop; it does not establish a fixed cutoff or verify the timeout.

If cancellation fails, the app keeps the session unresolved and leaves **Stop**
available to retry. If cancellation was sent but Off has not been confirmed,
it continues waiting instead of claiming success. Keep the logs if this happens.

## Updating

Use **`Launchpad-install-usb.zip`** from a newer release and follow the
installation steps above, starting with stopping any pending or active session.
When Launchpad is already installed, the installer updates it in place and keeps
its settings; `launchpad-install.log` says it updated. Do not clear app data to
force an update.

## Removing Launchpad

Stop any pending or active session and confirm **Off**. Open NEMESIS Manager,
then **Packages -> Launchpad -> Uninstall**, and restart infotainment. This
removes Launchpad, its TUNE service and `/system/app/Launchpad.apk`; it leaves
NEMESIS installed. If it fails, use **Logs -> Copy to USB** and keep
`NEMESIS-logs`.

## If something goes wrong

| What you see | What to do |
| --- | --- |
| Only "No supported files" and no NEMESIS prompt | Check NEMESIS is installed, the drive uses a data port, and `NEMESIS.data` is at the drive's top level. The media-player message alone does not diagnose NEMESIS. |
| Install/update reports failure | Keep the USB inserted. Open NEMESIS Manager, then **Logs -> Copy to USB**. Bring it back to the computer and keep `launchpad-install.log` and `NEMESIS-logs`. |
| Holding TUNE does nothing | Confirm you restarted infotainment after installation. In NEMESIS **Services**, check `paytondev.launchpad/knobd`. Report its status along with your software version. |
| Preconditioning is Unavailable | Read the reason shown in Advanced. Confirm READY and Battery conditioning mode; a firmware mismatch needs investigation, not disabled compatibility checks. |
| It stays Request sent | The car has not confirmed activation. Tap Stop, or let the five-minute pending timeout request cancellation. Do not assume warmth from this message. |
| Battery warm appears | No request was sent. Let the ten-second notice reset, or explicitly choose Start anyway. |
| Battery temp is stale/unavailable | Check Advanced for the response age and retry by leaving and reopening Launchpad. A missing reading is not zero degrees and does not establish that the battery is warm. |

When reporting a problem, include the model/year/market, full software version,
release name, steps taken, and the relevant error. Review logs before posting
them publicly; redact personal identifiers, locations, and device information
unrelated to the fault. Do not upload complete vehicle firmware dumps.

EV6 development, documentation, and vehicle testing: **Abro86**, with development,
documentation, and host-test assistance from **OpenAI Codex**. Launchpad remains
the original work of **paytondevs** and its contributors.
