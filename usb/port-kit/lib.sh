# Logging for the porting kit. NEMESIS runs it as
# `/system/bin/sh <USB>/launchpad-port/collect.sh` (root, cwd = USB root).
# common.sh is a copy of tune-service/scripts/common.sh added by the bundle builder.
LIBDIR=${0%/*}
. "$LIBDIR/common.sh"
USBROOT=${LIBDIR%/*}
ACTION=${0##*/}
ACTION=${ACTION%.sh}
LOG=$USBROOT/launchpad-$ACTION.log
# A script may set this before sourcing this file.
: "${SUCCESS_HINT:=Remove the USB drive.}"

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
