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

package org.microg.gms.droidguard;

import android.content.Context;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.UUID;
import org.json.JSONObject;

public class RemoteDroidGuardConnector {
    private static final String TAG = "RemoteDroidGuard";
    private final Context context;
    private final String serverUrl;

    public RemoteDroidGuardConnector(Context context, String serverUrl) {
        this.context = context;
        this.serverUrl = serverUrl;
    }

    /**
     * Executes single-step DroidGuard attestation (SafetyNet legacy).
     */
    public byte[] evaluate(String type, String packageName, byte[] data, Map<String, String> extras) throws Exception {
        return executeStep(null, 0, type, packageName, data, extras);
    }

    /**
     * Executes multi-step DroidGuard attestation session (Play Integrity).
     */
    public byte[] evaluateMultiStep(String type, String packageName, byte[] initialData, Map<String, String> extras) throws Exception {
        String sessionId = UUID.randomUUID().toString();
        Log.d(TAG, "Starting multi-step Play Integrity DroidGuard session: " + sessionId);

        // Step 1: Initialize session
        byte[] step1Response = executeStep(sessionId, 1, type, packageName, initialData, extras);
        
        // Check if server indicated single-pass or required continuation
        JSONObject step1Json;
        try {
            step1Json = new JSONObject(new String(step1Response));
        } catch (Exception e) {
            // Raw token returned directly
            return step1Response;
        }

        if (step1Json.has("token")) {
            return step1Json.getString("token").getBytes();
        }

        if (step1Json.has("intermediateChallenge")) {
            String challenge = step1Json.getString("intermediateChallenge");
            // Step 2: Finalize attestation with challenge response
            byte[] step2Response = executeStep(sessionId, 2, type, packageName, challenge.getBytes(), extras);
            return step2Response;
        }

        return step1Response;
    }

    private byte[] executeStep(String sessionId, int step, String type, String packageName, byte[] data, Map<String, String> extras) throws Exception {
        if (serverUrl == null || serverUrl.isEmpty()) {
            throw new IllegalStateException("Remote DroidGuard server URL is not configured");
        }

        URL url = new URL(serverUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");

        if (sessionId != null) {
            conn.setRequestProperty("X-DroidGuard-Session", sessionId);
            conn.setRequestProperty("X-DroidGuard-Step", String.valueOf(step));
        }

        JSONObject requestObj = new JSONObject();
        requestObj.put("type", type);
        requestObj.put("package", packageName);
        requestObj.put("step", step);
        if (sessionId != null) {
            requestObj.put("sessionId", sessionId);
        }
        if (data != null) {
            requestObj.put("payload", android.util.Base64.encodeToString(data, android.util.Base64.NO_WRAP));
        }

        if (extras != null) {
            JSONObject extrasObj = new JSONObject();
            for (Map.Entry<String, String> entry : extras.entrySet()) {
                extrasObj.put(entry.getKey(), entry.getValue());
            }
            requestObj.put("extras", extrasObj);
        }

        OutputStream os = conn.getOutputStream();
        os.write(requestObj.toString().getBytes("UTF-8"));
        os.flush();
        os.close();

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new Exception("Remote DroidGuard server returned HTTP " + responseCode);
        }

        InputStream is = conn.getInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read;
        while ((read = is.read(buffer)) != -1) {
            baos.write(buffer, 0, read);
        }
        is.close();

        return baos.toByteArray();
    }
}