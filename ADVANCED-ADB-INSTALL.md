# advanced: manual install over ADB

you don't need this. the [USB install in the README](README.md#installation) does the same thing without ADB, Wi-Fi or a terminal. this is for people who already use ADB with their head unit.

these steps need ADB over Wi-Fi. in my testing, an iPhone hotspot is too modern for the car to connect to. you may have success with older devices or Android phones. you still need NEMESIS installed first ([step 1 of the README](README.md#1-install-nemesis)).

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
