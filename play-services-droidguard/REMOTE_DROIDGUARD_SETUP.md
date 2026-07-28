# Remote DroidGuard Server Setup Guide

This guide explains how to use the bundled **Remote DroidGuard server** to let
devices running microG obtain Play Integrity / DroidGuard tokens over the
network from a single phone that already passes integrity checks.

## Overview

When a phone runs microG in **Remote DroidGuard** mode, every DroidGuard call
is forwarded over HTTP to a configured server. This guide covers turning an
old Android phone (the *server device*) into that server so other phones
(the *client devices*) can outsource attestation to it.

```
Client device (microG Remote)       Server device (old phone)
┌─────────────────────────┐         ┌────────────────────────────┐
│ App triggers Play       │  HTTP   │ Termux + droidguard_server │
│ Integrity check         ├────────>│  + microG Embedded DroidGuard│
│                         │  token  │                            │
│ DroidGuard mode: Remote │<────────├────────────────────────────┤
└─────────────────────────┘         └────────────────────────────┘
```

## Requirements

### Server device (the phone that produces tokens)

* An Android phone that passes at least **DEVICE** integrity level with microG
  (many phones from ~2015 onward work; e.g. Nexus 5X).
* Android 8.0+ (API 26+).
* microG GmsCore installed and signed into a Google account.
* DroidGuard enabled in **Embedded** mode.
* Network connectivity (Wi-Fi recommended).
* Constant power supply.
* Termux for running the Python server script.

### Client device (the phone requesting tokens)

* microG GmsCore installed.
* DroidGuard enabled in **Network (Remote)** mode.
* Network connectivity to reach the server.

## Server setup

### Step 1 — Prepare the server phone

1. Install microG GmsCore.
2. Open microG Settings → Google Accounts and sign in.
3. Open microG Settings → Google device registration and enable it.
4. Open microG Settings → DroidGuard:
   - Set mode to **Embedded**.
   - Enable DroidGuard.
5. Verify the phone passes Play Integrity at DEVICE level.

### Step 2 — Install Termux

Install [Termux](https://f-droid.org/en/packages/com.termux/) from F-Droid
(the Play Store version is outdated). In Termux run:

```bash
pkg update && pkg upgrade
pkg install python
```

### Step 3 — Download the server script

```bash
curl -LO https://raw.githubusercontent.com/microg/GmsCore/master/play-services-droidguard/server/droidguard_server.py
```

Or transfer it via adb:

```bash
adb push droidguard_server.py /sdcard/
# then in Termux:
cp /sdcard/droidguard_server.py ~/droidguard_server.py
```

### Step 4 — Start the server

**Test mode** (returns a fixed sample token — useful to validate the client
wiring before hooking the real runtime):

```bash
python3 droidguard_server.py --port 8080 --token dG9rZW46dGVzdA==
```

**Proxy mode** (forwards to an internal DroidGuard wrapper on the same phone):

```bash
python3 droidguard_server.py --port 8080 --proxy localhost:9090
```

You should see:

```
DroidGuard server starting on 0.0.0.0:8080
configure client with: http://<device-ip>:8080/
```

### Step 5 — Find the server IP

```bash
ip addr show wlan0 | grep inet | awk '{print $2}' | cut -d/ -f1
```

Or check Wi-Fi settings on the phone.

## Client configuration

On the phone that needs Play Integrity tokens:

1. Open microG Settings → DroidGuard.
2. Set mode to **Network (Remote)**.
3. Enter the Remote URL: `http://<server-ip>:8080/`.
4. Save.

The client will now forward both single-step DroidGuard calls and the
multi-step Play Integrity flow to the server.

## How the multi-step Play Integrity flow works

Play Integrity sometimes uses a multi-step DroidGuard session rather than a
single snapshot. The client and server exchange a small sequence:

1. **`/session/begin`** — client sends the initial request; server creates a
   session and returns `{"sessionId": "<id>"}`.
2. **`/session/next`** — client submits intermediate steps one at a time.
3. **`/session/snapshot`** — client finalizes; server combines all steps,
   forwards to the embedded DroidGuard runtime, and returns raw token bytes.
4. **`/session/close`** — clean up (optional).

Metadata such as `sessionId`, `isMultiStep`, `stepNumber`, and `totalSteps`
is carried through the request bundle so the local DroidGuard runtime sees a
Play Integrity-compatible flow.

## API reference

| Method | Path | Description |
|--------|------|-------------|
| GET | `/status` | `{"status":"ok","active_sessions":N}` |
| POST | `/` or `/droidguard/` | Single-step: returns raw token bytes |
| POST | `/session/begin` | Create multi-step session; returns JSON sessionId |
| POST | `/session/next` | Append step (`sessionId=<id>` query); 204 OK |
| POST | `/session/snapshot` | Finalize and return raw token bytes |
| POST | `/session/close` | Discard session |

All POST requests use `application/x-www-form-urlencoded` bodies.
Query parameters `flow` and `source` identify the flow; any
`x-request-*` parameters are preserved and forwarded.

## Troubleshooting

### Client receives `NotImplementedError`
Enable DroidGuard on the client and make sure the mode is set to **Remote**
with a valid URL.

### Server returns `session not found`
A multi-step session was asked to be finalized or closed before
`/session/begin` returned successfully. Check network connectivity and that
the client re-sends the same `sessionId` on every subsequent call.

### Token does not pass integrity on the client
The server phone itself must pass Play Integrity. Re-check the server's
self-check page and, if needed, apply the usual passing-phone setup
(Magisk / KernelSU + PlayIntegrityFix + TrickyStore) to the **server** phone.

### `--proxy` receives a connection error
The internal wrapper service must be listening on the given port. Verify with
`ss -ltnp` inside Termux.

## Production notes

For a robust production deployment, the Python script should be replaced or
wrapped by a small GmsCore IntentService that binds the embedded DroidGuard
runtime directly and exposes the same REST surface. The REST contract above
is stable and serves as the integration target for that wrapper.
