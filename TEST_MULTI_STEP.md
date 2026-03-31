# Multi-Step DroidGuard Test Plan

## Test Overview
This document outlines the test plan for verifying multi-step DroidGuard support for Play Integrity.

## Test Environment
- microG version: 0.3.6.244735 or later
- Android device with remote DroidGuard enabled
- Network connectivity to DroidGuard server

## Test Cases

### 1. Basic Multi-Step Session Lifecycle
**Objective**: Verify session creation, step execution, and cleanup.

**Steps**:
1. Create a multi-step session with `begin()` method
2. Execute 3 steps using `nextStep()` method
3. Get final result with `snapshotWithSession()`
4. Clean up with `closeSession()`

**Expected Results**:
- Session ID returned (positive long)
- Each step returns valid `DroidGuardInitReply`
- Final snapshot returns valid integrity token
- Session cleanup succeeds

### 2. Play Integrity Multi-Step Flow
**Objective**: Test actual Play Integrity attestation.

**Steps**:
1. Configure remote DroidGuard for Play Integrity
2. Begin multi-step session for "play-integrity" flow
3. Execute typical Play Integrity steps:
   - Step 0: Initialization
   - Step 1: Device attestation
   - Step 2: Integrity verification
4. Get Play Integrity token
5. Validate token with Google servers

**Expected Results**:
- Play Integrity token obtained
- Token passes basic integrity check
- Token structure matches Google's format

### 3. Error Handling
**Objective**: Test error conditions and recovery.

**Test Cases**:
1. **Invalid session ID**: Call `nextStep()` with non-existent session
   - Expected: Returns null or throws appropriate exception
2. **Step out of order**: Call step 2 before step 1
   - Expected: Error indicating invalid step sequence
3. **Session timeout**: Wait longer than timeout between steps
   - Expected: Session expiration error
4. **Network failure**: Disconnect during multi-step flow
   - Expected: Graceful error handling, session cleanup

### 4. Backward Compatibility
**Objective**: Ensure existing single-step flows still work.

**Test Cases**:
1. **Single-step snapshot**: Call original `snapshot()` method
   - Expected: Works as before
2. **Mixed usage**: Use both single-step and multi-step in same app
   - Expected: No interference between flows
3. **Legacy clients**: Old clients using only single-step API
   - Expected: Continue to work unchanged

### 5. Performance Testing
**Objective**: Measure multi-step overhead.

**Metrics to Measure**:
1. **Session creation time**: Time for `begin()` to return
2. **Step execution time**: Average time per `nextStep()` call
3. **Total flow time**: Complete multi-step flow duration
4. **Memory usage**: Session state memory consumption
5. **Network overhead**: Additional data transferred for multi-step

**Acceptance Criteria**:
- Multi-step overhead < 50% compared to single-step
- Memory usage scales linearly with active sessions
- Network latency within acceptable bounds for use case

### 6. Security Testing
**Objective**: Verify security of multi-step implementation.

**Test Cases**:
1. **Session hijacking**: Attempt to use another client's session ID
   - Expected: Access denied
2. **Replay attacks**: Reuse old session data
   - Expected: Session validation fails
3. **Data integrity**: Tamper with step data in transit
   - Expected: Signature verification fails
4. **Information leakage**: Check for sensitive data exposure
   - Expected: No sensitive data in logs or network traces

### 7. Integration Testing
**Objective**: Test with real applications.

**Applications to Test**:
1. **Dott app**: Original use case from issue #2851
2. **Banking apps**: Typically use Play Integrity
3. **Ride-sharing apps**: Similar to Dott
4. **Payment apps**: High-security requirements

**Success Criteria**:
- Apps can successfully authenticate
- Play Integrity passes at required level
- User experience not degraded

## Test Implementation

### Unit Tests
```java
public class MultiStepDroidGuardTest {
    @Test
    public void testSessionLifecycle() {
        DroidGuardClient client = DroidGuard.getClient(context);
        DroidGuardResultsRequest request = new DroidGuardResultsRequest()
            .setMultiStep(true)
            .setTotalSteps(3);
        
        // Test session creation
        long sessionId = client.begin("test-flow", request, new HashMap<>());
        assertTrue(sessionId > 0);
        
        // Test step execution
        for (int i = 0; i < 3; i++) {
            Map<String, String> stepData = new HashMap<>();
            stepData.put("step", String.valueOf(i));
            DroidGuardInitReply reply = client.nextStep(sessionId, stepData);
            assertNotNull(reply);
        }
        
        // Test final result
        String result = client.snapshotWithSession(sessionId, new HashMap<>());
        assertNotNull(result);
        
        // Test cleanup
        client.closeSession(sessionId);
    }
}
```

### Integration Test Script
```bash
#!/bin/bash
# test_multi_step.sh

echo "Starting multi-step DroidGuard tests..."

# Test 1: Basic session lifecycle
echo "Test 1: Basic session lifecycle"
adb shell am instrument -w \
  -e class org.microg.gms.droidguard.test.MultiStepBasicTest \
  org.microg.gms.droidguard.test/androidx.test.runner.AndroidJUnitRunner

# Test 2: Play Integrity flow
echo "Test 2: Play Integrity flow"
adb shell am instrument -w \
  -e class org.microg.gms.droidguard.test.PlayIntegrityTest \
  org.microg.gms.droidguard.test/androidx.test.runner.AndroidJUnitRunner

# Test 3: Error conditions
echo "Test 3: Error conditions"
adb shell am instrument -w \
  -e class org.microg.gms.droidguard.test.ErrorHandlingTest \
  org.microg.gms.droidguard.test/androidx.test.runner.AndroidJUnitRunner

echo "Tests completed."
```

## Test Data

### Sample Step Data
```json
{
  "step_0": {
    "action": "initialize",
    "device_info": "basic",
    "timestamp": "2026-04-01T02:15:00Z"
  },
  "step_1": {
    "action": "attest",
    "integrity_level": "device",
    "challenge": "abc123..."
  },
  "step_2": {
    "action": "verify",
    "attestation_result": "encoded_data...",
    "signature": "sig..."
  }
}
```

### Expected Results Format
```json
{
  "session_id": 1234567890,
  "status": "completed",
  "integrity_token": "eyJhbGciOiJSUzI1NiIs...",
  "integrity_level": "device",
  "timestamp": "2026-04-01T02:16:00Z"
}
```

## Debugging Tips

### Common Issues and Solutions:
1. **Session not found**: Verify session ID persistence across steps
2. **Step sequence error**: Ensure steps are called in order (0, 1, 2, ...)
3. **Network timeout**: Increase timeout in `DroidGuardResultsRequest`
4. **Server errors**: Check server logs and connectivity

### Logging:
Enable verbose logging for debugging:
```java
// In test setup
Log.setLoggable("DroidGuard", Log.VERBOSE);
Log.setLoggable("GmsGuardChimera", Log.VERBOSE);
```

### Monitoring:
Monitor these metrics during testing:
- Network requests and responses
- Session state transitions
- Error rates and types
- Performance timings

## Success Criteria

The implementation is considered successful when:

1. **Functional**: All test cases pass
2. **Performance**: Meets performance targets
3. **Security**: Passes security review
4. **Compatibility**: Maintains backward compatibility
5. **Reliability**: Handles errors gracefully
6. **Usability**: Works with real applications

## Next Steps After Testing

1. **Fix issues** identified during testing
2. **Optimize performance** based on test results
3. **Update documentation** with any changes
4. **Prepare for release** and PR submission
5. **Monitor production** usage after deployment