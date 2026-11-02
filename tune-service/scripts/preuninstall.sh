#!/system/bin/sh
# NEMESIS preuninstall (root; services already stopped). Runs before NEMESIS's
# pm uninstall, which deletes the app and its data. Also runs on the OLD package
# during `nemesis install --update`, after the new package has been extracted to
# $STAGE; NEMESIS then uninstalls the old APK and installs the new one.
. /data/nemesis/apps/paytondev.launchpad/scripts/common.sh

log "preuninstall: removing daemon dalvik cache"
rm -f "$DEXCACHE"

if [ -f "$STAGE/NEMESIS.data" ]; then
    # Update: save the settings for the new version's postinstall to restore.
    log "preuninstall: update in progress; saving settings"
    force_stop_app
    backup_prefs || log "preuninstall: WARN: settings not saved; the new version starts with defaults"
    exit 0
fi

force_stop_app
# Only if a 1.x copy is somehow still there (2.x never writes /system).
remove_legacy_sysapk || exit 1
remove_backup
rm -rf "$WORK"
log "preuninstall: done; NEMESIS removes the app next"
exit 0
