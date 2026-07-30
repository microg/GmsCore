# RCS Integration Guide for microG GmsCore

## What is RCS?

Rich Communication Services (RCS) is the GSMA-backed next-generation messaging protocol designed to replace SMS/MMS. It provides:

- **Rich messaging**: High-resolution photo/video sharing, group chats
- **Typing indicators**: Real-time typing status
- **Read receipts**: Message delivery and read status
- **WiFi/cellular calling**: Voice over IP integrated with messaging
- **End-to-end encryption**: Signal protocol for 1:1 and group chats
- **Business messaging**: Verified business profiles, chatbots, rich cards

Google has implemented RCS through Google Messages using their proprietary Jibe platform. microG's implementation aims to provide an open-source, privacy-respecting alternative.

## Architecture

```
┌─────────────────────────────────────────────┐
│                  User App                     │
│         (Google Messages / Compatible)        │
└──────────────┬──────────────────────────────┘
               │ AIDL (IAsterismService, IConstellationService)
┌──────────────▼──────────────────────────────┐
│              microG GmsCore                   │
│  ┌─────────────┐    ┌────────────────────┐   │
│  │  Asterism   │◄──►│   Constellation    │   │
│  │  (Consent)  │    │  (Verification)    │   │
│  └─────────────┘    └────────┬───────────┘   │
│                              │ RPC (Proto)    │
│                     ┌────────▼───────────┐   │
│                     │    RpcClient       │   │
│                     │ (gRPC/Protobuf)    │   │
│                     └────────┬───────────┘   │
└──────────────────────────────┼───────────────┘
                               │ HTTPS
┌──────────────────────────────▼───────────────┐
│          Google Constellation Backend          │
│          (constellation.googleapis.com)        │
└──────────────────┬───────────────────────────┘
                   │
┌──────────────────▼───────────────────────────┐
│              Carrier Network                   │
│       (SMS/TS43/RCS Provisioning)              │
└──────────────────────────────────────────────┘
```

## Components

### 1. Asterism — Consent Management

**Package**: `com.google.android.gms.asterism`

Handles user consent for RCS features. In the Google ecosystem, RCS requires explicit user consent for phone number verification and carrier provisioning. Asterism:

- Manages consent lifecycle (grant, revoke, expire)
- Stores consent tokens locally
- Communicates consent status to Constellation
- Broadcasts consent changes to listening apps

**Key Files**:
- `AsterismService.kt` — Service implementation
- `AsterismConsentStore.kt` — Persistent consent storage
- `GetAsterismConsent` — Retrieves current consent
- `SetAsterismConsent` — Updates consent state

**Consent States**:
```
UNKNOWN → PENDING → GRANTED  → EXPIRED
                  → DENIED   → (can re-request)
```

### 2. Constellation — Phone Verification & RPC

**Package**: `com.google.android.gms.constellation`

Handles phone number verification and secure communication with Google's Constellation backend. This is the most complex component of the RCS stack.

**Key Files**:
- `ConstellationService.kt` — Primary service entry point
- `RpcClient.kt` — Secure RPC communication
- `AuthManager.kt` — Authentication and token management
- `VerificationMappings.kt` — Verification method routing

**Verification Methods** (in priority order):
1. **TS43 (SIM-based)** — Uses EAP-AKA over SIM, most secure, requires carrier support
2. **CarrierID** — Uses carrier provisioning APIs, fast
3. **Registered SMS** — Pre-registered SMS patterns
4. **MO-SMS / MT-SMS** — Mobile-originated/terminated SMS fallback

### 3. Constellation Protocol Buffer

The RPC communication uses Protocol Buffers over HTTPS. The `constellation.proto` file defines:

- `SyncRequest` — Contains all request types (verification, capabilities, etc.)
- `SyncResponse` — Server response envelope
- `ClientInfo` — Device fingerprint and Android version
- `GaiaInfo` — Google account authentication
- `TelephonyInfo` — SIM, carrier, MCC/MNC data
- Phone verification request/response messages
- Capability check messages

### 4. RpcClient

The `RpcClient` manages the HTTPS connection to Google's Constellation servers:

```kotlin
// Simplified flow
1. Obtain IID token (Instance ID for device identification)
2. Build SyncRequest with ClientInfo, GaiaInfo, TelephonyInfo
3. Send via HTTPS POST to constellation.googleapis.com
4. Parse SyncResponse
5. Handle errors and retries with exponential backoff
```

**Endpoints**:
- Production: `https://constellation.googleapis.com/constellation/v1/sync`
- Staging: `https://constellation.sandbox.googleapis.com/constellation/v1/sync`

## Setup Guide

### Prerequisites

1. **microG installed** with signature spoofing
2. **Google Services Framework (GSF)** ID registered
3. **Working internet connection**
4. **Valid SIM card** with carrier that supports RCS

### Build Instructions

```bash
# Clone the repository
git clone https://github.com/microg/GmsCore.git
cd GmsCore

# Build Asterism module
./gradlew :play-services-asterism:assemble

# Build Constellation module
./gradlew :play-services-constellation:assemble

# Build full GmsCore
./gradlew :play-services-core:assemble
```

### Testing

```bash
# Run Asterism unit tests
./gradlew :play-services-asterism:test

# Run Constellation unit tests
./gradlew :play-services-constellation:test

# Deploy to device
adb install -r play-services-core/build/outputs/apk/debug/play-services-core-debug.apk
```

### Configuration

RCS features are controlled via GServices phenotypes:

```properties
# Enable RCS features
constellation_enable = true

# Server endpoint
constellation_server_endpoint = https://constellation.googleapis.com

# Verification timeout (ms)
constellation_verification_timeout_ms = 30000

# Enable TS43 verification on supported carriers
constellation_enable_ts43 = true

# Maximum RPC retries
constellation_max_retries = 3
```

## Protocol Flow

### Phone Number Verification Flow

```
┌──────┐     ┌───────────┐     ┌─────────────┐     ┌──────────┐
│ User │     │ GmsCore   │     │Constellation│     │ Carrier  │
│ App  │     │           │     │  Backend    │     │ Network  │
└──┬───┘     └─────┬─────┘     └──────┬──────┘     └────┬─────┘
   │               │                  │                  │
   │ verifyPhone() │                  │                  │
   │──────────────►│                  │                  │
   │               │ GetIidToken()    │                  │
   │               │─────────────────►│                  │
   │               │    token         │                  │
   │               │◄─────────────────│                  │
   │               │                  │                  │
   │               │ SyncRequest      │                  │
   │               │ (PhoneVerifReq)  │                  │
   │               │─────────────────►│                  │
   │               │                  │ SendChallenge()  │
   │               │                  │─────────────────►│
   │               │                  │   challenge      │
   │               │                  │◄─────────────────│
   │               │ SyncResponse     │                  │
   │               │ (challenge)      │                  │
   │               │◄─────────────────│                  │
   │               │                  │                  │
   │               │— Verify via SMS/TS43/CarrierID ─────►
   │               │                  │                  │
   │               │ SyncRequest      │                  │
   │               │ (verif_token)    │                  │
   │               │─────────────────►│                  │
   │               │                  │ ConfirmToken()   │
   │               │                  │─────────────────►│
   │               │                  │   MSISDN token   │
   │               │                  │◄─────────────────│
   │               │ SyncResponse     │                  │
   │               │ (msisdn_token)   │                  │
   │               │◄─────────────────│                  │
   │  ✓ verified   │                  │                  │
   │◄──────────────│                  │                  │
```

## Security Considerations

### Authentication
- All RPC calls are authenticated via IID tokens
- Tokens are refreshed every 60 minutes
- Connection uses TLS 1.3 with certificate pinning

### Privacy
- Consent is required before any verification
- Phone numbers are transmitted only over encrypted channels
- Consent tokens can be revoked at any time
- No message content passes through the Constellation backend (only signaling)

### Encryption
- RCS messages use Signal Protocol for E2E encryption
- Key exchange happens at the RCS client level, not Constellation
- Constellation only handles identity verification and provisioning

## Troubleshooting

### Common Issues

| Symptom | Likely Cause | Solution |
|---------|-------------|----------|
| `STATUS_UNKNOWN` consent | No prior consent recorded | Call `setAsterismConsent()` |
| Verification timeout | Carrier doesn't support selected method | Try fallback method (SMS) |
| `IID_TOKEN_INVALID` | Token expired or device ID changed | Re-register GSF ID |
| `CARRIER_NOT_SUPPORTED` | Carrier doesn't support RCS | Check carrier compatibility list |
| `RPC_CONNECTION_FAILED` | Network issue or backend unavailable | Check connectivity, retry with backoff |
| `TS43_NOT_SUPPORTED` | SIM doesn't support EAP-AKA | Fall back to SMS verification |

### Debug Logging

Enable verbose Constellation logging:

```properties
# adb shell setprop log.tag.Constellation VERBOSE
# adb shell setprop log.tag.Asterism VERBOSE
# adb shell setprop log.tag.RpcClient DEBUG
```

Key log tags:
- `ConstellationService` — Service lifecycle
- `RpcClient` — RPC communication
- `AsterismConsentStore` — Consent persistence
- `Ts43Verifier` — TS43 authentication
- `MoSmsVerifier` — SMS verification
- `ConstellationApiService` — High-level API calls

## Comparison with Google's Implementation

| Feature | Google Jibe | microG GmsCore |
|---------|-------------|----------------|
| Phone verification | Full support | In development |
| TS43 (EAP-AKA) | Supported | Partial |
| SMS fallback | Supported | In development |
| E2E encryption | Signal Protocol | Signal Protocol (client-side) |
| Business messaging | Supported | Not yet implemented |
| Carrier provisioning | Full | In development |
| Open source | No | Yes |
| Privacy | Google sees metadata | Self-hosted |

## Future Work

- [ ] Complete TS43 EAP-AKA implementation
- [ ] Add carrier provisioning API support
- [ ] Implement business messaging profile verification
- [ ] Add multi-SIM support
- [ ] Integrate with open-source RCS client
- [ ] WebRTC integration for VoIP calling
- [ ] Cross-platform key exchange for E2E encryption

## References

- [GSMA RCS Specification](https://www.gsma.com/futurenetworks/rcs/)
- [Google Jibe Platform](https://jibe.google.com/)
- [Constellation API Reference](play-services-constellation/API.md)
- [Asterism API Reference](play-services-asterism/API.md)
- [Constellation Architecture](play-services-constellation/ARCHITECTURE.md)
- [Asterism Architecture](play-services-asterism/ARCHITECTURE.md)
- [Signal Protocol Specification](https://signal.org/docs/)
