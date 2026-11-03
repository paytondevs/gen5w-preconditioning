# Shared by the hooks and the service. Toolbox only (no grep/awk/busybox on KitKat).
export PATH=/sbin:/vendor/bin:/system/sbin:/system/bin:/system/xbin
export LD_LIBRARY_PATH=/vendor/lib:/system/lib
umask 022
HERE=/data/nemesis/apps/paytondev.launchpad
NEMESIS=/system/xbin/nemesis
# The signed Launchpad APK. NEMESIS installs it as a normal Android app
# ([app.0] kind=apk: pm install on install, pm uninstall on removal), and the knob
# daemon uses this same file as its dex container (CLASSPATH under app_process).
DEX=$HERE/app/Launchpad.apk
# dalvik-cache entry app_process may create for that dex (named after its path).
DEXCACHE=/data/dalvik-cache/data@nemesis@apps@paytondev.launchpad@app@Launchpad.apk@classes.dex
# The Android package. Its classes stay in the Java package com.paytondev.launchpad,
# so components need full class names.
APP_PACKAGE=com.paytondev.precondition
APP_DATA=/data/data/$APP_PACKAGE
# Started once after install: it re-arms a restored schedule, and starting any
# component takes a freshly installed package out of Android's "stopped" state so
# BootReceiver gets BOOT_COMPLETED. Nothing may force-stop the app after that.
SERVICE_COMPONENT=$APP_PACKAGE/com.paytondev.launchpad.PreconditioningService
# Launchpad 1.x: a different Android package, copied into /system/app. 2.x never
# writes /system: this file is only ever removed, once, when moving 1.x over.
# Android keeps the 1.x package (and its data) until the next boot even after the
# file is gone, which is why 2.x has its own package name: it installs right away.
LEGACY_PACKAGE=com.paytondev.launchpad
LEGACY_DATA=/data/data/$LEGACY_PACKAGE
SYSAPK=/system/app/Launchpad.apk
# Persistent state (NEMESIS's /data/nemesis/backup/<id> convention). Survives
# reboots; removed on uninstall.
BACKUP_DIR=/data/nemesis/backup/paytondev.launchpad
PREFS_BACKUP=$BACKUP_DIR/prefs
# The app's SharedPreferences files, with the .bak copy Android keeps while a
# write is in progress (a .bak wins over its .xml when the app loads them).
PREF_FILES="launchpad.xml launchpad.xml.bak launchpad_preconditioning.xml launchpad_preconditioning.xml.bak"
# Temporary files only (cleared on reboot). 1.x staged its /system copy here.
WORK=/data/local/tmp/launchpad
# NEMESIS extracts an incoming package here before running the old package's
# uninstall hooks, so its presence during preuninstall means "update".
STAGE=/data/local/tmp/nemesis/stage/paytondev.launchpad

log() { echo "$(date '+%Y-%m-%d %H:%M:%S') launchpad $*"; }

# KitKat toolbox cmp has no -s option. Preserve its exit status and silence I/O.
files_equal() { cmp "$1" "$2" >/dev/null 2>&1; }

android_up() { [ "$(getprop sys.boot_completed)" = 1 ]; }

# app_process (and so am and pm) needs init's framework environment
# (BOOTCLASSPATH); load it if the caller (svcmgr, NEMESIS) did not pass it through.
BOOT_ENV=/init.environ.rc
load_boot_env() {
    if [ -z "$BOOTCLASSPATH" ] && [ -r "$BOOT_ENV" ]; then
        while read CMD NAME VALUE; do
            [ "$CMD" = export ] && export "$NAME=$VALUE"
        done < "$BOOT_ENV"
    fi
}

# 0 if the last /system entry in /proc/mounts is mounted read-only.
system_is_ro() {
    RO=1
    while read DEV MNT FS OPTS REST; do
        [ "$MNT" = /system ] || continue
        case "$OPTS" in ro|ro,*) RO=0 ;; *) RO=1 ;; esac
    done < /proc/mounts
    return $RO
}

# Make /system writable only if it is read-only now, and remember to put it back.
REMOUNTED=0
system_rw() {
    if system_is_ro; then
        "$NEMESIS" remount rw || return 1
        REMOUNTED=1
    fi
    return 0
}
system_restore() {
    if [ "$REMOUNTED" = 1 ]; then
        "$NEMESIS" remount ro || log "WARN: could not remount /system ro"
        REMOUNTED=0
    fi
}

# force_stop_app [PACKAGE]: only before a backup or a removal. Force-stop also puts
# a package back into Android's stopped state, which hides BOOT_COMPLETED from it.
force_stop_app() {
    if android_up; then
        load_boot_env
        am force-stop "${1:-$APP_PACKAGE}" >/dev/null 2>&1
    fi
    return 0
}

# Where Android has the app installed (pm path prints "package:<apk>"), or nothing.
# pm prints failures as text and exits 0, so read its output, not its status.
app_path() {
    load_boot_env
    pm path "$APP_PACKAGE" 2>/dev/null | while read LINE; do
        case "$LINE" in package:*) echo "${LINE#package:}" ;; esac
    done
}

# 0 if Android has the app as a normal user app (/data/app), the only kind NEMESIS
# Manager's app grid lists.
app_is_user_app() {
    case "$(app_path)" in /data/*) return 0 ;; *) return 1 ;; esac
}

# 0 if there is a settings backup waiting to be restored.
prefs_backed_up() {
    for F in $PREF_FILES; do
        [ -f "$PREFS_BACKUP/$F" ] && return 0
    done
    return 1
}

# Only ever deletes this package's own backup folder.
remove_backup() {
    case "$BACKUP_DIR" in /*/backup/paytondev.launchpad) ;; *) return 1 ;; esac
    rm -rf "$BACKUP_DIR"
}

# backup_prefs [DATA_DIR]: copy an app's settings out of its data directory
# (default: this app's), which pm uninstall (update) or the next boot (1.x) deletes.
# With no settings to save, an older backup that was never restored is kept.
backup_prefs() {
    FROM=${1:-$APP_DATA}/shared_prefs
    FOUND=0
    for F in $PREF_FILES; do
        [ -f "$FROM/$F" ] && FOUND=1
    done
    if [ "$FOUND" = 0 ]; then
        log "backup: no settings in $FROM"
        return 0
    fi
    remove_backup || return 1
    mkdir -p "$PREFS_BACKUP" || { log "backup: cannot create $PREFS_BACKUP"; return 1; }
    N=0
    for F in $PREF_FILES; do
        [ -f "$FROM/$F" ] || continue
        if cat "$FROM/$F" > "$PREFS_BACKUP/$F" \
            && files_equal "$FROM/$F" "$PREFS_BACKUP/$F"; then
            N=$((N + 1))
        else
            log "backup: could not copy $F"
            return 1
        fi
    done
    log "backup: saved $N settings file(s) from $FROM to $PREFS_BACKUP"
    return 0
}

# Put a settings backup back after pm install, owned by the app's (possibly new)
# uid, with the modes Android itself uses (shared_prefs 0771, files 0660). New
# files inherit the SELinux label of the app's data directory they are created in.
restore_prefs() {
    [ -d "$APP_DATA" ] || { log "restore: $APP_DATA missing"; return 1; }
    # toolbox ls -n on a directory: mode uid gid date time name
    set -- $(ls -nd "$APP_DATA" 2>/dev/null)
    OWNER=$2
    GROUP=$3
    case "$OWNER" in ''|*[!0-9]*) log "restore: cannot read the owner of $APP_DATA"; return 1 ;; esac
    case "$GROUP" in ''|*[!0-9]*) log "restore: cannot read the group of $APP_DATA"; return 1 ;; esac
    DIR=$APP_DATA/shared_prefs
    if [ ! -d "$DIR" ]; then
        mkdir "$DIR" || { log "restore: cannot create $DIR"; return 1; }
    fi
    chmod 0771 "$DIR" && chown "$OWNER:$GROUP" "$DIR" || { log "restore: cannot set up $DIR"; return 1; }
    N=0
    for F in $PREF_FILES; do
        [ -f "$PREFS_BACKUP/$F" ] || continue
        if cat "$PREFS_BACKUP/$F" > "$DIR/$F" && files_equal "$PREFS_BACKUP/$F" "$DIR/$F" \
            && chmod 0660 "$DIR/$F" && chown "$OWNER:$GROUP" "$DIR/$F"; then
            N=$((N + 1))
        else
            log "restore: could not restore $F"
            return 1
        fi
    done
    log "restore: restored $N settings file(s) as $OWNER:$GROUP"
    remove_backup
    return 0
}

# Remove the 1.x copy at /system/app/Launchpad.apk: the only /system change 2.x
# makes, and only once. /system is writable only for this and set back right after.
remove_legacy_sysapk() {
    [ -f "$SYSAPK" ] || return 0
    force_stop_app "$LEGACY_PACKAGE"
    if ! system_rw; then
        log "legacy: nemesis remount rw failed; $SYSAPK left in place"
        return 1
    fi
    rm -f "$SYSAPK"
    sync
    system_restore
    if [ -f "$SYSAPK" ]; then
        log "legacy: could not remove $SYSAPK"
        return 1
    fi
    log "legacy: removed $SYSAPK"
    return 0
}
