/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.carrierauth;

import android.content.Context;
import android.os.Build;
import android.os.RemoteException;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.android.gms.carrierauth.EAPAKARequest;
import com.google.android.gms.carrierauth.EAPAKAResponse;
import com.google.android.gms.carrierauth.EapInfoRequest;
import com.google.android.gms.carrierauth.EapInfoResponse;
import com.google.android.gms.carrierauth.internal.ICarrierAuthCallbacks;
import com.google.android.gms.carrierauth.internal.ICarrierAuthServiceStub;
import com.google.android.gms.common.api.ApiMetadata;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;

public final class CarrierAuthServiceImpl extends ICarrierAuthServiceStub {
    private static final String TAG = "CarrierAuthService";

    private final Context context;

    public CarrierAuthServiceImpl(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public void performEAPAKA(
            ICarrierAuthCallbacks callback,
            EAPAKARequest request,
            ApiMetadata metadata) {
        if (callback == null || request == null) {
            Log.w(TAG, "Ignoring EAP-AKA request with missing callback or request");
            return;
        }

        try {
            int subscriptionId = request.e;
            int appType = request.b != null
                    ? request.b
                    : TelephonyManager.APPTYPE_USIM;
            int authType = request.c != null
                    ? request.c
                    : TelephonyManager.AUTHTYPE_EAP_AKA;

            String modemResponse = performUsimAuthentication(
                    subscriptionId,
                    appType,
                    authType,
                    request.a);
            if (modemResponse == null) {
                callback.onEAPAKAResponse(
                        new Status(CommonStatusCodes.CANCELED, "Carrier authentication failed"),
                        null,
                        metadata);
                return;
            }

            callback.onEAPAKAResponse(
                    Status.SUCCESS,
                    new EAPAKAResponse(modemResponse),
                    metadata);
        } catch (RemoteException exception) {
            Log.w(TAG, "Carrier authentication callback failed", exception);
        } catch (RuntimeException exception) {
            Log.w(TAG, "Carrier authentication request failed", exception);
        }
    }

    @Override
    public void getEapInfo(
            ICarrierAuthCallbacks callback,
            EapInfoRequest request,
            ApiMetadata metadata) {
        if (callback == null) {
            return;
        }

        try {
            callback.onEapInfoResponse(
                    Status.SUCCESS,
                    new EapInfoResponse(),
                    metadata);
        } catch (RemoteException exception) {
            Log.w(TAG, "EAP info callback failed", exception);
        }
    }

    private String performUsimAuthentication(
            int subscriptionId,
            int appType,
            int authType,
            String challenge) {
        if (subscriptionId < 0 || challenge == null || challenge.isEmpty()) {
            return null;
        }

        try {
            TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (telephony == null) {
                return null;
            }

            TelephonyManager subscriptionTelephony =
                    telephony.createForSubscriptionId(subscriptionId);
            if (subscriptionTelephony == null) {
                return null;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    && !subscriptionTelephony.hasCarrierPrivileges()) {
                Log.w(TAG, "Carrier privileges unavailable for subscription " + subscriptionId);
                return null;
            }

            return subscriptionTelephony.getIccAuthentication(
                    appType,
                    authType,
                    challenge);
        } catch (SecurityException exception) {
            Log.w(TAG, "Telephony permission denied", exception);
        } catch (IllegalArgumentException exception) {
            Log.w(TAG, "Invalid ICC authentication request", exception);
        } catch (UnsupportedOperationException exception) {
            Log.w(TAG, "ICC authentication is unsupported", exception);
        } catch (RuntimeException exception) {
            Log.w(TAG, "ICC authentication failed", exception);
        }
        return null;
    }
}
