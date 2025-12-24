# AdBlocker - Technical Architecture Documentation

## Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [Package Structure](#package-structure)
3. [Layer Details](#layer-details)
4. [VPN Service Flow](#vpn-service-flow)
5. [Packet Processing Pipeline](#packet-processing-pipeline)
6. [DNS Filtering Logic](#dns-filtering-logic)
7. [Key Classes](#key-classes)
8. [Data Flow Diagrams](#data-flow-diagrams)
9. [Threading Model](#threading-model)
10. [State Management](#state-management)
11. [Dependencies](#dependencies)

---

## Architecture Overview

This application follows **Clean Architecture** principles with clear separation of concerns across three main layers:

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  MainActivity │  │  MainScreen  │  │ MainViewModel│      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                      Domain Layer                            │
│  ┌──────────────┐                                            │
│  │   VpnState   │  (Models & Business Rules)                │
│  └──────────────┘                                            │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                       Data Layer                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ VPN Service  │  │ DNS Handler  │  │ Preferences  │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │Packet Parser │  │  DNS Parser  │  │BlockedDomains│      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
```

### Design Principles Applied

1. **Dependency Rule**: Dependencies point inward (Presentation → Domain ← Data)
2. **Single Responsibility**: Each class has one clear purpose
3. **Separation of Concerns**: UI, business logic, and data handling are isolated
4. **Interface Segregation**: Components depend on abstractions where appropriate

---

## Package Structure

```
com.abhishek.adblocker/
│
├── MainActivity.kt                          # Entry point, Compose setup
│
├── ui/                                      # Presentation Layer
│   ├── screens/
│   │   └── MainScreen.kt                    # Main UI composables
│   ├── viewmodels/
│   │   └── MainViewModel.kt                 # UI state management
│   └── theme/
│       ├── Color.kt                         # Color definitions
│       ├── Type.kt                          # Typography definitions
│       └── Theme.kt                         # Material 3 theme
│
├── domain/                                  # Domain Layer
│   └── model/
│       └── VpnState.kt                      # VPN state sealed class
│
├── data/                                    # Data Layer
│   ├── blocklist/
│   │   └── BlockedDomains.kt                # Domain blocking logic
│   └── preferences/
│       └── VpnPreferencesRepository.kt      # DataStore integration
│
├── vpn/                                     # VPN Infrastructure
│   ├── AdBlockerVpnService.kt               # Android VPN service
│   ├── dns/
│   │   ├── DnsPacketHandler.kt              # DNS request routing
│   │   ├── DnsRequestParser.kt              # DNS query parsing
│   │   └── DnsResponseBuilder.kt            # DNS response creation
│   └── packet/
│       ├── IpPacketParser.kt                # IP layer parsing
│       ├── UdpPacketParser.kt               # UDP layer parsing
│       └── PacketWriter.kt                  # Packet assembly
│
└── util/
    └── Logger.kt                            # Centralized logging
```

---

## Layer Details

### Presentation Layer

#### **MainActivity.kt**
- **Responsibility**: App entry point, Compose host
- **Architecture Role**: UI Container
- **Key Features**:
  - Enables edge-to-edge display
  - Sets up Material 3 theme
  - Hosts MainScreen composable

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AdBlockerTheme {
                Scaffold { innerPadding ->
                    MainScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
```

#### **MainScreen.kt**
- **Responsibility**: User interface composition
- **Architecture Role**: Presentation Component
- **Key Features**:
  - VPN toggle switch with permission handling
  - VPN state display (Connected/Connecting/Disconnected)
  - Blocked domain count
  - VPN permission launcher integration

**UI Components**:
1. `AppTitle()` - Application branding
2. `StatusSection()` - Shows current VPN state with color coding
3. `VpnToggleSection()` - Switch to enable/disable VPN
4. `BlockedDomainsSection()` - Displays count of blocked domains

#### **MainViewModel.kt**
- **Responsibility**: UI state management and business logic coordination
- **Architecture Role**: Presentation Logic
- **State Management**:
  - `vpnState: StateFlow<VpnState>` - Current VPN connection state
  - `isVpnEnabled: StateFlow<Boolean>` - VPN enabled preference

**Key Methods**:
```kotlin
fun toggleVpn(context: Context)
  └─> startVpnService() or stopVpnService()
      └─> Updates preferences via VpnPreferencesRepository
      └─> Starts/stops AdBlockerVpnService
```

---

### Domain Layer

#### **VpnState.kt**
- **Responsibility**: Domain model representing VPN connection states
- **Architecture Role**: Domain Model (Sealed Class)
- **States**:
  ```kotlin
  sealed class VpnState {
      data object Disconnected : VpnState()
      data object Connecting : VpnState()
      data object Connected : VpnState()
  }
  ```

**Why Sealed Class?**
- Type-safe state representation
- Exhaustive when expressions
- No invalid states possible

---

### Data Layer

#### **VpnPreferencesRepository.kt**
- **Responsibility**: Persist VPN enabled state
- **Architecture Role**: Data Source (Preferences)
- **Technology**: Jetpack DataStore (Preferences)
- **Operations**:
  - `isVpnEnabled: Flow<Boolean>` - Observable VPN state
  - `setVpnEnabled(enabled: Boolean)` - Persist state

```kotlin
class VpnPreferencesRepository(private val context: Context) {
    private val vpnEnabledKey = booleanPreferencesKey("vpn_enabled")

    val isVpnEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[vpnEnabledKey] ?: false }

    suspend fun setVpnEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[vpnEnabledKey] = enabled
        }
    }
}
```

#### **BlockedDomains.kt**
- **Responsibility**: Domain blocking rules
- **Architecture Role**: Business Rules / Data Source
- **Implementation**: In-memory set of blocked domains
- **Blocking Logic**:
  ```kotlin
  fun isBlocked(hostname: String): Boolean {
      val normalized = hostname.lowercase().trimEnd('.')
      return domains.any { blocked ->
          normalized == blocked || normalized.endsWith(".$blocked")
      }
  }
  ```
  - Case-insensitive matching
  - Trailing dot normalization
  - Subdomain matching (e.g., `ads.google.com` matches `google.com`)

**Current Blocklist**: Google advertising domains
- `pagead2.googlesyndication.com`
- `googleads.g.doubleclick.net`
- `ad.doubleclick.net`
- And more...

---

## VPN Service Flow

### Start VPN Flow

```
User toggles VPN ON
    │
    ├─> MainViewModel.toggleVpn(true)
    │       │
    │       ├─> Check VPN permission (VpnService.prepare())
    │       │   ├─> If null: Permission granted
    │       │   └─> If Intent: Launch permission request
    │       │
    │       └─> startVpnService(context)
    │           ├─> Set state to Connecting
    │           ├─> Start AdBlockerVpnService with ACTION_START
    │           └─> Save preference (vpnEnabled = true)
    │
    └─> AdBlockerVpnService.onStartCommand(ACTION_START)
            │
            ├─> startForeground() with notification
            ├─> setupVpnInterface()
            │   ├─> Configure VPN parameters:
            │   │   • Address: 10.0.0.2/32
            │   │   • Route: 0.0.0.0/0 (all traffic)
            │   │   • DNS: 8.8.8.8
            │   │   • MTU: 1500
            │   │   • Non-blocking mode
            │   └─> Establish VPN interface
            │       └─> Get FileDescriptor for TUN device
            │
            ├─> Initialize DnsPacketHandler
            └─> startPacketProcessing()
                └─> Launch coroutine for packet reading
```

### Stop VPN Flow

```
User toggles VPN OFF
    │
    └─> MainViewModel.toggleVpn(false)
            │
            └─> stopVpnService(context)
                ├─> Start AdBlockerVpnService with ACTION_STOP
                ├─> Save preference (vpnEnabled = false)
                └─> Set state to Disconnected
                    │
                    └─> AdBlockerVpnService.onStartCommand(ACTION_STOP)
                            │
                            └─> stopVpnService()
                                ├─> Cancel serviceScope (stops packet processing)
                                ├─> Cleanup DnsPacketHandler
                                ├─> Close input/output streams
                                ├─> Close VPN interface
                                ├─> Stop foreground service
                                └─> Call stopSelf()
```

---

## Packet Processing Pipeline

### High-Level Pipeline

```
┌────────────────┐
│  TUN Interface │ (VPN virtual network interface)
└────────┬───────┘
         │
         │ Raw IP packets
         ▼
┌─────────────────────────────────────────┐
│   AdBlockerVpnService.processPackets()  │
│   • Read packets from TUN device        │
│   • Buffer allocation (32KB)            │
│   • Parallel processing per packet      │
└────────┬────────────────────────────────┘
         │
         │ Byte array (IP packet)
         ▼
┌─────────────────────────────────────────┐
│   DnsPacketHandler.processDnsPacket()   │
│   • IP packet validation                │
│   • UDP packet extraction               │
│   • DNS request filtering               │
└────────┬────────────────────────────────┘
         │
         │ DNS request
         ▼
┌─────────────────────────────────────────┐
│         DNS Filtering Decision          │
│   • Parse DNS query                     │
│   • Check against BlockedDomains        │
└────────┬────────────────────────────────┘
         │
    ┌────┴────┐
    │         │
BLOCKED    ALLOWED
    │         │
    │         └──> Forward to upstream DNS (8.8.8.8)
    │                     │
    │                     └──> Receive response
    │                          │
    │                          │
    └──> Create NXDOMAIN response
         │                     │
         └─────────┬───────────┘
                   │
                   │ DNS response payload
                   ▼
         ┌─────────────────────┐
         │  PacketWriter       │
         │  • Build UDP packet │
         │  • Build IP packet  │
         │  • Calculate checksum│
         └─────────┬───────────┘
                   │
                   │ Complete IP packet
                   ▼
         ┌─────────────────────┐
         │  Write to TUN       │
         │  • Synchronized     │
         │  • Return to system │
         └─────────────────────┘
```

### Detailed Packet Flow

#### 1. Packet Reading (AdBlockerVpnService)
```kotlin
private suspend fun processPackets() {
    val buffer = ByteBuffer.allocate(32767)  // Max IP packet size

    while (serviceScope.isActive && vpnInterface != null) {
        buffer.clear()
        val length = inputStream.read(buffer.array())

        if (length > 0) {
            val packetData = buffer.array().copyOf(length)
            serviceScope.launch {
                processSinglePacket(packetData, output)
            }
        }
    }
}
```
- **Non-blocking reads** from TUN device
- **Parallel processing**: Each packet processed in separate coroutine
- **Buffer reuse**: Single buffer for reading, copy for processing

#### 2. IP Packet Parsing (IpPacketParser)
```kotlin
fun parse(data: ByteArray): IpPacket? {
    // Validate minimum size (20 bytes for IP header)
    if (data.size < 20) return null

    // Extract IP version and header length
    val versionAndIhl = buffer.get().toInt() and 0xFF
    val version = (versionAndIhl shr 4) and 0x0F
    val ihl = versionAndIhl and 0x0F
    val headerLength = ihl * 4

    // Only support IPv4
    if (version != 4) return null

    // Extract protocol (17 = UDP, 6 = TCP)
    val protocol = buffer.get(9).toInt() and 0xFF

    // Extract source and destination IPs
    val sourceIp = ByteArray(4)
    val destIp = ByteArray(4)
    buffer.position(12)
    buffer.get(sourceIp)
    buffer.get(destIp)

    // Extract payload
    val payload = data.copyOfRange(headerLength, totalLength)

    return IpPacket(...)
}
```

**IP Packet Structure Parsed**:
```
 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|Version|  IHL  |Type of Service|          Total Length         |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|         Identification        |Flags|      Fragment Offset    |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|  Time to Live |    Protocol   |         Header Checksum       |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                       Source Address                          |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                    Destination Address                        |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                    Options                    |    Padding    |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                          Payload...                           |
```

#### 3. UDP Packet Parsing (UdpPacketParser)
```kotlin
fun parse(data: ByteArray): UdpPacket? {
    if (data.size < 8) return null  // Min UDP header size

    val sourcePort = buffer.short.toInt() and 0xFFFF
    val destPort = buffer.short.toInt() and 0xFFFF
    val length = buffer.short.toInt() and 0xFFFF
    buffer.short  // Skip checksum

    val payload = data.copyOfRange(8, length)

    return UdpPacket(sourcePort, destPort, length, payload)
}

fun isDnsRequest(packet: UdpPacket): Boolean = packet.destPort == 53
```

**UDP Packet Structure**:
```
 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|          Source Port          |       Destination Port        |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|            Length             |           Checksum            |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                        DNS Payload...                         |
```

#### 4. DNS Request Parsing (DnsRequestParser)
```kotlin
fun parseDnsQuery(rawData: ByteArray): DnsQuery? {
    val message = Message(rawData)  // dnsjava library
    val question = message.question ?: return null

    return DnsQuery(
        id = message.header.id,
        hostname = question.name.toString(true),  // Remove trailing dot
        type = question.type,  // A, AAAA, CNAME, etc.
        originalMessage = message
    )
}
```

---

## DNS Filtering Logic

### Step-by-Step DNS Request Handling

```
┌─────────────────────────────────────────┐
│  Incoming DNS Request                    │
│  • Query ID: 12345                       │
│  • Hostname: ads.example.com             │
│  • Type: A (IPv4 address)                │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│  DnsRequestParser.parseDnsQuery()        │
│  • Use dnsjava to parse DNS packet       │
│  • Extract hostname                      │
│  • Extract query type                    │
│  • Store original message                │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│  BlockedDomains.isBlocked()              │
│  • Normalize hostname (lowercase, trim)  │
│  • Check exact match                     │
│  • Check subdomain match                 │
└──────────────┬──────────────────────────┘
               │
         ┌─────┴──────┐
         │            │
     BLOCKED      ALLOWED
         │            │
         │            └──────────────────────────┐
         │                                       │
         ▼                                       ▼
┌────────────────────┐              ┌────────────────────────┐
│ Create NXDOMAIN    │              │ Forward to 8.8.8.8     │
│ Response           │              │ • Protect socket       │
│                    │              │ • Send DNS query       │
│ • Same query ID    │              │ • Wait for response    │
│ • QR flag set      │              │   (3 sec timeout)      │
│ • RCODE: NXDOMAIN  │              │ • Return real response │
│ • Question section │              └────────────────────────┘
└────────────────────┘
         │
         └──────────────┬────────────────────────┘
                        │
                        ▼
              ┌──────────────────┐
              │  PacketWriter    │
              │  Rebuild packet  │
              │  • Swap IPs      │
              │  • Swap ports    │
              │  • Add DNS data  │
              └────────┬─────────┘
                       │
                       ▼
              ┌──────────────────┐
              │ Write to TUN     │
              │ Return to app    │
              └──────────────────┘
```

### Blocked Response Generation

```kotlin
fun createBlockedResponse(request: Message): ByteArray {
    val response = Message(request.header.id)  // Same ID as request
    response.header.setFlag(Flags.QR.toInt())  // Query Response flag
    response.header.rcode = Rcode.NXDOMAIN     // Domain doesn't exist

    // Echo back the question
    if (request.question != null) {
        response.addRecord(request.question, Section.QUESTION)
    }

    return response.toWire()  // Convert to byte array
}
```

**NXDOMAIN Response**: Tells the app the domain doesn't exist, preventing connection attempts.

### Allowed Request Forwarding

```kotlin
private fun forwardDnsRequest(...): ByteArray? {
    val socket = getOrCreateSocket()
    vpnService.protect(socket)  // Prevent VPN loop

    // Send to upstream DNS
    val sendPacket = DatagramPacket(
        udpPacket.payload,
        udpPacket.payload.size,
        upstreamDnsServer,  // 8.8.8.8
        53
    )
    socket.send(sendPacket)

    // Wait for response
    val receiveBuffer = ByteArray(1024)
    val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
    socket.soTimeout = 3000  // 3 second timeout
    socket.receive(receivePacket)

    return receiveBuffer.copyOf(receivePacket.length)
}
```

**Socket Protection**: `protect()` ensures the upstream DNS socket doesn't route through the VPN (would cause infinite loop).

---

## Key Classes

### AdBlockerVpnService

**Extends**: `android.net.VpnService`

**Responsibilities**:
1. Manage VPN lifecycle
2. Configure VPN interface
3. Read packets from TUN device
4. Write response packets back to TUN
5. Coordinate DNS filtering

**Key Components**:
```kotlin
class AdBlockerVpnService : VpnService() {
    // VPN interface handle
    private var vpnInterface: ParcelFileDescriptor? = null

    // DNS filtering component
    private var dnsPacketHandler: DnsPacketHandler? = null

    // Coroutine scope for packet processing
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // I/O streams for TUN device
    private var inputStream: FileInputStream? = null
    private var outputStream: FileOutputStream? = null
}
```

**VPN Configuration**:
```kotlin
private fun setupVpnInterface() {
    val builder = Builder()
        .setSession("AdBlocker")
        .addAddress("10.0.0.2", 32)      // VPN interface address
        .addRoute("0.0.0.0", 0)          // Route all traffic
        .addDnsServer("8.8.8.8")         // DNS server
        .setMtu(1500)                    // Maximum transmission unit
        .setBlocking(false)              // Non-blocking I/O

    vpnInterface = builder.establish()
}
```

**Lifecycle**:
- **onCreate**: Service initialization
- **onStartCommand**: Handle START/STOP actions
- **onDestroy**: Cleanup resources

**Thread Safety**:
- Synchronized writes to TUN device
- Coroutine-based concurrent packet processing

---

### DnsPacketHandler

**Responsibilities**:
1. Process incoming IP packets
2. Filter DNS requests
3. Block or forward DNS queries
4. Manage upstream DNS socket

**Architecture Pattern**: Strategy Pattern (decides between blocking or forwarding)

**Key Methods**:

```kotlin
fun processDnsPacket(ipPacketData: ByteArray): ByteArray? {
    // 1. Parse IP packet
    val ipPacket = IpPacketParser.parse(ipPacketData) ?: return null

    // 2. Validate UDP protocol
    if (!IpPacketParser.isUdp(ipPacket)) return null

    // 3. Parse UDP packet
    val udpPacket = UdpPacketParser.parse(ipPacket.payload) ?: return null

    // 4. Validate DNS request (port 53)
    if (!UdpPacketParser.isDnsRequest(udpPacket)) return null

    // 5. Parse DNS query
    val dnsQuery = DnsRequestParser.parseDnsQuery(udpPacket.payload) ?: return null

    // 6. Apply filtering logic
    return if (BlockedDomains.isBlocked(dnsQuery.hostname)) {
        createBlockedResponse(...)
    } else {
        forwardDnsRequest(...)
    }
}
```

**Socket Management**:
```kotlin
private fun getOrCreateSocket(): DatagramSocket {
    if (upstreamDnsSocket == null || upstreamDnsSocket?.isClosed == true) {
        upstreamDnsSocket = DatagramSocket().apply {
            vpnService.protect(this)  // Bypass VPN for upstream DNS
        }
    }
    return upstreamDnsSocket!!
}
```

---

### Packet Parsers

#### **IpPacketParser**

**Responsibility**: Parse IPv4 packets

**Key Operations**:
- Version validation (IPv4 only)
- Header length calculation
- Protocol extraction (TCP=6, UDP=17)
- IP address extraction
- Payload extraction

**Data Class**:
```kotlin
data class IpPacket(
    val version: Int,           // IP version (4)
    val protocol: Int,          // 17 for UDP
    val sourceIp: ByteArray,    // 4 bytes
    val destIp: ByteArray,      // 4 bytes
    val headerLength: Int,      // Usually 20 bytes
    val totalLength: Int,       // Total packet length
    val payload: ByteArray      // UDP/TCP data
)
```

#### **UdpPacketParser**

**Responsibility**: Parse UDP packets

**Key Operations**:
- Port extraction (source and destination)
- Length validation
- DNS request detection (destPort == 53)

**Data Class**:
```kotlin
data class UdpPacket(
    val sourcePort: Int,        // Ephemeral port (e.g., 54321)
    val destPort: Int,          // DNS = 53
    val length: Int,            // UDP length
    val payload: ByteArray      // DNS query/response
)
```

#### **PacketWriter**

**Responsibility**: Construct response packets

**Key Operations**:
1. Build IP header
2. Build UDP header
3. Calculate IP checksum
4. Assemble complete packet

```kotlin
fun buildIpUdpPacket(
    sourceIp: ByteArray,      // Swapped from request's destIp
    destIp: ByteArray,        // Swapped from request's sourceIp
    sourcePort: Int,          // Swapped from request's destPort
    destPort: Int,            // Swapped from request's sourcePort
    dnsPayload: ByteArray
): ByteArray {
    val udpLength = 8 + dnsPayload.size
    val totalLength = 20 + udpLength

    val buffer = ByteBuffer.allocate(totalLength)

    // IP Header
    buffer.put(0x45.toByte())           // Version=4, IHL=5
    buffer.put(0)                       // TOS
    buffer.putShort(totalLength.toShort())
    buffer.putShort(0)                  // ID
    buffer.putShort(0)                  // Flags
    buffer.put(64)                      // TTL
    buffer.put(17)                      // Protocol (UDP)
    buffer.putShort(0)                  // Checksum (calculated later)
    buffer.put(sourceIp)
    buffer.put(destIp)

    // Calculate and insert IP checksum
    val checksum = calculateChecksum(buffer.array(), 0, 20)
    buffer.putShort(10, checksum.toShort())

    // UDP Header
    buffer.putShort(sourcePort.toShort())
    buffer.putShort(destPort.toShort())
    buffer.putShort(udpLength.toShort())
    buffer.putShort(0)                  // UDP checksum (optional)

    // DNS Payload
    buffer.put(dnsPayload)

    return buffer.array()
}
```

**Checksum Calculation**:
```kotlin
private fun calculateChecksum(data: ByteArray, offset: Int, length: Int): Int {
    var sum = 0L
    var i = offset

    // Sum 16-bit words
    while (i < offset + length - 1) {
        val word = ((data[i].toInt() and 0xFF) shl 8) or
                   (data[i + 1].toInt() and 0xFF)
        sum += word
        i += 2
    }

    // Add odd byte if present
    if (i < offset + length) {
        sum += (data[i].toInt() and 0xFF) shl 8
    }

    // Fold carries
    while ((sum shr 16) > 0) {
        sum = (sum and 0xFFFF) + (sum shr 16)
    }

    // One's complement
    return sum.inv().toInt() and 0xFFFF
}
```

---

### DNS Components

#### **DnsRequestParser**

**Responsibility**: Parse DNS queries using dnsjava library

**External Dependency**: `org.xbill.DNS.Message`

**Data Class**:
```kotlin
data class DnsQuery(
    val id: Int,                    // DNS transaction ID
    val hostname: String,           // e.g., "www.google.com"
    val type: Int,                  // A=1, AAAA=28, etc.
    val originalMessage: Message    // For response matching
)
```

**Implementation**:
```kotlin
fun parseDnsQuery(rawData: ByteArray): DnsQuery? {
    return try {
        val message = Message(rawData)
        val question = message.question ?: return null

        DnsQuery(
            id = message.header.id,
            hostname = question.name.toString(true),  // omitFinalDot=true
            type = question.type,
            originalMessage = message
        )
    } catch (e: IOException) {
        null  // Invalid DNS packet
    }
}
```

#### **DnsResponseBuilder**

**Responsibility**: Create DNS response packets

**Response Types**:

1. **NXDOMAIN (Blocked)**:
   ```kotlin
   fun createBlockedResponse(request: Message): ByteArray {
       val response = Message(request.header.id)
       response.header.setFlag(Flags.QR.toInt())  // Query Response
       response.header.rcode = Rcode.NXDOMAIN     // Domain not found

       if (request.question != null) {
           response.addRecord(request.question, Section.QUESTION)
       }

       return response.toWire()
   }
   ```

2. **NOERROR (Empty, not used currently)**:
   ```kotlin
   fun createEmptyResponse(request: Message): ByteArray {
       val response = Message(request.header.id)
       response.header.setFlag(Flags.QR.toInt())
       response.header.rcode = Rcode.NOERROR

       if (request.question != null) {
           response.addRecord(request.question, Section.QUESTION)
       }

       return response.toWire()
   }
   ```

---

### Logger

**Responsibility**: Centralized logging with configurable levels

**Log Levels**:
```kotlin
enum class LogLevel(val priority: Int) {
    VERBOSE(0),  // Detailed packet info
    DEBUG(1),    // Debug information
    INFO(2),     // Important events
    WARN(3),     // Warnings
    ERROR(4),    // Errors only
    NONE(5)      // Disable logging
}
```

**Specialized Log Methods**:
```kotlin
fun domainBlocked(hostname: String)
fun domainAllowed(hostname: String)
fun vpnStarted()
fun vpnStopped()
fun dnsQueryReceived(hostname: String, type: Int)
fun dnsForwarded(hostname: String, upstreamDns: String)
fun vpnInterfaceEstablished(address: String)
fun packetProcessingStarted()
fun packetProcessingStopped()
```

**Usage Example**:
```kotlin
Logger.dnsQueryReceived("www.google.com", 1)
// Output: 🔍 DNS Query: www.google.com (type=1)

Logger.domainBlocked("ads.example.com")
// Output: 🚫 BLOCKED: ads.example.com
```

---

## Data Flow Diagrams

### Complete Request Flow (Allowed Domain)

```
┌──────────────┐
│  Android App │ Makes network request to www.google.com
└──────┬───────┘
       │
       │ DNS query for www.google.com
       ▼
┌──────────────────────┐
│  Android OS          │
│  Network Stack       │
└──────┬───────────────┘
       │
       │ Route through VPN (10.0.0.2)
       ▼
┌────────────────────────────────────────────┐
│  AdBlockerVpnService (TUN Interface)       │
│  ┌──────────────────────────────────────┐  │
│  │ inputStream.read()                   │  │
│  │ • Read raw IP packet                 │  │
│  │ • Buffer size: 32KB                  │  │
│  └────────────┬─────────────────────────┘  │
│               │                             │
│               │ Launch coroutine            │
│               ▼                             │
│  ┌──────────────────────────────────────┐  │
│  │ processSinglePacket()                │  │
│  └────────────┬─────────────────────────┘  │
└───────────────┼─────────────────────────────┘
                │
                │ ByteArray (IP packet)
                ▼
┌────────────────────────────────────────────┐
│  DnsPacketHandler                          │
│  ┌──────────────────────────────────────┐  │
│  │ IpPacketParser.parse()               │  │
│  │ • Extract IPs: src, dest             │  │
│  │ • Validate IPv4                      │  │
│  │ • Extract protocol: UDP (17)         │  │
│  └────────────┬─────────────────────────┘  │
│               │                             │
│               │ IpPacket                    │
│               ▼                             │
│  ┌──────────────────────────────────────┐  │
│  │ UdpPacketParser.parse()              │  │
│  │ • Extract ports: src, dest           │  │
│  │ • Validate dest port = 53 (DNS)      │  │
│  └────────────┬─────────────────────────┘  │
│               │                             │
│               │ UdpPacket                   │
│               ▼                             │
│  ┌──────────────────────────────────────┐  │
│  │ DnsRequestParser.parseDnsQuery()     │  │
│  │ • Parse using dnsjava                │  │
│  │ • Extract hostname                   │  │
│  │ • Query: www.google.com (A record)   │  │
│  └────────────┬─────────────────────────┘  │
│               │                             │
│               │ DnsQuery                    │
│               ▼                             │
│  ┌──────────────────────────────────────┐  │
│  │ BlockedDomains.isBlocked()           │  │
│  │ • Check: www.google.com              │  │
│  │ • Result: NOT BLOCKED ✅             │  │
│  └────────────┬─────────────────────────┘  │
│               │                             │
│               │ Forward decision            │
│               ▼                             │
│  ┌──────────────────────────────────────┐  │
│  │ forwardDnsRequest()                  │  │
│  │ ┌────────────────────────────────┐   │  │
│  │ │ 1. getOrCreateSocket()         │   │  │
│  │ │    • Create DatagramSocket     │   │  │
│  │ │    • protect(socket)           │   │  │
│  │ │      (bypass VPN for this)     │   │  │
│  │ └────────────────────────────────┘   │  │
│  │ ┌────────────────────────────────┐   │  │
│  │ │ 2. Send to 8.8.8.8:53          │   │  │
│  │ │    • UDP packet with DNS query │   │  │
│  │ └────────────────────────────────┘   │  │
│  └──────────────┬───────────────────────┘  │
└─────────────────┼───────────────────────────┘
                  │
                  │ Network (bypasses VPN)
                  ▼
         ┌────────────────┐
         │  8.8.8.8:53    │ Google Public DNS
         │  DNS Server    │
         └────────┬───────┘
                  │
                  │ DNS Response: 142.250.185.46
                  ▼
┌────────────────────────────────────────────┐
│  DnsPacketHandler                          │
│  ┌──────────────────────────────────────┐  │
│  │ socket.receive()                     │  │
│  │ • Timeout: 3 seconds                 │  │
│  │ • DNS response packet received       │  │
│  └────────────┬─────────────────────────┘  │
│               │                             │
│               │ DNS response bytes          │
│               ▼                             │
│  ┌──────────────────────────────────────┐  │
│  │ PacketWriter.buildIpUdpPacket()      │  │
│  │ • Swap IPs:                          │  │
│  │   src = original dest (10.0.0.2)     │  │
│  │   dest = original src (app)          │  │
│  │ • Swap ports:                        │  │
│  │   srcPort = 53                       │  │
│  │   destPort = original srcPort        │  │
│  │ • Add DNS response payload           │  │
│  │ • Calculate IP checksum              │  │
│  └────────────┬─────────────────────────┘  │
└───────────────┼─────────────────────────────┘
                │
                │ Complete IP packet
                ▼
┌────────────────────────────────────────────┐
│  AdBlockerVpnService                       │
│  ┌──────────────────────────────────────┐  │
│  │ writePacketToTun()                   │  │
│  │ • synchronized(outputStream)         │  │
│  │ • outputStream.write(packet)         │  │
│  └──────────────────────────────────────┘  │
└────────────┬───────────────────────────────┘
             │
             │ Response packet to TUN
             ▼
┌────────────────────────┐
│  Android OS            │
│  Network Stack         │
└────────────┬───────────┘
             │
             │ Route to original app
             ▼
┌────────────────────────┐
│  Android App           │
│  Receives IP: 142.250.185.46
│  Connects to Google    │
└────────────────────────┘
```

### Blocked Domain Flow

```
App requests: ads.example.com
    │
    ▼
TUN Interface
    │
    ▼
AdBlockerVpnService
    │
    ▼
DnsPacketHandler
    │
    ├─> IpPacketParser    ✓
    ├─> UdpPacketParser   ✓
    ├─> DnsRequestParser  ✓ (hostname: ads.example.com)
    │
    ▼
BlockedDomains.isBlocked("ads.example.com")
    │
    └─> Result: BLOCKED 🚫
        │
        ▼
DnsResponseBuilder.createBlockedResponse()
    │
    ├─> Same query ID
    ├─> QR flag = 1 (response)
    ├─> RCODE = NXDOMAIN
    └─> Question section copied
        │
        ▼
PacketWriter.buildIpUdpPacket()
    │
    └─> Response packet
        │
        ▼
Write to TUN
    │
    ▼
Android OS
    │
    └─> App receives: NXDOMAIN
        │
        └─> Connection fails (domain not found)
            No ad loaded! ✅
```

---

## Threading Model

### Coroutines Architecture

```
┌────────────────────────────────────────────────┐
│  AdBlockerVpnService                           │
│  ┌──────────────────────────────────────────┐  │
│  │ serviceScope                             │  │
│  │ = CoroutineScope(                        │  │
│  │     SupervisorJob() + Dispatchers.IO     │  │
│  │   )                                      │  │
│  │                                          │  │
│  │ • SupervisorJob: Child failures don't   │  │
│  │   cancel entire scope                   │  │
│  │ • Dispatchers.IO: Optimized for I/O     │  │
│  │   operations                            │  │
│  └──────────────────────────────────────────┘  │
└────────────────────────────────────────────────┘

Main Packet Loop (Dispatchers.IO):
┌────────────────────────────────────┐
│ serviceScope.launch {              │
│   processPackets()                 │
│   └─> while (isActive) {           │
│       • Read packet (blocking)     │
│       • For each packet:           │
│         serviceScope.launch {      │
│           processSinglePacket()    │  <- Parallel processing
│         }                          │
│     }                              │
│ }                                  │
└────────────────────────────────────┘

Per-Packet Processing (Dispatchers.IO):
┌────────────────────────────────────┐
│ serviceScope.launch {              │
│   processSinglePacket()            │
│   └─> DnsPacketHandler.process()  │
│       ├─> Parse                    │
│       ├─> Filter                   │
│       └─> Forward or block         │
│           └─> socket.send()        │  <- May block
│           └─> socket.receive()     │  <- May block
│ }                                  │
└────────────────────────────────────┘
```

### Thread Safety Mechanisms

1. **Synchronized TUN Writes**:
   ```kotlin
   private fun writePacketToTun(packet: ByteArray, output: FileOutputStream) {
       synchronized(output) {
           output.write(packet)
       }
   }
   ```
   - Multiple coroutines writing responses
   - Must serialize writes to prevent corruption

2. **Coroutine Lifecycle**:
   ```kotlin
   override fun onDestroy() {
       serviceScope.cancel()  // Cancels all child coroutines
       // Wait for all operations to complete
       // Then cleanup resources
   }
   ```

3. **DataStore (Thread-Safe)**:
   ```kotlin
   // VpnPreferencesRepository uses DataStore
   // All operations are coroutine-based and thread-safe
   suspend fun setVpnEnabled(enabled: Boolean) {
       context.dataStore.edit { preferences ->
           preferences[vpnEnabledKey] = enabled
       }
   }
   ```

### Dispatcher Usage

| Component | Dispatcher | Reason |
|-----------|------------|--------|
| AdBlockerVpnService | Dispatchers.IO | I/O-bound packet processing |
| MainViewModel | Dispatchers.IO | Repository operations (DataStore) |
| DnsPacketHandler | Dispatchers.IO | Network socket operations |

---

## State Management

### VPN State Flow

```
┌─────────────────────────────────────────────────┐
│  MainViewModel                                  │
│  ┌───────────────────────────────────────────┐  │
│  │ private val _vpnState =                   │  │
│  │   MutableStateFlow<VpnState>(             │  │
│  │     VpnState.Disconnected                 │  │
│  │   )                                       │  │
│  │                                           │  │
│  │ val vpnState: StateFlow<VpnState> =       │  │
│  │   _vpnState.asStateFlow()                 │  │
│  └───────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
                    │
                    │ Observed by
                    ▼
┌─────────────────────────────────────────────────┐
│  MainScreen (Compose)                           │
│  ┌───────────────────────────────────────────┐  │
│  │ val vpnState by                           │  │
│  │   viewModel.vpnState.collectAsState()     │  │
│  │                                           │  │
│  │ when (vpnState) {                         │  │
│  │   is Connected -> "Connected" (Green)     │  │
│  │   is Connecting -> "Connecting..." (Amber)│  │
│  │   is Disconnected -> "Disconnected" (Gray)│  │
│  │ }                                         │  │
│  └───────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
```

### State Transitions

```
Disconnected
    │
    │ User toggles VPN ON
    │ └─> MainViewModel.toggleVpn(true)
    │
    ▼
Connecting
    │
    │ VPN service starts
    │ └─> AdBlockerVpnService.startVpnService()
    │     └─> setupVpnInterface()
    │         └─> startPacketProcessing()
    │
    ▼
Connected
    │
    │ User toggles VPN OFF
    │ └─> MainViewModel.toggleVpn(false)
    │
    ▼
Disconnected
```

### Preferences Persistence

```
┌─────────────────────────────────────────────────┐
│  VpnPreferencesRepository                       │
│  ┌───────────────────────────────────────────┐  │
│  │ DataStore<Preferences>                    │  │
│  │ • File: vpn_preferences.preferences_pb    │  │
│  │ • Location: app's datastore directory     │  │
│  │                                           │  │
│  │ Key-Value Store:                          │  │
│  │ ┌─────────────────────────────────────┐   │  │
│  │ │ "vpn_enabled" -> Boolean            │   │  │
│  │ └─────────────────────────────────────┘   │  │
│  │                                           │  │
│  │ val isVpnEnabled: Flow<Boolean>           │  │
│  │ suspend fun setVpnEnabled(Boolean)        │  │
│  └───────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
                    │
                    │ Collected by
                    ▼
┌─────────────────────────────────────────────────┐
│  MainViewModel.init                             │
│  ┌───────────────────────────────────────────┐  │
│  │ viewModelScope.launch {                   │  │
│  │   vpnPreferencesRepository                │  │
│  │     .isVpnEnabled                         │  │
│  │     .collect { enabled ->                 │  │
│  │       _isVpnEnabled.value = enabled       │  │
│  │       updateVpnState(enabled)             │  │
│  │     }                                     │  │
│  │ }                                         │  │
│  └───────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
```

**Persistence Benefits**:
- Survives app restarts
- Reactive updates (Flow-based)
- Type-safe (Preferences API)
- Coroutine-friendly

---

## Dependencies

### External Libraries

#### **AndroidX Core Libraries**
```gradle
implementation("androidx.core:core-ktx")
implementation("androidx.lifecycle:lifecycle-runtime-ktx")
implementation("androidx.activity:activity-compose")
```
- **Purpose**: Core Android functionality, Kotlin extensions, lifecycle management

#### **Jetpack Compose**
```gradle
implementation(platform("androidx.compose:compose-bom"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.ui:ui-graphics")
implementation("androidx.compose.ui:ui-tooling-preview")
implementation("androidx.compose.material3:material3")
implementation("androidx.lifecycle:lifecycle-viewmodel-compose")
```
- **Purpose**: Modern declarative UI framework
- **Version**: Material 3 (latest design system)
- **Features Used**:
  - Composable functions
  - State management (`collectAsState`)
  - Material 3 theming

#### **dnsjava**
```gradle
implementation("dnsjava:dnsjava:3.5.1")
```
- **Purpose**: DNS protocol parsing and message construction
- **Usage**:
  - Parse DNS queries: `Message(rawData)`
  - Extract DNS questions: `message.question`
  - Build DNS responses: `Message(id)`, `setFlag()`, `addRecord()`
- **Why Not Custom Parsing?**: DNS is complex (compression, multiple record types)

#### **DataStore Preferences**
```gradle
implementation("androidx.datastore:datastore-preferences")
```
- **Purpose**: Modern replacement for SharedPreferences
- **Features**:
  - Coroutine-based async API
  - Type-safe with preferences keys
  - Observable with Flow
  - Handles data corruption
- **Usage**: Persist VPN enabled state

#### **Kotlin Coroutines**
```gradle
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android")
```
- **Purpose**: Asynchronous programming
- **Usage**:
  - Packet processing loops
  - Network I/O (DNS forwarding)
  - DataStore operations
- **Dispatchers Used**: `Dispatchers.IO`

#### **Lifecycle Service**
```gradle
implementation("androidx.lifecycle:lifecycle-service")
```
- **Purpose**: Lifecycle-aware Service component
- **Usage**: Future-proofing for lifecycle observers in service

### Android SDK Dependencies

#### **VpnService** (`android.net.VpnService`)
- **Purpose**: Core VPN functionality
- **API Level**: Minimum SDK 25 (Android 7.1)
- **Key APIs**:
  - `Builder()`: Configure VPN interface
  - `establish()`: Create TUN device
  - `protect(socket)`: Bypass VPN for specific sockets

#### **NotificationChannel** (`android.app.NotificationChannel`)
- **Purpose**: Foreground service notification
- **API Level**: 26+ (Android 8.0)
- **Requirement**: Foreground services must show notification

### Build Configuration

```gradle
android {
    compileSdk = 36

    defaultConfig {
        minSdk = 25      // Android 7.1 (VpnService requirements)
        targetSdk = 36   // Latest Android
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }
}
```

---

## Performance Considerations

### Packet Processing Performance

1. **Non-blocking I/O**:
   ```kotlin
   .setBlocking(false)
   ```
   - Prevents thread blocking on TUN reads
   - Allows efficient coroutine scheduling

2. **Parallel Packet Processing**:
   ```kotlin
   serviceScope.launch {
       processSinglePacket(packetData, output)
   }
   ```
   - Each packet processed in separate coroutine
   - Leverages multi-core processors
   - DNS forwarding won't block other packets

3. **Buffer Size**: 32KB (32767 bytes)
   - Max IP packet size without fragmentation
   - Prevents multiple reads for single packet

4. **Socket Reuse**:
   ```kotlin
   private fun getOrCreateSocket(): DatagramSocket
   ```
   - Single socket for all upstream DNS queries
   - Avoids socket creation overhead
   - `protect()` called once

### Memory Management

1. **Object Allocation**:
   - Data classes for packets (GC pressure)
   - ByteArray copying (necessary for concurrent processing)

2. **Potential Optimizations** (not implemented):
   - Object pooling for packets
   - ByteBuffer direct allocation
   - Packet batching

### DNS Query Performance

1. **Timeout**: 3 seconds
   ```kotlin
   socket.soTimeout = 3000
   ```
   - Prevents indefinite blocking
   - Reasonable for network latency

2. **Upstream DNS**: Google Public DNS (8.8.8.8)
   - Fast, reliable
   - Global anycast network

---

## Security Considerations

### VPN Security

1. **Local VPN Only**:
   - No remote VPN server
   - All processing on-device
   - No data leaves device (except DNS queries)

2. **DNS Privacy**:
   - DNS queries go to Google DNS (8.8.8.8)
   - Not encrypted (DNS over HTTPS not implemented)
   - Potential improvement: DNS-over-TLS or DNS-over-HTTPS

3. **Socket Protection**:
   ```kotlin
   vpnService.protect(socket)
   ```
   - Essential to prevent VPN routing loops
   - Upstream DNS bypass VPN tunnel

### App Security

1. **No User Data Collection**:
   - Only logs domains (optional, debug only)
   - No analytics, no tracking

2. **Permissions Required**:
   - `BIND_VPN_SERVICE` (system permission)
   - `FOREGROUND_SERVICE` (notification)
   - VPN permission (user consent)

---

## Extensibility Points

### Adding More Blocked Domains

```kotlin
// data/blocklist/BlockedDomains.kt
private val domains = setOf(
    // Add more domains here
    "ads.facebook.com",
    "analytics.twitter.com",
    // ...
)
```

**Future Enhancement**: Load from file or remote URL

### Supporting Other Protocols

Currently only UDP/DNS is handled. To add TCP support:

1. Add `TcpPacketParser` (similar to `UdpPacketParser`)
2. Create `TcpPacketHandler`
3. Update `DnsPacketHandler` to route TCP packets
4. Implement TCP connection proxying (complex)

### Custom DNS Responses

Instead of NXDOMAIN, return custom IP:

```kotlin
fun createBlockedResponse(request: Message): ByteArray {
    val response = Message(request.header.id)
    response.header.setFlag(Flags.QR.toInt())
    response.header.rcode = Rcode.NOERROR

    val question = request.question
    val record = ARecord(
        question.name,
        DClass.IN,
        300,  // TTL
        InetAddress.getByName("127.0.0.1")  // Localhost
    )
    response.addRecord(record, Section.ANSWER)

    return response.toWire()
}
```

### Statistics Tracking

Add to `DnsPacketHandler`:

```kotlin
class DnsPacketHandler {
    private val blockedCount = AtomicInteger(0)
    private val allowedCount = AtomicInteger(0)

    fun processDnsPacket(...) {
        // ...
        if (BlockedDomains.isBlocked(...)) {
            blockedCount.incrementAndGet()
            // ...
        } else {
            allowedCount.incrementAndGet()
            // ...
        }
    }

    fun getStatistics(): Statistics {
        return Statistics(
            blocked = blockedCount.get(),
            allowed = allowedCount.get()
        )
    }
}
```

Expose via `StateFlow` in `MainViewModel` for UI display.

---

## Troubleshooting Guide

### Common Issues

#### **VPN Not Starting**
- Check `logcat` for errors
- Ensure VPN permission granted
- Verify no other VPN active
- Check `vpnInterface != null` after `establish()`

#### **DNS Not Blocking**
- Verify domain in `BlockedDomains.domains`
- Check `Logger` output for DNS queries
- Ensure packet processing started
- Test with known blocked domain

#### **App Not Loading Any Sites**
- Check upstream DNS socket created
- Verify `protect(socket)` called
- Test DNS forwarding timeout (3s)
- Check packet writing to TUN

#### **Performance Issues**
- Monitor coroutine count
- Check for packet processing backlog
- Verify non-blocking I/O
- Profile with Android Profiler

### Debug Logging

Enable verbose logging:
```kotlin
// In AdBlockerVpnService.onCreate()
Logger.currentLevel = Logger.LogLevel.VERBOSE
```

Key log messages:
- `🔒 VPN Service Started`
- `🌐 VPN Interface: 10.0.0.2`
- `▶️ Packet processing started`
- `🔍 DNS Query: <hostname>`
- `🚫 BLOCKED: <hostname>` or `✅ ALLOWED: <hostname>`

---

## Conclusion

This AdBlocker application demonstrates:

1. **Clean Architecture**: Clear separation of concerns across presentation, domain, and data layers
2. **Modern Android Development**: Jetpack Compose, Coroutines, DataStore
3. **VPN Fundamentals**: TUN device, packet parsing, DNS filtering
4. **Concurrent Programming**: Coroutine-based parallel packet processing
5. **Network Protocol Implementation**: IP, UDP, DNS packet handling

The architecture is designed for:
- **Maintainability**: Each component has a single responsibility
- **Testability**: Clear interfaces and dependencies
- **Extensibility**: Easy to add features (more protocols, statistics, etc.)
- **Performance**: Parallel processing, efficient I/O

This document serves as a comprehensive technical reference for developers working on or extending this project.
