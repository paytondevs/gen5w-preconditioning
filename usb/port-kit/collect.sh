#!/system/bin/sh
# NEMESIS type=execute: the read-only vehicle porting kit. Writes
# launchpad-port-report.txt to the USB drive for a vehicle port request.
# It installs nothing, never remounts /system and never sends a vehicle command.
# The bundled Launchpad.apk is not installed: a copy in $PROBE_WORK is only the
# dex container for PortProbe, and is removed again with its dalvik-cache entry.
SUCCESS_HINT="Report saved to launchpad-port-report.txt on the USB drive. No reboot needed."
. "${0%/*}/lib.sh"
KIT_VERSION=@VERSION@
REPORT=$USBROOT/launchpad-port-report.txt
FRAMEWORK_DIR=/system/framework
INPUT_DEVICES=/proc/bus/input/devices
PROBE_APK=$LIBDIR/Launchpad.apk
PROBE_WORK=/data/local/tmp/launchpad-port
PROBE_DEX=$PROBE_WORK/Launchpad.apk
PROBE_CACHE=/data/dalvik-cache/data@local@tmp@launchpad-port@Launchpad.apk@classes.dex
CAPTURE_SECONDS=30

section() { echo; echo "[$1]"; }

cleanup_probe() {
    rm -rf "$PROBE_WORK"
    rm -f "$PROBE_CACHE"
}

[ -f "$PROBE_APK" ] || { say "FAIL: missing $PROBE_APK"; finish 1; }
[ -f "$REPORT" ] && mv "$REPORT" "$REPORT.old" && say "previous report kept as $REPORT.old"

{
    echo "# Launchpad vehicle porting kit report"
    echo "# Read-only: nothing on the car was installed or changed."
    echo "# Attach this file to a \"Vehicle port request\" issue on GitHub."
    section kit
    echo "kit_version=$KIT_VERSION"
    echo "date=$(date '+%Y-%m-%d %H:%M:%S')"
    section identity
    echo "sdk=$(getprop ro.build.version.sdk)"
    echo "device=$(getprop ro.product.device)"
    echo "software=$(getprop ro.build.product)"
    # Build and product properties only. Anything that could identify the car
    # or its owner is left out.
    getprop | while read LINE; do
        KEY=${LINE#\[}
        KEY=${KEY%%\]*}
        case "$KEY" in
            *serial*|*Serial*|*SERIAL*|*imei*|*IMEI*|*mac*|*MAC*|*vin*|*Vin*|*VIN*) ;;
            ro.build.*|ro.product.*|ro.board.*|ro.hardware|ro.revision) echo "$LINE" ;;
        esac
    done
    section framework_files
    ls -l "$FRAMEWORK_DIR" 2>&1
    if [ -r "$BOOT_ENV" ]; then
        while read CMD NAME VALUE; do
            [ "$CMD" = export ] && [ "$NAME" = BOOTCLASSPATH ] && echo "bootclasspath=$VALUE"
        done < "$BOOT_ENV"
    fi
    section services
    service list 2>&1
    section input
    while read LINE; do
        case "$LINE" in "N: "*|"H: "*) echo "$LINE" ;; "") echo ;; esac
    done < "$INPUT_DEVICES"
    section nemesis
    "$NEMESIS" pkg-list 2>&1
} >> "$REPORT" 2>&1
say "wrote device, firmware, service and input details to $REPORT"

N=0
while ! android_up && [ $N -lt 60 ]; do
    sleep 1
    N=$((N + 1))
done
android_up || { say "FAIL: Android did not finish starting; the report has the shell sections only"; finish 1; }

# Stage the dex container in tmp and verify it, like postinstall does.
cleanup_probe
if ! mkdir -p "$PROBE_WORK" || ! cat "$PROBE_APK" > "$PROBE_DEX" || ! files_equal "$PROBE_APK" "$PROBE_DEX"; then
    cleanup_probe
    say "FAIL: could not copy $PROBE_APK to $PROBE_WORK"
    finish 1
fi
chmod 0644 "$PROBE_DEX"

load_boot_env
export CLASSPATH="$PROBE_DEX"
say "Now hold the right-hand TUNE knob for about 2 seconds, let go, and repeat until the result appears (about $CAPTURE_SECONDS seconds)."
run app_process /system/bin --nice-name=launchpad-port-probe com.paytondev.launchpad.PortProbe \
    "$REPORT" "$CAPTURE_SECONDS"
RC=$?
cleanup_probe
[ $RC = 0 ] || say "the report is incomplete; please attach it and this log anyway"
finish $RC
