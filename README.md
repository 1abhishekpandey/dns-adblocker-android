# AdBlocker 🛡️

A lightweight, VPN-based DNS filtering Android application that blocks Google Ads across all apps without requiring root access.

## 📋 Table of Contents

- [Features](#-features)
- [Screenshots](#-screenshots)
- [Requirements](#-requirements)
- [Installation](#-installation)
- [Usage](#-usage)
- [How It Works](#-how-it-works)
- [Blocked Domains](#-blocked-domains)
- [Limitations](#-limitations)
- [Tech Stack](#-tech-stack)
- [Architecture](#-architecture)
- [Development](#-development)
- [Logging](#-logging)
- [Contributing](#-contributing)
- [License](#-license)

## ✨ Features

- **System-Wide Ad Blocking**: Blocks Google Ads across all apps, browsers, and games
- **VPN-Based DNS Filtering**: Intercepts and filters DNS requests at the network layer
- **No Root Required**: Works on any Android device without special permissions
- **Lightweight**: Minimal resource usage with efficient packet processing
- **Privacy-Focused**: All filtering happens locally on your device
- **Real-time Blocking**: Instant DNS request filtering with no noticeable latency
- **Material Design 3**: Modern, beautiful UI built with Jetpack Compose
- **Persistent Notification**: Always know when ad blocking is active

## 📱 Screenshots

_Coming soon! Screenshots will showcase:_
- Main screen with VPN toggle
- Active blocking notification
- Statistics dashboard

## 📋 Requirements

- **Android Version**: 7.0 (Nougat) or higher (API Level 25+)
- **Permissions**: VPN permission (requested at runtime)
- **Root Access**: Not required
- **Storage**: ~5 MB

## 📥 Installation

### Option 1: Install from APK (Recommended for Users)

1. Download the latest APK from the [Releases](https://github.com/yourusername/AdBlocker/releases) page
2. Enable "Install from Unknown Sources" in your Android settings
3. Open the downloaded APK file and follow the installation prompts
4. Grant VPN permission when prompted

### Option 2: Build from Source (For Developers)

See the [Development](#-development) section below.

## 🚀 Usage

### Starting Ad Blocking

1. Launch the AdBlocker app
2. Tap the **"Start VPN"** button on the main screen
3. Accept the VPN connection request dialog
4. You'll see a persistent notification indicating that ad blocking is active
5. Browse apps and websites - Google Ads will be automatically blocked

### Stopping Ad Blocking

1. Open the AdBlocker app or tap the persistent notification
2. Tap the **"Stop VPN"** button on the main screen
3. The VPN connection and ad blocking will stop immediately

### What to Expect

- **Blocked Ads**: Google AdSense, DoubleClick, and Google Ad Services will be blocked
- **Notification**: A persistent notification shows when the VPN is active
- **Battery Usage**: Minimal battery impact due to efficient packet processing
- **Network Speed**: No noticeable impact on browsing or app performance

## 🔧 How It Works

AdBlocker uses a **local VPN service** to intercept DNS requests from all apps on your device:

1. **VPN Interface**: Creates a local VPN tunnel (10.0.0.2) that routes all network traffic
2. **Packet Capture**: Intercepts IP packets from the Android VPN interface
3. **DNS Filtering**: Parses DNS queries and checks them against the blocklist
4. **Selective Blocking**:
   - Blocked domains receive a DNS response pointing to `0.0.0.0` (null route)
   - Legitimate domains are forwarded to Google DNS (8.8.8.8) for resolution
5. **Response Handling**: Returns DNS responses to apps through the VPN tunnel

**Technical Flow:**
```
App DNS Request → VPN Interface → IP Packet Parser → DNS Request Parser
→ Domain Check → [Blocked? Return 0.0.0.0 : Forward to 8.8.8.8]
→ DNS Response Builder → Packet Writer → VPN Interface → App
```

**Key Components:**
- `AdBlockerVpnService`: Manages the VPN connection and packet processing loop
- `DnsPacketHandler`: Coordinates DNS request/response handling
- `IpPacketParser`: Extracts UDP DNS packets from IP packets
- `DnsRequestParser`: Parses DNS queries using dnsjava library
- `DnsResponseBuilder`: Constructs DNS responses for blocked/allowed domains
- `BlockedDomains`: Maintains the list of blocked advertising domains

## 🚫 Blocked Domains

AdBlocker currently blocks **8 major Google advertising domains**:

| Domain | Description |
|--------|-------------|
| `pagead2.googlesyndication.com` | Google AdSense ads |
| `googleads.g.doubleclick.net` | Google Ads via DoubleClick |
| `ad.doubleclick.net` | DoubleClick ad server |
| `pubads.g.doubleclick.net` | Google Publisher Ads |
| `googleadservices.com` | Google Ad Services |
| `tpc.googlesyndication.com` | Third-party content syndication |
| `partner.googleadservices.com` | Partner ad services |
| `adservices.google.com` | Google ad services platform |

### Blocking Logic

- **Exact Match**: `pagead2.googlesyndication.com` is blocked
- **Subdomain Match**: `api.pagead2.googlesyndication.com` is also blocked
- **Case Insensitive**: Matches are normalized to lowercase

## ⚠️ Limitations

### What AdBlocker Cannot Block

- **YouTube Video Ads**: YouTube serves ads from the same domain as videos, making DNS-based blocking ineffective
- **In-App Rewarded Ads**: Some apps may detect VPN usage and disable features
- **Non-Google Ads**: Only Google advertising networks are currently blocked
- **HTTPS Encrypted Content**: Cannot inspect or modify encrypted traffic

### General Limitations

- **One VPN at a Time**: Android allows only one VPN connection active at a time (cannot use AdBlocker with another VPN simultaneously)
- **VPN Permission Required**: Users must grant VPN permission for the app to function
- **Battery Usage**: Minimal but measurable battery usage due to continuous packet processing
- **Network Indicator**: VPN icon will appear in the status bar when active

### Future Enhancements

_Planned features to address limitations:_
- Support for additional ad networks (Facebook Audience Network, Unity Ads, etc.)
- Customizable blocklists with import/export functionality
- Statistics dashboard showing blocked requests and bandwidth saved
- Whitelist functionality for trusted domains

## 🛠 Tech Stack

### Core Technologies

- **Language**: [Kotlin](https://kotlinlang.org/) 1.9+
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3
- **Minimum SDK**: Android 7.0 (API 25) / Target SDK: Android 14 (API 36)
- **Build System**: Gradle with Kotlin DSL

### Key Libraries

| Library | Version | Purpose |
|---------|---------|---------|
| [dnsjava](https://github.com/dnsjava/dnsjava) | Latest | DNS message parsing and construction |
| [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) | Latest | Asynchronous packet processing |
| [DataStore Preferences](https://developer.android.com/topic/libraries/architecture/datastore) | Latest | VPN state persistence |
| [Lifecycle Components](https://developer.android.com/topic/libraries/architecture/lifecycle) | Latest | ViewModel and service lifecycle management |
| [Compose BOM](https://developer.android.com/jetpack/compose/bom) | Latest | Jetpack Compose libraries |

### Architecture Components

- **VpnService**: Android's VPN API for packet interception
- **ParcelFileDescriptor**: TUN interface for reading/writing packets
- **Foreground Service**: Keeps VPN running with persistent notification
- **ViewModel**: UI state management with Compose
- **Repository Pattern**: Data layer abstraction for preferences

## 🏗 Architecture

### High-Level Overview

```
┌─────────────────────────────────────────────────────────────┐
│                        Presentation Layer                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  MainActivity │  │  MainScreen  │  │ MainViewModel│      │
│  │   (Compose)  │◄─┤  (Compose)   │◄─┤   (State)    │      │
│  └──────────────┘  └──────────────┘  └──────┬───────┘      │
└───────────────────────────────────────────────┼──────────────┘
                                               │
┌───────────────────────────────────────────────┼──────────────┐
│                        Domain Layer          │               │
│  ┌──────────────┐  ┌──────────────────────┐ │               │
│  │  VpnState    │  │ VpnPreferencesRepo   │◄┘               │
│  │   (Model)    │  │   (Repository)       │                 │
│  └──────────────┘  └──────────────────────┘                 │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                         VPN Service Layer                    │
│  ┌────────────────────────────────────────────────────┐     │
│  │          AdBlockerVpnService (Foreground)          │     │
│  │  ┌──────────────┐  ┌──────────────────────────┐   │     │
│  │  │ VPN Interface│  │  Packet Processing Loop  │   │     │
│  │  │ (TUN Device) │  │   (Coroutines/IO)        │   │     │
│  │  └──────┬───────┘  └───────────┬──────────────┘   │     │
│  └─────────┼──────────────────────┼──────────────────┘     │
│            │                       │                         │
│  ┌─────────▼───────────────────────▼──────────────────┐     │
│  │           DnsPacketHandler (Coordinator)           │     │
│  │  ┌──────────────┐  ┌──────────────┐  ┌─────────┐  │     │
│  │  │ IpPacket     │  │ UdpPacket    │  │ Packet  │  │     │
│  │  │ Parser       │→ │ Parser       │→ │ Writer  │  │     │
│  │  └──────────────┘  └──────────────┘  └─────────┘  │     │
│  │  ┌──────────────┐  ┌──────────────┐               │     │
│  │  │ DnsRequest   │  │ DnsResponse  │               │     │
│  │  │ Parser       │→ │ Builder      │               │     │
│  │  └──────────────┘  └──────────────┘               │     │
│  └────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                          Data Layer                          │
│  ┌──────────────────┐  ┌──────────────────────────────┐     │
│  │ BlockedDomains   │  │  External DNS (8.8.8.8)      │     │
│  │  (Static List)   │  │  (Google Public DNS)         │     │
│  └──────────────────┘  └──────────────────────────────┘     │
└─────────────────────────────────────────────────────────────┘
```

### Layer Responsibilities

1. **Presentation Layer**: User interface and interaction handling
2. **Domain Layer**: Business logic and data models (VPN state, preferences)
3. **VPN Service Layer**: Packet capture, DNS filtering, and response generation
4. **Data Layer**: Blocklist storage and upstream DNS resolution

### Design Patterns

- **Repository Pattern**: Abstracts data access for VPN preferences
- **Builder Pattern**: VPN interface configuration
- **Object Pool Pattern**: ByteBuffer reuse for packet processing
- **Observer Pattern**: StateFlow for reactive UI updates
- **Singleton Pattern**: BlockedDomains and Logger utilities

## 💻 Development

### Prerequisites

- Android Studio Ladybug or later
- JDK 11 or later
- Android SDK with API 25+ installed
- Git for version control

### Building from Source

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/AdBlocker.git
   cd AdBlocker
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned repository

3. **Sync Gradle dependencies**
   ```bash
   ./gradlew build
   ```

4. **Run on device/emulator**
   - Connect an Android device via USB or start an emulator
   - Click the "Run" button (Shift + F10) in Android Studio
   - Select your target device

### Build Variants

- **Debug**: Development build with full logging
  ```bash
  ./gradlew assembleDebug
  ```

- **Release**: Production build with optimizations
  ```bash
  ./gradlew assembleRelease
  ```

### Running Tests

```bash
# Unit tests
./gradlew test

# Instrumented tests (requires connected device)
./gradlew connectedAndroidTest
```

### Code Structure

```
app/src/main/java/com/abhishek/adblocker/
├── data/
│   ├── blocklist/
│   │   └── BlockedDomains.kt          # Ad domain blocklist
│   └── preferences/
│       └── VpnPreferencesRepository.kt # State persistence
├── domain/
│   └── model/
│       └── VpnState.kt                 # Domain model
├── ui/
│   ├── screens/
│   │   └── MainScreen.kt               # Compose UI
│   ├── theme/
│   │   ├── Color.kt                    # Material colors
│   │   ├── Theme.kt                    # Theme configuration
│   │   └── Type.kt                     # Typography
│   └── viewmodels/
│       └── MainViewModel.kt            # UI state management
├── vpn/
│   ├── dns/
│   │   ├── DnsPacketHandler.kt         # DNS processing coordinator
│   │   ├── DnsRequestParser.kt         # Parse DNS queries
│   │   └── DnsResponseBuilder.kt       # Build DNS responses
│   ├── packet/
│   │   ├── IpPacketParser.kt           # IP packet parsing
│   │   ├── UdpPacketParser.kt          # UDP packet parsing
│   │   └── PacketWriter.kt             # Packet writing utility
│   └── AdBlockerVpnService.kt          # VPN service implementation
├── util/
│   └── Logger.kt                       # Logging utility
└── MainActivity.kt                     # App entry point
```

## 📊 Logging

AdBlocker includes comprehensive logging for debugging and monitoring:

### Log Tag

All logs use the tag: **`AdBlockerApp`**

### Viewing Logs

```bash
# View all AdBlocker logs
adb logcat -s AdBlockerApp

# View logs with timestamps
adb logcat -v time -s AdBlockerApp

# Save logs to file
adb logcat -s AdBlockerApp > adblocker.log
```

### Log Levels

- **DEBUG**: Packet processing details, DNS queries
- **INFO**: VPN lifecycle events, service state changes
- **WARN**: Non-critical issues, retries
- **ERROR**: Failures, exceptions with stack traces

### Key Log Events

| Event | Log Level | Example Message |
|-------|-----------|-----------------|
| VPN Started | INFO | `VPN service started successfully` |
| VPN Stopped | INFO | `VPN service stopped` |
| Domain Blocked | DEBUG | `DNS query blocked: pagead2.googlesyndication.com` |
| DNS Forwarded | DEBUG | `DNS query forwarded: example.com` |
| Packet Error | ERROR | `Error reading packet from TUN: [exception]` |

## 🤝 Contributing

Contributions are welcome! Here's how you can help:

### Reporting Bugs

1. Check if the bug has already been reported in [Issues](https://github.com/yourusername/AdBlocker/issues)
2. Create a new issue with:
   - Clear description of the bug
   - Steps to reproduce
   - Expected vs actual behavior
   - Android version and device model
   - Relevant logcat output (use tag: `AdBlockerApp`)

### Suggesting Enhancements

1. Open an issue with the "enhancement" label
2. Describe the feature and its use case
3. Explain how it benefits users

### Pull Requests

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Follow the existing code style and architecture
4. Add tests for new functionality
5. Ensure all tests pass (`./gradlew test`)
6. Commit your changes with clear messages
7. Push to your fork (`git push origin feature/amazing-feature`)
8. Open a Pull Request with a detailed description

### Code Style

- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable and function names
- Add KDoc comments for public APIs
- Keep functions focused and under 30 lines where possible
- Maximum cognitive complexity: 15 per function

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

**Disclaimer**: This app is for educational purposes and personal use. Users are responsible for complying with terms of service of apps and websites they visit. Ad blocking may affect content creators who rely on ad revenue.

**Made with ❤️ by Abhishek**

