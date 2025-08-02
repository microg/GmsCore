/*
 * Copyright (C) 2013-2024 microG Project Team
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

package org.microg.gms.rcs;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

public class RcsServiceImpl {
    private static final String TAG = "GmsRcsSvc";
    private static final String PREFS_NAME = "rcs_prefs";
    
    private final Context context;
    private final SharedPreferences prefs;
    private final TelephonyManager telephonyManager;

    public RcsServiceImpl() {
        this.context = null;
        this.prefs = null;
        this.telephonyManager = null;
    }

    public RcsServiceImpl(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    public IBinder getBinder() {
        return new IRcsBinder();
    }

    private class IRcsBinder extends android.os.Binder {
        
        public boolean isRcsSupported() {
            return true;
        }

        public boolean isRcsEnabled() {
            if (prefs != null) {
                return prefs.getBoolean("enabled", true);
            }
            return true;
        }

        public void setRcsEnabled(boolean enabled) {
            if (prefs != null) {
                prefs.edit().putBoolean("enabled", enabled).apply();
            }
        }

        public String getRcsVersion() {
            return "2.0";
        }

        public boolean isRcsCompatible() {
            return true;
        }

        public String getRcsProfile() {
            return "UP_2.0";
        }

        public boolean isRcsProvisioned() {
            return true;
        }

        public int getRcsRegistrationState() {
            return 2; // REGISTERED
        }

        public boolean isCapabilityDiscoveryEnabled() {
            if (prefs != null) {
                return prefs.getBoolean("capability_discovery", true);
            }
            return true;
        }

        public void setCapabilityDiscoveryEnabled(boolean enabled) {
            if (prefs != null) {
                prefs.edit().putBoolean("capability_discovery", enabled).apply();
            }
        }

        public int getRcsChatMode() {
            if (prefs != null) {
                return prefs.getInt("chat_mode", 1);
            }
            return 1;
        }

        public void setRcsChatMode(int mode) {
            if (prefs != null) {
                prefs.edit().putInt("chat_mode", mode).apply();
            }
        }

        public boolean isReadReceiptsEnabled() {
            if (prefs != null) {
                return prefs.getBoolean("read_receipts", true);
            }
            return true;
        }

        public void setReadReceiptsEnabled(boolean enabled) {
            if (prefs != null) {
                prefs.edit().putBoolean("read_receipts", enabled).apply();
            }
        }

        public boolean isTypingIndicatorsEnabled() {
            if (prefs != null) {
                return prefs.getBoolean("typing_indicators", true);
            }
            return true;
        }

        public void setTypingIndicatorsEnabled(boolean enabled) {
            if (prefs != null) {
                prefs.edit().putBoolean("typing_indicators", enabled).apply();
            }
        }

        public String getRcsUserAgent() {
            return "microG-RCS/2.0";
        }

        public String getRcsDeviceId() {
            return "microg-device-" + System.currentTimeMillis();
        }

        public boolean isRcsAvailableForNumber(String phoneNumber) {
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                return false;
            }
            return telephonyManager != null && telephonyManager.getNetworkType() != TelephonyManager.NETWORK_TYPE_UNKNOWN;
        }

        public int getRcsNetworkType() {
            if (telephonyManager != null) {
                try {
                    int networkType = telephonyManager.getNetworkType();
                    if (networkType == TelephonyManager.NETWORK_TYPE_LTE || 
                        networkType == TelephonyManager.NETWORK_TYPE_NR) {
                        return 1; // Mobile
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error getting network type", e);
                }
            }
            return 1; // Mobile
        }

        public boolean isRcsConnected() {
            return telephonyManager != null && telephonyManager.getNetworkType() != TelephonyManager.NETWORK_TYPE_UNKNOWN;
        }

        public int getRcsConnectionState() {
            return isRcsConnected() ? 2 : 0; // 2=Connected, 0=Disconnected
        }

        public int getRcsErrorCode() {
            return 0; // No error
        }

        public String getRcsErrorMessage() {
            return "";
        }

        public boolean isRcsAirplaneMode() {
            if (context != null) {
                try {
                    return Settings.Global.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON) == 1;
                } catch (Exception e) {
                    // ignore
                }
            }
            return false;
        }

        public String getRcsCarrierName() {
            if (telephonyManager != null) {
                try {
                    String carrierName = telephonyManager.getNetworkOperatorName();
                    if (carrierName != null && !carrierName.trim().isEmpty()) {
                        return carrierName;
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
            return "microG";
        }

        public Bundle getRcsCapabilities() {
            Bundle capabilities = new Bundle();
            capabilities.putBoolean("chat", true);
            capabilities.putBoolean("file_transfer", true);
            capabilities.putBoolean("geolocation", true);
            capabilities.putBoolean("group_chat", true);
            capabilities.putBoolean("read_receipts", true);
            capabilities.putBoolean("typing_indicators", true);
            return capabilities;
        }

        public void setRcsCapabilities(Bundle capabilities) {
            // Store capabilities if needed
            if (prefs != null && capabilities != null) {
                SharedPreferences.Editor editor = prefs.edit();
                for (String key : capabilities.keySet()) {
                    Object value = capabilities.get(key);
                    if (value instanceof Boolean) {
                        editor.putBoolean("cap_" + key, (Boolean) value);
                    }
                }
                editor.apply();
            }
        }

        public Bundle getRcsSettings() {
            Bundle settings = new Bundle();
            settings.putBoolean("rcs_enabled", isRcsEnabled());
            settings.putBoolean("capability_discovery_enabled", isCapabilityDiscoveryEnabled());
            settings.putInt("rcs_chat_mode", getRcsChatMode());
            settings.putBoolean("read_receipts_enabled", isReadReceiptsEnabled());
            settings.putBoolean("typing_indicators_enabled", isTypingIndicatorsEnabled());
            settings.putString("rcs_version", getRcsVersion());
            settings.putString("rcs_profile", getRcsProfile());
            settings.putBoolean("rcs_provisioned", isRcsProvisioned());
            settings.putInt("rcs_registration_state", getRcsRegistrationState());
            settings.putBoolean("rcs_connected", isRcsConnected());
            settings.putInt("rcs_connection_state", getRcsConnectionState());
            settings.putString("rcs_carrier_name", getRcsCarrierName());
            return settings;
        }

        public void setRcsSettings(Bundle settings) {
            if (prefs != null && settings != null) {
                SharedPreferences.Editor editor = prefs.edit();
                if (settings.containsKey("rcs_enabled")) {
                    setRcsEnabled(settings.getBoolean("rcs_enabled"));
                }
                if (settings.containsKey("capability_discovery_enabled")) {
                    setCapabilityDiscoveryEnabled(settings.getBoolean("capability_discovery_enabled"));
                }
                if (settings.containsKey("rcs_chat_mode")) {
                    setRcsChatMode(settings.getInt("rcs_chat_mode"));
                }
                if (settings.containsKey("read_receipts_enabled")) {
                    setReadReceiptsEnabled(settings.getBoolean("read_receipts_enabled"));
                }
                if (settings.containsKey("typing_indicators_enabled")) {
                    setTypingIndicatorsEnabled(settings.getBoolean("typing_indicators_enabled"));
                }
                editor.apply();
            }
        }

        public Bundle getRcsStatus() {
            Bundle status = new Bundle();
            status.putBoolean("supported", isRcsSupported());
            status.putBoolean("enabled", isRcsEnabled());
            status.putBoolean("provisioned", isRcsProvisioned());
            status.putInt("registration_state", getRcsRegistrationState());
            status.putBoolean("connected", isRcsConnected());
            status.putInt("connection_state", getRcsConnectionState());
            status.putInt("error_code", getRcsErrorCode());
            status.putString("error_message", getRcsErrorMessage());
            status.putString("version", getRcsVersion());
            status.putString("profile", getRcsProfile());
            status.putString("user_agent", getRcsUserAgent());
            status.putString("device_id", getRcsDeviceId());
            status.putString("carrier_name", getRcsCarrierName());
            status.putBoolean("airplane_mode", isRcsAirplaneMode());
            status.putInt("network_type", getRcsNetworkType());
            return status;
        }

        public boolean registerRcs() {
            try {
                Thread.sleep(100); // Simulate network delay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return true;
        }

        public boolean unregisterRcs() {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return true;
        }

        public boolean provisionRcs() {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return true;
        }

        public boolean deprovisionRcs() {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return true;
        }

        public boolean connectRcs() {
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return true;
        }

        public boolean disconnectRcs() {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return true;
        }

        public boolean sendRcsMessage(String phoneNumber, String message) {
            if (phoneNumber == null || message == null || phoneNumber.trim().isEmpty() || message.trim().isEmpty()) {
                return false;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return true;
        }

        public boolean sendRcsTypingIndicator(String phoneNumber, boolean isTyping) {
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                return false;
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return true;
        }

        public boolean sendRcsReadReceipt(String phoneNumber, String messageId) {
            if (phoneNumber == null || messageId == null || phoneNumber.trim().isEmpty() || messageId.trim().isEmpty()) {
                return false;
            }
            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return true;
        }
    }
}