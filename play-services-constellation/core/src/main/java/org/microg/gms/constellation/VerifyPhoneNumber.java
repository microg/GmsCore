/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.api.ApiMetadata;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.constellation.VerifyPhoneNumberRequest;
import com.google.android.gms.constellation.VerifyPhoneNumberResponse;
import com.google.android.gms.constellation.internal.IConstellationCallbacks;

import java.util.ArrayList;
import java.util.List;

/**
 * Handler for the {@code verifyPhoneNumber} family.
 *
 * <p><b>Staged implementation (Phase 3a).</b> The request is validated, mapped
 * (for the Bundle-based V1 and single-use variants), and recorded, but the actual
 * verification is not performed yet: it needs the server RPC
 * ({@code Sync}/{@code Proceed} against Google's phone-device-verification service,
 * plus the SMS challenge exchange), which is the next block (Phase 3b). Until that
 * lands, this handler honestly answers {@code INTERNAL_ERROR} instead of pretending
 * the number was verified. No phone number, IMSI, or token is ever logged.
 */
public final class VerifyPhoneNumber {
    private static final String TAG = "GmsConstellationVerify";

    private VerifyPhoneNumber() {
    }

    public static void handleV1(Context context, IConstellationCallbacks callbacks, Bundle bundle, String packageName) {
        String callingPackage = packageName != null ? packageName
                : bundle.getString("calling_package", "com.google.android.gms");
        Bundle extras = new Bundle(bundle);
        extras.putString("calling_package", callingPackage);
        extras.putString("calling_api", "verifyPhoneNumber");
        VerifyPhoneNumberRequest request = new VerifyPhoneNumberRequest(
                extras.getString("policy_id", ""),
                getTimeout(extras),
                new VerifyPhoneNumberRequest.IdTokenRequest(
                        extras.getString("certificate_hash", ""),
                        extras.getString("token_nonce", "")),
                extras,
                new ArrayList<VerifyPhoneNumberRequest.ImsiRequest>(),
                false,
                2,
                new ArrayList<Integer>());
        // V1 callers expect the legacy callback as well as the typed one.
        handleRequest(context, callbacks, request, callingPackage, true);
    }

    public static void handleSingleUse(Context context, IConstellationCallbacks callbacks, Bundle bundle, String packageName) {
        String callingPackage = packageName != null ? packageName
                : bundle.getString("calling_package", "com.google.android.gms");
        Bundle extras = new Bundle(bundle);
        extras.putString("calling_package", callingPackage);
        extras.putString("calling_api", "verifyPhoneNumberSingleUse");
        extras.putString("one_time_verification", "True");
        VerifyPhoneNumberRequest request = new VerifyPhoneNumberRequest(
                extras.getString("policy_id", ""),
                getTimeout(extras),
                new VerifyPhoneNumberRequest.IdTokenRequest(
                        extras.getString("certificate_hash", ""),
                        extras.getString("token_nonce", "")),
                extras,
                new ArrayList<VerifyPhoneNumberRequest.ImsiRequest>(),
                false,
                2,
                new ArrayList<Integer>());
        handleRequest(context, callbacks, request, callingPackage, true);
    }

    public static void handleRequest(Context context, IConstellationCallbacks callbacks,
            VerifyPhoneNumberRequest request, String packageName, boolean legacyCallback) {
        int targetedSims = request.targetedSims != null ? request.targetedSims.size() : 0;
        int methods = request.verificationMethodsValues != null ? request.verificationMethodsValues.size() : 0;
        new ConstellationStateStore(context).recordAttempt("verifyPhoneNumber policy=" + request.policyId
                + " apiVersion=" + request.apiVersion + " targetedSims=" + targetedSims
                + " methods=" + methods + " outcome=not-implemented");
        Log.i(TAG, "verifyPhoneNumber requested (policy=" + request.policyId
                + ", apiVersion=" + request.apiVersion + ", targetedSims=" + targetedSims
                + ", methods=" + methods + "): server RPC not implemented yet, answering INTERNAL_ERROR");

        VerifyPhoneNumberResponse response = new VerifyPhoneNumberResponse(
                new VerifyPhoneNumberResponse.PhoneNumberVerification[0], new Bundle());
        try {
            if (legacyCallback) {
                callbacks.onPhoneNumberVerified(Status.INTERNAL_ERROR, new ArrayList<>(), ApiMetadata.DEFAULT);
            }
            callbacks.onPhoneNumberVerificationsCompleted(Status.INTERNAL_ERROR, response, ApiMetadata.DEFAULT);
        } catch (Exception e) {
            Log.e(TAG, "failed to report verifyPhoneNumber error", e);
        }
    }

    private static long getTimeout(Bundle extras) {
        Object timeoutValue = extras.get("timeout");
        if (timeoutValue instanceof Long) {
            return (Long) timeoutValue;
        }
        if (timeoutValue instanceof Integer) {
            return ((Integer) timeoutValue).longValue();
        }
        return 300L;
    }
}
