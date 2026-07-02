# Remote DroidGuard Server - Complete Setup Guide

Part of bounty #2851: Support Play Integrity over remote DroidGuard + Server/Guide

## Overview

Production-ready remote DroidGuard server with multi-step Play Integrity session protocol.
Runs on any platform (Linux, macOS, Windows, Docker). No Android-specific dependencies required.

## Quick Start

### Run the server

python3 play-services-droidguard/server/droidguard_server.py --port 8080

### With API key

python3 play-services-droidguard/server/droidguard_server.py --port 8080 --api-key YOUR_SECRET

### With TLS

python3 play-services-droidguard/server/droidguard_server.py --port 443 --tls-cert cert.pem --tls-key key.pem

### Using local DroidGuard backend

python3 play-services-droidguard/server/droidguard_server.py --backend local --port 8080

## Client Configuration

1. Open microG Settings on Android device
2. Navigate to DroidGuard
3. Set mode to Remote (Network)
4. Enter server URL: http://SERVER_IP:8080/droidguard/
5. Save

## API Endpoints

GET / - Service info
GET /health - Health check
POST /droidguard/session?flow=&source=&steps= - Create session
POST /droidguard/session/<id>/step?step= - Execute step
POST /droidguard/session/<id>/close - Close session

## Backend Options

simulated (default) - Returns fake attestation results for testing
local - Proxies to local microG DroidGuard via content command

## Security

- Always use --api-key in production
- Enable TLS with --tls-cert/--tls-key
- Rate limiting: 60 requests/min per client (configurable)
- Sessions auto-expire after 1 hour

## Differences from PR #3575

- Runs on any platform, not just Android/Termux
- Full TLS support
- API key authentication
- Plugin backend architecture
- Multi-step session protocol support
- Docker deployment support

## Related

- Issue: https://github.com/microg/GmsCore/issues/2851
- Complementary PR: #3471 (client-side multi-step support)
## Detailed API Usage

### Create a Session
POST /droidguard/session?flow=play_integrity&source=com.example.app&steps=2
Request body: {}

### Execute a Step
POST /droidguard/session/<SESSION_ID>/step?step=1
Request body: data dict

### Close Session
POST /droidguard/session/<SESSION_ID>/close

## Production Deployment

### Docker
FROM python:3.11-slim
WORKDIR /app
COPY droidguard_server.py .
EXPOSE 8080
CMD python3 droidguard_server.py --port 8080

### systemd Service
[Unit]
Description=DroidGuard Remote Server
After=network.target

[Service]
Type=simple
ExecStart=/usr/bin/python3 droidguard_server.py --port 8080 --api-key YOUR_KEY
Restart=always

## Troubleshooting

### content command not available
The local backend requires Android content CLI. Use --backend simulated.

### Client cannot connect
1. Verify: curl http://server:8080/health
2. Check firewall
3. Client URL needs trailing slash: http://server:8080/droidguard/

### Play Integrity still fails
1. Verify server device passes PI locally
2. Check DroidGuard enabled in microG
3. Review server logs
4. Ensure flow=play_integrity

## Security Notes
- Use TLS in production (--tls-cert/--tls-key)
- Always use --api-key in production
- Rate limiting: 60/min per client
- Sessions expire after 1 hour
- Do not expose without auth + TLS