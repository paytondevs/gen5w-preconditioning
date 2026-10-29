Launchpad - vehicle porting kit USB

This drive reads the details needed to add your car to Launchpad and saves
them to launchpad-port-report.txt on this drive. It is read-only: it does not
install Launchpad or anything else, it does not change any files on the car,
and it does not start or stop preconditioning.

BEFORE YOU BEGIN
NEMESIS must already be installed (see the Launchpad README). Engineering Mode
is not needed for this step.

ON THE COMPUTER
1. Use an empty FAT32 or exFAT USB, or back it up first.
2. Move old NEMESIS.data, NEMESIS.sh, ota_package.tar, launchpad,
   launchpad-port and packages items off the drive so another installer
   cannot run by accident.
3. Extract the outer ZIP. Copy its CONTENTS to the drive, not the outer folder.
   NEMESIS.data and this README.txt must be visible immediately on opening it.
4. Safely eject the drive.

IN THE CAR
1. Park, turn the car on, and wait for the screen to finish starting.
2. Insert the drive into the USB DATA port used for wired Android Auto/CarPlay.
3. Accept the NEMESIS USB prompt by tapping Yes. If it expires, reinsert the
   drive.
4. Optional, but it helps: right after tapping Yes, hold the right-hand knob
   that changes radio stations (TUNE) for about 2 seconds, let go, and keep
   repeating until the result appears. This shows which button Launchpad can
   use to open itself. If your car has no such knob, skip this step.
5. Wait for the result. It takes about 30 seconds. No reboot is needed.

SEND IT IN
1. Open launchpad-port-report.txt on a computer. It is plain text, so you can
   read everything it contains. It has no VIN, serial number or location.
2. Open a GitHub issue using the "Vehicle port request" form:
   https://github.com/paytondevs/gen5w-preconditioning/issues/new?template=vehicle-port.yml
3. Fill in your vehicle (model, model year, trim and market, e.g. US 2024
   IONIQ 5 Limited AWD) and the head unit's software version from the car's
   settings, then attach launchpad-port-report.txt.
4. If the result said it failed, attach launchpad-collect.log too.

A report does not mean your car will be supported. It gives a developer what
they need to check whether a port is possible.

Launchpad: https://github.com/paytondevs/gen5w-preconditioning
