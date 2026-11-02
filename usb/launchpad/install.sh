#!/system/bin/sh
# NEMESIS type=execute: install the Launchpad package, or update it in place if it
# is already installed. NEMESIS installs the APK as a normal Android app. The
# package's hooks carry the app's settings across an update and move Launchpad 1.x
# over (settings kept, its /system/app copy removed) in the same run. Uninstall is
# NEMESIS Manager's Uninstall button, which runs the package's preuninstall.
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
