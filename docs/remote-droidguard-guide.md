# Remote DroidGuard Play Integrity Guide

This guide explains how to set up and use the remote DroidGuard service for Play Integrity attestation in microG.

## Overview

Play Integrity API verifies that a device is genuine and unmodified Android. Since microG cannot run the proprietary Google DroidGuard VM, this implementation delegates attestation to a remote service.

The remote DroidGuard flow uses a multi-step session protocol:

1. **begin** — Initialize a DroidGuard session on the remote server.
2. **snapshot** — Request an attestation blob from the remote DroidGuard VM.
3. **close** — Clean up the session on the server.

## Architecture

```
Android App → Play Integrity API → microG DroidGuard → Remote Service → Real DroidGuard VM
```

### Key Components

- **`DroidGuardServiceImpl`** — Entry point for `IDroidGuardService`, implements `guardWithRequest()`.
- **`RemoteHandleImpl`** — Implements `IDroidGuardHandle` using HTTP calls to a remote DroidGuard server.
- **`DroidGuardPreferences`** — Stores the remote server URL (configurable by the user).

## Setup

### 1. Configure the Remote Server URL

The remote DroidGuard server URL must be set. This can be done through the microG Settings UI or by setting the preference key directly.

A compliant remote server must implement the following endpoints:

```
POST /droidguard?action=begin&flow=<flow>&source=<package>
POST /droidguard?action=snapshot&flow=<flow>&source=<package>&sessionId=<id>
POST /droidguard?action=close&sessionId=<id>
```

### 2. Server Response Format

- **begin** response: `sessionId=<uuid>` (URL-encoded key-value pairs)
- **snapshot** response: Base64-encoded attestation blob (URL-safe, no padding)
- **close** response: Any success indicator

### 3. Error Handling

If the `begin` request fails (network error, server unavailable), the implementation falls back to single-step mode where `snapshot` operates without a session.

If `snapshot` or `close` fails, errors are logged and a best-effort error response is returned to the caller.

## Multi-Step Session Protocol

The PR implements the following changes:

### `DroidGuardServiceImpl.guardWithRequest()`

Previously a `TODO("Not yet implemented")` stub. Now:
- Receives `DroidGuardResultsRequest` from the caller.
- Creates a remote handle via `getHandle()`.
- Runs the full begin → snapshot → close lifecycle on a background thread.
- Reports results or errors back through `IDroidGuardCallbacks`.

### `RemoteHandleImpl`

- **`initWithRequest()`** — Starts a session via `beginSession()`, sending flow, source, and request bundle parameters to the server.
- **`beginSession()`** — Sends `action=begin` to the server and stores the returned `sessionId`.
- **`snapshot()`** — Sends `action=snapshot` with the stored `sessionId` and decodes the Base64 response.
- **`close()`** — Sends `action=close` to release the server-side session.

## Testing

### Prerequisites
- A running remote DroidGuard server.
- The remote server URL configured in microG Settings.
- An app that uses the Play Integrity API (e.g., any app with SafetyNet/Play Integrity checks).

### Test Flow
1. Install microG with this PR applied.
2. Configure the remote DroidGuard server URL.
3. Open an app that triggers Play Integrity attestation.
4. Check logcat for `RemoteGuardImpl` log messages:
   - `Session started: sessionId=...`
   - `POST <url> body=...`
   - `Session closed: sessionId=...`

## Limitations

- **Network dependency**: Attestation requires network access at call time.
- **Latency**: Remote attestation is slower than local DroidGuard (typical roundtrip: 500ms–2s).
- **Server trust**: The remote server has full access to the attestation flow. Users should run their own server or trust the provider.
- **No caching**: Each `guardWithRequest()` call creates and destroys a fresh session.
