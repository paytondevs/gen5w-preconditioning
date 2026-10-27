# ioniq5-preconditioning
an app that gives you a manual preconditioning button in your IONIQ 5.

hold the TUNE knob, press **Start**, and Launchpad asks the car to start conditioning the battery. no navigation destination needed.

this app was originally designed to be essentially a replacement for the stock headunit launcher, but it turned out to be kinda useless outside of the preconditioning and wifi settings. those parts are still there if you want them.

## before you start

> [!WARNING]
> This process involves making **unauthorized modifications to the infotainment system of your car.** One wrong move can render it **entirely unusable, leaving you with a very costly repair bill**. Take care to **never delete or modify any files in the car.** If you are unsure about **anything in this document**, don't leave it up to chance! Feel free to DM me on [Twitter](https://x.com/paytondev) or [Reddit](https://reddit.com/u/paytondev) and I'll try to help.

if you aren't comfortable modifying the infotainment system of your car, a [hardware mod](https://electroniqbuttons.com/products/ioniq-5-6-ev6-gv60-manual-preconditioning-button-beta-pre-order) exists that replicates this functionality in an easily reversible and safer manner.

**compatibility:** the app has exact firmware profiles for the **IONIQ 5 NEEV23 US head unit** (`wp_daudioplus_neev23_us`) and the **EV6 CVEV US head unit on software 260507** (`wp_daudioplus_cvev_us`). The EV6 profile was tested on a **US 2023 EV6 Wind RWD**. This does not establish support for every EV6 or firmware version. Launchpad checks the firmware before it enables **Start**; unsupported firmware stays **Unavailable**. Details and field-test limits are in [COMPATIBILITY.md](COMPATIBILITY.md).

The EV6 changes are source contributions pending a maintainer release. Existing release downloads may not include them; check the release notes before installing.

**EV6 owners:** start with the [EV6 installation and everyday-use guide](EV6-QUICK-START.md).

This follow-up contribution adds a battery-temperature display and warm-battery
advisory for the exact EV6 and Ioniq 5 profiles, plus an unconfirmed-request timeout
(five minutes on the EV6, two minutes on the Ioniq 5). See [behavior and validation](TEMPERATURE-AND-TIMEOUT.md).
These source changes are pending review and are not a new downloadable release.

**what you'll need:**
- a computer (Mac, Windows or Linux)
- a USB drive formatted **FAT32 or exFAT**. a small, empty one is easiest.
- a way to [reboot the headunit](https://www.facebook.com/groups/theioniqguy/posts/1355171539173388/) (the pinhole button in the climate control bar)
- about 15 minutes, parked

you **don't** need ADB, Wi-Fi, or a terminal.

## installation

### 1. install NEMESIS

[NEMESIS](https://github.com/xwtk/hmg_nemesis) is what lets us install things on the headunit. you only do this once. follow the **Quick Installation Guidance** in [its README](https://github.com/xwtk/hmg_nemesis#quick-installation-guidance). In short:

1. download [`ota_package.tar`](https://github.com/xwtk/hmg_nemesis/blob/main/ota_package.tar) (you don't need to build it yourself) and copy it to the root of the USB drive. don't extract it.
2. plug the drive into the car. "No Supported Files" is expected.
3. open Engineering Mode → **USB Copy** → **Update DSP** → **Update OTA**.
4. wait for it to install and restart the headunit. an **Error 684** afterwards is expected. **651** means the file didn't extract; check the drive and try again.

to check that it worked: press the **NAV** button three times quickly and **NEMESIS Manager** should open.

once that's done, delete `ota_package.tar` from the drive (or use a different drive).

### 2. put the Launchpad installer on the drive

download **`Launchpad-install-usb.zip`** from the [releases page](../../releases) (the newest one is at the top).

extract it so its **contents** sit at the **root** of the drive, with nothing else from NEMESIS left on it. the drive should look exactly like this:

```text
USB drive/
├── README.txt
├── NEMESIS.data
├── launchpad/
│   ├── common.sh
│   ├── install.sh
│   └── lib.sh
└── packages/
    └── paytondev.launchpad.zip
```

- **Mac:** double-clicking the zip makes a `Launchpad-install-usb` folder (Safari may already have done this for you). open that folder and copy everything **inside** it to the drive, not the folder itself.
- **Windows:** right-click the zip → **Extract All…**, and change the destination to the drive itself (e.g. `E:\`). by default Windows adds a subfolder, which won't work.
- don't unzip `packages/paytondev.launchpad.zip`. NEMESIS needs it as a zip.
- if an old `NEMESIS.data` or `NEMESIS.sh` is on the drive, move it off first. the car only looks at the one `NEMESIS.data` in the root.

<details>
<summary>or use the helper script (if you've cloned this repo)</summary>

it does the same thing, and moves anything NEMESIS-related already on the drive into a `NEMESIS-backup-<date>` folder instead of overwriting it.

```sh
sh tools/usb/write-usb.sh /Volumes/YOUR_USB ~/Downloads
```

on Windows PowerShell:
```powershell
.\tools\usb\write-usb.ps1 E:\ $HOME\Downloads
```

the last argument is the folder you downloaded the zip to.
</details>

### 3. run it in the car

1. park, turn the car on, and wait for the headunit to finish starting.
2. plug the drive into a USB **data** port (the one you'd use for music or CarPlay, not a charge-only port).
3. NEMESIS asks whether to run the item on the USB drive. tap **Yes**. you have 60 seconds. if you miss it, unplug the drive and plug it back in.
4. wait for the result dialog. it usually takes a few seconds and can take up to about 20.

the installer writes **`launchpad-install.log`** to the drive. if the dialog says it failed, that file says why. please include it if you ask me for help.

### 4. reboot and open it

reboot the headunit with the pinhole button and remove the drive. once it's back up, **hold the TUNE knob for about a second** and Launchpad should open.

you only need the USB drive again to update.

## using it

1. hold TUNE to open Launchpad and go to preconditioning.
2. press **Start**. On the supported EV6, a warm reading may show a ten-second advisory with **Start anyway**. Otherwise Launchpad shows **Request sent** until the car confirms activation. A request without confirmed activation is cancelled after five minutes; Start returns only after the car reports Off.
3. the battery management system can still say no, because of temperature, charge, or other conditions it controls.

**a request does not mean the battery heater is on.** Launchpad shows **Active** only when the car itself reports that conditioning is active. The pending timeout (five minutes on the EV6, two on the Ioniq 5) does not cut short an active session. Temperature display and the warm advisory use the same native reading on both supported profiles (EV6 field-tested; Ioniq 5 confirmed from its firmware, not yet field-tested).

press **Stop** to cancel, including while it's still waiting. closing Launchpad does **not** stop a request that's already running.

## updating

exactly the same as installing: put **`Launchpad-install-usb.zip`** from the newest release on the drive, run it, and reboot. when Launchpad is already installed, it updates it in place, and the log says so.

your favorites and settings are kept. the update replaces the app in place instead of removing it first. it also works if you installed an older version with the old manual (ADB) instructions. running the drive twice is harmless.

## uninstalling

1. if preconditioning is running, press **Stop** and wait for it to end.
2. open NEMESIS Manager (press NAV three times) → **Packages** → **Launchpad** → **Uninstall**. over ADB, `nemesis uninstall paytondev.launchpad` does the same.
3. reboot the headunit.

this removes Launchpad, its TUNE-hold service and `/system/app/Launchpad.apk`. if you installed with the old manual instructions and Launchpad isn't in the Packages list, run the USB installer once first, then uninstall. NEMESIS stays installed. if you want that gone too, use **Wipe NEMESIS** in NEMESIS Manager. see the [NEMESIS docs](https://github.com/xwtk/hmg_nemesis/blob/main/03-Install-and-Uninstall.md#5-uninstall-wipe).

## troubleshooting

| what you see | what to do |
| --- | --- |
| nothing happens when you plug in the drive | make sure `NEMESIS.data` is in the **root** of the drive, not in a folder. check the drive is FAT32/exFAT and in a data port. check NEMESIS is installed (press NAV three times; Manager should open). |
| "Invalid package descriptor" | the files were edited or copied incompletely. extract the zip again onto the drive. |
| the dialog says it failed | open `launchpad-install.log` on the drive. for more detail, open NEMESIS Manager → **Logs** → **Copy to USB**, which writes a `NEMESIS-logs` folder to the drive. |
| "not enough space on /system" in the log | nothing was changed. message me with the log. |
| holding TUNE doesn't open Launchpad | reboot once more. in NEMESIS Manager → **Services**, `paytondev.launchpad/knobd` should be running. a different headunit may need a different TUNE profile; message me. |
| preconditioning says **Unavailable** | your firmware doesn't match the checked profile, so Launchpad won't send commands. see [COMPATIBILITY.md](COMPATIBILITY.md). |
| it stays **Request sent** | activation is unconfirmed. press **Stop**, or allow the pending timeout (five minutes on the EV6, two on the Ioniq 5) to request cancellation. The app waits for Off before offering Start again. |

## what this changes on your car

- the TUNE-hold service and a copy of the app live in `/data/nemesis/apps/paytondev.launchpad/`, like any other NEMESIS package. they show up in NEMESIS Manager → **Packages**.
- the **only** file written under `/system` is **`/system/app/Launchpad.apk`**. that's how Android registers the app, because it can't be installed the normal way. no Hyundai files are changed.
- before that write, the installer copies and checks everything in `/data/local/tmp` (cleared on reboot) and checks there's enough space. `/system` is made writable only for that one write and is set back to read-only right after. if the written file doesn't match, the previous version is put back.
- uninstalling removes both.

<details>
<summary><b>advanced: manual install over ADB</b> (you don't need this for the USB install)</summary>

these steps need ADB over Wi-Fi. in my testing, an iPhone hotspot is too modern for the car to connect to. you may have success with older devices or Android phones. you still need NEMESIS installed first (step 1 above).

1. **Enable ADB and Wi-Fi on the car.**

   Download **Enable ADB.zip** and **Enable Wireless Features.zip** from [NEMESIS’s packages folder](https://github.com/xwtk/hmg_nemesis/tree/main/Packages).

   Install them **one at a time**:
   - Extract **Enable ADB.zip** into the root of your FAT32/exFAT USB drive. `NEMESIS.data` should be directly on the drive, not inside another folder.
   - Insert the drive into the car’s USB data port and confirm installation.
   - Reboot the headunit (using the pinhole button in the climate control bar), and remove the USB drive.
   - Replace the installation files on the USB drive with the contents of **Enable Wireless Features.zip**, then repeat.

   These packages enable network ADB and wireless features.

2. **Open the hidden Wi-Fi settings.**

   Enabling wireless features may not add a Wi-Fi button to Hyundai’s normal settings. You can open Android’s existing Wi-Fi screen with a small USB script. If Launchpad is already installed, its **Advanced** screen has a Wi-Fi settings button instead.

   Back up the previous `NEMESIS.data` from your USB drive to your computer. Create these two plain-text files at the root of the drive, using the exact filenames below.

   **`NEMESIS.data`:**
   ```ini
   [nemesis]
   version=1.0
   type=open
   ```

   **`NEMESIS.sh`:**
   ```sh
   #!/system/bin/sh
   export PATH=/sbin:/vendor/bin:/system/sbin:/system/bin:/system/xbin
   export LD_LIBRARY_PATH=/vendor/lib:/system/lib

   netcfg > /storage/usb0/NEMESIS-network.txt 2>&1
   am start -a android.settings.WIFI_SETTINGS
   ```

   Make sure your editor does not add `.txt` to either filename. Save the script with Unix/LF line endings.

   Insert the drive, then select **Run USB NEMESIS.sh** in NEMESIS Manager. Turn Wi-Fi on and connect the car to the same network as your computer.

   Run the script again after connecting. It saves the current network addresses to `NEMESIS-network.txt` on the USB drive.

3. **Install ADB on your computer.**

   Download [Android SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools) for your operating system and extract the ZIP. You do not need to install Android Studio.

   Open a terminal in the extracted `platform-tools` folder.

   On macOS/Linux:
   ```sh
   ./adb version
   ```

   On Windows PowerShell:
   ```powershell
   .\adb.exe version
   ```

   The remaining examples use `adb`. Substitute `./adb` on macOS/Linux or `.\adb.exe` in PowerShell unless you have already added Platform-Tools to your PATH.

4. **Find the car’s IP address and connect.**

   Check your router or hotspot’s connected-device list. Alternatively, read `NEMESIS-network.txt` from the USB drive and find the address of the connected Wi-Fi interface.

   Replace `CAR_IP` below with the car’s actual address:
   ```sh
   adb connect CAR_IP:5555
   adb devices -l
   ```

   The car should appear with the status **`device`**. If an authorization prompt appears on the car, approve it.

   Check that NEMESIS root access works:
   ```sh
   adb -s CAR_IP:5555 shell "nemesis root -c 'id'"
   ```

   You should see `uid=0(root)`.

   If the connection fails:
   - Confirm the car is still awake and connected to Wi-Fi.
   - Check whether its IP address changed.
   - Make sure your network allows devices to communicate with each other. Guest networks and some hotspots isolate clients.
   - If ADB reports `offline`, disconnect and reconnect:
     ```sh
     adb disconnect CAR_IP:5555
     adb connect CAR_IP:5555
     ```
   - Keep in mind `adb pair CAR_IP:5555` will NOT work due to Hyundai not supporting pairing codes.

5. **Install, update or uninstall the package.**

   Download **`paytondev.launchpad.zip`** from the [releases page](../../releases). It's the same NEMESIS package the USB drives install, with the same `/system` rule. Don't extract it.

   ```sh
   adb -s CAR_IP:5555 push "/path/to/paytondev.launchpad.zip" /data/local/tmp/paytondev.launchpad.zip
   adb -s CAR_IP:5555 shell "nemesis install /data/local/tmp/paytondev.launchpad.zip"
   ```

   To update, use `nemesis install --update /data/local/tmp/paytondev.launchpad.zip` instead. To uninstall, run `nemesis uninstall paytondev.launchpad`. Then check:
   ```sh
   adb -s CAR_IP:5555 shell "nemesis pkg-list"
   adb -s CAR_IP:5555 shell "nemesis service status paytondev.launchpad/knobd"
   adb -s CAR_IP:5555 shell "ls -l /system/app/Launchpad.apk"
   ```

   `nemesis install` prints the install hook's output. NEMESIS's own log is at `/data/nemesis/nemesis.log`. Do not use `adb install`, and do not disable package-verification settings to work around an error.

6. **Reboot and open Launchpad.**

   ```sh
   adb -s CAR_IP:5555 shell "nemesis root -c 'reboot'"
   ```

   Wait for the head unit to finish starting, then hold TUNE for roughly one second. If Launchpad doesn't open, try opening it directly:
   ```sh
   adb -s CAR_IP:5555 shell "am start -n com.paytondev.launchpad/.MainActivity"
   ```

   If that works, check the TUNE service:
   ```sh
   adb -s CAR_IP:5555 shell "nemesis service status paytondev.launchpad/knobd"
   ```

</details>

## building from source

see [BUILDING.md](BUILDING.md). releases are built with `tools/build_apk.sh` and `tools/build_usb_bundles.py`.

## credits

- EV6 development, documentation, and vehicle testing: [Abro86](https://github.com/Abro86), with development, documentation, and host-test assistance from OpenAI Codex.
- [https://github.com/xwtk/hmg_nemesis](https://github.com/xwtk/hmg_nemesis) - the NEMESIS exploit that made this all possible
- [https://github.com/tylerharvey/Ioniq5_CAN](https://github.com/tylerharvey/Ioniq5_CAN) - reverse engineered the CAN messages used to start preconditioning
- [https://github.com/Helloyunho/gen5w-utils](https://github.com/Helloyunho/gen5w-utils) - other scripts and tools for reverse engineering Gen5W
