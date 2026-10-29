#!/usr/bin/env python3
"""Build the NEMESIS package, the USB install bundle and the read-only vehicle porting
kit bundle; never installs or connects to a car."""
import argparse
from pathlib import Path
import shutil
import zipfile

ROOT = Path(__file__).resolve().parents[1]
PACKAGE_ID = "paytondev.launchpad"

def pack(source, destination):
    with zipfile.ZipFile(destination, "w", zipfile.ZIP_DEFLATED) as archive:
        for path in sorted(source.rglob("*")):
            if path.is_file():
                archive.write(path, path.relative_to(source).as_posix())

def require_signed(apk):
    with zipfile.ZipFile(apk) as archive:
        names = archive.namelist()
    if "classes.dex" not in names:
        raise SystemExit(f"{apk} has no classes.dex")
    if not any(n.startswith("META-INF/") and n.upper().endswith((".RSA", ".DSA", ".EC")) for n in names):
        raise SystemExit(f"{apk} is not signed; build it with tools/build_apk.sh (it goes into /system/app)")

def read_version():
    version = (ROOT / "VERSION").read_text(encoding="utf-8").strip()
    parts = version.split(".")
    if len(parts) != 3 or not all(p.isdigit() for p in parts):
        raise SystemExit(f"VERSION must be major.minor.patch, got {version!r}")
    return version

def copy_lf(source, destination):
    # Car-side scripts run under KitKat mksh; never ship CRLF.
    destination.write_bytes(source.read_bytes().replace(b"\r\n", b"\n"))

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk", type=Path, default=ROOT / "dist/Launchpad.apk",
                        help="Signed Launchpad APK (default: dist/Launchpad.apk from tools/build_apk.sh)")
    args = parser.parse_args()
    apk = args.apk.resolve()
    if not apk.is_file():
        raise SystemExit(f"Missing {apk}. Run tools/build_apk.sh first, or pass --apk.")
    require_signed(apk)
    version = read_version()

    # Work only in this repository's generated build directory.
    stage = ROOT / "build/usb-bundles"
    if stage.exists():
        shutil.rmtree(stage)
    package = stage / "package"
    package.mkdir(parents=True)
    for path in sorted((ROOT / "tune-service").rglob("*")):
        if path.is_file():
            target = package / path.relative_to(ROOT / "tune-service")
            target.parent.mkdir(parents=True, exist_ok=True)
            copy_lf(path, target)
    # The package version comes from /VERSION, like the APK's versionName.
    descriptor = package / "NEMESIS.data"
    text = descriptor.read_text(encoding="utf-8")
    if text.count("version=@VERSION@\n") != 1:
        raise SystemExit("tune-service/NEMESIS.data needs exactly one version=@VERSION@ line")
    descriptor.write_bytes(text.replace("@VERSION@", version).encode("utf-8"))
    (package / "app").mkdir()
    shutil.copy2(apk, package / "app/Launchpad.apk")

    dist = ROOT / "dist"
    dist.mkdir(exist_ok=True)
    package_zip = dist / f"{PACKAGE_ID}.zip"
    pack(package, package_zip)

    # install.sh also updates in place; uninstall is NEMESIS Manager's Uninstall
    # button, which runs the package's preuninstall.
    usb = stage / "install"
    scripts = usb / "launchpad"
    scripts.mkdir(parents=True)
    copy_lf(ROOT / "tune-service/scripts/common.sh", scripts / "common.sh")
    copy_lf(ROOT / "usb/launchpad/lib.sh", scripts / "lib.sh")
    copy_lf(ROOT / "usb/launchpad/install.sh", scripts / "install.sh")
    copy_lf(ROOT / "usb/README-template.txt", usb / "README.txt")
    (usb / "packages").mkdir()
    shutil.copy2(package_zip, usb / f"packages/{PACKAGE_ID}.zip")
    (usb / "NEMESIS.data").write_bytes(
        b"[nemesis]\nversion=1.0\ntype=execute\npath=launchpad/install.sh\n")
    out = dist / "Launchpad-install-usb.zip"
    pack(usb, out)

    # The porting kit only reads: no package, so nothing for NEMESIS to install.
    # The same signed APK goes along purely as PortProbe's dex container.
    kit = stage / "port-kit"
    kit_scripts = kit / "launchpad-port"
    kit_scripts.mkdir(parents=True)
    collect = (ROOT / "usb/port-kit/collect.sh").read_bytes().replace(b"\r\n", b"\n")
    if collect.count(b"\nKIT_VERSION=@VERSION@\n") != 1:
        raise SystemExit("usb/port-kit/collect.sh needs exactly one KIT_VERSION=@VERSION@ line")
    (kit_scripts / "collect.sh").write_bytes(collect.replace(b"@VERSION@", version.encode("ascii")))
    copy_lf(ROOT / "tune-service/scripts/common.sh", kit_scripts / "common.sh")
    copy_lf(ROOT / "usb/launchpad/lib.sh", kit_scripts / "lib.sh")
    shutil.copy2(apk, kit_scripts / "Launchpad.apk")
    copy_lf(ROOT / "usb/port-kit/README-template.txt", kit / "README.txt")
    (kit / "NEMESIS.data").write_bytes(
        b"[nemesis]\nversion=1.0\ntype=execute\npath=launchpad-port/collect.sh\n")
    kit_out = dist / "Launchpad-port-kit-usb.zip"
    pack(kit, kit_out)
    print(out)
    print(kit_out)
    print(package_zip)

if __name__ == "__main__":
    main()
