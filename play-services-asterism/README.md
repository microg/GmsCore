# play-services-asterism

> **Asterism: RCS Consent Management for microG GmsCore**

The `play-services-asterism` module implements Google Play Services' Asterism API, which manages Rich Communication Services (RCS) consent state. This module is part of the microG GmsCore RCS support stack and provides the service layer that Google Messages and other RCS clients use to query and manage user consent for RCS features.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [API Reference](#api-reference)
  - [AIDL Interfaces](#aidl-interfaces)
  - [Parcelable Types](#parcelable-types)
- [Implementation Details](#implementation-details)
  - [AsterismService](#asterismservice)
  - [AsterismApiService](#asterismapiservice)
  - [AsterismConsentStore](#asterismconsentstore)
- [Setup & Integration](#setup--integration)
- [Consent Lifecycle](#consent-lifecycle)
- [States & Error Codes](#states--error-codes)
- [Build Configuration](#build-configuration)
- [Permissions](#permissions)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)

## Overview

Asterism is the consent management layer for Google's RCS infrastructure. Before Google Messages (or any RCS client) can enable RCS features like read receipts, typing indicators, high-quality media sharing, and end-to-end encryption, it must obtain explicit user consent through the Asterism service.

### What Asterism Does

- **Consent Storage:** Persists user consent state (granted, denied, pending, expired) across device reboots
- **Token Management:** Generates and manages consent tokens with configurable TTL (Time-To-Live)
- **State Transitions:** Manages the full consent lifecycle: grant → active → refresh → expire → revoke
- **Callback System:** Notifies registered clients of consent state changes in real-time
- **Package Verification:** Identifies calling packages to prevent unauthorized consent operations
- **Device Identity:** Maintains a stable device identifier for consent binding

### Relationship to RCS Stack

```
┌─────────────────────────────────────────────────────────┐
│                   Google Messages App                     │
├─────────────────────────────────────────────────────────┤
│  Constellation API            │    Asterism API          │
│  (Phone verification, IID)    │    (Consent management)  │
├──────────────────────────────┼──────────────────────────┤
│  play-services-constellation  │  play-services-asterism  │
├──────────────────────────────┴──────────────────────────┤
│              microG GmsCore Foundation                    │
│    (PackageUtils, GServices, Auth, MultiPods)            │
└─────────────────────────────────────────────────────────┘
```

Asterism and Constellation work together: Constellation handles phone number verification and IID token management, while Asterism manages the user's consent to use RCS features on verified numbers. Google Messages queries both services to determine if RCS should be enabled for a given conversation.

## Architecture

The Asterism module follows microG's standard service architecture with three main components:

```
┌──────────────────────────────────────┐
│        AndroidManifest.xml           │
│  Registers AsterismService           │
│  Process: com.google.android.gms     │
│           .unstable                  │
└──────────────┬───────────────────────┘
               │ starts
┌──────────────▼───────────────────────┐
│         AsterismService              │
│  (android.app.Service)               │
│  • Lifecycle management              │
│  • IBinder provider                  │
│  • Runs in GMS unstable process      │
└──────────────┬───────────────────────┘
               │ creates
┌──────────────▼───────────────────────┐
│      AsterismApiService              │
│  (IAsterismApiService.Stub)          │
│  • AIDL IPC endpoint                 │
│  • Request validation                │
│  • Callback orchestration            │
└──────┬────────────────────┬──────────┘
       │                    │
       │ delegates          │ manages
┌──────▼──────────┐  ┌──────▼──────────────┐
│AsterismConsent  │  │ CopyOnWriteArrayList │
│    Store        │  │ <IAsterismCallbacks> │
│ • SharedPrefs   │  │ • Thread-safe list   │
│ • State machine │  │ • Auto-cleanup       │
│ • Token gen     │  │ • RemoteException    │
│ • TTL tracking  │  │   handling           │
└─────────────────┘  └─────────────────────┘
```

### Component Interaction Flow

1. **Google Messages** binds to `AsterismService` via Android's service binding mechanism
2. The service returns an `AsterismApiService` instance (AIDL Stub)
3. Google Messages calls AIDL methods to check/set consent state
4. `AsterismApiService` validates the request, delegates to `AsterismConsentStore`
5. State changes are persisted to `SharedPreferences` and broadcast to registered callbacks

### Data Flow

```
Client App                        AsterismService
    │                                   │
    ├─ bindService(intent) ────────────►│
    │                                   ├─ onCreate()
    │                                   │  └─ new AsterismApiService(ctx)
    │◄─ IBinder (AsterismApiService) ───┤
    │                                   │
    ├─ getAsterismConsent(request) ────►│
    │                                   ├─ validateRequest()
    │                                   ├─ consentStore.getCurrentState()
    │                                   │  └─ SharedPreferences read
    │◄─ GetAsterismConsentResponse ─────┤
    │                                   │
    ├─ setAsterismConsent(request) ────►│
    │                                   ├─ validateRequest()
    │                                   ├─ switch(action):
    │                                   │  ├─ GRANT → grantConsent()
    │                                   │  ├─ REVOKE → revokeConsent()
    │                                   │  ├─ REFRESH → refreshConsent()
    │                                   │  └─ CHECK → getCurrentState()
    │                                   ├─ SharedPreferences write
    │                                   ├─ notifyCallbacks()
    │                                   │  └─ cb.onConsentStateChanged()
    │◄─ SetAsterismConsentResponse ─────┤
```

## API Reference

### AIDL Interfaces

#### IAsterismApiService

The primary AIDL interface for Asterism consent operations. Located at:
`play-services-asterism/src/main/aidl/com/google/android/gms/asterism/internal/IAsterismApiService.aidl`

```java
interface IAsterismApiService {
    GetAsterismConsentResponse getAsterismConsent(in GetAsterismConsentRequest request) = 0;
    SetAsterismConsentResponse setAsterismConsent(in SetAsterismConsentRequest request) = 1;
    void registerCallbacks(in IAsterismCallbacks callbacks) = 2;
    void unregisterCallbacks(in IAsterismCallbacks callbacks) = 3;
}
```

**Methods:**

| Method | Transaction ID | Description |
|--------|---------------|-------------|
| `getAsterismConsent` | 0 | Retrieves the current Asterism consent state for the calling package. Returns consent status, timestamps, token, and any errors. |
| `setAsterismConsent` | 1 | Performs consent state mutations: GRANT, REVOKE, REFRESH, or CHECK_STATUS. Triggers callback notifications on state changes. |
| `registerCallbacks` | 2 | Registers an `IAsterismCallbacks` instance to receive real-time consent state change notifications. Thread-safe. |
| `unregisterCallbacks` | 3 | Unregisters a previously registered callback. Removes failed callbacks automatically. |

**Request Handling:**

- `getAsterismConsent`: Returns current consent state from persistent storage. If the consent has expired (current time > expiry timestamp), returns `CONSENT_EXPIRED` state.
- `setAsterismConsent`: Supports four actions:
  - `ACTION_GRANT`: Grants consent with optional device integrity check, generates consent token with TTL
  - `ACTION_REVOKE`: Revokes consent, clears consent token
  - `ACTION_REFRESH`: Extends consent expiry without changing state, increments refresh counter
  - `ACTION_CHECK_STATUS`: Validates current consent is still valid (granted + not expired)

#### IAsterismCallbacks

Callback interface for asynchronous consent state notifications. Located at:
`play-services-asterism/src/main/aidl/com/google/android/gms/asterism/internal/IAsterismCallbacks.aidl`

```java
oneway interface IAsterismCallbacks {
    void onConsentStateChanged(int consentState, long timestamp) = 0;
    void onAsterismError(int errorCode, String errorMessage) = 1;
}
```

**Methods:**

| Method | Description |
|--------|-------------|
| `onConsentStateChanged` | Called whenever consent state changes (grant, revoke, expire). Provides the new state and timestamp. |
| `onAsterismError` | Called when an error occurs during consent operations. Provides error code and human-readable message. |

The `oneway` keyword means these calls are asynchronous — the caller does not block waiting for the callback to complete.

### Parcelable Types

#### GetAsterismConsentRequest

Request object for querying consent state.

| Field | Type | Description |
|-------|------|-------------|
| `packageName` | String | The calling package name (optional, auto-detected if null) |
| `requestId` | long | Client-assigned request identifier for correlation |
| `includeDeviceInfo` | boolean | Whether to include device information in response |

#### GetAsterismConsentResponse

Response object containing current consent state.

| Field | Type | Description |
|-------|------|-------------|
| `consentState` | int | Current consent state (see [States](#states)) |
| `consentTimestamp` | long | Unix timestamp when consent was last modified (ms) |
| `expiryTimestamp` | long | Unix timestamp when consent expires (ms, 0 = never) |
| `consentToken` | String | Opaque consent token (null when not granted) |
| `resultCode` | int | Result code (0 = success) |
| `errorMessage` | String | Error description (null on success) |

**Consent States:**

| Constant | Value | Description |
|----------|-------|-------------|
| `CONSENT_UNKNOWN` | 0 | No consent state has been established |
| `CONSENT_GRANTED` | 1 | User has granted RCS consent |
| `CONSENT_DENIED` | 2 | User has explicitly denied RCS consent |
| `CONSENT_PENDING` | 3 | Consent decision is pending |
| `CONSENT_EXPIRED` | 4 | Previously granted consent has expired |

#### SetAsterismConsentRequest

Request object for modifying consent state.

| Field | Type | Description |
|-------|------|-------------|
| `action` | int | Action to perform (GRANT, REVOKE, REFRESH, CHECK_STATUS) |
| `callingPackage` | String | The calling package (optional) |
| `ttlMillis` | long | Time-to-live for consent in milliseconds (0 = default 7 days) |
| `requiresDeviceIntegrity` | boolean | Whether device integrity check is required |
| `consentToken` | String | Existing consent token (for refresh/check operations) |

**Action Constants:**

| Constant | Value | Description |
|----------|-------|-------------|
| `ACTION_GRANT` | 1 | Grant RCS consent |
| `ACTION_REVOKE` | 2 | Revoke RCS consent |
| `ACTION_REFRESH` | 3 | Refresh consent TTL |
| `ACTION_CHECK_STATUS` | 4 | Check consent validity |

#### SetAsterismConsentResponse

Response object for consent modification operations.

| Field | Type | Description |
|-------|------|-------------|
| `resultCode` | int | Result code (see below) |
| `consentState` | int | New consent state after operation |
| `errorMessage` | String | Error description (null on success) |
| `consentToken` | String | New/generated consent token |
| `expiryTimestamp` | long | New expiry timestamp |

**Result Codes:**

| Constant | Value | Description |
|----------|-------|-------------|
| `RESULT_OK` | 0 | Operation completed successfully |
| `RESULT_ERROR_UNKNOWN` | 1 | Unknown error occurred |
| `RESULT_ERROR_INVALID_ARGUMENT` | 2 | Invalid request parameters |
| `RESULT_ERROR_CONSENT_EXPIRED` | 3 | Consent has expired |
| `RESULT_ERROR_CONSENT_NOT_FOUND` | 4 | No consent record found |
| `RESULT_ERROR_INTERNAL` | 5 | Internal service error |

## Implementation Details

### AsterismService

**Location:** `play-services-asterism/core/src/main/kotlin/org/microg/gms/asterism/core/AsterismService.kt`

The `AsterismService` is an Android `Service` that hosts the Asterism API implementation. It runs in the `com.google.android.gms.unstable` process to match Google Play Services behavior for RCS-related services.

**Lifecycle:**

```
onCreate()  →  Creates AsterismApiService instance
onBind()    →  Returns AsterismApiService as IBinder
onUnbind()  →  Logs unbind event, returns to super
onDestroy() →  Logs destruction event
```

**Key Characteristics:**

- Runs in the **GMS unstable process** (`com.google.android.gms.unstable`)
- Declared in `AndroidManifest.xml` with `exported="true"` for external binding
- Lightweight service — delegates all logic to `AsterismApiService`
- Thread-safe: `AsterismApiService` handles concurrency internally
- Logging at debug/trace level for all lifecycle events

**Process Configuration:**

The service is isolated to the unstable GMS process:
- Ensures RCS consent operations don't block core GMS functionality
- Matches Google Play Services' process model
- Crash isolation: Asterism issues don't bring down the entire GmsCore

### AsterismApiService

**Location:** `play-services-asterism/core/src/main/kotlin/org/microg/gms/asterism/core/AsterismApiService.kt`

The `AsterismApiService` implements `IAsterismApiService.Stub()`, serving as the AIDL IPC endpoint. It bridges the AIDL interface to the local consent store.

**Dependencies:**

- `Context`: Android context for SharedPreferences and package resolution
- `AsterismConsentStore`: Persistent consent state manager
- `PackageUtils`: microG utility for caller identification

**Concurrency Model:**

- **Callback List:** `CopyOnWriteArrayList<IAsterismCallbacks>` — thread-safe iteration during notifications
- **Consent Store:** `AsterismConsentStore` uses `@Synchronized` methods for atomic state transitions
- **Request Handling:** AIDL calls arrive on Binder threads; the store serializes writes

**Error Handling:**

1. **Null Request Guard:** Both `getAsterismConsent` and `setAsterismConsent` check for null requests and return appropriate error responses
2. **Exception Wrapping:** All operations are wrapped in try-catch blocks that return structured error responses
3. **Callback Cleanup:** Callbacks that throw `RemoteException` (dead client) are automatically removed from the list
4. **Logging:** All operations log at appropriate levels (debug for normal flow, warning for anomalies, error for failures)

**Callback Notification Flow:**

```
setAsterismConsent() → consentStore.operation() → notifyCallbacks(newState)
                                                        │
                                        ┌───────────────┴───────────────┐
                                        │ for each callback in list:     │
                                        │   try: cb.onConsentStateChanged│
                                        │   catch RemoteException:       │
                                        │     remove dead callback       │
                                        └───────────────────────────────┘
```

### AsterismConsentStore

**Location:** `play-services-asterism/core/src/main/kotlin/org/microg/gms/asterism/core/AsterismConsentStore.kt`

The `AsterismConsentStore` is the persistent storage layer for Asterism consent state. It uses Android `SharedPreferences` for durability across process restarts and device reboots.

**Storage Keys:**

| Key | Type | Description |
|-----|------|-------------|
| `consent_state` | Int | Current consent state enum value |
| `consent_timestamp` | Long | Timestamp of last state modification |
| `expiry_timestamp` | Long | When the current consent expires |
| `consent_token` | String | Opaque consent token (UUID) |
| `last_refresh_timestamp` | Long | Last consent refresh time |
| `refresh_count` | Int | Number of times consent has been refreshed |
| `device_id` | String | Stable device identifier |

**State Transitions:**

```
                    ┌──────────┐
                    │  UNKNOWN  │ (initial state)
                    └─────┬─────┘
                          │ grantConsent()
                    ┌─────▼─────┐
               ┌───►│  GRANTED  │◄──────────┐
               │    └─────┬─────┘           │
               │          │                 │
               │    ┌─────▼─────┐    refreshConsent()
               │    │  EXPIRED  │           │
               │    └─────┬─────┘           │
               │          │                 │
               │    revokeConsent()   ┌─────┴──────┐
               │          │          │   PENDING   │
               └──────────▼──────────┴─────────────┘
                       DENIED
```

**Token Generation:**

- Consent tokens are generated as UUID v4 strings
- Each `grantConsent()` call generates a new token (unless one is provided externally)
- Tokens are cleared on `revokeConsent()` or `clearAll()`
- Default TTL: **7 days** (604,800,000 ms) — configurable via request parameter

**Methods:**

| Method | Thread Safety | Description |
|--------|---------------|-------------|
| `getCurrentState()` | `@Synchronized` | Returns current consent state; auto-detects expiry |
| `grantConsent(token, ttl)` | `@Synchronized` | Sets state to GRANTED, generates/uses token, sets expiry |
| `revokeConsent()` | `@Synchronized` | Sets state to DENIED, clears token |
| `refreshConsent(ttl)` | `@Synchronized` | Extends expiry for GRANTED state, increments counter |
| `setPending()` | `@Synchronized` | Sets state to PENDING |
| `getDeviceId()` | Thread-safe (lazy) | Returns stable device UUID |
| `getRefreshCount()` | Read-only | Number of refresh operations |
| `getLastRefreshTimestamp()` | Read-only | Timestamp of last refresh |
| `clearAll()` | Write operation | Clears all stored consent data |

**Expiry Detection:**

The store checks expiry on every `getCurrentState()` call:
```kotlin
val isExpired = state == CONSENT_GRANTED
    && expiryTimestamp > 0
    && System.currentTimeMillis() > expiryTimestamp
```

If expired, the state is automatically updated to `CONSENT_EXPIRED` in storage before returning.

## Setup & Integration

### Adding to Your Build

The `play-services-asterism` module is part of the microG GmsCore project. To include it:

**1. Clone and build the full GmsCore project:**

```bash
git clone https://github.com/microg/GmsCore.git
cd GmsCore
# If using the RCS branch:
git checkout feat/rcs-support-2994
./gradlew :play-services-asterism:assemble
```

**2. Add dependency in your app's `build.gradle`:**

```groovy
dependencies {
    implementation project(':play-services-asterism')
}
```

**3. Manifest Declaration:**

The service is registered in `play-services-asterism/src/main/AndroidManifest.xml`:

```xml
<service
    android:name="org.microg.gms.asterism.core.AsterismService"
    android:process="com.google.android.gms.unstable"
    android:exported="true">
    <intent-filter>
        <action android:name="com.google.android.gms.asterism.START" />
    </intent-filter>
</service>
```

### Binding from a Client App

```kotlin
val intent = Intent("com.google.android.gms.asterism.START").apply {
    setPackage("com.google.android.gms")
}

bindService(intent, object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
        val apiService = IAsterismApiService.Stub.asInterface(binder)
        
        // Check consent state
        val response = apiService.getAsterismConsent(
            GetAsterismConsentRequest().apply {
                packageName = context.packageName
                requestId = 1
            }
        )
        
        when (response.consentState) {
            GetAsterismConsentResponse.CONSENT_GRANTED -> {
                Log.i(TAG, "RCS consent granted, token: ${response.consentToken}")
            }
            GetAsterismConsentResponse.CONSENT_DENIED -> {
                Log.w(TAG, "RCS consent denied by user")
            }
            GetAsterismConsentResponse.CONSENT_UNKNOWN -> {
                Log.d(TAG, "No consent state yet — prompt user")
            }
        }
    }
    
    override fun onServiceDisconnected(name: ComponentName?) {
        Log.w(TAG, "AsterismService disconnected")
    }
}, Context.BIND_AUTO_CREATE)
```

### Registering for Callbacks

```kotlin
val callbacks = object : IAsterismCallbacks.Stub() {
    override fun onConsentStateChanged(consentState: Int, timestamp: Long) {
        Log.i(TAG, "Consent state changed to: $consentState at $timestamp")
        // Update UI, notify messaging stack, etc.
    }
    
    override fun onAsterismError(errorCode: Int, errorMessage: String?) {
        Log.e(TAG, "Asterism error $errorCode: $errorMessage")
    }
}

apiService.registerCallbacks(callbacks)

// Don't forget to unregister when appropriate:
// apiService.unregisterCallbacks(callbacks)
```

## Consent Lifecycle

### Typical RCS Setup Flow

```
1. Google Messages installed/updated
       │
2. Messages binds to AsterismService
       │
3. Messages calls getAsterismConsent()
       │  Returns: CONSENT_UNKNOWN
       ▼
4. Messages prompts user: "Enable RCS Chat features?"
       │
   ┌───┴───────────┐
   │ User Accepts  │    User Declines
   ▼               ▼
5. setAsterismConsent(ACTION_GRANT)    setAsterismConsent(ACTION_REVOKE)
   │                                    │
   │ State: CONSENT_GRANTED             │ State: CONSENT_DENIED
   │ Token: <UUID>                      │ Token: null
   │ Expiry: now + 7 days               │
   ▼                                    ▼
6. Messages proceeds with             Messages respects denial,
   Constellation verification          disables RCS features
                    │
7. After 7 days, consent expires
   → getAsterismConsent() returns CONSENT_EXPIRED
                    │
8. Messages calls setAsterismConsent(ACTION_REFRESH)
   → Expiry extended by another 7 days
   → Or user must re-grant if refresh fails
```

### Token Lifecycle

```
grantConsent() → Token generated (UUID)
      │
      ├─ Token valid for TTL period (default 7 days)
      │
      ├─ refreshConsent() → Token preserved, expiry extended
      │
      └─ revokeConsent() or expiry → Token invalidated/cleared
```

## States & Error Codes

### Consent State Reference

| State | Value | Meaning | Token Present? | RCS Behavior |
|-------|-------|---------|----------------|--------------|
| `CONSENT_UNKNOWN` | 0 | No consent decision made | No | Prompt user for consent |
| `CONSENT_GRANTED` | 1 | User granted consent | Yes | Enable RCS features |
| `CONSENT_DENIED` | 2 | User denied consent | No | Disable RCS features |
| `CONSENT_PENDING` | 3 | Consent decision pending | No | Wait for user action |
| `CONSENT_EXPIRED` | 4 | Previously granted consent expired | No | Prompt for re-consent |

### Error Code Reference

| Code | Constant | Typical Cause | Resolution |
|------|----------|--------------|------------|
| 0 | `RESULT_OK` | Success | N/A |
| 1 | `RESULT_ERROR_UNKNOWN` | Unexpected exception | Check logs for exception details |
| 2 | `RESULT_ERROR_INVALID_ARGUMENT` | Null request or invalid action | Fix request parameters |
| 3 | `RESULT_ERROR_CONSENT_EXPIRED` | Consent TTL elapsed | Call ACTION_REFRESH or ACTION_GRANT |
| 4 | `RESULT_ERROR_CONSENT_NOT_FOUND` | No consent record exists | Call ACTION_GRANT first |
| 5 | `RESULT_ERROR_INTERNAL` | Internal service error | Check service logs |

## Build Configuration

### Module build.gradle

The module's `build.gradle` is located at `play-services-asterism/build.gradle` and configures:

- **SDK Versions:** Target SDK matching GmsCore project
- **Dependencies:** Core GmsCore libraries, AndroidX annotations
- **AIDL Processing:** Automatic AIDL → Java stub generation
- **Kotlin Compilation:** JVM target compatibility

### Dependencies

```
play-services-asterism/core
├── com.google.android.gms:play-services-base (stubs)
├── androidx.annotation:annotation
├── org.microg.gms:gmscore-common (PackageUtils, etc.)
└── Android framework (android.app.Service, android.content.SharedPreferences)
```

## Permissions

The module does not require any special Android permissions beyond what GmsCore already holds. Consent data is stored in the app's private `SharedPreferences`, requiring no additional storage permissions.

For RCS functionality, the following permissions are typically needed by the consuming app (Google Messages):
- `android.permission.INTERNET`
- `android.permission.ACCESS_NETWORK_STATE`
- `android.permission.RECEIVE_SMS` (for verification SMS)
- `android.permission.READ_PHONE_STATE` (for phone number detection)

## Testing

### Unit Testing AsterismConsentStore

```kotlin
@Test
fun testConsentLifecycle() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val store = AsterismConsentStore(context)
    
    // Initial state should be UNKNOWN
    val initial = store.getCurrentState()
    assertEquals(GetAsterismConsentResponse.CONSENT_UNKNOWN, initial.consentState)
    
    // Grant consent
    val granted = store.grantConsent(null, 3600000L) // 1 hour TTL
    assertEquals(GetAsterismConsentResponse.CONSENT_GRANTED, granted.consentState)
    assertNotNull(granted.consentToken)
    assertTrue(granted.expiryTimestamp > System.currentTimeMillis())
    
    // Refresh consent
    val refreshed = store.refreshConsent(7200000L) // 2 hour TTL
    assertEquals(GetAsterismConsentResponse.CONSENT_GRANTED, refreshed.consentState)
    assertEquals(granted.consentToken, refreshed.consentToken) // Token preserved
    
    // Revoke consent
    val revoked = store.revokeConsent()
    assertEquals(GetAsterismConsentResponse.CONSENT_DENIED, revoked.consentState)
    assertNull(revoked.consentToken)
}
```

### Integration Testing

For integration testing, bind to the service and verify AIDL method responses:

```kotlin
@Test
fun testAidlRoundTrip() {
    // Bind to service
    val service = bindAsterismService()
    
    // Grant consent
    val grantRequest = SetAsterismConsentRequest().apply {
        action = SetAsterismConsentRequest.ACTION_GRANT
        ttlMillis = 60000L
    }
    val grantResponse = service.setAsterismConsent(grantRequest)
    assertEquals(SetAsterismConsentResponse.RESULT_OK, grantResponse.resultCode)
    
    // Verify state
    val stateResponse = service.getAsterismConsent(GetAsterismConsentRequest())
    assertEquals(GetAsterismConsentResponse.CONSENT_GRANTED, stateResponse.consentState)
}
```

## Troubleshooting

### Common Issues

#### Service Not Binding

**Symptom:** `bindService()` returns `false` or `onServiceConnected` never fires.

**Causes & Fixes:**
1. **GmsCore not installed:** Ensure microG GmsCore is installed on the device
2. **Wrong intent action:** Use `"com.google.android.gms.asterism.START"`
3. **Process mismatch:** Check that the service is running in the correct process
4. **Verify with adb:**
   ```bash
   adb shell dumpsys package com.google.android.gms | grep -A5 "Service Resolver"
   ```

#### Consent Not Persisting

**Symptom:** Consent state resets after app restart.

**Causes & Fixes:**
1. **SharedPreferences corruption:** Check `asterism_consent_store.xml` in app data
   ```bash
   adb shell run-as com.google.android.gms cat shared_prefs/asterism_consent_store.xml
   ```
2. **clearAll() called:** Check logs for "Consent store cleared" messages
3. **Storage issues:** Verify the app has sufficient storage space

#### Consent Expiring Too Quickly

**Symptom:** Consent expires before the expected TTL.

**Causes & Fixes:**
1. **Default TTL (7 days) used:** Pass explicit `ttlMillis` in `SetAsterismConsentRequest`
2. **System time changes:** Device time changes can affect expiry calculations
3. **Check expiry timestamp:**
   ```kotlin
   val state = apiService.getAsterismConsent(request)
   val remainingMs = state.expiryTimestamp - System.currentTimeMillis()
   Log.d(TAG, "Consent expires in ${remainingMs / 3600000} hours")
   ```

#### Callback Not Receiving Updates

**Symptom:** Registered callbacks don't receive `onConsentStateChanged` calls.

**Causes & Fixes:**
1. **Callback died:** Check for `RemoteException` in logs — dead callbacks are auto-removed
2. **Not registered:** Verify `registerCallbacks()` was called successfully
3. **Process death:** If the GMS unstable process died, re-register callbacks
4. **Check registration count:**
   ```bash
   adb logcat -s AsterismApiService:D | grep "Registered callbacks"
   ```

### Debug Logging

Enable verbose logging for Asterism:

```bash
adb shell setprop log.tag.AsterismService DEBUG
adb shell setprop log.tag.AsterismApiService DEBUG
adb shell setprop log.tag.AsterismConsentStore DEBUG

# Watch logs
adb logcat -s AsterismService:D AsterismApiService:D AsterismConsentStore:D
```

### Diagnostic Commands

```bash
# Check if AsterismService is running
adb shell dumpsys activity services | grep -A10 AsterismService

# Check consent data
adb shell run-as com.google.android.gms cat shared_prefs/asterism_consent_store.xml

# Clear consent data (reset state)
adb shell run-as com.google.android.gms rm shared_prefs/asterism_consent_store.xml

# Force-stop GMS to restart service
adb shell am force-stop com.google.android.gms
```

## Contributing

Contributions to `play-services-asterism` are welcome! Please follow the microG contribution guidelines:

1. **Fork** the repository
2. **Create** a feature branch
3. **Test** your changes (unit tests + integration tests)
4. **Submit** a pull request to `microg/GmsCore`

### Code Style

- Follow existing Kotlin code style (4-space indentation, camelCase)
- Add SPDX license headers to new files
- Include KDoc comments on public APIs
- Log at appropriate levels (D for debug, I for info, W for warnings, E for errors)

### Testing Requirements

- Unit tests for `AsterismConsentStore` state transitions
- Integration tests for AIDL round-trip operations
- Test consent expiry behavior
- Test concurrent access patterns

## License

```
SPDX-FileCopyrightText: 2025 microG Project Team
SPDX-License-Identifier: Apache-2.0
```

This module is licensed under the Apache License 2.0, consistent with the microG GmsCore project.

---

## See Also

- [play-services-constellation README](../play-services-constellation/README.md) — Companion RCS module for phone verification
- [Asterism Architecture](ARCHITECTURE.md) — Detailed architecture with diagrams
- [Asterism API Reference](core/API.md) — Kotlin API documentation
- [RCS Integration Guide](../docs/RCS_INTEGRATION.md) — End-to-end RCS setup guide
- [microG GmsCore Repository](https://github.com/microg/GmsCore)
- [Issue #2994: RCS Support](https://github.com/microg/GmsCore/issues/2994)
