/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation;

import static org.junit.Assert.assertEquals;

import com.google.android.gms.constellation.PhoneNumberVerification;
import com.squareup.wire.GrpcException;
import com.squareup.wire.GrpcStatus;

import org.junit.Test;

public class ConstellationServiceImplTest {

    // --- mapExceptionToStatusCode tests ---

    @Test
    public void mapException_resourceExhausted_returns5008() {
        GrpcException e = new GrpcException(GrpcStatus.RESOURCE_EXHAUSTED, "quota", null);
        assertEquals(5008, ConstellationServiceImpl.mapExceptionToStatusCode(e));
    }

    @Test
    public void mapException_deadlineExceeded_returns5007() {
        GrpcException e = new GrpcException(GrpcStatus.DEADLINE_EXCEEDED, "timeout", null);
        assertEquals(5007, ConstellationServiceImpl.mapExceptionToStatusCode(e));
    }

    @Test
    public void mapException_aborted_returns5007() {
        GrpcException e = new GrpcException(GrpcStatus.ABORTED, "aborted", null);
        assertEquals(5007, ConstellationServiceImpl.mapExceptionToStatusCode(e));
    }

    @Test
    public void mapException_unavailable_returns5007() {
        GrpcException e = new GrpcException(GrpcStatus.UNAVAILABLE, "unavailable", null);
        assertEquals(5007, ConstellationServiceImpl.mapExceptionToStatusCode(e));
    }

    @Test
    public void mapException_permissionDenied_returns5009() {
        GrpcException e = new GrpcException(GrpcStatus.PERMISSION_DENIED, "denied", null);
        assertEquals(5009, ConstellationServiceImpl.mapExceptionToStatusCode(e));
    }

    @Test
    public void mapException_invalidArgument_returns5002() {
        GrpcException e = new GrpcException(GrpcStatus.INVALID_ARGUMENT, "bad arg", null);
        assertEquals(5002, ConstellationServiceImpl.mapExceptionToStatusCode(e));
    }

    @Test
    public void mapException_unauthenticated_returns5002() {
        GrpcException e = new GrpcException(GrpcStatus.UNAUTHENTICATED, "no auth", null);
        assertEquals(5002, ConstellationServiceImpl.mapExceptionToStatusCode(e));
    }

    @Test
    public void mapException_wrappedGrpcException_unwraps() {
        GrpcException inner = new GrpcException(GrpcStatus.RESOURCE_EXHAUSTED, "quota", null);
        RuntimeException wrapper = new RuntimeException("wrapped", inner);
        assertEquals(5008, ConstellationServiceImpl.mapExceptionToStatusCode(wrapper));
    }

    @Test
    public void mapException_nonGrpc_returns8() {
        assertEquals(8, ConstellationServiceImpl.mapExceptionToStatusCode(new NullPointerException("npe")));
    }

    @Test
    public void mapException_plainIOException_returns8() {
        assertEquals(8, ConstellationServiceImpl.mapExceptionToStatusCode(new java.io.IOException("network")));
    }

    // --- decideVerificationOutcome tests ---

    @Test
    public void decideVerificationOutcome_verifiedKeepsToken() {
        ConstellationServiceImpl.VerificationDecision decision =
                ConstellationServiceImpl.decideVerificationOutcome("real-jwt", false);

        assertEquals(PhoneNumberVerification.STATUS_VERIFIED, decision.status);
        assertEquals("real-jwt", decision.token);
    }

    @Test
    public void decideVerificationOutcome_ineligibleForcesEmptyToken() {
        ConstellationServiceImpl.VerificationDecision decision =
                ConstellationServiceImpl.decideVerificationOutcome("some-token", true);

        assertEquals(PhoneNumberVerification.STATUS_INELIGIBLE, decision.status);
        assertEquals("", decision.token);
    }

    @Test
    public void decideVerificationOutcome_nullTokenBecomesFailureWithEmptyToken() {
        ConstellationServiceImpl.VerificationDecision decision =
                ConstellationServiceImpl.decideVerificationOutcome(null, false);

        assertEquals(PhoneNumberVerification.STATUS_NON_RETRYABLE_FAILURE, decision.status);
        assertEquals("", decision.token);
    }

    @Test
    public void decideVerificationOutcome_emptyTokenBecomesFailureWithEmptyToken() {
        ConstellationServiceImpl.VerificationDecision decision =
                ConstellationServiceImpl.decideVerificationOutcome("", false);

        assertEquals(PhoneNumberVerification.STATUS_NON_RETRYABLE_FAILURE, decision.status);
        assertEquals("", decision.token);
    }

    // --- extractVerificationMethodFromJwt tests ---

    private static String fakeJwt(String payloadJson) {
        String header = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"RS256\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String payload = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return header + "." + payload + ".fakesig";
    }

    @Test
    public void extractMethod_mtSms() {
        String jwt = fakeJwt("{\"google\":{\"phone_number_verification_method\":\"VERIFICATION_METHOD_MT_SMS\"}}");
        assertEquals(PhoneNumberVerification.METHOD_MT_SMS, ConstellationServiceImpl.extractVerificationMethodFromJwt(jwt));
    }

    @Test
    public void extractMethod_moSms() {
        String jwt = fakeJwt("{\"google\":{\"phone_number_verification_method\":\"VERIFICATION_METHOD_MO_SMS\"}}");
        assertEquals(PhoneNumberVerification.METHOD_MO_SMS, ConstellationServiceImpl.extractVerificationMethodFromJwt(jwt));
    }

    @Test
    public void extractMethod_ts43() {
        String jwt = fakeJwt("{\"google\":{\"phone_number_verification_method\":\"VERIFICATION_METHOD_TS43\"}}");
        assertEquals(PhoneNumberVerification.METHOD_TS43_AIDL, ConstellationServiceImpl.extractVerificationMethodFromJwt(jwt));
    }

    @Test
    public void extractMethod_noClaim_defaultsToTs43Aidl() {
        String jwt = fakeJwt("{\"iss\":\"accounts.google.com\"}");
        assertEquals(PhoneNumberVerification.METHOD_TS43_AIDL, ConstellationServiceImpl.extractVerificationMethodFromJwt(jwt));
    }

    @Test
    public void extractMethod_nullToken_defaultsToTs43Aidl() {
        assertEquals(PhoneNumberVerification.METHOD_TS43_AIDL, ConstellationServiceImpl.extractVerificationMethodFromJwt(null));
    }

    @Test
    public void extractMethod_emptyToken_defaultsToTs43Aidl() {
        assertEquals(PhoneNumberVerification.METHOD_TS43_AIDL, ConstellationServiceImpl.extractVerificationMethodFromJwt(""));
    }

    @Test
    public void extractMethod_nonJwtString_defaultsToTs43Aidl() {
        assertEquals(PhoneNumberVerification.METHOD_TS43_AIDL, ConstellationServiceImpl.extractVerificationMethodFromJwt("not-a-jwt"));
    }

    // --- mapVerificationMethodString tests ---

    @Test
    public void mapMethodString_allKnownMethods() {
        assertEquals(PhoneNumberVerification.METHOD_MT_SMS, ConstellationServiceImpl.mapVerificationMethodString("VERIFICATION_METHOD_MT_SMS"));
        assertEquals(PhoneNumberVerification.METHOD_MO_SMS, ConstellationServiceImpl.mapVerificationMethodString("VERIFICATION_METHOD_MO_SMS"));
        assertEquals(PhoneNumberVerification.METHOD_CARRIER_ID, ConstellationServiceImpl.mapVerificationMethodString("VERIFICATION_METHOD_CARRIER_ID"));
        assertEquals(PhoneNumberVerification.METHOD_IMSI_LOOKUP, ConstellationServiceImpl.mapVerificationMethodString("VERIFICATION_METHOD_IMSI_LOOKUP"));
        assertEquals(PhoneNumberVerification.METHOD_REGISTERED_SMS, ConstellationServiceImpl.mapVerificationMethodString("VERIFICATION_METHOD_REGISTERED_SMS"));
        assertEquals(PhoneNumberVerification.METHOD_FLASH_CALL, ConstellationServiceImpl.mapVerificationMethodString("VERIFICATION_METHOD_FLASH_CALL"));
        assertEquals(PhoneNumberVerification.METHOD_TS43_AIDL, ConstellationServiceImpl.mapVerificationMethodString("VERIFICATION_METHOD_TS43"));
    }

    @Test
    public void mapMethodString_unknown_defaultsToTs43Aidl() {
        assertEquals(PhoneNumberVerification.METHOD_TS43_AIDL, ConstellationServiceImpl.mapVerificationMethodString("SOMETHING_NEW"));
    }

    // --- EntitlementResult.needsManualMsisdn tests ---

    @Test
    public void entitlementResult_phoneNumberEntryRequired_isNotError() {
        Ts43Client.EntitlementResult result = Ts43Client.EntitlementResult.phoneNumberEntryRequired("test-reason");
        assertEquals(false, result.isError());
        assertEquals(true, result.needsManualMsisdn);
        assertEquals(null, result.token);
    }

    @Test
    public void entitlementResult_error_isError() {
        Ts43Client.EntitlementResult result = Ts43Client.EntitlementResult.error("test-error");
        assertEquals(true, result.isError());
        assertEquals(false, result.needsManualMsisdn);
    }

    @Test
    public void entitlementResult_success_isNotError() {
        Ts43Client.EntitlementResult result = Ts43Client.EntitlementResult.success("jwt-token");
        assertEquals(false, result.isError());
        assertEquals(false, result.needsManualMsisdn);
    }
}
