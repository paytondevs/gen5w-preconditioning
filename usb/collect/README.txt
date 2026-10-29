Launchpad firmware collect (read-only)

Copies the head unit's /system/framework odex/jar files, its properties and the
service list onto this USB drive, so the battery-temperature Binder contract can
be checked offline. It changes nothing on the car.

1. Use a FAT32/exFAT drive with about 200 MB free. Copy NEMESIS.data, README.txt
   and the launchpad-collect folder to the root of the drive. Move any other
   NEMESIS.data (for example the Launchpad installer's) off the drive first.
2. Plug the drive into the car with the head unit running. NEMESIS runs
   launchpad-collect/collect.sh; copying takes a few minutes.
3. When launchpad-collect.log on the drive ends in SUCCESS, remove the drive.
   The files are in the launchpad-collect-<date> folder.
