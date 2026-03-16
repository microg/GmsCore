# Play Integrity over Remote DroidGuard Implementation

## Problem Analysis

Play Integrity uses a **multi-step DroidGuard process** that the current remote implementation doesn't support:

### Current Issue
- Remote DroidGuard only supports single-step flow: `init()` → `snapshot()`
- Play Integrity requires: `initWithRequest()` → Process `DroidGuardInitReply` (pfd + object) → `snapshot()`
- The `DroidGuardInitReply` contains:
  - `ParcelFileDescriptor`: APK file descriptor for the VM
  - `Parcelable object`: Additional initialization data (includes VM key "h")

### Root Cause
In `DroidGuardApiClient.openHandle()`:
```java
DroidGuardInitReply reply = handle.initWithRequest(flow, request);
if (reply == null) {
    handle.init(flow);  // Fallback to simple init
}
if (reply != null) {
    if (reply.pfd != null && reply.object != null) {
        handleDroidGuardData(reply.pfd, (Bundle) reply.object);
    }
}
```

The `RemoteHandleImpl.initWithRequest()` currently returns `null`, forcing fallback to simple `init()`, which doesn't work for Play Integrity flows.

## Solution

### 1. Enhanced RemoteHandleImpl

Support the multi-step protocol by:
- Storing state between `initWithRequest()` and `snapshot()`
- Making HTTP requests to fetch VM data on `initWithRequest()`
- Returning proper `DroidGuardInitReply` with pfd and Bundle
- Using cached data in `snapshot()` call

### 2. Remote DroidGuard Server

Create a standalone server that:
- Receives DroidGuard requests from clients
- Forwards to Google's servers (or handles locally if device passes PI)
- Returns proper responses with VM bytecode
- Manages cache and VM lifecycle

### 3. Documentation

Guide for setting up a DroidGuard server device with:
- Required components (microG, Magisk, PlayIntegrityFix, TrickyStore)
- Server configuration
- Client configuration
- Troubleshooting

## Implementation Plan

### Phase 1: Fix RemoteHandleImpl (Multi-step Support)
- [ ] Update `initWithRequest()` to fetch VM data from server
- [ ] Create temporary file for APK and return ParcelFileDescriptor
- [ ] Return Bundle with VM key and metadata
- [ ] Store flow/request state for `snapshot()` call
- [ ] Update `snapshot()` to use cached VM data

### Phase 2: Create Remote DroidGuard Server
- [ ] Design HTTP API protocol (compatible with existing NetworkHandleProxyFactory)
- [ ] Implement server in Kotlin/Java or Python
- [ ] Support multi-step flow handling
- [ ] Add caching and rate limiting
- [ ] Include health check endpoint

### Phase 3: Server Device Setup Guide
- [ ] Document required ROM/modules
- [ ] Provide installation scripts
- [ ] Configuration examples
- [ ] Security considerations

### Phase 4: Testing & Documentation
- [ ] Test with Play Integrity API
- [ ] Test with affected apps (e.g., Dott)
- [ ] Write user documentation
- [ ] Create troubleshooting guide

## Technical Details

### Protocol Design

**Client → Server (initWithRequest)**
```
POST /init?flow=<flow>&source=<packageName>
Content-Type: application/x-protobuf

<Request protobuf>
```

**Server → Client Response**
```
HTTP 200 OK
Content-Type: application/x-protobuf

<SignedResponse protobuf>
```

**Client → Server (snapshot)**
```
POST /snapshot?flow=<flow>&source=<packageName>&vmKey=<key>
Content-Type: application/x-www-form-urlencoded

<map data as form-encoded>
```

**Server → Client Response**
```
HTTP 200 OK
Content-Type: text/plain

<base64-encoded result bytes>
```

### Server Implementation Options

**Option A: Kotlin/Java (Native microG)**
- Pros: Reuses existing code, type-safe
- Cons: Requires Android environment or complex setup

**Option B: Python (Standalone)**
- Pros: Easy deployment, cross-platform
- Cons: Need to reimplement protocol handling

**Recommendation**: Start with Python for easy deployment, provide Kotlin version later.

## Files to Create/Modify

### Modified Files
1. `play-services-droidguard/core/src/main/kotlin/org/microg/gms/droidguard/core/RemoteHandleImpl.kt`
   - Add multi-step support
   - Implement proper `initWithRequest()`
   - Cache VM data between calls

### New Files
1. `play-services-droidguard/server/README.md` - Server documentation
2. `play-services-droidguard/server/droidguard-server.py` - Reference server implementation
3. `play-services-droidguard/server/docker-compose.yml` - Docker deployment
4. `play-services-droidguard/docs/remote-setup-guide.md` - Setup guide

## Success Criteria

- [ ] Play Integrity API works over remote DroidGuard
- [ ] Dott app and similar Firebase App Check apps work
- [ ] Server can run on stock Android device with proper modules
- [ ] Client can connect to remote server seamlessly
- [ ] Documentation enables users to set up their own server
- [ ] Bounty claimed with working PR

## References

- Issue: https://github.com/microg/GmsCore/issues/2851
- Play Integrity API: https://developer.android.com/google/play/integrity
- Related: #1967, #1281
