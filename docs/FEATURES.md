# AdBlocker Features

## Core Features

### VPN-Based DNS Filtering
Local VPN tunnel intercepts DNS requests without routing traffic through external servers

### Ad Domain Blocking
Curated blocklist (~22 domains) blocks ad-serving domains with subdomain matching

### Per-App VPN Filtering
Select specific apps for VPN routing; empty selection defaults to all apps

### Real-Time Network Monitoring
DNS query logging with detailed packet inspection and forwarding metrics

### Persistent Foreground Service
Always-on VPN service with notification ensuring continuous ad blocking

### Interactive UI
Material3 Compose interface with VPN toggle, blocking statistics, and app selection

## Feature Details

### DNS Processing Pipeline
- **Packet Interception**: TUN interface captures all network packets at 10.0.0.2/32
- **Protocol Parsing**: IP/UDP/DNS packet analysis using custom parsers and dnsjava
- **Domain Filtering**: Real-time blocklist matching with NXDOMAIN responses
- **DNS Forwarding**: Allowed queries forwarded to Google DNS (8.8.8.8)
- **Response Handling**: DNS responses reconstructed and written back to TUN

### App Selection System
- **Installed Apps Repository**: Query all installed apps via PackageManager
- **Search & Filter**: Real-time search across app names and package names
- **Selection Persistence**: DataStore-based storage for selected app packages
- **Change Tracking**: Unsaved changes detection with visible SAVE button
- **Service Restart**: Automatic VPN service restart to apply new routing rules

### Blocking Mechanism
- **Blocklist Matching**: Exact domain and subdomain pattern matching
- **NXDOMAIN Response**: Returns 0.0.0.0 for blocked domains
- **Statistics Tracking**: Real-time counter of blocked requests
- **Logging**: Detailed logs with emoji indicators for blocked/allowed domains

### Platform Integration
- **Android VPN Service**: System VPN with BIND_VPN_SERVICE permission
- **Navigation Compose**: Multi-screen navigation framework
- **DataStore Preferences**: Reactive preferences storage for VPN state and selections
- **Coroutines & Flow**: Asynchronous packet processing and reactive UI updates

## Upcoming Features

### Enhanced Blocklist Management
Custom blocklist editor with domain add/remove functionality

### Traffic Statistics
Bandwidth tracking per app and per domain with historical charts

### Custom DNS Server
Configurable upstream DNS server (Cloudflare, Quad9, etc.)

### HTTP/HTTPS Inspection
Protocol-level traffic analysis beyond DNS filtering

### Backup & Restore
Export/import app selections and blocklist configurations
