# Changelog

## v1.0.0-beta.4

### Performance
- Implemented battery optimizations reducing power consumption by 30-50%
- Disabled verbose logging in production builds (10-15% battery savings)
- Added socket pooling with lazy initialization for DNS forwarding (15-20% savings)
- Removed per-packet coroutine spawning for more efficient packet processing (5-10% savings)
- Bounded domain observer memory with LRU eviction (max 500 domains, 5-8% savings)

### Documentation
- Added comprehensive battery optimization analysis documentation

## v1.0.0-beta.3

### Features
- Added DNS server selection for improved speed - choose between Google, Cloudflare, OpenDNS, and Quad9 DNS servers

### CI/CD
- Added build GitHub Actions workflow

## v1.0.0-beta.2

### Fixes
- Fixed domain monitor not updating when app is in background

### Features
- Added domain monitor with real-time DNS tracking and custom blocklist management
- Added auto-restart VPN when blocking/unblocking domains to apply changes immediately
- Added per-app VPN filtering with app selection UI
- Added app type filters to view apps by category (System/User/Selected)

## v1.0.0-beta.1

### Features
- Implemented VPN-based ad blocker for Android
- Added DNS-level domain blocking with configurable blocklist
- Added foreground service with persistent notification
- Added real-time blocked request counter
