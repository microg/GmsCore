/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.carrierauth;

import android.telephony.TelephonyManager;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.annotation.Resetter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Implements(TelephonyManager.class)
public class ShadowTelephonyManager extends org.robolectric.shadows.ShadowTelephonyManager {
    private static final Map<Integer, Boolean> CARRIER_PRIVILEGES =
            new ConcurrentHashMap<>();
    private static final Map<Integer, String> AUTH_RESPONSES =
            new ConcurrentHashMap<>();
    private static final ThreadLocal<Integer> CURRENT_SUBSCRIPTION =
            new ThreadLocal<>();

    @RealObject
    private TelephonyManager realObject;

    @Implementation
    public TelephonyManager createForSubscriptionId(int subId) {
        CURRENT_SUBSCRIPTION.set(subId);
        return realObject;
    }

    @Implementation
    public boolean hasCarrierPrivileges() {
        Integer subscriptionId = CURRENT_SUBSCRIPTION.get();
        return subscriptionId != null
                && Boolean.TRUE.equals(CARRIER_PRIVILEGES.get(subscriptionId));
    }

    @Implementation
    public String getIccAuthentication(int appType, int authType, String data) {
        return AUTH_RESPONSES.get(appType);
    }

    public static void setCarrierPrivileges(int subId, boolean hasPrivileges) {
        CARRIER_PRIVILEGES.put(subId, hasPrivileges);
    }

    public static void setMockAuthResponse(int appType, String response) {
        if (response == null) {
            AUTH_RESPONSES.remove(appType);
        } else {
            AUTH_RESPONSES.put(appType, response);
        }
    }

    @Resetter
    public static void reset() {
        CARRIER_PRIVILEGES.clear();
        AUTH_RESPONSES.clear();
        CURRENT_SUBSCRIPTION.remove();
    }
}
