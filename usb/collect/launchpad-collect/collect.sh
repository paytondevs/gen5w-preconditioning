#!/system/bin/sh
# NEMESIS type=execute: copy what is needed to map the battery-temperature
# Binder contract (framework odex/jar files, properties, service list) onto this
# USB drive. Read-only on the car: writes only under the drive, never remounts
# /system, and makes no vehicle Binder calls (`service list` only asks each
# service for its descriptor). Toolbox only (no grep/awk/busybox on KitKat).
# COLLECT_PATH/COLLECT_FRAMEWORK exist solely for host tests.
export PATH=${COLLECT_PATH:-/sbin:/vendor/bin:/system/sbin:/system/bin:/system/xbin}
FRAMEWORK=${COLLECT_FRAMEWORK:-/system/framework}
HERE=${0%/*}
USBROOT=${HERE%/*}
OUT=$USBROOT/launchpad-collect-$(date '+%Y%m%d-%H%M%S')
LOG=$USBROOT/launchpad-collect.log

say() {
    LINE="$(date '+%Y-%m-%d %H:%M:%S') $*"
    echo "$LINE"
    echo "$LINE" >> "$LOG"
}

say "==== Launchpad collect (USB: $USBROOT, output: $OUT)"
mkdir "$OUT" && mkdir "$OUT/framework" || { say "FAIL: cannot create $OUT"; exit 1; }
FAIL=0
getprop > "$OUT/props.txt" 2>&1 || { say "FAIL: getprop"; FAIL=1; }
service list > "$OUT/services.txt" 2>&1 || { say "FAIL: service list"; FAIL=1; }
ls -l "$FRAMEWORK" > "$OUT/framework-ls.txt" 2>&1 || { say "FAIL: ls $FRAMEWORK"; FAIL=1; }
echo "$BOOTCLASSPATH" > "$OUT/bootclasspath.txt"
cat /proc/version > "$OUT/kernel.txt" 2>&1

# The whole framework directory: baksmali needs the boot classpath to deodex
# automotive.odex and automotive-service.odex.
COPIED=0
for NAME in $(ls "$FRAMEWORK"); do
    SRC=$FRAMEWORK/$NAME
    [ -f "$SRC" ] || continue
    case "$NAME" in *.odex|*.jar|*.apk) ;; *) continue ;; esac
    # KitKat toolbox cmp has no -s option; silence it instead.
    if cat "$SRC" > "$OUT/framework/$NAME" && cmp "$SRC" "$OUT/framework/$NAME" >/dev/null 2>&1; then
        COPIED=$((COPIED + 1))
    else
        say "FAIL: copy $SRC"; FAIL=1
    fi
done
for NAME in automotive.odex automotive-service.odex; do
    [ -f "$OUT/framework/$NAME" ] || { say "FAIL: $NAME not collected"; FAIL=1; }
done
sync

if [ "$FAIL" = 0 ]; then
    say "SUCCESS: $COPIED framework files copied. Nothing on the car was changed; remove the USB drive."
else
    say "FAILED after copying $COPIED framework files. Keep $LOG and the $OUT folder."
fi
say "----"
exit "$FAIL"
