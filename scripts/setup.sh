#!/usr/bin/env bash
set -euo pipefail

# Downloads large binaries required to build EternalVPN.
# These files are NOT committed to git. Run once after cloning.
# All downloads are pinned by version and verified by SHA-256.

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LIBS="$ROOT/app/libs"
ASSETS="$ROOT/app/src/main/assets/geo"
mkdir -p "$LIBS" "$ASSETS"

verify_sha256() {
  local file="$1"
  local expected="$2"
  local actual
  actual="$(sha256sum "$file" | awk '{print $1}')"
  if [ "$actual" != "$expected" ]; then
    echo "SHA-256 mismatch for $file" >&2
    echo "  expected: $expected" >&2
    echo "  actual:   $actual" >&2
    rm -f "$file"
    exit 1
  fi
  echo "SHA-256 OK: $(basename "$file")"
}

download_verified() {
  local url="$1"
  local out="$2"
  local sha="$3"
  echo "Downloading $(basename "$out")..."
  curl -fL --retry 3 --retry-delay 2 "$url" -o "$out"
  verify_sha256 "$out" "$sha"
}

# AndroidLibXrayLite (libv2ray AAR) - provides Xray-core for Android.
# Verified latest official release as of 2026-11-25.
LIBV2RAY_VERSION="${LIBV2RAY_VERSION:-v26.11.9}"
LIBV2RAY_URL="https://github.com/2dust/AndroidLibXrayLite/releases/download/${LIBV2RAY_VERSION}/libv2ray.aar"
LIBV2RAY_SHA256="cdfef146218233a31a8b52c8b1016557730d3b63fc121d4c84cacea50e669155"
download_verified "$LIBV2RAY_URL" "$LIBS/libv2ray.aar" "$LIBV2RAY_SHA256"

# hev-socks5-tunnel AAR - fallback tun2socks path (used if Xray tunFd mode is unavailable).
# Pinned + sha256 verified per Phase 2 rules.
HEV_VERSION="${HEV_VERSION:-2.20.0}"
HEV_URL="https://github.com/heiher/hev-socks5-tunnel/releases/download/${HEV_VERSION}/hev-socks5-tunnel.aar"
HEV_SHA256="ae8a2e96b80b4eccccd037805a4bb4a278090c89dbcae8bc6fcb7634e9a255b9"
download_verified "$HEV_URL" "$LIBS/hev-socks5-tunnel.aar" "$HEV_SHA256"

# Geo files for Iran routing (Phase 5).
# Source: https://github.com/chocolate4u/Iran-v2ray-rules
GEOIP_URL="https://github.com/chocolate4u/Iran-v2ray-rules/releases/latest/download/geoip.dat"
GEOSITE_URL="https://github.com/chocolate4u/Iran-v2ray-rules/releases/latest/download/geosite.dat"
# NOTE: geo files track "latest" upstream release; hashes below are verified at pin time
# (2026-11-25). If upstream rotates latest, update hashes together with verification.
GEOIP_SHA256="feb0a85645cc954fc2ce551725f7299de938605a911fd6ed9016983d3b3274b3"
GEOSITE_SHA256="7b8025038eacfe89b3bf8b4bee0c27d5769e6c78e276279cf09cb8118e503a2e"
download_verified "$GEOIP_URL" "$ASSETS/geoip.dat" "$GEOIP_SHA256"
download_verified "$GEOSITE_URL" "$ASSETS/geosite.dat" "$GEOSITE_SHA256"

echo "Done. Verified binaries are in app/libs and app/src/main/assets/geo (gitignored)."
