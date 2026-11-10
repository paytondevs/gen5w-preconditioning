# Optional stock star shortcut (draft)

[한국어 안내](HMG-STAR.ko.md)

The NEMESIS package attempts to register **Open Precondition** in HMG-API's
stock star-button choices, leaving the current button selection unchanged. This
is optional: the app remains usable when the integration is unavailable. APK
installation alone does not perform this registration. After registration,
restart the head unit and choose the action in the stock star-button settings.

The current draft is limited to Android 4.4.2/API 19,
`NEEV.KOR.0000.V039.001.260902`, HMG-API **1.0.2**, and an already installed
and checked HMG-API map that includes the exact 31 ButtonSettings points and
framework resource hooks. It skips registration if HMG-API, Android or the
checked map is not ready. A 375-point baseline without ButtonSettings remains
unchanged and does not gain this feature. Retry the NEMESIS update only after
Android and HMG-API have fully started and the map is separately available.
The package checks the existing map, report and hook points read-only; it does
not create, edit or replace the map or its existing verification report.
HMG-API's official `engine install` owns its own module registry; no other
HMG module is intentionally changed. Do not run HMG-API install/remove/map
operations concurrently: HMG-API 1.0.2 does not provide a shared transaction
lock for these operations.

Updates preserve an existing matching module without replacing or enabling it.
A different manifest or a disabled module is left alone; this draft does not
automatically update it. For removal, choose a stock function for both star
buttons before uninstalling the app. The independently managed HMG-API module
is intentionally retained to avoid deleting a module installed by the owner;
after removing the app, the user can remove the module using HMG-API. Do not
force an uninstall while the action is assigned. The guard only applies when the
installed `mod.json` exactly matches the bundled descriptor; changed or
unverifiable descriptors may not be guarded. Always switch both buttons first,
even if no guard appears. Do not change button settings during install/removal.
NEMESIS forced operations may bypass the normal uninstall guard.

The bundled signed module is a **contributor test signature**, not a production
release key. The maintainer must decide ownership, sign releases with a key they
control, and document the update/migration policy before a release. The key is
not in this repository. The ButtonSettings metadata was derived from a report
produced by HMG-API's AGPL-3.0-or-later mapping tooling; no proprietary APK,
firmware, or raw disassembly is included. This code calls the installed HMG-API
1.0.2 checker at runtime instead of copying its engine code.

This upstream package, its target activity, automatic installation, update,
uninstallation, and button operation have **not** been tested on a real car.
A different Korean derivative and a separately installed HMG module showed
and launched from the stock button on one car; that is not a test of this PR.
No battery heating was tested. This is a draft for maintainer review, including
whether a call that has HMG-API manage its own data fits this repository's
package-local state rule.
