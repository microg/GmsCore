# Remote DroidGuard Server Setup Guide

This guide explains how to set up and use a Remote DroidGuard server to enable Play Integrity API support on devices that don't pass integrity checks natively.

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Server Setup](#server-setup)
4. [Client Configuration](#client-configuration)
5. [Server Device Setup](#server-device-setup)
6. [Testing & Verification](#testing--verification)
7. [Troubleshooting](#troubleshooting)
8. [Security Considerations](#security-considerations)

---

## Overview

### What is Remote DroidGuard?

Remote DroidGuard allows your device to offload DroidGuard/Play Integrity attestation to another device (server) that passes integrity checks. This is useful when:

- Your primary device runs custom ROMs or is rooted
- You want to avoid running obfuscated Google integrity code locally
- You need consistent Play Integrity passing without constant maintenance

### How It Works

```
┌─────────────────┐         ┌─────────────────┐         ┌─────────────────┐
│   Client Device │         │  DroidGuard     │         │   Google        │
│   (microG)      │────────▶│  Server         │────────▶│   Servers       │
│                 │◀────────│  (Your Device)  │◀────────│                 │
└─────────────────┘         └─────────────────┘         └─────────────────┘
     (Fails PI)                  (Passes PI)
```

1. Client device sends DroidGuard request to your server
2. Server forwards request to Google (or processes locally)
3. Server returns attestation results to client
4. Client uses results for Play Integrity API

---

## Architecture

### Components

1. **microG Client** (your daily driver)
   - Modified `RemoteHandleImpl.kt` for multi-step protocol
   - Configured with server URL
   - Runs on any Android device (rooted or not)

2. **DroidGuard Server** (your server device)
   - Python-based HTTP server
   - Handles multi-step DroidGuard protocol
   - Caches VM bytecode for performance
   - Can run on stock Android, Raspberry Pi, or cloud VPS

3. **Google Play Integrity** (remote attestation)
   - Validates server device integrity
   - Returns signed attestation tokens

---

## Server Setup

### Option 1: Docker Deployment (Recommended)

#### Prerequisites
- Docker and Docker Compose installed
- Google API key (optional, for fallback)

#### Steps

1. **Clone the repository**
   ```bash
   git clone https://github.com/microg/GmsCore.git
   cd GmsCore/play-services-droidguard/server
   ```

2. **Configure the server**
   ```bash
   cp config.yaml.example config.yaml
   # Edit config.yaml with your settings
   nano config.yaml
   ```

3. **Set environment variables**
   ```bash
   export GOOGLE_API_KEY="your_api_key_here"
   ```

4. **Start the server**
   ```bash
   docker-compose up -d
   ```

5. **Verify it's running**
   ```bash
   curl http://localhost:8080/health
   # Should return: {"status": "healthy", ...}
   ```

### Option 2: Direct Python Installation

#### Prerequisites
- Python 3.11+
- pip

#### Steps

1. **Install dependencies**
   ```bash
   pip install -r requirements.txt
   ```

2. **Configure the server**
   ```bash
   cp config.yaml.example config.yaml
   # Edit config.yaml
   ```

3. **Run the server**
   ```bash
   python3 droidguard-server.py --config config.yaml --port 8080
   ```

### Option 3: Android Device as Server

For best results, run the server on a stock Android device:

1. **Install Termux** from F-Droid
2. **Install Python** in Termux:
   ```bash
   pkg update && pkg install python
   ```
3. **Follow Option 2** above

---

## Client Configuration

### Configure microG

1. **Open microG Settings** on your client device

2. **Navigate to:**
   ```
   Device Registration → DroidGuard
   ```

3. **Enable Network Mode:**
   - Set Mode: `Network`
   - Enter Server URL: `http://your-server-ip:8080`
   - Enable DroidGuard

4. **Restart microG services**

### Manual Configuration (ADB)

```bash
# Enable network mode
adb shell content insert \
  --uri content://org.microg.gms.settings/settings \
  --bind mode:s:Network \
  --bind network_server_url:s:http://your-server-ip:8080 \
  --bind enabled:i:1

# Restart microG
adb shell am force-stop org.microg.gms
```

---

## Server Device Setup

To ensure your server device passes Play Integrity:

### Required Components

1. **Stock Android ROM** (recommended)
   - Unlocked bootloader OK
   - Must be stock or near-stock

2. **OR Custom ROM with:**
   - microG or GApps installed
   - Magisk or KernelSU
   - PlayIntegrityFix module
   - TrickyStore module

### Installation Steps

1. **Install Magisk** (if using custom ROM)
   ```bash
   # Patch boot image and flash via fastboot
   fastboot flash boot_patched boot.img
   ```

2. **Install Required Modules**
   - Download PlayIntegrityFix from GitHub
   - Download TrickyStore from GitHub
   - Flash via Magisk app

3. **Configure TrickyStore**
   ```bash
   # Create configuration
   echo "package=com.google.android.gms" > /data/adb/trickystore/target.txt
   ```

4. **Verify Integrity Status**
   - Install "Play Integrity API Checker" app
   - Run test - should show MEETS_DEVICE_INTEGRITY

### Server Software Setup

1. **Install Termux** (Android) or prepare Linux environment

2. **Clone and configure** (see Server Setup above)

3. **Set up auto-start**
   ```bash
   # Add to Termux boot scripts
   echo "cd /path/to/server && python3 droidguard-server.py --config config.yaml" >> ~/.termux/boot.sh
   ```

---

## Testing & Verification

### Test Server Health

```bash
curl http://your-server-ip:8080/health
# Expected: {"status": "healthy", "timestamp": "...", "version": "1.0.0"}
```

### Test Cache Stats

```bash
curl http://your-server-ip:8080/stats
# Expected: {"cache": {"entries": 0, "total_size_mb": 0, ...}}
```

### Test Play Integrity

1. **On client device**, install "Play Integrity API Checker"
2. **Run the test**
3. **Expected result:** MEETS_DEVICE_INTEGRITY

### Test Affected Apps

Test with apps that previously failed:
- Dott (scooter rental)
- Banking apps
- Google Pay/Wallet
- Netflix

---

## Troubleshooting

### Client Can't Connect to Server

**Symptoms:** Timeout errors, connection refused

**Solutions:**
1. Check server is running: `curl http://localhost:8080/health`
2. Verify firewall allows port 8080
3. Ensure client and server on same network or server has public IP
4. Check microG settings for correct URL

### Play Integrity Still Fails

**Symptoms:** MEETS_DEVICE_INTEGRITY not achieved

**Solutions:**
1. Verify server device passes PI itself
2. Check TrickyStore configuration
3. Update PlayIntegrityFix to latest version
4. Clear microG data and retry

### Server Returns Errors

**Symptoms:** 500 errors, failed responses

**Solutions:**
1. Check server logs: `docker-compose logs -f`
2. Verify API key is valid (if using Google fallback)
3. Check cache directory permissions
4. Increase timeout values in config

### Performance Issues

**Symptoms:** Slow responses, timeouts

**Solutions:**
1. Increase cache size in config
2. Check network latency between client and server
3. Enable compression in server config
4. Consider running server closer to client (same LAN)

---

## Security Considerations

### Network Security

1. **Use HTTPS in Production**
   - Configure SSL/TLS in server
   - Use reverse proxy (nginx) for SSL termination

2. **Restrict Access**
   - Use firewall rules
   - Configure allowed_packages in server config
   - Implement authentication tokens

3. **Monitor Access**
   - Review server logs regularly
   - Set up alerts for unusual activity

### Privacy

1. **Data Minimization**
   - Server only processes DroidGuard requests
   - No personal data stored long-term

2. **Local Processing**
   - Run your own server (don't use third-party)
   - Keep server on trusted network

3. **Cache Security**
   - VM bytecode is temporary (24h expiry)
   - Stored locally, not shared

### Production Hardening

```yaml
# config.yaml - Production Settings

# Restrict to specific packages
allowed_packages:
  - com.google.android.gms
  - com.google.android.gsf

# Rate limiting
rate_limit: 30  # Lower for production

# SSL/TLS
ssl:
  enabled: true
  cert_file: /etc/ssl/certs/droidguard.crt
  key_file: /etc/ssl/private/droidguard.key

# Authentication
auth_token: "your-secret-token-here"
```

---

## API Reference

### Endpoints

#### GET /health
Health check endpoint.

**Response:**
```json
{
  "status": "healthy",
  "timestamp": "2026-03-16T08:00:00Z",
  "version": "1.0.0"
}
```

#### GET /stats
Cache statistics.

**Response:**
```json
{
  "cache": {
    "entries": 5,
    "total_size_mb": 2.5,
    "max_size_mb": 100
  },
  "timestamp": "2026-03-16T08:00:00Z"
}
```

#### POST /init
Initialize DroidGuard session (multi-step protocol step 1).

**Parameters:**
- `flow` (query): DroidGuard flow name
- `source` (query): Package name
- Body: Request protobuf/JSON

**Response:**
```json
{
  "vmKey": "abc123...",
  "bytecode": "base64-encoded-bytecode",
  "extra": "base64-encoded-extra",
  "expiry": 1710604800,
  "cached": false
}
```

#### POST /snapshot
Execute VM snapshot (multi-step protocol step 2).

**Parameters:**
- `flow` (query): DroidGuard flow name
- `source` (query): Package name
- `vmKey` (query): VM key from init response
- Body: Snapshot data (form-encoded)

**Response:**
```
base64-encoded-result-bytes
```

---

## Contributing

Issues and PRs welcome! Please:

1. Test changes thoroughly
2. Update documentation
3. Follow existing code style
4. Add tests for new features

---

## License

Apache 2.0 - See LICENSE file for details.

---

## Support

- **Issues:** https://github.com/microg/GmsCore/issues
- **Discussions:** https://github.com/microg/GmsCore/discussions
- **Documentation:** https://microg.org/

---

**Last Updated:** March 16, 2026  
**Version:** 1.0.0
