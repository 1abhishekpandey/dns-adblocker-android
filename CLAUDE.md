# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# AdBlocker - VPN-Based Network Monitor for Android

VPN-based DNS filtering and network monitoring app. Creates local VPN tunnel to intercept DNS requests, filter ad domains, and monitor network traffic. Currently focused on ad blocking, evolving into comprehensive network monitoring tool.

## Architecture

Clean architecture with four distinct layers:

```
UI Layer (ui/)         → Jetpack Compose screens, ViewModels
Domain Layer (domain/) → VpnState model, business logic
Data Layer (data/)     → BlockedDomains, Preferences (DataStore)
VPN Layer (vpn/)       → VpnService, packet processing, DNS handling
```

### Packet Processing Pipeline

```
TUN Interface → IP Parser → UDP Parser → DNS Parser → Domain Check
                                                            ↓
App ← Packet Writer ← DNS Response Builder ← [Block/Forward Decision]
```

**Threading Model:**
- Main thread: UI updates, Compose recomposition
- IO Coroutine: Continuous packet processing loop (AdBlockerVpnService)
- Per-request threads: DNS forwarding to upstream (8.8.8.8)

**Key Technical Details:**
- VPN tunnel address: 10.0.0.2/32
- Upstream DNS: 8.8.8.8 (Google Public DNS)
- Packet buffer: 32KB
- DNS timeout: 5 seconds
- Uses `dnsjava` library for DNS message parsing

## Critical Files

| File | Lines | Purpose |
|------|-------|---------|
| `vpn/AdBlockerVpnService.kt` | 221 | VPN service lifecycle, TUN interface setup, packet I/O loop, foreground notification |
| `vpn/dns/DnsPacketHandler.kt` | 87 | Orchestrates DNS processing pipeline, coordinates parsers, blocking decisions |
| `vpn/packet/IpPacketParser.kt` | 93 | Extracts IP headers (source/dest IPs, protocol), validates IPv4 |
| `vpn/packet/UdpPacketParser.kt` | 64 | Extracts UDP headers (source/dest ports), isolates DNS payload |
| `vpn/dns/DnsRequestParser.kt` | 32 | Parses DNS queries using dnsjava, extracts hostnames |
| `vpn/dns/DnsResponseBuilder.kt` | 33 | Builds NXDOMAIN responses for blocked domains |
| `vpn/packet/PacketWriter.kt` | 63 | Reconstructs IP/UDP/DNS packets, calculates checksums |
| `data/blocklist/BlockedDomains.kt` | 51 | Blocklist (~22 domains), subdomain matching logic |
| `ui/viewmodels/MainViewModel.kt` | 99 | VPN state management, permission handling, service control |
| `ui/screens/MainScreen.kt` | 172 | Compose UI: VPN toggle, status display, blocked counter |
| `util/Logger.kt` | 94 | Centralized logging with emoji indicators (tag: AdBlockerApp) |

## Common Development Tasks

### Modify Blocklist
Edit `data/blocklist/BlockedDomains.kt`:
```kotlin
private val domains = setOf(
    "new-ad-domain.com",  // Add domains here
    // Supports exact match and subdomain matching
)
```

### Change Upstream DNS Server
Edit `vpn/dns/DnsPacketHandler.kt`:
```kotlin
private val upstreamDnsServer = InetAddress.getByName("1.1.1.1")  // Cloudflare
```

### Add Network Monitoring Features
- Packet capture already implemented in `AdBlockerVpnService.processPackets()`
- DNS requests accessible in `DnsPacketHandler.processDnsPacket()`
- For non-DNS traffic monitoring: extend `IpPacketParser` to handle TCP packets
- Add statistics repository in `data/` layer for tracking metrics

## Build & Development

```bash
# Build variants
./gradlew assembleDebug              # Debug build with full logging
./gradlew assembleRelease            # Release build (requires signing)

# Install on device
./gradlew installDebug               # Install debug APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Testing
./gradlew test                       # Unit tests
./gradlew connectedAndroidTest       # Instrumented tests (device required)

# Clean build
./gradlew clean build
```

## Debugging

### Logcat Filters
```bash
# All app logs (tag: AdBlockerApp)
adb logcat -s AdBlockerApp

# Blocked domains only
adb logcat -s AdBlockerApp | grep BLOCKED

# DNS queries (allowed + blocked)
adb logcat -s AdBlockerApp | grep "DNS query"

# VPN lifecycle events
adb logcat -s AdBlockerApp | grep "VPN service"

# Packet processing errors
adb logcat -s AdBlockerApp:E

# Save logs to file with timestamps
adb logcat -v time -s AdBlockerApp > debug.log
```

### Verify VPN Status
```bash
# Check if VPN interface exists
adb shell ifconfig tun0

# Check active VPN
adb shell dumpsys connectivity | grep -A 5 "Active network"
```

## Technical Constraints & Limitations

### Android VPN API Limitations
- **Single VPN:** Only one VPN connection allowed system-wide
- **Permission Required:** User must grant VPN permission via Android's VpnService.prepare()
- **Foreground Service:** Must run as foreground service with persistent notification

### DNS-Level Blocking Limitations
- **Same-Domain Content:** Cannot block YouTube ads (ads and videos from same domain)
- **No HTTPS Inspection:** Cannot inspect or modify encrypted traffic payloads
- **DNS-Only:** Only intercepts DNS (port 53/UDP); other traffic passes through unmodified

### Future Network Monitoring Capabilities
- **TCP Support:** Parser infrastructure ready for TCP packet inspection
- **Traffic Stats:** Can track bandwidth per app/domain
- **Protocol Analysis:** Can identify protocols beyond DNS (HTTP, HTTPS, etc.)

## Git Commit Workflow

**CRITICAL: Follow these steps in order when creating commits:**

1. **Review changes**: Run `git status` and `git diff` to understand what changed
2. **Update FEATURES.md FIRST** (if applicable):
   - If changes add/modify/remove ANY user-facing features → update `docs/FEATURES.md`
   - Add new features to appropriate sections
   - Update existing feature descriptions
   - Remove deprecated features
   - Commit FEATURES.md separately or with the feature changes
3. **Draft commit message**: Follow conventional commits format (feat:, fix:, docs:, etc.)
4. **Create commit**: Stage files and commit with descriptive message

**Examples of changes requiring FEATURES.md updates:**
- ✅ New UI components or screens
- ✅ New filtering, sorting, or search capabilities
- ✅ Modified app selection behavior
- ✅ New VPN configuration options
- ✅ Changed blocking logic or statistics
- ❌ Code refactoring without behavior changes
- ❌ Bug fixes that restore intended behavior
- ❌ Internal implementation changes

## Additional Documentation

Comprehensive documentation in `/docs`:
- **FEATURES.md** - User-facing feature list (update with every feature change!)
- **ARCHITECTURE.md** - Detailed architecture, data flow diagrams, threading model
- **DEVELOPMENT.md** - Development setup, code style guidelines, testing strategy
- **DEBUGGING.md** - Logger usage, logcat filtering, troubleshooting guide
- **README.md** - User documentation, features, installation instructions

## Important Notes

### VPN Service Lifecycle
1. User toggles switch → MainViewModel checks VPN permission
2. If permitted → startService(ACTION_START) → AdBlockerVpnService.onStartCommand()
3. Service creates TUN interface at 10.0.0.2/32
4. Continuous packet processing loop in IO coroutine
5. Each packet: Read → Parse → Process → Respond → Write back

### DNS Processing Decision Flow
```
DNS Query → Extract hostname → Check BlockedDomains
    ↓                                    ↓
BLOCKED: Return NXDOMAIN (0.0.0.0)      ALLOWED: Forward to 8.8.8.8
```

### When Extending for Network Monitoring
- Packet capture infrastructure is reusable
- Add protocol-specific parsers in `vpn/packet/` (e.g., TcpPacketParser)
- Create statistics tracking in `data/stats/` layer
- UI updates via StateFlow in ViewModel
- Consider performance impact of inspecting all traffic (not just DNS)