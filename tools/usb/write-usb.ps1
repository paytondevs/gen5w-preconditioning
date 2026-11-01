# Write a Launchpad USB bundle onto a FAT32/exFAT drive. Runs on your computer;
# does not connect to the car.
#   .\tools\usb\write-usb.ps1 E:\ [bundle-dir]
# bundle-dir holds Launchpad-install-usb.zip (default: .\dist, then the current dir).
# The same drive installs or updates Launchpad.
param(
    [Parameter(Mandatory = $true)][string]$Drive,
    [string]$BundleDir
)
$ErrorActionPreference = 'Stop'

$name = 'Launchpad-install-usb.zip'
$candidates = @($BundleDir, (Join-Path $PSScriptRoot '..\..\dist'), 'dist', '.') | Where-Object { $_ }
$zip = $candidates | ForEach-Object { Join-Path $_ $name } | Where-Object { Test-Path -LiteralPath $_ -PathType Leaf } | Select-Object -First 1
if (-not $zip) { throw "$name not found; pass the folder you downloaded it to." }
if (-not (Test-Path -LiteralPath $Drive -PathType Container)) { throw "$Drive is not a directory (is the drive mounted?)" }

# Move (never delete) anything from a previous NEMESIS job out of the drive root.
$backup = Join-Path $Drive ("NEMESIS-backup-" + (Get-Date -Format 'yyyyMMdd-HHmmss'))
foreach ($item in 'NEMESIS.data', 'NEMESIS.sh', 'ota_package.tar', 'README.txt', 'launchpad', 'packages') {
    $path = Join-Path $Drive $item
    if (Test-Path -LiteralPath $path) {
        New-Item -ItemType Directory -Force -Path $backup | Out-Null
        Move-Item -LiteralPath $path -Destination $backup
        Write-Host "moved $item -> $backup\"
    }
}

Expand-Archive -LiteralPath $zip -DestinationPath $Drive
Write-Host "Wrote $name to $Drive"
Get-ChildItem -LiteralPath $Drive -Name | Where-Object { $_ -in 'README.txt', 'NEMESIS.data', 'packages' }
Write-Host @"

Next:
  1. Eject the drive, insert it into the car's USB data port (car on, parked).
  2. Tap Yes on the NEMESIS install prompt (and on the update prompt, if
     Launchpad is already installed), then wait for the result dialog.
  3. Reboot the head unit (pinhole button), then remove the drive.
  If it fails: NEMESIS Manager > Logs > Copy to USB writes NEMESIS-logs to the drive.
"@
