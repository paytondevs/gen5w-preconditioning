# Shared by the USB scripts. NEMESIS runs them as
# `/system/bin/sh <USB>/launchpad/<action>.sh` (root, cwd = USB root).
# common.sh is a copy of tune-service/scripts/common.sh added by the bundle builder.
LIBDIR=${0%/*}
. "$LIBDIR/common.sh"
USBROOT=${LIBDIR%/*}
PKG_ID=paytondev.launchpad
PKGZIP=$USBROOT/packages/$PKG_ID.zip
ACTION=${0##*/}
ACTION=${ACTION%.sh}
LOG=$USBROOT/launchpad-$ACTION.log
# A script may set this before sourcing this file.
: "${SUCCESS_HINT:=Reboot the head unit (pinhole button) to finish, then remove the USB drive.}"

say() {
    LINE="$(date '+%Y-%m-%d %H:%M:%S') $*"
    echo "$LINE"
    echo "$LINE" >> "$LOG"
}

# Run a command, appending its output (including NEMESIS hook output) to the log.
run() {
    say "\$ $*"
    "$@" >> "$LOG" 2>&1
    RC=$?
    say "  -> exit $RC"
    return $RC
}

pkg_installed() {
    "$NEMESIS" pkg-list 2>/dev/null | {
        while IFS='|' read ID REST; do
            [ "$ID" = "$PKG_ID" ] && exit 0
        done
        exit 1
    }
}

knobd_state() {
    "$NEMESIS" service status "$PKG_ID/knobd" 2>/dev/null | {
        IFS='|' read QNAME MODE STATE PID
        echo "$STATE"
    }
}

# Package registered, /system/app copy matches the package's APK, knobd running.
verify_installed() {
    OK=0
    if pkg_installed; then say "ok: package $PKG_ID registered"
    else say "FAIL: package $PKG_ID not registered"; OK=1; fi
    if [ -f "$SYSAPK" ] && files_equal "$DEX" "$SYSAPK"; then say "ok: $SYSAPK matches the package APK"
    else say "FAIL: $SYSAPK missing or does not match $DEX"; OK=1; fi
    N=0
    STATE=$(knobd_state)
    while [ "$STATE" != running ] && [ $N -lt 10 ]; do
        sleep 1
        N=$((N + 1))
        STATE=$(knobd_state)
    done
    if [ "$STATE" = running ]; then say "ok: service $PKG_ID/knobd running"
    else say "FAIL: service $PKG_ID/knobd is '${STATE:-unknown}'"; OK=1; fi
    return $OK
}

finish() {
    if [ "$1" = 0 ]; then
        say "SUCCESS. $SUCCESS_HINT"
    else
        say "FAILED. Keep this log ($LOG); NEMESIS's own log is /data/nemesis/nemesis.log (Manager > Logs > Copy to USB)."
    fi
    say "----"
    exit "$1"
}

say "==== Launchpad $ACTION (USB: $USBROOT, Android up: $(getprop sys.boot_completed))"
