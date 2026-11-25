#!/usr/bin/env bash
set -euo pipefail

# Downloads large binaries required to build EternalVPN.
# These files are NOT committed to git. Run once after cloning.

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LIBS="$ROOT/app/libs"
ASSETS="$ROOT/app/src/main/assets/geo"
mkdir -p "$LIBS" "$ASSETS"

# AndroidLibXrayLite (libv2ray AAR) - provides Xray-core for Android.
# Pinned to verified latest (2026-11-25). Review API before bumping.
LIBV2RAY_VERSION="${LIBV2RAY_VERSION:-v26.11.9}"
LIBV2RAY_URL="https://github.com/2dust/AndroidLibXrayLite/releases/download/${LIBV2RAY_VERSION}/libv2ray.aar"

echo "Downloading libv2ray ${LIBV2RAY_VERSION}..."
curl -fL "$LIBV2RAY_URL" -o "$LIBS/libv2ray.aar"

# hev-socks5-tunnel is typically built as .so/jni; integration depends on chosen build.
# See: https://github.com/heiher/hev-socks5-tunnel
# For Phase 2, either build from source or use a prebuilt AAR that bundles it.
echo "NOTE: hev-socks5-tunnel native libs must be provided for Phase 2."
echo "      Place libhev-socks5-tunnel.so under app/src/main/jniLibs/<abi>/ or use a bundled AAR."

# Geo files for Iran routing (Phase 5).
# Source: https://github.com/chocolate4u/Iran-v2ray-rules (verify latest release/tag).
GEO_VERSION="${GEO_VERSION:-latest}"
GEO_BASE="https://github.com/chocolate4u/Iran-v2ray-rules/releases/latest/download"
echo "Downloading geoip.dat / geosite.dat..."
curl -fL "$GEO_BASE/geoip.dat" -o "$ASSETS/geoip.dat"
curl -fL "$GEO_BASE/geosite.dat" -o "$ASSETS/geosite.dat"

echo "Done. Large binaries are in app/libs and app/src/main/assets/geo (gitignored)."
