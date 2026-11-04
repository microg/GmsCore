# Remote DroidGuard Server Setup Guide

This guide explains how to turn an old Android phone into a remote DroidGuard server that other devices running microG can use for Play Integrity attestation.

## Overview

When microG runs in **Remote DroidGuard** mode, it sends DroidGuard requests to a server over HTTP instead of running DroidGuard locally. This lets you use one phone (the "server") with a passing Play Integrity status to serve attestation tokens to other devices.

### Architecture

```
Client Device (microG)          Server Device (old phone)
┌──────────────────────┐       ┌──────────────────────────┐
│  App requesting      │       │  Termux + Python server   │
│  Play Integrity      │──────>│  + microG embedded mode   │
│  token               │<──────│  + DroidGuard runtime     │
│                      │       │                           │
│  DroidGuard mode:    │       │  Handles DroidGuard       │
│  Network (Remote)    │       │  requests locally          │
└──────────────────────┘       └──────────────────────────┘
```

## Requirements

### Server Device (the phone hosting DroidGuard)

- An Android phone that can pass **DEVICE** integrity level (many phones from ~2015 onward work, e.g. Nexus 5X)
- Android 8.0+ (API 26+)
- microG GmsCore installed and configured
- DroidGuard enabled in **Embedded** mode
- Network connectivity (Wi-Fi recommended)
- Constant power supply

### Client Device (the phone requesting tokens)

- microG GmsCore installed
- DroidGuard enabled in **Network** mode
- Network connectivity to reach the server

## Server Setup

### Step 1: Prepare the Server Phone

1. Install microG GmsCore on the server phone
2. Open microG Settings > Google Accounts and sign in with a Google account
3. Open microG Settings > Google device registration and enable it
4. Open microG Settings > DroidGuard:
   - Set mode to **Embedded**
   - Enable DroidGuard
5. Verify DroidGuard is working by checking the self-check page

### Step 2: Install Termux

Install [Termux](https://f-droid.org/en/packages/com.termux/) from F-Droid (the Google Play version is outdated).

Open Termux and run:

```bash
pkg update && pkg upgrade
pkg install python
```

### Step 3: Download the Server Script

Option A - Download directly in Termux:
```bash
curl -LO https://raw.githubusercontent.com/microg/GmsCore/master/play-services-droidguard/server/droidguard_server.py
```

Option B - Transfer from another device:
```bash
# On your computer, push the script to the phone
adb push droidguard_server.py /sdcard/
# In Termux, move it to a working directory
cp /sdcard/droidguard_server.py ~/droidguard_server.py
```

### Step 4: Start the Server

```bash
python3 droidguard_server.py --port 8080
```

The server will start listening on port 8080. You should see:
```
DroidGuard server starting on 0.0.0.0:8080
Configure microG client to use: http://<device-ip>:8080/droidguard/
```

### Step 5: Find the Server's IP Address

In Termux, run:
```bash
ifconfig | grep "inet "
```

Or check the phone's Wi-Fi settings for the IP address.

## Client Configuration

On the device that needs Play Integrity tokens:

1. Open microG Settings > DroidGuard
2. Set mode to **Remote**
3. Enter the server URL: `http://<server-ip>:8080/droidguard/`
4. Save

The client will now forward DroidGuard requests to the server.

## Troubleshooting

### Server shows "ERROR :content command not available"

The server script uses Android's `content` command to invoke the DroidGuard service. This must run on an Android device with microG installed. If you see this error:

- Make sure the script is running in Termux on the Android device
- Make sure microG GmsCore is installed with DroidGuard enabled

### Client cannot connect to server

- Check that both devices are on the same network
- Check the server's firewall settings
- Try opening the server URL in a browser on the client device
- Ensure the server is running and listening on the correct port

### Play Integrity still fails

- Verify the server phone passes Play Integrity checks
- Check microG logs on both devices for errors
- Ensure the server phone has a valid Google account configured
- Some apps require **STRONG** integrity which requires a hardware-backed attestation device

### Session errors

If you see session-related errors in the logs, the server may have cleaned up stale sessions. Restart the server script.

## Security Notes

- The server communicates over HTTP, not HTTPS. Use this only on trusted networks
- Anyone on the same network can potentially use your server
- The server script does not encrypt or authenticate requests
- Consider running the server behind a VPN for additional security

## Limitations

- Play Integrity requests a limited number of tokens per device per time period
- The server phone must remain powered on and connected to the network
- DEVICE integrity level is the most achievable on older phones; STRONG integrity requires hardware attestation
- Some apps may reject tokens from remote DroidGuard depending on their configuration
