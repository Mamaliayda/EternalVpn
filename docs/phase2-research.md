# Phase 2 Research — Core Integration (Verified)

**Date:** 2026-11-25  
**Scope:** Verify the real latest AndroidLibXrayLite release/API, propose hev-socks5-tunnel NDK/Gradle build, compare with sing-box libbox, recommend one.  
**Status:** Research only. **No Phase 2 code started** (awaiting approval).

---

## 1) AndroidLibXrayLite — verified release and Java API

### 1.1 Latest official release (verified)

Verified via GitHub API (`GET https://api.github.com/repos/2dust/AndroidLibXrayLite/releases/latest`) on 2026-11-25:

| Field | Value |
|---|---|
| Repo | `2dust/AndroidLibXrayLite` |
| Latest tag | `v26.11.9` |
| Release published | `2026-11-10T06:38:42Z` |
| Commit (target) | `main` @ `2026-11-10T06:35:32Z` (release metadata) |
| Asset | `libv2ray.aar` (60,437,926 bytes, sha256 `cdfef146218233a31a8b52c8b1016557730d3b63fc121d4c84cacea50e669155`) |
| Asset | `libv2ray-sources.jar` (8,755 bytes) |

Source: https://github.com/2dust/AndroidLibXrayLite/releases/tag/v26.11.9

### 1.2 Build recipe (from upstream README, verified)

From `README.md` at tag `v26.11.9` (cloned and read):

```bash
git clone [repo] && cd AndroidLibXrayLite
gomobile init
go mod tidy -v
gomobile bind -v -androidapi 24 -trimpath -ldflags='-s -w -buildid= -checklinkname=0' ./
```

Build requirements: JDK, Android SDK, Go, gomobile.

### 1.3 Actual exported Go/Java API (read from source, not guessed)

I cloned `2dust/AndroidLibXrayLite` at tag `v26.11.9` and read the Go sources. gomobile exports these to Java packages/classes under `libv2ray.*` / `go.*`.

#### `libv2ray_main.go` (verified)

```go
const (
    coreAsset            = "xray.location.asset"
    coreCert             = "xray.location.cert"
    xudpBaseKey          = "xray.xudp.basekey"
    tunFdKey             = "xray.tun.fd"
    browserDialerAddress = "xray.browser.dialer"
    libVersion           = 40
)

type CoreController struct {
    CallbackHandler CoreCallbackHandler
    // ... unexported fields
    IsRunning bool
}

type CoreCallbackHandler interface {
    Startup() int
    Shutdown() int
    OnEmitStatus(int, string) int
}

func InitCoreEnv(envPath string, key string)
func NewCoreController(s CoreCallbackHandler) *CoreController
func (x *CoreController) StartLoop(configContent string, tunFd int32) (err error)
func (x *CoreController) StopLoop() error
func CheckVersionX() string
func ReconcileBrowserDialer(dialerAddr string)
```

Behavior notes from source:
- `InitCoreEnv` sets `xray.location.asset/cert`, `xray.xudp.basekey`, and installs a custom `corefilesystem.NewFileReader` that falls back to `golang.org/x/mobile/asset` for Android assets.
- `NewCoreController` registers a console log handler creator.
- `StartLoop` sets env `xray.tun.fd` to `tunFd` (`0` means "do not use TUN"), locks, no-ops if already running, loads JSON config via `coreserial.LoadJSONConfig`, creates and starts `core.New(config)`. On success calls `CallbackHandler.Startup()` and `OnEmitStatus(0, "Started successfully, running")`.
- `StopLoop` locks, shuts down core if running, calls `CallbackHandler.Shutdown()` and `OnEmitStatus(0, "Core stopped")`.

#### `libv2ray_android.go` (verified)

```go
type ProcessFinder interface {
    FindProcessByConnection(network, srcIP string, srcPort int, destIP string, destPort int) int
}
func (x *CoreController) RegisterProcessFinder(finder ProcessFinder)
```

Registers Android per-app routing via `corenet.RegisterAndroidProcessFinder`; must be called before starting core for UID-based routing rules.

#### `libv2ray_utils.go` (verified)

```go
func (x *CoreController) QueryAllOutboundTrafficStats() string
func (x *CoreController) MeasureDelay(url string) (int64, error)
func MeasureOutboundDelay(ConfigureFileContent string, url string) (int64, error)
```

- `QueryAllOutboundTrafficStats` returns `tag,direction,value;...` and resets counters.
- `MeasureDelay` uses `core.Dial` through the running instance, 12s timeout, 2 attempts, min RTT.
- `MeasureOutboundDelay` builds a minimal outbound-only instance from config and measures delay.

#### `libv2ray_certSha256.go` (verified)

```go
func FetchTlsCertSha256(requestJSON string) string
func FetchQuicCertSha256(requestJSON string) string
```

### 1.4 Implication for our architecture

The presence of `tunFd int32` in `StartLoop` and the `xray.tun.fd` env var is significant: **modern AndroidLibXrayLite can pass a TUN fd into Xray-core**. This means the "Xray SOCKS inbound + hev-socks5-tunnel" bridge described in our original plan is **not the only option**. Two viable integration patterns exist:

1. **TUN fd passed to Xray-core** (`startLoop(config, tunFd)`) — Xray handles TUN inbound internally. Simpler native stack (no separate tun2socks binary), but we must confirm the exact Xray JSON inbound/config expected by this mode in v26.11.9 and in v2rayNG's current implementation.
2. **Classic SOCKS + tun2socks** (`startLoop(config, 0)` + hev-socks5-tunnel) — our current plan. Well-known, works even when Xray TUN mode is unavailable/misconfigured; adds a native dependency.

**Before Phase 2 we must inspect v2rayNG's current `V2RayServiceManager`/TUN wiring for v26.11.9-era usage** to pick the pattern that matches the actual shipped behavior, not assumptions.

---

## 2) hev-socks5-tunnel — build options for Android/Gradle

### 2.1 Latest release (verified)

Verified via GitHub API on 2026-11-25:

| Field | Value |
|---|---|
| Repo | `heiher/hev-socks5-tunnel` |
| Latest tag | `2.20.0` |
| Published | `2026-11-13T13:06:16Z` |
| Android assets | `hev-socks5-tunnel-android-arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`, and `hev-socks5-tunnel.aar` |

Source: https://github.com/heiher/hev-socks5-tunnel/releases/tag/2.20.0

### 2.2 Upstream build methods (from README/search results)

Upstream README documents:

```bash
mkdir hev-socks5-tunnel
cd hev-socks5-tunnel
git clone --recursive https://github.com/heiher/hev-socks5-tunnel jni
ndk-build
```

Also supports `make static`/`make shared`, and CI produces a prebuilt AAR with JNI class `hev.htproxy.TProxyService`.

### 2.3 Options for integrating into Gradle

| Option | How | Pros | Cons | Recommendation |
|---|---|---|---|---|
| **A. Prebuilt AAR from release** | Download `hev-socks5-tunnel.aar` in `scripts/setup.sh`, place in `app/libs/` | Fastest; no NDK toolchain in CI beyond normal Android build; upstream-tested | Must trust/review binary; JNI class/name is fixed to `hev.htproxy.TProxyService` unless rebuilt; version bumps are manual | Good fallback / quickest for prototyping |
| **B. Prebuilt `.so` per ABI** | Download release ABI binaries or build once, commit as LFS or fetch in setup | Simple runtime | No JNI wrapper unless we add one; harder to call from Java/Kotlin; ABI management manual | Not ideal for our app-level integration |
| **C. ndk-build via Gradle `externalNativeBuild`/custom task** | Add `hev-socks5-tunnel` as git submodule under `jni/` or `third_party/`; Gradle task runs `ndk-build` (or CMake wrapper) producing `.so` | Reproducible from source; pins exact commit; can set `PKGNAME/CLSNAME` for our JNI package | Requires NDK in CI; submodule/build complexity; longer CI; need to maintain Android.mk integration | Viable if we need custom JNI package name or source build |
| **D. Build AAR from source in CI, cache it** | Separate job builds AAR via ndk-build, uploads artifact/cache; app build consumes it | Reproducible + fast app builds | More CI plumbing | Good for mature phase |

**Proposed Gradle approach (if we pick hev-socks5-tunnel):** start with **Option A** (pinned prebuilt AAR downloaded by `scripts/setup.sh`, sha256-verified) for Phase 2 to de-risk integration; move to **Option C/D** once the VPN path works. If we need our own JNI class name, rebuild with `APP_CFLAGS="-DPKGNAME=com/eternalvpn/app/tun2socks -DCLSNAME=TProxyService"` (pattern used by v2rayNG/paqetNG forks).

NDK requirement for source build: Android NDK r27+ (as seen in upstream CI refs), `ndk-build` on PATH; Gradle can invoke via `Exec` task or `externalNativeBuild { ndkBuild { path ... } }` with a thin `Android.mk` wrapper.

---

## 3) sing-box libbox comparison

### 3.1 What it is

`sing-box` (SagerNet) provides `libbox`, a GoMobile AAR for Android that bundles the sing-box core. It exposes a platform interface for TUN, notifications, and process protection. It is used by sing-box Android clients (SFA / NekoBox derivatives historically; ecosystem has shifted over time).

### 3.2 High-level comparison

| Dimension | AndroidLibXrayLite (Xray-core) | sing-box libbox |
|---|---|---|
| Core | Xray-core (XTLS) | sing-box (SagerNet) |
| Protocol coverage (v1 needs) | VLESS, VMess, Trojan, Shadowsocks, REALITY, XHTTP, WS, gRPC, TCP, TLS — all native to Xray ecosystem | VLESS/VMess/Trojan/SS supported; REALITY supported; **XHTTP support is limited/immature compared to Xray-core** (verify exact current status before committing) |
| TUN integration | `StartLoop(config, tunFd)` in v26.11.9 suggests built-in TUN fd support; classic SOCKS+tun2socks also possible | Native TUN via libbox platform interface (`PlatformInterface`) — designed for VPN apps |
| Android API style | gomobile bind; `Libv2ray.initCoreEnv/newCoreController/startLoop/stopLoop/...` (verified above) | gomobile bind; libbox command/client/platform interfaces (different API shape) |
| Config format | Xray JSON | sing-box JSON (different schema) |
| Routing/Geo | Xray routing rules, geoip/geosite `ir` assets | sing-box rule-sets/geoip/geosite; Iran rule assets also available but format/integration differs |
| APK/size impact | ~60 MB AAR (v26.11.9) | Similar order of magnitude (large GoMobile AAR) |
| Maturity for our exact v1 | Direct match: our plan is written around Xray JSON, VLESS/VMess/Trojan/SS, XHTTP, REALITY, Iran geoip/geosite | Would require rewriting config builder, parsers' semantic mapping, routing, and possibly dropping/deferring XHTTP |
| License | LGPL-3.0 (upstream repo) | GPL/other sing-box licensing terms — must verify for app distribution |

### 3.3 XHTTP note (critical for our v1)

Our v1 spec explicitly requires XHTTP transport. Xray-core is the reference implementation for XHTTP. Official SagerNet sing-box has historically lagged on XHTTP parity; downstream forks (e.g. `Leadaxe/sing-box-lx`) advertise libbox builds with `with_xhttp` enabled, but that is a third-party fork with separate maintenance, API stability, and licensing/distribution considerations. As of this research, for our v1 I cannot treat forked libbox as a drop-in equivalent to verified upstream AndroidLibXrayLite. **This remains a compatibility/maintenance risk for sing-box-based paths** given our stated v1 requirements.

---

## 4) Recommendation

**Recommend: AndroidLibXrayLite (Xray-core) for v1.**

### Rationale

1. **Spec fit:** Our v1 explicitly requires VLESS/VMess/Trojan/SS, transports TCP/WS/gRPC/XHTTP, TLS/REALITY, and Xray JSON-style routing with geoip/geosite. AndroidLibXrayLite matches this directly.
2. **Verified API:** v26.11.9 has a clear, small GoMobile API (`InitCoreEnv`, `NewCoreController`, `StartLoop(config, tunFd)`, `StopLoop`, `MeasureDelay`, stats, process finder). I verified it from source.
3. **TUN path simplification:** `StartLoop(..., tunFd)` means we may not need hev-socks5-tunnel at all if we use Xray's TUN inbound. That reduces native complexity. **This must be validated against v2rayNG's current wiring before implementation.**
4. **Lower migration risk:** The plan, parser model, and routing design are already Xray-shaped. Switching to sing-box would mean redesigning config builder, routing rules, geo assets, and re-verifying protocol/transport parity (especially XHTTP).

### What I would not do yet

- Do **not** start Phase 2 code before you approve this direction.
- Do **not** assume hev-socks5-tunnel is required until we confirm whether v26.11.9 TUN-fd mode covers our needs (v2rayNG reference check).
- Do **not** copy v2rayNG code; only use it as behavioral reference and keep GPL boundaries clean.

### Proposed Phase 2 implementation plan (after approval)

1. **Pin & fetch** `libv2ray.aar` v26.11.9 in `scripts/setup.sh` with sha256 check; update README.
2. **Reference study (no copy):** read v2rayNG's current TUN/service wiring for the `startLoop(config, tunFd)` pattern and Xray JSON TUN inbound structure.
3. **XrayConfigBuilder:** generate full Xray JSON (socks-in or tun-in as decided, outbounds, routing, DNS, sniffing). Unit-test JSON shape.
4. **XrayController:** wrap `Libv2ray.initCoreEnv`, `newCoreController`, `startLoop/stopLoop`, callback handler, stats/delay.
5. **TunService:** foreground VpnService, establish TUN fd, pass fd/config to controller, handle revoke/network changes/battery optimization.
6. **Integration test on real device:** connect to a real node, confirm foreign site via proxy and (Phase 5 later) Iranian site direct.

### Open questions for you to approve

1. **Core choice:** approve AndroidLibXrayLite/Xray-core for v1? (recommended)
2. **TUN strategy:** prefer validating Xray `tunFd` mode first (simpler stack), with hev-socks5-tunnel as fallback? (recommended)
3. **hev-socks5-tunnel packaging (if fallback needed):** start with prebuilt AAR pinned+sha256 in setup script, or invest in ndk-build-from-source in Gradle now?

---

## 5) Sources

- AndroidLibXrayLite latest release API: https://api.github.com/repos/2dust/AndroidLibXrayLite/releases/latest (verified 2026-11-25, returned `v26.11.9`)
- AndroidLibXrayLite source (cloned tag `v26.11.9`): `libv2ray_main.go`, `libv2ray_android.go`, `libv2ray_utils.go`, `libv2ray_certSha256.go`, `README.md`
- hev-socks5-tunnel latest release API: https://api.github.com/repos/heiher/hev-socks5-tunnel/releases/latest (verified 2026-11-25, returned `2.20.0`)
- hev-socks5-tunnel README/build docs: https://github.com/heiher/hev-socks5-tunnel
- v2rayNG (behavioral reference only, no code copied): https://github.com/2dust/v2rayNG
- sing-box: https://github.com/SagerNet/sing-box (comparison based on known project scope; XHTTP parity needs recheck before any switch)
