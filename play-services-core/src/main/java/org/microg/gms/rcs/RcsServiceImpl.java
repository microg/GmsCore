/*
 * Copyright (C) 2025 microG Project Team
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

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Parcel;
import android.os.RemoteException;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.rcs.internal.IRcsCallbacks;
import com.google.android.gms.rcs.internal.IRcsService;
import com.google.android.gms.rcs.internal.RcsCapabilitiesRequest;
import com.google.android.gms.rcs.internal.RcsCapabilitiesResponse;
import com.google.android.gms.rcs.internal.RcsConfigurationRequest;
import com.google.android.gms.rcs.internal.RcsConfigurationResponse;
import com.google.android.gms.rcs.internal.RcsMessageRequest;
import com.google.android.gms.rcs.internal.RcsMessageResponse;

import org.microg.gms.common.PackageUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RcsServiceImpl extends IRcsService.Stub {
    private static final String TAG = "GmsRcsImpl";
    private static final int STATUS_OK = 0;
    private static final int STATUS_ERROR = 13; // CommonStatusCodes.ERROR
    
    private final Context context;
    private final Map<String, String> rcsConfiguration = new HashMap<>();
    private final Map<String, IRcsCallbacks> registeredCallbacks = new HashMap<>();
    private boolean rcsEnabled = false;
    private RcsManager rcsManager;

    public RcsServiceImpl(Context context) {
        this.context = context;
        this.rcsManager = new RcsManager(context);
        initializeDefaultConfiguration();
    }

    private void initializeDefaultConfiguration() {
        // Initialize default RCS configuration
        rcsConfiguration.put("rcs.enabled", "false");
        rcsConfiguration.put("rcs.server.host", "config.rcs.mnc001.mcc001.pub.3gppnetwork.org");
        rcsConfiguration.put("rcs.server.port", "443");
        rcsConfiguration.put("rcs.user.agent", "microG-RCS/1.0");
        rcsConfiguration.put("rcs.capability.chat", "true");
        rcsConfiguration.put("rcs.capability.file_transfer", "true");
        rcsConfiguration.put("rcs.capability.group_chat", "true");
        rcsConfiguration.put("rcs.capability.geolocation", "true");
        rcsConfiguration.put("rcs.capability.read_receipts", "true");
        rcsConfiguration.put("rcs.capability.typing_indicators", "true");
    }

    @Override
    public void getRcsCapabilities(IRcsCallbacks callbacks, RcsCapabilitiesRequest request) throws RemoteException {
        Log.d(TAG, "getRcsCapabilities: " + request.phoneNumber + ", forceRefresh: " + request.forceRefresh);
        
        try {
            RcsCapabilitiesResponse response = new RcsCapabilitiesResponse();
            response.phoneNumber = request.phoneNumber;
            
            if (rcsEnabled && rcsManager.isPhoneNumberRcsCapable(request.phoneNumber)) {
                response.isRcsCapable = true;
                response.supportsChatMessages = Boolean.parseBoolean(rcsConfiguration.get("rcs.capability.chat"));
                response.supportsFileTransfer = Boolean.parseBoolean(rcsConfiguration.get("rcs.capability.file_transfer"));
                response.supportsGroupMessages = Boolean.parseBoolean(rcsConfiguration.get("rcs.capability.group_chat"));
                response.supportsGeoLocation = Boolean.parseBoolean(rcsConfiguration.get("rcs.capability.geolocation"));
                response.supportsReadReceipts = Boolean.parseBoolean(rcsConfiguration.get("rcs.capability.read_receipts"));
                response.supportsTypingIndicators = Boolean.parseBoolean(rcsConfiguration.get("rcs.capability.typing_indicators"));
                response.capabilityTimestamp = System.currentTimeMillis();
            }

            callbacks.onRcsCapabilities(Status.SUCCESS, response);
        } catch (Exception e) {
            Log.e(TAG, "Error getting RCS capabilities", e);
            callbacks.onRcsCapabilities(new Status(STATUS_ERROR, e.getMessage()), null);
        }
    }

    @Override
    public void enableRcs(IRcsCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "enableRcs");
        
        try {
            // Check for required permissions
            if (context.checkCallingPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED ||
                context.checkCallingPermission(Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED ||
                context.checkCallingPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                callbacks.onRcsEnabled(new Status(STATUS_ERROR, "Missing required permissions"));
                return;
            }

            boolean success = rcsManager.enableRcs();
            if (success) {
                rcsEnabled = true;
                rcsConfiguration.put("rcs.enabled", "true");
                callbacks.onRcsEnabled(Status.SUCCESS);
                
                // Notify all registered callbacks
                for (IRcsCallbacks callback : registeredCallbacks.values()) {
                    try {
                        callback.onRcsStatusUpdated(Status.SUCCESS, true);
                    } catch (RemoteException e) {
                        Log.w(TAG, "Failed to notify callback of RCS status update", e);
                    }
                }
            } else {
                callbacks.onRcsEnabled(new Status(STATUS_ERROR, "Failed to enable RCS"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error enabling RCS", e);
            callbacks.onRcsEnabled(new Status(STATUS_ERROR, e.getMessage()));
        }
    }

    @Override
    public void disableRcs(IRcsCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "disableRcs");
        
        try {
            boolean success = rcsManager.disableRcs();
            if (success) {
                rcsEnabled = false;
                rcsConfiguration.put("rcs.enabled", "false");
                callbacks.onRcsDisabled(Status.SUCCESS);
                
                // Notify all registered callbacks
                for (IRcsCallbacks callback : registeredCallbacks.values()) {
                    try {
                        callback.onRcsStatusUpdated(Status.SUCCESS, false);
                    } catch (RemoteException e) {
                        Log.w(TAG, "Failed to notify callback of RCS status update", e);
                    }
                }
            } else {
                callbacks.onRcsDisabled(new Status(STATUS_ERROR, "Failed to disable RCS"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error disabling RCS", e);
            callbacks.onRcsDisabled(new Status(STATUS_ERROR, e.getMessage()));
        }
    }

    @Override
    public void getRcsConfiguration(IRcsCallbacks callbacks, RcsConfigurationRequest request) throws RemoteException {
        Log.d(TAG, "getRcsConfiguration: " + request.configKey);
        
        try {
            RcsConfigurationResponse response = new RcsConfigurationResponse();
            response.configKey = request.configKey;
            response.configValue = rcsConfiguration.get(request.configKey);
            response.lastUpdated = System.currentTimeMillis();
            
            if (response.configValue != null) {
                callbacks.onRcsConfiguration(Status.SUCCESS, response);
            } else {
                callbacks.onRcsConfiguration(new Status(STATUS_ERROR, "Configuration key not found"), null);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting RCS configuration", e);
            callbacks.onRcsConfiguration(new Status(STATUS_ERROR, e.getMessage()), null);
        }
    }

    @Override
    public void setRcsConfiguration(IRcsCallbacks callbacks, RcsConfigurationRequest request) throws RemoteException {
        Log.d(TAG, "setRcsConfiguration: " + request.configKey + " = " + request.configValue);
        
        try {
            rcsConfiguration.put(request.configKey, request.configValue);
            callbacks.onRcsConfigurationSet(Status.SUCCESS);
        } catch (Exception e) {
            Log.e(TAG, "Error setting RCS configuration", e);
            callbacks.onRcsConfigurationSet(new Status(STATUS_ERROR, e.getMessage()));
        }
    }

    @Override
    public void isRcsEnabled(IRcsCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "isRcsEnabled");
        
        try {
            callbacks.onRcsEnabledStatus(Status.SUCCESS, rcsEnabled);
        } catch (Exception e) {
            Log.e(TAG, "Error checking RCS status", e);
            callbacks.onRcsEnabledStatus(new Status(STATUS_ERROR, e.getMessage()), false);
        }
    }

    @Override
    public void sendRcsMessage(IRcsCallbacks callbacks, RcsMessageRequest request) throws RemoteException {
        Log.d(TAG, "sendRcsMessage: " + request.messageId + " to " + (request.recipients != null ? request.recipients.length : 0) + " recipients");
        
        try {
            if (!rcsEnabled) {
                callbacks.onRcsMessageSent(new Status(STATUS_ERROR, "RCS is not enabled"), null);
                return;
            }

            RcsMessageResponse response = new RcsMessageResponse();
            response.messageId = request.messageId != null ? request.messageId : UUID.randomUUID().toString();
            response.threadId = request.threadId;
            response.timestamp = System.currentTimeMillis();
            
            // Attempt to send the message through RCS manager
            boolean success = rcsManager.sendMessage(request);
            if (success) {
                response.status = 1; // Sent
                response.statusText = "Message sent successfully";
                callbacks.onRcsMessageSent(Status.SUCCESS, response);
            } else {
                response.status = -1; // Failed
                response.statusText = "Failed to send message";
                callbacks.onRcsMessageSent(new Status(STATUS_ERROR, "Failed to send RCS message"), response);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending RCS message", e);
            callbacks.onRcsMessageSent(new Status(STATUS_ERROR, e.getMessage()), null);
        }
    }

    @Override
    public void registerForRcsUpdates(IRcsCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "registerForRcsUpdates");
        
        String callbackId = UUID.randomUUID().toString();
        registeredCallbacks.put(callbackId, callbacks);
    }

    @Override
    public void unregisterForRcsUpdates(IRcsCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unregisterForRcsUpdates");
        
        registeredCallbacks.values().removeIf(cb -> cb.equals(callbacks));
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (super.onTransact(code, data, reply, flags)) return true;
        Log.d(TAG, "onTransact [unknown]: " + code + ", " + data + ", " + flags);
        return false;
    }
}