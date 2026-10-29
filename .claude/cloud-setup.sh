#!/bin/bash
# Setup script for Claude Code cloud environments. Installs what the quickport and
# vehicle-port skills need: JDK 21 (keytool, jarsigner), Python 3, curl, unzip,
# openssl, and the Android SDK packages CI uses (platform 37, build-tools 36.0.0).
# Paste it into the environment's Setup script, or run `bash .claude/cloud-setup.sh`.
# Never connects to a car.
set -euo pipefail

SDK=/opt/android-sdk
CLT_ZIP=commandlinetools-linux-13114758_latest.zip   # same as quickport section 5
SDK_PACKAGES=("platforms;android-37.0" "build-tools;36.0.0")   # .github/workflows/build.yml

SUDO=
[ "$(id -u)" -eq 0 ] || SUDO=sudo

# --- Base tools ----------------------------------------------------------------
need=()
command -v javac >/dev/null && javac -version 2>&1 | grep -qE ' (2[1-9]|[3-9][0-9])' || need+=(openjdk-21-jdk-headless)
for tool in python3 curl unzip openssl; do
    command -v "$tool" >/dev/null || need+=("$tool")
done
if [ ${#need[@]} -gt 0 ]; then
    $SUDO apt-get update -qq
    DEBIAN_FRONTEND=noninteractive $SUDO apt-get install -y -qq "${need[@]}"
fi

JAVA_HOME=$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")
export JAVA_HOME

# --- Android SDK ---------------------------------------------------------------
if [ ! -x "$SDK/cmdline-tools/latest/bin/sdkmanager" ]; then
    tmp=$(mktemp -d)
    curl -fsSL --retry 4 -o "$tmp/clt.zip" "https://dl.google.com/android/repository/$CLT_ZIP"
    unzip -q "$tmp/clt.zip" -d "$tmp"
    $SUDO mkdir -p "$SDK/cmdline-tools"
    $SUDO rm -rf "$SDK/cmdline-tools/latest"
    $SUDO mv "$tmp/cmdline-tools" "$SDK/cmdline-tools/latest"
    rm -rf "$tmp"
fi
$SUDO chown -R "$(id -u):$(id -g)" "$SDK"
yes | "$SDK/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$SDK" --licenses >/dev/null || true
"$SDK/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$SDK" "${SDK_PACKAGES[@]}" >/dev/null
[ -x "$SDK/build-tools/36.0.0/apksigner" ] || { echo "build-tools 36.0.0 missing apksigner" >&2; exit 1; }

# Make ANDROID_HOME and JAVA_HOME visible to every later shell, login or not.
env_lines="export ANDROID_HOME=$SDK
export ANDROID_SDK_ROOT=$SDK
export JAVA_HOME=$JAVA_HOME
export PATH=\"\$PATH:$SDK/build-tools/36.0.0:$SDK/cmdline-tools/latest/bin\""
echo "$env_lines" | $SUDO tee /etc/profile.d/android-sdk.sh >/dev/null
grep -q 'android-sdk.sh' "$HOME/.bashrc" 2>/dev/null \
    || echo '[ -f /etc/profile.d/android-sdk.sh ] && . /etc/profile.d/android-sdk.sh' >> "$HOME/.bashrc"
export ANDROID_HOME=$SDK ANDROID_SDK_ROOT=$SDK

# --- Warm the Gradle cache (best effort) -----------------------------------------
# Maven Central rate-limits cloud sessions (HTTP 429) on the first download, so
# fetch the wrapper and dependencies here, with retries, when the repo is present.
# A failure here doesn't fail setup; quickport retries on its own.
repo=$(git rev-parse --show-toplevel 2>/dev/null || true)
if [ -n "$repo" ] && [ -x "$repo/gradlew" ]; then
    for attempt in 1 2 3 4; do
        if (cd "$repo" && ./gradlew --no-daemon --max-workers=1 -q :app:assembleDebug :app:testDebugUnitTest); then
            break
        fi
        [ "$attempt" -lt 4 ] || { echo "Gradle warm-up failed; skipping" >&2; break; }
        echo "Gradle warm-up attempt $attempt failed; retrying in $((attempt * 30)) s" >&2
        sleep $((attempt * 30))
    done
fi

echo "Setup done: JAVA_HOME=$JAVA_HOME ANDROID_HOME=$SDK"
