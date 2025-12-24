# AdBlocker

VPN-based DNS filtering app that blocks Google Ads across all Android apps without requiring root access.

## Features

- System-wide Google Ads blocking (AdSense, DoubleClick, Ad Services)
- VPN-based DNS filtering with local processing
- No root access required
- Minimal battery impact
- Material Design 3 UI with Jetpack Compose

## Requirements

- Android 7.0+ (API 25+)
- VPN permission (requested at runtime)
- ~5 MB storage

## Installation

### From APK
1. Download from [Releases](https://github.com/yourusername/AdBlocker/releases)
2. Enable "Install from Unknown Sources"
3. Install APK and grant VPN permission

### From Source
```bash
git clone https://github.com/yourusername/AdBlocker.git
cd AdBlocker
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Usage

1. Launch app and tap "Start VPN"
2. Accept VPN connection dialog
3. Google Ads are now blocked system-wide
4. Tap "Stop VPN" to disable

## How It Works

Creates a local VPN tunnel (10.0.0.2) to intercept DNS requests:
- Blocked domains → returns `0.0.0.0` (NXDOMAIN)
- Legitimate domains → forwards to Google DNS (8.8.8.8)

**Blocked Domains:**
- `pagead2.googlesyndication.com` (AdSense)
- `googleads.g.doubleclick.net` (DoubleClick)
- `ad.doubleclick.net`, `pubads.g.doubleclick.net`
- `googleadservices.com`, `partner.googleadservices.com`
- `tpc.googlesyndication.com`, `adservices.google.com`

Supports exact and subdomain matching (case-insensitive).

## Limitations

**Cannot block:**
- YouTube video ads (same domain as videos)
- Non-Google ad networks
- HTTPS encrypted content inspection

**General:**
- Only one VPN active at a time
- VPN icon appears in status bar
- Minimal battery usage from continuous packet processing

## Future Vision

This project is evolving into a comprehensive network monitoring and analysis tool. Planned features include:

- Real-time traffic monitoring and analytics
- Per-app bandwidth tracking and network behavior profiles
- Threat detection (malicious domains, C2 servers, DNS tunneling)
- Privacy protection (tracker blocking, DNS leak prevention)
- Firewall capabilities with granular filtering rules

See [ROADMAP.md](docs/ROADMAP.md) for the complete vision and technical implementation plan.

## Tech Stack

- **Kotlin** 1.9+ with Jetpack Compose
- **dnsjava** - DNS parsing
- **Kotlin Coroutines** - async packet processing
- **DataStore** - state persistence
- **VpnService API** - packet interception

## Architecture

Clean architecture with four layers:

```
UI Layer (Compose)
  ↓
Domain Layer (VpnState, Repository)
  ↓
VPN Service Layer (AdBlockerVpnService, DNS handling)
  ↓
Data Layer (BlockedDomains, upstream DNS)
```

**Packet Flow:**
```
TUN Interface → IP Parser → UDP Parser → DNS Parser → Domain Check
                                                            ↓
App ← Packet Writer ← DNS Response ← [Block NXDOMAIN / Forward to 8.8.8.8]
```

See [CLAUDE.md](.claude/CLAUDE.md) for detailed architecture and development guide.

## Development

**Prerequisites:** Android Studio Ladybug+, JDK 11+, Android SDK API 25+

**Build:**
```bash
./gradlew assembleDebug      # Debug build with logging
./gradlew assembleRelease    # Release build
./gradlew test               # Run tests
```

**Debugging:**
```bash
adb logcat -s AdBlockerApp              # View all logs
adb logcat -s AdBlockerApp | grep BLOCKED  # View blocked domains
```

**Code structure:**
```
app/src/main/java/com/abhishek/adblocker/
├── vpn/          # VPN service, DNS/packet handling
├── ui/           # Compose screens, ViewModels
├── data/         # BlockedDomains, Preferences
├── domain/       # VpnState model
└── util/         # Logger
```

## Contributing

1. Fork repository and create feature branch
2. Follow [Kotlin conventions](https://kotlinlang.org/docs/coding-conventions.html)
3. Add tests and ensure `./gradlew test` passes
4. Submit PR with clear description

**Bug reports:** Include Android version, device model, and `adb logcat -s AdBlockerApp` output.

## License

MIT License - see [LICENSE](LICENSE) file.

**Disclaimer:** For educational and personal use. Users are responsible for complying with app/website terms of service. Ad blocking may impact content creators relying on ad revenue.

---

Made with ❤️ by Vibe Coding!

