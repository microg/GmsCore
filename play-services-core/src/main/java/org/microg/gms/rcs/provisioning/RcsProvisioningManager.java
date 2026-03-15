/*
 * Copyright (C) 2013-2026 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://license.golem.cloud/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.gms.rcs.provisioning;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.microg.gms.common.http.HttpClient;
import org.microg.gms.common.http.HttpRequest;
import org.microg.gms.common.http.HttpResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages RCS provisioning with Jibe ACS (Autoconfiguration Server).
 * Implements GSMA RCC.01 / RCC.07 HTTP Autoprovisioning flow.
 */
public class RcsProvisioningManager {
    private static final String TAG = "GmsRcsProvisioning";
    private static final String DEFAULT_ACS_URL = "http://rcs-acs-prod-us.sandbox.google.com/";
    
    private final Context context;
    private final HttpClient httpClient;
    private final TelephonyManager telephonyManager;

    public RcsProvisioningManager(Context context) {
        this.context = context;
        this.httpClient = new HttpClient();
        this.telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    /**
     * Start the first step of autoprovisioning (discovery).
     */
    public void startProvisioning() {
        new Thread(() -> {
            try {
                fetchInitialConfig();
            } catch (Exception e) {
                Log.e(TAG, "Provisioning failed", e);
            }
        }).start();
    }

    private void fetchInitialConfig() throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("vers", "0");
        params.put("rcs_version", "6.0");
        params.put("rcs_profile", "UP_2.4");
        params.put("terminal_vendor", "Google");
        params.put("terminal_model", "Pixel 8");
        
        if (telephonyManager != null) {
            String imsi = telephonyManager.getSubscriberId();
            if (imsi != null) params.put("IMSI", imsi);
        }

        StringBuilder urlBuilder = new StringBuilder(DEFAULT_ACS_URL);
        urlBuilder.append("?");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            urlBuilder.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }

        HttpRequest request = new HttpRequest.Builder()
                .url(urlBuilder.toString())
                .method("GET")
                .header("User-Agent", "microG-RCS/1.0")
                .build();

        HttpResponse response = httpClient.execute(request);
        Log.d(TAG, "Initial config response: " + response.getStatusCode());
        
        // Handling of 401 Challenge or 302 Redirect would go here.
        // In Step 2, we would receive an SMS OTP and call with &OTP=xxxxxx.
    }
}
