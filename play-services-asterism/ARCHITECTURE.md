# Asterism Architecture

> **Internal Architecture of `play-services-asterism` — Consent Management for RCS**

This document describes the internal architecture of the Asterism module, including component interaction, data flow, state management, concurrency model, and design decisions.

## Table of Contents

- [System Overview](#system-overview)
- [Component Architecture](#component-architecture)
- [Data Flow](#data-flow)
- [Consent Management](#consent-management)
- [Concurrency Model](#concurrency-model)
- [Persistence Layer](#persistence-layer)
- [Error Handling](#error-handling)
- [Security Considerations](#security-considerations)
- [Performance Characteristics](#performance-characteristics)
- [Testing Architecture](#testing-architecture)

## System Overview

Asterism implements a service-oriented consent management system with three tiers:

```
┌─────────────────────────────────────────────────────────────────┐
│                        CLIENT LAYER                              │
│  ┌──────────────────┐         ┌──────────────────┐              │
│  │ Google Messages  │         │  RCS Clients      │              │
│  │ (Primary Client) │         │  (Third-party)    │              │
│  └────────┬─────────┘         └────────┬─────────┘              │
│           │                            │                        │
│           │       AIDL IPC (Binder)     │                        │
└───────────┼────────────────────────────┼────────────────────────┘
            │                            │
┌───────────▼────────────────────────────▼────────────────────────┐
│                      SERVICE LAYER                               │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                 AsterismService                           │   │
│  │  Process: com.google.android.gms.unstable                 │   │
│  │  ┌──────────────────────────────────────────────────┐    │   │
│  │  │            AsterismApiService                     │    │   │
│  │  │  ┌─────────────┐  ┌────────────┐  ┌───────────┐  │    │   │
│  │  │  │ Consent     │  │ Callback   │  │ Package   │  │    │   │
│  │  │  │ Operations  │  │ Management │  │ Validation│  │    │   │
│  │  │  └──────┬──────┘  └─────┬──────┘  └─────┬─────┘  │    │   │
│  │  └─────────┼───────────────┼───────────────┼────────┘    │   │
│  └────────────┼───────────────┼───────────────┼─────────────┘   │
└───────────────┼───────────────┼───────────────┼─────────────────┘
                │               │               │
┌───────────────▼───────────────▼───────────────▼─────────────────┐
│                     STORAGE LAYER                                │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              AsterismConsentStore                         │   │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐ │   │
│  │  │ State    │  │ Token    │  │ Expiry   │  │ Device   │ │   │
│  │  │ Machine  │  │ Manager  │  │ Tracker  │  │ Identity │ │   │
│  │  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘ │   │
│  └───────┼──────────────┼────────────┼──────────────┼───────┘   │
│          │              │            │              │           │
│  ┌───────▼──────────────▼────────────▼──────────────▼───────┐   │
│  │              SharedPreferences (asterism_consent_store)   │   │
│  │  consent_state | consent_timestamp | expiry_timestamp     │   │
│  │  consent_token | refresh_count | device_id               │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

## Component Architecture

### Component Diagram

```
┌─────────────────────────────────────────────┐
│              AsterismService                 │
│  extends: android.app.Service                │
│  process: com.google.android.gms.unstable    │
│                                              │
│  ┌──────────────────────────────────────┐    │
│  │ Fields:                              │    │
│  │  - apiService: AsterismApiService    │    │
│  │                                      │    │
│  │ Methods:                             │    │
│  │  + onCreate()                        │    │
│  │  + onBind(Intent): IBinder           │    │
│  │  + onUnbind(Intent): Boolean         │    │
│  │  + onDestroy()                       │    │
│  └──────────────────────────────────────┘    │
└──────────────────┬──────────────────────────┘
                   │ instantiates
┌──────────────────▼──────────────────────────┐
│           AsterismApiService                 │
│  extends: IAsterismApiService.Stub           │
│                                              │
│  ┌──────────────────────────────────────┐    │
│  │ Fields:                              │    │
│  │  - context: Context                  │    │
│  │  - consentStore: AsterismConsentStore│    │
│  │  - callbacks: CopyOnWriteArrayList   │    │
│  │          <IAsterismCallbacks>        │    │
│  │                                      │    │
│  │ Methods:                             │    │
│  │  + getAsterismConsent(Request): Resp │    │
│  │  + setAsterismConsent(Request): Resp │    │
│  │  + registerCallbacks(ICallbacks)     │    │
│  │  + unregisterCallbacks(ICallbacks)   │    │
│  │  - notifyCallbacks(state: Int)       │    │
│  └──────────────────────────────────────┘    │
└──────────────────┬──────────────────────────┘
                   │ delegates
┌──────────────────▼──────────────────────────┐
│          AsterismConsentStore                │
│                                              │
│  ┌──────────────────────────────────────┐    │
│  │ Fields:                              │    │
│  │  - prefs: SharedPreferences          │    │
│  │  - deviceId: String (lazy)           │    │
│  │                                      │    │
│  │ Constants:                           │    │
│  │  - PREFS_NAME =                      │    │
│  │    "asterism_consent_store"          │    │
│  │  - DEFAULT_TTL_MS = 7 days           │    │
│  │                                      │    │
│  │ Methods:                             │    │
│  │  + getCurrentState(): Response       │    │
│  │  + grantConsent(token, ttl): Resp    │    │
│  │  + revokeConsent(): Response          │    │
│  │  + refreshConsent(ttl): Response     │    │
│  │  + setPending(): Response            │    │
│  │  + getDeviceId(): String             │    │
│  │  + getRefreshCount(): Int            │    │
│  │  + getLastRefreshTimestamp(): Long   │    │
│  │  + clearAll()                        │    │
│  │  - updateState(state, ts, exp, tok)  │    │
│  └──────────────────────────────────────┘    │
└─────────────────────────────────────────────┘
```

### Component Responsibilities

| Component | Responsibility | Singleton? | Lifecycle |
|-----------|---------------|------------|-----------|
| `AsterismService` | Service lifecycle, IBinder provider | Yes (Android Service) | Bound to service lifecycle |
| `AsterismApiService` | AIDL endpoint, request routing, callback management | One per service instance | Created in service `onCreate()` |
| `AsterismConsentStore` | Persistent state, token management, TTL tracking | One per service instance | Created with API service |
| `CopyOnWriteArrayList<IAsterismCallbacks>` | Thread-safe callback registry | Shared within API service | Grows/shrinks with registrations |

## Data Flow

### Read Flow: getAsterismConsent

```
Client (Google Messages)
    │
    │  AIDL call: getAsterismConsent(request)
    ▼
┌───────────────────────────────────────────────┐
│ AsterismApiService.getAsterismConsent()        │
│                                                │
│  1. Validate request (null check)              │
│     └─ null → return CONSENT_UNKNOWN response  │
│                                                │
│  2. Resolve calling package                    │
│     ├─ request.packageName                     │
│     └─ OR PackageUtils.getCallingPackage(ctx)  │
│                                                │
│  3. delegate to consentStore.getCurrentState() │
└───────────────────────┬───────────────────────┘
                        │
┌───────────────────────▼───────────────────────┐
│ AsterismConsentStore.getCurrentState()         │
│                                                │
│  4. Read from SharedPreferences:               │
│     - consent_state    (Int)                   │
│     - consent_timestamp (Long)                 │
│     - expiry_timestamp (Long)                  │
│     - consent_token    (String)                │
│                                                │
│  5. Check expiry:                              │
│     if (state == GRANTED                       │
│         && expiryTimestamp > 0                 │
│         && now > expiryTimestamp)              │
│       → auto-update to CONSENT_EXPIRED         │
│                                                │
│  6. Return GetAsterismConsentResponse:         │
│     - consentState                             │
│     - consentTimestamp                         │
│     - expiryTimestamp                          │
│     - consentToken                             │
└───────────────────────────────────────────────┘
```

### Write Flow: setAsterismConsent

```
Client (Google Messages)
    │
    │  AIDL call: setAsterismConsent(request)
    ▼
┌───────────────────────────────────────────────────────────────┐
│ AsterismApiService.setAsterismConsent()                        │
│                                                                │
│  1. Validate request (null check)                              │
│     └─ null → return RESULT_ERROR_INVALID_ARGUMENT             │
│                                                                │
│  2. Resolve calling package                                    │
│                                                                │
│  3. Route by action:                                           │
│                                                                │
│  ┌──────────────────────────────────────────────────────┐      │
│  │ ACTION_GRANT                                          │      │
│  │  ├─ Check device integrity (if requested,             │      │
│  │  │  logged but not enforced in microG)                │      │
│  │  ├─ consentStore.grantConsent(token, ttlMillis)       │      │
│  │  │  └─ Generate token if null (UUID.randomUUID())     │      │
│  │  │  └─ Set expiry = now + ttl (default 7 days)        │      │
│  │  │  └─ Write to SharedPreferences                      │      │
│  │  └─ Return SetAsterismConsentResponse(RESULT_OK, ...)  │      │
│  ├──────────────────────────────────────────────────────┤      │
│  │ ACTION_REVOKE                                         │      │
│  │  ├─ consentStore.revokeConsent()                      │      │
│  │  │  └─ Set state = CONSENT_DENIED                     │      │
│  │  │  └─ Clear consent token                            │      │
│  │  │  └─ Write to SharedPreferences                      │      │
│  │  └─ Return SetAsterismConsentResponse(RESULT_OK, ...)  │      │
│  ├──────────────────────────────────────────────────────┤      │
│  │ ACTION_REFRESH                                        │      │
│  │  ├─ consentStore.refreshConsent(ttlMillis)             │      │
│  │  │  └─ Only works if state == CONSENT_GRANTED         │      │
│  │  │  └─ Extend expiry, preserve token                  │      │
│  │  │  └─ Increment refresh_count                        │      │
│  │  │  └─ Write to SharedPreferences                      │      │
│  │  └─ Return SetAsterismConsentResponse(RESULT_OK, ...)  │      │
│  ├──────────────────────────────────────────────────────┤      │
│  │ ACTION_CHECK_STATUS                                   │      │
│  │  ├─ consentStore.getCurrentState()                    │      │
│  │  ├─ If granted && not expired → RESULT_OK             │      │
│  │  ├─ If expired → RESULT_ERROR_CONSENT_EXPIRED         │      │
│  │  └─ Return response with current state                │      │
│  ├──────────────────────────────────────────────────────┤      │
│  │ Unknown action                                        │      │
│  │  └─ Return RESULT_ERROR_INVALID_ARGUMENT               │      │
│  └──────────────────────────────────────────────────────┘      │
│                                                                │
│  4. Notify callbacks with new consent state                    │
│     ┌──────────────────────────────────────────────┐           │
│     │ for each callback in CopyOnWriteArrayList:   │           │
│     │  try: cb.onConsentStateChanged(state, now)   │           │
│     │  catch RemoteException:                      │           │
│     │    → Remove dead callback                    │           │
│     └──────────────────────────────────────────────┘           │
└───────────────────────────────────────────────────────────────┘
```

### Callback Registration Flow

```
Client calls registerCallbacks(callbacks)
    │
    ▼
┌───────────────────────────────────────────────┐
│ AsterismApiService.registerCallbacks()         │
│                                                │
│  1. Null check: callbacks != null              │
│                                                │
│  2. Dedup check:                               │
│     if (!this.callbacks.contains(callbacks))   │
│       → this.callbacks.add(callbacks)          │
│                                                │
│  3. Log: "Registered callbacks, total: N"      │
└───────────────────────────────────────────────┘

Client calls unregisterCallbacks(callbacks)
    │
    ▼
┌───────────────────────────────────────────────┐
│ AsterismApiService.unregisterCallbacks()       │
│                                                │
│  1. Null check: callbacks != null              │
│                                                │
│  2. Remove: this.callbacks.remove(callbacks)   │
│                                                │
│  3. Log: "Unregistered callbacks, total: N"    │
└───────────────────────────────────────────────┘
```

## Consent Management

### State Machine

```
                         ┌─────────────┐
                         │   UNKNOWN   │
                         │   (start)   │
                         └──────┬──────┘
                                │
              ┌─────────────────┼──────────────────┐
              │                 │                  │
     grantConsent()     setPending()        (no action)
              │                 │                  │
    ┌─────────▼────────┐ ┌──────▼──────┐  ┌───────▼──────┐
    │     GRANTED      │ │   PENDING   │  │   UNKNOWN    │
    │  (token present) │ │ (temporary) │  │  (persists)  │
    └──┬───────────┬───┘ └─────────────┘  └──────────────┘
       │           │
       │    ┌──────▼──────┐
       │    │   EXPIRED   │ (auto-transition on getCurrentState)
       │    │ (token kept)│
       │    └──────┬──────┘
       │           │
       │    refreshConsent() ───► GRANTED (same token, new expiry)
       │
  revokeConsent()
       │
  ┌────▼─────┐
  │  DENIED  │ (token cleared)
  └──────────┘
       │
  grantConsent() ───► GRANTED (new token)
```

### State Transition Rules

| From | To | Trigger | Side Effects |
|------|----|---------|-------------|
| `UNKNOWN` | `GRANTED` | `grantConsent()` | Generate UUID token, set expiry |
| `UNKNOWN` | `PENDING` | `setPending()` | Set short TTL |
| `UNKNOWN` | `DENIED` | `revokeConsent()` | Clear any token |
| `GRANTED` | `DENIED` | `revokeConsent()` | Clear token, clear expiry |
| `GRANTED` | `EXPIRED` | Time passes + `getCurrentState()` called | Auto-detected, state persisted |
| `GRANTED` | `GRANTED` | `refreshConsent()` | Same token, new expiry, increment count |
| `EXPIRED` | `GRANTED` | `refreshConsent()` or `grantConsent()` | New expiry or new token |
| `EXPIRED` | `DENIED` | `revokeConsent()` | Clear token |
| `DENIED` | `GRANTED` | `grantConsent()` | New token, new expiry |

### TTL and Expiry Management

```
grantConsent(token=null, ttlMillis=0)
    │
    ├─ ttlMillis == 0 → use DEFAULT_TTL_MS (7 days)
    ├─ token == null → generate UUID.randomUUID()
    │
    ├─ now = System.currentTimeMillis()
    ├─ expiryTimestamp = now + ttl
    │
    └─ persist: state=GRANTED, timestamp=now, expiry=expiryTimestamp, token=<UUID>

refreshConsent(ttlMillis)
    │
    ├─ Check: current state MUST be GRANTED
    │  └─ if not → log warning, return current state unchanged
    │
    ├─ ttlMillis == 0 → use DEFAULT_TTL_MS (7 days)
    │
    ├─ newExpiry = now + ttl
    ├─ refreshCount += 1
    │
    └─ persist: expiry=newExpiry, lastRefresh=now, refreshCount++
```

### Token Lifecycle

```
Token Created (UUID v4)
    │
    ├─ Stored with GRANTED state
    ├─ Bound to device via device_id
    │
    ├─ Persisted until:
    │  ├─ revokeConsent() → cleared
    │  ├─ clearAll() → cleared
    │  └─ Re-grant with external token → replaced
    │
    ├─ Can be refreshed:
    │  └─ refreshConsent() → token preserved, expiry extended
    │
    └─ After expiry:
       └─ Token still stored but state = EXPIRED
       └─ Still available in GetAsterismConsentResponse
       └─ Can be recovered with grantConsent(existingToken)
```

## Concurrency Model

### Thread Safety Strategy

```
                    Binder Thread Pool
                           │
          ┌────────────────┼────────────────┐
          │                │                │
     Thread A         Thread B         Thread C
          │                │                │
    getAsterism     setAsterism     registerCallbacks
    Consent()       Consent()       ()
          │                │                │
          ▼                ▼                ▼
┌─────────────────────────────────────────────────┐
│           AsterismApiService                     │
│                                                  │
│  Reads (thread-safe):                            │
│    - consentStore.getCurrentState() ← @Synch'd   │
│    - PackageUtils.getCallingPackage()            │
│                                                  │
│  Shared mutable state:                           │
│    - callbacks: CopyOnWriteArrayList             │
│      (safe reads during iteration)               │
│      (safe add/remove during traversal)          │
│                                                  │
│  Writes (serialized):                            │
│    - consentStore.grantConsent() ← @Synch'd      │
│    - consentStore.revokeConsent() ← @Synch'd     │
│    - consentStore.refreshConsent() ← @Synch'd    │
└─────────────────────────────────────────────────┘
```

### Synchronization Points

| Operation | Synchronization | Reasoning |
|-----------|----------------|-----------|
| `getCurrentState()` | `@Synchronized` on store | Must see consistent state, auto-detects expiry |
| `grantConsent()` | `@Synchronized` on store | Atomic write: state + token + expiry |
| `revokeConsent()` | `@Synchronized` on store | Atomic write: state + token clear |
| `refreshConsent()` | `@Synchronized` on store | Atomic write: expiry + counter |
| `addVerifiedNumber()` | `@Synchronized` | Must not interleave with reads/writes |
| Callback list iteration | `CopyOnWriteArrayList` | Allows concurrent read during write |
| Callback list modification | `CopyOnWriteArrayList` | Copy-on-write semantics |

### Potential Race Conditions (and Mitigations)

| Scenario | Risk | Mitigation |
|----------|------|------------|
| Two threads call `grantConsent()` simultaneously | Double-token generation | `@Synchronized` serializes writes; second call overwrites first |
| Thread reads state while another writes | Inconsistent read | `@Synchronized` on store; SharedPreferences is process-safe |
| Callback fires while being unregistered | Stale notification | `CopyOnWriteArrayList` snapshot iteration; `RemoteException` catch |
| Expiry check during `refreshConsent()` | State changes between check and write | `@Synchronized` wraps entire refresh operation |

## Persistence Layer

### Storage Schema

```
SharedPreferences: "asterism_consent_store"
Mode: MODE_PRIVATE (app-private, no other apps can read)

┌──────────────────────┬─────────┬──────────────────────────────┐
│ Key                  │ Type    │ Example Value                │
├──────────────────────┼─────────┼──────────────────────────────┤
│ consent_state        │ Int     │ 1 (CONSENT_GRANTED)          │
│ consent_timestamp    │ Long    │ 1751300000000                │
│ expiry_timestamp     │ Long    │ 1751904800000                │
│ consent_token        │ String  │ "a1b2c3d4-..."               │
│ last_refresh_timestamp│ Long   │ 1751500000000                │
│ refresh_count        │ Int     │ 3                            │
│ device_id            │ String  │ "f9e8d7c6-..."              │
└──────────────────────┴─────────┴──────────────────────────────┘
```

### Persistence Guarantees

| Property | Guarantee |
|----------|-----------|
| **Durability** | `SharedPreferences.apply()` — async write to disk, survives process death |
| **Consistency** | All writes are atomic via `@Synchronized` + single `apply()` call |
| **Isolation** | MODE_PRIVATE — only GmsCore can read/write |
| **Recovery** | Auto-detects expiry on read; lazy `deviceId` initialization |

### Disk Layout

```
/data/data/com.google.android.gms/
└── shared_prefs/
    └── asterism_consent_store.xml
        <?xml version='1.0' encoding='utf-8' standalone='yes' ?>
        <map>
            <int name="consent_state" value="1" />
            <long name="consent_timestamp" value="1751300000000" />
            <long name="expiry_timestamp" value="1751904800000" />
            <string name="consent_token">a1b2c3d4-e5f6-7890-abcd-ef1234567890</string>
            <int name="refresh_count" value="3" />
            <string name="device_id">f9e8d7c6-b5a4-3210-fedc-ba9876543210</string>
        </map>
```

## Error Handling

### Error Classification

```
┌─────────────────────────────────────────────────┐
│                ERROR CATEGORIES                  │
├──────────────┬──────────────────────────────────┤
│ VALIDATION   │ Null request, invalid action     │
│              │ → RESULT_ERROR_INVALID_ARGUMENT   │
├──────────────┼──────────────────────────────────┤
│ STATE        │ Consent expired, not found       │
│              │ → RESULT_ERROR_CONSENT_EXPIRED    │
│              │ → RESULT_ERROR_CONSENT_NOT_FOUND  │
├──────────────┼──────────────────────────────────┤
│ INTERNAL     │ Unexpected exceptions            │
│              │ → RESULT_ERROR_UNKNOWN            │
│              │ → RESULT_ERROR_INTERNAL           │
├──────────────┼──────────────────────────────────┤
│ CALLBACK     │ RemoteException from dead client │
│              │ → Auto-remove from callback list  │
└──────────────┴──────────────────────────────────┘
```

### Error Recovery Strategy

```
Request arrives
    │
    ▼
┌──────────────┐    ┌──────────────────────────┐
│ Null check   │───►│ Return structured error   │
│ (request)    │ NO │ response with error code  │
└──────┬───────┘    │ and descriptive message   │
       │ YES        └──────────────────────────┘
       ▼
┌──────────────┐    ┌──────────────────────────┐
│ Try operation│───►│ Log exception at ERROR   │
│ block        │ NO │ level                     │
└──────┬───────┘    │ Return RESULT_ERROR_*     │
       │ YES        │ with exception.message    │
       ▼            └──────────────────────────┘
  Return success
  response
```

### Callback Error Handling

```
notifyCallbacks(newState)
    │
    for each callback:
    │
    ├─ try: cb.onConsentStateChanged(state, timestamp)
    │
    └─ catch RemoteException:
       ├─ Log warning: "Failed to notify callback, removing"
       └─ callbacks.remove(cb)  // Auto-cleanup
```

## Security Considerations

### Package Validation

Asterism identifies calling packages through two mechanisms:

1. **Explicit:** `request.packageName` field — caller declares its identity
2. **Implicit:** `PackageUtils.getCallingPackage(context)` — Binder-level package resolution

While microG's implementation is permissive (any app can bind to the service), the architecture supports future enforcement:

```kotlin
// Future enhancement: whitelist-based access control
fun validateCaller(packageName: String?): Boolean {
    val allowedPackages = setOf(
        "com.google.android.apps.messaging",  // Google Messages
        "com.samsung.android.messaging",       // Samsung Messages
        // ... more allowed packages
    )
    return packageName in allowedPackages
}
```

### Token Security

- Tokens are UUID v4 — cryptographically random, not sequential
- Tokens are stored in app-private `SharedPreferences` (MODE_PRIVATE)
- Tokens are cleared on consent revocation
- Device identity is stable but not exposed to clients directly

### Device Integrity

The `requiresDeviceIntegrity` flag is accepted but **not enforced** in microG:
```kotlin
if (request.requiresDeviceIntegrity) {
    Log.i(TAG, "Device integrity check requested but not enforced in microG")
}
```

This is a deliberate design choice — microG does not implement Google's SafetyNet/Play Integrity attestation.

## Performance Characteristics

### Operation Complexity

| Operation | Complexity | Dominant Factor |
|-----------|-----------|-----------------|
| `getCurrentState()` | O(1) | `SharedPreferences.get*()` (memory-mapped) |
| `grantConsent()` | O(1) | `SharedPreferences.edit().apply()` (async) |
| `revokeConsent()` | O(1) | `SharedPreferences.edit().apply()` (async) |
| `refreshConsent()` | O(1) | `SharedPreferences.edit().apply()` (async) |
| `registerCallbacks()` | O(1) | `CopyOnWriteArrayList.add()` |
| `notifyCallbacks()` | O(N) | N = number of registered callbacks |

### Memory Footprint

| Component | Approximate Size |
|-----------|-----------------|
| `AsterismService` (singleton) | ~1 KB (service skeleton) |
| `AsterismApiService` | ~2 KB (stub + fields) |
| `AsterismConsentStore` | ~2 KB (prefs reference + lazy deviceId) |
| Per-callback overhead | ~500 bytes (AIDL proxy) |
| SharedPreferences cache | Memory-mapped; negligible |

### Startup Time

```
Service creation: < 5ms
  ├─ AsterismApiService(ctx): < 3ms
  │  └─ AsterismConsentStore(ctx): < 2ms (SharedPreferences handle)
  └─ deviceId lazy init: deferred to first use

First consent read: < 2ms (SharedPreferences from memory)
First consent write: < 10ms (async disk I/O)
```

## Testing Architecture

### Test Pyramid

```
         ┌──────────┐
         │   E2E    │  Full RCS flow with real Google Messages
         │  Tests   │  (manual/semi-automated)
         ├──────────┤
         │Integration│  AIDL round-trip, service binding
         │  Tests   │  (Android Instrumentation tests)
         ├──────────┤
         │  Unit    │  ConsentStore state machine,
         │  Tests   │  state transitions, expiry detection
         └──────────┘
```

### Key Test Scenarios

| Scenario | What to Test |
|----------|-------------|
| **State transitions** | UNKNOWN → GRANTED → EXPIRED → DENIED → GRANTED |
| **Token lifecycle** | Generation, persistence, clearing on revoke |
| **Expiry detection** | Auto-detection on `getCurrentState()` |
| **TTL defaults** | Default 7-day TTL when 0 passed |
| **Concurrent access** | Multiple threads calling `grantConsent` simultaneously |
| **Callback lifecycle** | Register → notify → unregister → RemoteException cleanup |
| **Null safety** | Null requests return appropriate error codes |
| **Invalid actions** | Unknown action values → INVALID_ARGUMENT |
| **Refresh on non-GRANTED** | Should return current state unchanged |
| **Device ID stability** | Same ID across process restarts |

---

## See Also

- [Asterism README](README.md) — Module overview and integration guide
- [Asterism API Reference](core/API.md) — Kotlin API documentation
- [RCS Integration Guide](../docs/RCS_INTEGRATION.md) — End-to-end RCS setup
