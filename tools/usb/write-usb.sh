#!/bin/sh
# Write a Launchpad USB bundle onto a FAT32/exFAT drive. Runs on your computer;
# does not connect to the car.
#   tools/usb/write-usb.sh [--manual] /Volumes/USB [bundle-dir]
# bundle-dir holds Launchpad-install-usb.zip, or Launchpad-manual-install-usb.zip with
# --manual for NEMESIS older than 1.16 (default: ./dist, then the current dir).
# The same drive installs or updates Launchpad.
set -eu
usage() { echo "usage: $0 [--manual] <usb-drive-path> [bundle-dir]" >&2; exit 2; }
NAME=Launchpad-install-usb.zip
if [ "${1:-}" = --manual ]; then
    NAME=Launchpad-manual-install-usb.zip
    shift
fi
[ $# -ge 1 ] || usage
DRIVE=$1
ZIP=
find_zip() { [ -z "$ZIP" ] && [ -f "$1/$NAME" ] && ZIP=$1/$NAME; return 0; }
[ $# -ge 2 ] && find_zip "$2"
find_zip "$(dirname "$0")/../../dist"
find_zip dist
find_zip .
[ -n "$ZIP" ] || { echo "$NAME not found; pass the folder you downloaded it to." >&2; exit 1; }
[ -d "$DRIVE" ] && [ -w "$DRIVE" ] || { echo "$DRIVE is not a writable directory (is the drive mounted?)" >&2; exit 1; }
command -v unzip >/dev/null || { echo "unzip is required" >&2; exit 1; }

# Move (never delete) anything from a previous NEMESIS job out of the drive root.
BACKUP=$DRIVE/NEMESIS-backup-$(date +%Y%m%d-%H%M%S)
for ITEM in NEMESIS.data NEMESIS.sh ota_package.tar README.txt launchpad packages; do
    if [ -e "$DRIVE/$ITEM" ]; then
        mkdir -p "$BACKUP"
        mv "$DRIVE/$ITEM" "$BACKUP/"
        echo "moved $ITEM -> $BACKUP/"
    fi
done

unzip -q "$ZIP" -d "$DRIVE"
sync
echo "Wrote $(basename "$ZIP") to $DRIVE:"
(cd "$DRIVE" && ls -1d README.txt NEMESIS.data launchpad packages 2>/dev/null) || true
cat <<EOF

Next:
  1. Eject the drive, insert it into the car's USB data port (car on, parked).
  2. Tap Yes on the NEMESIS install prompt (and on the update prompt, if
     Launchpad is already installed), then wait for the result dialog.
  3. Reboot the head unit (pinhole button), then remove the drive.
  If it fails: NEMESIS Manager > Logs > Copy to USB writes NEMESIS-logs to the drive.
  The manual drive also writes launchpad-install.log.
EOF
