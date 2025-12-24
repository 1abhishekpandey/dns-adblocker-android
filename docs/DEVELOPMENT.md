# Development Guide

This guide provides comprehensive instructions for developers contributing to the AdBlocker project - a VPN-based ad blocking application for Android.

## Table of Contents
- [Development Setup](#development-setup)
- [Project Structure](#project-structure)
- [Build Instructions](#build-instructions)
- [Code Style Guide](#code-style-guide)
- [Adding New Features](#adding-new-features)
- [Testing Guide](#testing-guide)
- [Debugging](#debugging)
- [Common Tasks](#common-tasks)
- [Git Workflow](#git-workflow)
- [Performance Considerations](#performance-considerations)

---

## Development Setup

### Prerequisites

1. **Android Studio**: Ladybug | 2024.2.1 or later
2. **JDK**: Version 11 or later
3. **Android SDK**:
   - Min SDK: 25 (Android 7.1)
   - Target SDK: 36 (Android 15+)
   - Compile SDK: 36
4. **Gradle**: 8.13.2 (managed by wrapper)
5. **Kotlin**: 2.0.21

### Initial Setup

1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd AdBlocker
   ```

2. Open the project in Android Studio:
   - File > Open > Select the AdBlocker directory
   - Wait for Gradle sync to complete

3. Connect a physical device or start an emulator:
   - Physical device recommended for VPN testing
   - Enable USB debugging on your device
   - Minimum Android 7.1 required

4. Verify setup:
   ```bash
   ./gradlew clean build
   ```

---

## Project Structure

```
AdBlocker/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/abhishek/adblocker/
│   │   │   │   ├── data/              # Data layer
│   │   │   │   │   ├── blocklist/     # Blocked domains management
│   │   │   │   │   └── preferences/   # User preferences (DataStore)
│   │   │   │   ├── domain/            # Domain/Business logic layer
│   │   │   │   │   └── model/         # Domain models (VpnState, etc.)
│   │   │   │   ├── ui/                # Presentation layer
│   │   │   │   │   ├── screens/       # Composable screens
│   │   │   │   │   ├── theme/         # Material3 theme
│   │   │   │   │   └── viewmodels/    # ViewModels
│   │   │   │   ├── util/              # Utilities
│   │   │   │   │   └── Logger.kt      # Centralized logging
│   │   │   │   ├── vpn/               # VPN implementation
│   │   │   │   │   ├── dns/           # DNS packet handling
│   │   │   │   │   ├── packet/        # IP/UDP packet parsing
│   │   │   │   │   └── AdBlockerVpnService.kt
│   │   │   │   └── MainActivity.kt
│   │   │   ├── res/                   # Android resources
│   │   │   └── AndroidManifest.xml
│   │   ├── androidTest/               # Instrumented tests
│   │   └── test/                      # Unit tests
│   ├── build.gradle.kts               # App-level build configuration
│   └── proguard-rules.pro
├── gradle/
│   └── libs.versions.toml             # Dependency versions catalog
├── build.gradle.kts                   # Project-level build config
├── settings.gradle.kts
└── gradlew                            # Gradle wrapper
```

### Architecture Overview

The project follows **Clean Architecture** principles:

1. **Data Layer** (`data/`): Manages data sources
   - `BlockedDomains`: Maintains the ad domain blocklist
   - `VpnPreferencesRepository`: Handles user preferences

2. **Domain Layer** (`domain/`): Business logic and models
   - `VpnState`: Represents VPN connection state

3. **Presentation Layer** (`ui/`): UI components
   - Jetpack Compose-based UI
   - MVVM pattern with ViewModels

4. **VPN Layer** (`vpn/`): Core VPN functionality
   - Packet parsing and filtering
   - DNS request interception
   - Ad blocking logic

---

## Build Instructions

### Gradle Commands

```bash
# Clean build outputs
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run instrumented tests (requires connected device)
./gradlew connectedAndroidTest

# Generate test coverage report
./gradlew testDebugUnitTest jacocoTestReport

# Lint check
./gradlew lint

# Install debug build on connected device
./gradlew installDebug
```

### Build Variants

The project uses standard Android build types:

- **debug**: Development builds with logging enabled
  - Debuggable
  - No code obfuscation
  - Full logging output

- **release**: Production builds
  - Code obfuscation with ProGuard/R8
  - Optimized
  - Reduced logging

### Build Output Locations

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk`

---

## Code Style Guide

This project adheres to strict clean code principles. All contributions must follow these guidelines.

### Core Principles

#### 1. Stepdown Rule
Organize code like a newspaper - high-level concepts first, details below.

```kotlin
// Correct: High-level function appears first
fun processVpnPacket(packet: ByteArray) {
    val ipPacket = parseIpPacket(packet)
    val udpPacket = extractUdpPacket(ipPacket)
    handleDnsQuery(udpPacket)
}

// Supporting functions follow in call order
private fun parseIpPacket(packet: ByteArray): IpPacket { ... }
private fun extractUdpPacket(ip: IpPacket): UdpPacket { ... }
private fun handleDnsQuery(udp: UdpPacket) { ... }
```

#### 2. Single Responsibility
Each class/function should have ONE reason to change.

```kotlin
// Good: Single responsibility
object BlockedDomains {
    fun isBlocked(hostname: String): Boolean { ... }
}

// Bad: Multiple responsibilities
object DomainManager {
    fun isBlocked(hostname: String): Boolean { ... }
    fun logToFile(message: String) { ... }  // Wrong! Separate concern
}
```

#### 3. Cognitive Complexity ≤ 15
Keep functions simple and easy to understand.

```kotlin
// Good: Low complexity with early returns
fun validateDnsQuery(query: DnsQuery): Boolean {
    if (query.hostname.isEmpty()) return false
    if (query.type !in validTypes) return false
    if (query.payload.size > MAX_SIZE) return false
    return true
}

// Bad: High complexity with deep nesting
fun validateDnsQuery(query: DnsQuery): Boolean {
    if (query.hostname.isNotEmpty()) {
        if (query.type in validTypes) {
            if (query.payload.size <= MAX_SIZE) {
                return true
            }
        }
    }
    return false
}
```

#### 4. No Unused Code Policy

**Remove all unused code** - version control preserves history.

Exception: Code for near-term future use must be:
```kotlin
// TODO: [TICKET-123] Add support for IPv6 DNS queries
// Uncomment when IPv6 implementation is ready
// private fun parseIpv6Packet(packet: ByteArray): Ipv6Packet { ... }
```

Before submitting PR, verify:
- [ ] No unused imports
- [ ] No unused variables/parameters
- [ ] No commented-out code blocks
- [ ] No unreachable code
- [ ] No empty catch blocks

### Naming Conventions

```kotlin
// Classes/Objects: Noun phrases
class DnsPacketHandler
object BlockedDomains

// Functions: Verb phrases
fun processDnsPacket()
fun isBlocked()

// Booleans: Question form
val isConnected: Boolean
val hasPermission: Boolean
fun canProcessPacket(): Boolean

// Collections: Plural nouns
val blockedDomains: Set<String>
val dnsQueries: List<DnsQuery>

// Constants: SCREAMING_SNAKE_CASE
const val MAX_PACKET_SIZE = 32767
const val DNS_PORT = 53
```

### Error Handling

```kotlin
// Prefer early returns
fun processDnsPacket(packet: ByteArray): ByteArray? {
    val ipPacket = parseIpPacket(packet) ?: return null
    if (!isValidPacket(ipPacket)) return null

    // Main logic follows
    return buildResponse(ipPacket)
}

// Never swallow exceptions silently
try {
    socket.send(packet)
} catch (e: IOException) {
    Logger.e("Failed to send DNS packet", e)  // Always log or rethrow
    return null
}
```

---

## Adding New Features

### 1. Adding New Blocked Domains

Location: `/app/src/main/java/com/abhishek/adblocker/data/blocklist/BlockedDomains.kt`

```kotlin
object BlockedDomains {
    private val domains = setOf(
        "pagead2.googlesyndication.com",
        "googleads.g.doubleclick.net",
        // Add new domains here
        "newadserver.example.com",
        "analytics.tracking.com"
    )

    fun isBlocked(hostname: String): Boolean {
        val normalized = hostname.lowercase().trimEnd('.')
        return domains.any { blocked ->
            normalized == blocked || normalized.endsWith(".$blocked")
        }
    }
}
```

**Steps:**
1. Research the ad domain to block
2. Add to the `domains` set
3. Test with real traffic (see Testing Guide)
4. Document the source/reason in commit message

**Wildcard Support:**
The current implementation supports subdomain matching:
- `doubleclick.net` blocks `ad.doubleclick.net`, `stats.doubleclick.net`, etc.

### 2. Modifying DNS Filtering Logic

Location: `/app/src/main/java/com/abhishek/adblocker/vpn/dns/DnsPacketHandler.kt`

**Key Methods:**
```kotlin
fun processDnsPacket(ipPacketData: ByteArray): ByteArray? {
    // 1. Parse IP packet
    val ipPacket = IpPacketParser.parse(ipPacketData) ?: return null

    // 2. Extract UDP packet
    val udpPacket = UdpPacketParser.parse(ipPacket.payload) ?: return null

    // 3. Parse DNS query
    val dnsQuery = DnsRequestParser.parseDnsQuery(udpPacket.payload) ?: return null

    // 4. Check blocklist
    return if (BlockedDomains.isBlocked(dnsQuery.hostname)) {
        createBlockedResponse(dnsQuery)
    } else {
        forwardDnsRequest(ipPacket, udpPacket, dnsQuery)
    }
}
```

**To add custom filtering rules:**
```kotlin
// Example: Block by query type
if (dnsQuery.type == DNS_TYPE_AAAA) {
    Logger.d("Blocking IPv6 query: ${dnsQuery.hostname}")
    return createBlockedResponse(dnsQuery)
}

// Example: Whitelist specific domains
val whitelistedDomains = setOf("important-site.com")
if (whitelistedDomains.contains(dnsQuery.hostname)) {
    return forwardDnsRequest(ipPacket, udpPacket, dnsQuery)
}
```

### 3. Changing Upstream DNS Server

Location: `/app/src/main/java/com/abhishek/adblocker/vpn/dns/DnsPacketHandler.kt`

```kotlin
class DnsPacketHandler(private val vpnService: VpnService) {
    // Change this line to use different DNS server
    private val upstreamDnsServer = InetAddress.getByName("8.8.8.8")

    // Popular alternatives:
    // Cloudflare: "1.1.1.1"
    // Quad9: "9.9.9.9"
    // OpenDNS: "208.67.222.222"
}
```

Also update in `/app/src/main/java/com/abhishek/adblocker/vpn/AdBlockerVpnService.kt`:
```kotlin
companion object {
    private const val DNS_SERVER = "8.8.8.8"  // Update here too
}
```

### 4. Adding New Log Types

Location: `/app/src/main/java/com/abhishek/adblocker/util/Logger.kt`

```kotlin
object Logger {
    // Add custom log functions
    fun dnsQueryBlocked(hostname: String, reason: String) {
        i("BLOCKED: $hostname - Reason: $reason")
    }

    fun packetDropped(reason: String, size: Int) {
        w("Packet dropped: $reason (size: $size bytes)")
    }

    fun performanceMetric(operation: String, durationMs: Long) {
        d("PERF: $operation took ${durationMs}ms")
    }
}
```

**Usage:**
```kotlin
Logger.dnsQueryBlocked("malware.com", "Known malicious domain")
Logger.performanceMetric("DNS lookup", 45)
```

### 5. Updating UI

Location: `/app/src/main/java/com/abhishek/adblocker/ui/screens/MainScreen.kt`

**Adding new statistics:**
```kotlin
@Composable
fun MainScreen(viewModel: MainViewModel) {
    // Add state for new statistics
    var blockedCount by remember { mutableStateOf(0) }

    Column {
        StatisticCard(
            label = "Domains Blocked",
            value = blockedCount.toString()
        )

        // Add more UI elements
    }
}
```

**Update ViewModel** (`ui/viewmodels/MainViewModel.kt`):
```kotlin
class MainViewModel : ViewModel() {
    private val _blockedCount = MutableStateFlow(0)
    val blockedCount: StateFlow<Int> = _blockedCount.asStateFlow()

    fun incrementBlockedCount() {
        _blockedCount.value++
    }
}
```

---

## Testing Guide

### Testing VPN Functionality

#### 1. Manual VPN Testing

```bash
# Connect device via ADB
adb devices

# Install debug build
./gradlew installDebug

# Launch app
adb shell am start -n com.abhishek.adblocker/.MainActivity
```

**Test Steps:**
1. Launch the app
2. Tap "Start VPN" button
3. Accept VPN permission dialog
4. Verify notification shows "Ad Blocker is active"
5. Open browser and visit ad-heavy websites
6. Monitor logcat for blocked domains

#### 2. Testing DNS Filtering

**Monitor DNS queries in real-time:**
```bash
# Filter logs for DNS activity
adb logcat -s AdBlockerApp:V

# Filter for blocked domains only
adb logcat -s AdBlockerApp:I | grep "BLOCKED"

# Filter for allowed domains
adb logcat -s AdBlockerApp:V | grep "ALLOWED"
```

**Expected output:**
```
I/AdBlockerApp: 🚫 BLOCKED: pagead2.googlesyndication.com
V/AdBlockerApp: ✅ ALLOWED: www.example.com
V/AdBlockerApp: 🔍 DNS Query: ad.doubleclick.net (type=1)
V/AdBlockerApp: ➡️ Forwarded to 8.8.8.8: api.github.com
```

### Testing with Real Ad-Heavy Websites

**Recommended test sites:**
- News websites (CNN, BBC, Forbes)
- Tech blogs with ads
- Free streaming platforms
- Mobile game websites

**Verification:**
1. Open site with VPN disabled - note ad placements
2. Enable AdBlocker VPN
3. Reload site - ads should fail to load
4. Check logcat for blocked domain entries

### Using ADB and Logcat

```bash
# View all logs with timestamps
adb logcat -v time

# Clear log buffer before test
adb logcat -c

# Save logs to file for analysis
adb logcat -d > vpn_test_logs.txt

# Filter by tag and log level
adb logcat AdBlockerApp:D *:S

# Continuous monitoring with grep
adb logcat | grep -E "BLOCKED|ALLOWED|DNS"
```

### Unit Testing

Location: `/app/src/test/java/com/abhishek/adblocker/`

**Example: Testing BlockedDomains**
```kotlin
class BlockedDomainsTest {
    @Test
    fun testDomainBlocking() {
        assertTrue(BlockedDomains.isBlocked("pagead2.googlesyndication.com"))
        assertTrue(BlockedDomains.isBlocked("sub.doubleclick.net"))
        assertFalse(BlockedDomains.isBlocked("google.com"))
    }

    @Test
    fun testSubdomainMatching() {
        assertTrue(BlockedDomains.isBlocked("stats.doubleclick.net"))
        assertTrue(BlockedDomains.isBlocked("ad.doubleclick.net"))
    }
}
```

Run tests:
```bash
./gradlew test
./gradlew testDebugUnitTest --tests BlockedDomainsTest
```

### Instrumented Testing

Location: `/app/src/androidTest/java/com/abhishek/adblocker/`

```bash
# Run all instrumented tests
./gradlew connectedAndroidTest

# Run specific test class
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.abhishek.adblocker.VpnServiceTest
```

---

## Debugging

### Using Logger Utility

The project uses a centralized Logger with different levels:

```kotlin
import com.abhishek.adblocker.util.Logger

// Set log level (in debug builds)
Logger.currentLevel = Logger.LogLevel.VERBOSE  // Show all logs
Logger.currentLevel = Logger.LogLevel.INFO     // Info and above
Logger.currentLevel = Logger.LogLevel.ERROR    // Errors only

// Basic logging
Logger.v("Verbose message")
Logger.d("Debug message")
Logger.i("Info message")
Logger.w("Warning message")
Logger.e("Error message")

// Logging with exception
try {
    riskyOperation()
} catch (e: Exception) {
    Logger.e("Operation failed", e)
}

// Domain-specific logging
Logger.domainBlocked("ads.example.com")
Logger.domainAllowed("api.github.com")
Logger.dnsQueryReceived("example.com", DNS_TYPE_A)
Logger.dnsForwarded("example.com", "8.8.8.8")

// VPN lifecycle logging
Logger.vpnStarted()
Logger.vpnStopped()
Logger.vpnInterfaceEstablished("10.0.0.2")
Logger.packetProcessingStarted()
```

### Common Issues and Solutions

#### Issue 1: VPN Permission Denied
**Symptoms:** VPN service fails to start, no VPN icon in status bar

**Solution:**
```kotlin
// In MainActivity, check and request permission:
private fun startVpn() {
    val intent = VpnService.prepare(applicationContext)
    if (intent != null) {
        startActivityForResult(intent, VPN_REQUEST_CODE)
    } else {
        startVpnService()
    }
}
```

**Debug steps:**
```bash
# Check logcat for permission errors
adb logcat | grep "VpnService"

# Verify VPN permission in AndroidManifest
grep "BIND_VPN_SERVICE" app/src/main/AndroidManifest.xml
```

#### Issue 2: DNS Queries Not Being Intercepted
**Symptoms:** All domains allowed, nothing blocked

**Debug steps:**
```bash
# 1. Verify VPN interface is established
adb logcat | grep "VPN Interface"

# 2. Check if packets are being received
adb logcat | grep "Packet processing"

# 3. Verify DNS packet parsing
adb logcat | grep "DNS Query"
```

**Common causes:**
- VPN routing not properly configured
- DNS queries going directly to cellular/WiFi DNS
- MTU size mismatch

**Solution:**
```kotlin
// Verify VPN Builder configuration in AdBlockerVpnService.kt
private fun setupVpnInterface() {
    val builder = Builder()
        .addAddress(VPN_ADDRESS, VPN_PREFIX_LENGTH)    // Must be set
        .addRoute(ROUTE_ADDRESS, ROUTE_PREFIX_LENGTH)  // Route all traffic
        .addDnsServer(DNS_SERVER)                      // Override system DNS
        .setMtu(MTU)                                   // Match network MTU
}
```

#### Issue 3: App Crashes on VPN Start
**Symptoms:** App force closes when starting VPN

**Debug:**
```bash
# Get crash stack trace
adb logcat -b crash

# Full logcat with time
adb logcat -v threadtime
```

**Common causes:**
1. Null pointer in packet handling
2. Socket creation failure
3. Thread synchronization issues

**Prevention:**
```kotlin
// Always use null-safe operations
val ipPacket = IpPacketParser.parse(data) ?: return null

// Protect socket operations
try {
    socket = DatagramSocket().apply {
        vpnService.protect(this)  // Critical: prevents routing loop
    }
} catch (e: Exception) {
    Logger.e("Socket creation failed", e)
    return null
}
```

#### Issue 4: High Battery Drain
**Symptoms:** Excessive battery consumption

**Investigation:**
```bash
# Check wake locks
adb shell dumpsys batterystats | grep AdBlocker

# Monitor CPU usage
adb shell top | grep adblocker
```

**Optimization:**
```kotlin
// Use efficient coroutine dispatchers
private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

// Avoid excessive logging in production
if (BuildConfig.DEBUG) {
    Logger.v("Verbose debug info")
}

// Reuse packet buffers
private val packetBuffer = ByteBuffer.allocate(BUFFER_SIZE)
```

### VPN Permission Handling

**Proper flow:**
```kotlin
class MainActivity : ComponentActivity() {
    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startVpnService()
        } else {
            Logger.w("VPN permission denied by user")
        }
    }

    private fun requestVpnPermission() {
        val intent = VpnService.prepare(applicationContext)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            // Already have permission
            startVpnService()
        }
    }
}
```

---

## Common Tasks

### Adding a New Domain to Blocklist

**Quick steps:**
```bash
# 1. Open BlockedDomains.kt
open app/src/main/java/com/abhishek/adblocker/data/blocklist/BlockedDomains.kt

# 2. Add domain to set
# 3. Build and test
./gradlew installDebug

# 4. Verify blocking
adb logcat -s AdBlockerApp:I | grep "BLOCKED"
```

**Full example:**
```kotlin
// Before
private val domains = setOf(
    "pagead2.googlesyndication.com",
    "googleads.g.doubleclick.net"
)

// After
private val domains = setOf(
    "pagead2.googlesyndication.com",
    "googleads.g.doubleclick.net",
    "newadserver.example.com"  // Added
)
```

### Changing Upstream DNS Server

**Files to modify:**
1. `/app/src/main/java/com/abhishek/adblocker/vpn/dns/DnsPacketHandler.kt`
2. `/app/src/main/java/com/abhishek/adblocker/vpn/AdBlockerVpnService.kt`

```kotlin
// DnsPacketHandler.kt
private val upstreamDnsServer = InetAddress.getByName("1.1.1.1")  // Cloudflare

// AdBlockerVpnService.kt
companion object {
    private const val DNS_SERVER = "1.1.1.1"  // Must match
}
```

**Test:**
```bash
./gradlew installDebug
adb logcat | grep "Forwarded to"
# Should show: "➡️ Forwarded to 1.1.1.1: example.com"
```

### Modifying Notification

Location: `/app/src/main/java/com/abhishek/adblocker/vpn/AdBlockerVpnService.kt`

```kotlin
private fun createNotification(): Notification {
    return NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("My Custom Title")           // Change title
        .setContentText("Custom notification text")   // Change text
        .setSmallIcon(R.drawable.custom_icon)         // Change icon
        .setPriority(NotificationCompat.PRIORITY_LOW) // Change priority
        .build()
}
```

**Add action button:**
```kotlin
val stopIntent = Intent(this, AdBlockerVpnService::class.java).apply {
    action = ACTION_STOP
}
val stopPendingIntent = PendingIntent.getService(
    this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE
)

return NotificationCompat.Builder(this, CHANNEL_ID)
    .setContentTitle("Ad Blocker Active")
    .addAction(R.drawable.ic_stop, "Stop", stopPendingIntent)  // Add action
    .build()
```

### Adding New Log Levels

**Define new level:**
```kotlin
// Logger.kt
enum class LogLevel(val priority: Int) {
    VERBOSE(0),
    DEBUG(1),
    INFO(2),
    WARN(3),
    ERROR(4),
    CRITICAL(5),  // New level
    NONE(6)
}

fun critical(message: String, tag: String = TAG) {
    if (currentLevel.priority <= LogLevel.CRITICAL.priority) {
        Log.wtf(tag, message)  // "What a Terrible Failure"
    }
}
```

**Usage:**
```kotlin
Logger.critical("VPN service failed to start - critical error")
```

### Running Code Quality Checks

```bash
# Lint check
./gradlew lint

# View lint report
open app/build/reports/lint-results-debug.html

# Check for unused code
./gradlew app:lintDebug | grep "unused"

# Format code (if using ktlint)
./gradlew ktlintFormat

# Dependency analysis
./gradlew app:dependencies
```

---

## Git Workflow

### Branch Naming Convention

```bash
# Feature branches
git checkout -b feature/add-ipv6-support
git checkout -b feature/custom-blocklists

# Bug fixes
git checkout -b fix/vpn-permission-crash
git checkout -b fix/dns-timeout-handling

# Improvements/Refactoring
git checkout -b refactor/packet-parsing
git checkout -b improve/logging-performance

# Documentation
git checkout -b docs/setup-instructions
```

### Commit Message Format

Follow conventional commits:

```bash
# Format: <type>(<scope>): <subject>

# Feature additions
git commit -m "feat(blocklist): add support for regex domain patterns"

# Bug fixes
git commit -m "fix(vpn): resolve permission handling crash on Android 14"

# Refactoring
git commit -m "refactor(dns): simplify packet parsing logic"

# Documentation
git commit -m "docs(readme): add installation instructions"

# Performance improvements
git commit -m "perf(dns): optimize domain lookup with trie structure"

# Tests
git commit -m "test(blocklist): add unit tests for subdomain matching"

# Chores (build, dependencies)
git commit -m "chore(deps): update kotlin to 2.0.21"
```

**Example commit body:**
```
feat(blocklist): add support for wildcard domain patterns

- Implement wildcard matching (*.example.com)
- Add tests for wildcard patterns
- Update documentation with examples

Resolves: #123
```

### Pull Request Process

1. **Create feature branch:**
   ```bash
   git checkout -b feature/new-feature
   ```

2. **Make changes following code style guide**

3. **Commit with clear messages:**
   ```bash
   git add .
   git commit -m "feat(feature): implement new feature"
   ```

4. **Push to remote:**
   ```bash
   git push origin feature/new-feature
   ```

5. **Create Pull Request:**
   - Use descriptive title
   - Fill out PR template
   - Reference related issues
   - Add screenshots for UI changes

6. **PR Checklist:**
   - [ ] Code follows style guide
   - [ ] No unused code (imports, variables, functions)
   - [ ] Cognitive complexity ≤ 15 per function
   - [ ] Tests added/updated
   - [ ] Documentation updated
   - [ ] Lint passes (`./gradlew lint`)
   - [ ] Builds successfully (`./gradlew build`)

### Code Review Guidelines

**For reviewers:**
- Check adherence to clean code principles
- Verify cognitive complexity limits
- Look for unused code
- Ensure proper error handling
- Validate test coverage

**For authors:**
- Respond to all comments
- Make requested changes promptly
- Keep PR focused (one feature per PR)
- Update PR description if scope changes

---

## Performance Considerations

### Battery Optimization

**Best Practices:**

1. **Efficient Packet Processing:**
```kotlin
// Use IO dispatcher for network operations
private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

// Avoid creating new objects in hot path
private val packetBuffer = ByteBuffer.allocate(BUFFER_SIZE)  // Reuse

// Process packets asynchronously
serviceScope.launch {
    processSinglePacket(packetData, output)
}
```

2. **Minimize Wake Locks:**
```kotlin
// Set VPN interface to non-blocking
.setBlocking(false)

// Don't keep CPU awake unnecessarily
// Use foreground service only when actively filtering
```

3. **Optimize Logging:**
```kotlin
// Reduce logging in production
if (BuildConfig.DEBUG) {
    Logger.v("Verbose packet details")
}

// Use lazy evaluation
Logger.d { "Expensive string building: ${buildComplexString()}" }
```

### Memory Usage

**Avoid memory leaks:**

```kotlin
// Clean up resources properly
override fun onDestroy() {
    super.onDestroy()
    serviceScope.cancel()          // Cancel coroutines
    dnsPacketHandler?.cleanup()    // Close sockets
    vpnInterface?.close()          // Close file descriptors
}

// Use weak references for listeners if needed
private val listeners = WeakHashMap<Listener, Unit>()
```

**Efficient data structures:**

```kotlin
// Use Set for O(1) lookup
private val blockedDomains = setOf(...)  // Not List

// Avoid excessive string operations
val normalized = hostname.lowercase().trimEnd('.')  // Cache if used multiple times
```

### Network Performance

**DNS Query Optimization:**

```kotlin
// Set reasonable socket timeout
socket.soTimeout = 3000  // 3 seconds

// Reuse socket connections
private fun getOrCreateSocket(): DatagramSocket {
    if (upstreamDnsSocket == null || upstreamDnsSocket?.isClosed == true) {
        upstreamDnsSocket = DatagramSocket().apply {
            vpnService.protect(this)  // Prevent routing loop
        }
    }
    return upstreamDnsSocket!!
}
```

**Future optimizations:**
- Implement DNS response caching
- Use connection pooling for upstream DNS
- Consider DNS-over-HTTPS for privacy

### Profiling Tools

```bash
# CPU profiling
# Use Android Studio Profiler: View > Tool Windows > Profiler

# Memory profiling
adb shell dumpsys meminfo com.abhishek.adblocker

# Network profiling
adb shell dumpsys netstats detail full

# Battery stats
adb shell dumpsys batterystats com.abhishek.adblocker

# Method tracing
# Add to code:
Debug.startMethodTracing("vpn_trace")
// ... code to profile
Debug.stopMethodTracing()
# Pull trace: adb pull /sdcard/vpn_trace.trace
```

---

## Additional Resources

### Useful ADB Commands

```bash
# Clear app data
adb shell pm clear com.abhishek.adblocker

# Uninstall app
adb uninstall com.abhishek.adblocker

# Force stop app
adb shell am force-stop com.abhishek.adblocker

# Restart VPN service
adb shell am startservice -a com.abhishek.adblocker.STOP_VPN
adb shell am startservice -a com.abhishek.adblocker.START_VPN

# Simulate network conditions
adb shell cmd connectivity airplane-mode enable
adb shell cmd connectivity airplane-mode disable
```

### Documentation Links

- [Android VpnService](https://developer.android.com/reference/android/net/VpnService)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Android Best Practices](https://developer.android.com/topic/architecture)

---

## Getting Help

- Check existing issues on GitHub
- Review logs with `adb logcat -s AdBlockerApp`
- Use Android Studio debugger with breakpoints
- Profile performance with Android Studio Profiler
- Ask questions in project discussions

---

**Happy Coding!**
