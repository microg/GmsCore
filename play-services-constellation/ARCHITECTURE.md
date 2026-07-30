# Constellation Architecture

> **Internal Architecture of `play-services-constellation` — Phone Verification & Token Management for RCS**

This document describes the internal architecture of the Constellation module, including component interaction, phone verification flow, IID token management, PNV caching, and concurrency design.

## Table of Contents

- [System Overview](#system-overview)
- [Component Architecture](#component-architecture)
- [Phone Verification Flow](#phone-verification-flow)
- [IID Token Management](#iid-token-management)
- [PNV Capability Resolution](#pnv-capability-resolution)
- [Data Flow Diagrams](#data-flow-diagrams)
- [Persistence Layer](#persistence-layer)
- [Concurrency Model](#concurrency-model)
- [Network Layer (RpcClient)](#network-layer-rpcclient)
- [Error Handling](#error-handling)
- [Security Considerations](#security-considerations)
- [Performance Characteristics](#performance-characteristics)

## System Overview

Constellation implements a multi-tier phone verification and token management system:

```
┌──────────────────────────────────────────────────────────────────────┐
│                          CLIENT LAYER                                 │
│  ┌────────────────────┐        ┌────────────────────┐                │
│  │  Google Messages   │        │  RCS Clients        │                │
│  │  • verifyPhone()   │        │  • Third-party apps │                │
│  │  • getIidToken()   │        │  • Carrier apps     │                │
│  │  • getPnvCaps()    │        │                     │                │
│  │  • queryPhoneInfo()│        │                     │                │
│  └─────────┬──────────┘        └─────────┬──────────┘                │
│            │                             │                            │
│            │     AIDL IPC (Binder)       │                            │
└────────────┼─────────────────────────────┼────────────────────────────┘
             │                             │
┌────────────▼─────────────────────────────▼────────────────────────────┐
│                        SERVICE LAYER                                   │
│  ┌────────────────────────────────────────────────────────────────┐   │
│  │                   ConstellationService                          │   │
│  │  Process: com.google.android.gms.unstable                       │   │
│  │                                                                 │   │
│  │  onCreate() wires:                                              │   │
│  │  ┌──────────┐ ┌──────────────┐ ┌──────────┐ ┌──────────┐      │   │
│  │  │AuthMgr   │ │StateStore    │ │GServices │ │RpcClient │      │   │
│  │  │(OAuth)   │ │(Preferences) │ │(Flags)   │ │(HTTP)    │      │   │
│  │  └────┬─────┘ └──────┬───────┘ └────┬─────┘ └────┬─────┘      │   │
│  │       │              │              │            │             │   │
│  │       └──────────────┼──────────────┼────────────┘             │   │
│  │                      │              │                          │   │
│  │               ┌──────▼──────────────▼──────┐                   │   │
│  │               │ ConstellationApiService    │                   │   │
│  │               │ (IConstellationApiService. │                   │   │
│  │               │  Stub)                     │                   │   │
│  │               └────────────────────────────┘                   │   │
│  └────────────────────────────────────────────────────────────────┘   │
└───────────────────────────────────────────────────────────────────────┘
             │
┌────────────▼──────────────────────────────────────────────────────────┐
│                      NETWORK LAYER                                     │
│  ┌────────────────────────────────────────────────────────────────┐   │
│  │ Google RCS Backend                                              │   │
│  │  • /verifyPhoneNumber  — Phone verification endpoint            │   │
│  │  • /pnvCapabilities    — PNV capability query endpoint          │   │
│  │  • /iidToken           — Instance ID token endpoint             │   │
│  │  • Requires OAuth2 authentication                               │   │
│  └────────────────────────────────────────────────────────────────┘   │
└───────────────────────────────────────────────────────────────────────┘
```

## Component Architecture

### Component Interaction Diagram

```
┌──────────────────────────────────────────────────────────────┐
│                  ConstellationService                         │
│  ┌───────────────────────────────────────────────────────┐   │
│  │              ConstellationApiService                   │   │
│  │                                                       │   │
│  │  verifyPhoneNumber() ────┐                            │   │
│  │  getPnvCapabilities() ──┤                            │   │
│  │  getIidToken() ─────────┤                            │   │
│  │  getPhoneNumberInfo() ──┤                            │   │
│  │  registerCallbacks()    │                            │   │
│  └─────────────────────────┼────────────────────────────┘   │
│                            │                                │
│       ┌────────────────────┼────────────────────┐           │
│       │                    │                    │           │
│  ┌────▼─────┐    ┌────────▼────────┐   ┌───────▼──────┐    │
│  │  Auth    │    │   State         │   │   RpcClient  │    │
│  │  Manager │    │   Store         │   │              │    │
│  │          │    │                 │   │  POST verify │    │
│  │ OAuth2   │    │ SharedPrefs     │   │  POST pnv    │    │
│  │ tokens   │    │ verifiedNumbers │   │  POST iid    │    │
│  │          │    │ iidToken        │   │              │    │
│  │          │    │ pnvCache        │   │              │    │
│  └──────────┘    └────────┬────────┘   └──────┬───────┘    │
│                           │                    │            │
│                    ┌──────▼────────┐           │            │
│                    │   GServices   │           │            │
│                    │   (flags)     │           │            │
│                    └───────────────┘           │            │
│                           │                    │            │
│                    ┌──────▼────────┐           │            │
│                    │ Verification  │◄──────────┘            │
│                    │  Mappings     │ (state mapping)        │
│                    └───────────────┘                       │
│                                                           │
│  ┌─────────────────────────────────────────────────────┐  │
│  │          CopyOnWriteArrayList                        │  │
│  │          <IConstellationCallbacks>                   │  │
│  │  • onVerificationComplete()                         │  │
│  │  • onPnvCapabilitiesUpdated()                       │  │
│  │  • onConstellationError()                           │  │
│  │  • onIidTokenRefreshed()                            │  │
│  └─────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────┘
```

### Component Responsibilities Matrix

| Component | Primary Role | Dependencies | Thread-Safe |
|-----------|-------------|--------------|-------------|
| `ConstellationService` | Service lifecycle, dependency injection | All components | N/A (creates once) |
| `ConstellationApiService` | AIDL endpoint, request orchestration | All components | Via store sync |
| `ConstellationStateStore` | Persistent verification data | `SharedPreferences`, `TelephonyManager` | `@Synchronized` |
| `AuthManager` | OAuth2 token provisioning | GmsCore auth stack | Yes |
| `RpcClient` | HTTP client for Google backend | Network, AuthManager | Yes (stateless) |
| `GServices` | Feature flags, configuration | GMS config | Read-only |
| `VerificationMappings` | State enum mapping | None | Stateless utility |

## Phone Verification Flow

### Complete Verification Sequence

```
                     CLIENT            SERVICE           GOOGLE BACKEND
                       │                  │                    │
  1. verifyPhoneNumber │                  │                    │
  (E.164 + AUTO) ─────►│                  │                    │
                       │                  │                    │
  2.                   │  validatePhone   │                    │
                       │  Number()        │                    │
                       │       │          │                    │
                       │  ┌────▼──────┐   │                    │
                       │  │ Format    │   │                    │
                       │  │ E.164?    │   │                    │
                       │  └────┬──────┘   │                    │
                       │       │ NO       │                    │
                       │  ┌────▼──────┐   │                    │
                       │  │ Format    │   │                    │
                       │  │ attempt   │   │                    │
                       │  └──────────┘   │                    │
                       │                  │                    │
  3.                   │  Determine       │                    │
                       │  method: AUTO    │                    │
                       │       │          │                    │
                       │  ┌────▼──────┐   │                    │
                       │  │ Try       │   │                    │
                       │  │ SILENT    │   │                    │
                       │  └────┬──────┘   │                    │
                       │       │          │                    │
                       │       ├─ getOAuthToken()               │
                       │       │          │                    │
                       │       ▼          │                    │
                       │  POST /verify    │                    │
                       │  (silent) ────────────────────────────►
                       │                  │                    │
                       │                  │◄─── 200 OK ────────
                       │                  │     token + rcsConfig
                       │                  │                    │
                  ┌────┴──────┐           │                    │
                  │ SUCCESS   │           │                    │
                  └────┬──────┘           │                    │
                       │                  │                    │
                       │  ┌───────────────▼───────────────┐    │
                       │  │ stateStore.addVerifiedNumber()│    │
                       │  │ • Add to verifiedNumbers set  │    │
                       │  │ • Store verificationToken     │    │
                       │  │ • Store rcsConfigToken        │    │
                       │  │ • Set rcsEnabled = true       │    │
                       │  │ • Set tokenExpiry             │    │
                       │  └───────────────┬───────────────┘    │
                       │                  │                    │
                       │  ┌───────────────▼───────────────┐    │
                       │  │ notifyCallbacks()             │    │
                       │  │ onVerificationComplete(VERIFIED)   │
                       │  └──────────────────────────────┘    │
                       │                  │                    │
  4. ◄── VERIFIED ────────────────────────│                    │
     + token                              │                    │
                       │                  │                    │

  --- FALLBACK PATH (if SILENT fails) ---
                       │                  │                    │
                       │       │ (SILENT fails)                │
                       │  ┌────▼──────┐   │                    │
                       │  │ SMS_CODE  │   │                    │
                       │  └────┬──────┘   │                    │
                       │       │          │                    │
                       │       ▼          │                    │
                       │  POST /verify    │                    │
                       │  (sms) ───────────────────────────────►
                       │                  │                    │
                       │                  │◄─── SMS sent ──────
                       │                  │     status: PENDING
                       │                  │                    │
  5. ◄── PENDING ─────────────────────────│                    │
     (wait for SMS)   │                  │                    │
                       │                  │                    │
  6. User receives SMS │                  │                    │
  with 6-digit code    │                  │                    │
                       │                  │                    │
  7. verifyPhoneNumber │                  │                    │
  (code) ─────────────►│                  │                    │
                       │                  │                    │
                       │  POST /verify/   │                    │
                       │  confirm ─────────────────────────────►
                       │                  │                    │
                       │                  │◄─── VERIFIED ──────
                       │                  │                    │
                       │  ┌───────────────▼───────────────┐    │
                       │  │ stateStore.addVerifiedNumber()│    │
                       │  │ store tokens                  │    │
                       │  │ notifyCallbacks()             │    │
                       │  └──────────────────────────────┘    │
                       │                  │                    │
  8. ◄── VERIFIED ───────────────────────│                    │
```

### Verification Method Selection Logic

```
verifyPhoneNumber(request)
    │
    ├─ request.verificationMethod == SMS_CODE
    │  └─ Send SMS with verification code
    │     └─ Wait for user to provide code
    │        └─ Confirm code with server
    │
    ├─ request.verificationMethod == SILENT
    │  └─ Perform silent carrier verification
    │     ├─ Requires carrier support
    │     └─ No user interaction needed
    │
    ├─ request.verificationMethod == AUTO (recommended)
    │  ┌──────────────────────────────────┐
    │  │ 1. Try SILENT first              │
    │  │    ├─ Success → return VERIFIED  │
    │  │    └─ Failure → continue         │
    │  │                                  │
    │  │ 2. Fall back to SMS_CODE         │
    │  │    └─ Send SMS, wait for code    │
    │  └──────────────────────────────────┘
    │
    └─ request.verificationMethod == MANUAL
       └─ Return PENDING status
          └─ Client handles verification manually
```

## IID Token Management

### Token Lifecycle

```
┌─────────────────────────────────────────────────────────────┐
│                    IID TOKEN LIFECYCLE                       │
│                                                             │
│  ┌──────────┐                                               │
│  │ NO TOKEN │ (initial state)                               │
│  └────┬─────┘                                               │
│       │ getIidToken() called                                │
│       ▼                                                     │
│  ┌──────────────────────────────────────────────┐           │
│  │          TOKEN FETCH                          │           │
│  │                                               │           │
│  │  1. authManager.getOAuthToken()               │           │
│  │  2. rpcClient.fetchIidToken(                 │           │
│  │       oauthToken, audience, scope)           │           │
│  │  3. stateStore.storeIidToken(token, expiry)   │           │
│  │  4. notifyCallbacks(IID_TOKEN_REFRESHED)      │           │
│  └──────────────────────┬───────────────────────┘           │
│                         │                                   │
│  ┌──────────────────────▼───────────────────────┐           │
│  │            TOKEN CACHED                       │           │
│  │  • Stored in SharedPreferences                │           │
│  │  • TTL: configurable (default from server)    │           │
│  │  • Retrievable via stateStore.getIidToken()   │           │
│  └──────────────────────┬───────────────────────┘           │
│                         │                                   │
│              ┌──────────┴──────────┐                        │
│              │                     │                        │
│     Token still valid      Token expired or                 │
│     (getIidToken()         forceRefresh=true                │
│      returns cached)             │                          │
│              │                     │                        │
│              │              ┌──────▼──────┐                 │
│              │              │ RE-FETCH    │                 │
│              │              │ (same flow  │                 │
│              │              │  as above)  │                 │
│              │              └─────────────┘                 │
└─────────────────────────────────────────────────────────────┘
```

### Token Caching Strategy

```
getIidToken(request)
    │
    ├─ request.forceRefresh == false
    │  └─ Check cache
    │     ┌──────────────────────────┐
    │     │ stateStore.getIidToken() │
    │     │  ├─ Token exists?        │
    │     │  ├─ Token not expired?   │
    │     │  └─ → Return cached      │
    │     └──────────────────────────┘
    │
    ├─ Cache miss OR forceRefresh == true
    │  └─ Fetch fresh token
    │     ├─ authManager.getOAuthToken()
    │     ├─ rpcClient.fetchIidToken(...)
    │     └─ stateStore.storeIidToken(newToken, newExpiry)
    │
    └─ Return GetIidTokenResponse(token, expiry, resultCode)
```

### Expiry Check

```kotlin
// In ConstellationStateStore
fun getIidToken(): String? {
    val token = prefs.getString(KEY_IID_TOKEN, null)
    val expiry = prefs.getLong(KEY_IID_TOKEN_EXPIRY, 0L)
    return if (token != null && (expiry == 0L || System.currentTimeMillis() < expiry)) {
        token  // Still valid
    } else {
        null    // Expired or never set
    }
}
```

## PNV Capability Resolution

### PNV Query Flow

```
Client calls getPnvCapabilities(phoneNumbers, capabilityFlags)
    │
    ▼
┌────────────────────────────────────────────────────────────┐
│ ConstellationApiService.getPnvCapabilities()                │
│                                                             │
│  1. Split phone numbers into two groups:                    │
│     ┌──────────────────┐    ┌──────────────────┐           │
│     │ CACHED           │    │ UNCACHED          │           │
│     │ (in stateStore)  │    │ (need to fetch)   │           │
│     └──────────────────┘    └──────────────────┘           │
│                                                             │
│  2. If uncached group is non-empty:                         │
│     ├─ authManager.getOAuthToken()                          │
│     ├─ rpcClient.getPnvCapabilities(uncachedNumbers)        │
│     │     │                                                 │
│     │     ▼                                                 │
│     │  ┌──────────────────────────────────────┐            │
│     │  │  POST /pnvCapabilities               │            │
│     │  │  Body: {                             │            │
│     │  │    "numbers": ["+123...", "+456..."],│            │
│     │  │    "flags": 0xFF                     │            │
│     │  │  }                                   │            │
│     │  │                                      │            │
│     │  │  Response: {                         │            │
│     │  │    "results": [                      │            │
│     │  │      {"number": "+123...",           │            │
│     │  │       "capabilities": 0x1F},         │            │
│     │  │      ...                              │            │
│     │  │    ],                                 │            │
│     │  │    "cacheTtlSeconds": 3600           │            │
│     │  │  }                                   │            │
│     │  └──────────────────────────────────────┘            │
│     │                                                      │
│     └─ For each result:                                    │
│        stateStore.storePnvCache(number, json, ttl)         │
│                                                             │
│  3. Merge cached + fetched results                         │
│                                                             │
│  4. Return GetPnvCapabilitiesResponse                      │
│     notifyCallbacks(PNV_CAPABILITIES_UPDATED)              │
└────────────────────────────────────────────────────────────┘
```

### PNV Cache Architecture

```
SharedPreferences PNV Cache Layout:

pnv_cache_+1234567890 → {"capabilities": 31, "rcs": true, "e2ee": true, ...}
pnv_cache_+1234567890_expiry → 1751400000000

pnv_cache_+1987654321 → {"capabilities": 1, "rcs": true, "e2ee": false, ...}
pnv_cache_+1987654321_expiry → 1751403600000

Cache Lookup:
  getPnvCache("+1234567890")
  ├─ Read pnv_cache_+1234567890_expiry
  ├─ Compare with System.currentTimeMillis()
  ├─ If valid → return cached JSON
  └─ If expired → return null (triggers fresh fetch)
```

## Data Flow Diagrams

### verifyPhoneNumber Data Flow

```
┌──────────┐     ┌─────────────────┐     ┌──────────┐     ┌──────────────┐
│  Client  │────►│ Constellation   │────►│ AuthMgr  │────►│ Google RCS   │
│          │     │ ApiService      │     │          │     │ Backend      │
└──────────┘     └────────┬────────┘     └──────────┘     └──────┬───────┘
                          │                                      │
                          │ 1. verifyPhoneNumber(request)        │
                          │     │                                │
                          │     ├─ validatePhoneNumber()         │
                          │     ├─ selectMethod()                │
                          │     │                                │
                          │     ├─ [SILENT path]                 │
                          │     │  authMgr.getOAuthToken() ──────►│
                          │     │                                │
                          │     │  rpcClient.verify() ───────────►│
                          │     │                                │
                          │     │◄─────── token + config ────────│
                          │     │                                │
                          │     ├─ [SMS path]                    │
                          │     │  rpcClient.requestSms() ───────►│
                          │     │◄─────── PENDING ───────────────│
                          │     │                                │
                          │     │  [wait for code]               │
                          │     │  rpcClient.confirmSms() ───────►│
                          │     │◄─────── VERIFIED ──────────────│
                          │     │                                │
                          │     ├─ stateStore.addVerifiedNumber()│
                          │     │  ├─ verifiedNumbers += number  │
                          │     │  ├─ verificationToken = token  │
                          │     │  ├─ rcsConfigToken = config    │
                          │     │  ├─ rcsEnabled = true          │
                          │     │  └─ tokenExpiry = now + ttl    │
                          │     │                                │
                          │     ├─ notifyCallbacks(              │
                          │     │    onVerificationComplete)     │
                          │     │                                │
                          │◄──── VerifyPhoneNumberResponse       │
                          │                                      │
          ◄───────────────┘                                      │
```

### getPhoneNumberInfo Data Flow

```
┌──────────┐     ┌─────────────────┐     ┌───────────────────────┐
│  Client  │────►│ Constellation   │────►│ ConstellationStateStore│
│          │     │ ApiService      │     │                       │
└──────────┘     └────────┬────────┘     └───────────┬───────────┘
                          │                          │
                          │ getPhoneNumberInfo(num)  │
                          │     │                    │
                          │     ├─ stateStore        │
                          │     │  .getPhoneNumber   │
                          │     │  Info(num) ────────►
                          │     │                    │
                          │     │                    ├─ isVerified(num)
                          │     │                    ├─ isRcsEnabled()
                          │     │                    ├─ getTokenExpiry()
                          │     │                    ├─ getSimOperatorName()
                          │     │                    ├─ formatPhoneNumber()
                          │     │                    ├─ getSimCountryCode()
                          │     │                    ├─ isRoaming()
                          │     │                    │
                          │     │◄─ PhoneNumberInfo ─┤
                          │     │                    │
                          │◄──── PhoneNumberInfo     │
                          │                          │
          ◄───────────────┘                          │
```

## Persistence Layer

### Storage Schema

```
SharedPreferences: "constellation_state_store"
Mode: MODE_PRIVATE

┌─────────────────────────┬─────────────┬──────────────────────────────┐
│ Key                     │ Type        │ Purpose                      │
├─────────────────────────┼─────────────┼──────────────────────────────┤
│ verified_numbers        │ Set<String> │ Set of verified E.164 nums   │
│ last_verified_phone     │ String      │ Most recently verified       │
│ last_verified_timestamp │ Long        │ Timestamp of last verify     │
│ rcs_enabled             │ Boolean     │ Global RCS enabled flag      │
│ verification_token      │ String      │ Current verification token   │
│ rcs_config_token        │ String      │ RCS configuration token      │
│ token_expiry            │ Long        │ Token expiry timestamp       │
│ iid_token               │ String      │ Instance ID token            │
│ iid_token_expiry        │ Long        │ IID token expiry             │
│ device_fingerprint      │ String      │ Stable device ID             │
│ pnv_cache_{number}      │ String      │ PNV capabilities JSON        │
│ pnv_cache_{number}_expiry│ Long       │ PNV cache TTL                │
└─────────────────────────┴─────────────┴──────────────────────────────┘
```

### Persistence Guarantees

| Property | Value |
|----------|-------|
| **Durability** | `SharedPreferences.apply()` — async disk write |
| **Atomicity** | `@Synchronized` methods ensure atomic reads/writes |
| **Isolation** | MODE_PRIVATE — only GmsCore can access |
| **Recovery** | Lazy initialization for device fingerprint; expiry auto-checked |

### Disk Layout

```
/data/data/com.google.android.gms/
└── shared_prefs/
    └── constellation_state_store.xml
        <?xml version='1.0' encoding='utf-8' standalone='yes' ?>
        <map>
            <set name="verified_numbers">
                <string>+1234567890</string>
                <string>+1987654321</string>
            </set>
            <string name="last_verified_phone">+1234567890</string>
            <long name="last_verified_timestamp" value="1751300000000" />
            <boolean name="rcs_enabled" value="true" />
            <string name="verification_token">vt_a1b2c3d4...</string>
            <string name="iid_token">iid_e5f6g7h8...</string>
            <long name="iid_token_expiry" value="1751904800000" />
            <string name="device_fingerprint">constellation_f9e8d7c6b5a4</string>
            <string name="pnv_cache_+1234567890">
                {"capabilities":31,"rcs":true,"e2ee":true}
            </string>
            <long name="pnv_cache_+1234567890_expiry" value="1751400000000" />
        </map>
```

## Concurrency Model

### Thread Model

```
                    Binder Thread Pool
         ┌──────────────┼──────────────┐
         │              │              │
    Thread A        Thread B       Thread C
 verifyPhone()   getIidToken()  getPnvCaps()
         │              │              │
         ▼              ▼              ▼
┌─────────────────────────────────────────────────┐
│         ConstellationApiService                  │
│                                                  │
│  Thread-safe reads (via @Synchronized store):    │
│    - getIidToken() → stateStore.getIidToken()    │
│    - getPhoneNumberInfo() → stateStore.*()       │
│                                                  │
│  Thread-safe writes (via @Synchronized store):   │
│    - verifyPhoneNumber() → addVerifiedNumber()   │
│    - storeIidToken()                              │
│    - storePnvCache()                              │
│                                                  │
│  Concurrent network calls (via RpcClient):       │
│    - Multiple threads can make HTTP calls         │
│    - Results serialized through @Synchronized     │
│      store writes                                 │
└─────────────────────────────────────────────────┘
```

### Synchronization Points

| Operation | Synchronization | Notes |
|-----------|----------------|-------|
| `addVerifiedNumber()` | `@Synchronized` | Atomic write of number + token + expiry |
| `isVerified()` | `@Synchronized` | Consistent read of verified set + expiry |
| `getIidToken()` | `@Synchronized` | Atomic read of token + expiry check |
| `storeIidToken()` | `@Synchronized` | Atomic write of token + expiry |
| `storePnvCache()` | `@Synchronized` | Atomic cache write |
| `getPnvCache()` | `@Synchronized` | Atomic cache read with expiry check |
| `getPhoneNumberInfo()` | `@Synchronized` | Builds composite object from atomic reads |

### Concurrent Access Scenarios

```
Scenario 1: Simultaneous verifyPhoneNumber() calls
  Thread A: verifyPhoneNumber("+111")
  Thread B: verifyPhoneNumber("+222")
  
  Resolution:
  ├─ Both make independent network calls (RpcClient is stateless)
  ├─ Both call stateStore.addVerifiedNumber() — serialized by @Synchronized
  └─ Both numbers correctly stored in verified set

Scenario 2: Read during write
  Thread A: stateStore.addVerifiedNumber("+111", token, ttl) — writing
  Thread B: stateStore.isVerified("+111") — reading
  
  Resolution:
  ├─ @Synchronized serializes: Thread B waits for Thread A to complete
  └─ Thread B sees the new verified state

Scenario 3: PNV cache read/write race
  Thread A: storePnvCache("+111", data, ttl)
  Thread B: getPnvCache("+111")
  
  Resolution:
  ├─ @Synchronized serializes
  └─ Thread B either sees old cache or new cache, never corrupted
```

## Network Layer (RpcClient)

### API Endpoints

```
┌──────────────────────────────────────────────────────────────┐
│              Google RCS Backend API                          │
├─────────────────────┬────────────────────────────────────────┤
│ Endpoint            │ Purpose                                │
├─────────────────────┼────────────────────────────────────────┤
│ POST /verifyPhone   │ Request phone verification (silent     │
│                     │ or SMS)                                │
├─────────────────────┼────────────────────────────────────────┤
│ POST /verifyPhone/  │ Confirm SMS verification code          │
│       confirm       │                                        │
├─────────────────────┼────────────────────────────────────────┤
│ POST /pnvCapabilities│ Query RCS capabilities for phone       │
│                     │ numbers                                │
├─────────────────────┼────────────────────────────────────────┤
│ POST /iidToken      │ Request Instance ID token               │
├─────────────────────┼────────────────────────────────────────┤
│ Authentication:     │ OAuth2 Bearer token from AuthManager    │
│ Content-Type:       │ application/json                       │
└─────────────────────┴────────────────────────────────────────┘
```

### Request/Response Flow

```
RpcClient method
    │
    ├─ Check connectivity
    │  └─ No network → return error response
    │
    ├─ Obtain OAuth token from AuthManager
    │  └─ Token invalid/expired → AuthManager refreshes
    │
    ├─ Build HTTP request
    │  └─ Headers: Authorization: Bearer <token>
    │              Content-Type: application/json
    │
    ├─ Execute request (with timeout)
    │  ├─ Success (2xx) → parse JSON response
    │  ├─ Auth error (401/403) → trigger token refresh, retry once
    │  ├─ Rate limit (429) → apply backoff, retry
    │  └─ Server error (5xx) → return error
    │
    └─ Return parsed response or error
```

## Error Handling

### Error Categories

```
┌─────────────────────────────────────────────────────────────┐
│                    ERROR CATEGORIES                          │
├────────────────┬────────────────────────────────────────────┤
│ CLIENT ERROR   │ Invalid phone number format, missing       │
│                │ permissions, null request                   │
│                │ → Return error in response object          │
├────────────────┼────────────────────────────────────────────┤
│ NETWORK ERROR  │ No connectivity, timeout, DNS failure      │
│                │ → Return error with retry hint             │
├────────────────┼────────────────────────────────────────────┤
│ SERVER ERROR   │ Google backend returns error (4xx, 5xx)    │
│                │ → Map to appropriate response codes        │
├────────────────┼────────────────────────────────────────────┤
│ AUTH ERROR     │ OAuth token invalid or expired             │
│                │ → AuthManager refreshes, retry request     │
├────────────────┼────────────────────────────────────────────┤
│ STATE ERROR    │ Phone not in verified set, token expired   │
│                │ → Return accurate error codes              │
└────────────────┴────────────────────────────────────────────┘
```

### Verification Status Mapping

```
Network/Silent verification result → internal status → API response status
──────────────────────────────────────────────────────────────────────
200 OK with token    → VERIFIED     → STATUS_VERIFIED (1)
200 OK pending       → PENDING      → STATUS_PENDING (2)
4xx client error     → FAILED       → STATUS_FAILED (3)
Timeout              → TIMED_OUT    → STATUS_TIMED_OUT (4)
Not needed (config)  → SKIPPED      → STATUS_NOT_REQUIRED (5)
Transient error      → RETRY        → STATUS_RETRY (6)
Unexpected           → UNKNOWN      → STATUS_UNKNOWN (0)
```

## Security Considerations

### Authentication Chain

```
Google Messages
    │
    │ Binder IPC (AIDL)
    ▼
ConstellationApiService
    │
    │ Internal call
    ▼
AuthManager
    │
    │ OAuth2 token
    ▼
RpcClient
    │
    │ HTTPS + Bearer token
    ▼
Google RCS Backend
```

### Data Protection

| Data | Protection |
|------|-----------|
| Verification tokens | `SharedPreferences` MODE_PRIVATE |
| IID tokens | `SharedPreferences` MODE_PRIVATE |
| Phone numbers | `SharedPreferences` MODE_PRIVATE |
| OAuth tokens | Managed by AuthManager; in-memory or GmsCore secure storage |
| Network traffic | HTTPS (TLS 1.2+) to Google servers |
| Device fingerprint | Generated UUID, stored in private prefs |

### Telephony Permissions

The module reads telephony data (`TelephonyManager`) with `SecurityException` handling:

```kotlin
private fun getSimOperatorName(): String? {
    return try { tm?.simOperatorName } catch (e: SecurityException) { null }
}

private fun isRoaming(): Boolean {
    return try { tm?.isNetworkRoaming ?: false } catch (e: SecurityException) { false }
}
```

This ensures the module degrades gracefully when permissions are not granted.

## Performance Characteristics

### Operation Complexity

| Operation | Local Complexity | Network? | Typical Latency |
|-----------|-----------------|----------|-----------------|
| `getPhoneNumberInfo()` | O(1) | No | < 1ms |
| `getIidToken()` (cached) | O(1) | No | < 1ms |
| `getIidToken()` (fetch) | O(1) local | Yes | 200-500ms |
| `isVerified()` | O(N) in set | No | < 1ms |
| `addVerifiedNumber()` | O(1) | No | < 10ms |
| `verifyPhoneNumber()` (silent) | O(1) local | Yes | 1-3s |
| `verifyPhoneNumber()` (sms) | O(1) local | Yes | 5-30s (user wait) |
| `getPnvCapabilities()` (cached) | O(N) | No | < 1ms |
| `getPnvCapabilities()` (fetch) | O(N) local | Yes | 500ms-2s for batch |

### Memory Footprint

| Component | Approximate Size |
|-----------|-----------------|
| `ConstellationService` | ~1 KB |
| `ConstellationApiService` + deps | ~5 KB |
| `ConstellationStateStore` | ~3 KB |
| Per-verified-number | ~200 bytes (string + index) |
| Per-PNV-cache-entry | ~500-1000 bytes (JSON) |
| Per-callback proxy | ~500 bytes |

### Caching Strategy Summary

```
┌──────────┬───────────────┬──────────────────┬──────────────────┐
│ Cache    │ Storage       │ TTL              │ Invalidation     │
├──────────┼───────────────┼──────────────────┼──────────────────┤
│ IID Token│ SharedPrefs   │ Server-provided  │ Expiry check on  │
│          │               │ (default ~7d)    │ every read       │
├──────────┼───────────────┼──────────────────┼──────────────────┤
│ PNV Data │ SharedPrefs   │ Server-provided  │ Per-number       │
│          │ (per number)  │ cacheTtlSeconds  │ expiry check     │
├──────────┼───────────────┼──────────────────┼──────────────────┤
│ Verified │ SharedPrefs   │ Bounded by       │ Auto-checked     │
│ Numbers  │               │ token_expiry     │ in isVerified()  │
├──────────┼───────────────┼──────────────────┼──────────────────┤
│ Device   │ SharedPrefs   │ ∞ (permanent)    │ Never            │
│ Fingerprint│             │                  │                  │
└──────────┴───────────────┴──────────────────┴──────────────────┘
```

---

## See Also

- [Constellation README](README.md) — Module overview and integration guide
- [Constellation API Reference](core/API.md) — Kotlin API documentation
- [RCS Integration Guide](../docs/RCS_INTEGRATION.md) — End-to-end RCS setup
- [Asterism Architecture](../play-services-asterism/ARCHITECTURE.md) — Consent management architecture
