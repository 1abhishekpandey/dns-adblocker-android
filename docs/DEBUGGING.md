# Debugging Guide

## Logger Utility

**Tag:** `AdBlockerApp`

### Basic Usage
```kotlin
Logger.i("Info message")
Logger.d("Debug message")
Logger.w("Warning message")
Logger.e("Error message", exception)
```

### Domain-Specific Helpers
```kotlin
Logger.domainBlocked("example.com")
Logger.domainAllowed("google.com")
Logger.dnsQueryReceived("example.com", type = 1)
Logger.vpnStarted()
Logger.vpnStopped()
```

### Log Levels
```kotlin
Logger.currentLevel = Logger.LogLevel.VERBOSE  // All logs
Logger.currentLevel = Logger.LogLevel.DEBUG    // Debug and above
Logger.currentLevel = Logger.LogLevel.INFO     // Info and above
Logger.currentLevel = Logger.LogLevel.ERROR    // Errors only
Logger.currentLevel = Logger.LogLevel.NONE     // No logs
```

---

## Logcat Commands

```bash
# All app logs
adb logcat -s AdBlockerApp

# Blocked domains only
adb logcat -s AdBlockerApp | grep BLOCKED

# DNS queries
adb logcat -s AdBlockerApp | grep "DNS Query"

# Errors only
adb logcat -s AdBlockerApp:E

# VPN-related system logs
adb logcat -s VpnNetworkObserver NetworkStats VpnService

# Packet processing
adb logcat -s AdBlockerApp | grep "Packet processing"

# Save logs to file
adb logcat -s AdBlockerApp > adblocker.log
```

---

## Common Issues

### VPN Won't Start
```bash
adb logcat -s AdBlockerApp | grep -E "VPN|error"
```

**Checklist:**
- VPN permission granted?
- Another VPN active?
- Look for "Failed to establish VPN interface"

### Ads Not Blocking
```bash
adb logcat -s AdBlockerApp | grep -E "BLOCKED|ALLOWED"
```

**Checklist:**
- Domain in `BlockedDomains.kt`?
- DNS queries being intercepted?
- VPN actually running? (check notification)

### App Crashes
```bash
adb logcat -s AndroidRuntime:E AdBlockerApp
adb logcat -b crash
```

**Common causes:**
- Null pointer in packet handling
- Socket creation failure
- Coroutine scope lifecycle issues

### DNS Forwarding Issues
```bash
adb logcat -s AdBlockerApp | grep "DNS\|Forward"
```

**Checklist:**
- Upstream DNS (8.8.8.8) reachable?
- Socket protection applied? (`protect()`)
- Timeout issues? (default 3000ms)

### High Battery Drain
```bash
adb shell dumpsys batterystats | grep AdBlocker
adb shell top | grep adblocker
```

---

## Network Analysis

```bash
# Check VPN interface
adb shell ifconfig tun0

# VPN status
adb shell dumpsys vpn

# Network stats
adb shell dumpsys netstats detail full
```

---

## ADB Commands

```bash
# Clear app data
adb shell pm clear com.abhishek.adblocker

# Force stop
adb shell am force-stop com.abhishek.adblocker

# Uninstall
adb uninstall com.abhishek.adblocker

# Install and launch
adb install app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.abhishek.adblocker/.MainActivity
```

---

## Testing Ad Blocking

1. Enable VPN in app
2. Monitor: `adb logcat -s AdBlockerApp`
3. Open Chrome, visit ad-heavy site
4. Check logs for "BLOCKED" messages
5. Verify ads don't load

**Test sites:**
- News sites with Google Ads
- Mobile game websites
- Any site using Google AdSense