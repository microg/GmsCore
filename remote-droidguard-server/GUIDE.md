# Remote DroidGuard Server — Setup Guide

## Overview

microG's **remote DroidGuard** feature lets you offload Play Integrity attestation to
a separate *server device* — typically an old stock Android phone kept at home —
instead of running integrity-bypassing hacks on your daily-driver phone.

This guide covers:
1. Setting up the **server device** (the phone that runs DroidGuard)
2. Running the **bridge server** (python HTTP server)
3. Configuring microG on your **client device** to use it

---

## Architecture

```
┌─────────────────┐     HTTP       ┌──────────────────┐     ADB / RPC     ┌──────────────────┐
│  Client Device   │ ─────────────→ │  Bridge Server    │ ───────────────→ │  Server Device    │
│  (microG phone)  │                │  (server.py)      │                  │  (stock Android)  │
└─────────────────┘                └──────────────────┘                  └──────────────────┘
```

- **Client Device** — your daily phone running a custom ROM with microG.
- **Bridge Server** — runs `server.py` on a machine both devices can reach (LAN, VPS, etc.).
- **Server Device** — a stock (or rooted) Android phone that executes the actual
  Google DroidGuard bytecode and returns results.

---

## 1. Server Device Setup

The server device is the phone that *actually* runs DroidGuard. It needs:

### Option A: Stock (unrooted) phone — easiest, most reliable

1. Any Android phone running a stock, unmodified firmware (OEM ROM).
2. Google Play Services must be present and functional.
3. Install a companion app that exposes DroidGuard over a local HTTP/ADB bridge
   (see `android-bridge/` in this directory — WIP).

### Option B: Rooted phone with bypass stack — more flexible

1. Flash a custom ROM (LineageOS or similar).
2. Install **Magisk** or **KernelSU**.
3. Install **PlayIntegrityFix** and **TrickyStore** modules.
4. Install microG **or** keep GApps (microG preferred for open-source visibility).
5. Run the companion DroidGuard bridge app from this repo.

### Option B components explained

| Component | Purpose |
|---|---|
| **Magisk / KernelSU** | Root access for system-level spoofing |
| **PlayIntegrityFix** (PIF) | Spoofs build fingerprint to bypass hardware-backed attestation |
| **TrickyStore** | Handles keybox attestation for STRONG integrity (optional) |
| **DroidGuard Bridge** | Exposes the DroidGuard VM over an ADB-accessible interface |

> **Note:** The bypass software (PIF, TrickyStore) needs regular updates as Google
> revokes fingerprints and patches detection. Check their GitHub repos for the
> latest versions.

---

## 2. Bridge Server Setup

### Prerequisites

- **Python 3.9+** with Flask: `pip install flask`
- Network connectivity to both the client device and server device.

### Quick start

```bash
cd remote-droidguard-server

# Install dependencies
pip install flask

# (Optional) Set environment variables
export DG_LISTEN_HOST=0.0.0.0
export DG_LISTEN_PORT=8080
export DG_PLUGIN=./plugin.py

# Run the server
python server.py
```

The server will listen on `http://<your-host>:8080`.

### Plugin

The `plugin.py` script is the bridge between the HTTP server and the server device.
In production, replace `plugin.py` with a script that forwards requests to your
server device (via ADB, HTTP, or a custom bridge app).

The plugin receives JSON on stdin:
```json
{"action": "begin|snapshot|close", "params": {...}, "payload": "..."}
```

And returns the response on stdout (plain text).

### Running with systemd (Linux)

```ini
# /etc/systemd/system/droidguard-server.service
[Unit]
Description=Remote DroidGuard Bridge Server
After=network.target

[Service]
Type=simple
User=droidguard
WorkingDirectory=/opt/droidguard-server
ExecStart=/usr/bin/python3 server.py
Environment=DG_LISTEN_HOST=0.0.0.0
Environment=DG_LISTEN_PORT=8080
Environment=DG_PLUGIN=/opt/droidguard-server/plugin.py
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl enable --now droidguard-server
```

### Running with Docker

```bash
docker run -d --name droidguard-server \
  -p 8080:8080 \
  -v $(pwd):/app \
  -e DG_PLUGIN=/app/plugin.py \
  python:3.12-slim \
  sh -c "pip install flask && python /app/server.py"
```

### Running on Windows

```powershell
# In PowerShell (as Administrator if port < 1024)
$env:DG_LISTEN_HOST = "0.0.0.0"
$env:DG_LISTEN_PORT = "8080"
pip install flask
python server.py
```

Or register as a Windows Service using `nssm`.

---

## 3. Client Device Configuration

On your daily phone running microG:

1. Open microG Settings → **DroidGuard** → **Remote DroidGuard**.
2. Set the **Server URL** to: `http://<bridge-server-ip>:8080`
3. Ensure **Google device registration** and **Cloud Messaging** are enabled in
   microG Settings.
4. Test: open the Dott app (or another Play Integrity-gated app) and attempt sign-up/login.

### Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| "Network URL required" | Server URL not configured in microG | Set the remote DroidGuard URL in microG settings |
| "App attestation failed" / 403 | Server device not passing Play Integrity | Check server device passes SafetyNet / Play Integrity (use a checker app) |
| "Firebase App Check token is invalid" | Play Integrity token rejected by Firebase | Ensure server device returns a valid, non-expired PI token |
| `sessionId` not found | Server restart lost session state | Restart the app on client device to start a fresh session |
| `java.net.ConnectException` | Bridge server unreachable | Check firewall, network, ensure server is running |

---

## 4. Testing

### Quick smoke test

```bash
# Start a session
curl -X POST "http://localhost:8080/begin" -d "flow=play_integrity&source=com.ridedott.rider"
# → sessionId=a1b2c3d4e5f6

# Snapshot (multi-step)
curl -X POST "http://localhost:8080/snapshot" \
  -d "sessionId=a1b2c3d4e5f6&action=snapshot" \
  --data-urlencode "payload=dummy_data"

# Close
curl -X POST "http://localhost:8080/close" -d "sessionId=a1b2c3d4e5f6"
```

### End-to-end test with microG

1. Build microG from source with the remote DroidGuard patches:
   ```bash
   git clone https://github.com/ZacLou/GmsCore.git
   cd GmsCore
   git checkout issue-2851-multistep-droidguard
   ./gradlew assembleDebug
   ```
2. Install the APK on your client device.
3. Configure the remote DroidGuard URL to point to your bridge server.
4. Run the Dott app and attempt sign-up.
5. Check `logcat` for DroidGuard logs.

---

## 5. Security Considerations

- The bridge server should **not** be exposed to the public internet without TLS
  and authentication. Use a VPN or reverse proxy with HTTPS.
- Session IDs are randomly generated but consider adding authentication tokens.
- The server device contains sensitive Google account data — keep it on a
  trusted network.
- Rotate server device fingerprints regularly (via PIF updates) to stay ahead
  of Google's revocation.

---

## 6. Commercial Offering Idea

The server model naturally supports commercial integrity-attestation services:
- A provider runs a fleet of server devices.
- Users subscribe (monthly/yearly) for access to a bridge server.
- The bridge server load-balances across the device fleet.
- This avoids each user having to maintain their own server device and
  bypass stack.

---

## 7. References

- microG GmsCore: https://github.com/microg/GmsCore
- PlayIntegrityFix: https://github.com/KOWX712/PlayIntegrityFix
- TrickyStore: https://github.com/5ec1cff/TrickyStore
- Magisk: https://github.com/topjohnwu/Magisk
- KernelSU: https://github.com/tiann/KernelSU
