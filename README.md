# gen5w-preconditioning
an app that gives you a manual preconditioning button in your IONIQ 5 or EV6.

hold the TUNE knob, press **Start**, and Launchpad asks the car to start conditioning the battery. no navigation destination needed.

this app was originally designed to be essentially a replacement for the stock headunit launcher, but it turned out to be kinda useless outside of the preconditioning and wifi settings. those parts are still there if you want them.

## will it work on my car?

**check the [compatibility chart](COMPATIBILITY.md#compatibility-chart) first.** it lists every car and head-unit software version Launchpad supports right now. what matters is the head unit's software version, not the model year.

your car isn't listed? the chart also explains how to run the **vehicle porting kit**, a USB drive that safely reads your car's details so support can be added.

**EV6 owners:** start with the [EV6 installation and everyday-use guide](EV6-QUICK-START.md).

## before you start

> [!WARNING]
> This process involves making **unauthorized modifications to the infotainment system of your car.** One wrong move can render it **entirely unusable, leaving you with a very costly repair bill**. Take care to **never delete or modify any files in the car.** If you are unsure about **anything in this document**, don't leave it up to chance! Feel free to DM me on [Twitter](https://x.com/paytondev) or [Reddit](https://reddit.com/u/paytondev) and I'll try to help.

if you aren't comfortable modifying the infotainment system of your car, a [hardware mod](https://electroniqbuttons.com/products/ioniq-5-6-ev6-gv60-manual-preconditioning-button-beta-pre-order) exists that replicates this functionality in an easily reversible and safer manner.

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
2. press **Start**. Launchpad shows **Request sent** while it waits for the car to say yes. on the EV6, if the battery is already warm, it asks you first and offers **Start anyway**.
3. when the car confirms, it shows **Active**. that's the only sign the battery is actually being conditioned. the car can still say no, for example if the battery is warm enough or the charge is too low.

if the car hasn't confirmed after five minutes, Launchpad cancels the request for you. an **Active** session is never cut short.

press **Stop** to cancel, even while it's still waiting. closing Launchpad does **not** stop a request that's already running.

## updating

exactly the same as installing: put **`Launchpad-install-usb.zip`** from the newest release on the drive, run it, and reboot. if Launchpad is already installed, it updates it in place, and the log says so.

your favorites and settings are kept. it also works if you installed an older version over ADB. running the drive twice is harmless.

## uninstalling

1. if preconditioning is running, press **Stop** and wait for it to end.
2. open NEMESIS Manager (press NAV three times) → **Packages** → **Launchpad** → **Uninstall**.
3. reboot the headunit.

this removes Launchpad and its TUNE shortcut. if you installed an older version over ADB and Launchpad isn't in the Packages list, run the USB installer once first, then uninstall. NEMESIS stays installed. if you want that gone too, use **Wipe NEMESIS** in NEMESIS Manager. see the [NEMESIS docs](https://github.com/xwtk/hmg_nemesis/blob/main/03-Install-and-Uninstall.md#5-uninstall-wipe).

## troubleshooting

| what you see | what to do |
| --- | --- |
| nothing happens when you plug in the drive | make sure `NEMESIS.data` is in the **root** of the drive, not in a folder. check the drive is FAT32/exFAT and in a data port. check NEMESIS is installed (press NAV three times; Manager should open). |
| "Invalid package descriptor" | the files were edited or copied incompletely. extract the zip again onto the drive. |
| the dialog says it failed | open `launchpad-install.log` on the drive. for more detail, open NEMESIS Manager → **Logs** → **Copy to USB**, which writes a `NEMESIS-logs` folder to the drive. |
| "not enough space on /system" in the log | nothing was changed. message me with the log. |
| holding TUNE doesn't open Launchpad | reboot once more. in NEMESIS Manager → **Services**, `paytondev.launchpad/knobd` should be running. a different headunit may need a different TUNE profile; message me. |
| preconditioning says **Unavailable** | your car or its software version isn't supported yet, so Launchpad won't send anything to the car. check the [compatibility chart](COMPATIBILITY.md#compatibility-chart). if your car isn't listed, run the [vehicle porting kit](COMPATIBILITY.md#car-not-listed-run-the-vehicle-porting-kit). |
| it stays **Request sent** | the car hasn't confirmed yet. press **Stop**, or wait: Launchpad cancels it after five minutes. **Start** comes back once the car reports Off. |

## what this changes on your car

- Launchpad is installed as a normal NEMESIS package, so it shows up in NEMESIS Manager → **Packages**, and uninstalling it from there removes everything.
- it adds **one** file to the car's system area, `/system/app/Launchpad.apk`, because that's the only way Android will show the app. no Hyundai or Kia files are changed.
- the installer checks everything first and puts the previous version back if anything goes wrong.

<details>
<summary>more detail</summary>

- the TUNE-hold service and a copy of the app live in `/data/nemesis/apps/paytondev.launchpad/`.
- before writing `/system/app/Launchpad.apk`, the installer copies and checks everything in `/data/local/tmp` (cleared on reboot) and checks there's enough space. `/system` is made writable only for that one write and is set back to read-only right after. if the written file doesn't match, the previous version is put back.
- uninstalling removes both.
</details>

already use ADB with your car? there are [manual ADB install steps](ADVANCED-ADB-INSTALL.md), but you don't need them.

## building from source

see [BUILDING.md](BUILDING.md). releases are built with `tools/build_apk.sh` and `tools/build_usb_bundles.py`.

want to fix something or add your car? see [CONTRIBUTING.md](CONTRIBUTING.md).

## credits

- EV6 development, documentation, and vehicle testing: [Abro86](https://github.com/Abro86), with development, documentation, and host-test assistance from OpenAI Codex.
- [https://github.com/xwtk/hmg_nemesis](https://github.com/xwtk/hmg_nemesis) - the NEMESIS exploit that made this all possible
- [https://github.com/tylerharvey/Ioniq5_CAN](https://github.com/tylerharvey/Ioniq5_CAN) - reverse engineered the CAN messages used to start preconditioning
- [https://github.com/Helloyunho/gen5w-utils](https://github.com/Helloyunho/gen5w-utils) - other scripts and tools for reverse engineering Gen5W
