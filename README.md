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

download **`Launchpad-install-usb.zip`** from the [releases page](../../releases) (the newest one is at the top). it needs NEMESIS 1.16 or newer.

on an older NEMESIS, download **`Launchpad-manual-install-usb.zip`** instead. it works the same way, but its drive also has a `launchpad/` folder with our install script, NEMESIS asks to *run* it instead of *install* it (one prompt, also for updates), and it writes `launchpad-install.log` to the drive.

don't use `paytondev.launchpad.zip` for this: that's the package itself, and extracting it onto the drive gives "Invalid package descriptor".

extract it so its **contents** sit at the **root** of the drive, with nothing else from NEMESIS left on it. the drive should look exactly like this:

```text
USB drive/
├── README.txt
├── NEMESIS.data
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
3. NEMESIS asks whether to install the package on the USB drive. tap **Yes**. if Launchpad is already installed, it then asks whether to update it; tap **Yes** again. you have 60 seconds for each. if you miss one, unplug the drive and plug it back in.
4. wait for the result dialog. it usually takes a few seconds and can take up to about 20.

if the dialog says it failed, open NEMESIS Manager → **Logs** → **Copy to USB**, which writes a `NEMESIS-logs` folder to the drive. please include it if you ask me for help.

### 4. reboot and open it

reboot the headunit with the pinhole button and remove the drive. once it's back up, **hold the TUNE knob for about a second** and Launchpad should open.

you can also open it from NEMESIS Manager: it's on Manager's home screen as **Precondition**, after Manager's own tiles.

you only need the USB drive again to update.

## using it

1. hold TUNE to open Launchpad and go to preconditioning.
2. press **Start**. Launchpad shows **Request sent** while it waits for the car to say yes. if the battery is already warm, it asks you first and offers **Start anyway**.
3. when the car confirms, it shows **Active**. that's the only sign the battery is actually being conditioned. the car can still say no, for example if the battery is warm enough or the charge is too low.

if the car hasn't confirmed after five minutes on the EV6 (two on the IONIQ 5), Launchpad cancels the request for you. an **Active** session is never cut short.

press **Stop** to cancel, even while it's still waiting. closing Launchpad does **not** stop a request that's already running.

### scheduling it

the round **clock** button next to **Start** lets you pick a time instead. there are two modes:

- **Start by:** conditioning should be running by that time. the car takes about a minute to react, so Launchpad sends the request 70 seconds early: pick 1:30 and it goes out at 1:28:50.
- **Arrive by:** the battery should be warm when you get there, for example at a fast charger. Launchpad works out when to start from the battery temperature and the car's outside temperature sensor, and keeps adjusting the plan while it waits. the car stops a session after about 30 minutes, so that's the most it plans for. the heating rates it uses are estimates from owner reports, not measurements, so treat the start time as a best guess.

once it's scheduled, the card shows a countdown to the start, and for **Arrive by** the battery and outside temperatures it's planning with. press **Cancel schedule** to drop it, or the clock button to change it. pressing **Start** yourself also cancels the schedule.

you can close Launchpad; the schedule keeps running in the background and survives a head unit restart. a few things to know:

- the car has to be on and READY when the request goes out, same as pressing Start. if it isn't, Launchpad keeps trying until 5 minutes after the time you picked, then gives up and logs it as **missed**. it never sends late.
- if the battery is already warm at that point, nothing is sent and the log says so.
- the outcome is also posted as a notification, but the head unit may not show those.

**dark mode:** go to **Advanced → Appearance**. **Follow car** (the default) matches the head unit's day/night display, or pick **Light** or **Dark** to keep one. Follow car uses the head unit's Day/Night display setting, including Auto. if it doesn't switch on your car, the **Appearance signals** panel at the bottom of Advanced shows what the head unit reports.

## updating

exactly the same as installing: put **`Launchpad-install-usb.zip`** (or **`Launchpad-manual-install-usb.zip`** on NEMESIS older than 1.16) from the newest release on the drive, run it, and reboot. if Launchpad is already installed, it updates it in place.

your favorites and settings are kept. running the drive twice is harmless.

### coming from 1.x

nothing extra to do: run the drive the same way. if Launchpad 1.x is on the car, the drive moves your favorites and settings over and removes the old copy in the same run. if you already removed 1.x with NEMESIS Manager, it still installs, and your settings come along as long as the headunit hasn't restarted since. this also works if you installed 1.x over ADB.

## uninstalling

1. if preconditioning is running, press **Stop** and wait for it to end.
2. open NEMESIS Manager (press NAV three times) → **Packages** → **Precondition** → **Uninstall**.
3. reboot the headunit.

this removes Launchpad and its TUNE shortcut. if you still have Launchpad 1.x and it isn't in the Packages list (for example, you installed it over ADB), install 2.0 first, which removes 1.x, then uninstall. NEMESIS stays installed. if you want that gone too, use **Wipe NEMESIS** in NEMESIS Manager. see the [NEMESIS docs](https://github.com/xwtk/hmg_nemesis/blob/main/03-Install-and-Uninstall.md#5-uninstall-wipe).

## troubleshooting

| what you see | what to do |
| --- | --- |
| nothing happens when you plug in the drive | make sure `NEMESIS.data` is in the **root** of the drive, not in a folder. check the drive is FAT32/exFAT and in a data port. check NEMESIS is installed (press NAV three times; Manager should open). |
| "Invalid package descriptor" | the drive has the wrong files, or they were copied incompletely. check you extracted `Launchpad-install-usb.zip`, not `paytondev.launchpad.zip`, and that the drive looks like the layout above. extract it again onto the drive. |
| the dialog says it failed | open NEMESIS Manager → **Logs** → **Copy to USB**, which writes a `NEMESIS-logs` folder to the drive. message me with it, plus `launchpad-install.log` if you used the manual drive. |
| Precondition isn't on NEMESIS Manager's home screen | that screen needs NEMESIS 1.16 or newer. close Manager and open it again. if it's still missing, check that **Packages** lists Precondition, and run the install drive again. |
| holding TUNE doesn't open Launchpad | reboot once more. in NEMESIS Manager → **Services**, `paytondev.launchpad/knobd` should be running. a different headunit may need a different TUNE profile; message me. |
| preconditioning says **Unavailable** | your car or its software version isn't supported yet, so Launchpad won't send anything to the car. check the [compatibility chart](COMPATIBILITY.md#compatibility-chart). if your car isn't listed, run the [vehicle porting kit](COMPATIBILITY.md#car-not-listed-run-the-vehicle-porting-kit). |
| it stays **Request sent** | the car hasn't confirmed yet. press **Stop**, or wait: Launchpad cancels it after five minutes on the EV6 (two on the IONIQ 5). **Start** comes back once the car reports Off. |

## what this changes on your car

- Launchpad is installed as a normal NEMESIS package, so it shows up in NEMESIS Manager → **Packages** as **Precondition**, and uninstalling it from there removes everything.
- NEMESIS installs the app the normal Android way, like an app you'd install yourself, so it also shows up on NEMESIS Manager's home screen. nothing is written to the car's system area, and no Hyundai or Kia files are changed.
- updates keep your settings: they're copied to NEMESIS's backup folder while the old version is swapped for the new one, then put back.
- if an update fails partway, run the drive again. your settings stay in the backup folder until an install puts them back.

<details>
<summary>more detail</summary>

- the TUNE-hold service and a copy of the app live in `/data/nemesis/apps/paytondev.launchpad/`.
- NEMESIS installs the app from there with `pm install` and removes it with `pm uninstall`. each release is signed with its own key, which works because NEMESIS removes the old version before installing the new one.
- during an update, the settings files are copied to `/data/nemesis/backup/paytondev.launchpad/` and put back into the new install. uninstalling deletes that folder along with everything else.
- Launchpad 1.x copied itself to `/system/app/Launchpad.apk`. the only thing 2.0 does in `/system` is remove that file, once, when it finds 1.x. `/system` is made writable only for that and is set back to read-only right after.
- 2.0's Android package is `com.paytondev.precondition`; 1.x was `com.paytondev.launchpad`. Android keeps the old name reserved until the headunit restarts, even once 1.x is removed, so the new name lets 2.0 install right away.
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
