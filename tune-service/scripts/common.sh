# Shared by the hooks and the service. Toolbox only (no grep/awk/busybox on KitKat).
export PATH=/sbin:/vendor/bin:/system/sbin:/system/bin:/system/xbin
export LD_LIBRARY_PATH=/vendor/lib:/system/lib
umask 022
HERE=/data/nemesis/apps/paytondev.launchpad
NEMESIS=/system/xbin/nemesis
# The signed Launchpad APK. The knob daemon uses it as its dex container
# (CLASSPATH under app_process), and postinstall places a copy in /system/app so
# PackageManager registers the visible app.
DEX=$HERE/app/Launchpad.apk
# dalvik-cache entry app_process may create for that dex (named after its path).
DEXCACHE=/data/dalvik-cache/data@nemesis@apps@paytondev.launchpad@app@Launchpad.apk@classes.dex
# The ONLY file under /system this package ever writes or removes.
SYSAPK=/system/app/Launchpad.apk
# Everything else (staging, verification copies, rollback backup) lives here.
WORK=/data/local/tmp/launchpad
# NEMESIS extracts an incoming package here before running the old package's
# uninstall hooks, so its presence during preuninstall means "update".
STAGE=/data/local/tmp/nemesis/stage/paytondev.launchpad
APP_PACKAGE=com.paytondev.launchpad

log() { echo "$(date '+%Y-%m-%d %H:%M:%S') launchpad $*"; }

# KitKat toolbox cmp has no -s option. Preserve its exit status and silence I/O.
files_equal() { cmp "$1" "$2" >/dev/null 2>&1; }

android_up() { [ "$(getprop sys.boot_completed)" = 1 ]; }

# app_process needs init's framework environment (BOOTCLASSPATH); load it if the
# caller (svcmgr, NEMESIS) did not pass it through.
BOOT_ENV=/init.environ.rc
load_boot_env() {
    if [ -z "$BOOTCLASSPATH" ] && [ -r "$BOOT_ENV" ]; then
        while read CMD NAME VALUE; do
            [ "$CMD" = export ] && export "$NAME=$VALUE"
        done < "$BOOT_ENV"
    fi
}

# Size in bytes of a file, from toolbox `ls -l` (perms owner group size date time name).
file_size() {
    set -- $(ls -l "$1" 2>/dev/null)
    case "$4" in ''|*[!0-9]*) echo 0 ;; *) echo "$4" ;; esac
}

# Free bytes on /system from toolbox df ("Filesystem Size Used Free Blksize",
# sizes like 312.4M). Prints nothing if the output cannot be parsed.
system_free() {
    df /system 2>/dev/null | while read FS SIZE USED FREE BLK; do
        [ "$FS" = /system ] || continue
        case "$FREE" in
            *K) MUL=1024 ;; *M) MUL=1048576 ;; *G) MUL=1073741824 ;; *) continue ;;
        esac
        NUM=${FREE%?}
        INT=${NUM%%.*}
        FRAC=0
        [ "$NUM" != "$INT" ] && FRAC=${NUM#*.}
        FRAC=${FRAC%${FRAC#?}}
        case "$INT$FRAC" in ''|*[!0-9]*) continue ;; esac
        echo $(( INT * MUL + FRAC * MUL / 10 ))
    done
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

stop_app() {
    android_up && am force-stop "$APP_PACKAGE" >/dev/null 2>&1
    return 0
}
