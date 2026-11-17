/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;

import com.google.android.gms.common.api.ApiMetadata;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.constellation.GetPnvCapabilitiesRequest;
import com.google.android.gms.constellation.GetPnvCapabilitiesResponse;
import com.google.android.gms.constellation.GetPnvCapabilitiesResponse.SimCapability;
import com.google.android.gms.constellation.VerificationStatus;
import com.google.android.gms.constellation.internal.IConstellationCallbacks;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * Handler for {@code getPnvCapabilities}: reports, per active SIM, what phone-number
 * verification methods this build can attempt. This is computed entirely from local
 * telephony state, no server call.
 *
 * <p>Currently offered: MO-SMS and MT-SMS when the SIM is ready. Other methods
 * (carrier id, IMSI lookup, registered SMS, flash call, TS43) are not implemented in
 * this build and are therefore omitted from the answer rather than reported as
 * supported. TS43 additionally needs a carrier allowlist that does not exist yet.
 */
public final class GetPnvCapabilities {
    private static final String TAG = "GmsConstellationCaps";

    private GetPnvCapabilities() {
    }

    @SuppressLint("HardwareIds")
    public static void handle(Context context, IConstellationCallbacks callbacks, GetPnvCapabilitiesRequest request) {
        try {
            TelephonyManager baseTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (baseTelephonyManager == null) {
                throw new IllegalStateException("TelephonyManager unavailable");
            }
            List<SimCapability> simCapabilities = new ArrayList<>();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                SubscriptionManager subscriptionManager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
                if (subscriptionManager == null) {
                    throw new IllegalStateException("SubscriptionManager unavailable");
                }
                List<SubscriptionInfo> subscriptions = subscriptionManager.getActiveSubscriptionInfoList();
                if (subscriptions != null) {
                    for (SubscriptionInfo info : subscriptions) {
                        if (request.simSlotIndices != null && !request.simSlotIndices.isEmpty()
                                && !request.simSlotIndices.contains(info.getSimSlotIndex())) {
                            continue;
                        }
                        simCapabilities.add(buildSimCapability(baseTelephonyManager, info, request));
                    }
                }
            }
            callbacks.onGetPnvCapabilitiesCompleted(Status.SUCCESS,
                    new GetPnvCapabilitiesResponse(simCapabilities), ApiMetadata.DEFAULT);
        } catch (SecurityException e) {
            Log.e(TAG, "getPnvCapabilities missing permission", e);
            reportError(callbacks, new Status(5000));
        } catch (Exception e) {
            Log.e(TAG, "getPnvCapabilities failed", e);
            reportError(callbacks, Status.INTERNAL_ERROR);
        }
    }

    private static void reportError(IConstellationCallbacks callbacks, Status status) {
        try {
            callbacks.onGetPnvCapabilitiesCompleted(status,
                    new GetPnvCapabilitiesResponse(new ArrayList<SimCapability>()), ApiMetadata.DEFAULT);
        } catch (Exception e) {
            Log.e(TAG, "failed to report getPnvCapabilities error", e);
        }
    }

    @SuppressLint("HardwareIds")
    private static SimCapability buildSimCapability(TelephonyManager baseTelephonyManager,
            SubscriptionInfo info, GetPnvCapabilitiesRequest request) {
        TelephonyManager telephonyManager = baseTelephonyManager;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            telephonyManager = baseTelephonyManager.createForSubscriptionId(info.getSubscriptionId());
        }

        int carrierId = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            carrierId = telephonyManager.getSimCarrierId();
        }

        boolean simReady = telephonyManager.getSimState() == TelephonyManager.SIM_STATE_READY;
        List<GetPnvCapabilitiesResponse.VerificationCapability> capabilities = new ArrayList<>();
        List<Integer> wanted = request.verificationTypes;
        // An empty request means "any method".
        if (wanted == null || wanted.isEmpty()) {
            wanted = new ArrayList<>();
            wanted.add(VerificationMethods.METHOD_MO_SMS);
            wanted.add(VerificationMethods.METHOD_MT_SMS);
        }
        for (int method : wanted) {
            if (!VerificationMethods.isSupportedLocally(method)) {
                continue;
            }
            capabilities.add(VerificationStatus.verificationCapability(method,
                    simReady ? VerificationStatus.SUPPORTED : VerificationStatus.UNSUPPORTED_SIM_NOT_READY));
        }

        String subscriberIdDigest = digestSubscriberId(getSubscriberIdSafe(telephonyManager));
        String operatorName = telephonyManager.getSimOperatorName();
        // getSimSlotIndex() needs API 22. The caller only runs this method on
        // API 22+, but lint checks this call site directly, so guard it here too.
        int simSlotIndex = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1
                ? info.getSimSlotIndex() : -1;
        return new SimCapability(simSlotIndex, subscriberIdDigest, carrierId,
                operatorName != null ? operatorName : "", capabilities);
    }

    private static String getSubscriberIdSafe(TelephonyManager telephonyManager) {
        try {
            return telephonyManager.getSubscriberId();
        } catch (SecurityException e) {
            Log.w(TAG, "no permission to read subscriber id, using empty digest");
            return null;
        }
    }

    /** SHA-256 of the subscriber id (or of the empty string when unknown), Base64, no wrapping. */
    public static String digestSubscriberId(String subscriberId) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((subscriberId != null ? subscriberId : "").getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(digest, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "failed to digest subscriber id", e);
            return "";
        }
    }
}
