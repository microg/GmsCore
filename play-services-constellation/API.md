# Constellation API Reference

## Overview

Constellation is Google Play Services' backend communication module for RCS (Rich Communication Services). It handles phone number verification, carrier identity resolution, and secure RPC to Google's Constellation backend servers.

## Package

```
com.google.android.gms.constellation
```

## AIDL Interfaces

### IConstellationService

Located at: `com/google/android/gms/constellation/internal/IConstellationService.aidl`

```java
interface IConstellationService {
    void getPhoneNumberVerificationStatus(GetPhoneNumberVerifStatusRequest request, IConstellationCallback callback);
    void verifyPhoneNumber(VerifyPhoneNumberRequest request, IConstellationCallback callback);
    void getPnvCapabilities(GetPnvCapabilitiesRequest request, IConstellationCallback callback);
    void getVerifiedPhoneNumbers(Bundle params, IConstellationCallback callback);
}
```

### IConstellationCallback

```java
interface IConstellationCallback {
    void onPhoneNumberStatus(Bundle status);
    void onVerificationComplete(Bundle result);
    void onCapabilities(Bundle capabilities);
    void onError(int errorCode, String message);
}
```

## Java API

### GetPhoneNumberVerificationStatusRequest

```java
package com.google.android.gms.constellation;

public class GetPhoneNumberVerificationStatusRequest {
    private final List<String> phoneNumbers;  // E.164 format
    private final int verificationMethod;     // 0=SMS, 1=TS43, 2=Carrier ID, 3=Auto
    private final Bundle carrierConfig;

    public static class Builder {
        public Builder addPhoneNumber(String e164);
        public Builder setVerificationMethod(int method);
        public Builder setCarrierConfig(Bundle config);
        public GetPhoneNumberVerificationStatusRequest build();
    }

    public List<String> getPhoneNumbers();
    public int getVerificationMethod();
    public Bundle getCarrierConfig();
}
```

### GetPhoneNumberVerificationStatusResponse

```java
public class GetPhoneNumberVerificationStatusResponse {
    private final Map<String, PhoneNumberStatus> statuses;

    public Map<String, PhoneNumberStatus> getStatuses();

    public static class PhoneNumberStatus {
        public static final int UNVERIFIED = 0;
        public static final int VERIFIED = 1;
        public static final int PENDING = 2;
        public static final int FAILED = 3;

        private final int status;
        private final String carrierName;
        private final int mccMnc;
        private final long verificationTimestamp;
        private final String verificationMethod;

        public int getStatus();
        public String getCarrierName();
        public int getMccMnc();
        public long getVerificationTimestamp();
        public String getVerificationMethod();
    }
}
```

### VerifyPhoneNumberRequest

```java
public class VerifyPhoneNumberRequest {
    private final String phoneNumber;        // E.164 format
    private final String verificationToken;  // From carrier challenge
    private final int verificationMethod;    // SMS, TS43, CarrierId, etc.
    private final int timeoutSeconds;

    public static class Builder {
        public Builder setPhoneNumber(String e164);
        public Builder setVerificationToken(String token);
        public Builder setVerificationMethod(int method);
        public Builder setTimeoutSeconds(int seconds);
        public VerifyPhoneNumberRequest build();
    }

    public String getPhoneNumber();
    public String getVerificationToken();
    public int getVerificationMethod();
    public int getTimeoutSeconds();
}
```

### VerifyPhoneNumberResponse

```java
public class VerifyPhoneNumberResponse {
    private final boolean success;
    private final String verifiedPhoneNumber;
    private final String carrierName;
    private final String msisdnToken;
    private final long expiryTimestamp;

    public boolean isSuccess();
    public String getVerifiedPhoneNumber();
    public String getCarrierName();
    public String getMsisdnToken();
    public long getExpiryTimestamp();
}
```

### GetPnvCapabilitiesRequest / Response

```java
public class GetPnvCapabilitiesRequest {
    private final String carrierId;
    private final String mccMnc;

    // Builder pattern...
}

public class GetPnvCapabilitiesResponse {
    private final boolean smsVerificationSupported;
    private final boolean ts43Supported;
    private final boolean carrierIdVerificationSupported;
    private final List<String> supportedMethods;
    private final String carrierEndpoint;
}
```

## Internal Service Implementation

### ConstellationService

```kotlin
package org.microg.gms.constellation.core

class ConstellationService : IConstellationService.Stub() {
    
    override fun getPhoneNumberVerificationStatus(
        request: GetPhoneNumberVerifStatusRequest, 
        callback: IConstellationCallback
    )
    
    override fun verifyPhoneNumber(
        request: VerifyPhoneNumberRequest, 
        callback: IConstellationCallback
    )
    
    override fun getPnvCapabilities(
        request: GetPnvCapabilitiesRequest, 
        callback: IConstellationCallback
    )
    
    override fun getVerifiedPhoneNumbers(
        params: Bundle, 
        callback: IConstellationCallback
    )
}
```

### RpcClient

Handles secure communication with Google's Constellation backend servers.

```kotlin
class RpcClient(private val context: Context) {
    
    // Establish authenticated connection
    suspend fun connect(): ConnectionResult
    
    // Send protobuf request, receive response
    suspend fun sendRequest(request: SyncRequest): SyncResponse
    
    // Get IID token for authentication
    suspend fun getIidToken(): String
    
    // Refresh authentication token
    suspend fun refreshAuth(): AuthResult
    
    // Disconnect
    fun disconnect()
}

data class ConnectionResult(
    val success: Boolean,
    val sessionToken: String?,
    val serverEndpoint: String?,
    val error: String?
)
```

### ConstellationApiService

High-level API for RCS operations.

```kotlin
class ConstellationApiService(
    private val rpcClient: RpcClient,
    private val authManager: AuthManager
) {
    // Request phone number verification from carrier
    suspend fun requestVerification(phoneNumber: String): VerificationRequest
    
    // Confirm verification code
    suspend fun confirmVerification(code: String): VerificationResult
    
    // Get list of verified phone numbers
    suspend fun getVerifiedNumbers(): List<VerifiedNumber>
    
    // Check carrier capabilities
    suspend fun getCarrierCapabilities(carrierId: String): CarrierCapabilities
}
```

## Verification Methods

### SMS Verification (MoSmsVerifier, MtSmsVerifier)
- Mobile-Originated: Device sends SMS to carrier shortcode
- Mobile-Terminated: Carrier sends SMS with verification code
- Used as fallback when TS43 unavailable

### TS43 Verification (Ts43Verifier)
- SIM-based authentication using EAP-AKA
- Requires carrier support for TS43 service entitlement
- Components: EapAkaService, Fips186Prf, ServiceEntitlementExtension

### Carrier ID Verification (CarrierIdVerifier)
- Uses carrier provisioning APIs
- Requires carrier bundle configuration
- Fastest method when supported

### Registered SMS (RegisteredSmsVerifier)
- Pre-registered SMS patterns for verification
- Used by carriers with proprietary SMS verification flows

## Protobuf Protocol

The Constellation service uses Protocol Buffers for RPC communication. See `constellation.proto` for the full schema.

Key message types:
- `SyncRequest` - Main request envelope
- `SyncResponse` - Main response envelope  
- `PhoneVerificationRequest` - Verification request
- `PhoneVerificationResponse` - Verification result
- `ClientInfo` - Device/client identification
- `GaiaInfo` - Google account information
- `TelephonyInfo` - SIM/carrier information

## Required Permissions

```xml
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.READ_PHONE_NUMBERS" />
<uses-permission android:name="android.permission.SEND_SMS" />
<uses-permission android:name="android.permission.RECEIVE_SMS" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## Service Configuration

The Constellation service is configured via GServices (Google Services Framework) with the following phenotype keys:

| Key | Description | Default |
|-----|-------------|---------|
| `constellation_verification_timeout_ms` | Verification timeout | 30000 |
| `constellation_max_retries` | Max RPC retries | 3 |
| `constellation_server_endpoint` | Backend URL | (prod endpoint) |
| `constellation_enable_ts43` | Enable TS43 verification | true |
| `constellation_iid_token_refresh_seconds` | Token refresh interval | 3600 |

## Dependencies

- `play-services-base`: Common utilities, PhoneInfo, TelephonyInfo
- `play-services-asterism`: Consent management
- `play-services-tasks`: Async task management
- Protocol Buffers: `com.google.protobuf:protobuf-javalite`

## Related Documentation

- [ARCHITECTURE.md](ARCHITECTURE.md) - Architecture overview
- [README.md](README.md) - Module overview
- [RCS Protocol](../RCS_INTEGRATION.md) - RCS integration guide
- [constellation.proto](core/src/main/proto/constellation.proto) - Protobuf schema
