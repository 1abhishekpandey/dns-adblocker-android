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

### Domain Monitor
Real-time DNS query viewer with per-domain blocking controls and custom blocklist management

### DNS Server Selection
Choose from popular DNS providers or configure custom DNS server for improved speed and privacy

## Feature Details

### DNS Processing Pipeline
- **Packet Interception**: TUN interface captures all network packets at 10.0.0.2/32
- **Protocol Parsing**: IP/UDP/DNS packet analysis using custom parsers and dnsjava
- **Domain Filtering**: Real-time blocklist matching with NXDOMAIN responses
- **DNS Forwarding**: Allowed queries forwarded to user-selected DNS server (Google, Cloudflare, Quad9, OpenDNS, or custom)
- **Response Handling**: DNS responses reconstructed and written back to TUN

### App Selection System
- **Installed Apps Repository**: Query all installed apps via PackageManager
- **App Type Filters**: Toggle visibility of user apps and system apps with checkboxes
- **Search & Filter**: Real-time search across app names and package names
- **Smart Sorting**: Selected apps automatically appear at top of list
- **Selection Persistence**: DataStore-based storage for selected app packages
- **Change Tracking**: Unsaved changes detection with visible SAVE button
- **Service Restart**: Automatic VPN service restart to apply new routing rules

### Blocking Mechanism
- **Blocklist Matching**: Exact domain and subdomain pattern matching
- **NXDOMAIN Response**: Returns 0.0.0.0 for blocked domains
- **Statistics Tracking**: Real-time counter of blocked requests
- **Logging**: Detailed logs with emoji indicators for blocked/allowed domains

### Domain Monitor
- **Live Domain Feed**: Real-time display of DNS queries from selected apps
- **Deduplication**: Each domain shown once with last-seen timestamp tracking
- **Search & Filter**: Text search and "blocked only" filter for domain list
- **Click-to-Block**: Tap any domain to block/unblock with confirmation dialog
- **Parent Domain Suggestions**: Smart suggestions to block parent domains (e.g., when blocking `a.example.com`, suggests blocking `example.com` to block all `*.example.com` subdomains)
- **Subdomain Blocking**: Blocking any domain automatically blocks all its subdomains
- **User Blocklist**: Custom blocked domains, persisted per-domain in DataStore
- **Default Override**: Unblock default blocklist domains with user override capability
- **Visual Indicators**: Blocked domains shown with strikethrough, red text, and close icon
- **Smart Labeling**:
  - "Default" badge for domains in default blocklist
  - "User blocked" for custom blocks
  - "Overridden" for unblocked default domains
- **Reset to Default**: Clear all user customizations (blocks and overrides)
- **State Synchronization**: Changes immediately sync to VPN service via DomainObserver
- **Auto VPN Restart**: VPN automatically restarts after blocking/unblocking to clear DNS caches

### DNS Server Selection
- **Preset DNS Providers**: Quick selection from curated list:
  - Google (8.8.8.8) - Fast and reliable
  - Cloudflare (1.1.1.1) - Privacy-focused, fastest response time
  - Quad9 (9.9.9.9) - Security and privacy focused
  - OpenDNS (208.67.222.222) - Content filtering, good performance
- **Custom DNS Server**: Manual IP address entry for any DNS server
- **IP Validation**: Real-time validation ensures valid IPv4 address format
- **Settings Persistence**: Selected DNS server saved in DataStore preferences
- **Auto VPN Restart**: VPN automatically restarts when DNS server changes to apply new routing
- **Visual Feedback**: Radio button selection with DNS descriptions for informed choice
- **Error Handling**: Clear error messages for invalid DNS configurations

### Platform Integration
- **Android VPN Service**: System VPN with BIND_VPN_SERVICE permission
- **Navigation Compose**: Multi-screen navigation framework
- **DataStore Preferences**: Reactive preferences storage for VPN state and selections
- **Coroutines & Flow**: Asynchronous packet processing and reactive UI updates

## Upcoming Features

### Traffic Statistics
Bandwidth tracking per app and per domain with historical charts

### HTTP/HTTPS Inspection
Protocol-level traffic analysis beyond DNS filtering

### Backup & Restore
Export/import app selections and blocklist configurations
