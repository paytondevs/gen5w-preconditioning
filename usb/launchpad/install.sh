#!/system/bin/sh
# NEMESIS type=execute: install the Launchpad package (TUNE service +
# /system/app/Launchpad.apk), or update it in place if it is already installed.
# On update the old preuninstall leaves /system/app/Launchpad.apk alone and the new
# postinstall rewrites it, so the app keeps its data. Uninstall is NEMESIS
# Manager's Uninstall button, which runs the package's preuninstall.
. "${0%/*}/lib.sh"

[ -f "$PKGZIP" ] || { say "FAIL: missing $PKGZIP"; finish 1; }
if pkg_installed; then
    say "$PKG_ID already installed; updating in place"
    run "$NEMESIS" install --update "$PKGZIP" || finish 1
else
    run "$NEMESIS" install "$PKGZIP" || finish 1
fi
verify_installed
finish $?
