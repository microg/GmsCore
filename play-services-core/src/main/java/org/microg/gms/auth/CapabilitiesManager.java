/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.util.Arrays;
import java.util.HashSet;

public class CapabilitiesManager {

    private final Context mContext;
    private final String TAG;
    private HashSet<String> enabledCapabilities;
    private HashSet<String> disabledCapabilities;

    public CapabilitiesManager(String TAG, Context context) {
        this.mContext = context;
        this.TAG = TAG;
    }

    public int checkCapabilities(Account account, HashSet<String> capabilities) {
        if (capabilities == null || capabilities.isEmpty()) {
            return 1;
        }
        HashSet<String> capabilitiesForService = getCapabilitiesForService(account, capabilities);
        if (capabilitiesForService.isEmpty()) {
            return 1;
        }
        if (!capabilitiesSyncState(account)) {
            return 6;
        }
        return compareCapabilities(capabilities, account);
    }

    private int compareCapabilities(HashSet<String> capabilities, Account account) {
        HashSet<String> failedCapabilities = getUserDataSet(account, "failed_capabilities");
        int result = 1;
        for (String capability : capabilities) {
            if (!enabledCapabilities.contains(capability)) {
                if (disabledCapabilities.contains(capability)) {
                    return 2;
                }
                if (failedCapabilities != null && failedCapabilities.contains(capability)) {
                    result = 5;
                } else if (result == 1) {
                    result = 3;
                }
            }
        }
        return result;
    }

    private boolean capabilitiesSyncState(Account account) {
        enabledCapabilities = getUserDataSet(account, "enabled_capabilities");
        disabledCapabilities = getUserDataSet(account, "disabled_capabilities");
        return (enabledCapabilities != null && !enabledCapabilities.isEmpty()) || (disabledCapabilities != null && !disabledCapabilities.isEmpty());
    }

    private HashSet<String> getCapabilitiesForService(Account account, HashSet<String> capabilities) {
        HashSet<String> copiedHashSet = new HashSet<>(capabilities);
        HashSet<String> serviceSet = getUserDataSet(account, "services");
        if (serviceSet == null) {
            Log.w(TAG, "CapabilitiesManager getCapabilitiesForService Services not available!");
            return copiedHashSet;
        }
        for (String str : capabilities) {
            if (str.startsWith("service_")) {
                if (serviceSet.contains(str.substring(8))) {
                    copiedHashSet.remove(str);
                }
            } else if (serviceSet.contains(str)) {
                copiedHashSet.remove(str);
            }
        }
        return copiedHashSet;
    }

    private HashSet<String> getUserDataSet(Account account, String keyType) {
        String userData = AccountManager.get(mContext).getUserData(account, keyType);
        if (userData == null) {
            Log.w(TAG, "CapabilitiesManager getUserDataSet userData is null by key:" + keyType);
            return null;
        }
        return new HashSet<>(Arrays.asList(TextUtils.split(userData, ",")));
    }
}
