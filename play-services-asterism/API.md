# Asterism API Reference

## Overview

Asterism is Google Play Services' consent management module for Rich Communication Services (RCS). It manages user consent for RCS features, phone number verification, and carrier provisioning through the Constellation service.

## Package

```
com.google.android.gms.asterism
```

## AIDL Interfaces

### IAsterismService

Located at: `com/google/android/gms/asterism/internal/IAsterismService.aidl`

Primary service interface for Asterism operations.

```java
interface IAsterismService {
    void getAsterismConsent(GetAsterismConsentRequest request, IAsterismCallback callback);
    void setAsterismConsent(SetAsterismConsentRequest request, IAsterismCallback callback);
}
```

### IAsterismCallback

```java
interface IAsterismCallback {
    void onResult(Bundle result);
    void onError(int errorCode, String message);
}
```

## Java API

### GetAsterismConsentRequest

```java
package com.google.android.gms.asterism;

public class GetAsterismConsentRequest {
    private final String phoneNumber;
    private final int consentType;  // 0 = RCS, 1 = Carrier Provisioning, 2 = Phone Verification
    private final Bundle extras;

    public static class Builder {
        public Builder setPhoneNumber(String phoneNumber);
        public Builder setConsentType(int consentType);
        public Builder setExtras(Bundle extras);
        public GetAsterismConsentRequest build();
    }

    public String getPhoneNumber();
    public int getConsentType();
    public Bundle getExtras();
}
```

### GetAsterismConsentResponse

```java
public class GetAsterismConsentResponse {
    private final int status;       // 0 = Granted, 1 = Denied, 2 = Unknown, 3 = Pending
    private final String token;      // Consent token for RCS provisioning
    private final long expiryTime;   // Unix timestamp in milliseconds
    private final Bundle metadata;

    public int getStatus();
    public String getToken();
    public long getExpiryTime();
    public Bundle getMetadata();

    // Status constants
    public static final int STATUS_GRANTED = 0;
    public static final int STATUS_DENIED = 1;
    public static final int STATUS_UNKNOWN = 2;
    public static final int STATUS_PENDING = 3;
}
```

### SetAsterismConsentRequest

```java
public class SetAsterismConsentRequest {
    private final String phoneNumber;
    private final boolean consentGranted;
    private final int consentType;
    private final String reason;

    public static class Builder {
        public Builder setPhoneNumber(String phoneNumber);
        public Builder setConsentGranted(boolean granted);
        public Builder setConsentType(int consentType);
        public Builder setReason(String reason);
        public SetAsterismConsentRequest build();
    }

    public String getPhoneNumber();
    public boolean isConsentGranted();
    public int getConsentType();
    public String getReason();
}
```

### SetAsterismConsentResponse

```java
public class SetAsterismConsentResponse {
    private final boolean success;
    private final String message;
    private final long timestamp;

    public boolean isSuccess();
    public String getMessage();
    public long getTimestamp();
}
```

## Internal Service Implementation

### AsterismService

```kotlin
package org.microg.gms.asterism.core

class AsterismService : IAsterismService.Stub() {
    
    // Handles consent retrieval from local store or carrier
    override fun getAsterismConsent(request: GetAsterismConsentRequest, callback: IAsterismCallback)
    
    // Persists user consent preference
    override fun setAsterismConsent(request: SetAsterismConsentRequest, callback: IAsterismCallback)
    
    // Validates consent token with Constellation backend
    fun validateConsent(token: String, phoneNumber: String): ValidationResult
}
```

### AsterismConsentStore

```kotlin
class AsterismConsentStore(context: Context) {
    
    // Retrieve stored consent for a phone number
    fun getConsent(phoneNumber: String): ConsentRecord?
    
    // Save new consent
    fun setConsent(phoneNumber: String, consent: ConsentRecord)
    
    // Remove consent (user revocation)
    fun removeConsent(phoneNumber: String)
    
    // Check if consent is expired
    fun isExpired(phoneNumber: String): Boolean
    
    // List all active consents
    fun listActiveConsents(): List<ConsentRecord>
}

data class ConsentRecord(
    val phoneNumber: String,
    val consentType: Int,
    val granted: Boolean,
    val token: String?,
    val expiresAt: Long,
    val createdAt: Long
)
```

## Service Intent Actions

| Action | Description |
|--------|-------------|
| `com.google.android.gms.asterism.START` | Start Asterism service |
| `com.google.android.gms.asterism.GET_CONSENT` | Request consent check |
| `com.google.android.gms.asterism.SET_CONSENT` | Update consent status |
| `com.google.android.gms.asterism.CONSENT_CHANGED` | Broadcast: consent changed |

## Required Permissions

```xml
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.SEND_SMS" />
<uses-permission android:name="android.permission.RECEIVE_SMS" />
<uses-permission android:name="com.google.android.gms.asterism.CONSENT" />
```

## Consent Flow

```
1. App requests RCS consent → GetAsterismConsentRequest
2. AsterismService checks local AsterismConsentStore
3. If not found → queries Constellation backend via RPC
4. Constellation verifies phone number with carrier
5. Returns consent status + provisioning token
6. mGmsCore stores consent in AsterismConsentStore
7. Broadcast CONSENT_CHANGED to registered listeners

User Opt-Out Flow:
1. User revokes consent in Settings
2. SetAsterismConsentRequest(consentGranted=false)
3. AsterismConsentStore.removeConsent()
4. Carrier notified via Constellation
5. Broadcast CONSENT_CHANGED
```

## Dependencies

- `play-services-base`: Common utilities, PhoneInfo
- `play-services-constellation`: Backend RPC communication
- `play-services-tasks`: Async task management

## Related Documentation

- [ARCHITECTURE.md](ARCHITECTURE.md) - Architecture overview
- [README.md](README.md) - Module overview
- [RCS Protocol](../RCS_INTEGRATION.md) - RCS integration guide
