# EternalVPN

Personal Android VPN client (Kotlin + Jetpack Compose) built around Xray-core.

> Status: **Phase 0/1 skeleton in progress**. This repository currently contains the project skeleton,
> data layer, and link/subscription parsers with unit tests. Core VPN wiring (Xray + tun2socks),
> scanner, and routing are scaffolded but not yet implemented.

## Planned features (v1)

1. **V2Ray/Xray client** — import nodes from `vless://`, `vmess://`, `trojan://`, `ss://`, base64
   subscriptions, QR and clipboard; transports TCP/WS/gRPC/XHTTP; TLS/REALITY; node testing and
   subscription management.
2. **Clean-IP scanner** — two-stage: fast TCP+TLS probe over IP ranges, then real verification through
   the core; ranked results with one-tap apply.
3. **Iran routing + split tunnel** — `geoip:ir` / `geosite:category-ir` direct, rest proxied; per-app
   split tunnel via `VpnService`.

See [`personal-vpn-plan.md`](./personal-vpn-plan.md) for the full architecture and phased roadmap.

## Project layout

```
app/src/main/java/com/eternalvpn/app/
├── core/        # XrayController, XrayConfigBuilder, TunService
├── data/        # Room models/db, DataStore settings, link parsers
├── scanner/     # Stage 1/2 clean-IP scanner
├── routing/     # Iran routing, DNS, per-app split tunnel
└── ui/          # Compose screens: Home, Nodes, Scanner, Routing, Settings
```

## Build

### Prerequisites

- JDK 17
- Android SDK with platform 35 (or as configured in `app/build.gradle.kts`)
- Android SDK Build-Tools and Platform-Tools

### Setup large binaries (not committed)

```bash
./scripts/setup.sh
```

This downloads `libv2ray.aar` (AndroidLibXrayLite) into `app/libs/` and geo files into
`app/src/main/assets/geo/`. You must also provide `hev-socks5-tunnel` native libraries for Phase 2
(see script notes).

### Build debug APK

```bash
./gradlew assembleDebug
```

### Run parser unit tests (JVM)

```bash
./gradlew :app:testDebugUnitTest
```

## Version decisions (initial)

| Component | Version | Why |
|---|---:|---|
| Android Gradle Plugin | 8.7.3 | Stable, widely available; compatible with Gradle 8.9 and compileSdk 35 |
| Gradle | 8.9 | Matches AGP 8.7.x |
| Kotlin | 2.0.21 | Stable K2/Compose compiler plugin support |
| compileSdk / targetSdk / minSdk | 35 / 34 / 24 | Plan targets minSdk 24, target 34; compileSdk 35 chosen for AGP 8.7.x compatibility (plan noted 36 - bump with AGP 8.9+) |
| AndroidLibXrayLite (libv2ray) | v26.11.9 (pinned in setup script) | Latest verified release (2026-11-25); API verified from source in `docs/phase2-research.md` |
| hev-socks5-tunnel | 2.x (source/build) | To be integrated in Phase 2; native build or prebuilt bundle required |

> Note: Versions above are initial picks for the skeleton. Before Phase 2, re-check the latest stable
> AndroidLibXrayLite release and its Go-mobile Java API (`libv2ray.Libv2ray`, `CoreController`,
> callback interfaces) and update the controller/builder accordingly.

## Security / privacy

- No analytics, no telemetry, no third-party crash reporting.
- Subscription URLs and node configs stay on-device (Room/DataStore).
- Large binaries (`*.aar`, `*.so`, `*.dat`) are excluded from git; use `scripts/setup.sh`.

## License

TBD by repository owner. Reference projects (v2rayNG is GPL-3.0) are for ideas only; no code copied.
