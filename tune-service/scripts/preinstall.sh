#!/system/bin/sh
# NEMESIS preinstall (root). Runs from NEMESIS's staging copy of this package,
# before NEMESIS copies it into place and installs app/Launchpad.apk with pm
# install. Exiting non-zero stops NEMESIS before it installs or records anything.
#
# A car without Launchpad 1.x has nothing to do here. If 1.x is there (its copy in
# /system/app, or its data left after an uninstall the head unit has not restarted
# since), its settings are saved for postinstall and the /system copy is removed.
# 1.x is a different Android package (com.paytondev.launchpad), so it never blocks
# this install and no reboot is needed.
. "${0%/*}/common.sh"

if [ -f "$SYSAPK" ] || [ -d "$LEGACY_DATA" ]; then
    log "preinstall: Launchpad 1.x found; moving its settings over"
    if prefs_backed_up; then
        # An update's own backup (or one not restored yet) is newer: keep it.
        log "preinstall: a settings backup is already waiting; keeping it"
    else
        force_stop_app "$LEGACY_PACKAGE"
        backup_prefs "$LEGACY_DATA" || log "preinstall: WARN: could not save the 1.x settings"
    fi
fi

if ! remove_legacy_sysapk; then
    log "preinstall: FAILED: could not remove Launchpad 1.x from $SYSAPK; nothing installed"
    exit 1
fi
exit 0
