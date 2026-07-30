/*
 * Copyright (C) 2013-2026 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.gms.common.telephony;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;

import androidx.core.content.ContextCompat;

/**
 * Reads real telephony information from the Android framework.
 *
 * No carrier identifiers are hardcoded. When a permission is missing or the
 * information is unavailable, an empty string is returned.
 */
public class SystemTelephonyInfoProvider implements TelephonyInfoProvider {
    private final Context context;
    private final TelephonyManager telephonyManager;

    public SystemTelephonyInfoProvider(Context context) {
        this.context = context.getApplicationContext();
        this.telephonyManager = (TelephonyManager) this.context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    @Override
    public String getSimOperatorNumeric() {
        if (telephonyManager == null) return "";
        String value = telephonyManager.getSimOperator();
        return value != null ? value : "";
    }

    @Override
    public String getNetworkOperatorNumeric() {
        if (telephonyManager == null) return "";
        String value = telephonyManager.getNetworkOperator();
        return value != null ? value : "";
    }

    @Override
    public String getSubscriberId() {
        if (telephonyManager == null) return "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (!hasPermission("android.permission.READ_PRIVILEGED_PHONE_STATE")) return "";
        } else {
            if (!hasPermission(Manifest.permission.READ_PHONE_STATE)) return "";
        }
        try {
            String value = telephonyManager.getSubscriberId();
            return value != null ? value : "";
        } catch (SecurityException e) {
            return "";
        }
    }

    @Override
    public String getRoamingState() {
        if (telephonyManager == null) return "";
        boolean isRoaming = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ServiceState serviceState = telephonyManager.getServiceState();
            if (serviceState != null) {
                isRoaming = serviceState.getRoaming();
            }
        } else {
            String sim = getSimOperatorNumeric();
            String network = getNetworkOperatorNumeric();
            isRoaming = !sim.isEmpty() && !network.isEmpty() && !sim.equals(network);
        }
        return isRoaming ? "mobile-roaming" : "mobile-notroaming";
    }

    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }
}
