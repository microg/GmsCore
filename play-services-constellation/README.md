# play-services-constellation

> **Constellation: RCS Phone Verification & Token Management for microG GmsCore**

The `play-services-constellation` module implements Google Play Services' Constellation API, which manages Rich Communication Services (RCS) phone number verification, IID (Instance ID) token management, and Phone Number Verification (PNV) capability resolution. This module is the core enabler for RCS features in Google Messages on microG-powered devices.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [API Reference](#api-reference)
  - [AIDL Interfaces](#aidl-interfaces)
  - [Parcelable Types](#parcelable-types)
- [Implementation Details](#implementation-details)
  - [ConstellationService](#constellationservice)
  - [ConstellationApiService](#constellationapiservice)
  - [ConstellationStateStore](#constellationstatestore)
  - [Supporting Components](#supporting-components)
- [Phone Verification Flow](#phone-verification-flow)
- [Setup & Integration](#setup--integration)
- [Phone Number Info Model](#phone-number-info-model)
- [PNV Capabilities](#pnv-capabilities)
- [Build Configuration](#build-configuration)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)

## Overview

Constellation is the phone identity and verification layer for Google's RCS infrastructure. It handles the critical task of verifying that a phone number belongs to the device, managing IID tokens for RCS registration, and querying phone number capabilities (PNV) to determine if a contact supports RCS.

### What Constellation Does

- **Phone Number Verification:** Verifies device phone number ownership through multiple mechanisms (SMS, silent verification, manual)
- **IID Token Management:** Generates, stores, caches, and refreshes Instance ID tokens used for RCS registration with Google's servers
- **PNV Capability Resolution:** Queries whether a given phone number supports RCS features
- **Verified Number Tracking:** Maintains a persistent set of verified phone numbers with token binding
- **Callback System:** Notifies registered clients of verification events, capability updates, and IID token refreshes
- **SIM/Telephony Integration:** Reads device phone number, SIM operator, roaming status, and country code
- **PNV Caching:** Caches PNV capability results with configurable TTL to reduce network calls

### Relationship to RCS Stack

```
┌──────────────────────────────────────────────────────┐
│                 Google Messages App                    │
├──────────────────────────────────────────────────────┤
│  Constellation API              │  Asterism API       │
│  • verifyPhoneNumber()          │  • getConsent()     │
│  • getIidToken()                │  • setConsent()     │
│  • getPnvCapabilities()         │  • callbacks        │
│  • getPhoneNumberInfo()         │                     │
├────────────────────────────────┼─────────────────────┤
│  play-services-constellation     │ play-services-asterism│
├────────────────────────────────┴─────────────────────┤
│               microG GmsCore Foundation                │
│   (AuthManager, GServices, RpcClient, PackageUtils)   │
└──────────────────────────────────────────────────────┘
```

## Architecture

The Constellation module follows a layered architecture with multiple supporting components:

```
┌──────────────────────────────────────────────────────────┐
│                   AndroidManifest.xml                     │
│  Registers ConstellationService                           │
│  Process: com.google.android.gms.unstable                 │
└────────────────────┬─────────────────────────────────────┘
                     │ starts
┌────────────────────▼─────────────────────────────────────┐
│               ConstellationService                        │
│  (android.app.Service)                                    │
│  • Lifecycle management                                   │
│  • Component instantiation                                │
│  • IBinder provider                                       │
└────────────────────┬─────────────────────────────────────┘
                     │ creates & wires
┌────────────────────▼─────────────────────────────────────┐
│            ConstellationApiService                        │
│  (IConstellationApiService.Stub)                          │
│  • AIDL IPC endpoint                                      │
│  • Request routing                                        │
│  • Callback orchestration                                 │
└──┬──────────┬───────────┬──────────┬─────────────────────┘
   │          │           │          │
   │ uses     │ uses      │ uses     │ uses
┌──▼────┐ ┌──▼──────┐ ┌──▼────┐ ┌──▼──────────────────┐
│ Auth  │ │State    │ │GServ  │ │RpcClient             │
│Manager│ │Store    │ │ices   │ │(Network calls)       │
│ • OAuth│ │• Prefs  │ │• Flags│ │• verifyPhoneNumber() │
│ • Tokens│ │• Numbers│ │• Conf │ │• getPnvCapabilities()│
└───────┘ │• IID    │ └───────┘ │• getIidToken()       │
          │• PNV    │           └──────────────────────┘
          │ cache   │
          └─────────┘
```

### Component Descriptions

| Component | Role |
|-----------|------|
| `ConstellationService` | Android Service hosting the API; wires all components together |
| `ConstellationApiService` | AIDL Stub implementation; routes requests to appropriate handlers |
| `ConstellationStateStore` | Persistent storage for verified numbers, tokens, IID data, PNV cache |
| `AuthManager` | OAuth token management for Google server authentication |
| `GServices` | GMS configuration flags and feature toggles |
| `RpcClient` | Network client for Google's RCS backend API calls |
| `VerificationMappings` | Maps between different verification states and formats |

## API Reference

### AIDL Interfaces

#### IConstellationApiService

The primary AIDL interface for Constellation operations. Located at:
`play-services-constellation/src/main/aidl/com/google/android/gms/constellation/internal/IConstellationApiService.aidl`

```java
interface IConstellationApiService {
    VerifyPhoneNumberResponse verifyPhoneNumber(in VerifyPhoneNumberRequest request) = 0;
    GetPnvCapabilitiesResponse getPnvCapabilities(in GetPnvCapabilitiesRequest request) = 1;
    GetIidTokenResponse getIidToken(in GetIidTokenRequest request) = 2;
    void registerCallbacks(in IConstellationCallbacks callbacks) = 3;
    void unregisterCallbacks(in IConstellationCallbacks callbacks) = 4;
    PhoneNumberInfo getPhoneNumberInfo(String phoneNumber) = 5;
}
```

**Methods:**

| Method | Transaction ID | Description |
|--------|---------------|-------------|
| `verifyPhoneNumber` | 0 | Initiates phone number verification. Supports multiple verification methods (SMS code, silent, auto). |
| `getPnvCapabilities` | 1 | Queries Phone Number Verification capabilities for a given phone number. Determines if a contact supports RCS. |
| `getIidToken` | 2 | Retrieves or refreshes the Instance ID token used for RCS registration. |
| `registerCallbacks` | 3 | Registers an `IConstellationCallbacks` instance for event notifications. |
| `unregisterCallbacks` | 4 | Unregisters a previously registered callback. |
| `getPhoneNumberInfo` | 5 | Returns comprehensive phone number information including verification status, RCS state, carrier info. |

#### IConstellationCallbacks

Callback interface for Constellation verification events. Located at:
`play-services-constellation/src/main/aidl/com/google/android/gms/constellation/internal/IConstellationCallbacks.aidl`

```java
oneway interface IConstellationCallbacks {
    void onVerificationComplete(int status, String phoneNumber, long timestamp) = 0;
    void onPnvCapabilitiesUpdated(int capabilities, long timestamp) = 1;
    void onConstellationError(int errorCode, String errorMessage) = 2;
    void onIidTokenRefreshed(String token, long expiryTimestamp) = 3;
}
```

**Methods:**

| Method | Description |
|--------|-------------|
| `onVerificationComplete` | Called when phone verification completes. Provides status, phone number, and timestamp. |
| `onPnvCapabilitiesUpdated` | Called when PNV capabilities are refreshed. Provides capabilities bitmap and timestamp. |
| `onConstellationError` | Called on any Constellation error. Provides error code and message. |
| `onIidTokenRefreshed` | Called when IID token is refreshed. Provides new token and expiry. |

### Parcelable Types

#### VerifyPhoneNumberRequest

Initiates phone number verification.

| Field | Type | Description |
|-------|------|-------------|
| `phoneNumber` | String | Phone number to verify (E.164 format recommended) |
| `verificationMethod` | int | Method: SMS_CODE (0), SILENT (1), AUTO (2), MANUAL (3) |
| `verificationCode` | String | SMS verification code (for SMS_CODE method) |
| `timeoutMillis` | long | Verification timeout in milliseconds |
| `callingPackage` | String | Calling package name |
| `requestId` | long | Client-assigned request identifier |

#### VerifyPhoneNumberResponse

Result of phone number verification.

| Field | Type | Description |
|-------|------|-------------|
| `status` | int | Verification status (see below) |
| `phoneNumber` | String | Verified phone number |
| `token` | String | Verification token (used for RCS registration) |
| `rcsConfigToken` | String | RCS configuration token |
| `expiryTimestamp` | long | Token expiry timestamp (ms) |
| `errorCode` | int | Error code (0 = success) |
| `errorMessage` | String | Error description |

**Verification Status Codes:**

| Constant | Value | Description |
|----------|-------|-------------|
| `STATUS_UNKNOWN` | 0 | Verification status unknown |
| `STATUS_VERIFIED` | 1 | Phone number successfully verified |
| `STATUS_PENDING` | 2 | Verification in progress |
| `STATUS_FAILED` | 3 | Verification failed |
| `STATUS_TIMED_OUT` | 4 | Verification timed out |
| `STATUS_NOT_REQUIRED` | 5 | Verification not required |
| `STATUS_RETRY` | 6 | Verification should be retried |

#### GetIidTokenRequest

Requests an Instance ID token.

| Field | Type | Description |
|-------|------|-------------|
| `audience` | String | Target audience for the token |
| `scope` | String | OAuth scope string |
| `forceRefresh` | boolean | If true, ignores cached token |
| `callingPackage` | String | Calling package name |

#### GetIidTokenResponse

Contains the IID token response.

| Field | Type | Description |
|-------|------|-------------|
| `token` | String | The IID token string (null if error) |
| `expiryTimestamp` | long | Token expiry timestamp (ms) |
| `resultCode` | int | Result code (0 = success) |
| `errorMessage` | String | Error description |

#### GetPnvCapabilitiesRequest

Queries PNV capabilities for phone numbers.

| Field | Type | Description |
|-------|------|-------------|
| `phoneNumbers` | String[] | Array of phone numbers to query |
| `capabilityFlags` | int | Bitmask of capabilities to check |
| `callingPackage` | String | Calling package name |

#### GetPnvCapabilitiesResponse

Contains PNV capability results.

| Field | Type | Description |
|-------|------|-------------|
| `results` | PnvResult[] | Array of PNV results, one per phone number |
| `cacheTtlSeconds` | long | How long results should be cached |
| `timestamp` | long | Response timestamp |

#### PhoneNumberInfo

Comprehensive phone number information.

| Field | Type | Description |
|-------|------|-------------|
| `phoneNumber` | String | The phone number (E.164 format) |
| `isVerified` | boolean | Whether the number is verified |
| `isRcsEnabled` | boolean | Whether RCS is enabled for this number |
| `verificationStatus` | int | Detailed verification status |
| `lastVerifiedTimestamp` | long | Last verification timestamp |
| `tokenExpiryTimestamp` | long | Token expiry timestamp |
| `simOperatorName` | String | SIM carrier operator name |
| `formattedPhoneNumber` | String | Locally formatted phone number |
| `simCountryCode` | int | SIM country calling code |
| `isRoaming` | boolean | Whether the device is roaming |
| `lineType` | int | Phone line type (mobile, voip, etc.) |

## Implementation Details

### ConstellationService

**Location:** `play-services-constellation/core/src/main/kotlin/org/microg/gms/constellation/core/ConstellationService.kt`

The `ConstellationService` is the Android Service entry point that hosts the entire Constellation API stack. It wires together all dependencies during `onCreate()`:

```kotlin
override fun onCreate() {
    super.onCreate()
    val authManager = AuthManager(this)
    val stateStore = ConstellationStateStore(this)
    val gServices = GServices(this)
    val rpcClient = RpcClient()
    val verificationMappings = VerificationMappings(this)
    apiService = ConstellationApiService(
        this, authManager, stateStore, gServices, rpcClient, verificationMappings
    )
}
```

**Wired Components:**

```
ConstellationService.onCreate()
├── AuthManager(this)           → OAuth token management
├── ConstellationStateStore(this) → Persistent state
├── GServices(this)              → Feature flags
├── RpcClient()                  → Network client
├── VerificationMappings(this)   → State mapping
└── ConstellationApiService(...)  → AIDL endpoint
```

**Lifecycle:**

```
onCreate() → Wire all components, create API service
onBind()   → Return ConstellationApiService as IBinder
onUnbind() → Log unbind, return to super
onDestroy() → Log destruction
```

### ConstellationApiService

The `ConstellationApiService` (created inline in `ConstellationService.kt`) implements `IConstellationApiService.Stub()` and orchestrates requests across all components:

**Dependencies:**

| Dependency | Purpose |
|-----------|---------|
| `Context` | Android context for system services |
| `AuthManager` | OAuth2 token for Google server authentication |
| `ConstellationStateStore` | Persistent storage for verified numbers and tokens |
| `GServices` | GMS configuration and feature flags |
| `RpcClient` | HTTP client for Google's RCS backend |
| `VerificationMappings` | Maps between verification states |

**Request Routing:**

```
verifyPhoneNumber(request)
├── validatePhoneNumber(request.phoneNumber)
├── checkVerificationMethod(request.verificationMethod)
│   ├── SMS_CODE   → sendVerificationSMS()
│   ├── SILENT     → performSilentVerification()
│   ├── AUTO       → determineBestMethod()
│   └── MANUAL     → return pending status
├── waitForVerification()
├── stateStore.addVerifiedNumber(...)
└── notifyCallbacks(VERIFICATION_COMPLETE)

getPnvCapabilities(request)
├── checkPnvCache(phoneNumbers)
├── rpcClient.queryPnvCapabilities(uncachedNumbers)
├── stateStore.storePnvCache(results)
└── return aggregated results

getIidToken(request)
├── if (!request.forceRefresh)
│   └── check stateStore.getIidToken()
├── if (cachedToken still valid)
│   └── return cachedToken
├── authManager.getOAuthToken()
├── rpcClient.fetchIidToken(oauthToken, audience, scope)
├── stateStore.storeIidToken(token, expiry)
└── notifyCallbacks(IID_TOKEN_REFRESHED)
```

### ConstellationStateStore

**Location:** `play-services-constellation/core/src/main/kotlin/org/microg/gms/constellation/core/ConstellationStateStore.kt`

The persistent storage layer for all Constellation data. Uses `SharedPreferences` for durability.

**Storage Keys:**

| Key | Type | Purpose |
|-----|------|---------|
| `verified_numbers` | Set<String> | Set of verified phone numbers |
| `last_verified_phone` | String | Most recently verified number |
| `last_verified_timestamp` | Long | Timestamp of last verification |
| `rcs_enabled` | Boolean | Whether RCS is enabled |
| `verification_token` | String | Current verification token |
| `rcs_config_token` | String | RCS configuration token |
| `token_expiry` | Long | Token expiry timestamp |
| `iid_token` | String | Instance ID token |
| `iid_token_expiry` | Long | IID token expiry |
| `device_fingerprint` | String | Stable device identifier |
| `pnv_cache_{number}` | String | PNV cache per phone number |
| `pnv_cache_{number}_expiry` | Long | PNV cache TTL per number |

**Methods:**

| Method | Thread Safety | Description |
|--------|---------------|-------------|
| `getVerifiedNumbers()` | `@Synchronized` | Returns immutable copy of verified numbers set |
| `addVerifiedNumber(...)` | `@Synchronized` | Adds number to verified set, stores tokens, sets RCS enabled |
| `removeVerifiedNumber(number)` | `@Synchronized` | Removes number from verified set |
| `isVerified(number)` | `@Synchronized` | Checks if number is verified and token not expired |
| `getLastVerifiedPhoneNumber()` | `@Synchronized` | Returns most recently verified number |
| `getVerificationToken()` | `@Synchronized` | Returns current verification token |
| `getRcsConfigToken()` | `@Synchronized` | Returns RCS configuration token |
| `isRcsEnabled()` | `@Synchronized` | Returns RCS enabled state |
| `getTokenExpiryTimestamp()` | `@Synchronized` | Returns token expiry timestamp |
| `getPhoneNumberInfo(number)` | `@Synchronized` | Builds comprehensive PhoneNumberInfo object |
| `storeIidToken(token, expiry)` | `@Synchronized` | Persists IID token and expiry |
| `getIidToken()` | `@Synchronized` | Returns valid IID token or null if expired |
| `isIidTokenExpired()` | `@Synchronized` | Checks IID token expiry |
| `storePnvCache(num, json, ttl)` | `@Synchronized` | Caches PNV results |
| `getPnvCache(number)` | `@Synchronized` | Returns cached PNV data or null |
| `clearAll()` | Write | Clears all stored data |

**Device Fingerprint:**

The `deviceFingerprint` is a lazily-generated stable identifier:
```kotlin
val deviceFingerprint: String by lazy {
    prefs.getString(KEY_DEVICE_FINGERPRINT, null) ?: run {
        val fp = generateFingerprint() // "constellation_" + UUID(12)
        prefs.edit().putString(KEY_DEVICE_FINGERPRINT, fp).apply()
        fp
    }
}
```

**Telephony Integration:**

The store integrates with `TelephonyManager` for:
- `simOperatorName`: Carrier display name (e.g., "T-Mobile", "Verizon")
- `simCountryIso`: Country code mapped to calling code (US → 1, FR → 33, etc.)
- `isNetworkRoaming`: Whether device is currently roaming
- All telephony reads handle `SecurityException` gracefully

### Supporting Components

#### AuthManager

Manages OAuth2 authentication tokens for Google server API calls:
- Obtains tokens using GmsCore's auth infrastructure
- Handles token refresh and expiry
- Provides tokens for RpcClient network calls

#### RpcClient

Network client for Google's RCS backend:
- `verifyPhoneNumber()`: Makes verification requests to Google servers
- `getPnvCapabilities()`: Queries phone number capability endpoints
- `fetchIidToken()`: Retrieves IID tokens from Google's identity service
- Handles HTTP errors, timeouts, and retries

#### GServices

Configuration and feature flag management:
- Reads GMS configuration for RCS feature flags
- Controls which verification methods are available
- Manages server endpoint URLs
- Governs TTL defaults and caching behavior

#### VerificationMappings

State mapping utility:
- Maps between internal verification states and API status codes
- Handles SMS code format validation
- Manages retry logic and backoff strategies

## Phone Verification Flow

### Standard Verification Flow

```
Google Messages                ConstellationService          Google Servers
     │                               │                            │
     │ 1. verifyPhoneNumber(E.164)    │                            │
     │───────────────────────────────►│                            │
     │                               │ 2. Send verification SMS    │
     │                               │───────────────────────────►│
     │                               │                            │
     │                               │◄─── SMS with code ─────────│
     │                               │                            │
     │ 3. User enters SMS code       │                            │
     │───────────────────────────────►│                            │
     │ verifyPhoneNumber(code)       │                            │
     │                               │ 4. Verify code with server │
     │                               │───────────────────────────►│
     │                               │                            │
     │                               │◄─ Verification token ───────│
     │                               │                            │
     │                               │ 5. Store in StateStore     │
     │                               │    - Add to verified set   │
     │                               │    - Store verification    │
     │                               │      token                 │
     │                               │    - Set RCS enabled       │
     │                               │                            │
     │◄─ VERIFIED + Token ──────────│                            │
     │                               │                            │
     │ 6. onVerificationComplete()   │                            │
     │    callback fired              │                            │
```

### Verification Methods

| Method | Value | How It Works | When to Use |
|--------|-------|-------------|-------------|
| `SMS_CODE` | 0 | Server sends SMS with code; user enters code in app | Standard verification |
| `SILENT` | 1 | Server performs background verification using carrier APIs | When carrier supports silent auth |
| `AUTO` | 2 | System tries SILENT first, falls back to SMS_CODE if needed | Recommended default |
| `MANUAL` | 3 | User manually confirms number ownership through app UI | Last resort fallback |

### Silent Verification (AUTO method)

```
1. verifyPhoneNumber(AUTO)
        │
2. Try SILENT verification
        │
   ┌────┴────────────┐
   │ Success         │ Failed
   ▼                 ▼
3a. VERIFIED      3b. Fall back to SMS_CODE
   Store token        │
   Set RCS enabled    ├─ Send SMS with code
                      ├─ User enters code
                      ├─ Verify code
                      └─ VERIFIED + Store token
```

### Token Refresh Flow

```
getIidToken(audience, scope)
        │
        ├─ Check cache: stateStore.getIidToken()
        │     │
        │  ┌──┴──────────────┐
        │  │ Valid (not       │ Expired or null
        │  │ expired)         │
        │  ▼                  ▼
        │ Return cached     authManager.getOAuthToken()
        │ token                  │
        │                   rpcClient.fetchIidToken()
        │                        │
        │                   stateStore.storeIidToken()
        │                        │
        │                   notifyCallbacks(
        │                     IID_TOKEN_REFRESHED)
        │                        │
        │                   Return new token
```

## Setup & Integration

### Adding to Your Build

```bash
git clone https://github.com/microg/GmsCore.git
cd GmsCore
git checkout feat/rcs-support-2994
./gradlew :play-services-constellation:assemble
```

### Binding from a Client App

```kotlin
val intent = Intent("com.google.android.gms.constellation.START").apply {
    setPackage("com.google.android.gms")
}

bindService(intent, object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
        val api = IConstellationApiService.Stub.asInterface(binder)
        
        // Verify phone number
        val verifyRequest = VerifyPhoneNumberRequest().apply {
            phoneNumber = "+1234567890"
            verificationMethod = VerifyPhoneNumberRequest.AUTO
            timeoutMillis = 30000L
        }
        val verifyResponse = api.verifyPhoneNumber(verifyRequest)
        
        when (verifyResponse.status) {
            VerifyPhoneNumberResponse.STATUS_VERIFIED -> {
                Log.i(TAG, "Phone verified! Token: ${verifyResponse.token}")
            }
            VerifyPhoneNumberResponse.STATUS_PENDING -> {
                Log.d(TAG, "Verification pending...")
            }
        }
    }
    
    override fun onServiceDisconnected(name: ComponentName?) {}
}, Context.BIND_AUTO_CREATE)
```

### Checking PNV Capabilities

```kotlin
// Check if contacts support RCS
val pnvRequest = GetPnvCapabilitiesRequest().apply {
    phoneNumbers = arrayOf("+1234567890", "+1987654321")
    capabilityFlags = PNV_CAPABILITY_RCS or PNV_CAPABILITY_E2EE
}
val pnvResponse = api.getPnvCapabilities(pnvRequest)

for (result in pnvResponse.results) {
    Log.d(TAG, "${result.phoneNumber}: RCS=${result.supportsRcs}, E2EE=${result.supportsE2ee}")
}
```

### Registering for Callbacks

```kotlin
val callbacks = object : IConstellationCallbacks.Stub() {
    override fun onVerificationComplete(status: Int, phoneNumber: String?, timestamp: Long) {
        if (status == VerifyPhoneNumberResponse.STATUS_VERIFIED) {
            Log.i(TAG, "RCS verification complete for $phoneNumber")
            enableRcsFeatures()
        }
    }
    
    override fun onPnvCapabilitiesUpdated(capabilities: Int, timestamp: Long) {
        Log.d(TAG, "PNV capabilities updated: $capabilities")
    }
    
    override fun onConstellationError(errorCode: Int, errorMessage: String?) {
        Log.e(TAG, "Constellation error $errorCode: $errorMessage")
    }
    
    override fun onIidTokenRefreshed(token: String?, expiryTimestamp: Long) {
        Log.i(TAG, "IID token refreshed, expires at $expiryTimestamp")
        updateRcsRegistration(token)
    }
}

api.registerCallbacks(callbacks)
```

## Phone Number Info Model

The `getPhoneNumberInfo()` method returns comprehensive phone data:

```
PhoneNumberInfo
├── phoneNumber: "+1234567890"        (E.164 format)
├── isVerified: true                   (in verified set + token valid)
├── isRcsEnabled: true                 (from SharedPreferences)
├── verificationStatus: STATUS_VERIFIED
├── lastVerifiedTimestamp: 1751300000000
├── tokenExpiryTimestamp: 1751900000000
├── simOperatorName: "T-Mobile"        (from TelephonyManager)
├── formattedPhoneNumber: "123-456-7890"
├── simCountryCode: 1                  (mapped from ISO)
├── isRoaming: false                   (from TelephonyManager)
├── lineType: LINE_TYPE_MOBILE
└── capabilities: RCS | E2EE | GROUP_CHAT
```

### Country Code Mapping

The store maps SIM country ISO codes to numeric calling codes:

| ISO Code | Country | Calling Code |
|----------|---------|--------------|
| `US` | United States | 1 |
| `CA` | Canada | 1 |
| `FR` | France | 33 |
| `DE` | Germany | 49 |
| `GB` | United Kingdom | 44 |
| `IN` | India | 91 |
| `CN` | China | 86 |
| `JP` | Japan | 81 |
| `BR` | Brazil | 55 |
| `RU` | Russia | 7 |

## PNV Capabilities

PNV (Phone Number Verification) capabilities indicate which RCS features a phone number supports:

| Flag | Value | Feature |
|------|-------|---------|
| `PNV_CAPABILITY_RCS` | 0x01 | Basic RCS messaging |
| `PNV_CAPABILITY_E2EE` | 0x02 | End-to-end encryption |
| `PNV_CAPABILITY_GROUP_CHAT` | 0x04 | Group chat support |
| `PNV_CAPABILITY_FILE_TRANSFER` | 0x08 | File/media sharing |
| `PNV_CAPABILITY_VIDEO_CALL` | 0x10 | RCS video calling |
| `PNV_CAPABILITY_TYPING` | 0x20 | Typing indicators |
| `PNV_CAPABILITY_READ_RECEIPT` | 0x40 | Read receipts |
| `PNV_CAPABILITY_HD_MEDIA` | 0x80 | High-definition media |

### Caching Strategy

PNV results are cached per-phone-number with configurable TTL:
- Cache TTL is determined by the server response (`cacheTtlSeconds`)
- Cache hit: returns cached data without network call
- Cache miss/expired: queries server, stores new results
- Cache namespaced by phone number to prevent cross-number interference

## Build Configuration

### Module build.gradle

The module's build configuration includes:
- **AIDL Processing:** Automatic stub generation from `.aidl` files
- **Kotlin:** JVM 1.8 target compatibility
- **Dependencies:** GmsCore common libraries, AndroidX

### Dependencies

```
play-services-constellation/core
├── com.google.android.gms:play-services-base (stubs)
├── org.microg.gms:gmscore-common (PackageUtils, MultiPods)
├── org.microg.gms:gmscore-auth (AuthManager integration)
├── Android framework:
│   ├── android.app.Service
│   ├── android.content.SharedPreferences
│   └── android.telephony.TelephonyManager
```

## Testing

### Unit Testing ConstellationStateStore

```kotlin
@Test
fun testVerificationLifecycle() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val store = ConstellationStateStore(context)
    
    // Initially no verified numbers
    assertTrue(store.getVerifiedNumbers().isEmpty())
    assertFalse(store.isVerified("+1234567890"))
    
    // Add verified number
    store.addVerifiedNumber(
        phoneNumber = "+1234567890",
        token = "verification-token-123",
        rcsConfigToken = "rcs-config-456",
        ttlMillis = 3600000L
    )
    
    // Should now be verified
    assertTrue(store.isVerified("+1234567890"))
    assertTrue(store.isRcsEnabled())
    assertEquals("verification-token-123", store.getVerificationToken())
    assertEquals("rcs-config-456", store.getRcsConfigToken())
    
    // Get phone info
    val info = store.getPhoneNumberInfo("+1234567890")
    assertEquals("+1234567890", info.phoneNumber)
    assertTrue(info.isVerified)
    assertTrue(info.isRcsEnabled)
    assertEquals(VerifyPhoneNumberResponse.STATUS_VERIFIED, info.verificationStatus)
}
```

### Testing IID Token Management

```kotlin
@Test
fun testIidTokenLifecycle() {
    val store = ConstellationStateStore(context)
    
    // Initially no token
    assertNull(store.getIidToken())
    assertFalse(store.isIidTokenExpired())
    
    // Store token with future expiry
    val futureExpiry = System.currentTimeMillis() + 3600000L
    store.storeIidToken("iid-token-abc", futureExpiry)
    
    // Token should be retrievable
    assertEquals("iid-token-abc", store.getIidToken())
    assertFalse(store.isIidTokenExpired())
}
```

## Troubleshooting

### Common Issues

#### Phone Verification Fails

**Symptom:** `verifyPhoneNumber()` returns `STATUS_FAILED`. SMS code never arrives.

**Causes & Fixes:**
1. **SMS permissions:** Ensure `RECEIVE_SMS` permission is granted
2. **Network connectivity:** Device needs internet to reach Google RCS servers
3. **Carrier support:** Some carriers block silent verification; try SMS_CODE method
4. **Check verification state:**
   ```bash
   adb shell run-as com.google.android.gms cat shared_prefs/constellation_state_store.xml
   ```

#### IID Token Always Null

**Symptom:** `getIidToken()` always returns null or expired token.

**Causes & Fixes:**
1. **No OAuth token:** AuthManager may not have credentials; verify Google account is signed in
2. **Network issues:** Token fetch requires internet connectivity
3. **Server rejection:** Some Google servers may reject non-certified devices
4. **Check token state:**
   ```bash
   adb logcat -s ConstellationStateStore:D | grep -i "iid"
   ```

#### PNV Capabilities Return Empty

**Symptom:** All PNV queries return no capabilities.

**Causes & Fixes:**
1. **Cached stale data:** Clear PNV cache: `adb shell run-as com.google.android.gms rm shared_prefs/constellation_state_store.xml`
2. **Server throttling:** Google may rate-limit PNV queries; implement backoff
3. **Phone format:** Ensure E.164 format with country code (`+1234567890`)

#### Service Not Starting

**Symptom:** `bindService()` fails or `onServiceConnected` never fires.

**Causes & Fixes:**
1. **GmsCore version:** Ensure latest GmsCore with Constellation support is installed
2. **Process conflicts:** Check that no other service is using the unstable GMS process
3. **Verify service declaration:**
   ```bash
   adb shell dumpsys package com.google.android.gms | grep -A5 "ConstellationService"
   ```

### Debug Logging

```bash
# Enable verbose logging
adb shell setprop log.tag.ConstellationService DEBUG
adb shell setprop log.tag.ConstellationApiService DEBUG
adb shell setprop log.tag.ConstellationStateStore DEBUG
adb shell setprop log.tag.RpcClient DEBUG

# Watch all Constellation logs
adb logcat -s ConstellationService:D ConstellationApiService:D ConstellationStateStore:D RpcClient:D
```

### Diagnostic Commands

```bash
# Check running services
adb shell dumpsys activity services | grep -A10 ConstellationService

# Read stored state
adb shell run-as com.google.android.gms cat shared_prefs/constellation_state_store.xml

# Clear all Constellation data (full reset)
adb shell run-as com.google.android.gms rm shared_prefs/constellation_state_store.xml

# Check SIM info
adb shell dumpsys telephony.registry | grep -E "mCallState|mServiceState|mNetworkType"

# Force-stop and restart GMS
adb shell am force-stop com.google.android.gms
adb shell am startservice com.google.android.gms/.constellation.core.ConstellationService
```

## Contributing

Contributions to `play-services-constellation` are welcome:

1. **Fork** the repository
2. **Create** a feature branch
3. **Test** thoroughly — especially verification flows
4. **Submit** a pull request

### Testing Requirements

- Unit tests for `ConstellationStateStore`
- Integration tests for verification flows
- Mock `RpcClient` for deterministic testing
- Test token expiry and refresh behavior
- Test PNV caching logic

## License

```
SPDX-FileCopyrightText: 2025 microG Project Team
SPDX-License-Identifier: Apache-2.0
```

Licensed under Apache License 2.0.

---

## See Also

- [play-services-asterism README](../play-services-asterism/README.md) — Companion RCS module for consent management
- [Constellation Architecture](ARCHITECTURE.md) — Detailed architecture diagrams
- [Constellation API Reference](core/API.md) — Kotlin API documentation
- [RCS Integration Guide](../docs/RCS_INTEGRATION.md) — End-to-end RCS setup
- [microG GmsCore Repository](https://github.com/microg/GmsCore)
- [Issue #2994: RCS Support](https://github.com/microg/GmsCore/issues/2994)
