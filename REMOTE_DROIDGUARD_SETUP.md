# Remote DroidGuard Setup Guide for Play Integrity Multi-Step Support

## Overview

This guide explains how to set up and use remote DroidGuard with multi-step support for Play Integrity attestation.

## Problem Statement

Play Integrity requires a multi-step DroidGuard process, but the current remote DroidGuard implementation only supports single-step flows. This document describes the implementation to support multi-step Play Integrity over remote DroidGuard.

## Implementation Details

### 1. Multi-Step Session Support

The implementation adds session-based multi-step support to remote DroidGuard:

#### Key Changes:

1. **Extended Interfaces**:
   - `IDroidGuardHandle.aidl`: Added `begin()`, `nextStep()`, `snapshotWithSession()`, `closeSession()` methods
   - `DroidGuardHandle.java`: Updated Java interface to match AIDL
   - `DroidGuardHandleImpl.java`: Implemented multi-step methods

2. **Session Management**:
   - Session IDs are generated for multi-step flows
   - Session state is maintained across multiple steps
   - Intermediate results are stored per session

3. **Request Extensions**:
   - `DroidGuardResultsRequest` now includes session ID, step number, and multi-step flags
   - These are converted to `x-request-*` query parameters for remote server communication

### 2. Setting Up Remote DroidGuard Server

#### Option A: Using Existing Server (Simplified)

The current implementation works with Google's DroidGuard server at:
```
https://www.googleapis.com/androidantiabuse/v1/x/create?alt=PROTO&key=AIzaSyBofcZsgLSS7BOnBjZPEkk4rYwzOIz-lTI
```

Multi-step information is passed via request bundle parameters:
- `x-request-session-id`: Unique session identifier
- `x-request-step-number`: Current step number (0-based)
- `x-request-total-steps`: Total number of steps in the flow
- `x-request-is-multi-step`: "true" for multi-step flows

#### Option B: Self-Hosted Server (Advanced)

For complete control, you can host your own DroidGuard server:

1. **Requirements**:
   - Android device that passes Play Integrity (DEVICE or STRONG level)
   - Network connectivity
   - microG with remote DroidGuard enabled

2. **Setup Steps**:

```bash
# 1. Enable remote DroidGuard on the server device
#    Settings → microG Settings → SafetyNet → DroidGuard Mode → Remote

# 2. Configure the remote server URL
#    Set to your server's IP address and port

# 3. Start the DroidGuard server service
adb shell am startservice \
  -a com.google.android.gms.droidguard.service.START \
  com.google.android.gms/.droidguard.DroidGuardService
```

3. **Server Configuration**:
```java
// Example server configuration
DroidGuardResultsRequest request = new DroidGuardResultsRequest()
    .setMultiStep(true)
    .setSessionId(sessionId)
    .setStepNumber(currentStep)
    .setTotalSteps(totalSteps);
```

### 3. Client Configuration

#### For microG Users:

1. **Enable Remote DroidGuard**:
   ```
   Settings → microG Settings → SafetyNet → DroidGuard Mode → Remote
   ```

2. **Configure Server URL** (if using self-hosted):
   ```
   Settings → microG Settings → SafetyNet → Remote DroidGuard URL
   ```

3. **Test Play Integrity**:
   Use an app like "Play Integrity API Checker" to verify multi-step support.

#### For App Developers:

```java
// Using multi-step DroidGuard for Play Integrity
DroidGuardClient client = DroidGuard.getClient(context);
DroidGuardResultsRequest request = new DroidGuardResultsRequest()
    .setMultiStep(true)
    .setTotalSteps(3); // Play Integrity typically uses 3 steps

// Begin multi-step session
long sessionId = client.begin("play-integrity", request, initialData);

// Execute steps
for (int step = 0; step < request.getTotalSteps(); step++) {
    Map<String, String> stepData = getStepData(step);
    DroidGuardInitReply reply = client.nextStep(sessionId, stepData);
    processStepReply(reply);
}

// Get final result
String integrityToken = client.snapshotWithSession(sessionId, finalData);

// Clean up
client.closeSession(sessionId);
```

### 4. Troubleshooting

#### Common Issues:

1. **Session Timeout**:
   - Increase timeout in `DroidGuardResultsRequest.setTimeoutMillis()`
   - Default is 60000ms (60 seconds)

2. **Network Connectivity**:
   - Ensure server is accessible from client device
   - Check firewall settings
   - Verify URL configuration

3. **Play Integrity Levels**:
   - Remote device must pass at least DEVICE integrity
   - Use Play Integrity Fix or similar tools on server device

4. **Multi-Step Protocol Errors**:
   - Ensure step numbers are sequential (0, 1, 2, ...)
   - Verify total steps matches actual flow
   - Check session ID consistency

### 5. Security Considerations

1. **Session Security**:
   - Session IDs should be cryptographically random
   - Implement session expiration
   - Validate session ownership

2. **Network Security**:
   - Use HTTPS for remote connections
   - Implement certificate pinning
   - Consider VPN for sensitive applications

3. **Data Privacy**:
   - Minimize data sent to remote server
   - Implement data encryption
   - Follow privacy best practices

### 6. Testing

#### Unit Tests:
```java
@Test
public void testMultiStepPlayIntegrity() {
    // Test multi-step session lifecycle
    long sessionId = droidGuard.begin("play-integrity", request, data);
    assertTrue(sessionId > 0);
    
    // Test step execution
    DroidGuardInitReply reply = droidGuard.nextStep(sessionId, stepData);
    assertNotNull(reply);
    
    // Test final snapshot
    String result = droidGuard.snapshotWithSession(sessionId, finalData);
    assertNotNull(result);
    
    // Test session cleanup
    droidGuard.closeSession(sessionId);
}
```

#### Integration Tests:
- Test with actual Play Integrity API
- Verify token validity with Google servers
- Test network failure scenarios

### 7. Performance Considerations

1. **Latency**:
   - Multi-step adds round-trip latency
   - Consider local caching of intermediate results
   - Optimize step data size

2. **Concurrency**:
   - Support multiple concurrent sessions
   - Implement connection pooling
   - Handle server load balancing

3. **Resource Usage**:
   - Monitor memory usage for session storage
   - Implement session cleanup
   - Optimize network bandwidth

## Conclusion

This implementation enables Play Integrity support over remote DroidGuard by adding multi-step session management. The solution maintains backward compatibility while extending the protocol to support complex attestation flows.

For questions or issues, please refer to the microG GitHub repository or contact the development team.