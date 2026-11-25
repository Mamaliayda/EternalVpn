# VPN شخصی اندروید — معماری، نقشهٔ راه و مستر پرامپت

نسخهٔ اول: کلاینت V2Ray/Xray + اشتراک، اسکنر IP تمیز، Split tunnel + مسیریابی ایران.
مرجع: مخزن عمومی `mlmvpn/mlmvpn_android` (فقط برای الهام ساختار؛ کپی نکن).

---

## بخش ۱ — معماری

### انتخاب‌های اصلی
| بخش | انتخاب | دلیل |
|---|---|---|
| زبان/UI | Kotlin + Jetpack Compose (minSdk 24) | همان استک MLM VPN |
| هستهٔ پروکسی | Xray-core از طریق `libv2ray` (AndroidLibXrayLite) | پشتیبانی VLESS/VMess/Trojan/SS + REALITY، gRPC، WS، XHTTP |
| تبدیل TUN به SOCKS | `hev-socks5-tunnel` | سبک و سریع، همان چیزی که v2rayNG استفاده می‌کند |
| ذخیره‌سازی | Room (کانفیگ‌ها، اشتراک‌ها، نتایج اسکن) + DataStore (تنظیمات) | ساده و پایدار |
| پس‌زمینه | `VpnService` + Foreground Service + Quick Settings Tile | الزام اندروید |
| دادهٔ مسیریابی ایران | `geoip.dat` / `geosite.dat` با دسته‌های `ir` (از یک ریپوی جامعهٔ ایرانی مثل Iran-v2ray-rules؛ آخرین نسخه را چک کن) | قوانین direct برای سایت‌های ایرانی |

نکته: `libv2ray.aar` و فایل‌های geo حجیم‌اند؛ مثل MLM VPN با Git LFS یا دانلود در بیلد نگه‌شان دار، نه داخل کامیت معمولی.

### ساختار پوشه‌ها
```
app/src/main/java/com/yourname/vpn/
├── core/
│   ├── XrayController.kt        # start/stop هسته، لاگ، آمار
│   ├── XrayConfigBuilder.kt     # ساخت JSON از مدل Node
│   └── TunService.kt            # VpnService + tun2socks
├── data/
│   ├── model/                   # Node, Subscription, ScanResult
│   ├── db/                      # Room
│   ├── parser/                  # vless:// vmess:// trojan:// ss:// + base64 sub
│   └── SubscriptionManager.kt
├── scanner/
│   ├── RangeProvider.kt         # رنج‌های IP (Cloudflare و ورودی کاربر)
│   ├── Stage1Probe.kt           # TCP/TLS handshake سریع
│   ├── Stage2Verifier.kt        # تست واقعی از طریق Xray
│   └── ScanRepository.kt
├── routing/
│   ├── RoutingRules.kt          # قوانین Xray (geoip:ir → direct)
│   ├── DnsConfig.kt
│   └── PerAppManager.kt         # allow/disallow apps
└── ui/                          # صفحه‌ها: Home, Nodes, Scanner, Routing, Settings
```

### جریان اتصال
1. کاربر یک Node انتخاب می‌کند ← `XrayConfigBuilder` JSON می‌سازد (inbound: socks محلی، outbound: Node، routing: قوانین ایران).
2. `TunService` رابط TUN باز می‌کند ← `hev-socks5-tunnel` ترافیک را به socks محلی Xray می‌دهد.
3. Xray بر اساس routing تصمیم می‌گیرد: `geoip:ir` و `geosite:category-ir` ← direct، بقیه ← proxy.
4. `VpnService.Builder` با `addDisallowedApplication` (یا `addAllowedApplication`) برنامه‌های Split tunnel را اعمال می‌کند. خود برنامه همیشه از تونل مستثنی باشد.

### اسکنر IP تمیز (دو مرحله‌ای)
- **مرحله ۱ (سریع):** هزاران IP از رنج‌ها، با concurrency بالا (مثلاً ۲۰۰–۵۰۰)، TCP connect + TLS handshake با SNI دلخواه روی پورت ۴۴۳، timeout کوتاه. خروجی: لیست IPهایی که جواب دادند + latency.
- **مرحله ۲ (دقیق):** بهترین N تا (مثلاً ۵۰) را با کانفیگ واقعی کاربر تست کن: address کانفیگ را موقتاً با IP کاندید عوض کن (SNI/Host دست‌نخورده)، از طریق Xray یک درخواست واقعی بزن (مثلاً `generate_204`) و latency + موفقیت را بسنج. خروجی: رتبه‌بندی نهایی.
- کاربر بتواند IP برتر را با یک تپ روی کانفیگ اعمال یا ذخیره کند.

---

## بخش ۲ — نقشهٔ راه (فاز به فاز)

| فاز | کار | معیار قبولی |
|---|---|---|
| ۰ | اسکلت پروژه، Compose، Room، تم، ناوبری | `./gradlew assembleDebug` سبز؛ اپ باز می‌شود |
| ۱ | Parserها (vless/vmess/trojan/ss، base64 sub)، import از clipboard و QR | لینک نمونه‌ها درست پارس و در لیست نمایش داده می‌شوند |
| ۲ | هسته: XrayConfigBuilder + XrayController + TunService | اتصال به یک نود واقعی و باز شدن سایت خارجی |
| ۳ | تست نودها: TCP ping + real delay + تست دسته‌ای + مرتب‌سازی | لیست نودها با latency و رتبه |
| ۴ | مدیریت اشتراک: افزودن، آپدیت دستی/خودکار، حذف | آپدیت اشتراک نودها را به‌روز می‌کند |
| ۵ | مسیریابی ایران + DNS + geo files | سایت ایرانی direct، سایت خارجی از پروکسی (با لاگ تأیید) |
| ۶ | Split tunnel per-app | برنامهٔ انتخاب‌شده از تونل خارج می‌شود |
| ۷ | اسکنر مرحله ۱ | اسکن یک رنج /24 و نمایش نتیجه با progress |
| ۸ | اسکنر مرحله ۲ + اعمال IP روی کانفیگ | رتبه‌بندی نهایی و اعمال با یک تپ |
| ۹ | Quick Settings Tile، notification کنترل، بکاپ/ریستور، پولیش | تست دستی کامل روی گوشی |

ریسک‌های اصلی: (۱) ادغام `libv2ray` و tun2socks (فاز ۲ سخت‌ترین است)، (۲) ناپایداری اتصال در پس‌زمینه با Doze/باتری، (۳) حجم geo files. هر فاز را جدا تست کن و نرو فاز بعد تا قبلی کار نکند.

---

## بخش ۳ — مستر پرامپت برای ایجنت کدنویس

```
You are a senior Android engineer. Build a personal, single-user Android VPN app in Kotlin + Jetpack Compose (minSdk 24, targetSdk 34, compileSdk 36). Work autonomously, phase by phase, and keep the project compiling at all times.

## Product
A personal VPN client with three v1 features:
1. V2Ray/Xray client: import nodes from vless://, vmess://, trojan://, ss:// links, base64 subscription URLs, QR codes and clipboard. Support transports TCP, WebSocket, gRPC, XHTTP and security TLS and REALITY. Manage subscriptions (add, manual update, auto-update, delete). Test nodes (TCP ping + real-delay test through the core, batch test, sort by latency). Connect/disconnect with one tap.
2. Clean-IP scanner (two-stage): Stage 1 = fast concurrent TCP+TLS handshake probe over IP ranges (configurable concurrency, timeout, SNI, port, user-supplied ranges plus a built-in Cloudflare range list). Stage 2 = verify the top N candidates by temporarily substituting the candidate IP as the address of the user's chosen config (SNI/Host unchanged) and doing a real request through the core. Show ranked results; one-tap apply/save the best IP to a config.
3. Routing and split tunnel: Iran routing (geoip:ir and geosite:category-ir go direct, everything else through the proxy) using geoip.dat/geosite.dat; DNS config (remote DoH for proxied traffic, local/direct DNS for Iranian domains); per-app split tunnel (choose which apps bypass or use the VPN) via VpnService addAllowedApplication/addDisallowedApplication. The app itself must always be excluded from its own tunnel.

## Architecture (follow strictly)
- Core: Xray-core via the AndroidLibXrayLite (libv2ray) AAR. Look up the latest stable release and its API before coding; do not guess the API.
- TUN bridge: hev-socks5-tunnel (tun2socks) connecting the VpnService TUN fd to the local Xray SOCKS inbound.
- Storage: Room for nodes/subscriptions/scan results, DataStore for settings.
- Services: VpnService in a foreground service with a persistent notification, plus a Quick Settings Tile.
- Package layout: core/, data/ (model, db, parser), scanner/, routing/, ui/. Keep each class single-purpose.
- UI: Compose, dark theme, bottom navigation: Home, Nodes, Scanner, Routing, Settings. Support Persian (RTL) and English; default to Persian.
- Reference open-source projects for ideas only (v2rayNG, the public mlmvpn_android repo). Do not copy code verbatim; respect licenses (GPL-3.0 for v2rayNG).
- Large binaries (.aar, .so, .dat) must not be committed as normal files; download them in a documented setup script or use Git LFS.

## Phases (do them in order; stop and report after each)
0. Project skeleton, theme, navigation, Room, DataStore.
1. Link/subscription parsers with unit tests (include sample links for every protocol/transport) + import via clipboard and QR.
2. XrayConfigBuilder, XrayController, TunService: a working connection to a real node.
3. Node testing: TCP ping, real delay, batch, sort.
4. Subscription management.
5. Iran routing + DNS + geo files, with a debug log screen that shows which outbound each domain used.
6. Per-app split tunnel with a searchable app picker.
7. Scanner stage 1 with progress, cancel, and result list.
8. Scanner stage 2 + apply IP to config.
9. Quick Settings Tile, notification actions, backup/restore (JSON export/import), polish.

## Rules
- After every phase: `./gradlew assembleDebug` must pass; fix all warnings that are real bugs; commit with a clear message; then summarize what works, what is untested, and what you need from me.
- Never leave TODO stubs that pretend to work. If something cannot be done, say so explicitly.
- Handle failures visibly (snackbar/log), never swallow exceptions silently.
- Handle Android lifecycle: VPN must survive app swipe-away, reconnect after network change, and request battery-optimization exemption politely.
- No analytics, no telemetry, no crash reporting that sends data anywhere. Everything stays on device.
- Ask me before adding any dependency that is not in the architecture list.
- Start by listing the exact versions of libv2ray, hev-socks5-tunnel and the Android Gradle Plugin you will use and why. Then begin Phase 0.
```

---

## نکته‌های عملی
- فاز ۲ را اگر ایجنت گیر کرد، بگو ساختار `AndroidLibXrayLite` و `TProxyService` در v2rayNG را مطالعه کند و همان الگو را با کد خودش پیاده کند.
- هر فاز که تمام شد، APK را روی گوشی واقعی تست کن؛ مشکل VPN در امولاتور معمولاً نشان داده نمی‌شود.
- مرحله ۲ اسکنر به تست روی خود شبکهٔ تو بستگی دارد؛ نتیجه برای هر اپراتور فرق می‌کند.
