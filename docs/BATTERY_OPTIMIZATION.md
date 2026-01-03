# Battery Optimization Guide

## Executive Summary

This document outlines battery optimization opportunities for the DNS Ad Blocker VPN app. Analysis reveals **4 high-severity**, **3 medium-severity**, and **2 low-severity** battery drains that can be addressed to significantly improve battery performance during active VPN sessions.

**Estimated Impact:** Implementing the high-priority optimizations could reduce battery consumption by 30-50% during active browsing.

---

## High Priority Optimizations

### 1. Socket Connection Pooling for DNS Forwarding

**Location:** `app/src/main/java/com/abhishek/adblocker/vpn/dns/DnsPacketHandler.kt:59-94`

**Problem:**
- A new `DatagramSocket` is created and destroyed for EVERY DNS request
- During active browsing: 50-100+ socket operations per minute
- Each socket creation involves kernel transitions, port allocation, and memory overhead

**Current Code:**
```kotlin
private fun forwardDnsRequest(...): ByteArray? {
    var socket: DatagramSocket? = null
    return try {
        socket = DatagramSocket()  // NEW SOCKET EVERY TIME
        vpnService.protect(socket)
        socket.soTimeout = 5000
        // ... send/receive ...
    } finally {
        socket?.close()
    }
}
```

**Solution:**
Implement socket pooling or reuse a single protected socket:

```kotlin
class DnsPacketHandler(...) {
    private val dnsSocket: DatagramSocket by lazy {
        DatagramSocket().apply {
            vpnService.protect(this)
            soTimeout = 5000
        }
    }

    // Cleanup in destroy method
    fun cleanup() {
        dnsSocket.close()
    }
}
```

**Benefits:**
- Eliminates hundreds of socket create/destroy cycles per session
- Reduces kernel syscall overhead
- Decreases port allocation churn

**Estimated Battery Savings:** 15-20%

---

### 2. Reduce Production Logging

**Location:** Multiple files

**Problem:**
- Logger defaults to `VERBOSE` level (util/Logger.kt:17)
- Every DNS query generates 3-4 log statements
- Logcat I/O prevents CPU from entering low-power states
- No build-type differentiation (debug vs release)

**Current Logging Hot Spots:**
- `DnsPacketHandler.kt:32` - Every DNS query (VERBOSE)
- `DnsPacketHandler.kt:49` - Every allowed domain (VERBOSE)
- `DnsPacketHandler.kt:65` - Every forwarded request (VERBOSE)
- `AdBlockerVpnService.kt:197` - Every packet write (DEBUG)

**Solution A: BuildConfig-based Logging**

1. Add BuildConfig flag to `build.gradle.kts`:
```kotlin
buildTypes {
    debug {
        buildConfigField("int", "LOG_LEVEL", "0") // VERBOSE
    }
    release {
        buildConfigField("int", "LOG_LEVEL", "3") // ERROR only
        isMinifyEnabled = true  // Enable ProGuard
    }
}
```

2. Update Logger initialization:
```kotlin
init {
    currentLevel = when (BuildConfig.LOG_LEVEL) {
        0 -> LogLevel.VERBOSE
        1 -> LogLevel.DEBUG
        2 -> LogLevel.INFO
        3 -> LogLevel.WARN
        4 -> LogLevel.ERROR
        else -> LogLevel.ERROR
    }
}
```

**Solution B: ProGuard Stripping (Recommended)**

1. Enable minification in release builds (line 25):
```kotlin
release {
    isMinifyEnabled = true  // Changed from false
}
```

2. Add to `proguard-rules.pro`:
```
# Strip verbose logging in release builds
-assumenosideeffects class com.abhishek.adblocker.util.Logger {
    public static void v(...);
    public static void d(...);
}
```

**Benefits:**
- Eliminates I/O overhead in hot path
- Reduces string concatenation CPU usage
- Release builds have zero logging overhead

**Estimated Battery Savings:** 10-15%

---

### 3. Eliminate Per-Packet Coroutine Spawning

**Location:** `app/src/main/java/com/abhishek/adblocker/vpn/AdBlockerVpnService.kt:165-167`

**Problem:**
- New coroutine launched for EVERY packet received
- Unnecessary concurrency for sequential I/O operations
- Context switching overhead

**Current Code:**
```kotlin
while (serviceScope.isActive && vpnInterface != null) {
    val length = input.read(buffer.array())
    if (length > 0) {
        val packetData = buffer.array().copyOf(length)
        serviceScope.launch {  // NEW COROUTINE EVERY PACKET
            processSinglePacket(packetData, output)
        }
    }
}
```

**Solution:**
Process packets sequentially (I/O is already non-blocking):

```kotlin
while (serviceScope.isActive && vpnInterface != null) {
    val length = input.read(buffer.array())
    if (length > 0) {
        // Process directly - no coroutine spawn
        processSinglePacket(buffer.array(), length, output)
    }
}

private fun processSinglePacket(buffer: ByteArray, length: Int, output: FileOutputStream) {
    try {
        val handler = dnsPacketHandler ?: return
        val responsePacket = handler.processDnsPacket(buffer, length)
        // ...
    } catch (e: Exception) {
        Logger.e("Error processing packet", e)
    }
}
```

**Additional Benefits:**
- Eliminates unnecessary `copyOf()` allocation per packet
- Simplifies execution model
- Reduces coroutine scheduler overhead

**Estimated Battery Savings:** 5-10%

---

### 4. Bound Domain Observer Memory Growth

**Location:** `app/src/main/java/com/abhishek/adblocker/vpn/dns/DomainObserver.kt:20-32`

**Problem:**
- Map grows indefinitely during long VPN sessions
- Every DNS query adds to map (called from DnsPacketHandler.kt:36)
- Each update creates new map copy (immutability overhead)
- Triggers StateFlow emissions that keep coroutines active

**Current Code:**
```kotlin
fun addDomain(hostname: String, isBlocked: Boolean, isUserBlocked: Boolean) {
    val current = _observedDomains.value.toMutableMap()
    current[normalized] = ObservedDomain(...)  // NO LIMIT
    _observedDomains.value = current
}
```

**Solution A: LRU Cache with Size Limit**

```kotlin
private const val MAX_OBSERVED_DOMAINS = 500

fun addDomain(hostname: String, isBlocked: Boolean, isUserBlocked: Boolean) {
    val current = _observedDomains.value.toMutableMap()

    // Remove oldest entry if at capacity
    if (current.size >= MAX_OBSERVED_DOMAINS) {
        val oldestKey = current.entries.minByOrNull { it.value.firstSeen }?.key
        oldestKey?.let { current.remove(it) }
    }

    current[normalized] = ObservedDomain(...)
    _observedDomains.value = current
}
```

**Solution B: Time-based Eviction**

```kotlin
fun addDomain(hostname: String, isBlocked: Boolean, isUserBlocked: Boolean) {
    val current = _observedDomains.value.toMutableMap()

    // Remove entries older than 1 hour
    val cutoffTime = System.currentTimeMillis() - 3600000
    current.entries.removeIf { it.value.firstSeen < cutoffTime }

    current[normalized] = ObservedDomain(...)
    _observedDomains.value = current
}
```

**Solution C: Disable Observer in Background**

```kotlin
// In DnsPacketHandler
fun processDnsPacket(ipPacketData: ByteArray): ByteArray? {
    // ...
    // Only observe if app is in foreground
    if (isAppInForeground) {
        DomainObserver.addDomain(dnsQuery.hostname, isBlocked, isUserBlocked)
    }
    // ...
}
```

**Benefits:**
- Prevents unbounded memory growth
- Reduces GC pressure from map operations
- Limits StateFlow emission frequency

**Estimated Battery Savings:** 5-8%

---

## Medium Priority Optimizations

### 5. Reduce Buffer Allocations

**Problem:**
Multiple allocations per packet across the processing pipeline:

1. `AdBlockerVpnService.kt:163` - Packet copy: `buffer.array().copyOf(length)`
2. `IpPacketParser.kt:69-77` - Three allocations per packet
3. `UdpPacketParser.kt:51` - One allocation per packet
4. `DnsPacketHandler.kt:75` - DNS receive buffer
5. `PacketWriter.kt:17` - Response buffer

**Solution: Object Pooling**

```kotlin
class BufferPool {
    private val pool = ArrayDeque<ByteArray>()
    private val maxSize = 20

    fun acquire(size: Int): ByteArray {
        return synchronized(pool) {
            pool.removeFirstOrNull()?.takeIf { it.size >= size }
        } ?: ByteArray(size)
    }

    fun release(buffer: ByteArray) {
        synchronized(pool) {
            if (pool.size < maxSize) {
                pool.addLast(buffer)
            }
        }
    }
}
```

**Benefits:**
- Reduces GC frequency
- Eliminates allocation overhead in hot path

**Estimated Battery Savings:** 3-5%

---

### 6. Optimize StateFlow Collections

**Problem:**
7 continuous flow collectors running (see report above), including 5 in ViewModels and 2 in VpnService.

**Solution: Lifecycle-Aware Collection**

For ViewModels:
```kotlin
// Use stateIn with WhileSubscribed to stop collection when UI is not visible
val vpnEnabled = vpnPreferencesRepository.vpnEnabled
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),  // 5s timeout
        initialValue = false
    )
```

For VpnService:
```kotlin
// Only collect when actually needed
private fun observeUserBlockedDomains() {
    serviceScope.launch {
        vpnPreferencesRepository.userBlockedDomains
            .collectLatest { domains ->  // Use collectLatest to skip intermediate emissions
                BlockedDomains.updateUserDomains(domains)
                Logger.d("Updated user blocked domains: ${domains.size} domains")
            }
    }
}
```

**Benefits:**
- Reduces coroutine overhead when app is backgrounded
- Prevents unnecessary processing of state changes

**Estimated Battery Savings:** 2-4%

---

### 7. Add Backoff to Packet Processing Loop

**Problem:**
Tight loop with non-blocking I/O might cause rapid wake-ups when no packets available.

**Current Code:**
```kotlin
while (serviceScope.isActive && vpnInterface != null) {
    buffer.clear()
    val length = input.read(buffer.array())  // May return 0 if no data
    if (length > 0) {
        // process packet
    }
    // Immediately loops back
}
```

**Solution: Yield on Empty Reads**

```kotlin
private var consecutiveEmptyReads = 0

while (serviceScope.isActive && vpnInterface != null) {
    buffer.clear()
    val length = input.read(buffer.array())

    if (length > 0) {
        consecutiveEmptyReads = 0
        processSinglePacket(buffer.array(), length, output)
    } else {
        consecutiveEmptyReads++
        // Yield CPU if multiple empty reads
        if (consecutiveEmptyReads > 5) {
            delay(1)  // 1ms delay
            consecutiveEmptyReads = 0
        }
    }
}
```

**Benefits:**
- Allows CPU to enter low-power states during idle periods
- Reduces unnecessary loop iterations

**Estimated Battery Savings:** 2-3%

---

## Low Priority Optimizations

### 8. Build-Type Configuration

**Location:** `app/build.gradle.kts:23-28`

**Current:**
```kotlin
buildTypes {
    release {
        isMinifyEnabled = false
        proguardFiles(...)
    }
}
```

**Recommended:**
```kotlin
buildTypes {
    debug {
        applicationIdSuffix = ".debug"
        isDebuggable = true
    }
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

**Benefits:**
- Smaller APK size
- Removes dead code
- Optimizes bytecode

---

### 9. Domain Matching Optimization (Future)

**Current:** Linear search with string operations (acceptable for ~40 domains)

**Future Optimization (when blocklist grows >1000 domains):**
- Implement Trie or Radix Tree for O(m) lookup (m = domain length)
- Use domain suffix matching optimizations

**Not urgent:** Current implementation is efficient enough for small blocklists.

---

## Implementation Priority

### Phase 1 (Immediate - Highest Impact)
1. **Socket pooling** (DnsPacketHandler) - 15-20% savings
2. **Production logging reduction** - 10-15% savings
3. **Remove per-packet coroutines** - 5-10% savings

**Combined Phase 1 Savings: ~30-45%**

### Phase 2 (Short-term)
4. **Bound domain observer** - 5-8% savings
5. **Optimize flow collections** - 2-4% savings

**Combined Phase 2 Savings: ~7-12%**

### Phase 3 (Nice-to-have)
6. **Buffer pooling** - 3-5% savings
7. **Loop backoff** - 2-3% savings
8. **Build configuration** - Reduces APK size, minor battery impact

---

## Measurement Strategy

### Before Optimization
```bash
# Capture battery stats before changes
adb shell dumpsys batterystats --reset
# Use app for 30 minutes
adb shell dumpsys batterystats > before_optimization.txt
```

### After Optimization
```bash
# Capture battery stats after changes
adb shell dumpsys batterystats --reset
# Use app for 30 minutes (same usage pattern)
adb shell dumpsys batterystats > after_optimization.txt
```

### Key Metrics to Compare
- **CPU time:** Look for `Cpu times` in batterystats
- **Wake locks:** Search for wake lock acquisitions
- **Network activity:** Mobile/WiFi packet counts
- **Battery drain rate:** mAh consumed per hour

### Battery Historian
```bash
# Generate detailed timeline
adb bugreport > bugreport.zip
# Upload to https://bathist.ef.lc/
```

---

## Android Battery Best Practices (Reference)

### VPN-Specific Considerations

1. **Foreground Service Properly Configured** ✅
   - Already using `IMPORTANCE_LOW` notification
   - Properly declared in manifest

2. **Doze Mode Compatibility**
   - VPN services are exempt from Doze restrictions
   - No action needed

3. **Background Restrictions**
   - VPN maintains connection in background (by design)
   - Consider detecting screen-off state to reduce logging

4. **Wake Lock Management** ✅
   - No explicit wake locks used (good!)
   - Avoid acquiring PARTIAL_WAKE_LOCK

5. **Network Efficiency**
   - Current DNS forwarding is already efficient (single UDP packet)
   - Socket pooling will further improve this

---

## Testing Checklist

- [ ] Socket pooling implemented and tested
- [ ] Release build logging disabled
- [ ] Per-packet coroutines removed
- [ ] Domain observer bounded
- [ ] Battery tests run (before/after comparison)
- [ ] ProGuard rules validated
- [ ] No crashes in release build
- [ ] DNS resolution still works correctly
- [ ] VPN performance not degraded

---

## Additional Resources

- [Android Battery Optimization Guide](https://developer.android.com/topic/performance/power)
- [VpnService API Reference](https://developer.android.com/reference/android/net/VpnService)
- [Optimize for Doze and App Standby](https://developer.android.com/training/monitoring-device-state/doze-standby)
