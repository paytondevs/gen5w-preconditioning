#!/system/bin/sh
# Optional HMG-API integration. HMG-API owns its registry and performs writes;
# this package never edits its maps or other modules. No star key is assigned.
# Sourced common.sh sets HERE and DEX. All checks fail closed.
. /data/nemesis/apps/paytondev.launchpad/scripts/common.sh
export PATH=/sbin:/vendor/bin:/system/sbin:/system/bin:/system/xbin:/data/nemesis/bin
unset LD_PRELOAD HMGAPI_SELFTEST HMGAPI_ROOT HMGAPI_HOST HMGAPI_PRODUCT HMGAPI_ONEBIN HMGAPI_PROPS HMGAPI_TEST_ANY_CAR HMGAPI_LPATCH HMGAPI_HMGAPID
ID=precondition-star
UUID=8843fd55-d673-8045-a574-11e4e09794b0
MOD=/data/hmgapi/mods/$ID
SOURCE=$HERE/star/mod
ENGINE=/system/framework/hmgapi-hook.jar
PROCESS=/system/bin/app_process.hmgapi-stock
java_check() (
    export CLASSPATH="$DEX:$ENGINE" ANDROID_ROOT=/system ANDROID_DATA=/data
    exec "$PROCESS" /system/bin com.paytondev.launchpad.HmgStarCheck "$@"
)
verify_mod() (
    export CLASSPATH="$ENGINE" ANDROID_ROOT=/system ANDROID_DATA=/data
    exec "$PROCESS" /system/bin hmgapi.engine.Signing verify "$1" "$UUID"
)
case "$1" in
install)
    [ "$(getprop ro.build.product)" = NEEV.KOR.0000.V039.001.260902 ] || { log "star: unsupported car; skipped"; exit 1; }
    [ "$(getprop ro.board.platform)" = tcc803x ] && [ "$(getprop ro.build.version.sdk)" = 19 ] || exit 1
    android_up && [ "$(hmgapid version 2>/dev/null)" = 'hmgapid 1.0.2' ] || { log "star: HMG-API not ready; skipped"; exit 1; }
    [ -f /data/hmgapi/hook/enable ] && [ ! -e /data/hmgapi/hook/disable ] && [ ! -e /data/hmgapi/state/journal.json ] || exit 1
    [ -x "$PROCESS" ] && [ -f "$ENGINE" ] && [ -f "$DEX" ] || exit 1
    load_boot_env
    java_check ready "$HERE/star/data/buttonsettings.json" || { log "star: checked map unavailable; skipped"; exit 1; }
    verify_mod "$SOURCE" || { log "star: signed package invalid"; exit 1; }
    if [ -e "$MOD" ]; then
        [ ! -e "$MOD/.disabled" ] || { log "star: existing mod is disabled; unchanged"; exit 1; }
        [ -d "$MOD" ] && [ ! -L "$MOD" ] && verify_mod "$MOD" &&
            files_equal "$MOD/META-HMGAPI/manifest.json" "$SOURCE/META-HMGAPI/manifest.json" || {
            log "star: existing mod differs; unchanged"; exit 1;
        }
        log "star: matching mod already installed; unchanged"
        exit 0
    fi
    hmgapid engine install "$SOURCE" --uuid "$UUID" --from-nemesis || { log "star: HMG-API install failed"; exit 1; }
    log "star: HMG-API mod installed; restart required"
    ;;
remove)
    # Registry entries belong to HMG-API and are never removed by this app.
    [ -e "$MOD" ] || exit 0
    [ -d "$MOD" ] && [ ! -L "$MOD" ] || exit 0
    # Only guard an action whose descriptor targets this exact activity. Do not
    # block app removal for a different author's unrelated action with this id.
    files_equal "$MOD/mod.json" "$SOURCE/mod.json" || exit 0
    android_up && [ -f "$DEX" ] || { log "star: cannot check assigned buttons until Android is ready"; exit 1; }
    [ -x "$PROCESS" ] || PROCESS=/system/bin/app_process
    [ -x "$PROCESS" ] || { log "star: cannot check assigned buttons"; exit 1; }
    load_boot_env
    java_check unused || { log "star: choose another function for both star buttons, or restore button service"; exit 1; }
    log "star: HMG-API retains the independently managed mod"
    ;;
*) exit 2 ;;
esac
